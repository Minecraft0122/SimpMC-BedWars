package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
                Map.of(invisible, red, teammate, red, opponent, blue));

        assertTrue(InvisibilityManager.shouldHideEquipment(arena, invisible, opponent));
        assertTrue(InvisibilityManager.shouldHideEquipment(arena, invisible, spectator));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, invisible, teammate));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, invisible, invisible));
        assertFalse(InvisibilityManager.shouldHideEquipment(arena, invisible, outsider));
    }

    private static IArena arena(Set<Player> players, Set<Player> spectators, Map<Player, ITeam> teams) {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isPlayer" -> players.contains(args[0]);
                    case "isSpectator" -> spectators.contains(args[0]);
                    case "getTeam" -> teams.get(args[0]);
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
