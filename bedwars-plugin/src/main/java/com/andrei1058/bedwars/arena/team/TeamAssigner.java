/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TeamAssigner implements ITeamAssigner {

    public static boolean canFormValidTeams(IArena arena) {
        List<List<Player>> groups = PreGameSquadManager.getInstance().getAssignmentGroups(arena);
        int players = playerCount(groups);
        if (!ArenaStartPolicy.hasEnoughPlayers(players, arena.getMinPlayers(),
                arena.getTeams().size(), arena.getMaxInTeam())) {
            return false;
        }

        Map<ITeam, List<Player>> allocation = allocate(arena, groups, 2, ThreadLocalRandom.current());
        return ArenaStartPolicy.canStartWithTeamSizes(teamSizes(allocation),
                arena.getMinPlayers(), arena.getMaxInTeam(), false);
    }

    static <T> boolean canFormValidGroups(List<List<T>> groups, int configuredTeamCount,
                                          int minimumPlayers, int maximumInTeam, Random random) {
        int players = playerCount(groups);
        if (!ArenaStartPolicy.hasEnoughPlayers(players, minimumPlayers,
                configuredTeamCount, maximumInTeam)) {
            return false;
        }
        List<List<T>> allocation = TeamAllocationPlanner.allocate(
                groups, configuredTeamCount, maximumInTeam, random);
        return ArenaStartPolicy.canStartWithTeamSizes(
                allocation.stream().map(List::size).toList(), minimumPlayers, maximumInTeam, false);
    }

    @Override
    public void assignTeams(IArena arena) {
        PreGameSquadManager squads = PreGameSquadManager.getInstance();
        try {
            List<List<Player>> groups = squads.getAssignmentGroups(arena);
            StartingTask startingTask = arena.getStartingTask();
            boolean debugStart = startingTask != null && startingTask.isSingleTeamDebugStart();
            Map<ITeam, List<Player>> allocation = findAllocation(arena, groups, debugStart);
            if (allocation.isEmpty()) {
                BedWars.plugin.getLogger().warning("竞技场 " + arena.getArenaName()
                        + " 无法在 minPlayers=" + arena.getMinPlayers()
                        + "、maxInTeam=" + arena.getMaxInTeam() + " 的限制下完成分队，已停止开局。");
                return;
            }

            List<ITeam> selectedTeams = new ArrayList<>(allocation.keySet());
            List<List<Player>> approvedAllocation = new ArrayList<>(selectedTeams.size());
            for (int index = 0; index < selectedTeams.size(); index++) approvedAllocation.add(new ArrayList<>());

            for (int teamIndex = 0; teamIndex < selectedTeams.size(); teamIndex++) {
                ITeam team = selectedTeams.get(teamIndex);
                for (Player player : allocation.get(team)) {
                    TeamAssignEvent event = new TeamAssignEvent(player, team, arena);
                    Bukkit.getPluginManager().callEvent(event);
                    if (!event.isCancelled()) approvedAllocation.get(teamIndex).add(player);
                }
            }

            List<Integer> approvedTeamSizes = approvedAllocation.stream().map(List::size).toList();
            if (!canApplyAllocation(approvedTeamSizes, arena.getMinPlayers(),
                    arena.getMaxInTeam(), startingTask)) {
                BedWars.plugin.getLogger().warning("竞技场 " + arena.getArenaName()
                        + " 的分队事件执行后不再满足最低开局人数、队伍数量或容量限制，已停止开局。");
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

    private static Map<ITeam, List<Player>> findAllocation(IArena arena,
                                                            List<List<Player>> groups,
                                                            boolean debugStart) {
        if (!debugStart && !ArenaStartPolicy.hasEnoughPlayers(playerCount(groups), arena.getMinPlayers(),
                arena.getTeams().size(), arena.getMaxInTeam())) {
            return Map.of();
        }
        return allocate(arena, groups, debugStart ? 1 : 2, ThreadLocalRandom.current());
    }

    private static Map<ITeam, List<Player>> allocate(IArena arena, List<List<Player>> groups,
                                                     int requiredActiveTeams, Random random) {
        PreGameTeamSelectionManager selections = PreGameTeamSelectionManager.getInstance();
        return TeamAllocationPlanner.allocateBalanced(groups, arena.getTeams(), arena.getMaxInTeam(),
                requiredActiveTeams, random, player -> selections.getSelection(arena, player));
    }

    static boolean canApplyAllocation(List<Integer> teamSizes, int minimumPlayers,
                                      int maximumInTeam, StartingTask startingTask) {
        return ArenaStartPolicy.canStartWithTeamSizes(teamSizes, minimumPlayers, maximumInTeam,
                startingTask != null && startingTask.isSingleTeamDebugStart());
    }

    private static List<Integer> teamSizes(Map<?, ? extends List<?>> allocation) {
        return allocation.values().stream().map(List::size).toList();
    }

    private static int playerCount(List<? extends List<?>> groups) {
        return groups.stream().filter(group -> group != null).mapToInt(List::size).sum();
    }
}
