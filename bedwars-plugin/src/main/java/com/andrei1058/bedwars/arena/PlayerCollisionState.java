package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Owns the entity collision flag for BedWars player lifecycle states. */
public final class PlayerCollisionState {

    private PlayerCollisionState() {
    }

    public static boolean shouldCollide(@NotNull GameState state, boolean spectator, boolean respawning) {
        // Paper 1.21.11 documents setCollidable as unsuitable for players.
        // Player pushing is controlled by Bukkit scoreboard Team rules.
        return true;
    }

    public static void apply(@NotNull Player player, @NotNull GameState state,
                      boolean spectator, boolean respawning) {
        // Paper 1.21.11 requires scoreboard Team collision rules for players.
    }
}
