package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvisibilityManagerTest {

    @Test
    void hidesEquipmentFromOpponentsAndSpectatorsOnly() {
        Player invisible = player("invisible");
        Player teammate = player("teammate");
        Player opponent = player("opponent");
        Player spectator = player("spectator");
        Player outsider = player("outsider");
        ITeam red = team("red");
        ITeam blue = team("blue");
        IArena arena = arena(
                Set.of(invisible, teammate, opponent),
                Set.of(spectator),
                Map.of(invisible, red, teammate, red, opponent, blue),
                Set.of(),
                Set.of(invisible));

        assertTrue(InvisibilityManager.shouldHideEquipment(arena, invisible, opponent));
        assertTrue(InvisibilityManager.shouldHideEquipment(arena, invisible, spectator));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, invisible, teammate));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, invisible, invisible));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, invisible, outsider));
    }

    @Test
    void hidesRespawningPlayerEquipmentFromEveryArenaViewer() {
        Player respawning = player("respawning");
        Player teammate = player("teammate");
        Player opponent = player("opponent");
        Player spectator = player("spectator");
        Player outsider = player("outsider");
        ITeam red = team("red");
        ITeam blue = team("blue");
        IArena arena = arena(
                Set.of(respawning, teammate, opponent),
                Set.of(spectator),
                Map.of(respawning, red, teammate, red, opponent, blue),
                Set.of(respawning),
                Set.of());

        assertTrue(InvisibilityManager.hasHiddenEquipment(arena, respawning));
        assertTrue(InvisibilityManager.shouldHideEquipment(arena, respawning, teammate));
        assertTrue(InvisibilityManager.shouldHideEquipment(arena, respawning, opponent));
        assertTrue(InvisibilityManager.shouldHideEquipment(arena, respawning, spectator));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, respawning, respawning));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, respawning, outsider));
    }

    @Test
    void combinesPotionAndRespawnPlayersWhenSynchronizingANewViewer() {
        Player potionUser = player("potion-user");
        Player respawning = player("respawning");
        Player viewer = player("viewer");
        IArena arena = arena(
                Set.of(potionUser, respawning, viewer),
                Set.of(),
                Map.of(),
                Set.of(respawning),
                Set.of(potionUser));

        assertEquals(Set.of(potionUser, respawning), InvisibilityManager.hiddenEquipmentPlayers(arena));
    }

    private static IArena arena(Set<Player> players, Set<Player> spectators, Map<Player, ITeam> teams,
                                Set<Player> respawning, Set<Player> potionUsers) {
        ConcurrentHashMap<Player, Integer> respawnSessions = new ConcurrentHashMap<>();
        respawning.forEach(player -> respawnSessions.put(player, 5));
        ConcurrentHashMap<Player, Integer> showTime = new ConcurrentHashMap<>();
        potionUsers.forEach(player -> showTime.put(player, 5));
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isPlayer" -> players.contains(args[0]);
                    case "isSpectator" -> spectators.contains(args[0]);
                    case "isReSpawning" -> respawning.contains(args[0]);
                    case "getTeam" -> teams.get(args[0]);
                    case "getShowTime" -> showTime;
                    case "getRespawnSessions" -> respawnSessions;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ITeam team(String name) {
        return (ITeam) Proxy.newProxyInstance(ITeam.class.getClassLoader(), new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player(String name) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
