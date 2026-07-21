package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaJoinAnnouncementTest {

    @Test
    void onlyBroadcastsWhileArenaIsPreparing() {
        assertTrue(ArenaAnnouncementPolicy.shouldBroadcastJoin(GameState.waiting));
        assertTrue(ArenaAnnouncementPolicy.shouldBroadcastJoin(GameState.starting));
        assertFalse(ArenaAnnouncementPolicy.shouldBroadcastJoin(GameState.playing));
        assertFalse(ArenaAnnouncementPolicy.shouldBroadcastJoin(GameState.restarting));
        assertFalse(ArenaAnnouncementPolicy.shouldBroadcastJoin(null));
    }
}
