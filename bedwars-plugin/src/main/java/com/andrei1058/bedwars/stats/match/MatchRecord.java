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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Mutable, main-thread-owned lifecycle state for one playing arena.
 *
 * <p>The database worker only receives the immutable records produced by this
 * class. All methods are synchronized so a worker can safely request a
 * snapshot while a Bukkit event is updating counters.</p>
 */
public final class MatchRecord {

    private final UUID matchUuid;
    private final String serverId;
    private final String templateName;
    private final String runtimeArenaName;
    private final String arenaGroup;
    private final String timezone;
    private final Instant startedAt;
    private final MatchStats stats = new MatchStats();

    private long nextEventSequence;
    private int nextReportNumber;
    private String status = "RUNNING";
    private String winnerTeam;
    private String endReason;
    private Instant endedAt;
    private boolean finished;
    private MatchRecordSnapshot finalSnapshot;

    public MatchRecord(UUID matchUuid, String serverId, String templateName,
                       String runtimeArenaName, String arenaGroup, String timezone,
                       Instant startedAt) {
        this.matchUuid = Objects.requireNonNull(matchUuid, "matchUuid");
        this.serverId = requireText(serverId, "serverId");
        this.templateName = requireText(templateName, "templateName");
        this.runtimeArenaName = requireText(runtimeArenaName, "runtimeArenaName");
        this.arenaGroup = requireText(arenaGroup, "arenaGroup");
        this.timezone = requireText(timezone, "timezone");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    public UUID getMatchUuid() {
        return matchUuid;
    }

    public MatchStats getStats() {
        return stats;
    }

    public synchronized MatchEventSnapshot event(String eventType, @Nullable UUID actorUuid,
                                                   @Nullable UUID targetUuid, @Nullable String details,
                                                   Instant occurredAt) {
        if (finished) return null;
        return new MatchEventSnapshot(UUID.randomUUID(), matchUuid, ++nextEventSequence,
                Objects.requireNonNull(eventType, "eventType"), actorUuid, targetUuid,
                details, Objects.requireNonNull(occurredAt, "occurredAt"));
    }

    public synchronized MatchRecordSnapshot reportSnapshot(Instant capturedAt) {
        return snapshot("RUNNING", null, null, Objects.requireNonNull(capturedAt, "capturedAt"),
                ++nextReportNumber);
    }

    /** Snapshot used by the initial match row; report numbering starts at 1. */
    public synchronized MatchRecordSnapshot startSnapshot(Instant capturedAt) {
        return snapshot("RUNNING", null, null, Objects.requireNonNull(capturedAt, "capturedAt"), 0);
    }

    /**
     * Finish this record once. Repeated calls return the exact same immutable
     * snapshot, which makes queue retries idempotent.
     */
    public synchronized MatchRecordSnapshot finish(String endStatus, @Nullable String winnerTeam,
                                                    @Nullable String endReason, Instant endedAt) {
        if (finished) return finalSnapshot;
        this.status = Objects.requireNonNull(endStatus, "endStatus");
        this.winnerTeam = winnerTeam;
        this.endReason = endReason;
        this.endedAt = Objects.requireNonNull(endedAt, "endedAt");
        this.finished = true;
        this.finalSnapshot = snapshot(this.status, this.winnerTeam, this.endReason,
                this.endedAt, ++nextReportNumber);
        return finalSnapshot;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized MatchRecordSnapshot getFinalSnapshot() {
        return finalSnapshot;
    }

    private synchronized MatchRecordSnapshot snapshot(String state, @Nullable String winner,
                                                        @Nullable String reason, Instant capturedAt,
                                                        int reportNumber) {
        return new MatchRecordSnapshot(matchUuid, serverId, templateName, runtimeArenaName,
                arenaGroup, timezone, state, winner, reason, startedAt, endedAt,
                capturedAt, reportNumber, nextEventSequence, stats.snapshot());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}

/** An immutable event waiting to be inserted into bw_match_events. */
record MatchEventSnapshot(UUID eventId, UUID matchUuid, long sequence, String eventType,
                          @Nullable UUID actorUuid, @Nullable UUID targetUuid,
                          @Nullable String details, Instant occurredAt) {
}

/** Immutable match metadata plus a detached player statistics snapshot. */
record MatchRecordSnapshot(UUID matchUuid, String serverId, String templateName,
                           String runtimeArenaName, String arenaGroup, String timezone,
                           String status, @Nullable String winnerTeam, @Nullable String endReason,
                           Instant startedAt, @Nullable Instant endedAt, Instant capturedAt,
                           int reportNumber, long lastEventSequence,
                           MatchStatsSnapshot playerStats) {
}
