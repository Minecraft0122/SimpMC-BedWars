package com.andrei1058.bedwars.arena.despawnables;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.support.version.common.DespawnableTargeting;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TargetListenerTest {

    @Test
    void replacesCloserTeammateWithNearestEnemy() {
        World world = world("arena");
        ITeam red = team("red");
        ITeam blue = team("blue");
        Player teammate = player("teammate", world, 1, false);
        Player closeEnemy = player("close-enemy", world, 4, false);
        Player farEnemy = player("far-enemy", world, 9, false);
        List<Player> players = List.of(teammate, farEnemy, closeEnemy);
        IArena arena = arena(GameState.playing, players, Set.of(), Set.of(),
                Map.of(teammate, red, closeEnemy, blue, farEnemy, blue));

        Player resolved = DespawnableTargeting.resolveTarget(
                arena, red, teammate, new Location(world, 0, 64, 0), players);

        assertSame(closeEnemy, resolved);
    }

    @Test
    void keepsCurrentEnemyInsteadOfRetargetingEveryRefresh() {
        World world = world("arena");
        ITeam red = team("red");
        ITeam blue = team("blue");
        Player currentEnemy = player("current", world, 8, false);
        Player closerEnemy = player("closer", world, 2, false);
        List<Player> players = List.of(currentEnemy, closerEnemy);
        IArena arena = arena(GameState.playing, players, Set.of(), Set.of(),
                Map.of(currentEnemy, blue, closerEnemy, blue));

        Player resolved = DespawnableTargeting.resolveTarget(
                arena, red, currentEnemy, new Location(world, 0, 64, 0), players);

        assertSame(currentEnemy, resolved);
    }

    @Test
    void excludesDeadRespawningSpectatingCrossWorldAndSameTeamPlayers() {
        World world = world("arena");
        World otherWorld = world("other");
        ITeam red = team("red");
        ITeam blue = team("blue");
        Player teammate = player("teammate", world, 1, false);
        Player deadEnemy = player("dead", world, 2, true);
        Player respawningEnemy = player("respawning", world, 3, false);
        Player spectatorEnemy = player("spectator", world, 4, false);
        Player crossWorldEnemy = player("cross-world", otherWorld, 1, false);
        List<Player> players = List.of(teammate, deadEnemy, respawningEnemy, spectatorEnemy, crossWorldEnemy);
        IArena arena = arena(GameState.playing, players, Set.of(spectatorEnemy), Set.of(respawningEnemy),
                Map.of(teammate, red, deadEnemy, blue, respawningEnemy, blue,
                        spectatorEnemy, blue, crossWorldEnemy, blue));

        Player resolved = DespawnableTargeting.findNearestTarget(
                arena, red, new Location(world, 0, 64, 0), players);

        assertNull(resolved);
    }

    @Test
    void clearsTargetsOutsideThePlayingState() {
        World world = world("arena");
        ITeam red = team("red");
        ITeam blue = team("blue");
        Player enemy = player("enemy", world, 2, false);
        IArena arena = arena(GameState.restarting, List.of(enemy), Set.of(), Set.of(), Map.of(enemy, blue));

        Player resolved = DespawnableTargeting.resolveTarget(
                arena, red, enemy, new Location(world, 0, 64, 0), List.of(enemy));

        assertNull(resolved);
    }

    private static IArena arena(GameState state, List<Player> players, Set<Player> spectators,
                                Set<Player> respawning, Map<Player, ITeam> teams) {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getStatus" -> state;
                    case "getPlayers" -> players;
                    case "isPlayer" -> players.contains(args[0]);
                    case "isSpectator" -> spectators.stream()
                            .anyMatch(player -> player.getUniqueId().equals(args[0]));
                    case "isReSpawning" -> respawning.stream()
                            .anyMatch(player -> player.getUniqueId().equals(args[0]));
                    case "getTeam" -> teams.get(args[0]);
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "arena";
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

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player(String name, World world, double x, boolean dead) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        Location location = new Location(world, x, 64, 0);
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getWorld" -> world;
                    case "getLocation" -> location.clone();
                    case "isDead" -> dead;
                    case "getName", "toString" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
