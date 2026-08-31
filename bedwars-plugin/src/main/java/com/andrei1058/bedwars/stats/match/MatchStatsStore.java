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

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.database.MySQL;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Asynchronous MySQL writer for match-level statistics.
 *
 * <p>Every queued operation owns a short transaction. No transaction is held
 * while a game is running, and the match number is allocated by MySQL's
 * auto-increment column. This keeps the start path independent from any
 * aggregate/player-statistics row locks.</p>
 */
public final class MatchStatsStore implements AutoCloseable {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SCHEMA_ATTEMPTS = 5;
    private static final DateTimeFormatter MYSQL_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final MySQL database;
    private final ZoneId zone;
    private final String serverId;
    private final int retryDelaySeconds;
    private final List<Integer> warningThresholds;
    private final BlockingQueue<QueuedOperation> queue;
    /**
     * Match lifecycle and event operations use a separate bounded queue. It is
     * deliberately non-blocking for Bukkit threads: an unavailable database
     * may build a backlog, but the backlog has a hard memory limit and cannot
     * stall a new match from starting.
     */
    private final BlockingQueue<QueuedOperation> criticalQueue;
    private final ExecutorService executor;
    private volatile boolean running;

    public MatchStatsStore(MySQL database, ZoneId zone, String serverId,
                           int queueCapacity, int retryDelaySeconds) {
        this(database, zone, serverId, queueCapacity, retryDelaySeconds,
                List.of(10, 20, 50, 100));
    }

