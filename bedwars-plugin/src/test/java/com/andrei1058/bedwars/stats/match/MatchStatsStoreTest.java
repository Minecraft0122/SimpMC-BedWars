package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.api.stats.MatchPlayerResult;
import com.andrei1058.bedwars.api.stats.PlayerMatchTotals;
import com.andrei1058.bedwars.database.MySQL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchStatsStoreTest {
    @TempDir Path directory;
    private static final Instant START = Instant.parse("2026-09-19T04:00:00.123Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void persistsInitialPlayersAndUniqueNumbersAcrossReopening() throws Exception {
        UUID player = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        MatchStatsDatabase database = database();
        long firstNumber;
        try (MatchStatsStore store = initialized(database)) {
            firstNumber = transact(database, connection -> store.writeStart(connection, snapshot(match, player, "RUNNING", 0, 0, 0, 0, 0)));
            MatchHistoryReader reader = new MatchHistoryReader(database);
            assertEquals(0, reader.getMatchPlayers(match).getFirst().kills());
            assertEquals(0, reader.getMatchPlayers(match).getFirst().kdRatio());
            transact(database, connection -> store.writeStart(connection, snapshot(match, player, "RUNNING", 0, 0, 0, 0, 0)));
            assertEquals(firstNumber, reader.findMatch(match).orElseThrow().matchNumber());
            transact(database, connection -> {
                store.writeFinish(connection, snapshot(match, player, "FINISHED", 5, 2, 2, 1, 10), List.of());
                return null;
            });
        }
        MatchStatsDatabase reopened = MatchStatsDatabase.sqlite(directory.resolve("matches.db"));
        try (MatchStatsStore store = initialized(reopened)) {
            MatchHistoryReader reader = new MatchHistoryReader(reopened);
            MatchInfo info = reader.findMatch(firstNumber).orElseThrow();
            assertEquals(match, info.matchUuid());
            assertEquals(START, info.startedAt());
            assertEquals(START.plusSeconds(120), info.endedAt());
            assertEquals("FINISHED", info.status());
            MatchPlayerResult result = reader.getMatchPlayers(match).getFirst();
            assertEquals(5, result.kills());
            assertEquals(2, result.finalKills());
            assertEquals(1, result.bedsDestroyed());
            assertEquals(2.5, result.kdRatio());
            long secondNumber = transact(reopened, connection -> store.writeStart(connection,
                    snapshot(UUID.randomUUID(), player, "RUNNING", 0, 0, 0, 0, 0)));
            assertTrue(secondNumber > firstNumber);
            assertEquals(2, reader.getPlayerMatches(player, 100, 0).size());
            assertEquals(match, reader.getPlayerMatches(player, 1, 1).getFirst().matchUuid());
        }
    }

    @Test
    void reportsCanCreateMissingMatchAndStartRetriesDoNotEraseStats() throws Exception {
        MatchStatsDatabase database = database();
        UUID match = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        try (MatchStatsStore store = initialized(database)) {
            transact(database, connection -> {
                store.writeReport(connection, snapshot(match, player, "RUNNING", 4, 1, 2, 1, 7));
                return null;
            });
            transact(database, connection -> store.writeStart(connection,
                    snapshot(match, player, "RUNNING", 0, 0, 0, 0, 0)));
            MatchPlayerResult result = new MatchHistoryReader(database).getMatchPlayers(match).getFirst();
            assertEquals(4, result.kills());
            assertEquals(2, result.kdRatio());
        }
    }

    @Test
    void finalStatesRejectLateReportsAndConflictingRepeatedFinishes() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        try (MatchStatsStore store = initialized(database)) {
            for (String status : List.of("FINISHED", "ABORTED")) {
                UUID match = UUID.randomUUID();
                MatchRecordSnapshot finished = snapshot(match, player, status, 8, 2, 0, 2, 20);
                transact(database, connection -> { store.writeFinish(connection, finished, List.of()); return null; });
                transact(database, connection -> {
                    store.writeReport(connection, snapshot(match, player, "RUNNING", 1, 0, 1, 0, 1));
                    return null;
                });
                transact(database, connection -> {
                    store.writeFinish(connection, snapshot(match, player, "FINISHED", 99, 99, 99, 99, 99), List.of());
                    return null;
                });
                transact(database, connection -> store.writeStart(connection,
                        snapshot(match, player, "RUNNING", 0, 0, 0, 0, 0)));
                MatchHistoryReader reader = new MatchHistoryReader(database);
                assertEquals(status, reader.findMatch(match).orElseThrow().status());
                assertEquals(8, reader.getMatchPlayers(match).getFirst().kills());
                assertEquals(8, reader.getMatchPlayers(match).getFirst().kdRatio());
                assertEquals(8, scalar(database, "SELECT kd_ratio FROM bw_match_players WHERE match_uuid='" + match + "'"));
            }
        }
    }

    @Test
    void totalsUseSumOfCompletedMatchesAndWorkWithoutSummaryView() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        try (MatchStatsStore store = initialized(database)) {
            finish(database, store, snapshot(UUID.randomUUID(), player, "FINISHED", 2, 1, 1, 1, 3));
            finish(database, store, snapshot(UUID.randomUUID(), player, "FINISHED", 10, 2, 5, 2, 8));
            finish(database, store, snapshot(UUID.randomUUID(), player, "ABORTED", 100, 100, 100, 100, 20));
            transact(database, connection -> store.writeStart(connection,
                    snapshot(UUID.randomUUID(), player, "RUNNING", 200, 200, 200, 200, 30)));
            assertEquals(2, scalar(database, "SELECT kd_ratio FROM bw_player_match_summary WHERE player_uuid='" + player + "'"));
            try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP VIEW bw_player_match_summary");
            }
            MatchHistoryReader reader = new MatchHistoryReader(database);
            PlayerMatchTotals totals = reader.getPlayerTotals(player);
            assertEquals(2, totals.matchesPlayed());
            assertEquals(12, totals.kills());
            assertEquals(3, totals.finalKills());
            assertEquals(6, totals.deaths());
            assertEquals(3, totals.bedsDestroyed());
            assertEquals(2, totals.kdRatio());
            assertEquals(0, reader.getPlayerTotals(UUID.randomUUID()).kdRatio());
            assertTrue(reader.findMatch(UUID.randomUUID()).isEmpty());
            assertTrue(reader.findMatch(Long.MAX_VALUE).isEmpty());
            assertTrue(reader.getPlayerMatches(UUID.randomUUID(), 10, 0).isEmpty());
        }
    }

    @Test
    void zeroDeathTotalsEqualKillsAndSupportLargeCounters() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        try (MatchStatsStore store = initialized(database)) {
            finish(database, store, snapshot(UUID.randomUUID(), player, "FINISHED", Integer.MAX_VALUE, 3, 0, 1, 1));
            finish(database, store, snapshot(UUID.randomUUID(), player, "FINISHED", Integer.MAX_VALUE, 4, 0, 2, 2));
            PlayerMatchTotals totals = new MatchHistoryReader(database).getPlayerTotals(player);
            assertEquals(2L * Integer.MAX_VALUE, totals.kills());
            assertEquals((double) totals.kills(), totals.kdRatio());
            assertEquals((double) totals.kills(), scalar(database,
                    "SELECT kd_ratio FROM bw_player_match_summary WHERE player_uuid='" + player + "'"));
        }
    }

    @Test
    void sqliteViolationTotalsAndEventsRemainIdempotent() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        MatchPlayerSnapshot stats = new MatchPlayerSnapshot(player, "Alice", "red", 3, 1, 0, 1,
                12, 3, -2, 0, 0, MatchPlayerOutcome.WIN);
        MatchRecordSnapshot finished = withPlayers(snapshot(match, player, "FINISHED", 0, 0, 0, 0, 9), stats);
        try (MatchStatsStore store = initialized(database)) {
            finish(database, store, finished);
            finish(database, store, finished);
            assertEquals(15, scalar(database, "SELECT crime_total_vl FROM bw_player_violation_totals"));
            assertEquals(13, scalar(database, "SELECT punishment_total_vl FROM bw_player_violation_totals"));
            MatchEventSnapshot event = new MatchEventSnapshot(UUID.randomUUID(), match, 1, "BED_BREAK", player, null, null, START);
            transact(database, connection -> { store.writeEvent(connection, event); store.writeEvent(connection, event); return null; });
            assertEquals(1, scalar(database, "SELECT COUNT(*) FROM bw_match_events"));
            UUID next = UUID.randomUUID();
            MatchRecordSnapshot punished = withPlayers(snapshot(next, player, "FINISHED", 0, 0, 0, 0, 1), stats);
            transact(database, connection -> { store.writeFinish(connection, punished, List.of(player)); return null; });
            assertEquals(30, scalar(database, "SELECT crime_total_vl FROM bw_player_violation_totals"));
            assertEquals(0, scalar(database, "SELECT punishment_total_vl FROM bw_player_violation_totals"));
        }
    }

    @Test
    void migratesLegacyRatiosOnceAndReadsEachMatchTimezone() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        try (MatchStatsStore store = initialized(database)) {
            finish(database, store, snapshot(match, player, "FINISHED", 6, 3, 0, 1, 2));
            try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE bw_match_players SET kd_ratio=999");
                statement.executeUpdate("DELETE FROM bw_match_schema WHERE migration_id='normal-kd-zero-deaths-v1'");
                statement.executeUpdate("UPDATE bw_matches SET arena_timezone='UTC', started_at='2026-09-19 04:00:00.123', ended_at='2026-09-19 04:02:00.123'");
                store.createSchema(connection);
            }
            assertEquals(6, scalar(database, "SELECT kd_ratio FROM bw_match_players"));
            assertEquals(START, new MatchHistoryReader(database).findMatch(match).orElseThrow().startedAt());
            try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE bw_match_players SET kd_ratio=123");
                store.createSchema(connection);
            }
            assertEquals(123, scalar(database, "SELECT kd_ratio FROM bw_match_players"));
        }
    }

    @Test
    void callbackRunsOnlyAfterCommitAndQueuedLateReportsCannotOverwriteFinish() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        UUID match = UUID.randomUUID();
        CompletableFuture<Long> number = new CompletableFuture<>();
        try (MatchStatsStore store = new MatchStatsStore(database, ZONE, "test-server", 100, 1)) {
            store.start();
            assertTrue(store.enqueueStart(snapshot(match, player, "RUNNING", 0, 0, 0, 0, 0), value -> {
                try {
                    assertEquals(value, new MatchHistoryReader(database).findMatch(match).orElseThrow().matchNumber());
                    number.complete(value);
                } catch (Throwable exception) {
                    number.completeExceptionally(exception);
                }
            }));
            assertTrue(number.get(10, TimeUnit.SECONDS) > 0);
            assertTrue(store.enqueueFinish(snapshot(match, player, "FINISHED", 9, 3, 0, 1, 10)));
            assertTrue(store.enqueueReport(snapshot(match, player, "RUNNING", 1, 0, 1, 0, 1)));
        }
        assertEquals(9, new MatchHistoryReader(database).getMatchPlayers(match).getFirst().kills());
    }

    @Test
    void recoveryOnlyAbortsThisServersRunningMatchesAndUsesTheStoredTimezone() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        UUID own = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        try (MatchStatsStore store = initialized(database)) {
            transact(database, connection -> store.writeStart(connection, snapshot(own, player, "RUNNING", 0, 0, 0, 0, 0)));
            transact(database, connection -> store.writeStart(connection, snapshot(other, player, "RUNNING", 0, 0, 0, 0, 0)));
            try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(
                    "UPDATE bw_matches SET arena_timezone='UTC', started_at='2026-09-19 04:00:00.123' WHERE match_uuid=?")) {
                statement.setString(1, own.toString());
                statement.executeUpdate();
            }
            Instant beforeRecovery = Instant.now().minusMillis(1);
            try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(
                    "UPDATE bw_matches SET server_id='other-server' WHERE match_uuid=?")) {
                statement.setString(1, other.toString());
                statement.executeUpdate();
                store.recoverStaleMatches(connection);
            }
            MatchHistoryReader reader = new MatchHistoryReader(database);
            MatchInfo recovered = reader.findMatch(own).orElseThrow();
            assertEquals("ABORTED", recovered.status());
            assertEquals(START, recovered.startedAt());
            assertFalse(recovered.endedAt().isBefore(beforeRecovery));
            assertFalse(recovered.endedAt().isAfter(Instant.now()));
            assertEquals("RUNNING", reader.findMatch(other).orElseThrow().status());
        }
    }

    @Test
    void mysqlMigrationExpandsDecimalRepairsNullsAndToleratesMissingViewPermission() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet migrations = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(contains("bw_match_schema"))).thenReturn(migrations);
        when(migrations.next()).thenReturn(false);
        when(statement.executeUpdate(startsWith("CREATE OR REPLACE VIEW")))
                .thenThrow(new SQLException("CREATE VIEW command denied", "42000", 1142));
        try (MatchStatsStore store = new MatchStatsStore(mock(MySQL.class), ZONE, "test-server", 100, 1)) {
            assertDoesNotThrow(() -> store.createSchema(connection));
        }
        verify(statement).executeUpdate("ALTER TABLE bw_match_players MODIFY COLUMN kd_ratio DECIMAL(20,4) NULL");
        verify(statement).executeUpdate("UPDATE bw_match_players SET kd_ratio=ROUND(1.0 * normal_kills / CASE WHEN deaths=0 THEN 1 ELSE deaths END, 4)");
        verify(connection).commit();
    }

    private MatchStatsDatabase database() {
        return MatchStatsDatabase.sqlite(directory.resolve("matches.db"));
    }

    private MatchStatsStore initialized(MatchStatsDatabase database) throws SQLException {
        MatchStatsStore store = new MatchStatsStore(database, ZONE, "test-server", 100, 1);
        try (Connection connection = database.openConnection()) {
            store.createSchema(connection);
        }
        return store;
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player, String status,
                                                 int kills, int finalKills, int deaths, int beds, long sequence) {
        MatchPlayerSnapshot stats = new MatchPlayerSnapshot(player, "Alice", "red", kills, finalKills, deaths, beds,
                0, 0, 0, 0, 0, MatchPlayerOutcome.WIN);
        return new MatchRecordSnapshot(match, "test-server", "solo", "solo-1", "Default", ZONE.getId(), status,
                "RUNNING".equals(status) ? null : "red", "RUNNING".equals(status) ? null : "WINNER",
                START, "RUNNING".equals(status) ? null : START.plusSeconds(120), START.plusSeconds(sequence),
                1, sequence, new MatchStatsSnapshot(List.of(stats)));
    }

    private static MatchRecordSnapshot withPlayers(MatchRecordSnapshot source, MatchPlayerSnapshot player) {
        return new MatchRecordSnapshot(source.matchUuid(), source.serverId(), source.templateName(), source.runtimeArenaName(),
                source.arenaGroup(), source.timezone(), source.status(), source.winnerTeam(), source.endReason(),
                source.startedAt(), source.endedAt(), source.capturedAt(), source.reportNumber(), source.lastEventSequence(),
                new MatchStatsSnapshot(List.of(player)));
    }

    private static void finish(MatchStatsDatabase database, MatchStatsStore store, MatchRecordSnapshot snapshot) throws Exception {
        transact(database, connection -> { store.writeFinish(connection, snapshot, List.of()); return null; });
    }

    private static <T> T transact(MatchStatsDatabase database, Transaction<T> action) throws Exception {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = action.run(connection);
                connection.commit();
                return result;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static double scalar(MatchStatsDatabase database, String sql) throws SQLException {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getDouble(1);
        }
    }

    @FunctionalInterface
    private interface Transaction<T> {
        T run(Connection connection) throws Exception;
    }
}
