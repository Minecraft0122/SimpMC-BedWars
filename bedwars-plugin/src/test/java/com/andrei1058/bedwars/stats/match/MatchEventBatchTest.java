package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchEventBatchTest {
    private static final Instant START = Instant.parse("2026-09-19T04:00:00.123Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @TempDir
    Path directory;

    @Test
    void seventyAdjacentEventsCommitInBatchesOfSixtyFourAndSix() throws Exception {
        MatchStatsDatabase database = database();
        List<MatchEventSnapshot> events = events(UUID.randomUUID(), 1, 70);
        List<List<MatchEventSnapshot>> batches = new ArrayList<>();
        try (PausedWriter writer = new PausedWriter(database)) {
            doAnswer(call -> {
                batches.add(List.copyOf(call.<List<MatchEventSnapshot>>getArgument(1)));
                return call.callRealMethod();
            }).when(writer.store).writeEvents(any(), anyList());
            writer.start();
            enqueue(writer.store, events);
            CompletableFuture<Long> done = barrier(writer.store);
            writer.resume();
            assertTrue(done.get(10, TimeUnit.SECONDS) > 0);

            assertEquals(List.of(64, 6), batches.stream().map(List::size).toList());
            assertEquals(events, batches.stream().flatMap(List::stream).toList());
            verify(writer.store, never()).writeEvent(any(), any());
            assertEvents(database, events);
        }
        assertTrue(journal(database).readAll().isEmpty());
    }

    @Test
    void batchesStopAtStartAndFinishWithoutReorderingEvents() throws Exception {
        MatchStatsDatabase database = database();
        UUID match = UUID.randomUUID();
        List<MatchEventSnapshot> beforeStart = events(match, 1, 2);
        List<MatchEventSnapshot> inMatch = events(match, 3, 3);
        List<MatchEventSnapshot> afterFinish = events(match, 6, 2);
        MatchRecordSnapshot start = snapshot(match, "RUNNING");
        MatchRecordSnapshot finish = snapshot(match, "FINISHED");
        try (PausedWriter writer = new PausedWriter(database)) {
            writer.start();
            enqueue(writer.store, beforeStart);
            assertTrue(writer.store.enqueueStart(start));
            enqueue(writer.store, inMatch);
            assertTrue(writer.store.enqueueFinish(finish));
            enqueue(writer.store, afterFinish);
            CompletableFuture<Long> done = barrier(writer.store);
            writer.resume();
            assertTrue(done.get(10, TimeUnit.SECONDS) > 0);

            var ordered = inOrder(writer.store);
            ordered.verify(writer.store).writeEvents(any(), eq(beforeStart));
            ordered.verify(writer.store).writeStart(any(), eq(start));
            ordered.verify(writer.store).writeEvents(any(), eq(inMatch));
            ordered.verify(writer.store).writeFinish(any(), eq(finish), anyList());
            ordered.verify(writer.store).writeEvents(any(), eq(afterFinish));
            verify(writer.store, times(3)).writeEvents(any(), anyList());
            verify(writer.store, never()).writeEvent(any(), any());
            assertEquals("FINISHED", new MatchHistoryReader(database).findMatch(match).orElseThrow().status());
            List<MatchEventSnapshot> all = new ArrayList<>(beforeStart);
            all.addAll(inMatch);
            all.addAll(afterFinish);
            assertEvents(database, all);
        }
        assertTrue(journal(database).readAll().isEmpty());
    }

    @Test
    void partialBatchFailureRollsBackBeforeIndividualRetriesAndKeepsOnlyTheBadEvent() throws Exception {
        MatchStatsDatabase database = database();
        List<MatchEventSnapshot> events = events(UUID.randomUUID(), 1, 4);
        MatchEventSnapshot bad = events.get(1);
        AtomicBoolean checkedRollback = new AtomicBoolean();
        AtomicBoolean batchSimulation = new AtomicBoolean();
        try (PausedWriter writer = new PausedWriter(database)) {
            doAnswer(call -> {
                Connection connection = call.getArgument(0);
                List<MatchEventSnapshot> batch = call.getArgument(1);
                // Simulate a driver that wrote part of the batch before a later
                // element failed. The retry must begin after a full rollback.
                batchSimulation.set(true);
                try {
                    writer.store.writeEvent(connection, batch.getFirst());
                } finally {
                    batchSimulation.set(false);
                }
                assertEquals(1, countEvents(connection));
                throw new SQLException("invalid batch data", "22001", 1406);
            }).when(writer.store).writeEvents(any(), anyList());
            doAnswer(call -> {
                if (!batchSimulation.get() && checkedRollback.compareAndSet(false, true)) {
                    assertEquals(0, countEvents(call.getArgument(0)),
                            "The partial insert must be rolled back before the first individual retry");
                }
                return call.callRealMethod();
            }).when(writer.store).writeEvent(any(), eq(events.getFirst()));
            doThrow(new SQLException("invalid event data", "22001", 1406))
                    .when(writer.store).writeEvent(any(), eq(bad));
            writer.start();
            enqueue(writer.store, events);
            CompletableFuture<Long> done = barrier(writer.store);
            writer.resume();
            assertTrue(done.get(10, TimeUnit.SECONDS) > 0);

            assertTrue(checkedRollback.get());
            verify(writer.store, times(1)).writeEvents(any(), eq(events));
            verify(writer.store, times(2)).writeEvent(any(), eq(events.getFirst()));
            for (MatchEventSnapshot event : events.subList(1, events.size())) {
                verify(writer.store, times(1)).writeEvent(any(), eq(event));
            }
            assertEvents(database, List.of(events.get(0), events.get(2), events.get(3)));
        }
        List<PendingMatchWrite> pending = journal(database).readAll();
        assertEquals(1, pending.size());
        assertEquals(PendingMatchWrite.Kind.EVENT, pending.getFirst().kind());
        assertEquals(bad, pending.getFirst().event());

        // Once the bad input can be written, replay fills only the missing row.
        try (MatchStatsStore recovered = newStore(database)) {
            recovered.start();
            assertTrue(barrier(recovered).get(10, TimeUnit.SECONDS) > 0);
        }
        assertEvents(database, events);
        assertTrue(journal(database).readAll().isEmpty());
    }

    private MatchStatsDatabase database() {
        return MatchStatsDatabase.sqlite(directory.resolve("events.db"));
    }

    private static MatchStatsStore newStore(MatchStatsDatabase database) {
        return new MatchStatsStore(database, ZONE, "event-test", 100, 1);
    }

    private static MatchWriteJournal journal(MatchStatsDatabase database) {
        return new MatchWriteJournal(database.pendingWritesDirectory(), database.storageIdentity());
    }

    private static void enqueue(MatchStatsStore store, List<MatchEventSnapshot> events) {
        events.forEach(event -> assertTrue(store.enqueueEvent(event)));
    }

    private static CompletableFuture<Long> barrier(MatchStatsStore store) {
        CompletableFuture<Long> committed = new CompletableFuture<>();
        assertTrue(store.enqueueStart(snapshot(UUID.randomUUID(), "RUNNING"), committed::complete));
        return committed;
    }

    private static List<MatchEventSnapshot> events(UUID match, int startSequence, int count) {
        List<MatchEventSnapshot> events = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int sequence = startSequence + index;
            events.add(new MatchEventSnapshot(UUID.randomUUID(), match, sequence, "PLAYER_KILL", null, null,
                    "sequence=" + sequence, START.plusSeconds(sequence)));
        }
        return List.copyOf(events);
    }

    private static MatchRecordSnapshot snapshot(UUID match, String status) {
        boolean finished = "FINISHED".equals(status);
        return new MatchRecordSnapshot(match, "event-test", "solo", "solo-1", "Default", ZONE.getId(), status,
                null, finished ? "NO_WINNER" : null, START, finished ? START.plusSeconds(120) : null,
                START.plusSeconds(120), 1, 7, new MatchStatsSnapshot(List.of()));
    }

    private static long countEvents(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM bw_match_events")) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static void assertEvents(MatchStatsDatabase database, List<MatchEventSnapshot> expected) throws SQLException {
        Set<UUID> expectedIds = new HashSet<>();
        expected.forEach(event -> expectedIds.add(event.eventId()));
        Set<UUID> stored = new HashSet<>();
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT event_uuid, match_uuid, event_sequence, details FROM bw_match_events")) {
            while (result.next()) {
                UUID id = UUID.fromString(result.getString(1));
                assertTrue(stored.add(id), "Each event must have exactly one stored row");
                MatchEventSnapshot event = expected.stream().filter(value -> value.eventId().equals(id)).findFirst().orElseThrow();
                assertEquals(event.matchUuid().toString(), result.getString(2));
                assertEquals(event.sequence(), result.getLong(3));
                assertEquals(event.details(), result.getString(4));
            }
        }
        assertEquals(expectedIds, stored);
    }

    private static final class PausedWriter implements AutoCloseable {
        private final MatchStatsStore store;
        private final CountDownLatch schemaReady = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private PausedWriter(MatchStatsDatabase database) throws SQLException {
            store = spy(newStore(database));
            doAnswer(call -> {
                call.callRealMethod();
                schemaReady.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) throw new SQLException("Timed out waiting for test events");
                return null;
            }).when(store).createSchema(any());
        }

        private void start() throws InterruptedException {
            store.start();
            assertTrue(schemaReady.await(10, TimeUnit.SECONDS));
        }

        private void resume() {
            release.countDown();
        }

        @Override
        public void close() {
            resume();
            store.close();
        }
    }
}
