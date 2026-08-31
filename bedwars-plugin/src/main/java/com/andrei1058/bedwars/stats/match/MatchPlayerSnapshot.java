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

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Immutable player statistics captured at one point in a match.
 *
 * <p>The K/D ratio uses regular kills plus final kills as its numerator.
 * When a player has no deaths, {@link #kdRatio()} is empty so persistence
 * code can store SQL NULL instead of an arbitrary value.</p>
 */
public record MatchPlayerSnapshot(
        UUID playerUuid,
        @Nullable String playerName,
        @Nullable String teamId,
        int kills,
        int finalKills,
        int deaths,
        int bedsDestroyed,
        int illegalTeamVl,
        int killBoostingVl,
        int evidenceAdjustment,
        int reconnects,
        int disconnects,
        MatchPlayerOutcome outcome
) {

    /** Source-compatible constructor for integrations written before signed evidence was added. */
    public MatchPlayerSnapshot(UUID playerUuid, @Nullable String playerName, @Nullable String teamId,
                               int kills, int finalKills, int deaths, int bedsDestroyed,
                               int illegalTeamVl, int killBoostingVl, int reconnects,
                               int disconnects, MatchPlayerOutcome outcome) {
        this(playerUuid, playerName, teamId, kills, finalKills, deaths, bedsDestroyed,
                illegalTeamVl, killBoostingVl, 0, reconnects, disconnects, outcome);
    }

    public MatchPlayerSnapshot {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(outcome, "outcome");
        if (kills < 0 || finalKills < 0 || deaths < 0 || bedsDestroyed < 0
                || illegalTeamVl < 0 || killBoostingVl < 0 || reconnects < 0 || disconnects < 0) {
            throw new IllegalArgumentException("Player statistics cannot be negative");
        }
    }

    public int totalKills() {
        return kills + finalKills;
    }

    public int totalVl() {
        long effective = (long) rawVl() + evidenceAdjustment;
        return effective <= 0 ? 0 : effective >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) effective;
    }

    /** Positive VL before signed exclusion evidence is applied. */
    public int rawVl() {
        long raw = (long) illegalTeamVl + killBoostingVl;
        return raw >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) raw;
    }

    public OptionalDouble kdRatio() {
        if (deaths == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) totalKills() / deaths);
    }
}
