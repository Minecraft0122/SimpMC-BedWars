package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaLobbyTransitionTest {

    @Test
    void delayedLobbyWorkRunsOnlyWhileThePlayerIsStillOnlineAndUnassigned() {
        assertTrue(ArenaTransitionPolicy.shouldApplyLobbyTransition(false, true, false));
        assertFalse(ArenaTransitionPolicy.shouldApplyLobbyTransition(true, true, false));
        assertFalse(ArenaTransitionPolicy.shouldApplyLobbyTransition(false, false, false));
        assertFalse(ArenaTransitionPolicy.shouldApplyLobbyTransition(false, true, true));
    }
}
