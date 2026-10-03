package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;

/** Hide actual arena spectators, including their PlayerInfo entry. */
public final class SpectatorVisibility {
    private SpectatorVisibility() { }

    public static boolean hideIfSpectator(IArena arena, Player viewer, Player target) {
        if (arena == null || viewer.equals(target) || !arena.isPlayer(viewer)
                || arena.isSpectator(viewer) || !arena.isSpectator(target)) return false;
        viewer.hidePlayer(BedWars.plugin, target);
        return true;
    }
}
