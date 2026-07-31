package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaStartPolicyTest {

    @Test
    void requiresEnoughPlayersToFillTwoTeamsToTheArenaMinimum() {
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(3, 8, 2, 4));
        assertTrue(ArenaStartPolicy.hasEnoughPlayers(4, 8, 2, 4));
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(8, 4, 3, 3));
        assertTrue(ArenaStartPolicy.hasEnoughPlayers(9, 4, 3, 3));
    }

    @Test
    void selectsTheMostTeamsThatCanAllReachTheMinimum() {
        assertTrue(ArenaStartPolicy.isFeasibleActiveTeamCount(5, 2, 2, 4));
        assertFalse(ArenaStartPolicy.isFeasibleActiveTeamCount(5, 3, 2, 4));
        assertEquals(2, ArenaStartPolicy.minimumRequiredActiveTeams(6, 4));
        assertEquals(3, ArenaStartPolicy.minimumRequiredActiveTeams(9, 4));
        assertEquals(3, ArenaStartPolicy.maximumFeasibleActiveTeams(6, 8, 2, 4));
    }

    @Test
    void requiresSixPlayersWhenTeamSizeRangeIsThreeToFour() {
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(5, 8, 3, 4));
        assertTrue(ArenaStartPolicy.hasEnoughPlayers(6, 8, 3, 4));
        assertEquals(2, ArenaStartPolicy.maximumFeasibleActiveTeams(6, 8, 3, 4));
    }

    @Test
    void rejectsAStartedRoundWithOnlyOneActiveTeam() {
        assertFalse(ArenaStartPolicy.hasEnoughActiveTeams(1));
        assertTrue(ArenaStartPolicy.hasEnoughActiveTeams(2));
    }

    @Test
    void allowsExactlyOneTeamOnlyForDebugStarts() {
        assertFalse(ArenaStartPolicy.canStartWithActiveTeams(0, true));
        assertFalse(ArenaStartPolicy.canStartWithActiveTeams(1, false));
        assertTrue(ArenaStartPolicy.canStartWithActiveTeams(1, true));
        assertTrue(ArenaStartPolicy.canStartWithActiveTeams(2, false));
    }

    @Test
    void validatesEveryActiveTeamPopulationAfterAssignment() {
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(2, 3, 0), 2, false));
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(2, 2, 2), 2, false));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(3, 1), 2, false));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(2, 0, 0), 2, false));
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(1), 4, true));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(), 1, true));
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(3, 4), 3, 4, false));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(3, 5), 3, 4, false));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(5), 3, 4, true));
    }

    @Test
    void calculatesTheMaximumTeamCountFromTheInequalities() {
        for (int configuredTeams = 1; configuredTeams <= 10; configuredTeams++) {
            for (int minimum = 1; minimum <= 6; minimum++) {
                for (int maximum = minimum; maximum <= 8; maximum++) {
                    for (int players = 0; players <= 50; players++) {
                        int calculated = ArenaStartPolicy.maximumFeasibleActiveTeams(
                                players, configuredTeams, minimum, maximum);
                        int lowerBound = players < 1 ? 0
                                : Math.max(2, players / maximum + (players % maximum == 0 ? 0 : 1));
                        int upperBound = players < 1 ? 0
                                : Math.min(configuredTeams, players / minimum);
                        int expected = upperBound >= lowerBound ? upperBound : 0;
                        assertEquals(expected, calculated,
                                "players=" + players + ", teams=" + configuredTeams
                                        + ", range=" + minimum + ".." + maximum);
                    }
                }
            }
        }
    }
}
