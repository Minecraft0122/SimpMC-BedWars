package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.api.stats.PlayerMatchTotals;
import com.andrei1058.bedwars.database.MySQL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 可选真实 MySQL 测试；未配置测试 URL 时跳过，不接触连接 URL 指向的业务数据库。 */
@Timeout(60)
class MySqlMatchHistoryIntegrationTest {
    private static final Instant START = Instant.parse("2026-09-19T04:00:00.123Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final List<MatchStatsStore> stores = new ArrayList<>();
    private final Properties credentials = new Properties();
    private String url;
    private String schema;
    private boolean schemaCreated;
    private MatchStatsDatabase database;

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        url = System.getenv("BEDWARS_TEST_MYSQL_URL");
        assumeTrue(url != null && !url.isBlank(), "未配置 BEDWARS_TEST_MYSQL_URL，跳过真实 MySQL 测试");
        String user = System.getenv("BEDWARS_TEST_MYSQL_USER");
        String password = System.getenv("BEDWARS_TEST_MYSQL_PASSWORD");
        if (user != null) credentials.setProperty("user", user);
        if (password != null) credentials.setProperty("password", password);
        credentials.setProperty("connectTimeout", "10000");
        credentials.setProperty("socketTimeout", "15000");
        schema = "bw_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(url, credentials);
             Statement statement = connection.createStatement()) {
            // 不使用 IF NOT EXISTS：仅成功新建的随机数据库才归本测试清理。
            statement.executeUpdate("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
            schemaCreated = true;
        }
        MySQL mysql = mock(MySQL.class);
        when(mysql.openConnection()).thenAnswer(ignored -> openTestConnection());
        when(mysql.storageIdentity()).thenReturn("mysql-test:" + schema);
        database = MatchStatsDatabase.mysql(mysql);
    }

    @AfterEach
    void dropOnlyTheSchemaCreatedByThisTest() throws Exception {
        try {
            for (MatchStatsStore store : stores) store.close();
        } finally {
            if (schemaCreated) {
                assertNotNull(schema);
                assertTrue(schema.matches("bw_test_[0-9a-f]{32}"), "拒绝清理非本测试生成的数据库名");
                try (Connection connection = DriverManager.getConnection(url, credentials);
                     Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP DATABASE `" + schema + "`");
                    schemaCreated = false;
                }
            }
        }
    }

