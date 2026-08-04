package com.andrei1058.bedwars.listeners;

import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerQuitPolicyTest {

    @Test
    void onlyACompletedKickAbandonsTheGame() {
        for (PlayerQuitEvent.QuitReason reason : PlayerQuitEvent.QuitReason.values()) {
            assertEquals(reason == PlayerQuitEvent.QuitReason.KICKED,
                    PlayerQuitPolicy.abandonsGame(reason), reason.name());
        }
    }
}
