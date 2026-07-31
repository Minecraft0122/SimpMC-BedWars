package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaTeamParticipationTest {

    @Test
    void snapshotSurvivesEliminationAndResetsBetweenGames() {
        Player redPlayer = player("RedPlayer");
        List<Player> redMembers = new java.util.ArrayList<>();
        redMembers.add(redPlayer);
        List<Player> blueMembers = new java.util.ArrayList<>();
        ITeam red = team("Red", redMembers);
        ITeam blue = team("Blue", blueMembers);
        ArenaTeamParticipation participation = new ArenaTeamParticipation();

        participation.capture(List.of(red, blue));
        assertEquals(List.of(red), participation.activeTeams(List.of(red, blue)));
        assertEquals(1, participation.gameStartSize(red));
        assertEquals(0, participation.gameStartSize(blue));
        assertSame(red, participation.gameStartTeam(redPlayer.getUniqueId()));

        redMembers.clear();
        assertEquals(List.of(red), participation.activeTeams(List.of(red, blue)));
        assertEquals(1, participation.gameStartSize(red));

        participation.reset();
        assertTrue(participation.activeTeams(List.of(red, blue)).isEmpty());
        assertEquals(0, participation.gameStartSize(red));
        assertNull(participation.gameStartTeam(redPlayer.getUniqueId()));
    }

    private static ITeam team(String name, List<Player> members) {
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

    private static Player player(String name) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> identity;
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
