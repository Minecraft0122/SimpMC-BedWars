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
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

public class WorldLoadListener implements Listener {

    /** Apply the environment after this listener has been registered. */
    public void enforceLoadedWorlds() {
        for (World world : Bukkit.getWorlds()) {
            GameRules.disableLocatorBar(world);
            enforceEnvironment(world);
        }
    }

    @EventHandler
    public void onInit(WorldInitEvent event) {
        GameRules.disableLocatorBar(event.getWorld());
        enforceEnvironment(event.getWorld());
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
        enforceEnvironment(e.getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTimeSkip(TimeSkipEvent event) {
        if (BedWarsWorldEnvironment.shouldForceBrightNoon(event.getWorld())) {
            event.setSkipAmount(GameRules.skipAmountToFixedTime(event.getWorld().getFullTime()));
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameRuleChange(WorldGameRuleChangeEvent event) {
        if (!BedWarsWorldEnvironment.shouldForceBrightNoon(event.getWorld())) return;
        if (event.getGameRule().equals(org.bukkit.GameRules.ADVANCE_TIME)
                || event.getGameRule().equals(org.bukkit.GameRules.ADVANCE_WEATHER)) {
            event.setValue("false");
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (BedWarsWorldEnvironment.isArenaManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (BedWarsWorldEnvironment.isArenaManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (BedWarsWorldEnvironment.isArenaManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (BedWarsWorldEnvironment.isArenaManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (BedWarsWorldEnvironment.isArenaManagedWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    private static void enforceEnvironment(World world) {
        if (BedWarsWorldEnvironment.isArenaManagedWorld(world)) {
            GameRules.enforceArenaEnvironment(world);
        } else {
            BedWarsWorldEnvironment.enforceBrightNoon(world);
        }
    }
}
