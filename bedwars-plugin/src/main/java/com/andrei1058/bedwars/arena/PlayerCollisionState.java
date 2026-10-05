package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Owns server-side collision for BedWars player lifecycle states. */
final class PlayerCollisionState {

    private PlayerCollisionState() {
    }

    static boolean shouldCollide(@NotNull GameState state, boolean spectator, boolean respawning) {
        return state == GameState.playing && !spectator && !respawning;
    }

    static void apply(@NotNull Player player, @NotNull GameState state,
                      boolean spectator, boolean respawning) {
        player.setCollidable(shouldCollide(state, spectator, respawning));
    }
}
