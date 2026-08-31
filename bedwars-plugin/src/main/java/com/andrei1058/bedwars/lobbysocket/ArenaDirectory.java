/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.lobbysocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory directory of remote arena copies.
 *
 * <p>The directory deliberately owns short-lived capacity reservations. A
 * lobby process can therefore reject two simultaneous requests before either
 * remote node has observed the corresponding player connection. A second
 * lobby should share a lease service (Redis/SQL) when it is introduced; this
 * class is intentionally the single-lobby implementation.</p>
 */
public final class ArenaDirectory {

    private final ConcurrentHashMap<String, ArenaNodeSnapshot> arenas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> reservations = new ConcurrentHashMap<>();
    /** Serializes reservation lifecycle with node replacement/removal. */
    private final Object reservationLock = new Object();

    public void upsert(ArenaNodeSnapshot snapshot) {
        if (snapshot == null || snapshot.sessionId().isBlank() || snapshot.arenaIdentifier().isBlank()) return;
        synchronized (reservationLock) {
            arenas.compute(snapshot.key(), (key, previous) -> {
                if (previous == null) return snapshot;
                if (!previous.nodeInstanceId().equals(snapshot.nodeInstanceId())) {
                    // A restarted node has no knowledge of leases created by
                    // its predecessor. Drop them before accepting its state.
                    reservations.remove(key);
                    return snapshot;
                }
                return snapshot.sequence() >= previous.sequence() ? snapshot : previous;
            });
        }
    }

    public void removeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        synchronized (reservationLock) {
            arenas.entrySet().removeIf(entry -> entry.getValue().sessionId().equals(sessionId));
            reservations.keySet().removeIf(key -> key.startsWith(sessionId + "|"));
        }
    }

    /** Remove one arena copy, ignoring stale removal messages from a replaced node. */
    public void remove(String sessionId, String nodeInstanceId, String arenaIdentifier) {
        if (sessionId == null || sessionId.isBlank()
                || arenaIdentifier == null || arenaIdentifier.isBlank()) return;
        String key = sessionId + '|' + arenaIdentifier.trim();
        synchronized (reservationLock) {
            ArenaNodeSnapshot current = arenas.get(key);
            if (current == null) return;
            if (nodeInstanceId != null && !nodeInstanceId.isBlank()
                    && !current.nodeInstanceId().equals(nodeInstanceId.trim())) return;
            arenas.remove(key, current);
            reservations.remove(key);
        }
    }

    public List<ArenaNodeSnapshot> snapshot() {
        return List.copyOf(arenas.values());
    }

    public List<String> suggestions() {
        java.util.TreeSet<String> values = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ArenaNodeSnapshot arena : arenas.values()) {
            values.add(arena.arenaName());
            values.addAll(arena.groups());
        }
        return List.copyOf(values);
    }

    /**
     * Select a dispatchable copy. If {@code arenaSelector} is non-empty it
     * matches either the configured map name or the runtime world identifier;
     * otherwise {@code group} filters the advertised groups.
     */
    public Optional<ArenaNodeSnapshot> select(String group, String arenaSelector, int amount,
                                              long nowMillis, long timeoutMillis) {
        return select(group, arenaSelector, amount, nowMillis, timeoutMillis, false);
    }

    /**
     * Select a copy for either a normal join or a spectator handoff. Playing
     * copies are deliberately considered only for spectator requests; normal
     * joins must never race a game already in progress.
     */
    public Optional<ArenaNodeSnapshot> select(String group, String arenaSelector, int amount,
                                              long nowMillis, long timeoutMillis,
                                              boolean spectator) {
        String normalizedGroup = normalize(group);
        String normalizedArena = normalize(arenaSelector);
        List<Candidate> candidates = new ArrayList<>();
        for (ArenaNodeSnapshot arena : arenas.values()) {
            if (!arena.dispatchable()) continue;
            boolean playable = arena.isWaitingOrStarting();
            boolean spectatable = spectator && "PLAYING".equals(arena.status()) && arena.spectate();
            if (!playable && !spectatable) continue;
            if (nowMillis - arena.lastSeenMillis() > timeoutMillis) continue;
            if (!normalizedArena.isEmpty()) {
                if (!arena.arenaName().equalsIgnoreCase(normalizedArena)
                        && !arena.arenaIdentifier().equalsIgnoreCase(normalizedArena)) continue;
            } else if (!arena.belongsToGroup(normalizedGroup)) {
                continue;
            }
            int reserved = reserved(arena.key());
            // Spectators do not consume the waiting-room player capacity.
            if (!spectator && !arena.hasCapacity(amount, reserved)) continue;
            candidates.add(new Candidate(arena, reserved));
        }
        if (candidates.isEmpty()) return Optional.empty();

        // Empty waiting copies are preferred. This is the default behavior
        // requested for a new game and leaves starting copies as a fallback.
        List<Candidate> empty = spectator ? List.of() : candidates.stream()
                .filter(candidate -> candidate.snapshot.currentPlayers() == 0
                        && candidate.snapshot.status().equals("WAITING")
                        && candidate.reserved == 0)
                .toList();
        List<Candidate> pool = empty.isEmpty() ? candidates : empty;
        int leastLoad = pool.stream().mapToInt(candidate -> candidate.snapshot.currentPlayers()
                + candidate.reserved).min().orElse(0);
        List<Candidate> leastLoaded = pool.stream()
                .filter(candidate -> candidate.snapshot.currentPlayers() + candidate.reserved == leastLoad)
                .toList();
        return Optional.of(leastLoaded.get(ThreadLocalRandom.current().nextInt(leastLoaded.size())).snapshot);
    }

    /** Atomically reserve capacity on a selected copy. */
    public boolean reserve(ArenaNodeSnapshot snapshot, int amount) {
        if (snapshot == null || amount <= 0) return false;
        synchronized (reservationLock) {
            ArenaNodeSnapshot current = arenas.get(snapshot.key());
            if (current == null || !current.nodeInstanceId().equals(snapshot.nodeInstanceId())) return false;
            AtomicInteger value = reservations.computeIfAbsent(snapshot.key(), ignored -> new AtomicInteger());
            int old = value.get();
            if (!current.hasCapacity(amount, old)) return false;
            value.set(old + amount);
            return true;
        }
    }

    public void release(ArenaNodeSnapshot snapshot, int amount) {
        if (snapshot == null || amount <= 0) return;
        synchronized (reservationLock) {
            ArenaNodeSnapshot current = arenas.get(snapshot.key());
            // A late completion from an old node instance must never release
            // a lease belonging to the replacement instance.
            if (current != null && !current.nodeInstanceId().equals(snapshot.nodeInstanceId())) return;
            AtomicInteger value = reservations.get(snapshot.key());
            if (value == null) return;
            value.set(Math.max(0, value.get() - amount));
            if (value.get() == 0) reservations.remove(snapshot.key(), value);
        }
    }

    public int reserved(String key) {
        AtomicInteger value = reservations.get(key);
        return value == null ? 0 : Math.max(0, value.get());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record Candidate(ArenaNodeSnapshot snapshot, int reserved) {
    }
}
