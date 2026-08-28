package com.andrei1058.bedwars.arena;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Keeps plugin-controlled flight transitions from carrying stale movement state. */
public final class PlayerMotion {

    private PlayerMotion() {
    }

    public static void reset(Player player) {
        if (player == null) return;
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0.0F);
    }

    public static void enableFlight(Player player) {
        if (player == null) return;
        reset(player);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public static void disableFlight(Player player) {
        if (player == null) return;
        reset(player);
        player.setFlying(false);
        player.setAllowFlight(false);
    }
}
