package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.stats.MatchInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Timeout(10)
class MatchHistoryServiceConcurrencyTest {
    @Test
    void twoQueriesStartTogetherAndTheBoundedQueueRejectsOverflow() throws Exception {
        MatchHistoryReader reader = mock(MatchHistoryReader.class);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(reader.findMatch(anyLong())).thenAnswer(call -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return Optional.empty();
        });
        try (MatchHistoryService service = new MatchHistoryService(reader, null, Duration.ofSeconds(5))) {
            List<CompletableFuture<Optional<MatchInfo>>> accepted = new ArrayList<>();
            accepted.add(service.findMatch(1));
            accepted.add(service.findMatch(2));
            assertTrue(started.await(2, TimeUnit.SECONDS), "Both workers should start before the queue fills");
            for (int number = 3; number <= 130; number++) accepted.add(service.findMatch(number));

            ExecutionException rejected = assertThrows(ExecutionException.class,
                    () -> service.findMatch(131).get(1, TimeUnit.SECONDS));
            assertInstanceOf(RejectedExecutionException.class, rejected.getCause());
            release.countDown();
            for (CompletableFuture<Optional<MatchInfo>> result : accepted) {
                assertTrue(result.get(3, TimeUnit.SECONDS).isEmpty());
            }
        } finally {
            release.countDown();
        }
    }

    @Test
    void closeTerminatesAllFuturesEvenWhenRunningQueriesIgnoreInterruption() throws Exception {
        MatchHistoryReader reader = mock(MatchHistoryReader.class);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(2);
        when(reader.findMatch(anyLong())).thenAnswer(call -> {
            started.countDown();
            try {
                boolean released = false;
                while (!released) {
                    try {
                        released = release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        // Some JDBC drivers do not abort an operation on interrupt.
                    }
                }
                return Optional.empty();
            } finally {
                exited.countDown();
            }
        });
        MatchHistoryService service = new MatchHistoryService(reader, null, Duration.ofSeconds(5));
        try {
            CompletableFuture<?> first = service.findMatch(1);
            CompletableFuture<?> second = service.findMatch(2);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            CompletableFuture<?> queued = service.findMatch(3);

            service.close();

            assertFalse(service.isEnabled());
            assertTrue(first.isCompletedExceptionally());
            assertTrue(second.isCompletedExceptionally());
            assertTrue(queued.isCompletedExceptionally());
            assertTrue(service.findMatch(4).isCompletedExceptionally());
            release.countDown();
            assertTrue(exited.await(2, TimeUnit.SECONDS));
            verify(reader, times(2)).findMatch(anyLong());
            verify(reader, never()).findMatch(3L);
            verify(reader, never()).findMatch(4L);
        } finally {
            release.countDown();
            service.close();
        }
    }

    @Test
    void queryDeadlineFailsTheCallerAndInterruptsTheRunningOperation() throws Exception {
        MatchHistoryReader reader = mock(MatchHistoryReader.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        when(reader.findMatch(1)).thenAnswer(call -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
                return Optional.empty();
            } catch (InterruptedException exception) {
                interrupted.countDown();
                throw new SQLException("查询已中断", exception);
            }
        });
        try (MatchHistoryService service = new MatchHistoryService(reader, null, Duration.ofMillis(250))) {
            CompletableFuture<?> result = service.findMatch(1);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> result.get(2, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, failure.getCause());
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void cancellingAQueuedFuturePreventsItsReaderCall() throws Exception {
        MatchHistoryReader reader = mock(MatchHistoryReader.class);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(reader.findMatch(anyLong())).thenAnswer(call -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return Optional.empty();
        });
        try (MatchHistoryService service = new MatchHistoryService(reader, null, Duration.ofSeconds(5))) {
            CompletableFuture<?> first = service.findMatch(1);
            CompletableFuture<?> second = service.findMatch(2);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            CompletableFuture<?> cancelled = service.findMatch(3);
            assertTrue(cancelled.cancel(true));
            CompletableFuture<?> afterCancellation = service.findMatch(4);
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
            afterCancellation.get(2, TimeUnit.SECONDS);
            verify(reader, never()).findMatch(3L);
            verify(reader).findMatch(4L);
        } finally {
            release.countDown();
        }
    }
}
