package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidebarTeamPolicyTest {

    @Test
    void hidesTeamsThatWereEmptyWhenPlayingStarted() {
        ITeam red = team("Red");
        ITeam blue = team("Blue");

        assertEquals(List.of(red, blue), SidebarTeamPolicy.displayedTeams(
                arena(GameState.waiting, List.of(red, blue), List.of(red))));
        assertEquals(List.of(red), SidebarTeamPolicy.displayedTeams(
                arena(GameState.playing, List.of(red, blue), List.of(red))));
        assertEquals(List.of(red), SidebarTeamPolicy.displayedTeams(
                arena(GameState.restarting, List.of(red, blue), List.of(red))));
    }

    @Test
    void removesExplicitPlaceholderLinesForHiddenTeams() {
        ITeam red = team("Red");
        ITeam blue = team("Blue");

        assertTrue(SidebarTeamPolicy.referencesHiddenTeam(
                "{TeamBlueColor}{TeamBlueName}", List.of(red, blue), List.of(red)));
        assertFalse(SidebarTeamPolicy.referencesHiddenTeam(
                "{TeamRedColor}{TeamRedName}", List.of(red, blue), List.of(red)));
        assertFalse(SidebarTeamPolicy.referencesHiddenTeam(
                "{team}", List.of(red, blue), List.of(red)));
    }

    private static ITeam team(String name) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIdentity" -> identity;
                    case "getName", "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IArena arena(GameState state, List<ITeam> configured, List<ITeam> active) {
        return (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getStatus" -> state;
                    case "getTeams" -> configured;
                    case "getActiveTeamsAtGameStart" -> active;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
