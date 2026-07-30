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
import java.util.List;
import java.util.Random;

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

        int soloIndex = 0;
        for (List<T> team : teams) {
            while (team.size() < minimumInTeam) team.add(soloPlayers.get(soloIndex++));
        }
        while (soloIndex < soloPlayers.size()) {
            List<Integer> available = fittingTeams(teams, capacity, 1);
            teams.get(randomLeastFilled(teams, available, random)).add(soloPlayers.get(soloIndex++));
        }
        return teams;
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
