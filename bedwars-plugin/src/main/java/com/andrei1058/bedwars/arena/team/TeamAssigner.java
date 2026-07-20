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

package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.ITeamAssigner;
import com.andrei1058.bedwars.api.events.gameplay.TeamAssignEvent;
import com.andrei1058.bedwars.api.tasks.StartingTask;
import com.andrei1058.bedwars.arena.ArenaStartPolicy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TeamAssigner implements ITeamAssigner {

    @Override
    public void assignTeams(IArena arena) {
        PreGameSquadManager squads = PreGameSquadManager.getInstance();
        try {
            List<List<Player>> groups = squads.getAssignmentGroups(arena);
            List<ITeam> arenaTeams = new ArrayList<>(arena.getTeams());
            Collections.shuffle(arenaTeams);
            List<List<Player>> allocation = TeamAllocationPlanner.allocate(
                    groups,
                    arenaTeams.size(),
                    arena.getMaxInTeam(),
                    ThreadLocalRandom.current()
            );
            List<List<Player>> approvedAllocation = new ArrayList<>(arenaTeams.size());
            for (int index = 0; index < arenaTeams.size(); index++) approvedAllocation.add(new ArrayList<>());

            for (int teamIndex = 0; teamIndex < arenaTeams.size(); teamIndex++) {
                ITeam team = arenaTeams.get(teamIndex);
                for (Player player : allocation.get(teamIndex)) {
                    TeamAssignEvent event = new TeamAssignEvent(player, team, arena);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) continue;
                    approvedAllocation.get(teamIndex).add(player);
                }
            }

            long activeTeams = approvedAllocation.stream().filter(team -> !team.isEmpty()).count();
            if (!canApplyAllocation(activeTeams, arena.getStartingTask())) {
                BedWars.plugin.getLogger().warning("竞技场 " + arena.getArenaName()
                        + " 的分队事件被取消后少于两支非空队伍，已停止开局。");
                return;
            }

            for (int teamIndex = 0; teamIndex < arenaTeams.size(); teamIndex++) {
                ITeam team = arenaTeams.get(teamIndex);
                for (Player player : approvedAllocation.get(teamIndex)) {
                    player.closeInventory();
                    team.addPlayers(player);
                }
            }
        } finally {
            squads.clearArena(arena);
        }
    }

    static boolean canApplyAllocation(long activeTeams, StartingTask startingTask) {
        return ArenaStartPolicy.canStartWithActiveTeams(activeTeams,
                startingTask != null && startingTask.isSingleTeamDebugStart());
    }
}
