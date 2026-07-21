package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.GameState;

/** Keeps scoreboard health limited to living players in an active match. */
final class SidebarHealthPolicy {

    private SidebarHealthPolicy() {
    }

    static boolean shouldDisplay(GameState state, boolean spectator) {
        return state == GameState.playing && !spectator;
    }
}
