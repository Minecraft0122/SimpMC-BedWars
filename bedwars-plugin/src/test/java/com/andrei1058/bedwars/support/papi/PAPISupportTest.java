package com.andrei1058.bedwars.support.papi;

import com.andrei1058.bedwars.stats.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PAPISupportTest {

    @Test
    void matchCountLeaderboardAlwaysReturnsNumericCareerValue() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID());
        stats.setGamesPlayed(42);

        assertEquals("42", PAPISupport.gamesPlayedLeaderboardValue(stats));
    }

    @Test
    void kdLeaderboardRequiresStrictlyMoreThanOneHundredGames() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID());
        stats.setKills(8);
        stats.setDeaths(2);
        stats.setFinalDeaths(2);
        stats.setGamesPlayed(100);
        assertEquals("N/A", PAPISupport.qualifiedKdLeaderboardValue(stats));

        stats.setGamesPlayed(101);
        assertEquals("2.0", PAPISupport.qualifiedKdLeaderboardValue(stats));
    }

    @Test
    void qualifiedKdUsesRegularKillsAndAllDeathsWithZeroDeathRule() {
        PlayerStats stats = new PlayerStats(UUID.randomUUID());
        stats.setGamesPlayed(101);
        stats.setKills(7);
        stats.setFinalKills(99);

        assertEquals("7.0", PAPISupport.qualifiedKdLeaderboardValue(stats));
    }
}
