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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.arena.ArenaStartPolicy;
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
import java.util.stream.Collectors;

/**
 * Capacity-aware randomized allocator used by the arena team assigner.
 * Normal-sized squads are atomic: matchmaking either finds a valid placement
 * for the whole squad or waits for more players. Only an external party that
 * already exceeds one team's capacity is split because keeping it intact is
 * impossible in every legal allocation.
 */
final class TeamAllocationPlanner {

    private TeamAllocationPlanner() {
    }

    static <T> List<List<T>> allocateWithMinimum(@NotNull List<List<T>> sourceGroups,
                                                  int configuredTeamCount, int minimumInTeam,
                                                  int capacity, @NotNull Random random) {
        if (configuredTeamCount < 1 || minimumInTeam < 1 || capacity < minimumInTeam) {
            return List.of();
        }
        int players = sourceGroups.stream().filter(group -> group != null).mapToInt(List::size).sum();
        int maximumTeams = ArenaStartPolicy.maximumFeasibleActiveTeams(players, configuredTeamCount,
                minimumInTeam, capacity);
        for (int teamCount = maximumTeams; teamCount >= 2; teamCount--) {
            if (!ArenaStartPolicy.isFeasibleActiveTeamCount(players, teamCount, minimumInTeam, capacity)) {
                continue;
            }
            List<List<T>> allocation = findAtomicAllocation(sourceGroups, teamCount, minimumInTeam,
                    capacity, random, false);
            if (allocation != null) return allocation;
        }
        return List.of();
    }

    /**
     * Allocate directly onto configured teams. A compatible pre-game choice is
     * honoured, while an impossible choice is treated as a preference and must
     * never prevent an otherwise valid round from starting.
     */
    static <T, K> Map<K, List<T>> allocateWithMinimum(@NotNull List<List<T>> sourceGroups,
                                                       @NotNull List<K> configuredTeams,
                                                       int minimumInTeam, int capacity,
                                                       @NotNull Random random,
                                                       @NotNull Function<T, K> preferredTeam) {
        if (configuredTeams.isEmpty() || minimumInTeam < 1 || capacity < minimumInTeam) {
            return Map.of();
        }

        int players = sourceGroups.stream().filter(group -> group != null).mapToInt(List::size).sum();
        int maximumTeams = ArenaStartPolicy.maximumFeasibleActiveTeams(players, configuredTeams.size(),
                minimumInTeam, capacity);
        for (int teamCount = maximumTeams; teamCount >= 2; teamCount--) {
            if (!ArenaStartPolicy.isFeasibleActiveTeamCount(players, teamCount, minimumInTeam, capacity)) {
                continue;
            }

            Map<K, List<T>> preferred = findPreferredAllocation(sourceGroups, configuredTeams, teamCount,
                    minimumInTeam, capacity, random, preferredTeam);
            if (preferred != null) return preferred;

            List<List<T>> fallback = findAtomicAllocation(sourceGroups, teamCount, minimumInTeam,
                    capacity, random, false);
            if (fallback != null) {
                return mapToConfiguredTeams(fallback, configuredTeams, random, preferredTeam);
            }
        }
        return Map.of();
    }

    static <T> List<List<T>> allocate(@NotNull List<List<T>> sourceGroups, int teamCount,
                                      int capacity, @NotNull Random random) {
        if (teamCount < 1 || capacity < 1) {
            throw new IllegalArgumentException("Team count and capacity must be positive");
        }

        int players = sourceGroups.stream().filter(group -> group != null).mapToInt(List::size).sum();
        if (players > teamCount * capacity) {
            throw new IllegalArgumentException("Players exceed total team capacity");
        }
        List<List<T>> allocation = findAtomicAllocation(sourceGroups, teamCount, 0, capacity, random, false);
        if (allocation != null) return allocation;

        // Debug/custom assignment still has to fit every player. If atomic
        // parties make the requested fixed team count impossible, split them
        // only in this non-matchmaking fallback.
        allocation = findAtomicAllocation(sourceGroups, teamCount, 0, capacity, random, true);
        if (allocation == null) throw new IllegalStateException("No team allocation could be created");
        return allocation;
    }

    private static <T> List<List<T>> findAtomicAllocation(List<List<T>> sourceGroups, int teamCount,
                                                           int minimumInTeam, int capacity, Random random,
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
        int remainingPartyPlayers = parties.stream().mapToInt(List::size).sum();

        if (!calculatePartyPlacement(parties, 0, minimumInTeam, capacity, soloPlayers.size(),
                remainingPartyPlayers, loads, partyTeams, random)) {
            return null;
        }

        List<List<T>> teams = new ArrayList<>(teamCount);
        for (int index = 0; index < teamCount; index++) teams.add(new ArrayList<>());
        for (int index = 0; index < parties.size(); index++) {
            teams.get(partyTeams[index]).addAll(parties.get(index));
        }

        distributeSoloPlayers(teams, soloPlayers, minimumInTeam, capacity, random);
        return teams;
    }

