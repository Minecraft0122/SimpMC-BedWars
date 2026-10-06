package com.andrei1058.bedwars.api.stats;

import com.andrei1058.bedwars.api.arena.IArena;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 持久化对局查询。数据库方法异步完成；回调中操作 Bukkit 对象前必须回到主线程。
 * 查询异常通过 future 传播，不把数据库故障当作没有战绩。
 */
public interface MatchHistory {
    boolean isEnabled();

    /** 当前对局的即时身份；未开局为空，编号尚未落库时为 0。 */
    Optional<MatchInfo> getCurrentMatch(IArena arena);

    CompletableFuture<Optional<MatchInfo>> findMatch(long matchNumber);

    CompletableFuture<Optional<MatchInfo>> findMatch(UUID matchUuid);

    /** 已持久化的玩家快照，包含中途退出者，不包含纯旁观者。 */
    CompletableFuture<List<MatchPlayerResult>> getMatchPlayers(UUID matchUuid);

    /** 只累计 FINISHED 对局；没有已完成对局时各项为 0。 */
    CompletableFuture<PlayerMatchTotals> getPlayerTotals(UUID playerUuid);

    /** 按编号倒序分页，包含进行中和中止的记录；limit 为 1..100，offset 不小于 0。 */
    CompletableFuture<List<MatchInfo>> getPlayerMatches(UUID playerUuid, int limit, int offset);

    /** 全部对局按编号倒序分页，包含进行中和中止记录；分页参数与玩家历史相同。 */
    default CompletableFuture<List<MatchInfo>> getMatches(int limit, int offset) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("当前实现不支持全部对局查询"));
    }

    /** 按已保存的玩家名查找离线 UUID（不区分大小写）；重名时异常完成，要求调用方指定 UUID。 */
    default CompletableFuture<Optional<UUID>> findPlayerUuid(String playerName) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("当前实现不支持玩家名查询"));
    }
}
