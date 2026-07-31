package com.andrei1058.bedwars.arena.team;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAllocationPlannerTest {

    @Test
    void keepsInvitedSquadTogether() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("Alice", "Bob"), List.of("Carol"), List.of("Dave")),
                2,
                2,
                new Random(7)
        );

        List<String> invitedTeam = allocation.stream()
                .filter(team -> team.contains("Alice"))
                .findFirst()
                .orElseThrow();
        assertTrue(invitedTeam.contains("Bob"));
        assertEquals(Set.of("Alice", "Bob", "Carol", "Dave"), flatten(allocation));
    }

    @Test
    void balancesAndRandomlyFillsSoloPlayers() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("A"), List.of("B"), List.of("C"), List.of("D"),
                        List.of("E"), List.of("F"), List.of("G")),
                3,
                3,
                new Random(11)
        );

        int minimum = allocation.stream().mapToInt(List::size).min().orElseThrow();
        int maximum = allocation.stream().mapToInt(List::size).max().orElseThrow();
        assertTrue(maximum - minimum <= 1);
        assertEquals(Set.of("A", "B", "C", "D", "E", "F", "G"), flatten(allocation));
    }

    @Test
    void splitsOnlyAnOversizedExternalParty() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("A", "B", "C", "D")),
                2,
                2,
                new Random(3)
        );

        assertTrue(allocation.stream().allMatch(team -> team.size() == 2));
        assertEquals(Set.of("A", "B", "C", "D"), flatten(allocation));
    }

    @Test
    void doesNotSplitAFullSquadJustToInventAnOpponent() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A", "B")),
                4,
                1,
                2,
                new Random(5)
        );

        assertTrue(allocation.isEmpty());
    }

    @Test
    void rejectsPlayersBeyondArenaCapacity() {
        assertThrows(IllegalArgumentException.class, () -> TeamAllocationPlanner.allocate(
                List.of(List.of("A", "B", "C")),
                1,
                2,
                new Random(1)
        ));
    }

    @Test
    void minimumAwareAllocationUsesOnlyTeamsItCanFill() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A", "B", "C", "D"), List.of("E"), List.of("F")),
                4,
                2,
                4,
                new Random(13)
        );

        assertEquals(2, allocation.size());
        assertTrue(allocation.stream().allMatch(team -> team.size() >= 2));
        assertEquals(Set.of("A", "B", "C", "D", "E", "F"), flatten(allocation));
    }

    @Test
    void keepsSquadsAtomicWhenGreedyPlacementWouldSplitOne() {
        List<List<String>> groups = List.of(
                List.of("A", "B", "C"),
                List.of("D", "E", "F"),
                List.of("G", "H"),
                List.of("I", "J"),
                List.of("K", "L")
        );

        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                groups, 2, 1, 6, new Random(29));

        assertEquals(2, allocation.size());
        assertTrue(allocation.stream().allMatch(team -> team.size() == 6));
        for (List<String> group : groups) {
            assertTrue(allocation.stream().anyMatch(team -> team.containsAll(group)));
        }
    }

    @Test
    void enablesTheMostTeamsAllowedBySquadBoundaries() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A", "B"), List.of("C", "D"), List.of("E"), List.of("F")),
                4, 1, 4, new Random(31));

        assertEquals(4, allocation.size());
        assertEquals(List.of(1, 1, 2, 2), allocation.stream().map(List::size).sorted().toList());
        assertTrue(allocation.stream().anyMatch(team -> team.containsAll(List.of("A", "B"))));
        assertTrue(allocation.stream().anyMatch(team -> team.containsAll(List.of("C", "D"))));
    }

    @Test
    void fallsBackToFewerTeamsInsteadOfBreakingSquads() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A", "B"), List.of("C", "D")),
                4, 1, 4, new Random(37));

        assertEquals(2, allocation.size());
        assertTrue(allocation.stream().allMatch(team -> team.size() == 2));
    }

    @Test
    void minimumAwareAllocationRejectsAnUnfillableOpponentTeam() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A", "B", "C"), List.of("D")),
                4,
                2,
                4,
                new Random(17)
        );

        assertTrue(allocation.isEmpty());
    }

    @Test
    void startsTwoSoloTeamsBeforeTheArenaIsFull() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A"), List.of("B")),
                4,
                1,
                2,
                new Random(19)
        );

        assertEquals(2, allocation.size());
        assertTrue(allocation.stream().allMatch(team -> team.size() == 1));
        assertEquals(Set.of("A", "B"), flatten(allocation));
    }

    @Test
    void fillsTwoThreePlayerTeamsWhenRangeIsThreeToFour() {
        List<List<String>> groups = List.of(
                List.of("A"), List.of("B"), List.of("C"),
                List.of("D"), List.of("E"), List.of("F")
        );

        assertTrue(TeamAllocationPlanner.allocateWithMinimum(
                groups.subList(0, 5), 8, 3, 4, new Random(23)
        ).isEmpty());

        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                groups, 8, 3, 4, new Random(23)
        );

        assertEquals(2, allocation.size());
        assertTrue(allocation.stream().allMatch(team -> team.size() == 3));
        assertEquals(Set.of("A", "B", "C", "D", "E", "F"), flatten(allocation));
    }

    @Test
    void spreadsNineSoloPlayersAcrossThreeMinimumTeams() {
        List<List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                List.of(List.of("A"), List.of("B"), List.of("C"), List.of("D"), List.of("E"),
                        List.of("F"), List.of("G"), List.of("H"), List.of("I")),
                8, 3, 4, new Random(41));

        assertEquals(3, allocation.size());
        assertTrue(allocation.stream().allMatch(team -> team.size() == 3));
    }

    @Test
    void honoursCompatibleNamedTeamChoicesAndStillMaximizesTeams() {
        List<List<String>> groups = List.of(
                List.of("A"), List.of("B"), List.of("C"), List.of("D"), List.of("E"),
                List.of("F"), List.of("G"), List.of("H"), List.of("I"));
        Map<String, String> choices = Map.of("A", "Red", "B", "Red", "D", "Blue");

        Map<String, List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                groups, List.of("Red", "Blue", "Green", "Yellow"), 3, 4,
                new Random(43), choices::get);

        assertEquals(3, allocation.size());
        assertTrue(allocation.get("Red").containsAll(List.of("A", "B")));
        assertTrue(allocation.get("Blue").contains("D"));
        assertTrue(allocation.values().stream().allMatch(team -> team.size() >= 3 && team.size() <= 4));
    }

    @Test
    void conflictingChoicesCannotBlockAnOtherwiseValidRound() {
        List<List<String>> groups = List.of(List.of("A", "B"), List.of("C", "D"));
        Map<String, String> choices = Map.of("A", "Red", "B", "Blue");

        Map<String, List<String>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                groups, List.of("Red", "Blue"), 2, 2, new Random(47), choices::get);

        assertEquals(2, allocation.size());
        assertTrue(allocation.values().stream().allMatch(team -> team.size() == 2));
        assertEquals(Set.of("A", "B", "C", "D"), flatten(new java.util.ArrayList<>(allocation.values())));
    }

    @Test
    void soloAllocationMatchesTheCalculatedTeamCountForEveryRange() {
        List<String> configuredTeams = List.of("T1", "T2", "T3", "T4", "T5", "T6");
        for (int minimum = 1; minimum <= 4; minimum++) {
            for (int maximum = minimum; maximum <= 6; maximum++) {
                for (int players = 0; players <= 36; players++) {
                    List<List<Integer>> groups = java.util.stream.IntStream.range(0, players)
                            .mapToObj(List::of)
                            .toList();
                    Map<String, List<Integer>> allocation = TeamAllocationPlanner.allocateWithMinimum(
                            groups, configuredTeams, minimum, maximum, new Random(53), ignored -> null);
                    int expectedTeams = com.andrei1058.bedwars.arena.ArenaStartPolicy
                            .maximumFeasibleActiveTeams(players, configuredTeams.size(), minimum, maximum);

                    assertEquals(expectedTeams, allocation.size(),
                            "players=" + players + ", range=" + minimum + ".." + maximum);
                    int lowerBound = minimum;
                    int upperBound = maximum;
                    assertTrue(allocation.values().stream()
                            .allMatch(team -> team.size() >= lowerBound && team.size() <= upperBound));
                }
            }
        }
    }

    private static Set<String> flatten(List<List<String>> allocation) {
        Set<String> players = new HashSet<>();
        allocation.forEach(players::addAll);
        return players;
    }
}
