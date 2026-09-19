package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.stats.*;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/** 独立的有界查询线程池；数据库读取不会阻塞游戏主线程或最终战绩写入。 */
public final class MatchHistoryService implements MatchHistory, AutoCloseable {
    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(10);
    private final MatchHistoryReader reader;
    private final MatchStatsRecorder recorder;
    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor timeoutExecutor;
    private final long queryTimeoutNanos;
    private final Object lifecycle = new Object();
    private final Set<QueryTask<?>> pending = new HashSet<>();
    private volatile boolean closed;

    public MatchHistoryService(MatchStatsDatabase database, MatchStatsRecorder recorder) {
        this(new MatchHistoryReader(database), recorder, QUERY_TIMEOUT);
    }

    MatchHistoryService(MatchHistoryReader reader, MatchStatsRecorder recorder, Duration queryTimeout) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.recorder = recorder;
        this.queryTimeoutNanos = Objects.requireNonNull(queryTimeout, "queryTimeout").toNanos();
        if (queryTimeoutNanos <= 0) throw new IllegalArgumentException("查询超时必须为正数");
        this.executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(128), runnable -> {
                    Thread thread = new Thread(runnable, "SimpMC-BedWars-MatchHistory");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        this.timeoutExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "SimpMC-BedWars-MatchHistory-Timeout");
            thread.setDaemon(true);
            return thread;
        });
        timeoutExecutor.setRemoveOnCancelPolicy(true);
    }

    @Override
    public boolean isEnabled() {
        return !closed;
    }

    @Override
    public Optional<MatchInfo> getCurrentMatch(IArena arena) {
        return closed || recorder == null ? Optional.empty() : recorder.getCurrentMatch(arena);
    }

    @Override
    public CompletableFuture<Optional<MatchInfo>> findMatch(long matchNumber) {
        if (matchNumber <= 0) return CompletableFuture.failedFuture(new IllegalArgumentException("对局编号必须为正数"));
        return query(() -> reader.findMatch(matchNumber));
    }

    @Override
    public CompletableFuture<Optional<MatchInfo>> findMatch(UUID matchUuid) {
        Objects.requireNonNull(matchUuid, "matchUuid");
        return query(() -> reader.findMatch(matchUuid));
    }

    @Override
    public CompletableFuture<List<MatchPlayerResult>> getMatchPlayers(UUID matchUuid) {
        Objects.requireNonNull(matchUuid, "matchUuid");
        return query(() -> reader.getMatchPlayers(matchUuid));
    }

    @Override
    public CompletableFuture<PlayerMatchTotals> getPlayerTotals(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        return query(() -> reader.getPlayerTotals(playerUuid));
    }

    @Override
    public CompletableFuture<List<MatchInfo>> getPlayerMatches(UUID playerUuid, int limit, int offset) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (limit < 1 || limit > 100 || offset < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("分页参数无效"));
        }
        return query(() -> reader.getPlayerMatches(playerUuid, limit, offset));
    }

    private <T> CompletableFuture<T> query(SqlQuery<T> query) {
        QueryTask<T> task = new QueryTask<>(() -> {
            // shutdownNow removes queued tasks; this gate also covers a worker
            // that took a task immediately before shutdown began.
            synchronized (lifecycle) {
                if (closed) throw new IllegalStateException("对局查询已关闭");
            }
            return query.get();
        });
        task.result.whenComplete((result, failure) -> {
            // Cancellation and timeout must reach both a running query and a
            // queued query; cancelling only CompletableFuture does neither.
            task.cancel(true);
            executor.remove(task);
            ScheduledFuture<?> timeout = task.timeout;
            if (timeout != null) timeout.cancel(false);
            synchronized (lifecycle) {
                pending.remove(task);
            }
        });
        synchronized (lifecycle) {
            if (closed) return CompletableFuture.failedFuture(new IllegalStateException("对局查询已关闭"));
            pending.add(task);
            try {
                task.timeout = timeoutExecutor.schedule(() -> task.result.completeExceptionally(
                        new TimeoutException("对局查询超时")), queryTimeoutNanos, TimeUnit.NANOSECONDS);
                executor.execute(task);
            } catch (RejectedExecutionException exception) {
                task.result.completeExceptionally(exception);
            }
        }
        return task.result;
    }

    @Override
    public void close() {
        List<QueryTask<?>> outstanding;
        synchronized (lifecycle) {
            if (closed) return;
            closed = true;
            outstanding = new ArrayList<>(pending);
        }
        // The caller may be the server thread. Do not wait for JDBC drivers
        // that ignore interruption; their caller futures still finish now.
        executor.shutdownNow();
        timeoutExecutor.shutdownNow();
        for (QueryTask<?> task : outstanding) {
            task.result.completeExceptionally(new IllegalStateException("对局查询已关闭"));
            task.cancel(true);
        }
    }

    private static final class QueryTask<T> extends FutureTask<T> {
        private final CompletableFuture<T> result = new CompletableFuture<>();
        private volatile ScheduledFuture<?> timeout;

        private QueryTask(Callable<T> query) {
            super(query);
        }

        @Override
        protected void done() {
            try {
                result.complete(get());
            } catch (CancellationException exception) {
                result.cancel(false);
            } catch (ExecutionException exception) {
                result.completeExceptionally(exception.getCause());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                result.completeExceptionally(exception);
            }
        }
    }

    @FunctionalInterface
    private interface SqlQuery<T> {
        T get() throws SQLException;
    }
}
