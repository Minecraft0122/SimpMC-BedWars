package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BwTabListTest {

    @Test
    void activePlayerNameUsesItsOwnTeamColor() {
        ITeam redTeam = (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getColor")) return TeamColor.RED;
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        assertSame(ChatColor.RED, BwTabList.getPlayerListColor(redTeam));
        assertSame(ChatColor.WHITE, BwTabList.getPlayerListColor(null));
    }

    @Test
    void arenaPlayersAreGroupedByTeamAndSortedByName() {
        ITeam red = team("red", TeamColor.RED);
        ITeam blue = team("blue", TeamColor.BLUE);
        Player bob = player("Bob");
        Player charlie = player("charlie");
        Player alice = player("Alice");
        Player adam = player("Adam");
        Player aaron = player("Aaron");
        Player viewer = player("Viewer");

        Map<UUID, ITeam> currentTeams = Map.of(
                bob.getUniqueId(), red,
                alice.getUniqueId(), red,
                charlie.getUniqueId(), blue,
                adam.getUniqueId(), blue
        );
        Map<UUID, ITeam> formerTeams = Map.of(aaron.getUniqueId(), red);
        IArena arena = arena(
                List.of(red, blue),
                List.of(charlie, bob, adam, alice),
                List.of(viewer, aaron),
                currentTeams,
                formerTeams
        );

        assertEquals(
                List.of("Aaron", "Alice", "Bob", "Adam", "charlie", "Viewer"),
                BwTabList.orderedArenaPlayers(arena).stream().map(Player::getName).toList()
        );
    }

    private static ITeam team(String name, TeamColor color) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIdentity" -> identity;
                    case "getColor" -> color;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Player player(String name) {
        UUID uniqueId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> uniqueId;
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static IArena arena(List<ITeam> teams, List<Player> players, List<Player> spectators,
                                Map<UUID, ITeam> currentTeams, Map<UUID, ITeam> formerTeams) {
        return (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTeams" -> teams;
                    case "getPlayers" -> players;
                    case "getSpectators" -> spectators;
                    case "getTeam" -> currentTeams.get(((Player) args[0]).getUniqueId());
                    case "getExTeam" -> formerTeams.get((UUID) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
