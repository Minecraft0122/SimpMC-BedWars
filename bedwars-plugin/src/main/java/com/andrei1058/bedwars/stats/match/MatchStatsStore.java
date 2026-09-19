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
import com.andrei1058.bedwars.api.stats.KillDeathRatio;
import com.andrei1058.bedwars.database.MySQL;

import java.math.BigDecimal;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLTransientException;
import java.sql.SQLRecoverableException;
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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.logging.Level;

/**
 * Asynchronous MySQL/SQLite writer for match-level statistics.
 *
 * <p>Lifecycle writes and bounded event batches own short transactions. No transaction is held
 * while a game is running, and the match number is allocated by the database's
 * auto-increment column. This keeps the start path independent from any
 * aggregate/player-statistics row locks.</p>
 */
public final class MatchStatsStore implements AutoCloseable {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_SCHEMA_ATTEMPTS = 5;
    private static final DateTimeFormatter MYSQL_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final MatchStatsDatabase database;
    private final MatchWriterConnection writerConnection;
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
    private volatile boolean acceptingCritical;
    private final MatchWriteJournal journal;
    private final Object pendingLock = new Object();
    private final Map<UUID, QueuedOperation> unconfirmed = new LinkedHashMap<>();
    private final Set<UUID> orderedPlayersBlocked = new HashSet<>();
    private final Set<UUID> deferredMatches = new HashSet<>();
    private final Object wakeup = new Object();
    private volatile boolean persistenceFailed;
    private volatile boolean shutdownDeadlineExceeded;

    public MatchStatsStore(MySQL database, ZoneId zone, String serverId,
                           int queueCapacity, int retryDelaySeconds) {
        this(database, zone, serverId, queueCapacity, retryDelaySeconds,
                List.of(10, 20, 50, 100));
    }

    public MatchStatsStore(MySQL database, ZoneId zone, String serverId,
                           int queueCapacity, int retryDelaySeconds,
                           List<Integer> warningThresholds) {
        this(MatchStatsDatabase.mysql(database), zone, serverId, queueCapacity, retryDelaySeconds, warningThresholds);
    }

    public MatchStatsStore(MatchStatsDatabase database, ZoneId zone, String serverId,
                           int queueCapacity, int retryDelaySeconds) {
        this(database, zone, serverId, queueCapacity, retryDelaySeconds, List.of(10, 20, 50, 100));
    }

    public MatchStatsStore(MatchStatsDatabase database, ZoneId zone, String serverId,
                           int queueCapacity, int retryDelaySeconds,
                           List<Integer> warningThresholds) {
        if (queueCapacity < 100) throw new IllegalArgumentException("queueCapacity must be at least 100");
        if (retryDelaySeconds < 1) throw new IllegalArgumentException("retryDelaySeconds must be positive");
        this.database = Objects.requireNonNull(database, "database");
        this.writerConnection = new MatchWriterConnection(database);
        this.zone = Objects.requireNonNull(zone, "zone");
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
        this.journal = new MatchWriteJournal(database.pendingWritesDirectory(), database.storageIdentity());
    }

    /** Start the writer; schema creation is deliberately off the server thread. */
    public synchronized void start() {
        if (acceptingCritical) return;
        if (executor.isShutdown()) throw new IllegalStateException("已关闭的统计写入器不能重新启动");
        running = true;
        acceptingCritical = true;
        executor.submit(this::runWorker);
    }

    public boolean enqueueStart(MatchRecordSnapshot snapshot) {
        return enqueueStart(snapshot, ignored -> { });
    }

    /** 编号回调在事务提交之后的数据库线程执行，禁止直接操作 Bukkit 对象。 */
    public boolean enqueueStart(MatchRecordSnapshot snapshot, LongConsumer callback) {
        Objects.requireNonNull(callback, "callback");
        long[] matchNumber = new long[1];
        return enqueueCritical(new QueuedOperation("start " + snapshot.matchUuid(),
                connection -> {
                    matchNumber[0] = writeStart(connection, snapshot);
                    return List.of();
                }, true, () -> callback.accept(matchNumber[0]), PendingMatchWrite.start(snapshot)));
    }

    public boolean enqueueEvent(MatchEventSnapshot event) {
        return enqueueCritical(new QueuedOperation("event " + event.eventId(),
                connection -> {
                    writeEvent(connection, event);
                    return List.of();
                }, true, () -> { }, PendingMatchWrite.event(event)));
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
        return enqueueCritical(finishOperation(snapshot, punishedPlayers));
    }

    /** 仅供关闭流程：队列已满时仍保留最终快照，由紧随其后的 close() 转存。 */
    boolean enqueueFinishForShutdown(MatchRecordSnapshot snapshot, Set<UUID> punishedPlayers) {
        QueuedOperation operation = finishOperation(snapshot, punishedPlayers);
        synchronized (pendingLock) {
            if (!acceptingCritical) return false;
            unconfirmed.put(operation.pending.operationId(), operation);
            criticalQueue.offer(operation);
            synchronized (wakeup) { wakeup.notifyAll(); }
            return true;
        }
    }

