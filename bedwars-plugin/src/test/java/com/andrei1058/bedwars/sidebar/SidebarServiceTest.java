package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.sidebar.ISidebar;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidebarServiceTest {

    @Test
    void keepsLobbyTabIndependentFromTheSidebarObjective() {
        assertTrue(SidebarService.shouldKeepLobbyTabContext(true, false));
        assertTrue(SidebarService.shouldKeepLobbyTabContext(false, true));
        assertFalse(SidebarService.shouldKeepLobbyTabContext(false, false));
    }

    @Test
    void alwaysKeepsArenaTeamColorAndNameTagContext() {
        assertTrue(SidebarService.shouldKeepArenaTabContext(arena()));
        assertFalse(SidebarService.shouldKeepArenaTabContext(null));
    }

    @Test
    void onlyReplaysTheFullScoreboardForLoginOrCrossWorldLoads() {
        assertTrue(SidebarService.requiresClientResynchronization(true, false));
        assertTrue(SidebarService.requiresClientResynchronization(false, true));
        assertTrue(SidebarService.requiresClientResynchronization(true, true));
        assertFalse(SidebarService.requiresClientResynchronization(false, false));
    }

    @Test
    void clientLoadCreatesAMissingSidebarButReplaysAnExistingOne() {
        assertTrue(SidebarService.shouldCreateSidebarOnClientLoad(true, false, false));
        assertFalse(SidebarService.shouldCreateSidebarOnClientLoad(true, false, true));
        assertTrue(SidebarService.shouldCreateSidebarOnClientLoad(false, true, false));
        assertFalse(SidebarService.shouldCreateSidebarOnClientLoad(false, false, false));
    }

    @Test
    void eliminationRefreshesEveryOtherViewerInTheSameArena() {
        IArena arena = arena();
        IArena otherArena = arena();
        Player eliminated = player("eliminated");
        AtomicInteger arenaUpdates = new AtomicInteger();
        AtomicInteger targetUpdates = new AtomicInteger();
        AtomicInteger otherArenaUpdates = new AtomicInteger();
        AtomicReference<Object[]> updateArguments = new AtomicReference<>();

        SidebarService.updateArenaPlayerTabs(List.of(
                sidebar(arena, player("viewer"), arenaUpdates, updateArguments),
                sidebar(arena, eliminated, targetUpdates, new AtomicReference<>()),
                sidebar(otherArena, player("other"), otherArenaUpdates, new AtomicReference<>())
        ), arena, eliminated, true);

        assertEquals(1, arenaUpdates.get());
        assertEquals(0, targetUpdates.get());
        assertEquals(0, otherArenaUpdates.get());
        assertSame(eliminated, updateArguments.get()[0]);
        assertEquals(false, updateArguments.get()[1]);
        assertEquals(true, updateArguments.get()[2]);
    }

    @Test
    void preGameSelectionRefreshesAffectedRowsForEveryArenaViewerIncludingSelf() {
        IArena arena = arena();
        IArena otherArena = arena();
        Player alice = player("alice");
        Player bob = player("bob");
        AtomicInteger firstViewerUpdates = new AtomicInteger();
        AtomicInteger secondViewerUpdates = new AtomicInteger();
        AtomicInteger otherArenaUpdates = new AtomicInteger();

        SidebarService.updatePreGameTeamTabs(List.of(
                sidebar(arena, alice, firstViewerUpdates, new AtomicReference<>()),
                sidebar(arena, bob, secondViewerUpdates, new AtomicReference<>()),
                sidebar(otherArena, player("other"), otherArenaUpdates, new AtomicReference<>())
        ), arena, List.of(alice, bob));

        assertEquals(2, firstViewerUpdates.get());
        assertEquals(2, secondViewerUpdates.get());
        assertEquals(0, otherArenaUpdates.get());
    }

    private static IArena arena() {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player(String name) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return identity;
                    if (method.getName().equals("isOnline")) return true;
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ISidebar sidebar(IArena arena, Player owner, AtomicInteger updates,
                                    AtomicReference<Object[]> updateArguments) {
        return (ISidebar) Proxy.newProxyInstance(ISidebar.class.getClassLoader(),
                new Class<?>[]{ISidebar.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getArena" -> arena;
                    case "getPlayer" -> owner;
                    case "giveUpdateTabFormat" -> {
                        updates.incrementAndGet();
                        updateArguments.set(args);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
