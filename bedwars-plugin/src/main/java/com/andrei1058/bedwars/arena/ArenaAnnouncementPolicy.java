package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;

/** Defines when arena presence messages are appropriate for players. */
final class ArenaAnnouncementPolicy {

    private ArenaAnnouncementPolicy() {
    }

    static boolean shouldBroadcastJoin(GameState state) {
        return state == GameState.waiting || state == GameState.starting;
    }
}
