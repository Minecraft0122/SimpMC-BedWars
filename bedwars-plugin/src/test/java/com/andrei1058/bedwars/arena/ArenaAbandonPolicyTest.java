package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaAbandonPolicyTest {

    @Test
    void eliminatesOnlyTheLastActiveOrPendingTeamMember() {
        assertTrue(ArenaAbandonPolicy.eliminatesTeam(0, false));
        assertFalse(ArenaAbandonPolicy.eliminatesTeam(0, true));
        assertFalse(ArenaAbandonPolicy.eliminatesTeam(1, false));
        assertFalse(ArenaAbandonPolicy.eliminatesTeam(1, true));
    }
}
