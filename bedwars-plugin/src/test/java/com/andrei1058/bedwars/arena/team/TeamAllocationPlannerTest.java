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
    void keepsInvitedSquadTogetherAndBalancesSoloPlayers() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("Alice", "Bob"), List.of("Carol"), List.of("Dave"), List.of("Eve")),
                3, 2, new Random(7));

        List<String> invitedTeam = allocation.stream()
                .filter(team -> team.contains("Alice")).findFirst().orElseThrow();
        assertTrue(invitedTeam.contains("Bob"));
        assertEquals(Set.of("Alice", "Bob", "Carol", "Dave", "Eve"), flatten(allocation));
        assertTrue(allocation.stream().mapToInt(List::size).max().orElseThrow()
                - allocation.stream().mapToInt(List::size).min().orElseThrow() <= 1);
    }

    @Test
    void splitsOnlyAGroupThatCannotFitInOneTeam() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("A", "B", "C", "D")), 2, 2, new Random(3));

        assertTrue(allocation.stream().allMatch(team -> team.size() == 2));
        assertEquals(Set.of("A", "B", "C", "D"), flatten(allocation));
    }

    @Test
    void rejectsPlayersBeyondArenaCapacity() {
        assertThrows(IllegalArgumentException.class, () -> TeamAllocationPlanner.allocate(
                List.of(List.of("A", "B", "C")), 1, 2, new Random(1)));
    }

    @Test
    void spreadsSoloPlayersAcrossAsManyConfiguredTeamsAsPossible() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("A"), List.of("B"), List.of("C"), List.of("D")),
                8, 4, new Random(19));

        assertEquals(4, allocation.stream().filter(team -> !team.isEmpty()).count());
        assertTrue(allocation.stream().allMatch(team -> team.size() <= 1));
    }

    @Test
    void doesNotSplitAValidSquadJustToInventAnOpponent() {
        Map<String, List<String>> allocation = TeamAllocationPlanner.allocateBalanced(
                List.of(List.of("A", "B")), List.of("Red", "Blue", "Green"),
                4, 2, new Random(5), ignored -> null);

        assertTrue(allocation.isEmpty());
    }

    @Test
    void honoursCompatibleTeamChoices() {
        List<List<String>> groups = List.of(
                List.of("A", "B"), List.of("C"), List.of("D"), List.of("E"));
        Map<String, String> choices = Map.of("A", "Red", "B", "Red", "D", "Blue");

        Map<String, List<String>> allocation = TeamAllocationPlanner.allocateBalanced(
                groups, List.of("Red", "Blue", "Green", "Yellow"),
                3, 2, new Random(43), choices::get);

        assertTrue(allocation.get("Red").containsAll(List.of("A", "B")));
        assertTrue(allocation.get("Blue").contains("D"));
        assertEquals(Set.of("A", "B", "C", "D", "E"), flatten(allocation.values().stream().toList()));
    }

    @Test
    void impossibleChoicesCannotBlockAnOtherwiseValidRound() {
        List<List<String>> groups = List.of(List.of("A", "B"), List.of("C", "D"));
        Map<String, String> choices = Map.of("A", "Red", "B", "Blue");

        Map<String, List<String>> allocation = TeamAllocationPlanner.allocateBalanced(
                groups, List.of("Red", "Blue"), 2, 2, new Random(47), choices::get);

        assertEquals(2, allocation.values().stream().filter(team -> !team.isEmpty()).count());
        assertEquals(Set.of("A", "B", "C", "D"), flatten(allocation.values().stream().toList()));
    }

    @Test
    void oneTeamPreferenceFallsBackToBalancedOpponents() {
        Map<String, String> choices = Map.of("A", "Red", "B", "Red");

        Map<String, List<String>> allocation = TeamAllocationPlanner.allocateBalanced(
                List.of(List.of("A"), List.of("B")), List.of("Red", "Blue", "Green"),
                2, 2, new Random(53), choices::get);

        assertEquals(2, allocation.values().stream().filter(team -> !team.isEmpty()).count());
    }

    private static Set<String> flatten(List<List<String>> allocation) {
        Set<String> players = new HashSet<>();
        allocation.forEach(players::addAll);
        return players;
    }
}
