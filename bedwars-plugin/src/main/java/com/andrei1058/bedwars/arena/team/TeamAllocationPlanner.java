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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Capacity-aware allocator. It packs groups into as few active teams as the
 * start requirements allow and keeps normal squads together. Only a group
 * larger than one team's capacity may be split.
 */
final class TeamAllocationPlanner {

    private TeamAllocationPlanner() {
    }

    static <T> List<List<T>> allocate(@NotNull List<List<T>> sourceGroups, int teamCount,
                                      int capacity, @NotNull Random random) {
        validateCapacity(sourceGroups, teamCount, capacity);

        List<List<T>> allocation = findAtomicAllocation(
                sourceGroups, teamCount, capacity, Math.min(2, teamCount), random);
        if (allocation == null) throw new IllegalStateException("No team allocation could be created");
        return allocation;
    }

    static <T, K> Map<K, List<T>> allocateBalanced(@NotNull List<List<T>> sourceGroups,
                                                     @NotNull List<K> configuredTeams,
                                                     int capacity, int requiredActiveTeams,
                                                     @NotNull Random random) {
        if (configuredTeams.isEmpty() || requiredActiveTeams < 1
                || requiredActiveTeams > configuredTeams.size()) {
            return Map.of();
        }
        validateCapacity(sourceGroups, configuredTeams.size(), capacity);

        List<List<T>> allocation = findAtomicAllocation(
                sourceGroups, configuredTeams.size(), capacity, requiredActiveTeams, random);
        if (allocation == null) return Map.of();
        return mapToConfiguredTeams(allocation, configuredTeams, random);
    }

    private static void validateCapacity(List<? extends List<?>> groups, int teamCount, int capacity) {
        if (teamCount < 1 || capacity < 1) {
            throw new IllegalArgumentException("Team count and capacity must be positive");
        }
        long players = groups.stream().filter(group -> group != null).mapToLong(List::size).sum();
        if (players > (long) teamCount * capacity) {
            throw new IllegalArgumentException("Players exceed total team capacity");
        }
    }

    private static <T> List<List<T>> findAtomicAllocation(List<List<T>> sourceGroups, int teamCount,
                                                           int capacity, int requiredActiveTeams,
                                                           Random random) {
        List<List<T>> groups = new ArrayList<>();
        int playerCount = 0;
        for (List<T> sourceGroup : sourceGroups) {
            if (sourceGroup == null || sourceGroup.isEmpty()) continue;
            playerCount += sourceGroup.size();
            if (sourceGroup.size() > capacity) {
                // An oversized permanent party cannot fit in one team. Split
                // it into random capacity-sized blocks so each block remains
                // together instead of scattering every member independently.
                List<T> shuffledGroup = new ArrayList<>(sourceGroup);
                Collections.shuffle(shuffledGroup, random);
                for (int start = 0; start < shuffledGroup.size(); start += capacity) {
                    groups.add(new ArrayList<>(shuffledGroup.subList(
                            start, Math.min(start + capacity, shuffledGroup.size()))));
                }
            } else {
                groups.add(new ArrayList<>(sourceGroup));
            }
        }

        Collections.shuffle(groups, random);
        groups.sort(Comparator.comparingInt((List<T> group) -> group.size()).reversed());

        int minimumActiveTeams = Math.max(requiredActiveTeams, (playerCount + capacity - 1) / capacity);
        for (int activeTeams = minimumActiveTeams; activeTeams <= teamCount; activeTeams++) {
            List<List<T>> allocation = tryAtomicAllocation(groups, activeTeams, capacity, random);
            if (allocation == null) continue;
            while (allocation.size() < teamCount) allocation.add(new ArrayList<>());
            return allocation;
        }
        return null;
    }

    private static <T> List<List<T>> tryAtomicAllocation(List<List<T>> groups, int teamCount,
                                                          int capacity, Random random) {
        int[] loads = new int[teamCount];
        int[] groupTeams = new int[groups.size()];
        if (!calculateGroupPlacement(
                groups, 0, capacity, teamCount, loads, groupTeams, random)) return null;

        List<List<T>> teams = emptyTeams(teamCount);
        for (int index = 0; index < groups.size(); index++) {
            teams.get(groupTeams[index]).addAll(groups.get(index));
        }
        return teams;
    }

    private static <T> List<List<T>> emptyTeams(int teamCount) {
        List<List<T>> teams = new ArrayList<>(teamCount);
        for (int index = 0; index < teamCount; index++) teams.add(new ArrayList<>());
        return teams;
    }

    private static <T, K> Map<K, List<T>> mapToConfiguredTeams(List<List<T>> allocation,
                                                               List<K> configuredTeams, Random random) {
        List<K> availableTeams = new ArrayList<>(configuredTeams);
        Collections.shuffle(availableTeams, random);
        Map<K, List<T>> result = new LinkedHashMap<>();
        for (int index = 0; index < allocation.size(); index++) {
            result.put(availableTeams.get(index), allocation.get(index));
        }
        return result;
    }

    private static <T> boolean calculateGroupPlacement(List<List<T>> groups, int groupIndex,
                                                        int capacity, int requiredActiveTeams,
                                                        int[] loads, int[] groupTeams,
                                                        Random random) {
        if (groupIndex == groups.size()) {
            return activeTeamCount(loads) >= requiredActiveTeams;
        }
        if (activeTeamCount(loads) + groups.size() - groupIndex < requiredActiveTeams) return false;

        int groupSize = groups.get(groupIndex).size();
        List<Integer> candidates = new ArrayList<>();
        for (int teamIndex = 0; teamIndex < loads.length; teamIndex++) {
            if (loads[teamIndex] + groupSize <= capacity) candidates.add(teamIndex);
        }
        Collections.shuffle(candidates, random);
        candidates.sort(Comparator.comparingInt((Integer teamIndex) -> loads[teamIndex]).reversed());

        Integer previousLoad = null;
        for (int teamIndex : candidates) {
            int currentLoad = loads[teamIndex];
            if (previousLoad != null && previousLoad == currentLoad) continue;
            previousLoad = currentLoad;
            loads[teamIndex] += groupSize;
            groupTeams[groupIndex] = teamIndex;
            if (calculateGroupPlacement(groups, groupIndex + 1, capacity,
                    requiredActiveTeams, loads, groupTeams, random)) {
                return true;
            }
            loads[teamIndex] -= groupSize;
        }
        return false;
    }

    private static int activeTeamCount(int[] loads) {
        int activeTeams = 0;
        for (int load : loads) {
            if (load > 0) activeTeams++;
        }
        return activeTeams;
    }
}
