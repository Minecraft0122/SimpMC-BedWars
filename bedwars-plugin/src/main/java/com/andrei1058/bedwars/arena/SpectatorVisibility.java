package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Hide actual arena spectators, including their PlayerInfo entry. */
public final class SpectatorVisibility {
    private SpectatorVisibility() { }

    /** Establish visibility before Paper broadcasts the joining PlayerInfo entry. */
    public static void synchronizeJoin(Player joined, Collection<? extends Player> onlinePlayers) {
        IArena joinedArena = Arena.getArenaByPlayer(joined);
        for (Player other : onlinePlayers) {
            if (joined.equals(other)) continue;
            IArena otherArena = Arena.getArenaByPlayer(other);
            if (joinedArena != otherArena) {
                other.hidePlayer(BedWars.plugin, joined);
                joined.hidePlayer(BedWars.plugin, other);
            } else if (joinedArena != null) {
                hideIfSpectator(joinedArena, other, joined);
                hideIfSpectator(joinedArena, joined, other);
            }
        }
    }

    public static boolean hideIfSpectator(IArena arena, Player viewer, Player target) {
        if (arena == null || viewer.equals(target) || !arena.isPlayer(viewer)
                || arena.isSpectator(viewer) || !arena.isSpectator(target)) return false;
        viewer.hidePlayer(BedWars.plugin, target);
        return true;
    }
}