    private static <T, K> Map<K, List<T>> findPreferredAllocation(List<List<T>> sourceGroups,
                                                                   List<K> configuredTeams, int teamCount,
                                                                   int minimumInTeam, int capacity,
                                                                   Random random,
                                                                   Function<T, K> preferredTeam) {
        List<PreferredGroup<T, K>> preferredGroups = new ArrayList<>();
        LinkedHashSet<K> requestedTeams = new LinkedHashSet<>();
        for (List<T> group : sourceGroups) {
            if (group == null || group.isEmpty()) continue;
            GroupPreference<K> groupPreference = commonPreference(group, preferredTeam);
            if (groupPreference.conflicting()) return null;
            K preference = groupPreference.team();
            if (preference != null && !configuredTeams.contains(preference)) return null;
            preferredGroups.add(new PreferredGroup<>(group, preference));
            if (preference != null) requestedTeams.add(preference);
        }
        if (requestedTeams.size() > teamCount) return null;

        List<K> activeTeams = new ArrayList<>(requestedTeams);
        List<K> unrequested = configuredTeams.stream()
                .filter(team -> !requestedTeams.contains(team))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(unrequested, random);
        activeTeams.addAll(unrequested.subList(0, teamCount - activeTeams.size()));

        List<List<T>> teams = new ArrayList<>(teamCount);
        for (int index = 0; index < teamCount; index++) teams.add(new ArrayList<>());
        List<List<T>> parties = new ArrayList<>();
        List<T> soloPlayers = new ArrayList<>();

        for (PreferredGroup<T, K> preferredGroup : preferredGroups) {
            List<T> group = preferredGroup.members();
            K preference = preferredGroup.team();
            if (group.size() > capacity) return null;
            if (preference != null) {
                List<T> selectedTeam = teams.get(activeTeams.indexOf(preference));
                if (selectedTeam.size() + group.size() > capacity) return null;
                selectedTeam.addAll(group);
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
        int partyPlayers = parties.stream().mapToInt(List::size).sum();
        if (!calculatePartyPlacement(parties, 0, minimumInTeam, capacity, soloPlayers.size(),
                partyPlayers, loads, partyTeams, random)) {
            return null;
        }

        for (int index = 0; index < parties.size(); index++) {
            teams.get(partyTeams[index]).addAll(parties.get(index));
        }
        distributeSoloPlayers(teams, soloPlayers, minimumInTeam, capacity, random);

        Map<K, List<T>> result = new LinkedHashMap<>();
        for (K configuredTeam : configuredTeams) {
            int activeIndex = activeTeams.indexOf(configuredTeam);
            if (activeIndex >= 0) result.put(configuredTeam, teams.get(activeIndex));
        }
        return result;
    }

    private static <T> void distributeSoloPlayers(List<List<T>> teams, List<T> soloPlayers,
                                                   int minimumInTeam, int capacity, Random random) {
        int soloIndex = 0;
        for (List<T> team : teams) {
            while (team.size() < minimumInTeam) team.add(soloPlayers.get(soloIndex++));
        }
        while (soloIndex < soloPlayers.size()) {
            List<Integer> available = fittingTeams(teams, capacity, 1);
            teams.get(randomLeastFilled(teams, available, random)).add(soloPlayers.get(soloIndex++));
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

    private record PreferenceScore<K>(int allocationIndex, K team, int score) {
    }

    private record PreferredGroup<T, K>(List<T> members, K team) {
    }

    private record GroupPreference<K>(K team, boolean conflicting) {
    }

    private static <T> boolean calculatePartyPlacement(List<List<T>> parties, int partyIndex,
                                                        int minimumInTeam, int capacity, int soloPlayers,
                                                        int remainingPartyPlayers, int[] loads,
                                                        int[] partyTeams, Random random) {
        int deficit = Arrays.stream(loads).map(load -> Math.max(0, minimumInTeam - load)).sum();
        if (remainingPartyPlayers + soloPlayers < deficit) return false;
        if (partyIndex == parties.size()) return soloPlayers >= deficit;

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
            // Equal-load teams are interchangeable for capacity calculation.
            if (previousLoad != null && previousLoad == currentLoad) continue;
            previousLoad = currentLoad;
            loads[teamIndex] += partySize;
            partyTeams[partyIndex] = teamIndex;
            if (calculatePartyPlacement(parties, partyIndex + 1, minimumInTeam, capacity, soloPlayers,
                    remainingPartyPlayers - partySize, loads, partyTeams, random)) {
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
}
