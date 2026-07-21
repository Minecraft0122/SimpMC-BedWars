package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidebarHealthPolicyTest {

    @Test
    void onlyShowsHealthForLivingPlayersDuringTheMatch() {
        assertTrue(SidebarHealthPolicy.shouldDisplay(GameState.playing, false));
        assertFalse(SidebarHealthPolicy.shouldDisplay(GameState.playing, true));
        assertFalse(SidebarHealthPolicy.shouldDisplay(GameState.waiting, false));
        assertFalse(SidebarHealthPolicy.shouldDisplay(GameState.starting, false));
        assertFalse(SidebarHealthPolicy.shouldDisplay(GameState.restarting, false));
        assertFalse(SidebarHealthPolicy.shouldDisplay(null, false));
    }
}
