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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TeamAssigner implements ITeamAssigner {

    public static boolean canFormValidTeams(IArena arena) {
        List<List<Player>> groups = PreGameSquadManager.getInstance().getAssignmentGroups(arena);
        PreGameTeamSelectionManager selections = PreGameTeamSelectionManager.getInstance();
        return !TeamAllocationPlanner.allocateWithMinimum(groups, arena.getTeams(), arena.getMinInTeam(),
                arena.getMaxInTeam(), ThreadLocalRandom.current(),
                player -> selections.getSelection(arena, player)).isEmpty();
    }

    static <T> boolean canFormValidGroups(List<List<T>> groups, int configuredTeamCount,
                                          int minimumInTeam, int maximumInTeam,
                                          Random random) {
        return !TeamAllocationPlanner.allocateWithMinimum(groups, configuredTeamCount,
                minimumInTeam, maximumInTeam, random).isEmpty();
    }

    @Override
    public void assignTeams(IArena arena) {
        PreGameSquadManager squads = PreGameSquadManager.getInstance();
        try {
            List<List<Player>> groups = squads.getAssignmentGroups(arena);
            List<ITeam> arenaTeams = new ArrayList<>(arena.getTeams());
            StartingTask startingTask = arena.getStartingTask();
            boolean debugStart = startingTask != null && startingTask.isSingleTeamDebugStart();
            AllocationPlan plan = findAllocation(arena, groups, arenaTeams, arena.getMinInTeam(),
                    arena.getMaxInTeam(), debugStart);
            if (plan == null) {
                BedWars.plugin.getLogger().warning("竞技场 " + arena.getArenaName()
                        + " 无法按每队最少 " + arena.getMinInTeam() + " 人完成分队，已停止开局。");
                return;
            }

            List<ITeam> selectedTeams = plan.teams();
            List<List<Player>> allocation = plan.allocation();
            List<List<Player>> approvedAllocation = new ArrayList<>(selectedTeams.size());
            for (int index = 0; index < selectedTeams.size(); index++) approvedAllocation.add(new ArrayList<>());

            for (int teamIndex = 0; teamIndex < selectedTeams.size(); teamIndex++) {
                ITeam team = selectedTeams.get(teamIndex);
                for (Player player : allocation.get(teamIndex)) {
                    TeamAssignEvent event = new TeamAssignEvent(player, team, arena);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) continue;
                    approvedAllocation.get(teamIndex).add(player);
                }
            }

            List<Integer> approvedTeamSizes = approvedAllocation.stream().map(List::size).toList();
            if (!canApplyAllocation(approvedTeamSizes, arena.getMinInTeam(),
                    arena.getMaxInTeam(), startingTask)) {
                BedWars.plugin.getLogger().warning("竞技场 " + arena.getArenaName()
                        + " 的分队事件执行后不再满足队伍数量或每队人数范围，已停止开局。");
                return;
            }

            for (int teamIndex = 0; teamIndex < selectedTeams.size(); teamIndex++) {
                ITeam team = selectedTeams.get(teamIndex);
                for (Player player : approvedAllocation.get(teamIndex)) {
                    player.closeInventory();
                    team.addPlayers(player);
                }
            }
        } finally {
            squads.clearArena(arena);
        }
    }

    private static AllocationPlan findAllocation(IArena arena, List<List<Player>> groups,
                                                 List<ITeam> arenaTeams,
                                                 int minimumInTeam, int maximumInTeam,
                                                 boolean debugStart) {
        int playerCount = groups.stream().mapToInt(List::size).sum();
        PreGameTeamSelectionManager selections = PreGameTeamSelectionManager.getInstance();
        Map<ITeam, List<Player>> allocation = TeamAllocationPlanner.allocateWithMinimum(groups, arenaTeams,
                minimumInTeam, maximumInTeam, ThreadLocalRandom.current(),
                player -> selections.getSelection(arena, player));
        if (!allocation.isEmpty()) {
            return new AllocationPlan(new ArrayList<>(allocation.keySet()), new ArrayList<>(allocation.values()));
        }

        if (!debugStart) return null;
        int teamCount = ArenaStartPolicy.debugActiveTeamCount(playerCount, arenaTeams.size(), maximumInTeam);
        if (teamCount == 0) return null;
        Collections.shuffle(arenaTeams);
        List<List<Player>> debugAllocation = TeamAllocationPlanner.allocate(groups, teamCount, maximumInTeam,
                ThreadLocalRandom.current());
        return new AllocationPlan(new ArrayList<>(arenaTeams.subList(0, teamCount)), debugAllocation);
    }

    static boolean canApplyAllocation(List<Integer> teamSizes, int minimumInTeam,
                                      int maximumInTeam, StartingTask startingTask) {
        return ArenaStartPolicy.canStartWithTeamSizes(teamSizes, minimumInTeam, maximumInTeam,
                startingTask != null && startingTask.isSingleTeamDebugStart());
    }

    private record AllocationPlan(List<ITeam> teams, List<List<Player>> allocation) {
    }
}
