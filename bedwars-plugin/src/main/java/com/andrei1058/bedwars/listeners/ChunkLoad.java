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

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.listeners.blockstatus.BlockStatusListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayList;

public class ChunkLoad implements Listener {

    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent e){
        if (e == null) return;
        if (e.getChunk() == null) return;
        if (e.getChunk().getEntities() == null) return;
        Bukkit.getScheduler().runTask(BedWars.plugin, ()-> {
            for (Entity entity : e.getChunk().getEntities()){
                if (entity.hasMetadata("bw1058-setup")) {
                    entity.remove();
                    continue;
                }
                if (entity instanceof ArmorStand){
                    if (!((ArmorStand)entity).isVisible()){
                        if (((ArmorStand)entity).isMarker()){
                            //if (!entity.hasGravity()){
                            if (entity.isCustomNameVisible()){
                                if (entity instanceof org.bukkit.Nameable nameable
                                        && (AdventureText.customName(nameable).contains(" SET") || AdventureText.customName(nameable).contains(" set"))){
                                    entity.remove();
                                }
                            }
                            //}
                        }
                    }
                }
            }
            for (IArena arena : new ArrayList<>(Arena.getArenas())) {
                if (!(arena instanceof Arena concreteArena)) continue;
                concreteArena.refreshSigns(e.getChunk());
                BlockStatusListener.updateBlock(concreteArena, e.getChunk());
            }
        });
    }
}
