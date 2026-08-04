package com.andrei1058.bedwars.listeners;

import org.bukkit.event.player.PlayerQuitEvent;

/** Classifies final Paper quit reasons without depending on an anti-cheat API. */
final class PlayerQuitPolicy {

    private PlayerQuitPolicy() {
    }

    static boolean abandonsGame(PlayerQuitEvent.QuitReason reason) {
        return reason == PlayerQuitEvent.QuitReason.KICKED;
    }
}
