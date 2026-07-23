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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Capacity-aware randomized allocator used by the arena team assigner.
 * Groups that fit are never split; oversized or otherwise unplaceable groups
 * fall back to randomized single-player placement.
 */
final class TeamAllocationPlanner {

    private TeamAllocationPlanner() {
    }

    static <T> List<List<T>> allocateWithMinimum(@NotNull List<List<T>> sourceGroups,
                                                  int configuredTeamCount, int minimumInTeam,
                                                  int capacity, @NotNull Random random) {
        int players = sourceGroups.stream().filter(group -> group != null).mapToInt(List::size).sum();
        int maximumTeams = ArenaStartPolicy.maximumFeasibleActiveTeams(players, configuredTeamCount,
                minimumInTeam, capacity);
        for (int teamCount = maximumTeams; teamCount >= 2; teamCount--) {
            if (!ArenaStartPolicy.isFeasibleActiveTeamCount(players, teamCount, minimumInTeam, capacity)) {
                continue;
            }
            List<List<T>> allocation = allocate(sourceGroups, teamCount, capacity, random);
            if (allocation.stream().allMatch(team -> team.size() >= minimumInTeam)) return allocation;
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

        List<List<T>> groups = new ArrayList<>();
        for (List<T> group : sourceGroups) {
            if (group != null && !group.isEmpty()) groups.add(new ArrayList<>(group));
        }
        Collections.shuffle(groups, random);
        groups.sort(Comparator.comparingInt((List<T> group) -> group.size()).reversed());

        List<List<T>> teams = new ArrayList<>(teamCount);
        for (int index = 0; index < teamCount; index++) teams.add(new ArrayList<>());

        for (List<T> group : groups) {
            List<Integer> fitting = fittingTeams(teams, capacity, group.size());
            if (!fitting.isEmpty()) {
                teams.get(randomLeastFilled(teams, fitting, random)).addAll(group);
                continue;
            }

            // This only happens for an oversized external party or fragmented
            // capacity. Keep assignment valid and distribute those players.
            List<T> shuffled = new ArrayList<>(group);
            Collections.shuffle(shuffled, random);
            for (T player : shuffled) {
                List<Integer> available = fittingTeams(teams, capacity, 1);
                teams.get(randomLeastFilled(teams, available, random)).add(player);
            }
        }
        ensureAtLeastTwoActiveTeams(teams, players, random);
        return teams;
    }

    /**
     * A party may fill one team exactly, but a BedWars round must still have
     * an opponent. Split one player only when keeping the party intact would
     * otherwise create a one-team game.
     */
    private static <T> void ensureAtLeastTwoActiveTeams(List<List<T>> teams, int playerCount, Random random) {
        if (playerCount < 2 || teams.size() < 2
                || teams.stream().filter(team -> !team.isEmpty()).count() >= 2) {
            return;
        }

        List<Integer> emptyTeams = new ArrayList<>();
        for (int index = 0; index < teams.size(); index++) {
            if (teams.get(index).isEmpty()) emptyTeams.add(index);
        }
        List<T> source = teams.stream().filter(team -> team.size() > 1).findFirst().orElseThrow();
        T player = source.remove(random.nextInt(source.size()));
        teams.get(emptyTeams.get(random.nextInt(emptyTeams.size()))).add(player);
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
