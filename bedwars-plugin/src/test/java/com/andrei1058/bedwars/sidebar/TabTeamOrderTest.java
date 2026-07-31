package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabTeamOrderTest {

    @Test
    void calculatesVisibleSpectrumOrderFromRgb() {
        List<ITeam> teams = new ArrayList<>(List.of(
                team("dark-gray", TeamColor.DARK_GRAY),
                team("blue", TeamColor.BLUE),
                team("pink", TeamColor.PINK),
                team("white", TeamColor.WHITE),
                team("cyan", TeamColor.CYAN),
                team("dark-green", TeamColor.DARK_GREEN),
                team("red", TeamColor.RED),
                team("gray", TeamColor.GRAY),
                team("green", TeamColor.GREEN),
                team("yellow", TeamColor.YELLOW)
        ));

        teams.sort(TabTeamOrder.COMPARATOR);

        assertEquals(
                List.of(TeamColor.RED, TeamColor.YELLOW, TeamColor.GREEN, TeamColor.DARK_GREEN,
                        TeamColor.CYAN, TeamColor.BLUE, TeamColor.PINK,
                        TeamColor.WHITE, TeamColor.GRAY, TeamColor.DARK_GRAY),
                teams.stream().map(ITeam::getColor).toList()
        );
    }

    @Test
    void usesTeamNameToKeepSameColorTeamsDeterministicAndSeparate() {
        List<ITeam> teams = new ArrayList<>(List.of(
                team("Zulu", TeamColor.RED),
                team("alpha", TeamColor.RED),
                team("Beta", TeamColor.RED)
        ));

        teams.sort(TabTeamOrder.COMPARATOR);

        assertEquals(List.of("alpha", "Beta", "Zulu"), teams.stream().map(ITeam::getName).toList());
    }

    @Test
    @SuppressWarnings("deprecation")
    void keepsLegacyAquaAdjacentToCanonicalCyan() {
        List<ITeam> teams = new ArrayList<>(List.of(
                team("cyan-b", TeamColor.CYAN),
                team("red", TeamColor.RED),
                team("cyan-a", TeamColor.AQUA),
                team("blue", TeamColor.BLUE)
        ));

        teams.sort(TabTeamOrder.COMPARATOR);

        assertEquals(List.of("red", "cyan-a", "cyan-b", "blue"),
                teams.stream().map(ITeam::getName).toList());
    }

    private static ITeam team(String name, TeamColor color) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIdentity" -> identity;
                    case "getColor" -> color;
                    case "getName" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
