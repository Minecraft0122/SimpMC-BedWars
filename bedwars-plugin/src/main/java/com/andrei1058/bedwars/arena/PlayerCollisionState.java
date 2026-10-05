package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Owns server-side collision for BedWars player lifecycle states. */
public final class PlayerCollisionState {

    private PlayerCollisionState() {
    }

    public static boolean shouldCollide(@NotNull GameState state, boolean spectator, boolean respawning) {
        return state == GameState.playing && !spectator && !respawning;
    }

    public static void apply(@NotNull Player player, @NotNull GameState state,
                      boolean spectator, boolean respawning) {
        player.setCollidable(shouldCollide(state, spectator, respawning));
    }
}
