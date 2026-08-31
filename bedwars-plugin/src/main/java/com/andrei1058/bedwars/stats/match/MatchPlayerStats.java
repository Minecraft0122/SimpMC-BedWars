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
import java.util.UUID;

/**
 * Mutable, synchronized counters for one player in one match.
 *
 * <p>Event listeners update this object on the server thread while database
 * workers may request snapshots asynchronously. Synchronization keeps each
 * snapshot internally consistent without exposing mutable counters.</p>
 */
public final class MatchPlayerStats {

    private final UUID playerUuid;
    private String playerName;
    private String teamId;
    private int kills;
    private int finalKills;
    private int deaths;
    private int bedsDestroyed;
    private int illegalTeamVl;
    private int killBoostingVl;
    /** Signed exclusion evidence; positive VL counters remain non-negative. */
    private int evidenceAdjustment;
    private int reconnects;
    private int disconnects;
    private MatchPlayerOutcome outcome = MatchPlayerOutcome.UNKNOWN;

    public MatchPlayerStats(UUID playerUuid, @Nullable String playerName, @Nullable String teamId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.playerName = playerName;
        this.teamId = teamId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public synchronized String getPlayerName() {
        return playerName;
    }

    public synchronized String getTeamId() {
        return teamId;
    }

    /** Update identity fields when a player rejoins with a current name/team. */
    public synchronized void updateIdentity(@Nullable String playerName, @Nullable String teamId) {
        if (playerName != null) {
            this.playerName = playerName;
        }
        if (teamId != null) {
            this.teamId = teamId;
        }
    }

    public synchronized void recordKill(boolean finalKill) {
        if (finalKill) {
            finalKills = increment(finalKills, "finalKills");
        } else {
            kills = increment(kills, "kills");
        }
    }

    public synchronized void recordDeath() {
        deaths = increment(deaths, "deaths");
    }

    public synchronized void recordBedBreak() {
        bedsDestroyed = increment(bedsDestroyed, "bedsDestroyed");
    }

    public synchronized void addIllegalTeamVl(int amount) {
        illegalTeamVl = addNonNegative(illegalTeamVl, amount, "illegalTeamVl");
    }

    public synchronized void addKillBoostingVl(int amount) {
        killBoostingVl = addNonNegative(killBoostingVl, amount, "killBoostingVl");
    }

    /**
     * Add signed exclusion evidence. Negative values reduce the effective
     * match VL while remaining separately auditable in the database.
     */
    public synchronized void adjustEvidence(int amount) {
        try {
            evidenceAdjustment = Math.addExact(evidenceAdjustment, amount);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("evidenceAdjustment overflow", exception);
        }
    }

    public synchronized void recordReconnect() {
        reconnects = increment(reconnects, "reconnects");
    }

    public synchronized void recordDisconnect() {
        disconnects = increment(disconnects, "disconnects");
    }

    public synchronized void setOutcome(MatchPlayerOutcome outcome) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public synchronized MatchPlayerSnapshot snapshot() {
        return new MatchPlayerSnapshot(
                playerUuid,
                playerName,
                teamId,
                kills,
                finalKills,
                deaths,
                bedsDestroyed,
                illegalTeamVl,
                killBoostingVl,
                evidenceAdjustment,
                reconnects,
                disconnects,
                outcome
        );
    }

    private static int addNonNegative(int current, int amount, String counter) {
        if (amount < 0) {
            throw new IllegalArgumentException(counter + " increment cannot be negative");
        }
        try {
            return Math.addExact(current, amount);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(counter + " overflow", exception);
        }
    }

    private static int increment(int current, String counter) {
        return addNonNegative(current, 1, counter);
    }
}
