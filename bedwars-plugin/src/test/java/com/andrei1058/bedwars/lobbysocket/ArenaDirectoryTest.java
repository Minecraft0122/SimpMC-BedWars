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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDirectoryTest {

    private static final long NOW = 100_000L;

    @Test
    void defaultSelectionPrefersAnEmptyWaitingCopy() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot busyWaiting = node("node-busy", "Busy", "world-busy", "WAITING", 2, 8,
                Set.of("SOLO"), 1, NOW, true);
        ArenaNodeSnapshot emptyWaiting = node("node-empty", "Empty", "world-empty", "WAITING", 0, 8,
                Set.of("SOLO"), 1, NOW, true);
        ArenaNodeSnapshot emptyStarting = node("node-starting", "Starting", "world-starting", "STARTING", 0, 8,
                Set.of("SOLO"), 1, NOW, true);
        directory.upsert(busyWaiting);
        directory.upsert(emptyStarting);
        directory.upsert(emptyWaiting);

        assertSame(emptyWaiting, directory.select("solo", null, 1, NOW, 1_000L).orElseThrow());
    }

    @Test
    void selectorMatchesConfiguredMapNameOrRuntimeIdentifier() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot map = node("node-1", "SkyWars", "skywars-copy-7", "WAITING", 0, 8,
                Set.of("DOUBLES"), 1, NOW, true);
        directory.upsert(map);

        assertSame(map, directory.select("ignored", "skywars", 1, NOW, 1_000L).orElseThrow());
        assertSame(map, directory.select("ignored", "SKYWARS-COPY-7", 1, NOW, 1_000L).orElseThrow());
    }

    @Test
    void selectionFiltersByGroupWhenNoMapWasSpecified() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot solo = node("node-solo", "Solo", "solo-copy", "WAITING", 0, 8,
                Set.of("SOLO"), 1, NOW, true);
        ArenaNodeSnapshot doubles = node("node-doubles", "Doubles", "doubles-copy", "WAITING", 0, 8,
                Set.of("DOUBLES"), 1, NOW, true);
        directory.upsert(solo);
        directory.upsert(doubles);

        assertSame(solo, directory.select("solo", null, 1, NOW, 1_000L).orElseThrow());
        assertTrue(directory.select("triples", null, 1, NOW, 1_000L).isEmpty());
    }

    @Test
    void staleNonDispatchableAndPlayingCopiesAreExcluded() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot stale = node("node-stale", "Stale", "stale-copy", "WAITING", 0, 8,
                Set.of("SOLO"), 1, NOW - 101L, true);
        ArenaNodeSnapshot disabled = node("node-disabled", "Disabled", "disabled-copy", "WAITING", 0, 8,
                Set.of("SOLO"), 1, NOW, false);
        ArenaNodeSnapshot playing = node("node-playing", "Playing", "playing-copy", "PLAYING", 0, 8,
                Set.of("SOLO"), 1, NOW, true);
        directory.upsert(stale);
        directory.upsert(disabled);
        directory.upsert(playing);

        assertTrue(directory.select("solo", null, 1, NOW, 100L).isEmpty());
    }

    @Test
    void spectatorSelectionAllowsAFullPlayingCopyOnlyWhenSpectating() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot playing = node("node-playing", "Playing", "playing-copy", "PLAYING", 8, 8,
                Set.of("SOLO"), 1, NOW, true);
        // The helper defaults spectate=false; construct the advertised copy
        // explicitly so the capacity rule is exercised independently.
        playing = new ArenaNodeSnapshot(playing.sessionId(), playing.serverId(), playing.proxyServer(),
                playing.nodeInstanceId(), playing.arenaName(), playing.arenaIdentifier(), playing.status(),
                playing.currentPlayers(), playing.maxPlayers(), playing.maxInTeam(), playing.groups(), true,
                playing.sequence(), playing.lastSeenMillis(), playing.dispatchable());
        directory.upsert(playing);

        assertTrue(directory.select("solo", "playing-copy", 1, NOW, 1_000L).isEmpty());
        assertSame(playing, directory.select("solo", "playing-copy", 1, NOW, 1_000L, true).orElseThrow());
    }

    @Test
    void reservationIsCapacityBoundAndReleaseReturnsCapacity() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot arena = node("node-1", "Map", "copy", "WAITING", 2, 4,
                Set.of("SOLO"), 1, NOW, true);
        directory.upsert(arena);

        assertTrue(directory.reserve(arena, 2));
        assertFalse(directory.reserve(arena, 1));
        assertEquals(2, directory.reserved(arena.key()));

        directory.release(arena, 1);
        assertEquals(1, directory.reserved(arena.key()));
        directory.release(arena, 5);
        assertEquals(0, directory.reserved(arena.key()));
        assertTrue(directory.reserve(arena, 2));
    }

    @Test
    void concurrentReservationsCannotExceedAdvertisedCapacity() throws Exception {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot arena = node("node-atomic", "Map", "copy", "WAITING", 0, 4,
                Set.of("SOLO"), 1, NOW, true);
        directory.upsert(arena);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> calls = java.util.stream.Stream.generate(() ->
                    (Callable<Boolean>) () -> directory.reserve(arena, 1)).limit(8).toList();
            List<Future<Boolean>> results = executor.invokeAll(calls);
            long successful = results.stream().filter(this::successful).count();
            assertEquals(4L, successful);
            assertEquals(4, directory.reserved(arena.key()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void olderSequenceDoesNotReplaceNewerStateButNewNodeInstanceCan() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot newest = node("node-1", "Map", "copy", "WAITING", 3, 8,
                Set.of("SOLO"), 9, NOW, true);
        ArenaNodeSnapshot oldSequence = node("node-1", "Map", "copy", "PLAYING", 4, 8,
                Set.of("SOLO"), 8, NOW + 1, true);
        ArenaNodeSnapshot restartedNode = nodeWithInstance("node-1", "instance-2", "Map", "copy", "WAITING", 0, 8,
                Set.of("SOLO"), 1, NOW + 2, true);
        directory.upsert(newest);
        directory.upsert(oldSequence);
        assertEquals(newest, directory.snapshot().get(0));

        directory.upsert(restartedNode);
        assertEquals(List.of(restartedNode), directory.snapshot());
    }

    @Test
    void lateReleaseFromPreviousNodeInstanceCannotConsumeReplacementLease() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot firstInstance = node("node-1", "Map", "copy", "WAITING", 0, 4,
                Set.of("SOLO"), 1, NOW, true);
        ArenaNodeSnapshot replacement = nodeWithInstance("node-1", "instance-2", "Map", "copy", "WAITING",
                0, 4, Set.of("SOLO"), 1, NOW + 1, true);
        directory.upsert(firstInstance);
        assertTrue(directory.reserve(firstInstance, 2));

        directory.upsert(replacement);
        assertEquals(0, directory.reserved(replacement.key()));
        assertTrue(directory.reserve(replacement, 4));

        // The old batch may finish after the node has restarted. Its release
        // must not decrement the replacement's lease.
        directory.release(firstInstance, 2);
        assertEquals(4, directory.reserved(replacement.key()));
    }

    @Test
    void removalOnlyDeletesTheAdvertisedNodeInstance() {
        ArenaDirectory directory = new ArenaDirectory();
        ArenaNodeSnapshot first = node("node-1", "Map", "copy", "WAITING", 0, 4,
                Set.of("SOLO"), 1, NOW, true);
        ArenaNodeSnapshot replacement = nodeWithInstance("node-1", "instance-2", "Map", "copy",
                "WAITING", 0, 4, Set.of("SOLO"), 2, NOW + 1, true);
        directory.upsert(first);
        directory.upsert(replacement);

        directory.remove(replacement.sessionId(), first.nodeInstanceId(), replacement.arenaIdentifier());
        assertEquals(List.of(replacement), directory.snapshot());

        directory.remove(replacement.sessionId(), replacement.nodeInstanceId(), replacement.arenaIdentifier());
        assertTrue(directory.snapshot().isEmpty());
    }

    private static ArenaNodeSnapshot node(String sessionId, String arenaName, String identifier,
                                          String status, int currentPlayers, int maxPlayers,
                                          Set<String> groups, long sequence, long lastSeen, boolean dispatchable) {
        return nodeWithInstance(sessionId, "instance-1", arenaName, identifier, status, currentPlayers,
                maxPlayers, groups, sequence, lastSeen, dispatchable);
    }

    private static ArenaNodeSnapshot nodeWithInstance(String sessionId, String instanceId,
                                                      String arenaName, String identifier, String status,
                                                      int currentPlayers, int maxPlayers, Set<String> groups,
                                                      long sequence, long lastSeen, boolean dispatchable) {
        return new ArenaNodeSnapshot(sessionId, sessionId, sessionId, instanceId, arenaName, identifier,
                status, currentPlayers, maxPlayers, 2, groups, false, sequence, lastSeen, dispatchable);
    }

    private boolean successful(Future<Boolean> result) {
        try {
            return result.get();
        } catch (Exception exception) {
            throw new AssertionError("reservation task failed", exception);
        }
    }
}
