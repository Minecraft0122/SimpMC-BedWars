package com.andrei1058.bedwars.api.stats;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchStatisticsTest {
    @Test
    void zeroDeathsUsesKillsIncludingTheZeroZeroCase() {
        assertEquals(0.0, KillDeathRatio.calculate(0, 0));
        assertEquals(7.0, KillDeathRatio.calculate(7, 0));
        assertEquals(2.5, KillDeathRatio.calculate(5, 2));
    }

    @Test
    void finalKillsAreIndependentAndTotalsUseCombinedCounts() {
        UUID uuid = UUID.randomUUID();
        MatchPlayerResult first = new MatchPlayerResult(uuid, "玩家", "红队", 8, 2, 2, 1, "WIN");
        MatchPlayerResult second = new MatchPlayerResult(uuid, "玩家", "蓝队", 1, 4, 1, 2, "LOSS");
        PlayerMatchTotals totals = new PlayerMatchTotals(uuid, 2,
                first.kills() + second.kills(), first.finalKills() + second.finalKills(),
                first.deaths() + second.deaths(), first.bedsDestroyed() + second.bedsDestroyed());
        assertEquals(4.0, first.kdRatio());
        assertEquals(1.0, second.kdRatio());
        assertEquals(3.0, totals.kdRatio());
        assertEquals(6, totals.finalKills());
        assertEquals(3, totals.bedsDestroyed());
    }

    @Test
    void lifetimeCountsDoNotNarrowToInt() {
        PlayerMatchTotals totals = new PlayerMatchTotals(UUID.randomUUID(), 1,
                4_000_000_000L, 1, 2, 1);
        assertEquals(2_000_000_000.0, totals.kdRatio());
        assertThrows(IllegalArgumentException.class, () -> KillDeathRatio.calculate(-1, 0));
    }
}
