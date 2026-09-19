package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.stats.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/** 独立的有界查询线程池；数据库读取不会阻塞游戏主线程或最终战绩写入。 */
public final class MatchHistoryService implements MatchHistory, AutoCloseable {
    private final MatchHistoryReader reader;
    private final MatchStatsRecorder recorder;
    private final ExecutorService executor;
    private volatile boolean closed;

    public MatchHistoryService(MatchStatsDatabase database, MatchStatsRecorder recorder) {
        this.reader = new MatchHistoryReader(database);
        this.recorder = recorder;
        this.executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(128), runnable -> {
                    Thread thread = new Thread(runnable, "SimpMC-BedWars-MatchHistory");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
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
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("对局查询已关闭"));
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return query.get();
                } catch (SQLException exception) {
                    throw new CompletionException(exception);
                }
            }, executor);
        } catch (RejectedExecutionException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface SqlQuery<T> {
        T get() throws SQLException;
    }
}
