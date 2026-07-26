/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameRules;
import com.andrei1058.bedwars.arena.SetupSession;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.LinkedList;

public class WorldLoadListener implements Listener {

    public WorldLoadListener() {
        Bukkit.getWorlds().forEach(GameRules::disableLocatorBar);
    }

    @EventHandler
    public void onLoad(WorldLoadEvent e) {
        GameRules.disableLocatorBar(e.getWorld());
        for (IArena a : new LinkedList<>(Arena.getEnableQueue())) {
            if (a.getWorldName().equalsIgnoreCase(e.getWorld().getName())) {
                a.init(e.getWorld());
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        String worldName = event.getWorld().getName();
        if (Arena.getArenaByIdentifier(worldName) != null || SetupSession.isSetupWorld(worldName)) {
            event.setCancelled(true);
        }
    }
}