    public MatchStatsStore(MySQL database, ZoneId zone, String serverId,
                           int queueCapacity, int retryDelaySeconds,
                           List<Integer> warningThresholds) {
        if (queueCapacity < 100) throw new IllegalArgumentException("queueCapacity must be at least 100");
        if (retryDelaySeconds < 1) throw new IllegalArgumentException("retryDelaySeconds must be positive");
        this.database = database;
        this.zone = zone;
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.retryDelaySeconds = retryDelaySeconds;
        List<Integer> thresholds = new ArrayList<>();
        if (warningThresholds != null) {
            warningThresholds.stream().filter(value -> value != null && value > 0)
                    .distinct().sorted().forEach(thresholds::add);
        }
        this.warningThresholds = thresholds.isEmpty()
                ? List.of(10, 20, 50, 100) : Collections.unmodifiableList(thresholds);
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SimpMC-BedWars-MatchStats");
            thread.setDaemon(true);
            return thread;
        });
        this.criticalQueue = new ArrayBlockingQueue<>(queueCapacity);
    }

    /** Start the writer; schema creation is deliberately off the server thread. */
    public synchronized void start() {
        if (running) return;
        running = true;
        executor.submit(this::runWorker);
    }

    public boolean enqueueStart(MatchRecordSnapshot snapshot) {
        return enqueueCritical(new QueuedOperation("start " + snapshot.matchUuid(),
                connection -> {
                    writeStart(connection, snapshot);
                    return List.of();
                }, true));
    }

    public boolean enqueueEvent(MatchEventSnapshot event) {
        return enqueueCritical(new QueuedOperation("event " + event.eventId(),
                connection -> {
                    writeEvent(connection, event);
                    return List.of();
                }, true));
    }

    public boolean enqueueReport(MatchRecordSnapshot snapshot) {
        return enqueue(new QueuedOperation("report " + snapshot.matchUuid() + '/' + snapshot.reportNumber(),
                connection -> {
                    writeReport(connection, snapshot);
                    return List.of();
                }, false));
    }

    public boolean enqueueFinish(MatchRecordSnapshot snapshot) {
        return enqueueFinish(snapshot, Collections.emptySet());
    }

    /**
     * Finish a match and, for players punished in that match, clear only the
     * punishment accumulator after the final amount has been applied. Both
     * actions stay in the same transaction so a retry cannot lose or
     * prematurely reset a punishment record.
     */
    public boolean enqueueFinish(MatchRecordSnapshot snapshot, Set<UUID> punishedPlayers) {
        List<UUID> resetPlayers = punishedPlayers == null ? List.of() : punishedPlayers.stream()
                .filter(Objects::nonNull).distinct().sorted().toList();
        return enqueueCritical(new QueuedOperation("finish " + snapshot.matchUuid(),
                connection -> writeFinish(connection, snapshot, resetPlayers), true));
    }

    /**
     * Clear only the punishment counter after an external punishment has been
     * applied. The immutable crime counter is intentionally preserved.
     */
    public boolean enqueueResetPunishmentVl(UUID playerUuid) {
        UUID uuid = Objects.requireNonNull(playerUuid, "playerUuid");
        Instant punishedAt = Instant.now();
        return enqueueCritical(new QueuedOperation("reset punishment VL " + uuid,
                connection -> {
                    resetPunishmentVl(connection, uuid, punishedAt);
                    return List.of();
                }, true));
    }

    public int queuedOperations() {
        return queue.size() + criticalQueue.size();
    }

    private boolean enqueueCritical(QueuedOperation operation) {
        if (!running) {
            logQueueRejection(operation, "对局统计写入器未启动");
            return false;
        }
        if (criticalQueue.offer(operation)) return true;
        logQueueRejection(operation, "对局统计关键写入队列已满");
        return false;
    }

    private boolean enqueue(QueuedOperation operation) {
        if (!running) {
            logQueueRejection(operation, "对局统计写入器未启动");
            return false;
        }
        if (queue.offer(operation)) return true;
        logQueueRejection(operation, "对局统计写入队列已满");
        return false;
    }

    private void logQueueRejection(QueuedOperation operation, String reason) {
        if (BedWars.plugin != null) {
            BedWars.plugin.getLogger().warning(reason + "，已拒绝：" + operation.description);
        }
    }

    private void runWorker() {
        if (!initializeSchemaWithRetry()) {
            /* Keep the bounded queues intact for diagnostics or a later
             * explicit restart, while stopping producers from growing them
             * indefinitely after schema setup has failed. */
            running = false;
            return;
        }
        while (running || !criticalQueue.isEmpty() || !queue.isEmpty()) {
            try {
                QueuedOperation operation = criticalQueue.poll();
                if (operation == null) operation = queue.poll(500, TimeUnit.MILLISECONDS);
                if (operation == null) continue;
                executeWithRetry(operation);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean initializeSchemaWithRetry() {
        for (int attempt = 1; attempt <= MAX_SCHEMA_ATTEMPTS && running; attempt++) {
            try (Connection connection = database.openConnection()) {
                createSchema(connection);
                recoverStaleMatches(connection);
                return true;
            } catch (SQLException exception) {
                logFailure("创建对局统计表", exception, attempt);
                if (attempt < MAX_SCHEMA_ATTEMPTS && sleepBeforeRetry(attempt)) continue;
                break;
            }
        }
        if (running && BedWars.plugin != null) {
            BedWars.plugin.getLogger().warning(
                    "对局统计表初始化达到重试上限，已暂停统计写入；请修复数据库后重新启动统计写入器。");
        }
        return false;
    }

    private void executeWithRetry(QueuedOperation operation) {
        int attempt = 0;
        while (running || attempt == 0) {
            attempt++;
            try (Connection connection = database.openConnection()) {
                connection.setAutoCommit(false);
                List<VlWarning> warnings = operation.writer.write(connection);
                connection.commit();
                logWarnings(warnings);
                return;
            } catch (SQLException exception) {
                logFailure(operation.description, exception, attempt);
                if (operation.critical) {
                    /* Lifecycle and event rows are idempotent. Keep retrying
                    * while the plugin is alive so a short MySQL outage
                     * cannot silently lose a match settlement. The bounded
                     * queue still prevents an outage from growing memory
                     * without limit. */
                    if (sleepBeforeRetry(Math.min(attempt, MAX_ATTEMPTS))) continue;
                    if (BedWars.plugin != null) {
                        BedWars.plugin.getLogger().warning(
                                "关键对局统计写入因统计线程停止而中止，未确认已保存：" + operation.description);
                    }
                    return;
                }
                if (attempt < MAX_ATTEMPTS && sleepBeforeRetry(attempt)) continue;
                if (BedWars.plugin != null) {
                    BedWars.plugin.getLogger().warning("已放弃本次对局统计写入：" + operation.description);
                }
                return;
            }
        }
    }

    /** Warnings are intentionally emitted only after the transaction commits. */
    private void logWarnings(List<VlWarning> warnings) {
        if (warnings == null || warnings.isEmpty() || BedWars.plugin == null) return;
        for (VlWarning warning : warnings) {
            String player = warning.playerName() == null || warning.playerName().isBlank()
                    ? warning.playerUuid().toString() : warning.playerName() + " (" + warning.playerUuid() + ")";
            BedWars.plugin.getLogger().warning("[VL] 玩家 " + player + " 的处罚依据累计 VL 已超过 "
                    + warning.threshold() + "：本次结算累计值 " + warning.newTotal() + "，对局 "
                    + warning.matchUuid() + "。");
        }
    }

    private boolean sleepBeforeRetry(int attempt) {
        if (!running) return false;
        long delay = Math.min(60L, (long) retryDelaySeconds * (1L << Math.min(attempt - 1, 5)));
        try {
            Thread.sleep(delay * 1000L);
            return running;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_matches (" +
                    "match_uuid CHAR(36) NOT NULL, " +
                    "match_no BIGINT NOT NULL AUTO_INCREMENT, " +
                    "server_id VARCHAR(64) NOT NULL, " +
                    "template_name VARCHAR(128) NOT NULL, " +
                    "runtime_arena VARCHAR(128) NOT NULL, " +
                    "arena_group VARCHAR(64) NOT NULL, " +
                    "arena_timezone VARCHAR(64) NOT NULL, " +
                    "status VARCHAR(16) NOT NULL, " +
                    "winner_team VARCHAR(64) NULL, " +
                    "end_reason VARCHAR(64) NULL, " +
                    "started_at DATETIME(3) NOT NULL, " +
                    "ended_at DATETIME(3) NULL, " +
                    "last_seen_at DATETIME(3) NOT NULL, " +
                    "last_event_sequence BIGINT NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (match_uuid), " +
                    "UNIQUE KEY uq_bw_matches_match_no (match_no), " +
                    "KEY idx_bw_matches_status (status, started_at), " +
                    "KEY idx_bw_matches_server_status (server_id, status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_players (" +
                    "match_uuid CHAR(36) NOT NULL, " +
                    "player_uuid CHAR(36) NOT NULL, " +
                    "player_name VARCHAR(128) NULL, " +
                    "team_id VARCHAR(64) NULL, " +
                    "normal_kills INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "final_kills INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "deaths INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "beds_destroyed INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "kd_ratio DECIMAL(10,4) NULL, " +
                    "illegal_team_vl INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "kill_boosting_vl INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "evidence_adjustment INT NOT NULL DEFAULT 0, " +
                    "effective_vl INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "reconnects INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "disconnects INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "outcome VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN', " +
                    "vl_applied TINYINT(1) NOT NULL DEFAULT 0, " +
                    "updated_at DATETIME(3) NOT NULL, " +
                    "PRIMARY KEY (match_uuid, player_uuid), " +
                    "KEY idx_bw_match_players_player (player_uuid)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_events (" +
                    "event_uuid CHAR(36) NOT NULL, " +
                    "match_uuid CHAR(36) NOT NULL, " +
                    "event_sequence BIGINT NOT NULL, " +
                    "event_type VARCHAR(32) NOT NULL, " +
                    "actor_uuid CHAR(36) NULL, " +
                    "target_uuid CHAR(36) NULL, " +
                    "details VARCHAR(255) NULL, " +
                    "occurred_at DATETIME(3) NOT NULL, " +
                    "PRIMARY KEY (event_uuid), " +
                    "UNIQUE KEY uq_bw_match_events_sequence (match_uuid, event_sequence), " +
                    "KEY idx_bw_match_events_match_time (match_uuid, occurred_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_reports (" +
                    "report_id BIGINT NOT NULL AUTO_INCREMENT, " +
                    "match_uuid CHAR(36) NOT NULL, " +
                    "report_number INT UNSIGNED NOT NULL, " +
                    "status VARCHAR(16) NOT NULL, " +
                    "captured_at DATETIME(3) NOT NULL, " +
                    "player_count INT UNSIGNED NOT NULL, " +
                    "last_event_sequence BIGINT NOT NULL, " +
                    "PRIMARY KEY (report_id), " +
                    "UNIQUE KEY uq_bw_match_reports_number (match_uuid, report_number), " +
                    "KEY idx_bw_match_reports_captured (captured_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_player_violation_totals (" +
                    "player_uuid CHAR(36) NOT NULL, " +
                    "crime_total_vl INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "punishment_total_vl INT UNSIGNED NOT NULL DEFAULT 0, " +
                    "punishment_warning_mask TINYINT UNSIGNED NOT NULL DEFAULT 0, " +
                    "last_punished_at DATETIME(3) NULL, " +
                    "updated_at DATETIME(3) NOT NULL, " +
                    "PRIMARY KEY (player_uuid)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            /* Add columns introduced after the initial match-statistics
             * release without taking a long-lived application lock. */
            addColumnIfMissing(statement, "ALTER TABLE bw_match_players ADD COLUMN evidence_adjustment INT NOT NULL DEFAULT 0 AFTER kill_boosting_vl");
            addColumnIfMissing(statement, "ALTER TABLE bw_match_players ADD COLUMN effective_vl INT UNSIGNED NOT NULL DEFAULT 0 AFTER evidence_adjustment");
            addColumnIfMissing(statement, "ALTER TABLE bw_player_violation_totals ADD COLUMN punishment_warning_mask TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER punishment_total_vl");

            /*
             * A read-only aggregation surface for lobby dashboards and
             * administrative comparison commands. Restricted MySQL accounts
             * may create tables but not views, so a missing VIEW privilege is
             * logged without preventing match writes from starting.
             */
            try {
                statement.executeUpdate("CREATE OR REPLACE VIEW bw_player_match_summary AS " +
                        "SELECT p.player_uuid AS player_uuid, MAX(p.player_name) AS player_name, " +
                        "COUNT(*) AS matches_played, SUM(p.normal_kills) AS normal_kills, " +
                        "SUM(p.final_kills) AS final_kills, SUM(p.normal_kills + p.final_kills) AS total_kills, " +
                        "SUM(p.deaths) AS deaths, SUM(p.beds_destroyed) AS beds_destroyed, " +
                        "CASE WHEN SUM(p.deaths)=0 THEN NULL ELSE ROUND(SUM(p.normal_kills + p.final_kills) / SUM(p.deaths), 4) END AS kd_ratio, " +
                        "SUM(p.illegal_team_vl) AS illegal_team_vl, SUM(p.kill_boosting_vl) AS kill_boosting_vl, " +
                        "SUM(p.evidence_adjustment) AS evidence_adjustment, " +
                        "SUM(CASE WHEN p.effective_vl=0 AND (p.illegal_team_vl > 0 OR p.kill_boosting_vl > 0 OR p.evidence_adjustment <> 0) " +
                        "THEN GREATEST(0, CAST(p.illegal_team_vl AS SIGNED) + CAST(p.kill_boosting_vl AS SIGNED) + p.evidence_adjustment) " +
                        "ELSE p.effective_vl END) AS effective_vl, " +
                        "SUM(p.reconnects) AS reconnects, SUM(p.disconnects) AS disconnects, " +
                        "SUM(CASE WHEN p.outcome='WIN' THEN 1 ELSE 0 END) AS wins, " +
                        "SUM(CASE WHEN p.outcome='LOSS' THEN 1 ELSE 0 END) AS losses, " +
                        "SUM(CASE WHEN p.outcome='ABANDONED' THEN 1 ELSE 0 END) AS abandoned, " +
                        "SUM(CASE WHEN p.outcome='DISCONNECTED' THEN 1 ELSE 0 END) AS disconnected, " +
                        "COALESCE(MAX(v.crime_total_vl), 0) AS crime_total_vl, " +
                        "COALESCE(MAX(v.punishment_total_vl), 0) AS punishment_total_vl " +
                        "FROM bw_match_players p INNER JOIN bw_matches m ON m.match_uuid=p.match_uuid " +
                        "LEFT JOIN bw_player_violation_totals v ON v.player_uuid=p.player_uuid " +
                        "WHERE m.status='FINISHED' GROUP BY p.player_uuid");
            } catch (SQLException exception) {
                if (BedWars.plugin != null) {
                    BedWars.plugin.getLogger().log(Level.WARNING,
                            "无法创建玩家对局汇总视图 bw_player_match_summary；明细表仍可正常写入。", exception);
                }
            }
        }
    }

    /** Mark rows left RUNNING by a previous process on this server as aborted. */
    private void recoverStaleMatches(Connection connection) throws SQLException {
        String sql = "UPDATE bw_matches SET status='ABORTED', end_reason='SERVER_RESTART', " +
                "ended_at=?, last_seen_at=? WHERE server_id=? AND status='RUNNING'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String timestamp = sqlTime(Instant.now());
            statement.setString(1, timestamp);
            statement.setString(2, timestamp);
            statement.setString(3, serverId);
            int recovered = statement.executeUpdate();
            if (recovered > 0 && BedWars.plugin != null) {
                BedWars.plugin.getLogger().info("已将本子服上次异常退出遗留的 " + recovered + " 场对局标记为 ABORTED。");
            }
        }
    }

    private void writeStart(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        String sql = "INSERT INTO bw_matches (match_uuid, server_id, template_name, runtime_arena, arena_group, " +
                "arena_timezone, status, started_at, last_seen_at, last_event_sequence) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_seen_at=VALUES(last_seen_at), last_event_sequence=GREATEST(last_event_sequence, VALUES(last_event_sequence))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setMatchFields(statement, snapshot);
            statement.setString(7, "RUNNING");
            statement.setString(8, sqlTime(snapshot.startedAt()));
            statement.setString(9, sqlTime(snapshot.capturedAt()));
            statement.setLong(10, snapshot.lastEventSequence());
            statement.executeUpdate();
        }
    }

    private void writeEvent(Connection connection, MatchEventSnapshot event) throws SQLException {
        String sql = "INSERT INTO bw_match_events (event_uuid, match_uuid, event_sequence, event_type, actor_uuid, target_uuid, details, occurred_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_uuid=VALUES(event_uuid)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.eventId().toString());
            statement.setString(2, event.matchUuid().toString());
            statement.setLong(3, event.sequence());
            statement.setString(4, event.eventType());
            statement.setString(5, uuid(event.actorUuid()));
            statement.setString(6, uuid(event.targetUuid()));
            statement.setString(7, event.details());
            statement.setString(8, sqlTime(event.occurredAt()));
            statement.executeUpdate();
        }
    }

    private void writeReport(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        ensureMatch(connection, snapshot, "RUNNING");
        writePlayers(connection, snapshot);
        String sql = "INSERT INTO bw_match_reports (match_uuid, report_number, status, captured_at, player_count, last_event_sequence) " +
                "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE captured_at=VALUES(captured_at), status=VALUES(status), " +
                "player_count=VALUES(player_count), last_event_sequence=VALUES(last_event_sequence)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, snapshot.matchUuid().toString());
            statement.setInt(2, snapshot.reportNumber());
            statement.setString(3, snapshot.status());
            statement.setString(4, sqlTime(snapshot.capturedAt()));
            statement.setInt(5, snapshot.playerStats().players().size());
            statement.setLong(6, snapshot.lastEventSequence());
            statement.executeUpdate();
        }
        touchMatch(connection, snapshot);
    }

    private List<VlWarning> writeFinish(Connection connection, MatchRecordSnapshot snapshot,
                                        List<UUID> punishedPlayers) throws SQLException {
        // Insert a RUNNING row if the asynchronous start operation has not
        // completed yet; the following update then records the final state.
        ensureMatch(connection, snapshot, "RUNNING");
        writePlayers(connection, snapshot);

        String sql = "UPDATE bw_matches SET status=?, winner_team=?, end_reason=?, ended_at=?, last_seen_at=?, " +
                "last_event_sequence=? WHERE match_uuid=? AND status='RUNNING'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, snapshot.status());
            statement.setString(2, snapshot.winnerTeam());
            statement.setString(3, snapshot.endReason());
            statement.setString(4, sqlTime(snapshot.endedAt() == null ? snapshot.capturedAt() : snapshot.endedAt()));
            statement.setString(5, sqlTime(snapshot.capturedAt()));
            statement.setLong(6, snapshot.lastEventSequence());
            statement.setString(7, snapshot.matchUuid().toString());
            statement.executeUpdate();
        }

        List<VlWarning> warnings = applyViolationTotals(connection, snapshot);
        for (UUID playerUuid : punishedPlayers) {
            resetPunishmentVl(connection, playerUuid, snapshot.capturedAt());
        }
        return warnings;
    }

    private void ensureMatch(Connection connection, MatchRecordSnapshot snapshot, String state) throws SQLException {
        String sql = "INSERT INTO bw_matches (match_uuid, server_id, template_name, runtime_arena, arena_group, " +
                "arena_timezone, status, started_at, last_seen_at, last_event_sequence) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_seen_at=VALUES(last_seen_at), last_event_sequence=GREATEST(last_event_sequence, VALUES(last_event_sequence))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setMatchFields(statement, snapshot);
            statement.setString(7, state);
            statement.setString(8, sqlTime(snapshot.startedAt()));
            statement.setString(9, sqlTime(snapshot.capturedAt()));
            statement.setLong(10, snapshot.lastEventSequence());
            statement.executeUpdate();
        }
    }

    private void touchMatch(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        String sql = "UPDATE bw_matches SET last_seen_at=?, last_event_sequence=GREATEST(last_event_sequence, ?) WHERE match_uuid=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sqlTime(snapshot.capturedAt()));
            statement.setLong(2, snapshot.lastEventSequence());
            statement.setString(3, snapshot.matchUuid().toString());
            statement.executeUpdate();
        }
    }

    private void setMatchFields(PreparedStatement statement, MatchRecordSnapshot snapshot) throws SQLException {
        statement.setString(1, snapshot.matchUuid().toString());
        statement.setString(2, snapshot.serverId());
        statement.setString(3, snapshot.templateName());
        statement.setString(4, snapshot.runtimeArenaName());
        statement.setString(5, snapshot.arenaGroup());
        statement.setString(6, snapshot.timezone());
    }

    private void writePlayers(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        String sql = "INSERT INTO bw_match_players (match_uuid, player_uuid, player_name, team_id, normal_kills, final_kills, deaths, beds_destroyed, " +
                "kd_ratio, illegal_team_vl, kill_boosting_vl, evidence_adjustment, effective_vl, reconnects, disconnects, outcome, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), team_id=VALUES(team_id), normal_kills=VALUES(normal_kills), " +
                "final_kills=VALUES(final_kills), deaths=VALUES(deaths), beds_destroyed=VALUES(beds_destroyed), kd_ratio=VALUES(kd_ratio), " +
                "illegal_team_vl=VALUES(illegal_team_vl), kill_boosting_vl=VALUES(kill_boosting_vl), evidence_adjustment=VALUES(evidence_adjustment), " +
                "effective_vl=VALUES(effective_vl), reconnects=VALUES(reconnects), " +
                "disconnects=VALUES(disconnects), outcome=VALUES(outcome), updated_at=VALUES(updated_at)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (MatchPlayerSnapshot player : snapshot.playerStats().players()) {
                statement.setString(1, snapshot.matchUuid().toString());
                statement.setString(2, player.playerUuid().toString());
                statement.setString(3, player.playerName());
                statement.setString(4, player.teamId());
                statement.setInt(5, player.kills());
                statement.setInt(6, player.finalKills());
                statement.setInt(7, player.deaths());
                statement.setInt(8, player.bedsDestroyed());
                if (player.kdRatio().isPresent()) {
                    statement.setBigDecimal(9, BigDecimal.valueOf(player.kdRatio().getAsDouble()));
                } else {
                    statement.setNull(9, java.sql.Types.DECIMAL);
                }
                statement.setInt(10, player.illegalTeamVl());
                statement.setInt(11, player.killBoostingVl());
                statement.setInt(12, player.evidenceAdjustment());
                statement.setInt(13, player.totalVl());
                statement.setInt(14, player.reconnects());
                statement.setInt(15, player.disconnects());
                statement.setString(16, player.outcome().name());
                statement.setString(17, sqlTime(snapshot.capturedAt()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<VlWarning> applyViolationTotals(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        String select = "SELECT vl_applied FROM bw_match_players WHERE match_uuid=? AND player_uuid=? FOR UPDATE";
        String ensureTotals = "INSERT INTO bw_player_violation_totals (player_uuid, crime_total_vl, punishment_total_vl, punishment_warning_mask, updated_at) VALUES (?, 0, 0, 0, ?) " +
                "ON DUPLICATE KEY UPDATE player_uuid=VALUES(player_uuid)";
        String selectTotals = "SELECT crime_total_vl, punishment_total_vl, punishment_warning_mask FROM bw_player_violation_totals WHERE player_uuid=? FOR UPDATE";
        String updateTotals = "UPDATE bw_player_violation_totals SET crime_total_vl=?, punishment_total_vl=?, punishment_warning_mask=?, updated_at=? WHERE player_uuid=?";
        String mark = "UPDATE bw_match_players SET vl_applied=1 WHERE match_uuid=? AND player_uuid=?";
        List<MatchPlayerSnapshot> players = new ArrayList<>(snapshot.playerStats().players());
        players.sort(Comparator.comparing(MatchPlayerSnapshot::playerUuid));
        List<VlWarning> warnings = new ArrayList<>();

        for (MatchPlayerSnapshot player : players) {
            boolean applied;
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setString(1, snapshot.matchUuid().toString());
                statement.setString(2, player.playerUuid().toString());
                try (ResultSet result = statement.executeQuery()) {
                    applied = result.next() && result.getBoolean(1);
                }
            }
            if (applied) continue;

            int crimeAmount = player.rawVl();
            int punishmentAmount = player.totalVl();
            if (crimeAmount > 0 || punishmentAmount > 0) {
                String now = sqlTime(snapshot.capturedAt());
                try (PreparedStatement statement = connection.prepareStatement(ensureTotals)) {
                    statement.setString(1, player.playerUuid().toString());
                    statement.setString(2, now);
                    statement.executeUpdate();
                }
                int crimeTotal;
                int punishmentTotal;
                int warningMask;
                try (PreparedStatement statement = connection.prepareStatement(selectTotals)) {
                    statement.setString(1, player.playerUuid().toString());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("无法锁定玩家 VL 汇总行 " + player.playerUuid());
                        crimeTotal = result.getInt(1);
                        punishmentTotal = result.getInt(2);
                        warningMask = result.getInt(3);
                    }
                }
                int newCrimeTotal = saturatingAdd(crimeTotal, crimeAmount);
                int newPunishmentTotal = saturatingAdd(punishmentTotal, punishmentAmount);
                ViolationThresholdPolicy.Evaluation evaluation = ViolationThresholdPolicy.evaluate(
                        punishmentTotal, newPunishmentTotal, warningMask, warningThresholds);
                for (int threshold : evaluation.crossedThresholds()) {
                    warnings.add(new VlWarning(player.playerUuid(), player.playerName(),
                            snapshot.matchUuid(), threshold, newPunishmentTotal));
                }
                try (PreparedStatement statement = connection.prepareStatement(updateTotals)) {
                    statement.setInt(1, newCrimeTotal);
                    statement.setInt(2, newPunishmentTotal);
                    statement.setInt(3, evaluation.warningMask());
                    statement.setString(4, now);
                    statement.setString(5, player.playerUuid().toString());
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(mark)) {
                statement.setString(1, snapshot.matchUuid().toString());
                statement.setString(2, player.playerUuid().toString());
                statement.executeUpdate();
            }
        }
        return warnings;
    }

    private void resetPunishmentVl(Connection connection, UUID playerUuid, Instant punishedAt) throws SQLException {
        String sql = "UPDATE bw_player_violation_totals SET punishment_total_vl=0, punishment_warning_mask=0, last_punished_at=?, updated_at=? " +
                "WHERE player_uuid=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String timestamp = sqlTime(punishedAt);
            statement.setString(1, timestamp);
            statement.setString(2, timestamp);
            statement.setString(3, playerUuid.toString());
            statement.executeUpdate();
        }
    }

    private String sqlTime(Instant instant) {
        LocalDateTime local = LocalDateTime.ofInstant(instant, zone);
        return MYSQL_DATETIME.format(local);
    }

    private static int saturatingAdd(int current, int amount) {
        long result = (long) current + amount;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private static void addColumnIfMissing(Statement statement, String sql) throws SQLException {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            /* MySQL error 1060 / SQLState 42S21 means another startup (or a
             * previous plugin version) already created this column. */
            if (exception.getErrorCode() != 1060 && !"42S21".equals(exception.getSQLState())) {
                throw exception;
            }
        }
    }

    private static String uuid(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    private static void logFailure(String operation, Throwable exception, int attempt) {
        if (BedWars.plugin != null) {
            BedWars.plugin.getLogger().log(Level.WARNING,
                    "对局统计数据库操作失败（第 " + attempt + " 次）：" + operation, exception);
        }
    }

    @Override
    public synchronized void close() {
        if (!running && executor.isShutdown()) return;
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private record QueuedOperation(String description, SqlWriter writer, boolean critical) {
    }

    private record VlWarning(UUID playerUuid, String playerName, UUID matchUuid,
                             int threshold, int newTotal) {
    }

    @FunctionalInterface
    private interface SqlWriter {
        List<VlWarning> write(Connection connection) throws SQLException;
    }
}
