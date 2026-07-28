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
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.LinkedList;

import static com.andrei1058.bedwars.BedWars.plugin;

public class WorldLoadListener implements Listener {

    public WorldLoadListener() {
        Bukkit.getWorlds().forEach(GameRules::disableLocatorBar);
        Bukkit.getScheduler().runTaskTimer(plugin, this::enforceManagedWorlds, 20L, 20L);
    }

    @EventHandler
    public void onInit(WorldInitEvent event) {
        GameRules.disableLocatorBar(event.getWorld());
        if (isManagedWorld(event.getWorld())) GameRules.enforceArenaEnvironment(event.getWorld());
    }

    @EventHandler
    public void onLoad(WorldLoadEvent e) {
        GameRules.disableLocatorBar(e.getWorld());
        for (IArena a : new LinkedList<>(Arena.getEnableQueue())) {
            if (a.getWorldName().equalsIgnoreCase(e.getWorld().getName())) {
                GameRules.enforceArenaEnvironment(e.getWorld());
                a.init(e.getWorld());
                return;
            }
        }
        if (SetupSession.isSetupWorld(e.getWorld().getName())) {
            GameRules.enforceArenaEnvironment(e.getWorld());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        if (isManagedWorld(event.getWorld())) {
            // Paper fires this before applying World#setTime. Calling the
            // environment guard here would publish the same event recursively.
            event.setCancelled(!GameRules.reachesBedWarsDayTime(
                    event.getWorld().getFullTime(), event.getSkipAmount()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (isManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (isManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (isManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (isManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (isManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    private void enforceManagedWorlds() {
        Arena.getArenas().stream().map(IArena::getWorld).forEach(GameRules::enforceArenaEnvironment);
        SetupSession.getSetupSessions().stream()
                .map(session -> Bukkit.getWorld(session.getWorldName()))
                .forEach(GameRules::enforceArenaEnvironment);
    }

    private static boolean isManagedWorld(World world) {
        if (world == null) return false;
        String worldName = world.getName();
        return Arena.getArenaByIdentifier(worldName) != null
                || Arena.getEnableQueue().stream().anyMatch(arena -> arena.getWorldName().equalsIgnoreCase(worldName))
                || SetupSession.isSetupWorld(worldName);
    }
}
