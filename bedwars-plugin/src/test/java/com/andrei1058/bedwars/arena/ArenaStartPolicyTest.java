package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaStartPolicyTest {

    @Test
    void requiresTwoPlayersBeforeStartingCountdown() {
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(0));
        assertFalse(ArenaStartPolicy.hasEnoughPlayers(1));
        assertTrue(ArenaStartPolicy.hasEnoughPlayers(2));
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
}
