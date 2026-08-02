package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.sidebar.ISidebar;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void ordinaryWorldChangeSchedulesOneFallbackAndDeduplicatesAClientLoad() {
        UUID playerId = UUID.randomUUID();
        Set<UUID> pending = new HashSet<>();
        List<Runnable> scheduled = new ArrayList<>();
        AtomicInteger resynchronizations = new AtomicInteger();

        SidebarService.scheduleWorldResynchronization(
                pending, playerId, scheduled::add, resynchronizations::incrementAndGet);
        SidebarService.scheduleWorldResynchronization(
                pending, playerId, scheduled::add, resynchronizations::incrementAndGet);

        assertEquals(1, scheduled.size(), "rapid world changes must share one next-tick fallback");
        assertTrue(pending.remove(playerId), "a real client-load event consumes the same marker");
        scheduled.getFirst().run();
        assertEquals(0, resynchronizations.get(), "the consumed fallback must not replay twice");

        SidebarService.scheduleWorldResynchronization(
                pending, playerId, scheduled::add, resynchronizations::incrementAndGet);
        scheduled.get(1).run();
        assertEquals(1, resynchronizations.get(), "ordinary cross-world travel needs its fallback");
        assertFalse(pending.contains(playerId));
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
    void eliminationsInOneArenaAndTickShareOneRefresh() {
        IArena firstArena = arena();
        IArena secondArena = arena();
        Player alice = player("alice");
        Player bob = player("bob");
        Map<IArena, LinkedHashMap<UUID, Player>> pending = new IdentityHashMap<>();
        List<Runnable> scheduled = new ArrayList<>();
        List<IArena> refreshedArenas = new ArrayList<>();
        List<Collection<Player>> refreshedPlayers = new ArrayList<>();

        SidebarService.scheduleArenaEliminationRefresh(
                pending, firstArena, alice, scheduled::add,
                (arena, players) -> {
                    refreshedArenas.add(arena);
                    refreshedPlayers.add(players);
                });
        SidebarService.scheduleArenaEliminationRefresh(
                pending, firstArena, alice, scheduled::add,
                (arena, players) -> {
                    refreshedArenas.add(arena);
                    refreshedPlayers.add(players);
                });
        SidebarService.scheduleArenaEliminationRefresh(
                pending, firstArena, bob, scheduled::add,
                (arena, players) -> {
                    refreshedArenas.add(arena);
                    refreshedPlayers.add(players);
                });
        SidebarService.scheduleArenaEliminationRefresh(
                pending, secondArena, bob, scheduled::add,
                (arena, players) -> {
                    refreshedArenas.add(arena);
                    refreshedPlayers.add(players);
                });

        assertEquals(2, scheduled.size(), "each arena must own one next-tick refresh");
        scheduled.getFirst().run();
        assertEquals(List.of(firstArena), refreshedArenas);
        assertEquals(List.of(alice, bob), List.copyOf(refreshedPlayers.getFirst()));
        assertFalse(pending.containsKey(firstArena));
        assertTrue(pending.containsKey(secondArena));

        scheduled.get(1).run();
        assertEquals(List.of(firstArena, secondArena), refreshedArenas);
        assertEquals(List.of(bob), List.copyOf(refreshedPlayers.get(1)));
        assertTrue(pending.isEmpty());
    }

    @Test
    void showPlayerDoesNotReplayAnEliminationRowAlreadyQueuedForTheArena() {
        IArena arena = arena();
        IArena otherArena = arena();
        Player eliminated = player("eliminated");
        Map<IArena, LinkedHashMap<UUID, Player>> pending = new IdentityHashMap<>();
        pending.computeIfAbsent(arena, ignored -> new LinkedHashMap<>())
                .put(eliminated.getUniqueId(), eliminated);

        assertFalse(SidebarService.shouldReplayPlayerShown(
                arena, eliminated.getUniqueId(), pending));
        assertTrue(SidebarService.shouldReplayPlayerShown(
                otherArena, eliminated.getUniqueId(), pending));
        assertTrue(SidebarService.shouldReplayPlayerShown(
                null, eliminated.getUniqueId(), pending));
        assertTrue(SidebarService.shouldReplayPlayerShown(
                arena, player("other").getUniqueId(), pending));
    }

    @Test
    void delayedEliminationRefreshRejectsOfflineLeftAndTransferredPlayers() {
        IArena originalArena = arena(true);
        IArena otherArena = arena(true);
        Player online = player("online");
        Player offline = player("offline", false);

        assertTrue(SidebarService.isCurrentElimination(originalArena, online, ignored -> originalArena));
        assertFalse(SidebarService.isCurrentElimination(originalArena, offline, ignored -> originalArena));
        assertFalse(SidebarService.isCurrentElimination(originalArena, online, ignored -> null));
        assertFalse(SidebarService.isCurrentElimination(originalArena, online, ignored -> otherArena));
        assertFalse(SidebarService.isCurrentElimination(arena(false), online, ignored -> originalArena));
    }

    @Test
    void staleEliminationStillRefreshesArenaPlaceholdersWithoutRestoringTabRow() {
        AtomicInteger placeholderRefreshes = new AtomicInteger();
        AtomicInteger tabRefreshes = new AtomicInteger();

        SidebarService.refreshEliminationState(
                List.of(player("left")),
                ignored -> false,
                placeholderRefreshes::incrementAndGet,
                ignored -> tabRefreshes.incrementAndGet()
        );

        assertEquals(1, placeholderRefreshes.get());
        assertEquals(0, tabRefreshes.get());
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
        return arena(null);
    }

    private static IArena arena(Boolean spectator) {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isSpectator") && spectator != null) return spectator;
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player(String name) {
        return player(name, true);
    }

    private static Player player(String name, boolean online) {
        UUID identity = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return identity;
                    if (method.getName().equals("isOnline")) return online;
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
