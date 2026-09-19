package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchStatsRecoveryTest {
    @TempDir Path directory;
    private static final Instant START = Instant.parse("2026-09-19T04:00:00.123Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void failedJournalSaveCannotLetLaterResetOvertakeFinish() throws Exception {
        MatchStatsDatabase database = database();
        MatchWriteJournal persisted = journal(database);
        AtomicInteger saves = new AtomicInteger();
        UUID player = UUID.randomUUID();
        CountDownLatch tryingSave = new CountDownLatch(1), allowSave = new CountDownLatch(1);
        try (var journals = mockConstruction(MatchWriteJournal.class, (mock, context) -> {
            when(mock.readAll()).thenReturn(List.of());
            doAnswer(call -> {
                if (saves.getAndIncrement() == 0) {
                    tryingSave.countDown();
                    assertTrue(allowSave.await(10, TimeUnit.SECONDS));
                    throw new java.io.IOException("disk temporarily unavailable");
                }
                persisted.save(call.getArgument(0));
                return null;
            }).when(mock).save(any());
        }); MatchStatsStore store = spy(newStore(database))) {
            doThrow(new SQLException("invalid data", "22001", 1406)).when(store).writeFinish(any(), any(), anyList());
            store.start();
            assertTrue(store.enqueueFinish(snapshot(UUID.randomUUID(), player, "FINISHED", 10)));
            assertTrue(tryingSave.await(10, TimeUnit.SECONDS));
            // 磁盘保存未返回时，事件线程仍能立即入队。
            CompletableFuture<Boolean> accepted = CompletableFuture.supplyAsync(() -> store.enqueueResetPunishmentVl(player));
            try { assertTrue(accepted.get(2, TimeUnit.SECONDS)); }
            finally { allowSave.countDown(); }
        }
        assertEquals(List.of(PendingMatchWrite.Kind.FINISH, PendingMatchWrite.Kind.RESET),
                persisted.readAll().stream().map(PendingMatchWrite::kind).toList());
    }

    @Test
    void permanentBadEventIsSavedWithoutBlockingLaterMatch() throws Exception {
        MatchStatsDatabase database = database();
        MatchEventSnapshot event = new MatchEventSnapshot(UUID.randomUUID(), UUID.randomUUID(), 1, "BED_BREAK", null, null, "test", START);
        try (MatchStatsStore store = spy(newStore(database))) {
            doThrow(new SQLException("invalid data", "22001", 1406)).when(store).writeEvent(any(), eq(event));
            store.start();
            assertTrue(store.enqueueEvent(event));
            barrier(store);
        }
        List<PendingMatchWrite> writes = journal(database).readAll();
        assertEquals(1, writes.size());
        assertEquals(event, writes.getFirst().event());
        try (MatchStatsStore store = newStore(database)) {
            store.start();
            barrier(store);
        }
        assertTrue(journal(database).readAll().isEmpty());
        try (Connection connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM bw_match_events")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void deferredFinishSurvivesRecoveryAndRetainsOriginalEndTime() throws Exception {
        MatchStatsDatabase database = database();
        UUID match = UUID.randomUUID(), player = UUID.randomUUID();
        MatchRecordSnapshot finished = snapshot(match, player, "FINISHED", 12);
        try (MatchStatsStore setup = newStore(database); Connection connection = database.openConnection()) {
            setup.createSchema(connection);
            connection.setAutoCommit(false);
            setup.writeStart(connection, snapshot(match, player, "RUNNING", 0));
            connection.commit();
        }
        journal(database).save(PendingMatchWrite.finish(finished, List.of()));
        try (MatchStatsStore broken = spy(newStore(database))) {
            doThrow(new SQLException("permission denied", "42000", 1142)).when(broken).writeFinish(any(), any(), anyList());
            broken.start();
            barrier(broken);
            assertEquals("RUNNING", new MatchHistoryReader(database).findMatch(match).orElseThrow().status());
        }
        try (MatchStatsStore recovered = newStore(database)) {
            recovered.start();
            barrier(recovered);
        }
        var info = new MatchHistoryReader(database).findMatch(match).orElseThrow();
        assertEquals("FINISHED", info.status());
        assertEquals(finished.endedAt(), info.endedAt());
        assertEquals(12, new MatchHistoryReader(database).getMatchPlayers(match).getFirst().kills());
    }

    @Test
    void failedFinishKeepsLaterResetAndFinishInOriginalPlayerOrder() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID(), first = UUID.randomUUID(), next = UUID.randomUUID();
        MatchRecordSnapshot finish = snapshot(first, player, "FINISHED", 10);
        try (MatchStatsStore store = spy(newStore(database))) {
            doThrow(new SQLException("invalid data", "22001", 1406)).when(store).writeFinish(any(), eq(finish), anyList());
            store.start();
            assertTrue(store.enqueueFinish(finish));
            assertTrue(store.enqueueResetPunishmentVl(player));
            assertTrue(store.enqueueFinish(snapshot(next, player, "FINISHED", 5)));
            barrier(store);
        }
        assertEquals(List.of(PendingMatchWrite.Kind.FINISH, PendingMatchWrite.Kind.RESET, PendingMatchWrite.Kind.FINISH),
                journal(database).readAll().stream().map(PendingMatchWrite::kind).toList());
        try (MatchStatsStore store = newStore(database)) {
            store.start();
            barrier(store);
        }
        assertEquals(5, punishmentTotal(database, player));
    }

    @Test
    void repeatedResetReceiptCannotEraseNewerPunishmentTotals() throws Exception {
        MatchStatsDatabase database = database();
        UUID player = UUID.randomUUID();
        PendingMatchWrite reset = PendingMatchWrite.reset(player, START.plusSeconds(121));
        try (MatchStatsStore store = newStore(database)) {
            store.start();
            assertTrue(store.enqueueFinish(snapshot(UUID.randomUUID(), player, "FINISHED", 10)));
            barrier(store);
        }
        journal(database).save(reset);
        try (MatchStatsStore store = newStore(database)) {
            store.start();
            assertTrue(store.enqueueFinish(snapshot(UUID.randomUUID(), player, "FINISHED", 7, START.plusSeconds(240))));
            barrier(store);
        }
        assertEquals(7, punishmentTotal(database, player));
        journal(database).save(reset); // 模拟提交成功但待写文件删除未确认。
        try (MatchStatsStore store = newStore(database)) {
            store.start();
            barrier(store);
        }
        assertEquals(7, punishmentTotal(database, player));
    }

    @Test
    void delayedResetCannotEraseNewerFinishFromIndependentWriter() throws Exception {
        MatchStatsDatabase firstNode = database();
        UUID player = UUID.randomUUID();
        try (MatchStatsStore store = newStore(firstNode)) {
            store.start();
            assertTrue(store.enqueueFinish(snapshot(UUID.randomUUID(), player, "FINISHED", 10)));
            barrier(store);
        }
        PendingMatchWrite delayedReset = PendingMatchWrite.reset(player, START.plusSeconds(121));
        journal(firstNode).save(delayedReset);

        // 两个子服共享数据库，但各自拥有本地待写目录。所有结算仍由真实 SQLite 执行。
        MatchStatsDatabase secondNode = spy(MatchStatsDatabase.sqlite(directory.resolve("matches.db")));
        doReturn(directory.resolve("second-node.pending")).when(secondNode).pendingWritesDirectory();
        try (MatchStatsStore store = new MatchStatsStore(secondNode, ZONE, "second-server", 100, 1)) {
            store.start();
            assertTrue(store.enqueueFinish(snapshot(UUID.randomUUID(), player, "FINISHED", 7, START.plusSeconds(240))));
            barrier(store);
        }
        assertEquals(17, punishmentTotal(firstNode, player));
        assertEquals(List.of(delayedReset), journal(firstNode).readAll());

        try (MatchStatsStore recovered = newStore(firstNode)) {
            recovered.start();
            barrier(recovered);
        }
        // 旧 RESET 晚到时保留全部当前累计，不能清掉另一个子服在处罚时间之后新增的 7。
        assertEquals(17, punishmentTotal(firstNode, player));
        assertTrue(journal(firstNode).readAll().isEmpty());
    }

    @Test
    void shutdownDeadlinePersistsFinalSnapshotEvenWhenCriticalQueueIsFull() throws Exception {
        MatchStatsDatabase database = database();
        MatchRecordSnapshot start = snapshot(UUID.randomUUID(), UUID.randomUUID(), "RUNNING", 0);
        MatchRecordSnapshot finish = snapshot(start.matchUuid(), start.playerStats().players().getFirst().playerUuid(), "FINISHED", 9);
        CountDownLatch entered = new CountDownLatch(1);
        MatchStatsStore store = spy(newStore(database));
        doAnswer(invocation -> {
            entered.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException ignored) { throw new SQLException("shutdown", "08006"); }
            return 0L;
        }).when(store).writeStart(any(), eq(start));
        store.start();
        assertTrue(store.enqueueStart(start));
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        for (int sequence = 1; sequence <= 100; sequence++) {
            assertTrue(store.enqueueEvent(new MatchEventSnapshot(UUID.randomUUID(), start.matchUuid(), sequence,
                    "BED_BREAK", start.playerStats().players().getFirst().playerUuid(), null, null,
                    START.plusSeconds(sequence))));
        }
        assertEquals(100, store.queuedOperations());
        assertFalse(store.enqueueFinish(finish), "普通入队必须确认关键队列已经满载");
        assertTrue(store.enqueueFinishForShutdown(finish, Set.of()), "关闭快照必须绕过已满队列保留待写数据");
        store.close(0, TimeUnit.MILLISECONDS);
        List<PendingMatchWrite> writes = journal(database).readAll();
        assertTrue(writes.stream().anyMatch(write -> write.kind() == PendingMatchWrite.Kind.START));
        assertTrue(writes.stream().anyMatch(write -> write.kind() == PendingMatchWrite.Kind.FINISH));
        try (MatchStatsStore recovered = newStore(database)) {
            recovered.start();
            barrier(recovered);
        }
        assertEquals("FINISHED", new MatchHistoryReader(database).findMatch(start.matchUuid()).orElseThrow().status());
        assertEquals(finish.endedAt(), new MatchHistoryReader(database).findMatch(start.matchUuid()).orElseThrow().endedAt());
        assertEquals(9, new MatchHistoryReader(database).getMatchPlayers(start.matchUuid()).getFirst().kills());
    }

    @Test
    void schemaFailureStillPersistsNewFinalSnapshotDuringShutdown() throws Exception {
        MatchStatsDatabase database = database();
        MatchRecordSnapshot started = snapshot(UUID.randomUUID(), UUID.randomUUID(), "RUNNING", 0);
        MatchRecordSnapshot finished = snapshot(started.matchUuid(), started.playerStats().players().getFirst().playerUuid(), "ABORTED", 9);
        MatchWriteJournal persisted = journal(database);
        CountDownLatch schemaEntered = new CountDownLatch(1), failSchema = new CountDownLatch(1), writerStopped = new CountDownLatch(1);
        try (var journals = mockConstruction(MatchWriteJournal.class, (mock, context) -> {
            when(mock.readAll()).thenAnswer(call -> persisted.readAll());
            doAnswer(call -> {
                persisted.save(call.getArgument(0));
                writerStopped.countDown();
                return null;
            }).when(mock).save(any());
        }); MatchStatsStore store = spy(newStore(database))) {
            doAnswer(invocation -> {
                schemaEntered.countDown();
                assertTrue(failSchema.await(10, TimeUnit.SECONDS));
                throw new SQLException("CREATE denied", "42000", 1142);
            }).when(store).createSchema(any());
            store.start();
            assertTrue(schemaEntered.await(10, TimeUnit.SECONDS));
            assertTrue(store.enqueueStart(started));
            failSchema.countDown();
            // 只有初始化失败后的 finally 才保存该开局快照；此时 running 已经变为 false。
            assertTrue(writerStopped.await(25, TimeUnit.SECONDS));
            // Recorder 在关闭时才产生这个新的最终快照：不能只保留此前的开局快照。
            assertTrue(store.enqueueFinishForShutdown(finished, Set.of()), "schema 失败后关闭产生的最终快照仍应保存到待写记录");
        }
        List<PendingMatchWrite> writes = persisted.readAll();
        assertTrue(writes.stream().anyMatch(write -> write.kind() == PendingMatchWrite.Kind.START && write.match().equals(started)));
        assertTrue(writes.stream().anyMatch(write -> write.kind() == PendingMatchWrite.Kind.FINISH && write.match().equals(finished)));
    }

    private MatchStatsDatabase database() { return MatchStatsDatabase.sqlite(directory.resolve("matches.db")); }
    private static MatchStatsStore newStore(MatchStatsDatabase database) { return new MatchStatsStore(database, ZONE, "server", 100, 1); }
    private static MatchWriteJournal journal(MatchStatsDatabase database) { return new MatchWriteJournal(database.pendingWritesDirectory(), database.storageIdentity()); }

    private static void barrier(MatchStatsStore store) throws Exception {
        CompletableFuture<Long> done = new CompletableFuture<>();
        assertTrue(store.enqueueStart(snapshot(UUID.randomUUID(), UUID.randomUUID(), "RUNNING", 0), done::complete));
        assertTrue(done.get(10, TimeUnit.SECONDS) > 0);
    }

    private static int punishmentTotal(MatchStatsDatabase database, UUID player) throws Exception {
        try (Connection connection = database.openConnection(); var statement = connection.prepareStatement("SELECT punishment_total_vl FROM bw_player_violation_totals WHERE player_uuid=?")) {
            statement.setString(1, player.toString());
            try (var result = statement.executeQuery()) { assertTrue(result.next()); return result.getInt(1); }
        }
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player, String status, int amount) {
        return snapshot(match, player, status, amount, START.plusSeconds(120));
    }

    private static MatchRecordSnapshot snapshot(UUID match, UUID player, String status, int amount, Instant capturedAt) {
        MatchPlayerSnapshot stats = new MatchPlayerSnapshot(player, "Alice", "red", amount, 2, 1, 1,
                amount, 0, 0, 0, 0, MatchPlayerOutcome.WIN);
        return new MatchRecordSnapshot(match, "server", "solo", "solo-1", "Default", ZONE.getId(), status,
                "RUNNING".equals(status) ? null : "red", "RUNNING".equals(status) ? null : "WINNER",
                START, "RUNNING".equals(status) ? null : capturedAt, capturedAt,
                1, amount, new MatchStatsSnapshot(List.of(stats)));
    }
}
