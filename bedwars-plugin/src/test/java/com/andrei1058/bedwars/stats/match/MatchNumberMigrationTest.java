package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class MatchNumberMigrationTest {
    @TempDir Path folder;

    @Test
    void oldGapsAreCompactedWithReferencesAndRestartIsIdempotent() throws Exception {
        MatchStatsDatabase db = MatchStatsDatabase.sqlite(folder.resolve("matches.db"));
        try (var store = new MatchStatsStore(db, ZoneId.of("UTC"), "test", 100, 1);
             Connection c = db.openConnection()) {
            store.createSchema(c);
            seedLegacy(c);
            store.createSchema(c);
            assertMigrated(c);
            store.createSchema(c);
            assertMigrated(c);
            assertEquals(List.of(1L,4L,9L), values(c,"SELECT old_match_no FROM bw_match_number_migration ORDER BY new_match_no"));
        }
    }

    @Test
    void failureBetweenParentAndPlayerUpdatesRollsBackAndRetries() throws Exception {
        MatchStatsDatabase db = MatchStatsDatabase.sqlite(folder.resolve("matches.db"));
        try (var store = new MatchStatsStore(db, ZoneId.of("UTC"), "test", 100, 1);
             Connection c = db.openConnection(); Statement s = c.createStatement()) {
            store.createSchema(c);
            seedLegacy(c);
            s.executeUpdate("CREATE TRIGGER fail_renumber BEFORE UPDATE OF match_no ON bw_match_players "
                    + "BEGIN SELECT RAISE(ABORT, 'simulated failure'); END");
            assertThrows(SQLException.class, () -> MatchNumberMigration.migrate(c,true));
            assertEquals(List.of(1L,4L,9L), values(c,"SELECT match_no FROM bw_matches ORDER BY match_no"));
            assertEquals(List.of(), values(c,"SELECT new_match_no FROM bw_match_number_migration"));
            s.executeUpdate("DROP TRIGGER fail_renumber");
            MatchNumberMigration.migrate(c,true);
            assertMigrated(c);
        }
    }

    static void seedLegacy(Connection c) throws SQLException {
        try (Statement s=c.createStatement()) {
            s.executeUpdate("DELETE FROM bw_match_schema WHERE migration_id='"+MatchNumberMigration.ID+"'");
            for (int i=0;i<3;i++) {
                long number=new long[]{1,4,9}[i];
                String uuid="00000000-0000-0000-0000-00000000000"+i;
                String state=i==1?"ABORTED":"FINISHED";
                s.executeUpdate("INSERT INTO bw_matches (match_uuid,match_no,server_id,template_name,runtime_arena,arena_group,arena_timezone,status,started_at,ended_at,last_seen_at) "
                        +"VALUES ('"+uuid+"',"+number+",'old','map','map','Default','UTC','"+state+"','2026-01-01 00:00:00.000','2026-01-01 00:01:00.000','2026-01-01 00:01:00.000')");
                s.executeUpdate("INSERT INTO bw_match_players (match_uuid,player_uuid,player_name,team_id,normal_kills,final_kills,deaths,beds_destroyed,kd_ratio,outcome,updated_at,match_no) "
                        +"VALUES ('"+uuid+"','10000000-0000-0000-0000-000000000000','Alice','red',2,1,1,1,2,'WIN','2026-01-01 00:01:00.000',"+number+")");
            }
            s.executeUpdate("UPDATE bw_match_number_sequence SET last_match_no=500 WHERE sequence_id=1");
        }
    }

    static void assertMigrated(Connection c) throws SQLException {
        assertEquals(List.of(1L,2L,3L), values(c,"SELECT match_no FROM bw_matches ORDER BY match_no"));
        assertEquals(List.of(1L,2L,3L), values(c,"SELECT match_no FROM bw_match_players ORDER BY match_no"));
        assertEquals(List.of(3L), values(c,"SELECT last_match_no FROM bw_match_number_sequence WHERE sequence_id=1"));
        assertEquals(List.of(1L), values(c,"SELECT COUNT(*) FROM bw_matches WHERE status='ABORTED'"));
        assertEquals(List.of(6L), values(c,"SELECT SUM(normal_kills) FROM bw_match_players"));
    }

    static List<Long> values(Connection c,String sql) throws SQLException {
        List<Long> result=new ArrayList<>();
        try (Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) {while(r.next()) result.add(r.getLong(1));}
        return result;
    }
}
