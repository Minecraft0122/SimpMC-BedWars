/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.stats.match;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Player counter registry for one match.
 */
public final class MatchStats {

    private final Map<UUID, MatchPlayerStats> players = new LinkedHashMap<>();

    /**
     * Register a player once, returning the existing counters on duplicate
     * registration. This makes join/rejoin handling idempotent.
     */
    public synchronized MatchPlayerStats registerPlayer(
            UUID playerUuid,
            @Nullable String playerName,
            @Nullable String teamId
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        MatchPlayerStats player = players.get(playerUuid);
        if (player == null) {
            player = new MatchPlayerStats(playerUuid, playerName, teamId);
            players.put(playerUuid, player);
        } else {
            player.updateIdentity(playerName, teamId);
        }
        return player;
    }

    public synchronized Optional<MatchPlayerStats> getPlayer(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return Optional.ofNullable(players.get(playerUuid));
    }

    public synchronized List<MatchPlayerStats> getPlayers() {
        return List.copyOf(players.values());
    }

    public synchronized MatchStatsSnapshot snapshot() {
        List<MatchPlayerSnapshot> snapshots = new ArrayList<>(players.size());
        for (MatchPlayerStats player : players.values()) {
            snapshots.add(player.snapshot());
        }
        return new MatchStatsSnapshot(snapshots);
    }
}
