package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.configuration.MainConfig;
import com.andrei1058.spigot.sidebar.Sidebar;
import com.andrei1058.spigot.sidebar.PlayerTab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListLifecycleTest {

    private MainConfig previousConfig;
    private Player player;
    private IArena arena;
    private ITeam team;
    private SidebarService service;
    private final Component original = Component.text("[VIP] Alice");
    private final AtomicReference<Component> listName = new AtomicReference<>(original);

    @BeforeEach
    void setUp() {
        previousConfig = BedWars.config;
        BedWars.config = mock(MainConfig.class);
        player = player(UUID.randomUUID(), listName);
        arena = mock(IArena.class);
        team = mock(ITeam.class);
        when(team.getColor()).thenReturn(TeamColor.RED);
        when(arena.getTeam(player)).thenReturn(team);
        when(arena.getPlayers()).thenReturn(List.of(player));
        when(arena.getSpectators()).thenReturn(List.of());
        when(arena.getStatus()).thenReturn(GameState.playing);
        Arena.setArenaByPlayer(player, arena);
        service = mock(SidebarService.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        ArenaPlayerListNames.clear();
        Arena.getArenaByPlayer().remove(player);
        BedWars.config = previousConfig;
    }

    @Test
    void startEventSetsTeamColorsBeforeAnyViewerSidebarExists() {
        new ScoreboardListener().gameStateChanged(
                new GameStateChangeEvent(arena, GameState.starting, GameState.playing));

        assertName(NamedTextColor.RED, false);
    }

    @Test
    void unassignedWaitingPlayerKeepsOriginalName() {
        when(arena.getTeam(player)).thenReturn(null);
        when(arena.getStatus()).thenReturn(GameState.waiting);

        service.giveSidebar(player, arena, false);

        assertEquals(original, listName.get());
        verify(player, never()).playerListName(any(Component.class));
    }

    @Test
    void viewerRefreshReplacesCachedColorAndRespawnStyle() throws ReflectiveOperationException {
        PlayerTab row = new PlayerTab(player.getUniqueId().toString(), player);
        BwTabList tabList = deployedRow(row);
        when(arena.isReSpawning(player)).thenReturn(true);

        tabList.refreshPlayerListState();

        assertEquals(NamedTextColor.RED, row.getTextColor());
        assertEquals(true, row.isItalic());
        when(team.getColor()).thenReturn(TeamColor.BLUE);
        when(arena.isReSpawning(player)).thenReturn(false);
        tabList.refreshPlayerListState();
        assertEquals(NamedTextColor.BLUE, row.getTextColor());
        assertEquals(false, row.isItalic());
    }

    @Test
    void viewerRefreshRemovesEliminatedTeamRow() throws ReflectiveOperationException {
        PlayerTab row = new PlayerTab(player.getUniqueId().toString(), player);
        BwSidebar viewer = mock(BwSidebar.class);
        Sidebar handle = mock(Sidebar.class);
        when(viewer.getArena()).thenReturn(arena);
        when(viewer.getHandle()).thenReturn(handle);
        BwTabList tabList = new BwTabList(viewer);
        deploy(tabList, row);
        when(arena.isSpectator(player)).thenReturn(true);

        tabList.refreshPlayerListState();

        verify(handle).removeTab(row.getIdentifier());
    }

    @Test
    void regularRefreshRepairsPaperNameEvenWithoutViewerRows() {
        service.handleJoin(arena, player, false);
        listName.set(Component.text("Alice"));

        service.refreshTabList();

        assertName(NamedTextColor.RED, false);
    }

    @Test
    void regularRefreshReadsTheCurrentTeamInsteadOfTheCachedColor() {
        service.handleJoin(arena, player, false);
        when(team.getColor()).thenReturn(TeamColor.BLUE);

        service.refreshTabList();

        assertName(NamedTextColor.BLUE, false);
    }

    @Test
    void respawnEventsRetainTeamColorAndClearItalicAfterRespawning() {
        when(arena.isReSpawning(player)).thenReturn(true);
        service.handleRespawnState(arena, player);
        assertName(NamedTextColor.RED, true);

        when(arena.isReSpawning(player)).thenReturn(false);
        service.handleRespawnState(arena, player);
        assertName(NamedTextColor.RED, false);
    }

    @Test
    void eliminatedSpectatorNeverInheritsFormerTeamColor() {
        service.handleJoin(arena, player, false);
        when(arena.getTeam(player)).thenReturn(null);
        when(arena.getExTeam(player.getUniqueId())).thenReturn(team);
        when(arena.isSpectator(player)).thenReturn(true);

        service.refreshTabList();

        assertName(NamedTextColor.GRAY, true);
    }

    @Test
    void removingAViewerRowDoesNotRestoreTheTargetsUncoloredName() {
        service.handleJoin(arena, player, false);
        BwSidebar viewer = mock(BwSidebar.class);
        when(viewer.getHandle()).thenReturn(mock(Sidebar.class));

        new BwTabList(viewer).onSidebarRemoval();

        assertName(NamedTextColor.RED, false);
    }

    @Test
    void returningToLobbyRestoresOriginalName() {
        service.handleJoin(arena, player, false);
        Arena.getArenaByPlayer().remove(player);

        service.refreshTabList();

        assertEquals(original, listName.get());
    }

    @Test
    void arenaTeardownRestoresNamesWithoutWaitingForPeriodicRefresh() {
        service.handleJoin(arena, player, false);

        ArenaPlayerListNames.release(arena);

        assertEquals(original, listName.get());
        clearInvocations(player);
        service.refreshTabList();
        verify(player, never()).playerListName(any(Component.class));
    }

    @Test
    void unchangedStateDoesNotSendAnotherGlobalDisplayNameUpdate() {
        service.handleJoin(arena, player, false);
        clearInvocations(player);

        service.refreshTabList();
        service.handleRespawnState(arena, player);

        verify(player, never()).playerListName(any(Component.class));
    }

    @Test
    void oldConnectionCleanupCannotRemoveReconnectedPlayersColor() {
        service.handleJoin(arena, player, false);
        Component reconnectOriginal = Component.text("Alice reconnected");
        AtomicReference<Component> reconnectName = new AtomicReference<>(reconnectOriginal);
        Player reconnected = player(player.getUniqueId(), reconnectName);
        when(arena.getTeam(reconnected)).thenReturn(team);

        service.handleReJoin(arena, reconnected);
        ArenaPlayerListNames.release(player);

        assertEquals(original, listName.get());
        assertEquals(Component.text("Alice", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false), reconnectName.get());
        ArenaPlayerListNames.release(reconnected);
        assertEquals(reconnectOriginal, reconnectName.get());
    }

    @Test
    void leavingPreservesANewExternalName() {
        service.handleJoin(arena, player, false);
        Component externalName = Component.text("[MVP] Alice");
        listName.set(externalName);

        ArenaPlayerListNames.release(player);

        assertEquals(externalName, listName.get());
    }

    private void assertName(NamedTextColor color, boolean italic) {
        assertEquals(Component.text("Alice", color).decoration(TextDecoration.ITALIC, italic), listName.get());
    }

    private BwTabList deployedRow(PlayerTab row) throws ReflectiveOperationException {
        BwSidebar viewer = mock(BwSidebar.class);
        when(viewer.getArena()).thenReturn(arena);
        BwTabList tabList = new BwTabList(viewer);
        deploy(tabList, row);
        return tabList;
    }

    @SuppressWarnings("unchecked")
    private static void deploy(BwTabList tabList, PlayerTab row) throws ReflectiveOperationException {
        var field = BwTabList.class.getDeclaredField("deployedPerPlayerTabList");
        field.setAccessible(true);
        ((Map<UUID, PlayerTab>) field.get(tabList)).put(row.getPlayer().getUniqueId(), row);
    }

    private static Player player(UUID uniqueId, AtomicReference<Component> name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uniqueId);
        when(player.getName()).thenReturn("Alice");
        when(player.isOnline()).thenReturn(true);
        when(player.playerListName()).thenAnswer(invocation -> name.get());
        doAnswer(invocation -> {
            name.set(invocation.getArgument(0));
            return null;
        }).when(player).playerListName(any(Component.class));
        return player;
    }
}
