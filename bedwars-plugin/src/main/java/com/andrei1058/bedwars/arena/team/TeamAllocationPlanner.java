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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * Capacity-aware allocator. It spreads groups over every configured team,
 * keeps normal squads together and treats pre-game team choices as preferences.
 * Only a group larger than one team's capacity may be split.
 */
final class TeamAllocationPlanner {

    private TeamAllocationPlanner() {
    }

    static <T> List<List<T>> allocate(@NotNull List<List<T>> sourceGroups, int teamCount,
                                      int capacity, @NotNull Random random) {
        validateCapacity(sourceGroups, teamCount, capacity);

        List<List<T>> allocation = findAtomicAllocation(sourceGroups, teamCount, capacity, random, false);
        if (allocation != null) return allocation;

        // A group larger than maxInTeam cannot remain atomic. This fallback is
        // also useful to custom/debug callers that supplied impossible groups.
        allocation = findAtomicAllocation(sourceGroups, teamCount, capacity, random, true);
        if (allocation == null) throw new IllegalStateException("No team allocation could be created");
        return allocation;
    }

    static <T, K> Map<K, List<T>> allocateBalanced(@NotNull List<List<T>> sourceGroups,
                                                    @NotNull List<K> configuredTeams,
                                                    int capacity, int requiredActiveTeams,
                                                    @NotNull Random random,
                                                    @NotNull Function<T, K> preferredTeam) {
        if (configuredTeams.isEmpty() || requiredActiveTeams < 1
                || requiredActiveTeams > configuredTeams.size()) {
            return Map.of();
        }
        validateCapacity(sourceGroups, configuredTeams.size(), capacity);

        Map<K, List<T>> preferred = findPreferredAllocation(
                sourceGroups, configuredTeams, capacity, random, preferredTeam);
        if (preferred != null && hasActiveTeams(preferred.values(), requiredActiveTeams)) return preferred;

        List<List<T>> fallback = findAtomicAllocation(
                sourceGroups, configuredTeams.size(), capacity, random, false);
        if (fallback == null) {
            fallback = findAtomicAllocation(sourceGroups, configuredTeams.size(), capacity, random, true);
        }
        if (fallback == null || !hasActiveTeams(fallback, requiredActiveTeams)) return Map.of();
        return mapToConfiguredTeams(fallback, configuredTeams, random, preferredTeam);
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

    private static boolean hasActiveTeams(Iterable<? extends List<?>> teams, int required) {
        int active = 0;
        for (List<?> team : teams) {
            if (team != null && !team.isEmpty() && ++active >= required) return true;
        }
        return false;
    }

    private static <T> List<List<T>> findAtomicAllocation(List<List<T>> sourceGroups, int teamCount,
                                                           int capacity, Random random,
                                                           boolean splitEveryGroup) {
        List<List<T>> parties = new ArrayList<>();
        List<T> soloPlayers = new ArrayList<>();
        for (List<T> sourceGroup : sourceGroups) {
            if (sourceGroup == null || sourceGroup.isEmpty()) continue;
            if (splitEveryGroup || sourceGroup.size() == 1 || sourceGroup.size() > capacity) {
                soloPlayers.addAll(sourceGroup);
            } else {
                parties.add(new ArrayList<>(sourceGroup));
            }
        }

        Collections.shuffle(parties, random);
        parties.sort(Comparator.comparingInt((List<T> group) -> group.size()).reversed());
        Collections.shuffle(soloPlayers, random);

        int[] loads = new int[teamCount];
        int[] partyTeams = new int[parties.size()];
        Arrays.fill(partyTeams, -1);
        if (!calculatePartyPlacement(parties, 0, capacity, loads, partyTeams, random)) return null;

        List<List<T>> teams = emptyTeams(teamCount);
        for (int index = 0; index < parties.size(); index++) {
            teams.get(partyTeams[index]).addAll(parties.get(index));
        }
        distributeSoloPlayers(teams, soloPlayers, capacity, random);
        return teams;
    }

    private static <T, K> Map<K, List<T>> findPreferredAllocation(List<List<T>> sourceGroups,
                                                                   List<K> configuredTeams,
                                                                   int capacity, Random random,
                                                                   Function<T, K> preferredTeam) {
        List<List<T>> teams = emptyTeams(configuredTeams.size());
        List<List<T>> parties = new ArrayList<>();
        List<T> soloPlayers = new ArrayList<>();

        for (List<T> group : sourceGroups) {
            if (group == null || group.isEmpty()) continue;
            GroupPreference<K> preference = commonPreference(group, preferredTeam);
            if (preference.conflicting() || group.size() > capacity) return null;
            if (preference.team() != null) {
                int index = configuredTeams.indexOf(preference.team());
                if (index < 0 || teams.get(index).size() + group.size() > capacity) return null;
                teams.get(index).addAll(group);
            } else if (group.size() == 1) {
                soloPlayers.add(group.getFirst());
            } else {
                parties.add(new ArrayList<>(group));
            }
        }

        Collections.shuffle(parties, random);
        parties.sort(Comparator.comparingInt((List<T> group) -> group.size()).reversed());
        Collections.shuffle(soloPlayers, random);

        int[] loads = teams.stream().mapToInt(List::size).toArray();
        int[] partyTeams = new int[parties.size()];
        Arrays.fill(partyTeams, -1);
        if (!calculatePartyPlacement(parties, 0, capacity, loads, partyTeams, random)) return null;
        for (int index = 0; index < parties.size(); index++) {
            teams.get(partyTeams[index]).addAll(parties.get(index));
        }
        distributeSoloPlayers(teams, soloPlayers, capacity, random);

        Map<K, List<T>> result = new LinkedHashMap<>();
        for (int index = 0; index < configuredTeams.size(); index++) {
            result.put(configuredTeams.get(index), teams.get(index));
        }
        return result;
    }

    private static <T> List<List<T>> emptyTeams(int teamCount) {
        List<List<T>> teams = new ArrayList<>(teamCount);
        for (int index = 0; index < teamCount; index++) teams.add(new ArrayList<>());
        return teams;
    }

    private static <T> void distributeSoloPlayers(List<List<T>> teams, List<T> soloPlayers,
                                                   int capacity, Random random) {
        for (T player : soloPlayers) {
            List<Integer> available = fittingTeams(teams, capacity, 1);
            teams.get(randomLeastFilled(teams, available, random)).add(player);
        }
    }

    private static <T, K> GroupPreference<K> commonPreference(List<T> group,
                                                              Function<T, K> preferredTeam) {
        K common = null;
        for (T player : group) {
            K preference = preferredTeam.apply(player);
            if (preference == null) continue;
            if (common != null && !common.equals(preference)) return new GroupPreference<>(null, true);
            common = preference;
        }
        return new GroupPreference<>(common, false);
    }

    private static <T, K> Map<K, List<T>> mapToConfiguredTeams(List<List<T>> allocation,
                                                               List<K> configuredTeams, Random random,
                                                               Function<T, K> preferredTeam) {
        List<K> availableTeams = new ArrayList<>(configuredTeams);
        Collections.shuffle(availableTeams, random);
        Map<Integer, K> assigned = new HashMap<>();
        Set<K> used = new LinkedHashSet<>();

        List<PreferenceScore<K>> scores = new ArrayList<>();
        for (int allocationIndex = 0; allocationIndex < allocation.size(); allocationIndex++) {
            Map<K, Integer> counts = new LinkedHashMap<>();
            for (T player : allocation.get(allocationIndex)) {
                K preference = preferredTeam.apply(player);
                if (preference != null && configuredTeams.contains(preference)) {
                    counts.merge(preference, 1, Integer::sum);
                }
            }
            int index = allocationIndex;
            counts.forEach((team, score) -> scores.add(new PreferenceScore<>(index, team, score)));
        }
        scores.sort(Comparator.comparingInt((PreferenceScore<K> score) -> score.score()).reversed());
        for (PreferenceScore<K> score : scores) {
            if (assigned.containsKey(score.allocationIndex()) || used.contains(score.team())) continue;
            assigned.put(score.allocationIndex(), score.team());
            used.add(score.team());
        }

        availableTeams.removeAll(used);
        Map<K, List<T>> result = new LinkedHashMap<>();
        int remainingIndex = 0;
        for (int index = 0; index < allocation.size(); index++) {
            K team = assigned.get(index);
            if (team == null) team = availableTeams.get(remainingIndex++);
            result.put(team, allocation.get(index));
        }
        return result;
    }

    private static <T> boolean calculatePartyPlacement(List<List<T>> parties, int partyIndex,
                                                        int capacity, int[] loads,
                                                        int[] partyTeams, Random random) {
        if (partyIndex == parties.size()) return true;

        int partySize = parties.get(partyIndex).size();
        List<Integer> candidates = new ArrayList<>();
        for (int teamIndex = 0; teamIndex < loads.length; teamIndex++) {
            if (loads[teamIndex] + partySize <= capacity) candidates.add(teamIndex);
        }
        Collections.shuffle(candidates, random);
        candidates.sort(Comparator.comparingInt(teamIndex -> loads[teamIndex]));

        Integer previousLoad = null;
        for (int teamIndex : candidates) {
            int currentLoad = loads[teamIndex];
            if (previousLoad != null && previousLoad == currentLoad) continue;
            previousLoad = currentLoad;
            loads[teamIndex] += partySize;
            partyTeams[partyIndex] = teamIndex;
            if (calculatePartyPlacement(parties, partyIndex + 1, capacity, loads, partyTeams, random)) {
                return true;
            }
            partyTeams[partyIndex] = -1;
            loads[teamIndex] -= partySize;
        }
        return false;
    }

    private static <T> List<Integer> fittingTeams(List<List<T>> teams, int capacity, int required) {
        List<Integer> fitting = new ArrayList<>();
        for (int index = 0; index < teams.size(); index++) {
            if (teams.get(index).size() + required <= capacity) fitting.add(index);
        }
        return fitting;
    }

    private static <T> int randomLeastFilled(List<List<T>> teams, List<Integer> candidates, Random random) {
        if (candidates.isEmpty()) throw new IllegalStateException("No team capacity remains");
        int minimum = candidates.stream().mapToInt(index -> teams.get(index).size()).min().orElseThrow();
        List<Integer> leastFilled = candidates.stream()
                .filter(index -> teams.get(index).size() == minimum)
                .toList();
        return leastFilled.get(random.nextInt(leastFilled.size()));
    }

    private record PreferenceScore<K>(int allocationIndex, K team, int score) {
    }

    private record GroupPreference<K>(K team, boolean conflicting) {
    }
}
