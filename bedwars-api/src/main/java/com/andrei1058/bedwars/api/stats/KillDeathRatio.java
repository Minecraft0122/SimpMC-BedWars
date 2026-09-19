package com.andrei1058.bedwars.api.stats;

/** 单局与累计共用的战损比口径：普通击杀除以全部死亡；零死亡时等于普通击杀。 */
public final class KillDeathRatio {
    private KillDeathRatio() { }

    public static double calculate(long kills, long deaths) {
        if (kills < 0 || deaths < 0) throw new IllegalArgumentException("战绩不能为负数");
        return deaths == 0 ? (double) kills : (double) kills / deaths;
    }
}
