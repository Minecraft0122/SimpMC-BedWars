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

package com.andrei1058.bedwars.arena.tasks;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.arena.ReJoin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReJoinTask implements Runnable {

    private static final List<ReJoinTask> reJoinTasks = new ArrayList<>();

    private final IArena arena;
    private final ITeam bedWarsTeam;
    private final ReJoin reJoin;
    private final BukkitTask task;
    private boolean destroyed;

    public ReJoinTask(ReJoin reJoin, IArena arena, ITeam bedWarsTeam) {
        this.reJoin = reJoin;
        this.arena = arena;
        this.bedWarsTeam = bedWarsTeam;
        task = Bukkit.getScheduler().runTaskLater(BedWars.plugin, this,
                Math.max(1, BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME)) * 20L);
        reJoinTasks.add(this);
    }

    @Override
    public void run() {
        reJoin.expire();
        destroy();
    }

    /**
     * Get arena
     */
    public IArena getArena() {
        return arena;
    }

    /**
     * Destroy task
     */
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        reJoinTasks.remove(this);
        task.cancel();
    }

    /**
     * Get tasks list
     */
    @NotNull
    @Contract(pure = true)
    public static Collection<ReJoinTask> getReJoinTasks() {
        return Collections.unmodifiableCollection(reJoinTasks);
    }

    public void cancel() {
        destroy();
    }
}
