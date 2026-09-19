package com.andrei1058.bedwars.api.stats;

import java.util.Objects;
import java.util.UUID;

/** 单局玩家战绩。kills 为普通击杀，deaths 包含普通死亡和最终死亡。 */
public record MatchPlayerResult(UUID playerUuid, String playerName, String teamId,
                                long kills, long finalKills, long deaths,
                                long bedsDestroyed, String outcome) {
    public MatchPlayerResult {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (kills < 0 || finalKills < 0 || deaths < 0 || bedsDestroyed < 0) {
            throw new IllegalArgumentException("战绩不能为负数");
        }
    }

    public double kdRatio() {
        return KillDeathRatio.calculate(kills, deaths);
    }
}
