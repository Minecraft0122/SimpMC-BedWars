package com.andrei1058.bedwars.api.stats;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 一场实际开始的比赛；数据库编号可有间隔，UUID 永久标识这场比赛。 */
public record MatchInfo(long matchNumber, UUID matchUuid, String arenaName,
                        String runtimeArenaName, String arenaGroup, String serverId,
                        String status, Instant startedAt, Instant endedAt, String winnerTeam) {
    public MatchInfo {
        if (matchNumber < 0) throw new IllegalArgumentException("对局编号不能为负数");
        Objects.requireNonNull(matchUuid, "matchUuid");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
    }
}
