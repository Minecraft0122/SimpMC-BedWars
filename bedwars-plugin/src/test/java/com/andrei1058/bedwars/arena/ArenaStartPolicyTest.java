package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaStartPolicyTest {

    @Test
    void usesTheArenaWideMinimumInsteadOfAPerTeamMinimum() {
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(3, 4, 8, 4));
        assertTrue(ArenaStartPolicy.hasEnoughPlayers(4, 4, 8, 4));
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(3, 1, 0), 4, 4, false));
    }

    @Test
    void checksConfiguredTeamCapacity() {
        assertTrue(ArenaStartPolicy.hasEnoughPlayers(8, 2, 2, 4));
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(9, 2, 2, 4));
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(2, 2, 1, 4));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(5, 1), 2, 4, false));
    }

    @Test
    void normalRoundsStillRequireTwoNonEmptyTeams() {
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(4, 0), 2, 4, false));
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(1, 1), 2, 4, false));
    }

    @Test
    void debugStartMayBypassTheMinimumAndOpponentRequirementButNotCapacity() {
        assertTrue(ArenaStartPolicy.canStartWithTeamSizes(List.of(1), 8, 4, true));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(), 8, 4, true));
        assertFalse(ArenaStartPolicy.canStartWithTeamSizes(List.of(5), 8, 4, true));
    }

    @Test
    void fullArenaShortensOnlyCountdownsAboveFiveSeconds() {
        assertEquals(5, ArenaStartPolicy.shortenCountdownWhenFull(8, 8, 40));
        assertEquals(5, ArenaStartPolicy.shortenCountdownWhenFull(8, 8, 5));
        assertEquals(3, ArenaStartPolicy.shortenCountdownWhenFull(8, 8, 3));
        assertEquals(40, ArenaStartPolicy.shortenCountdownWhenFull(7, 8, 40));
    }
}
