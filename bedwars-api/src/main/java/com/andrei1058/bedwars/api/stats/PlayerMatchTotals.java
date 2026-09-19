package com.andrei1058.bedwars.api.stats;

import java.util.Objects;
import java.util.UUID;

/** 玩家所有已完成对局之和；K/D 使用总击杀与总死亡计算，不平均单局 K/D。 */
public record PlayerMatchTotals(UUID playerUuid, long matchesPlayed, long kills,
                                long finalKills, long deaths, long bedsDestroyed) {
    public PlayerMatchTotals {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (matchesPlayed < 0 || kills < 0 || finalKills < 0 || deaths < 0 || bedsDestroyed < 0) {
            throw new IllegalArgumentException("战绩不能为负数");
        }
    }

    public double kdRatio() {
        return KillDeathRatio.calculate(kills, deaths);
    }
}