    @Test
    void mysqlLifecyclePersistsNumbersTimesAndFinalCountersIdempotently() throws Exception {
        MatchStatsStore store = createStore("server-one");
        initialize(store);
        initialize(store);
        UUID player = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        MatchRecordSnapshot start = snapshot(match, player, "server-one", "RUNNING", 0, 0, 0, 0, 0);
        long number = transaction(connection -> store.writeStart(connection, start));
        MatchHistoryReader reader = new MatchHistoryReader(database);
        MatchInfo running = reader.findMatch(number).orElseThrow();
        assertTrue(number > 0);
        assertEquals(match, running.matchUuid());
        assertEquals(START, running.startedAt());
        assertNull(running.endedAt());
        assertEquals(0, reader.getMatchPlayers(match).getFirst().kills());

        transaction(connection -> {
            store.writeReport(connection, snapshot(match, player, "server-one", "RUNNING", 3, 1, 2, 1, 2));
            return null;
        });
        assertEquals(3, reader.getMatchPlayers(match).getFirst().kills());
        assertEquals(0, reader.getPlayerTotals(player).matchesPlayed());
        long retryNumber = transaction(connection -> store.writeStart(connection, start));
        assertEquals(number, retryNumber);
        assertEquals(3, reader.getMatchPlayers(match).getFirst().kills());

        MatchRecordSnapshot finished = snapshot(match, player, "server-one", "FINISHED", 7, 2, 0, 3, 10);
        finish(store, finished);
        transaction(connection -> {
            store.writeReport(connection, snapshot(match, player, "server-one", "RUNNING", 1, 0, 1, 0, 1));
            store.writeFinish(connection, snapshot(match, player, "server-one", "FINISHED", 99, 99, 99, 99, 99), List.of());
            return null;
        });
        MatchInfo ended = reader.findMatch(match).orElseThrow();
        assertEquals("FINISHED", ended.status());
        assertEquals(number, ended.matchNumber());
        assertEquals(START.plusSeconds(120), ended.endedAt());
        assertEquals(7, reader.getMatchPlayers(match).getFirst().kdRatio());
        assertEquals(new PlayerMatchTotals(player, 1, 7, 2, 0, 3), reader.getPlayerTotals(player));
        assertEquals(7, scalar("SELECT kd_ratio FROM bw_match_players WHERE match_uuid='" + match + "'"));
        assertEquals(3, scalar("SELECT crime_total_vl FROM bw_player_violation_totals WHERE player_uuid='" + player + "'"));
        assertEquals(2, scalar("SELECT punishment_total_vl FROM bw_player_violation_totals WHERE player_uuid='" + player + "'"));
        assertTrue(scalar("SELECT last_activity_ms FROM bw_player_violation_totals WHERE player_uuid='" + player + "'") > 0);

        UUID aborted = UUID.randomUUID();
        finish(store, snapshot(aborted, player, "server-one", "ABORTED", 50, 50, 50, 50, 11));
        assertEquals(new PlayerMatchTotals(player, 1, 7, 2, 0, 3), reader.getPlayerTotals(player));
        assertEquals(List.of(aborted, match), reader.getPlayerMatches(player, 10, 0).stream().map(MatchInfo::matchUuid).toList());
        assertEquals(match, reader.getPlayerMatches(player, 1, 1).getFirst().matchUuid());
        initialize(store);
        assertEquals(new PlayerMatchTotals(player, 1, 7, 2, 0, 3), reader.getPlayerTotals(player));
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP VIEW bw_player_match_summary");
        }
        assertEquals(7, reader.getPlayerTotals(player).kdRatio());
    }

    @Test
    void mysqlMigrationBackfillsLegacyRowsAndRepeatedStartupPreservesTotals() throws Exception {
        MatchStatsStore store = createStore("migration-server");
        initialize(store);
        UUID player = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        transaction(connection -> {
            try (PreparedStatement metadata = connection.prepareStatement(
                    "INSERT INTO bw_matches (match_uuid, server_id, template_name, runtime_arena, arena_group, arena_timezone, status, started_at, ended_at, last_seen_at) "
                            + "VALUES (?, 'old-server', 'template', 'template-1', 'Default', 'UTC', 'FINISHED', '2026-09-19 04:00:00.123', '2026-09-19 04:02:00.123', '2026-09-19 04:02:00.123')");
                 PreparedStatement result = connection.prepareStatement(
                         "INSERT INTO bw_match_players (match_uuid, player_uuid, player_name, team_id, normal_kills, final_kills, deaths, beds_destroyed, kd_ratio, outcome, updated_at) "
                             + "VALUES (?, ?, 'Legacy', 'red', 6, 4, 0, 2, 999, 'WIN', '2026-09-19 04:02:00.123')")) {
                metadata.setString(1, match.toString());
                metadata.executeUpdate();
                result.setString(1, match.toString());
                result.setString(2, player.toString());
                result.executeUpdate();
            }
            return null;
        });
        MatchHistoryReader reader = new MatchHistoryReader(database);
        assertEquals(new PlayerMatchTotals(player, 1, 6, 4, 0, 2), reader.getPlayerTotals(player));
        assertEquals(match, reader.getPlayerMatches(player, 10, 0).getFirst().matchUuid());
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE bw_match_players MODIFY COLUMN kd_ratio DECIMAL(10,4) NULL");
            statement.executeUpdate("UPDATE bw_match_players SET kd_ratio=NULL WHERE match_uuid='" + match + "'");
            statement.executeUpdate("DELETE FROM bw_match_schema WHERE migration_id='normal-kd-zero-deaths-v1'");
        }
        initialize(store);
        initialize(store);
        assertEquals(new PlayerMatchTotals(player, 1, 6, 4, 0, 2), reader.getPlayerTotals(player));
        assertEquals(6, scalar("SELECT kd_ratio FROM bw_match_players WHERE match_uuid='" + match + "'"));
        assertEquals(1, scalar("SELECT totals_applied FROM bw_match_players WHERE match_uuid='" + match + "'"));
        long number = reader.findMatch(match).orElseThrow().matchNumber();
        assertEquals(number, scalar("SELECT match_no FROM bw_match_players WHERE match_uuid='" + match + "'"));
        assertEquals(START, reader.findMatch(number).orElseThrow().startedAt());
        assertEquals(START.plusSeconds(120), reader.findMatch(number).orElseThrow().endedAt());
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT NUMERIC_PRECISION, NUMERIC_SCALE FROM information_schema.columns "
                        + "WHERE TABLE_SCHEMA=? AND TABLE_NAME='bw_match_players' AND COLUMN_NAME='kd_ratio'")) {
            statement.setString(1, schema);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(20, result.getInt(1));
                assertEquals(4, result.getInt(2));
            }
        }
    }

    @Test
    void twoStoresConcurrentlySettleDifferentMatchesForTheSamePlayer() throws Exception {
        MatchStatsStore firstStore = createStore("server-one");
        MatchStatsStore secondStore = createStore("server-two");
        initialize(firstStore);
        initialize(secondStore);
        UUID player = UUID.randomUUID();
        UUID firstMatch = UUID.randomUUID();
        UUID secondMatch = UUID.randomUUID();
        long firstNumber = transaction(connection -> firstStore.writeStart(connection,
                snapshot(firstMatch, player, "server-one", "RUNNING", 0, 0, 0, 0, 0)));
        long secondNumber = transaction(connection -> secondStore.writeStart(connection,
                snapshot(secondMatch, player, "server-two", "RUNNING", 0, 0, 0, 0, 0)));
        assertNotEquals(firstNumber, secondNumber);
        MatchRecordSnapshot first = snapshot(firstMatch, player, "server-one", "FINISHED", 4, 1, 2, 1, 8);
        MatchRecordSnapshot second = snapshot(secondMatch, player, "server-two", "FINISHED", 9, 3, 4, 2, 12);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch begin = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "BedWars-MySQL-integration");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> firstFuture = workers.submit(() -> finishTogether(firstStore, first, ready, begin));
            Future<?> secondFuture = workers.submit(() -> finishTogether(secondStore, second, ready, begin));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "两个数据库连接应及时就绪");
            begin.countDown();
            firstFuture.get(20, TimeUnit.SECONDS);
            secondFuture.get(20, TimeUnit.SECONDS);
        } finally {
            begin.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(20, TimeUnit.SECONDS), "并发测试连接必须在清理数据库前关闭");
        }
        MatchHistoryReader reader = new MatchHistoryReader(database);
        PlayerMatchTotals expected = new PlayerMatchTotals(player, 2, 13, 4, 6, 3);
        assertEquals(expected, reader.getPlayerTotals(player));
        assertEquals(13.0 / 6.0, reader.getPlayerTotals(player).kdRatio());
        assertEquals(List.of(secondMatch, firstMatch), reader.getPlayerMatches(player, 10, 0).stream().map(MatchInfo::matchUuid).toList());
        finish(firstStore, first);
        finish(secondStore, second);
        assertEquals(expected, reader.getPlayerTotals(player));
        assertEquals(6, scalar("SELECT crime_total_vl FROM bw_player_violation_totals WHERE player_uuid='" + player + "'"));
        assertEquals(4, scalar("SELECT punishment_total_vl FROM bw_player_violation_totals WHERE player_uuid='" + player + "'"));
    }

    private Void finishTogether(MatchStatsStore store, MatchRecordSnapshot snapshot,
                                CountDownLatch ready, CountDownLatch begin) throws Exception {
        return transaction(connection -> {
            ready.countDown();
            assertTrue(begin.await(10, TimeUnit.SECONDS), "并发结算同步超时");
            store.writeFinish(connection, snapshot, List.of());
            return null;
        });
    }

    private Connection openTestConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url, credentials);
        try {
            connection.setCatalog(schema);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET SESSION innodb_lock_wait_timeout=10");
            }
            return connection;
        } catch (SQLException failure) {
            try { connection.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    private MatchStatsStore createStore(String server) {
        MatchStatsStore store = new MatchStatsStore(database, ZONE, server, 100, 1);
        stores.add(store);
        return store;
    }

    private void initialize(MatchStatsStore store) throws SQLException {
        try (Connection connection = database.openConnection()) {
            store.createSchema(connection);
        }
    }

    private void finish(MatchStatsStore store, MatchRecordSnapshot snapshot) throws Exception {
        transaction(connection -> {
            store.writeFinish(connection, snapshot, List.of());
            return null;
        });
    }

    private <T> T transaction(Transaction<T> action) throws Exception {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                T value = action.run(connection);
                connection.commit();
                return value;
            } catch (Exception failure) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                throw failure;
            }
        }
    }

    private long scalar(String sql) throws SQLException {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player, String server, String status,
                                                int kills, int finalKills, int deaths, int beds, long sequence) {
        boolean finished = !"RUNNING".equals(status);
        MatchPlayerSnapshot counters = new MatchPlayerSnapshot(player, "MysqlPlayer", "red", kills, finalKills, deaths,
                beds, 2, 1, -1, 0, 0, finished ? MatchPlayerOutcome.WIN : MatchPlayerOutcome.UNKNOWN);
        return new MatchRecordSnapshot(match, server, "template", "template-1", "Default", ZONE.getId(), status,
                finished ? "red" : null, finished ? "WINNER" : null, START, finished ? START.plusSeconds(120) : null,
                START.plusSeconds(sequence), (int) sequence, sequence, new MatchStatsSnapshot(List.of(counters)));
    }

    @FunctionalInterface
    private interface Transaction<T> {
        T run(Connection connection) throws Exception;
    }
}
