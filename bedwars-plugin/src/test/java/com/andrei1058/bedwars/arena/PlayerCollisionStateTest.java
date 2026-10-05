package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCollisionStateTest {

    @Test
    void onlyActivePlayingParticipantsCollide() {
        assertFalse(PlayerCollisionState.shouldCollide(GameState.waiting, false, false));
        assertFalse(PlayerCollisionState.shouldCollide(GameState.starting, false, false));
        assertTrue(PlayerCollisionState.shouldCollide(GameState.playing, false, false));
        assertFalse(PlayerCollisionState.shouldCollide(GameState.playing, true, false));
        assertFalse(PlayerCollisionState.shouldCollide(GameState.playing, false, true));
        assertFalse(PlayerCollisionState.shouldCollide(GameState.restarting, false, false));
    }
}
