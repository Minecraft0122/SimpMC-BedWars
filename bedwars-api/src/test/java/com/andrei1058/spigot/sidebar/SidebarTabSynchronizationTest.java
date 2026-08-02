package com.andrei1058.spigot.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidebarTabSynchronizationTest {

    @Test
    void allocatesStableCollisionFreeTeamNames() {
        Sidebar sidebar = sidebar();

        assertEquals("bw_t_0", sidebar.teamName("first-player"));
        assertEquals("bw_t_0", sidebar.teamName("first-player"));
        assertEquals("bw_t_1", sidebar.teamName("second-player"));
        assertNotEquals(sidebar.teamName("FB"), sidebar.teamName("Ea"),
                "Java hash collisions must not merge scoreboard teams");

        Set<String> names = new HashSet<>();
        for (int index = 0; index < 10_000; index++) {
            String name = sidebar.teamName("player-" + index);
            assertTrue(name.length() <= 16);
            assertTrue(names.add(name));
        }
    }

    @Test
    void unchangedRefreshDoesNotWriteScoreboardTeam() {
        Sidebar sidebar = sidebar();
        TeamState state = new TeamState("Alice");
        Team team = team(state);
        Scoreboard scoreboard = scoreboard(team);
        Player player = player("Alice");
        PlayerTab unchanged = tab(player, new SidebarLine(), ChatColor.WHITE,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.applyTab(scoreboard, unchanged);
        assertEquals(0, state.writeCount);

        PlayerTab changed = tab(player, new SidebarLine("&a队伍"), ChatColor.RED,
                PlayerTab.NameTagVisibility.NEVER);
        sidebar.applyTab(scoreboard, changed);
        assertEquals(3, state.writeCount);

        sidebar.applyTab(scoreboard, changed);
        assertEquals(3, state.writeCount, "repeating the same refresh must not send more team updates");
    }

    @Test
    void reattachesOnlyTheActiveOwnerWhenItsManagedScoreboardWasReplaced() {
        Scoreboard managed = scoreboard(team(new TeamState()));
        Scoreboard external = scoreboard(team(new TeamState()));

        assertTrue(Sidebar.shouldReattachScoreboard(true, managed, external));
        assertFalse(Sidebar.shouldReattachScoreboard(false, managed, external));
        assertFalse(Sidebar.shouldReattachScoreboard(true, managed, managed));
        assertFalse(Sidebar.shouldReattachScoreboard(true, null, external));
    }

    @Test
    void periodicReplayReattachesADriftedViewerAfterMapIteration() {
        AtomicInteger reattachments = new AtomicInteger();
        Sidebar sidebar = new Sidebar(new SidebarLine(), List.of(), new ConcurrentLinkedQueue<>()) {
            @Override
            public void add(Player player) {
                reattachments.incrementAndGet();
            }
        };
        Scoreboard managed = scoreboard(team(new TeamState()));
        Scoreboard external = scoreboard(team(new TeamState()));
        String name = "DriftedViewer";
        java.util.UUID viewerId = java.util.UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        Player viewer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> viewerId;
                    case "isOnline" -> true;
                    case "getScoreboard" -> external;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        fieldMap(sidebar, "scoreboards").put(viewerId, managed);
        fieldMap(sidebar, "viewers").put(viewerId, viewer);

        SidebarManager manager = SidebarManager.getInstance();
        manager.claimDisplayNameOwnership(sidebar, viewer);
        try {
            sidebar.playerTabRefreshAnimation();
            assertEquals(1, reattachments.get());
        } finally {
            manager.releaseDisplayNameOwnership(sidebar, viewer);
        }
    }

    @Test
    void clearsThePlayerInfoNameAndLeavesTheScoreboardTeamAsRenderingAuthority() {
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        Player target = player("Alice");
        TeamState state = new TeamState();
        Scoreboard scoreboard = scoreboard(team(state));
        PlayerTab tab = new PlayerTab("alice", target,
                new SidebarLine("&7[&cR&7] "), new SidebarLine("&8 !"),
                PlayerTab.PushingRule.NEVER, List.of(), ChatColor.RED,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.applyTab(scoreboard, tab);
        sidebar.renderPlayerListName(viewer, tab);

        assertEquals(1, renderer.cleared.size());
        assertSame(viewer, renderer.cleared.getFirst().viewer());
        assertSame(target, renderer.cleared.getFirst().target());
        assertEquals(Sidebar.component("§7[§cR§7] "), state.prefix);
        assertEquals(Sidebar.component("§8 !"), state.suffix);
        assertSame(ChatColor.RED, state.color);
        assertTrue(state.entries.contains("Alice"));
    }

    @Test
    void creatingATabRowNeverMutatesTheTargetsGlobalPlayerListName() {
        Sidebar sidebar = sidebar();
        AtomicBoolean globalNameMutated = new AtomicBoolean();
        String name = "ExternalAlice";
        Player target = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> java.util.UUID.nameUUIDFromBytes(
                            name.getBytes(StandardCharsets.UTF_8));
                    case "playerListName" -> {
                        if (args != null && args.length == 1) globalNameMutated.set(true);
                        yield Component.text("[VIP] " + name);
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        sidebar.playerTabCreate("external-alice", target, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);

        assertFalse(globalNameMutated.get(),
                "per-viewer BedWars formatting must not overwrite Paper's global player-list name");
    }

    @Test
    void cachesClearedRowsAcrossTeamAnimationAndRestoresTheTargetsRealServerState() {
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        Player target = player("Alice");
        String[] prefix = {"&7["};
        SidebarLine changingPrefix = new SidebarLine() {
            @Override
            public String getLine() {
                return prefix[0];
            }
        };
        PlayerTab tab = new PlayerTab("alice", target, changingPrefix, new SidebarLine(),
                PlayerTab.PushingRule.NEVER, List.of(), ChatColor.BLUE,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.renderPlayerListName(viewer, tab);
        sidebar.renderPlayerListName(viewer, tab);
        assertEquals(1, renderer.cleared.size(), "an already-cleared row must not resend packets");

        prefix[0] = "&8[";
        sidebar.renderPlayerListName(viewer, tab);
        assertEquals(1, renderer.cleared.size(),
                "scoreboard prefix animation must not resend an unchanged null display name");

        sidebar.restorePlayerListName(viewer, tab);
        sidebar.restorePlayerListName(viewer, tab);
        assertEquals(1, renderer.restored.size(), "a managed row is restored exactly once");
        assertSame(target, renderer.restored.getFirst().target());
    }

    @Test
    void offlineRowsReleaseTheirPerViewerCacheWithoutSendingARestorePacket() {
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        boolean[] online = {true};
        Player target = player("OfflineAlice", () -> online[0]);
        PlayerTab tab = new PlayerTab("offline-alice", target, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, List.of(), ChatColor.RED,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.renderPlayerListName(viewer, tab);
        online[0] = false;
        sidebar.restorePlayerListName(viewer, tab);
        online[0] = true;
        sidebar.renderPlayerListName(viewer, tab);

        assertEquals(2, renderer.cleared.size(),
                "removing an offline target must release its cached row");
        assertTrue(renderer.restored.isEmpty(),
                "an offline target no longer has a PlayerInfo row to restore");
    }

    @Test
    void forcedReplayRepairsAPlayerInfoEntryOverwrittenAfterRendering() {
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        Player target = player("Alice");
        PlayerTab tab = sidebar.playerTabCreate("alice", target, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);

        sidebar.renderPlayerListName(viewer, tab);
        sidebar.forcePlayerListNameRefresh(viewer);

        assertEquals(2, renderer.cleared.size(),
                "a later ADD_PLAYER or third-party update must be repairable even when text did not change");
    }

    @Test
    void failedPacketWriteDoesNotPoisonTheRenderedNameCache() {
        RecordingRenderer renderer = new RecordingRenderer();
        renderer.clearResult = false;
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        Player target = player("Alice");
        PlayerTab tab = new PlayerTab("alice", target, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, List.of(), ChatColor.RED,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.renderPlayerListName(viewer, tab);
        renderer.clearResult = true;
        sidebar.renderPlayerListName(viewer, tab);

        assertEquals(2, renderer.clearCalls,
                "an unchanged row must retry after the previous packet write failed");
    }

    @Test
    void staleSidebarCannotReleaseALaterOwnersDisplayNames() {
        SidebarManager manager = SidebarManager.getInstance();
        Sidebar oldSidebar = sidebar(new RecordingRenderer());
        Sidebar currentSidebar = sidebar(new RecordingRenderer());
        Player viewer = player("Viewer");

        manager.claimDisplayNameOwnership(oldSidebar, viewer);
        manager.claimDisplayNameOwnership(currentSidebar, viewer);

        assertFalse(manager.releaseDisplayNameOwnership(oldSidebar, viewer));
        assertTrue(manager.ownsDisplayNames(currentSidebar, viewer));
        assertTrue(manager.releaseDisplayNameOwnership(currentSidebar, viewer));
    }

    @Test
    void removingTabsReleasesTheirAllocatedTeamNames() {
        Sidebar neverRendered = sidebar();
        Player unrendered = player("MappingUnrendered");
        neverRendered.playerTabCreate("unrendered", unrendered, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.GRAY);
        neverRendered.removeTab("unrendered");
        assertTrue(fieldMap(neverRendered, "tabTeamNames").isEmpty(),
                "removing an unrendered row must not allocate a team name on the cleanup path");

        Sidebar singleRemoval = sidebar();
        Player alice = player("MappingAlice");
        singleRemoval.playerTabCreate("alice", alice, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        singleRemoval.teamName("alice");

        singleRemoval.removeTab("alice");

        assertFalse(fieldMap(singleRemoval, "tabTeamNames").containsKey("alice"),
                "removeTab must release identifiers belonging to players that have left");

        Sidebar bulkRemoval = sidebar();
        Player bob = player("MappingBob");
        Player charlie = player("MappingCharlie");
        bulkRemoval.playerTabCreate("bob", bob, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.BLUE);
        bulkRemoval.playerTabCreate("charlie", charlie, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.GREEN);
        bulkRemoval.teamName("bob");
        bulkRemoval.teamName("charlie");

        bulkRemoval.removeTabs();

        assertTrue(fieldMap(bulkRemoval, "tabTeamNames").isEmpty(),
                "removeTabs must not retain identifiers from historical players");
    }

    @Test
    void releasingANewerSidebarRestoresAndReplaysThePreviousOwner() {
        SidebarManager manager = SidebarManager.getInstance();
        RecordingRenderer previousRenderer = new RecordingRenderer();
        RecordingRenderer currentRenderer = new RecordingRenderer();
        Sidebar previous = sidebar(previousRenderer);
        Sidebar current = sidebar(currentRenderer);
        Player viewer = player("OwnershipViewer");
        Player previousTarget = player("PreviousTarget");
        Player currentTarget = player("CurrentTarget");
        PlayerTab previousTab = previous.playerTabCreate("previous", previousTarget,
                new SidebarLine(), new SidebarLine(), PlayerTab.PushingRule.NEVER,
                new ConcurrentLinkedQueue<>(), ChatColor.RED);
        PlayerTab currentTab = current.playerTabCreate("current", currentTarget,
                new SidebarLine(), new SidebarLine(), PlayerTab.PushingRule.NEVER,
                new ConcurrentLinkedQueue<>(), ChatColor.BLUE);

        manager.claimDisplayNameOwnership(previous, viewer);
        try {
            previous.renderPlayerListName(viewer, previousTab);
            manager.claimDisplayNameOwnership(current, viewer);

            assertEquals(1, previousRenderer.restoreCalls,
                    "claiming a newer Sidebar must remove the previous owner's client rows");
            assertSame(previousTarget, previousRenderer.restored.getFirst().target());

            current.renderPlayerListName(viewer, currentTab);
            assertTrue(manager.releaseDisplayNameOwnership(current, viewer));

            assertTrue(manager.ownsDisplayNames(previous, viewer),
                    "releasing the newer Sidebar must reactivate the still-attached previous owner");
            assertEquals(2, previousRenderer.cleared.size(),
                    "reactivating the previous owner must replay its complete player list state");
        } finally {
            manager.releaseDisplayNameOwnership(current, viewer);
            manager.releaseDisplayNameOwnership(previous, viewer);
        }
    }

    @Test
    void removingASuspendedSidebarRepairsTheNewerScoreboardFallback() {
        SidebarManager manager = SidebarManager.getInstance();
        Sidebar previous = sidebar(new RecordingRenderer());
        Sidebar current = sidebar(new RecordingRenderer());
        Player viewer = player("NonLifoViewer");
        Scoreboard external = scoreboard(team(new TeamState()));
        Scoreboard previousManaged = scoreboard(team(new TeamState()));
        fieldMap(current, "previousScoreboards").put(viewer.getUniqueId(), previousManaged);

        manager.claimDisplayNameOwnership(previous, viewer);
        manager.claimDisplayNameOwnership(current, viewer);
        try {
            manager.unlinkPreviousScoreboard(previous, viewer, previousManaged, external);
            assertFalse(manager.releaseDisplayNameOwnership(previous, viewer));

            assertSame(external,
                    fieldMap(current, "previousScoreboards").get(viewer.getUniqueId()),
                    "removing a suspended owner must not leave its dead scoreboard as the fallback");
        } finally {
            manager.releaseDisplayNameOwnership(current, viewer);
            manager.releaseDisplayNameOwnership(previous, viewer);
        }
    }

    @Test
    void replacingAnIdentifierRemovesThePreviousPlayerFromItsScoreboardTeam() {
        Sidebar sidebar = sidebar();
        TeamState state = new TeamState();
        Scoreboard scoreboard = scoreboard(team(state));
        Player viewer = player("ReplacementViewer");
        Player alice = player("ReplacementAlice");
        Player bob = player("ReplacementBob");
        fieldMap(sidebar, "scoreboards").put(viewer.getUniqueId(), scoreboard);
        fieldMap(sidebar, "viewers").put(viewer.getUniqueId(), viewer);

        sidebar.playerTabCreate("slot", alice, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        sidebar.playerTabCreate("slot", bob, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.BLUE);

        assertFalse(state.entries.contains(alice.getName()),
                "the old player must not keep the replacement row's overhead color");
        assertTrue(state.entries.contains(bob.getName()));
    }

    @Test
    void failedRestoreRemainsPendingUntilAnExplicitRefreshSucceeds() {
        SidebarManager manager = SidebarManager.getInstance();
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("RetryViewer");
        Player target = player("RetryTarget");
        PlayerTab tab = sidebar.playerTabCreate("retry", target, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        fieldMap(sidebar, "viewers").put(viewer.getUniqueId(), viewer);

        manager.claimDisplayNameOwnership(sidebar, viewer);
        try {
            sidebar.renderPlayerListName(viewer, tab);
            renderer.restoreResult = false;

            sidebar.removeTab("retry");

            assertEquals(1, renderer.restoreCalls);
            renderer.restoreResult = true;
            sidebar.forcePlayerListNameRefresh(viewer);
            assertEquals(2, renderer.restoreCalls,
                    "an explicit refresh must retry a restore that previously failed");

            sidebar.forcePlayerListNameRefresh(viewer);
            assertEquals(2, renderer.restoreCalls,
                    "a successful retry must clear the pending restore state");
        } finally {
            manager.releaseDisplayNameOwnership(sidebar, viewer);
        }
    }

    @Test
    void failedBulkRestoreKeepsAllTargetsUntilRefreshSucceeds() {
        SidebarManager manager = SidebarManager.getInstance();
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("BulkRetryViewer");
        Player alice = player("BulkRetryAlice");
        Player bob = player("BulkRetryBob");
        PlayerTab aliceTab = sidebar.playerTabCreate("bulk-alice", alice, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        PlayerTab bobTab = sidebar.playerTabCreate("bulk-bob", bob, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.BLUE);
        fieldMap(sidebar, "viewers").put(viewer.getUniqueId(), viewer);

        manager.claimDisplayNameOwnership(sidebar, viewer);
        try {
            sidebar.renderPlayerListName(viewer, aliceTab);
            sidebar.renderPlayerListName(viewer, bobTab);
            renderer.restoreResult = false;

            sidebar.removeTabs();
            assertEquals(1, renderer.restoreCalls);

            renderer.restoreResult = true;
            sidebar.forcePlayerListNameRefresh(viewer);
            assertEquals(2, renderer.restoreCalls);
            assertEquals(Set.of(alice.getUniqueId(), bob.getUniqueId()),
                    renderer.restored.subList(2, 4).stream()
                            .map(captured -> captured.target().getUniqueId())
                            .collect(java.util.stream.Collectors.toSet()));
        } finally {
            manager.releaseDisplayNameOwnership(sidebar, viewer);
        }
    }

    @Test
    void capturesOnlyTheLatestExternallyOwnedScoreboard() {
        Scoreboard managed = scoreboard(team(new TeamState()));
        Scoreboard external = scoreboard(team(new TeamState()));

        assertFalse(Sidebar.shouldCapturePreviousScoreboard(managed, managed));
        assertTrue(Sidebar.shouldCapturePreviousScoreboard(managed, external));
        assertTrue(Sidebar.shouldCapturePreviousScoreboard(null, external));
    }

    @Test
    void appliesRequestedColorAndAddsPlayerToScoreboardTeam() {
        Sidebar sidebar = sidebar();
        TeamState state = new TeamState();
        Scoreboard scoreboard = scoreboard(team(state));
        Player player = player("Alice");

        sidebar.applyTab(scoreboard, tab(player, new SidebarLine("&cRed "), ChatColor.RED,
                PlayerTab.NameTagVisibility.NEVER));

        assertSame(ChatColor.RED, state.color);
        assertTrue(state.entries.contains("Alice"));
        assertSame(Team.OptionStatus.NEVER, state.visibility);
        assertEquals(Sidebar.component("§cRed "), state.prefix);
    }

    private static Sidebar sidebar() {
        return new Sidebar(new SidebarLine(), List.of(), new ConcurrentLinkedQueue<>());
    }

    private static Sidebar sidebar(PlayerListDisplayNameRenderer renderer) {
        return new Sidebar(new SidebarLine(), List.of(), new ConcurrentLinkedQueue<>(), renderer);
    }

    private static PlayerTab tab(Player player, SidebarLine prefix, ChatColor color,
                                 PlayerTab.NameTagVisibility visibility) {
        return new PlayerTab(player.getUniqueId().toString(), player, prefix, new SidebarLine(),
                PlayerTab.PushingRule.NEVER, List.of(), color, visibility);
    }

    private static Player player(String name) {
        return player(name, () -> true);
    }

    private static Player player(String name, BooleanSupplier online) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> java.util.UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                    case "isOnline" -> online.getAsBoolean();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Scoreboard scoreboard(Team team) {
        return (Scoreboard) Proxy.newProxyInstance(Scoreboard.class.getClassLoader(),
                new Class<?>[]{Scoreboard.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTeam" -> team;
                    case "registerNewTeam" -> throw new AssertionError("the existing team must be reused");
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Team team(TeamState state) {
        return (Team) Proxy.newProxyInstance(Team.class.getClassLoader(), new Class<?>[]{Team.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "prefix" -> {
                        if (args == null || args.length == 0) yield state.prefix;
                        state.prefix = (Component) args[0];
                        state.writeCount++;
                        yield null;
                    }
                    case "suffix" -> {
                        if (args == null || args.length == 0) yield state.suffix;
                        state.suffix = (Component) args[0];
                        state.writeCount++;
                        yield null;
                    }
                    case "getColor" -> state.color;
                    case "setColor" -> {
                        state.color = (ChatColor) args[0];
                        state.writeCount++;
                        yield null;
                    }
                    case "getOption" -> state.visibility;
                    case "setOption" -> {
                        state.visibility = (Team.OptionStatus) args[1];
                        state.writeCount++;
                        yield null;
                    }
                    case "hasEntry" -> state.entries.contains((String) args[0]);
                    case "addEntry" -> {
                        state.entries.add((String) args[0]);
                        state.writeCount++;
                        yield null;
                    }
                    case "removeEntry" -> {
                        boolean removed = state.entries.remove((String) args[0]);
                        state.writeCount++;
                        yield removed;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> fieldMap(Sidebar sidebar, String fieldName) {
        try {
            Field field = Sidebar.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Map<K, V>) field.get(sidebar);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to inspect Sidebar." + fieldName, exception);
        }
    }

    private static final class TeamState {
        private Component prefix = Component.empty();
        private Component suffix = Component.empty();
        private ChatColor color = ChatColor.WHITE;
        private Team.OptionStatus visibility = Team.OptionStatus.ALWAYS;
        private final Set<String> entries = new HashSet<>();
        private int writeCount;

        private TeamState() {
        }

        private TeamState(String entry) {
            entries.add(entry);
        }
    }

    private static final class RecordingRenderer implements PlayerListDisplayNameRenderer {
        private final java.util.ArrayList<CapturedTarget> cleared = new java.util.ArrayList<>();
        private final java.util.ArrayList<CapturedTarget> restored = new java.util.ArrayList<>();
        private boolean clearResult = true;
        private boolean restoreResult = true;
        private int clearCalls;
        private int restoreCalls;

        @Override
        public boolean clear(Player viewer, java.util.Collection<Player> targets) {
            clearCalls++;
            targets.forEach(target -> cleared.add(new CapturedTarget(viewer, target)));
            return clearResult;
        }

        @Override
        public boolean restore(Player viewer, java.util.Collection<Player> targets) {
            restoreCalls++;
            targets.forEach(target -> restored.add(new CapturedTarget(viewer, target)));
            return restoreResult;
        }
    }

    private record CapturedTarget(Player viewer, Player target) {
    }
}
