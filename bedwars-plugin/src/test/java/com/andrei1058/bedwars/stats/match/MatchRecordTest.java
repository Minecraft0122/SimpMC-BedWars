package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MatchRecordTest {

    @Test
    void assignsOrderedEventsAndKeepsOneFinalSnapshot() {
        UUID matchUuid = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-08-31T00:00:00Z");
        MatchRecord record = new MatchRecord(matchUuid, "arena-1", "solo", "solo-1",
                "Solo", "Asia/Shanghai", startedAt);
        UUID playerUuid = UUID.randomUUID();
        record.getStats().registerPlayer(playerUuid, "Alice", "Red").recordKill(false);

        MatchEventSnapshot first = record.event("PLAYER_JOIN", playerUuid, null, null, startedAt);
        MatchEventSnapshot second = record.event("PLAYER_KILL", playerUuid, null, "final=false", startedAt.plusSeconds(1));
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(1L, first.sequence());
        assertEquals(2L, second.sequence());
        assertEquals(matchUuid, first.matchUuid());

        MatchRecordSnapshot report = record.reportSnapshot(startedAt.plusSeconds(60));
        assertEquals(1, report.reportNumber());
        assertEquals(2L, report.lastEventSequence());

        MatchRecordSnapshot finalSnapshot = record.finish("FINISHED", "Red", "WINNER", startedAt.plusSeconds(120));
        assertEquals("FINISHED", finalSnapshot.status());
        assertEquals(startedAt.plusSeconds(120), finalSnapshot.endedAt());
        assertEquals(2, finalSnapshot.reportNumber());
        assertSame(finalSnapshot, record.finish("ABORTED", null, "PLUGIN_DISABLE", startedAt.plusSeconds(180)));
        assertNull(record.event("LATE_EVENT", playerUuid, null, null, startedAt.plusSeconds(181)));
    }
}
