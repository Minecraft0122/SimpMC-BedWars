/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.discipline;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.database.MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Small, serialized MySQL store for cross-server discipline state.
 *
 * <p>Only one player row is locked for a strike transaction and the audit row
 * has a unique deduplication key. Consequently a retry cannot award a second
 * strike and no match-start transaction is held while a player is playing.</p>
 */
public final class DisciplineStore implements AutoCloseable {

    private static final DateTimeFormatter MYSQL_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final MySQL database;
    private final ZoneId zone;
    private final ExecutorService executor;
    private volatile CompletableFuture<Void> schemaReady = CompletableFuture.completedFuture(null);
    private volatile boolean running;

    public DisciplineStore(MySQL database, ZoneId zone) {
        this.database = Objects.requireNonNull(database, "database");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "SimpMC-BedWars-Discipline");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        schemaReady = CompletableFuture.runAsync(this::initializeSchema, executor);
    }

    public CompletableFuture<Status> load(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return afterSchema(() -> loadNow(playerUuid));
    }

    /**
     * Load a player while handling a Paper async pre-login callback. A failed
     * database read deliberately fails open; the local cache still protects a
     * player already seen on this node.
     */
    public Status loadBlocking(UUID playerUuid, long timeoutMillis) {
        try {
            return load(playerUuid).get(Math.max(100L, timeoutMillis), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            logFailure("加载玩家纪律状态", exception);
            return Status.empty(playerUuid);
        }
    }

    public CompletableFuture<PenaltyResult> record(UUID playerUuid,
                                                     UUID sourceMatchUuid,
                                                     DisciplinePolicy.Category category,
                                                     String reason,
                                                     DisciplinePolicy policy,
                                                     Instant issuedAt) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(issuedAt, "issuedAt");
        String normalizedReason = reason == null || reason.isBlank() ? category.name() : reason;
        final String safeReason = normalizedReason.length() > 128
                ? normalizedReason.substring(0, 128) : normalizedReason;
        String dedupeKey = dedupeKey(playerUuid, sourceMatchUuid, category, safeReason);
        return afterSchema(() -> recordNow(playerUuid, sourceMatchUuid, category, safeReason,
                dedupeKey, policy, issuedAt));
    }

    private <T> CompletableFuture<T> afterSchema(SqlSupplier<T> supplier) {
        if (!running) return CompletableFuture.failedFuture(new IllegalStateException("纪律存储未启动"));
        return schemaReady.handle((ignored, failure) -> null)
                .thenApplyAsync(ignored -> {
                    if (schemaReady.isCompletedExceptionally()) {
                        throw new IllegalStateException("纪律表尚未初始化", schemaFailure());
                    }
                    try {
                        return supplier.get();
                    } catch (SQLException exception) {
                        logFailure("纪律数据库操作", exception);
                        throw new DisciplineStoreException(exception);
                    }
                }, executor);
    }

    private Throwable schemaFailure() {
        try {
            schemaReady.join();
            return null;
        } catch (Exception exception) {
            return exception.getCause() == null ? exception : exception.getCause();
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_player_discipline (" +
                    "player_uuid CHAR(36) NOT NULL, " +
                    "afk_strikes INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "abandonment_strikes INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "violation_strikes INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "cooldown_until DATETIME(3) NULL, " +
                    "cooldown_category VARCHAR(16) NULL, " +
                    "cooldown_reason VARCHAR(128) NULL, " +
                    "updated_at DATETIME(3) NOT NULL, " +
                    "PRIMARY KEY (player_uuid), " +
                    "KEY idx_bw_player_discipline_cooldown (cooldown_until)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_discipline_penalties (" +
                    "penalty_id BIGINT NOT NULL AUTO_INCREMENT, " +
                    "player_uuid CHAR(36) NOT NULL, " +
                    "match_uuid CHAR(36) NULL, " +
                    "category VARCHAR(16) NOT NULL, " +
                    "occurrence INT UNSIGNED NOT NULL, " +
                    "cooldown_seconds INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "reason VARCHAR(128) NOT NULL, " +
                    "dedupe_key VARCHAR(191) NOT NULL, " +
                    "issued_at DATETIME(3) NOT NULL, " +
                    "expires_at DATETIME(3) NULL, " +
                    "PRIMARY KEY (penalty_id), " +
                    "UNIQUE KEY uq_bw_discipline_dedupe (dedupe_key), " +
                    "KEY idx_bw_discipline_player_time (player_uuid, issued_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (SQLException exception) {
            logFailure("创建纪律表", exception);
            throw new DisciplineStoreException(exception);
        }
    }

    private Status loadNow(UUID playerUuid) throws SQLException {
        String sql = "SELECT afk_strikes, abandonment_strikes, violation_strikes, cooldown_until, " +
                "cooldown_category, cooldown_reason, updated_at FROM bw_player_discipline WHERE player_uuid=?";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Status.empty(playerUuid);
                return new Status(playerUuid, result.getInt(1), result.getInt(2), result.getInt(3),
                        readInstant(result, 4), result.getString(5), result.getString(6),
                        readInstant(result, 7));
            }
        }
    }

    private PenaltyResult recordNow(UUID playerUuid, UUID sourceMatchUuid,
                                    DisciplinePolicy.Category category, String reason,
                                    String dedupeKey, DisciplinePolicy policy,
                                    Instant issuedAt) throws SQLException {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                ensurePlayerRow(connection, playerUuid, issuedAt);
                Status before = selectForUpdate(connection, playerUuid);

                String insert = "INSERT IGNORE INTO bw_discipline_penalties " +
                        "(player_uuid, match_uuid, category, occurrence, cooldown_seconds, reason, dedupe_key, issued_at) " +
                        "VALUES (?, ?, ?, 0, 0, ?, ?, ?)";
                boolean inserted;
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                    statement.setString(1, playerUuid.toString());
                    statement.setString(2, sourceMatchUuid == null ? null : sourceMatchUuid.toString());
                    statement.setString(3, category.name());
                    statement.setString(4, reason);
                    statement.setString(5, dedupeKey);
                    statement.setString(6, sqlTime(issuedAt));
                    inserted = statement.executeUpdate() > 0;
                }
                if (!inserted) {
                    connection.commit();
                    return new PenaltyResult(false, before, 0, policy.evaluate(category, occurrence(before, category)));
                }

                int occurrence = occurrence(before, category) + 1;
                DisciplinePolicy.Decision decision = policy.evaluate(category, occurrence);
                Instant existingUntil = before.cooldownUntil();
                Instant requestedUntil = decision.shouldPunish() ? decision.expiresAt(issuedAt) : existingUntil;
                boolean replaceCooldownMetadata = shouldReplaceCooldown(existingUntil, requestedUntil);
                Instant effectiveUntil = replaceCooldownMetadata ? requestedUntil : existingUntil;
                String effectiveCategory = replaceCooldownMetadata ? category.name() : before.cooldownCategory();
                String effectiveReason = replaceCooldownMetadata ? reason : before.cooldownReason();
                String update = "UPDATE bw_player_discipline SET afk_strikes=?, abandonment_strikes=?, " +
                        "violation_strikes=?, cooldown_until=?, cooldown_category=?, cooldown_reason=?, updated_at=? " +
                        "WHERE player_uuid=?";
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setInt(1, category == DisciplinePolicy.Category.AFK ? occurrence : before.afkStrikes());
                    statement.setInt(2, category == DisciplinePolicy.Category.ABANDONMENT ? occurrence : before.abandonmentStrikes());
                    statement.setInt(3, category == DisciplinePolicy.Category.VIOLATION ? occurrence : before.violationStrikes());
                    statement.setString(4, effectiveUntil == null ? null : sqlTime(effectiveUntil));
                    statement.setString(5, effectiveCategory);
                    statement.setString(6, effectiveReason);
                    statement.setString(7, sqlTime(issuedAt));
                    statement.setString(8, playerUuid.toString());
                    statement.executeUpdate();
                }
                String updatePenalty = "UPDATE bw_discipline_penalties SET occurrence=?, cooldown_seconds=?, expires_at=? WHERE dedupe_key=?";
                try (PreparedStatement statement = connection.prepareStatement(updatePenalty)) {
                    statement.setInt(1, occurrence);
                    statement.setLong(2, decision.cooldownSeconds());
                    statement.setString(3, decision.shouldPunish() ? sqlTime(decision.expiresAt(issuedAt)) : null);
                    statement.setString(4, dedupeKey);
                    statement.executeUpdate();
                }
                connection.commit();
                Status after = new Status(playerUuid,
                        category == DisciplinePolicy.Category.AFK ? occurrence : before.afkStrikes(),
                        category == DisciplinePolicy.Category.ABANDONMENT ? occurrence : before.abandonmentStrikes(),
                        category == DisciplinePolicy.Category.VIOLATION ? occurrence : before.violationStrikes(),
                        effectiveUntil, effectiveCategory, effectiveReason, issuedAt);
                return new PenaltyResult(true, after, occurrence, decision);
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        }
    }

    private void ensurePlayerRow(Connection connection, UUID playerUuid, Instant now) throws SQLException {
        String sql = "INSERT INTO bw_player_discipline (player_uuid, updated_at) VALUES (?, ?) ON DUPLICATE KEY UPDATE player_uuid=VALUES(player_uuid)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, sqlTime(now));
            statement.executeUpdate();
        }
    }

    private Status selectForUpdate(Connection connection, UUID playerUuid) throws SQLException {
        String sql = "SELECT afk_strikes, abandonment_strikes, violation_strikes, cooldown_until, cooldown_category, cooldown_reason, updated_at " +
                "FROM bw_player_discipline WHERE player_uuid=? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("纪律状态行不存在：" + playerUuid);
                return new Status(playerUuid, result.getInt(1), result.getInt(2), result.getInt(3),
                        readInstant(result, 4), result.getString(5), result.getString(6), readInstant(result, 7));
            }
        }
    }

    private Instant readInstant(ResultSet result, int index) throws SQLException {
        String text = result.getString(index);
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'),
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(zone).toInstant();
        } catch (RuntimeException ignored) {
            java.sql.Timestamp timestamp = result.getTimestamp(index);
            if (timestamp == null) return null;
            return timestamp.toInstant();
        }
    }

    private String sqlTime(Instant instant) {
        return MYSQL_DATETIME.format(LocalDateTime.ofInstant(instant, zone));
    }

    private static int occurrence(Status status, DisciplinePolicy.Category category) {
        return switch (category) {
            case AFK -> status.afkStrikes();
            case ABANDONMENT -> status.abandonmentStrikes();
            case VIOLATION -> status.violationStrikes();
        };
    }

    static boolean shouldReplaceCooldown(Instant existingUntil, Instant requestedUntil) {
        return requestedUntil != null
                && (existingUntil == null || requestedUntil.isAfter(existingUntil));
    }

    private static String dedupeKey(UUID playerUuid, UUID matchUuid,
                                    DisciplinePolicy.Category category, String reason) {
        String source = matchUuid == null ? "NO_MATCH" : matchUuid.toString();
        String key = playerUuid + ":" + source + ":" + category.name() + ":" + reason;
        return key.length() <= 191 ? key : key.substring(0, 191);
    }

    private void logFailure(String operation, Throwable exception) {
        if (BedWars.plugin != null) {
            BedWars.plugin.getLogger().log(Level.WARNING, operation + "失败；本次处罚将不会阻塞新对局。", exception);
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    public record Status(UUID playerUuid, int afkStrikes, int abandonmentStrikes, int violationStrikes,
                         Instant cooldownUntil, String cooldownCategory, String cooldownReason,
                         Instant updatedAt) {
        public static Status empty(UUID playerUuid) {
            return new Status(playerUuid, 0, 0, 0, null, null, null, null);
        }

        public boolean blockedAt(Instant now) {
            return cooldownUntil != null && now.isBefore(cooldownUntil);
        }

        public long remainingSeconds(Instant now) {
            if (!blockedAt(now)) return 0L;
            return Math.max(1L, java.time.Duration.between(now, cooldownUntil).toSeconds());
        }
    }

    public record PenaltyResult(boolean applied, Status status, int occurrence,
                                DisciplinePolicy.Decision decision) {
    }

    private static final class DisciplineStoreException extends RuntimeException {
        private DisciplineStoreException(Throwable cause) {
            super(cause);
        }
    }
}
