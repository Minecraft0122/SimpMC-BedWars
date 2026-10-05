package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCollisionStateTest {

    @Test
    void playerEntityCollisionFlagAlwaysRemainsEnabled() {
        assertTrue(PlayerCollisionState.shouldCollide(GameState.waiting, false, false));
        assertTrue(PlayerCollisionState.shouldCollide(GameState.starting, false, false));
        assertTrue(PlayerCollisionState.shouldCollide(GameState.playing, false, false));
        assertTrue(PlayerCollisionState.shouldCollide(GameState.playing, true, false));
        assertTrue(PlayerCollisionState.shouldCollide(GameState.playing, false, true));
        assertTrue(PlayerCollisionState.shouldCollide(GameState.restarting, false, false));
    }
}