    private QueuedOperation finishOperation(MatchRecordSnapshot snapshot, Set<UUID> punishedPlayers) {
        List<UUID> resetPlayers = punishedPlayers == null ? List.of() : punishedPlayers.stream()
                .filter(Objects::nonNull).distinct().sorted().toList();
        return new QueuedOperation("finish " + snapshot.matchUuid(),
                connection -> writeFinish(connection, snapshot, resetPlayers), true,
                () -> { }, PendingMatchWrite.finish(snapshot, resetPlayers));
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
                }, true, () -> { }, PendingMatchWrite.reset(uuid, punishedAt)));
    }

    public int queuedOperations() {
        return queue.size() + criticalQueue.size();
    }

    private boolean enqueueCritical(QueuedOperation operation) {
        synchronized (pendingLock) {
            if (!acceptingCritical) {
                logQueueRejection(operation, "对局统计写入器未启动");
                return false;
            }
            if (criticalQueue.offer(operation)) {
                if (operation.pending != null) unconfirmed.put(operation.pending.operationId(), operation);
                synchronized (wakeup) { wakeup.notifyAll(); }
                return true;
            }
        }
        logQueueRejection(operation, "对局统计关键写入队列已满");
        return false;
    }

    private boolean enqueue(QueuedOperation operation) {
        if (!running) {
            logQueueRejection(operation, "对局统计写入器未启动");
            return false;
        }
        if (queue.offer(operation)) {
            synchronized (wakeup) { wakeup.notifyAll(); }
            return true;
        }
        logQueueRejection(operation, "对局统计写入队列已满");
        return false;
    }

    private void logQueueRejection(QueuedOperation operation, String reason) {
        if (BedWars.plugin != null) {
            BedWars.plugin.getLogger().warning(reason + "，已拒绝：" + operation.description);
        }
    }

    private void runWorker() {
        try {
        if (!initializeSchemaWithRetry()) {
            // 保留有界队列，允许关闭时生成的最终快照入队并统一转存。
            running = false;
            return;
        }
        while (!shutdownDeadlineExceeded && !persistenceFailed && (running || !criticalQueue.isEmpty() || !queue.isEmpty())) {
            try {
                QueuedOperation operation;
                synchronized (wakeup) {
                    operation = criticalQueue.poll();
                    if (operation == null) operation = queue.poll();
                    if (operation == null && running) wakeup.wait();
                }
                if (operation == null) continue;
                if (operation.pending != null && operation.pending.orderedPlayers().stream().anyMatch(orderedPlayersBlocked::contains)) {
                    defer(operation);
                    continue;
                }
                if (isEvent(operation)) executeEventBatch(operation);
                else executeWithRetry(operation);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        } finally {
            running = false;
            persistUnconfirmed();
            try { writerConnection.close(); }
            catch (SQLException exception) { logFailure("关闭对局写连接", exception, 1); }
        }
    }

    private boolean initializeSchemaWithRetry() {
        for (int attempt = 1; attempt <= MAX_SCHEMA_ATTEMPTS && running; attempt++) {
            try {
                writerConnection.withConnection(connection -> { createSchema(connection); return null; });
                replayPending();
                if (persistenceFailed) return false;
                writerConnection.withConnection(connection -> { recoverUnblockedStaleMatches(connection); return null; });
                return true;
            } catch (SQLException | IOException exception) {
                logFailure("创建对局统计表", exception, attempt);
                if (exception instanceof SQLException sql && !isRetryable(sql)) break;
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
        while (!shutdownDeadlineExceeded && (running || attempt == 0)) {
            attempt++;
            try {
                List<VlWarning> warnings = writerConnection.inTransaction(connection -> {
                    boolean execute = operation.pending == null || operation.pending.kind() != PendingMatchWrite.Kind.RESET
                            || insertResetReceipt(connection, operation.pending.operationId());
                    return execute ? operation.writer.write(connection) : List.of();
                });
                confirm(operation);
                logWarnings(warnings);
                try {
                    operation.afterCommit.run();
                } catch (RuntimeException exception) {
                    // 回调失败不能重新执行已经提交的结算。
                    logFailure("对局写入回调 " + operation.description, exception, attempt);
                }
                return;
            } catch (SQLException exception) {
                logFailure(operation.description, exception, attempt);
                if (operation.critical && !isRetryable(exception)) {
                    defer(operation);
                    return;
                }
                if (operation.critical) {
                    /* Lifecycle and event rows are idempotent. Keep retrying
                    * while the plugin is alive so a short MySQL outage
                     * cannot silently lose a match settlement. The bounded
                     * queue still prevents an outage from growing memory
                     * without limit. */
                    if (sleepBeforeRetry(Math.min(attempt, MAX_ATTEMPTS))) continue;
                    defer(operation);
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
            } catch (RuntimeException exception) {
                logFailure(operation.description, exception, attempt);
                if (operation.critical) defer(operation);
                return;
            }
        }
    }

    private static boolean isEvent(QueuedOperation operation) {
        return operation != null && operation.pending != null && operation.pending.kind() == PendingMatchWrite.Kind.EVENT;
    }

    private void executeEventBatch(QueuedOperation first) {
        List<QueuedOperation> batch = new ArrayList<>();
        batch.add(first);
        // 只合并已到达的相邻事件；不等待凑批，也不跨过开局/结算/重置的顺序边界。
        while (batch.size() < 64 && isEvent(criticalQueue.peek())) batch.add(criticalQueue.poll());
        if (batch.size() == 1) {
            executeWithRetry(first);
            return;
        }
        try {
            writerConnection.inTransaction(connection -> {
                writeEvents(connection, batch.stream().map(operation -> operation.pending.event()).toList());
                return null;
            });
            batch.forEach(this::confirm);
        } catch (SQLException | RuntimeException exception) {
            // 批处理已回滚，逐条重试才能隔离一条坏数据而保留其余事件。
            logFailure("批量写入对局事件，转为逐条重试", exception, 1);
            for (QueuedOperation operation : batch) {
                if (persistenceFailed || shutdownDeadlineExceeded) break;
                executeWithRetry(operation);
            }
        }
    }

    private static boolean isRetryable(SQLException exception) {
        if (exception instanceof SQLTransientException || exception instanceof SQLRecoverableException) return true;
        String state = exception.getSQLState();
        if (state != null && (state.startsWith("08") || state.startsWith("40") || state.startsWith("HYT"))) return true;
        return Set.of(5, 6, 8, 10, 13, 14, 1205, 1213, 2006, 2013).contains(exception.getErrorCode());
    }

    private boolean insertResetReceipt(Connection connection, UUID operationId) throws SQLException {
        String sql = (database.isSqlite() ? "INSERT OR IGNORE" : "INSERT IGNORE")
                + " INTO bw_match_write_receipts (operation_uuid) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, operationId.toString());
            return statement.executeUpdate() > 0;
        }
    }

    private void confirm(QueuedOperation operation) {
        if (operation.pending == null || shutdownDeadlineExceeded) return;
        synchronized (pendingLock) {
            unconfirmed.remove(operation.pending.operationId());
        }
        try { journal.remove(operation.pending.operationId()); }
        catch (IOException exception) { logFailure("删除已提交的对局待写记录", exception, 1); }
    }

    private void defer(QueuedOperation operation) {
        if (operation.pending == null) return;
        orderedPlayersBlocked.addAll(operation.pending.orderedPlayers());
        if (operation.pending.match() != null) deferredMatches.add(operation.pending.match().matchUuid());
        try {
            journal.save(operation.pending);
            synchronized (pendingLock) { unconfirmed.remove(operation.pending.operationId()); }
            if (BedWars.plugin != null) BedWars.plugin.getLogger().warning(
                    "已保留对局待写记录，修复数据库后会在下次启动重试：" + operation.description);
        } catch (IOException exception) {
            logFailure("保存对局待写记录 " + operation.description, exception, 1);
            // 不能继续消费后续关键操作，否则它们可能先于本操作落盘。
            persistenceFailed = true;
            running = false;
            synchronized (wakeup) { wakeup.notifyAll(); }
        }
    }

    private void persistUnconfirmed() {
        // shutdownNow 的中断不能让 FileChannel.force 抛 ClosedByInterruptException。
        boolean interrupted = Thread.interrupted();
        try {
        List<QueuedOperation> pending;
        synchronized (pendingLock) {
            pending = new ArrayList<>(unconfirmed.values());
        }
        for (QueuedOperation operation : pending) {
            if (operation.pending == null) continue;
            try { journal.save(operation.pending); }
            catch (IOException exception) {
                logFailure("保存未确认的对局写入 " + operation.description, exception, 1);
                persistenceFailed = true;
                break;
            }
        }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private void replayPending() throws IOException {
        for (PendingMatchWrite pending : journal.readAll()) {
            if (persistenceFailed || shutdownDeadlineExceeded) break;
            QueuedOperation operation = restoredOperation(pending);
            synchronized (pendingLock) { unconfirmed.putIfAbsent(pending.operationId(), operation); }
            if (pending.orderedPlayers().stream().anyMatch(orderedPlayersBlocked::contains)) defer(operation);
            else executeWithRetry(operation);
        }
    }

    private QueuedOperation restoredOperation(PendingMatchWrite pending) {
        SqlWriter writer = switch (pending.kind()) {
            case START -> connection -> { writeStart(connection, pending.match()); return List.of(); };
            case EVENT -> connection -> { writeEvent(connection, pending.event()); return List.of(); };
            case FINISH -> connection -> writeFinish(connection, pending.match(), pending.punishedPlayers());
            case RESET -> connection -> { resetPunishmentVl(connection, pending.resetPlayer(), pending.resetAt()); return List.of(); };
        };
        return new QueuedOperation("恢复 " + pending.kind() + ' ' + pending.operationId(), writer, true, () -> { }, pending);
    }

    private void recoverUnblockedStaleMatches(Connection connection) throws SQLException {
        if (deferredMatches.isEmpty()) {
            recoverStaleMatches(connection);
            return;
        }
        // 尚未回放成功的最终快照保留 RUNNING，避免恢复标记抢先封存该对局。
        String sql = "SELECT match_uuid, arena_timezone FROM bw_matches WHERE server_id=? AND status='RUNNING'";
        List<String[]> stale = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String match = result.getString(1);
                    if (deferredMatches.stream().noneMatch(id -> id.toString().equals(match))) {
                        stale.add(new String[]{match, result.getString(2)});
                    }
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("UPDATE bw_matches SET status='ABORTED', end_reason='SERVER_RESTART', ended_at=?, last_seen_at=? WHERE match_uuid=? AND status='RUNNING'")) {
            Instant recoveredAt = Instant.now();
            for (String[] row : stale) {
                String timestamp = sqlTime(recoveredAt, row[1]);
                statement.setString(1, timestamp); statement.setString(2, timestamp); statement.setString(3, row[0]);
                statement.addBatch();
            }
            statement.executeBatch();
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

    void createSchema(Connection connection) throws SQLException {
        if (database.isSqlite()) {
            createSqliteSchema(connection);
        } else {
            createMysqlSchema(connection);
        }
        migrateKd(connection);
        MatchHistoryIndex.initialize(connection, database.isSqlite());
        migrateViolationActivity(connection);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_write_receipts (operation_uuid VARCHAR(36) PRIMARY KEY)"
                    + (database.isSqlite() ? "" : " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));
        }
        createSummaryView(connection);
    }

    private void migrateViolationActivity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE bw_player_violation_totals ADD COLUMN last_activity_ms BIGINT NOT NULL DEFAULT 0");
        } catch (SQLException exception) {
            if (exception.getErrorCode() != 1060 && !"42S21".equals(exception.getSQLState())
                    && !(database.isSqlite() && exception.getMessage() != null
                    && exception.getMessage().contains("duplicate column name"))) throw exception;
        }
    }

    private void createMysqlSchema(Connection connection) throws SQLException {
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
                    "kd_ratio DECIMAL(20,4) NOT NULL DEFAULT 0, " +
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
        }
    }

    /** 视图只是额外的 SQL 查询入口；没有创建权限不影响明细保存和查询 API。 */
    private void createSummaryView(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try {
                if (database.isSqlite()) statement.executeUpdate("DROP VIEW IF EXISTS bw_player_match_summary");
                String sql = (database.isSqlite() ? "CREATE VIEW" : "CREATE OR REPLACE VIEW") + " bw_player_match_summary AS " +
                        "SELECT p.player_uuid AS player_uuid, MAX(p.player_name) AS player_name, " +
                        "COUNT(*) AS matches_played, SUM(p.normal_kills) AS normal_kills, " +
                        "SUM(p.final_kills) AS final_kills, SUM(p.normal_kills + p.final_kills) AS total_kills, " +
                        "SUM(p.deaths) AS deaths, SUM(p.beds_destroyed) AS beds_destroyed, " +
                        "ROUND(1.0 * SUM(p.normal_kills) / CASE WHEN SUM(p.deaths)=0 THEN 1 ELSE SUM(p.deaths) END, 4) AS kd_ratio, " +
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
                        "WHERE m.status='FINISHED' GROUP BY p.player_uuid";
                if (database.isSqlite()) sql = sql.replace("GREATEST(", "MAX(").replace("AS SIGNED", "AS INTEGER");
                statement.executeUpdate(sql);
            } catch (SQLException exception) {
                if (BedWars.plugin != null) {
                    BedWars.plugin.getLogger().log(Level.WARNING,
                            "无法创建玩家对局汇总视图 bw_player_match_summary；明细表仍可正常写入。", exception);
                }
            }
        }
    }

    private void createSqliteSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_matches ("
                    + "match_no INTEGER PRIMARY KEY AUTOINCREMENT, match_uuid TEXT NOT NULL UNIQUE,"
                    + "server_id TEXT NOT NULL, template_name TEXT NOT NULL, runtime_arena TEXT NOT NULL,"
                    + "arena_group TEXT NOT NULL, arena_timezone TEXT NOT NULL, status TEXT NOT NULL,"
                    + "winner_team TEXT, end_reason TEXT, started_at TEXT NOT NULL, ended_at TEXT,"
                    + "last_seen_at TEXT NOT NULL, last_event_sequence INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_players ("
                    + "match_uuid TEXT NOT NULL, player_uuid TEXT NOT NULL, player_name TEXT, team_id TEXT,"
                    + "normal_kills INTEGER NOT NULL DEFAULT 0, final_kills INTEGER NOT NULL DEFAULT 0,"
                    + "deaths INTEGER NOT NULL DEFAULT 0, beds_destroyed INTEGER NOT NULL DEFAULT 0,"
                    + "kd_ratio REAL NOT NULL DEFAULT 0, illegal_team_vl INTEGER NOT NULL DEFAULT 0,"
                    + "kill_boosting_vl INTEGER NOT NULL DEFAULT 0, evidence_adjustment INTEGER NOT NULL DEFAULT 0,"
                    + "effective_vl INTEGER NOT NULL DEFAULT 0, reconnects INTEGER NOT NULL DEFAULT 0,"
                    + "disconnects INTEGER NOT NULL DEFAULT 0, outcome TEXT NOT NULL DEFAULT 'UNKNOWN',"
                    + "vl_applied INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL,"
                    + "PRIMARY KEY (match_uuid, player_uuid))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_events ("
                    + "event_uuid TEXT PRIMARY KEY, match_uuid TEXT NOT NULL, event_sequence INTEGER NOT NULL,"
                    + "event_type TEXT NOT NULL, actor_uuid TEXT, target_uuid TEXT, details TEXT, occurred_at TEXT NOT NULL,"
                    + "UNIQUE (match_uuid, event_sequence))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_reports ("
                    + "report_id INTEGER PRIMARY KEY AUTOINCREMENT, match_uuid TEXT NOT NULL, report_number INTEGER NOT NULL,"
                    + "status TEXT NOT NULL, captured_at TEXT NOT NULL, player_count INTEGER NOT NULL,"
                    + "last_event_sequence INTEGER NOT NULL, UNIQUE (match_uuid, report_number))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_player_violation_totals ("
                    + "player_uuid TEXT PRIMARY KEY, crime_total_vl INTEGER NOT NULL DEFAULT 0,"
                    + "punishment_total_vl INTEGER NOT NULL DEFAULT 0, punishment_warning_mask INTEGER NOT NULL DEFAULT 0,"
                    + "last_punished_at TEXT, updated_at TEXT NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bw_match_players_player ON bw_match_players(player_uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bw_matches_status ON bw_matches(status, started_at)");
        }
    }

    private void migrateKd(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_schema (migration_id VARCHAR(64) PRIMARY KEY)"
                    + (database.isSqlite() ? "" : " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));
            try (ResultSet result = statement.executeQuery("SELECT migration_id FROM bw_match_schema WHERE migration_id='normal-kd-zero-deaths-v1'")) {
                if (result.next()) return;
            }
            if (!database.isSqlite()) {
                // 旧 DECIMAL(10,4) 无法容纳零死亡时的完整 INT 击杀数。
                statement.executeUpdate("ALTER TABLE bw_match_players MODIFY COLUMN kd_ratio DECIMAL(20,4) NULL");
            }
            connection.setAutoCommit(false);
            try {
                statement.executeUpdate("UPDATE bw_match_players SET kd_ratio=ROUND(1.0 * normal_kills / CASE WHEN deaths=0 THEN 1 ELSE deaths END, 4)");
                statement.executeUpdate(database.isSqlite()
                        ? "INSERT OR IGNORE INTO bw_match_schema (migration_id) VALUES ('normal-kd-zero-deaths-v1')"
                        : "INSERT IGNORE INTO bw_match_schema (migration_id) VALUES ('normal-kd-zero-deaths-v1')");
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** Mark rows left RUNNING by a previous process on this server as aborted. */
    void recoverStaleMatches(Connection connection) throws SQLException {
        List<String> timezones = new ArrayList<>();
        try (PreparedStatement statement = prepare(connection,
                "SELECT DISTINCT arena_timezone FROM bw_matches WHERE server_id=? AND status='RUNNING'")) {
            statement.setString(1, serverId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) timezones.add(result.getString(1));
            }
        }
        String sql = "UPDATE bw_matches SET status='ABORTED', end_reason='SERVER_RESTART', " +
                "ended_at=?, last_seen_at=? WHERE server_id=? AND status='RUNNING' AND arena_timezone=?";
        Instant recoveredAt = Instant.now();
        int recovered = 0;
        try (PreparedStatement statement = prepare(connection, sql)) {
            for (String timezone : timezones) {
                // 时区配置可能在重启时变化，历史行必须沿用自身保存的时区。
                String timestamp = sqlTime(recoveredAt, timezone);
                statement.setString(1, timestamp);
                statement.setString(2, timestamp);
                statement.setString(3, serverId);
                statement.setString(4, timezone);
                recovered += statement.executeUpdate();
            }
        }
        if (recovered > 0 && BedWars.plugin != null) {
            BedWars.plugin.getLogger().info("已将本子服上次异常退出遗留的 " + recovered + " 场对局标记为 ABORTED。");
        }
    }

    long writeStart(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        ensureMatch(connection, snapshot, "RUNNING");
        StoredMatch match = lockMatch(connection, snapshot.matchUuid());
        if ("RUNNING".equals(match.status())) {
            // 重试开局只补充缺失的参赛者，不覆盖已有的较新计数。
            writePlayers(connection, snapshot, true, match.number());
        }
        return match.number();
    }

    private static final String EVENT_SQL = "INSERT INTO bw_match_events (event_uuid, match_uuid, event_sequence, event_type, actor_uuid, target_uuid, details, occurred_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_uuid=VALUES(event_uuid)";

    void writeEvent(Connection connection, MatchEventSnapshot event) throws SQLException {
        try (PreparedStatement statement = prepare(connection, EVENT_SQL)) {
            bindEvent(statement, event);
            statement.executeUpdate();
        }
    }

    void writeEvents(Connection connection, List<MatchEventSnapshot> events) throws SQLException {
        try (PreparedStatement statement = prepare(connection, EVENT_SQL)) {
            for (MatchEventSnapshot event : events) {
                bindEvent(statement, event);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void bindEvent(PreparedStatement statement, MatchEventSnapshot event) throws SQLException {
        statement.setString(1, event.eventId().toString());
        statement.setString(2, event.matchUuid().toString());
        statement.setLong(3, event.sequence());
        statement.setString(4, event.eventType());
        statement.setString(5, uuid(event.actorUuid()));
        statement.setString(6, uuid(event.targetUuid()));
        statement.setString(7, event.details());
        statement.setString(8, sqlTime(event.occurredAt()));
    }

    void writeReport(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        ensureMatch(connection, snapshot, "RUNNING");
        StoredMatch match = lockMatch(connection, snapshot.matchUuid());
        // 关键队列可能先提交最终结算，普通队列中的旧快照必须保持只读。
        if (!"RUNNING".equals(match.status()) || snapshot.lastEventSequence() < match.sequence()) return;
        writePlayers(connection, snapshot, false, match.number());
        String sql = "INSERT INTO bw_match_reports (match_uuid, report_number, status, captured_at, player_count, last_event_sequence) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE captured_at=VALUES(captured_at), status=VALUES(status), "
                + "player_count=VALUES(player_count), last_event_sequence=VALUES(last_event_sequence)";
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, snapshot.matchUuid().toString());
            statement.setInt(2, snapshot.reportNumber());
            statement.setString(3, snapshot.status());
            statement.setString(4, sqlTime(snapshot.capturedAt(), snapshot.timezone()));
            statement.setInt(5, snapshot.playerStats().players().size());
            statement.setLong(6, snapshot.lastEventSequence());
            statement.executeUpdate();
        }
        touchMatch(connection, snapshot);
    }

    List<VlWarning> writeFinish(Connection connection, MatchRecordSnapshot snapshot,
                                       List<UUID> punishedPlayers) throws SQLException {
        ensureMatch(connection, snapshot, "RUNNING");
        StoredMatch match = lockMatch(connection, snapshot.matchUuid());
        if (!"RUNNING".equals(match.status())) return List.of();
        writePlayers(connection, snapshot, false, match.number());
        String sql = "UPDATE bw_matches SET status=?, winner_team=?, end_reason=?, ended_at=?, last_seen_at=?, "
                + "last_event_sequence=? WHERE match_uuid=? AND status='RUNNING'";
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, snapshot.status());
            statement.setString(2, snapshot.winnerTeam());
            statement.setString(3, snapshot.endReason());
            statement.setString(4, sqlTime(snapshot.endedAt() == null ? snapshot.capturedAt() : snapshot.endedAt(), snapshot.timezone()));
            statement.setString(5, sqlTime(snapshot.capturedAt(), snapshot.timezone()));
            statement.setLong(6, snapshot.lastEventSequence());
            statement.setString(7, snapshot.matchUuid().toString());
            statement.executeUpdate();
        }
        MatchHistoryIndex.recordFinish(connection, database.isSqlite(), snapshot);
        List<VlWarning> warnings = applyViolationTotals(connection, snapshot);
        for (UUID playerUuid : punishedPlayers) {
            resetPunishmentVl(connection, playerUuid, snapshot.capturedAt());
        }
        return warnings;
    }

    private void ensureMatch(Connection connection, MatchRecordSnapshot snapshot, String state) throws SQLException {
        String sql = "INSERT INTO bw_matches (match_uuid, server_id, template_name, runtime_arena, arena_group, "
                + "arena_timezone, status, started_at, last_seen_at, last_event_sequence) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE match_uuid=VALUES(match_uuid)";
        try (PreparedStatement statement = prepare(connection, sql)) {
            setMatchFields(statement, snapshot);
            statement.setString(7, state);
            statement.setString(8, sqlTime(snapshot.startedAt(), snapshot.timezone()));
            statement.setString(9, sqlTime(snapshot.capturedAt(), snapshot.timezone()));
            statement.setLong(10, snapshot.lastEventSequence());
            statement.executeUpdate();
        }
    }

    private StoredMatch lockMatch(Connection connection, UUID matchUuid) throws SQLException {
        String sql = "SELECT match_no, status, last_event_sequence FROM bw_matches WHERE match_uuid=? FOR UPDATE";
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, matchUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("无法读取对局 " + matchUuid);
                return new StoredMatch(result.getLong(1), result.getString(2), result.getLong(3));
            }
        }
    }

    private void touchMatch(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        String sql = "UPDATE bw_matches SET last_seen_at=?, last_event_sequence=GREATEST(last_event_sequence, ?) WHERE match_uuid=?";
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, sqlTime(snapshot.capturedAt(), snapshot.timezone()));
            statement.setLong(2, snapshot.lastEventSequence());
            statement.setString(3, snapshot.matchUuid().toString());
            statement.executeUpdate();
        }
    }

    /** 仅转换本类固定 SQL 中的方言差异，值始终由参数绑定。 */
    private PreparedStatement prepare(Connection connection, String sql) throws SQLException {
        if (database.isSqlite()) {
            sql = sql.replace("ON DUPLICATE KEY UPDATE", "ON CONFLICT DO UPDATE SET")
                    .replaceAll("VALUES\\(([a-z_]+)\\)", "excluded.$1")
                    .replace("GREATEST(", "MAX(")
                    .replace(" FOR UPDATE", "");
        }
        return connection.prepareStatement(sql);
    }

    private record StoredMatch(long number, String status, long sequence) { }

    private void setMatchFields(PreparedStatement statement, MatchRecordSnapshot snapshot) throws SQLException {
        statement.setString(1, snapshot.matchUuid().toString());
        statement.setString(2, snapshot.serverId());
        statement.setString(3, snapshot.templateName());
        statement.setString(4, snapshot.runtimeArenaName());
        statement.setString(5, snapshot.arenaGroup());
        statement.setString(6, snapshot.timezone());
    }

    private void writePlayers(Connection connection, MatchRecordSnapshot snapshot, boolean insertOnly, long matchNumber) throws SQLException {
        String sql = "INSERT INTO bw_match_players (match_uuid, player_uuid, player_name, team_id, normal_kills, final_kills, deaths, beds_destroyed, "
                + "kd_ratio, illegal_team_vl, kill_boosting_vl, evidence_adjustment, effective_vl, reconnects, disconnects, outcome, updated_at, match_no) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                + (insertOnly ? "player_uuid=VALUES(player_uuid), match_no=VALUES(match_no)"
                : "player_name=VALUES(player_name), team_id=VALUES(team_id), normal_kills=VALUES(normal_kills), "
                + "final_kills=VALUES(final_kills), deaths=VALUES(deaths), beds_destroyed=VALUES(beds_destroyed), kd_ratio=VALUES(kd_ratio), "
                + "illegal_team_vl=VALUES(illegal_team_vl), kill_boosting_vl=VALUES(kill_boosting_vl), evidence_adjustment=VALUES(evidence_adjustment), "
                + "effective_vl=VALUES(effective_vl), reconnects=VALUES(reconnects), "
                + "disconnects=VALUES(disconnects), outcome=VALUES(outcome), updated_at=VALUES(updated_at), match_no=VALUES(match_no)");
        try (PreparedStatement statement = prepare(connection, sql)) {
            for (MatchPlayerSnapshot player : snapshot.playerStats().players()) {
                statement.setString(1, snapshot.matchUuid().toString());
                statement.setString(2, player.playerUuid().toString());
                statement.setString(3, player.playerName());
                statement.setString(4, player.teamId());
                statement.setInt(5, player.kills());
                statement.setInt(6, player.finalKills());
                statement.setInt(7, player.deaths());
                statement.setInt(8, player.bedsDestroyed());
                statement.setBigDecimal(9, BigDecimal.valueOf(KillDeathRatio.calculate(player.kills(), player.deaths())));
                statement.setInt(10, player.illegalTeamVl());
                statement.setInt(11, player.killBoostingVl());
                statement.setInt(12, player.evidenceAdjustment());
                statement.setInt(13, player.totalVl());
                statement.setInt(14, player.reconnects());
                statement.setInt(15, player.disconnects());
                statement.setString(16, player.outcome().name());
                statement.setString(17, sqlTime(snapshot.capturedAt(), snapshot.timezone()));
                statement.setLong(18, matchNumber);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<VlWarning> applyViolationTotals(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        List<MatchPlayerSnapshot> players = snapshot.playerStats().players().stream()
                .filter(player -> player.rawVl() > 0 || player.totalVl() > 0)
                .sorted(Comparator.comparing(MatchPlayerSnapshot::playerUuid)).toList();
        List<VlWarning> warnings = new ArrayList<>();
        if (!players.isEmpty()) {
            // 父对局行已锁定；一次读取本局标记，避免每名玩家都往返数据库。
            Set<UUID> unapplied = new java.util.HashSet<>();
            try (PreparedStatement statement = prepare(connection,
                    "SELECT player_uuid FROM bw_match_players WHERE match_uuid=? AND vl_applied=0 FOR UPDATE")) {
                statement.setString(1, snapshot.matchUuid().toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) unapplied.add(UUID.fromString(result.getString(1)));
                }
            }
            String ensureSql = "INSERT INTO bw_player_violation_totals (player_uuid, crime_total_vl, punishment_total_vl, punishment_warning_mask, updated_at) VALUES (?, 0, 0, 0, ?) "
                    + "ON DUPLICATE KEY UPDATE player_uuid=VALUES(player_uuid)";
            String selectSql = "SELECT crime_total_vl, punishment_total_vl, punishment_warning_mask FROM bw_player_violation_totals WHERE player_uuid=? FOR UPDATE";
            String updateSql = "UPDATE bw_player_violation_totals SET crime_total_vl=?, punishment_total_vl=?, punishment_warning_mask=?, updated_at=?, last_activity_ms=GREATEST(last_activity_ms, ?) WHERE player_uuid=?";
            try (PreparedStatement ensure = prepare(connection, ensureSql);
                 PreparedStatement select = prepare(connection, selectSql);
                 PreparedStatement update = prepare(connection, updateSql)) {
                String now = sqlTime(snapshot.capturedAt(), snapshot.timezone());
                // 所有服都按 UUID 顺序锁定累计行，避免交叉结算时反向抢锁。
                for (MatchPlayerSnapshot player : players) {
                    if (!unapplied.contains(player.playerUuid())) continue;
                    ensure.setString(1, player.playerUuid().toString());
                    ensure.setString(2, now);
                    ensure.addBatch();
                }
                ensure.executeBatch();
                for (MatchPlayerSnapshot player : players) {
                    if (!unapplied.contains(player.playerUuid())) continue;
                    select.setString(1, player.playerUuid().toString());
                    int crimeTotal;
                    int punishmentTotal;
                    int warningMask;
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) throw new SQLException("无法锁定玩家 VL 汇总行 " + player.playerUuid());
                        crimeTotal = result.getInt(1);
                        punishmentTotal = result.getInt(2);
                        warningMask = result.getInt(3);
                    }
                    int newCrimeTotal = saturatingAdd(crimeTotal, player.rawVl());
                    int newPunishmentTotal = saturatingAdd(punishmentTotal, player.totalVl());
                    ViolationThresholdPolicy.Evaluation evaluation = ViolationThresholdPolicy.evaluate(
                            punishmentTotal, newPunishmentTotal, warningMask, warningThresholds);
                    for (int threshold : evaluation.crossedThresholds()) {
                        warnings.add(new VlWarning(player.playerUuid(), player.playerName(),
                                snapshot.matchUuid(), threshold, newPunishmentTotal));
                    }
                    update.setInt(1, newCrimeTotal);
                    update.setInt(2, newPunishmentTotal);
                    update.setInt(3, evaluation.warningMask());
                    update.setString(4, now);
                    update.setLong(5, snapshot.capturedAt().toEpochMilli());
                    update.setString(6, player.playerUuid().toString());
                    update.addBatch();
                }
                update.executeBatch();
            }
        }
        // 零违规对局只需这一次批量标记，不读取或锁定玩家累计表。
        try (PreparedStatement statement = prepare(connection,
                "UPDATE bw_match_players SET vl_applied=1 WHERE match_uuid=? AND vl_applied=0")) {
            statement.setString(1, snapshot.matchUuid().toString());
            statement.executeUpdate();
        }
        return warnings;
    }

    private void resetPunishmentVl(Connection connection, UUID playerUuid, Instant punishedAt) throws SQLException {
        String sql = "UPDATE bw_player_violation_totals SET punishment_total_vl=0, punishment_warning_mask=0, last_punished_at=?, updated_at=?, last_activity_ms=? " +
                "WHERE player_uuid=? AND last_activity_ms<=?";
        try (PreparedStatement statement = prepare(connection, sql)) {
            String timestamp = sqlTime(punishedAt);
            statement.setString(1, timestamp);
            statement.setString(2, timestamp);
            statement.setLong(3, punishedAt.toEpochMilli());
            statement.setString(4, playerUuid.toString());
            statement.setLong(5, punishedAt.toEpochMilli());
            int changed = statement.executeUpdate();
            if (changed == 0 && BedWars.plugin != null) {
                BedWars.plugin.getLogger().warning("未重置玩家 " + playerUuid
                        + " 的处罚累计：记录不存在，或处罚后已有新结算/重置，已保留较新的累计。");
            }
        }
    }

    private String sqlTime(Instant instant) {
        return sqlTime(instant, zone.getId());
    }

    private static String sqlTime(Instant instant, String timezone) {
        return MYSQL_DATETIME.format(LocalDateTime.ofInstant(instant, ZoneId.of(timezone)));
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
        close(15, TimeUnit.SECONDS);
    }

    synchronized void close(long timeout, TimeUnit unit) {
        if (!running && executor.isShutdown()) return;
        synchronized (pendingLock) { acceptingCritical = false; }
        running = false;
        synchronized (wakeup) { wakeup.notifyAll(); }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                shutdownDeadlineExceeded = true;
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            shutdownDeadlineExceeded = true;
            executor.shutdownNow();
        } finally {
            // 线程仍被驱动阻塞时也保存其未确认操作；RESET receipt 避免提交结果未知时重复清零。
            persistUnconfirmed();
        }
    }

    private record QueuedOperation(String description, SqlWriter writer, boolean critical, Runnable afterCommit,
                                   PendingMatchWrite pending) {
        private QueuedOperation(String description, SqlWriter writer, boolean critical) {
            this(description, writer, critical, () -> { }, null);
        }
    }

    private record VlWarning(UUID playerUuid, String playerName, UUID matchUuid,
                             int threshold, int newTotal) {
    }

    @FunctionalInterface
    private interface SqlWriter {
        List<VlWarning> write(Connection connection) throws SQLException;
    }
}
