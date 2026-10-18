package com.andrei1058.bedwars.listeners.joinhandler;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Applies the arena/lobby visibility boundary before the join event returns. */
final class JoinVisibility {

    private JoinVisibility() {
    }

    static void synchronize(Player joined) {
        if (joined == null) return;
        boolean joinedInArena = Arena.isInArena(joined);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(joined)) continue;
            boolean onlineInArena = Arena.isInArena(online);
            if (joinedInArena || onlineInArena) {
                BedWars.nms.spigotHidePlayer(joined, online);
                BedWars.nms.spigotHidePlayer(online, joined);
            } else {
                BedWars.nms.spigotShowPlayer(joined, online);
                BedWars.nms.spigotShowPlayer(online, joined);
            }
        }
    }
}
