package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class MatchSettlementBatchTest {
    @TempDir Path directory;

    @Test
    void zeroViolationMatchMarksAllPlayersWithoutReadingViolationRows() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("zero.db"));
        try (MatchStatsStore store = new MatchStatsStore(database, ZoneId.of("UTC"), "test", 100, 1);
             Connection connection = database.openConnection()) {
            store.createSchema(connection);
            Connection measured = mock(Connection.class, delegatesTo(connection));
            List<MatchPlayerSnapshot> players = new ArrayList<>();
            for (int index = 0; index < 32; index++) players.add(player(UUID.randomUUID(), 0));
            connection.setAutoCommit(false);
            store.writeFinish(measured, finished(players), List.of());
            connection.commit();

            verify(measured, never()).prepareStatement(contains("SELECT player_uuid FROM bw_match_players"));
            verify(measured, never()).prepareStatement(contains("bw_player_violation_totals"));
            verify(measured, times(1)).prepareStatement(contains("SET vl_applied=1 WHERE match_uuid=? AND vl_applied=0"));
            assertEquals(32, scalar(connection, "SELECT COUNT(*) FROM bw_match_players WHERE vl_applied=1"));
        }
    }

    @Test
    void violationBatchesRollbackTogetherAndRetriesOnlyApplyOnce() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("batch.db"));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        MatchRecordSnapshot snapshot = finished(List.of(player(second, 17), player(first, 12)));
        try (MatchStatsStore store = new MatchStatsStore(database, ZoneId.of("UTC"), "test", 100, 1);
             Connection connection = database.openConnection()) {
            store.createSchema(connection);
            connection.setAutoCommit(false);
            store.writeFinish(connection, snapshot, List.of());
            connection.rollback();
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM bw_player_violation_totals"));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM bw_matches"));
            Connection measured = mock(Connection.class, delegatesTo(connection));
            store.writeFinish(measured, snapshot, List.of());
            connection.commit();
            store.writeFinish(connection, snapshot, List.of());
            connection.commit();

            verify(measured, times(1)).prepareStatement(contains("SELECT player_uuid FROM bw_match_players"));
            verify(measured, times(1)).prepareStatement(contains("INSERT INTO bw_player_violation_totals"));
            verify(measured, times(1)).prepareStatement(contains("UPDATE bw_player_violation_totals SET crime_total_vl"));
            assertEquals(29, scalar(connection, "SELECT SUM(crime_total_vl) FROM bw_player_violation_totals"));
            assertEquals(29, scalar(connection, "SELECT SUM(punishment_total_vl) FROM bw_player_violation_totals"));
            assertEquals(2, scalar(connection, "SELECT COUNT(*) FROM bw_match_players WHERE vl_applied=1"));
        }
    }

    private static MatchPlayerSnapshot player(UUID id, int vl) {
        return new MatchPlayerSnapshot(id, "玩家", "red", 2, 1, 0, 1,
                vl, 0, 0, 0, 0, MatchPlayerOutcome.WIN);
    }

    private static MatchRecordSnapshot finished(List<MatchPlayerSnapshot> players) {
        Instant start = Instant.parse("2026-09-19T00:00:00Z");
        return new MatchRecordSnapshot(UUID.randomUUID(), "test", "map", "map-1", "Default", "UTC",
                "FINISHED", "red", "WINNER", start, start.plusSeconds(120), start.plusSeconds(120),
                1, 10, new MatchStatsSnapshot(players));
    }

    private static long scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
