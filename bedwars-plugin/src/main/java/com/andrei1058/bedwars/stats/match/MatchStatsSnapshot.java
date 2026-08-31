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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of all player counters in a match.
 */
public record MatchStatsSnapshot(List<MatchPlayerSnapshot> players) {

    public MatchStatsSnapshot {
        Objects.requireNonNull(players, "players");
        players = List.copyOf(players);
    }

    public MatchPlayerSnapshot player(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return players.stream()
                .filter(player -> player.playerUuid().equals(playerUuid))
                .findFirst()
                .orElse(null);
    }

    public int totalKills() {
        return players.stream().mapToInt(MatchPlayerSnapshot::totalKills).sum();
    }

    public int totalBedsDestroyed() {
        return players.stream().mapToInt(MatchPlayerSnapshot::bedsDestroyed).sum();
    }
}
