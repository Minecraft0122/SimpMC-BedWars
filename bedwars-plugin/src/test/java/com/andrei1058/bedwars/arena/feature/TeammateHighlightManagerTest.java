package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeammateHighlightManagerTest {

    private final TeammateHighlightManager manager = TeammateHighlightManager.getInstance();

    @AfterEach
    void clearHighlights() {
        manager.clearAll();
    }

    @Test
    void togglesTeammateGlowAndRestoresTheOriginalState() {
        MutablePlayer viewer = player("viewer", true, false, false);
        MutablePlayer teammate = player("teammate", true, false, false);
        IArena arena = arena(viewer.player, List.of(viewer.player, teammate.player), Set.of());

        TeammateHighlightManager.ToggleResult enabled = manager.toggle(viewer.player, arena);
        assertEquals(TeammateHighlightManager.Outcome.ENABLED, enabled.outcome());
        assertEquals(List.of(teammate.player), enabled.teammates());
        assertTrue(teammate.glowing[0]);

        TeammateHighlightManager.ToggleResult disabled = manager.toggle(viewer.player, arena);
        assertEquals(TeammateHighlightManager.Outcome.DISABLED, disabled.outcome());
        assertFalse(teammate.glowing[0]);
    }

    @Test
    void reportsNoTeammatesForASoloTeam() {
        MutablePlayer viewer = player("solo", true, false, false);
        IArena arena = arena(viewer.player, List.of(viewer.player), Set.of());

        assertEquals(TeammateHighlightManager.Outcome.NO_TEAMMATES,
                manager.toggle(viewer.player, arena).outcome());
    }

    @Test
    void filtersOfflineDeadSpectatingAndRespawningMembers() {
        MutablePlayer viewer = player("viewer", true, false, false);
        MutablePlayer valid = player("valid", true, false, false);
        MutablePlayer offline = player("offline", false, false, false);
        MutablePlayer dead = player("dead", true, true, false);
        MutablePlayer respawning = player("respawning", true, false, true);
        IArena arena = arena(viewer.player,
                List.of(viewer.player, valid.player, offline.player, dead.player, respawning.player),
                Set.of(respawning.player.getUniqueId()));

        assertEquals(List.of(valid.player),
                TeammateHighlightManager.eligibleTeammates(arena, viewer.player));
    }

    private static IArena arena(Player viewer, List<Player> members, Set<UUID> respawning) {
        Set<Player> players = Set.copyOf(members);
        ITeam team = (ITeam) Proxy.newProxyInstance(ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getMembers")) return members;
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "team";
                    throw new UnsupportedOperationException(method.getName());
                });
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStatus" -> GameState.playing;
                    case "isPlayer" -> players.contains(args[0]);
                    case "isSpectator" -> false;
                    case "isReSpawning" -> {
                        UUID id = args[0] instanceof UUID uuid ? uuid : ((Player) args[0]).getUniqueId();
                        yield respawning.contains(id);
                    }
                    case "getTeam" -> team;
                    case "getArenaName" -> "test";
                    case "getWorldName" -> "world";
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "arena";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static MutablePlayer player(String name, boolean online, boolean dead, boolean respawning) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        boolean[] glowing = {false};
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getName" -> name;
                    case "isOnline" -> online;
                    case "isDead" -> dead;
                    case "isGlowing" -> glowing[0];
                    case "setGlowing" -> {
                        glowing[0] = (Boolean) args[0];
                        yield null;
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new MutablePlayer(player, glowing);
    }

    private record MutablePlayer(Player player, boolean[] glowing) {
    }
}
