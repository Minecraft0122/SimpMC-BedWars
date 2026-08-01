package com.andrei1058.spigot.sidebar;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    void rendersTheCompletePlayerListRowWithAnExplicitTeamColoredName() {
        RecordingRenderer renderer = new RecordingRenderer();
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        Player target = player("Alice");
        PlayerTab tab = new PlayerTab("alice", target,
                new SidebarLine("&7[&cR&7] "), new SidebarLine("&8 !"),
                PlayerTab.PushingRule.NEVER, List.of(), ChatColor.RED,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.renderPlayerListName(viewer, tab);

        assertEquals("§7[§cR§7] §cAlice§8 !", renderer.rendered.getFirst().displayName());
        assertSame(viewer, renderer.rendered.getFirst().viewer());
        assertSame(target, renderer.rendered.getFirst().target());
    }

    @Test
    void cachesUnchangedRowsAndRestoresTheTargetsRealServerState() {
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
        assertEquals(1, renderer.rendered.size(), "unchanged animation frames must not resend packets");

        prefix[0] = "&8[";
        sidebar.renderPlayerListName(viewer, tab);
        assertEquals(2, renderer.rendered.size());

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

        assertEquals(2, renderer.rendered.size(),
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

        assertEquals(2, renderer.rendered.size(),
                "a later ADD_PLAYER or third-party update must be repairable even when text did not change");
    }

    @Test
    void failedPacketWriteDoesNotPoisonTheRenderedNameCache() {
        RecordingRenderer renderer = new RecordingRenderer();
        renderer.renderResult = false;
        Sidebar sidebar = sidebar(renderer);
        Player viewer = player("Viewer");
        Player target = player("Alice");
        PlayerTab tab = new PlayerTab("alice", target, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, List.of(), ChatColor.RED,
                PlayerTab.NameTagVisibility.ALWAYS);

        sidebar.renderPlayerListName(viewer, tab);
        renderer.renderResult = true;
        sidebar.renderPlayerListName(viewer, tab);

        assertEquals(2, renderer.renderCalls,
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
                    default -> throw new UnsupportedOperationException(method.getName());
                });
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
        private final java.util.ArrayList<CapturedName> rendered = new java.util.ArrayList<>();
        private final java.util.ArrayList<CapturedName> restored = new java.util.ArrayList<>();
        private boolean renderResult = true;
        private int renderCalls;

        @Override
        public boolean render(Player viewer, java.util.Collection<PlayerListDisplayNameRenderer.RenderedName> names) {
            renderCalls++;
            names.forEach(name -> rendered.add(
                    new CapturedName(viewer, name.target(), name.legacyDisplayName())));
            return renderResult;
        }

        @Override
        public boolean restore(Player viewer, java.util.Collection<Player> targets) {
            targets.forEach(target -> restored.add(new CapturedName(viewer, target, null)));
            return true;
        }
    }

    private record CapturedName(Player viewer, Player target, String displayName) {
    }
}
