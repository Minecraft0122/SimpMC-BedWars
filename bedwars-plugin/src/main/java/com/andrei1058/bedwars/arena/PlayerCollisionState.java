package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Owns the entity collision flag for BedWars player lifecycle states. */
public final class PlayerCollisionState {

    private PlayerCollisionState() {
    }

    public static boolean shouldCollide(@NotNull GameState state, boolean spectator, boolean respawning) {
        return state == GameState.playing && !spectator && !respawning;
    }

    public static void apply(@NotNull Player player, @NotNull GameState state,
                      boolean spectator, boolean respawning) {
        // Keep the server-side entity flag in sync with the scoreboard rule. This
        // prevents anti-cheat plugins from seeing a real collision while the client
        // is merely prevented from pushing by Team.COLLISION_RULE.
        player.setCollidable(shouldCollide(state, spectator, respawning));
    }
}
