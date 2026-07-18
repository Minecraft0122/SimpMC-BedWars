package com.andrei1058.bedwars.arena.team;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
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
    void splitsAFullPartyRatherThanStartingWithOneTeam() {
        List<List<String>> allocation = TeamAllocationPlanner.allocate(
                List.of(List.of("A", "B")),
                4,
                2,
                new Random(5)
        );

        assertEquals(2, allocation.stream().filter(team -> !team.isEmpty()).count());
        assertEquals(Set.of("A", "B"), flatten(allocation));
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

    private static Set<String> flatten(List<List<String>> allocation) {
        Set<String> players = new HashSet<>();
        allocation.forEach(players::addAll);
        return players;
    }
}
