package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.stats.MatchInfo;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchHistoryIndexTest {
    @TempDir
    Path directory;

    @Test
    void migratesExistingRowsAndCanBeRetriedAfterAnInterruptedColumnMigration() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("matches.db"));
        UUID match = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE bw_matches (match_no INTEGER PRIMARY KEY AUTOINCREMENT, match_uuid TEXT UNIQUE NOT NULL, "
                    + "server_id TEXT NOT NULL, template_name TEXT NOT NULL, runtime_arena TEXT NOT NULL, arena_group TEXT NOT NULL, "
                    + "arena_timezone TEXT NOT NULL, status TEXT NOT NULL, winner_team TEXT, started_at TEXT NOT NULL, ended_at TEXT, "
                    + "last_seen_at TEXT NOT NULL, last_event_sequence INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE bw_match_players (match_uuid TEXT NOT NULL, player_uuid TEXT NOT NULL, player_name TEXT, "
                    + "team_id TEXT, normal_kills INTEGER NOT NULL, final_kills INTEGER NOT NULL, deaths INTEGER NOT NULL, "
                    + "beds_destroyed INTEGER NOT NULL, outcome TEXT NOT NULL, PRIMARY KEY(match_uuid, player_uuid))");
            statement.execute("INSERT INTO bw_matches(match_uuid, server_id, template_name, runtime_arena, arena_group, arena_timezone, status, started_at, last_seen_at) "
                    + "VALUES ('" + match + "','server','template','runtime','group','UTC','FINISHED','2026-01-01 00:00:00.000','2026-01-01 00:00:00.000')");
            statement.execute("INSERT INTO bw_match_players(match_uuid, player_uuid, player_name, team_id, normal_kills, final_kills, deaths, beds_destroyed, outcome) "
                    + "VALUES ('" + match + "','" + player + "','P','red',3,1,2,1,'WIN')");
            // 模拟上一进程在加列后退出、尚未回填和创建累计表。
            statement.executeUpdate("ALTER TABLE bw_match_players ADD COLUMN match_no INTEGER NULL");
        }
        try (Connection connection = database.openConnection()) {
            MatchHistoryIndex.initialize(connection, true);
            MatchHistoryIndex.initialize(connection, true);
        }
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT match_no FROM bw_match_players WHERE match_uuid='" + match + "'")) {
                assertTrue(result.next());
                assertEquals(1, result.getLong(1));
            }
            assertTrue(hasIndex(connection, MatchHistoryIndex.PLAYER_MATCH_INDEX));
        }
        assertEquals(1, new MatchHistoryReader(database).getPlayerTotals(player).matchesPlayed());
        assertEquals(3, new MatchHistoryReader(database).getPlayerTotals(player).kills());
        assertEquals(1, count(database, "SELECT matches_played FROM bw_player_match_totals"));
        assertEquals(3, count(database, "SELECT normal_kills FROM bw_player_match_totals"));
        assertEquals(1, count(database, "SELECT totals_applied FROM bw_match_players"));
    }

    @Test
    void historyUsesMatchNumberIndexAndReadsNullFallbackRows() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("matches.db"));
        MatchStatsStore store = new MatchStatsStore(database, ZoneId.of("UTC"), "server", 100, 1);
        try (Connection connection = database.openConnection()) {
            store.createSchema(connection);
            MatchHistoryIndex.initialize(connection, true);
        }
        UUID player = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        transact(database, connection -> {
            store.writeStart(connection, snapshot(first, player));
            store.writeStart(connection, snapshot(second, player));
            return null;
        });
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE bw_match_players SET match_no=NULL WHERE match_uuid='" + first + "'");
        }
        List<MatchInfo> matches = new MatchHistoryReader(database).getPlayerMatches(player, 100, 0);
        assertEquals(List.of(second, first), matches.stream().map(MatchInfo::matchUuid).toList());
    }

    @Test
    void onlyFinishedRowsEnterTotals() throws Exception {
        MatchStatsDatabase database = database("finished-only.db");
        createBaseTables(database);
        UUID player = UUID.randomUUID();
        insertMatch(database, "FINISHED", player, 4, 2, 1, 3);
        UUID aborted = insertMatch(database, "ABORTED", player, 99, 99, 99, 99);
        insertMatch(database, "RUNNING", player, 88, 88, 88, 88);
        initialize(database);

        var totals = new MatchHistoryReader(database).getPlayerTotals(player);
        assertEquals(1, totals.matchesPlayed());
        assertEquals(4, totals.kills());
        assertEquals(2, totals.finalKills());
        assertEquals(1, totals.deaths());
        assertEquals(3, totals.bedsDestroyed());
        assertEquals(1, count(database, "SELECT matches_played FROM bw_player_match_totals"));
        assertEquals(4, count(database, "SELECT normal_kills FROM bw_player_match_totals"));
        assertEquals(0, count(database, "SELECT totals_applied FROM bw_match_players WHERE match_uuid='" + aborted + "'"));
    }

    @Test
    void finishTotalsAreRolledBackAndCanBeRetriedWithoutDoubleCounting() throws Exception {
        MatchStatsDatabase database = database("rollback.db");
        createBaseTables(database);
        UUID player = UUID.randomUUID();
        UUID match = insertMatch(database, "RUNNING", player, 0, 0, 0, 0);
        initialize(database);
        MatchRecordSnapshot snapshot = snapshot(match, player, "FINISHED", 7, 2, 3, 1);

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            settle(connection, snapshot);
            connection.rollback();
        }
        assertEquals(0, count(database, "SELECT COUNT(*) FROM " + MatchHistoryIndex.TOTALS_TABLE
                + " WHERE player_uuid='" + player + "'"));
        assertEquals(0, count(database, "SELECT totals_applied FROM bw_match_players"));
        assertEquals(0, count(database, "SELECT normal_kills FROM bw_match_players"));
        assertEquals(0, new MatchHistoryReader(database).getPlayerTotals(player).matchesPlayed());
        assertEquals("RUNNING", new MatchHistoryReader(database).findMatch(match).orElseThrow().status());

        transact(database, connection -> {
            settle(connection, snapshot);
            return null;
        });
        transact(database, connection -> {
            MatchHistoryIndex.recordFinish(connection, true, snapshot);
            return null;
        });
        var totals = new MatchHistoryReader(database).getPlayerTotals(player);
        assertEquals(1, totals.matchesPlayed());
        assertEquals(7, totals.kills());
        assertEquals(2, totals.finalKills());
        assertEquals(3, totals.deaths());
        assertEquals(1, totals.bedsDestroyed());
        assertEquals(1, count(database, "SELECT totals_applied FROM bw_match_players"));
        assertEquals(7, count(database, "SELECT normal_kills FROM bw_player_match_totals"));
    }

    @Test
    void oldServerFinishedRowsRemainVisibleAsPendingFallback() throws Exception {
        MatchStatsDatabase database = database("pending-fallback.db");
        createBaseTables(database);
        UUID player = UUID.randomUUID();
        insertMatch(database, "FINISHED", player, 2, 1, 1, 1);
        initialize(database);

        // 旧版 INSERT 不含新列，数据库默认值必须自动保留这条待累计明细。
        UUID oldMatch = insertMatch(database, "FINISHED", player, 5, 3, 2, 4);
        insertMatch(database, "ABORTED", player, 99, 99, 99, 99);
        insertMatch(database, "RUNNING", player, 88, 88, 88, 88);
        assertEquals(0, count(database, "SELECT totals_applied FROM bw_match_players WHERE match_uuid='" + oldMatch + "'"));
        assertEquals(1, count(database, "SELECT COUNT(*) FROM bw_match_players WHERE match_uuid='" + oldMatch + "' AND match_no IS NULL"));
        assertEquals(2, count(database, "SELECT normal_kills FROM bw_player_match_totals"));
        var totals = new MatchHistoryReader(database).getPlayerTotals(player);
        assertEquals(2, totals.matchesPlayed());
        assertEquals(7, totals.kills());
        assertEquals(4, totals.finalKills());
        assertEquals(3, totals.deaths());
        assertEquals(5, totals.bedsDestroyed());

        // Startup migration must consume the pending row exactly once later.
        initialize(database);
        assertEquals(2, new MatchHistoryReader(database).getPlayerTotals(player).matchesPlayed());
        assertEquals(7, new MatchHistoryReader(database).getPlayerTotals(player).kills());
        assertEquals(7, count(database, "SELECT normal_kills FROM bw_player_match_totals"));
        assertEquals(1, count(database, "SELECT totals_applied FROM bw_match_players WHERE match_uuid='" + oldMatch + "'"));
        assertEquals(0, new MatchHistoryReader(database).getPlayerTotals(UUID.randomUUID()).matchesPlayed());
    }

    @Test
    void failedBackfillLeavesTheMatchPendingAndRetryAddsItExactlyOnce() throws Exception {
        MatchStatsDatabase database = database("interrupted-backfill.db");
        createBaseTables(database);
        initialize(database);
        UUID player = UUID.randomUUID();
        insertMatch(database, "FINISHED", player, 11, 2, 0, 3);
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TRIGGER reject_test_totals BEFORE INSERT ON bw_player_match_totals "
                    + "BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END");
            assertThrows(SQLException.class, () -> MatchHistoryIndex.initialize(connection, true));
            assertTrue(connection.getAutoCommit());
        }
        assertEquals(0, count(database, "SELECT COUNT(*) FROM bw_player_match_totals"));
        assertEquals(0, count(database, "SELECT totals_applied FROM bw_match_players"));
        assertEquals(11, new MatchHistoryReader(database).getPlayerTotals(player).kills());
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TRIGGER reject_test_totals");
        }
        initialize(database);
        assertEquals(1, count(database, "SELECT matches_played FROM bw_player_match_totals"));
        assertEquals(11, count(database, "SELECT normal_kills FROM bw_player_match_totals"));
        assertEquals(1, count(database, "SELECT totals_applied FROM bw_match_players"));
        assertEquals(11, new MatchHistoryReader(database).getPlayerTotals(player).kdRatio());
    }

    private static boolean hasIndex(Connection connection, String name) throws SQLException {
        try (ResultSet result = connection.getMetaData().getIndexInfo(null, null, "bw_match_players", false, false)) {
            while (result.next()) if (name.equalsIgnoreCase(result.getString("INDEX_NAME"))) return true;
        }
        return false;
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player) {
        return snapshot(match, player, "RUNNING", 0, 0, 0, 0);
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player, String status,
                                                int kills, int finalKills, int deaths, int beds) {
        MatchPlayerSnapshot stats = new MatchPlayerSnapshot(player, "P", "red", kills, finalKills, deaths, beds,
                0, 0, 0, 0, 0, MatchPlayerOutcome.UNKNOWN);
        Instant started = Instant.parse("2026-01-01T00:00:00Z");
        return new MatchRecordSnapshot(match, "server", "template", "runtime", "group", "UTC", status,
                "FINISHED".equals(status) ? "red" : null, "FINISHED".equals(status) ? "WINNER" : null,
                started, "FINISHED".equals(status) ? started.plusSeconds(10) : null,
                started.plusSeconds(10), 1, 1, new MatchStatsSnapshot(List.of(stats)));
    }

    private MatchStatsDatabase database(String file) {
        return MatchStatsDatabase.sqlite(directory.resolve(file));
    }

    private static void initialize(MatchStatsDatabase database) throws SQLException {
        try (Connection connection = database.openConnection()) {
            MatchHistoryIndex.initialize(connection, true);
            MatchHistoryIndex.initialize(connection, true);
        }
    }

    private static void createBaseTables(MatchStatsDatabase database) throws SQLException {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE bw_matches (match_no INTEGER PRIMARY KEY AUTOINCREMENT, match_uuid TEXT UNIQUE NOT NULL, "
                    + "server_id TEXT NOT NULL, template_name TEXT NOT NULL, runtime_arena TEXT NOT NULL, arena_group TEXT NOT NULL, "
                    + "arena_timezone TEXT NOT NULL, status TEXT NOT NULL, winner_team TEXT, started_at TEXT NOT NULL, ended_at TEXT, "
                    + "last_seen_at TEXT NOT NULL, last_event_sequence INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE bw_match_players (match_uuid TEXT NOT NULL, player_uuid TEXT NOT NULL, player_name TEXT, "
                    + "team_id TEXT, normal_kills INTEGER NOT NULL, final_kills INTEGER NOT NULL, deaths INTEGER NOT NULL, "
                    + "beds_destroyed INTEGER NOT NULL, outcome TEXT NOT NULL, PRIMARY KEY(match_uuid, player_uuid))");
        }
    }

    private static UUID insertMatch(MatchStatsDatabase database, String status, UUID player,
                                     int kills, int finalKills, int deaths, int beds) throws SQLException {
        UUID match = UUID.randomUUID();
        try (Connection connection = database.openConnection();
             PreparedStatement matchStatement = connection.prepareStatement(
                     "INSERT INTO bw_matches(match_uuid, server_id, template_name, runtime_arena, arena_group, arena_timezone, status, winner_team, started_at, ended_at, last_seen_at) VALUES (?, 'server', 'template', 'runtime', 'group', 'UTC', ?, 'red', '2026-01-01 00:00:00.000', '2026-01-01 00:00:10.000', '2026-01-01 00:00:10.000')");
             PreparedStatement playerStatement = connection.prepareStatement(
                     "INSERT INTO bw_match_players(match_uuid, player_uuid, player_name, team_id, normal_kills, final_kills, deaths, beds_destroyed, outcome) VALUES (?, ?, 'P', 'red', ?, ?, ?, ?, 'WIN')")) {
            matchStatement.setString(1, match.toString());
            matchStatement.setString(2, status);
            matchStatement.executeUpdate();
            playerStatement.setString(1, match.toString());
            playerStatement.setString(2, player.toString());
            playerStatement.setInt(3, kills);
            playerStatement.setInt(4, finalKills);
            playerStatement.setInt(5, deaths);
            playerStatement.setInt(6, beds);
            playerStatement.executeUpdate();
        }
        return match;
    }

    private static void settle(Connection connection, MatchRecordSnapshot snapshot) throws SQLException {
        MatchPlayerSnapshot player = snapshot.playerStats().players().getFirst();
        try (PreparedStatement players = connection.prepareStatement(
                "UPDATE bw_match_players SET normal_kills=?, final_kills=?, deaths=?, beds_destroyed=? WHERE match_uuid=?");
             PreparedStatement match = connection.prepareStatement("UPDATE bw_matches SET status='FINISHED' WHERE match_uuid=?")) {
            players.setInt(1, player.kills());
            players.setInt(2, player.finalKills());
            players.setInt(3, player.deaths());
            players.setInt(4, player.bedsDestroyed());
            players.setString(5, snapshot.matchUuid().toString());
            players.executeUpdate();
            match.setString(1, snapshot.matchUuid().toString());
            match.executeUpdate();
        }
        MatchHistoryIndex.recordFinish(connection, true, snapshot);
    }

    private static long count(MatchStatsDatabase database, String sql) throws SQLException {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
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

    @FunctionalInterface
    private interface Transaction<T> {
        T run(Connection connection) throws Exception;
    }
}
