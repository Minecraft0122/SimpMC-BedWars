package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchWriteJournalTest {
    private static final Instant START = Instant.parse("2026-09-19T04:00:00.123Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @TempDir
    Path directory;

    @Test
    void roundTripsAllReplayableOperationsAndDoesNotDuplicateFiles() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        MatchRecordSnapshot match = snapshot(matchId, playerId, "FINISHED");
        MatchEventSnapshot event = new MatchEventSnapshot(UUID.randomUUID(), matchId, 4,
                "BED_BREAK", playerId, null, "details", START);
        PendingMatchWrite start = PendingMatchWrite.start(snapshot(matchId, playerId, "RUNNING"));
        PendingMatchWrite eventWrite = PendingMatchWrite.event(event);
        PendingMatchWrite finish = PendingMatchWrite.finish(match, List.of(playerId));
        PendingMatchWrite reset = PendingMatchWrite.reset(playerId, START.plusSeconds(10));

        MatchWriteJournal journal = new MatchWriteJournal(directory, "sqlite:test.db");
        for (PendingMatchWrite write : List.of(start, eventWrite, finish, reset)) {
            journal.save(write);
            journal.save(write);
        }

        try (var paths = Files.list(directory)) {
            assertEquals(4, paths.filter(path -> path.toString().endsWith(".pending")).count());
        }
        MatchWriteJournal reopened = new MatchWriteJournal(directory, "sqlite:test.db");
        List<PendingMatchWrite> writes = reopened.readAll();
        assertEquals(List.of(start.operationId(), eventWrite.operationId(), finish.operationId(), reset.operationId()),
                writes.stream().map(PendingMatchWrite::operationId).toList());
        assertEquals(event, writes.get(1).event());
        assertEquals(match, writes.get(2).match());
        assertEquals(playerId, writes.get(3).resetPlayer());
        assertEquals(START.plusSeconds(10), writes.get(3).resetAt());
        reopened.remove(finish.operationId());
        assertEquals(3, reopened.readAll().size());
    }

    @Test
    void refusesRecordsBoundToAnotherDatabaseTarget() throws Exception {
        MatchWriteJournal source = new MatchWriteJournal(directory, "mysql:one");
        PendingMatchWrite write = PendingMatchWrite.start(snapshot(UUID.randomUUID(), UUID.randomUUID(), "RUNNING"));
        source.save(write);
        MatchWriteJournal wrongTarget = new MatchWriteJournal(directory, "mysql:two");
        assertThrows(java.io.IOException.class, wrongTarget::readAll);
    }

    @Test
    void rejectsTruncatedRecordWithoutDeletingIt() throws Exception {
        MatchWriteJournal journal = new MatchWriteJournal(directory, "sqlite:test.db");
        PendingMatchWrite write = PendingMatchWrite.event(new MatchEventSnapshot(UUID.randomUUID(), UUID.randomUUID(),
                1, "MATCH_START", null, null, null, START));
        journal.save(write);
        Path file;
        try (var paths = Files.list(directory)) {
            file = paths.filter(path -> path.toString().endsWith(".pending")).findFirst().orElseThrow();
        }
        byte[] bytes = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(bytes, Math.max(1, bytes.length / 2)));
        MatchWriteJournal reopened = new MatchWriteJournal(directory, "sqlite:test.db");
        assertThrows(java.io.IOException.class, reopened::readAll);
        assertTrue(Files.exists(file));
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player, String status) {
        MatchPlayerSnapshot stats = new MatchPlayerSnapshot(player, "Alice", "red", 5, 2, 1, 1,
                0, 0, 0, 0, 0, MatchPlayerOutcome.WIN);
        return new MatchRecordSnapshot(match, "server", "solo", "solo-1", "Default", ZONE.getId(), status,
                "RUNNING".equals(status) ? null : "red", "RUNNING".equals(status) ? null : "WINNER",
                START, "RUNNING".equals(status) ? null : START.plusSeconds(120), START.plusSeconds(120),
                1, 4, new MatchStatsSnapshot(List.of(stats)));
    }
}
