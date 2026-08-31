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

import java.util.Locale;
import java.util.Set;

/** Immutable status advertised by one runtime arena copy. */
public record ArenaNodeSnapshot(
        String sessionId,
        String serverId,
        String proxyServer,
        String nodeInstanceId,
        String arenaName,
        String arenaIdentifier,
        String status,
        int currentPlayers,
        int maxPlayers,
        int maxInTeam,
        Set<String> groups,
        boolean spectate,
        long sequence,
        long lastSeenMillis,
        boolean dispatchable) {

    public ArenaNodeSnapshot {
        sessionId = normalize(sessionId);
        serverId = normalize(serverId);
        proxyServer = normalize(proxyServer);
        nodeInstanceId = normalize(nodeInstanceId);
        arenaName = normalize(arenaName);
        arenaIdentifier = normalize(arenaIdentifier);
        status = normalize(status).toUpperCase(Locale.ROOT);
        groups = groups == null || groups.isEmpty() ? Set.of("DEFAULT") : Set.copyOf(groups.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet()));
        if (groups.isEmpty()) groups = Set.of("DEFAULT");
        currentPlayers = Math.max(0, currentPlayers);
        maxPlayers = Math.max(0, maxPlayers);
        maxInTeam = Math.max(0, maxInTeam);
        lastSeenMillis = Math.max(0, lastSeenMillis);
    }

    public String key() {
        return sessionId + '|' + arenaIdentifier;
    }

    public boolean isWaitingOrStarting() {
        return "WAITING".equals(status) || "STARTING".equals(status);
    }

    public boolean hasCapacity(int amount, int reserved) {
        return amount > 0 && maxPlayers > 0 && currentPlayers + reserved + amount <= maxPlayers;
    }

    public boolean belongsToGroup(String group) {
        return group == null || group.isBlank() || groups.contains(group.trim().toUpperCase(Locale.ROOT));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
