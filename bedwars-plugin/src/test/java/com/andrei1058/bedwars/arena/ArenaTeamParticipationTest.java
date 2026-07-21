package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaTeamParticipationTest {

    @Test
    void snapshotSurvivesEliminationAndResetsBetweenGames() {
        List<Object> redMembers = new ArrayList<>();
        redMembers.add(new Object());
        List<Object> blueMembers = new ArrayList<>();
        ITeam red = team("Red", redMembers);
        ITeam blue = team("Blue", blueMembers);
        ArenaTeamParticipation participation = new ArenaTeamParticipation();

        participation.capture(List.of(red, blue));
        assertEquals(List.of(red), participation.activeTeams(List.of(red, blue)));

        redMembers.clear();
        assertEquals(List.of(red), participation.activeTeams(List.of(red, blue)));

        participation.reset();
        assertTrue(participation.activeTeams(List.of(red, blue)).isEmpty());
    }

    private static ITeam team(String name, List<Object> members) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIdentity" -> identity;
                    case "getMembers" -> members;
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
