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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void clearsAndRestoresCustomPlayerListNameAcrossViewers() {
        Component customName = Component.text("大厅称号 Alice");
        PlayerNameState playerState = new PlayerNameState(customName);
        Player player = player("Alice", playerState);
        Sidebar firstViewer = sidebar();
        Sidebar secondViewer = sidebar();

        firstViewer.playerTabCreate("alice", player, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        assertNull(playerState.storedPlayerListName,
                "a custom component prevents the client from applying the viewer's scoreboard team color");

        secondViewer.playerTabCreate("alice", player, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        firstViewer.removeTab("alice");
        assertNull(playerState.storedPlayerListName, "another viewer still needs scoreboard formatting");

        secondViewer.removeTab("alice");
        assertEquals(customName, playerState.storedPlayerListName);
    }

    @Test
    void reClearsAndEventuallyRestoresAPlayerListNameChangedByAnotherPlugin() {
        Component original = Component.text("原始名称");
        Component external = Component.text("其他插件名称");
        PlayerNameState state = new PlayerNameState(original);
        Player player = player("ExternalAlice", state);
        Sidebar firstViewer = sidebar();
        Sidebar secondViewer = sidebar();

        firstViewer.playerTabCreate("external-alice", player, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        state.storedPlayerListName = external;
        secondViewer.playerTabCreate("external-alice", player, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        assertNull(state.storedPlayerListName, "a later refresh must not leave a custom component bypassing team color");

        firstViewer.removeTab("external-alice");
        secondViewer.removeTab("external-alice");

        assertEquals(external, state.storedPlayerListName,
                "the latest external value must be restored instead of the stale value from arena entry");
    }

    @Test
    void modelsPaperDefaultNameWithoutCreatingAnExplicitWhiteListName() {
        PlayerNameState state = new PlayerNameState(null);
        Player player = player("DefaultAlice", state);
        Sidebar firstViewer = sidebar();
        Sidebar secondViewer = sidebar();

        firstViewer.playerTabCreate("default-alice", player, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        secondViewer.playerTabCreate("default-alice", player, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);

        assertEquals(Component.text("DefaultAlice"), player.playerListName(),
                "Paper exposes the profile name even while its internal custom value is null");
        assertNull(state.storedPlayerListName);
        assertEquals(1, state.setterCalls,
                "additional viewers must not broadcast duplicate UPDATE_DISPLAY_NAME packets");

        firstViewer.removeTab("default-alice");
        secondViewer.removeTab("default-alice");
        assertNull(state.storedPlayerListName,
                "release must not restore the effective profile name as an explicit white component");
        assertEquals(1, state.setterCalls);
    }

    @Test
    void reconnectReplacesStalePlayerListNameOwnership() {
        PlayerNameState oldState = new PlayerNameState(Component.text("Old session"));
        Player oldPlayer = player("ReconnectAlice", oldState);
        Sidebar firstOldViewer = sidebar();
        Sidebar secondOldViewer = sidebar();
        firstOldViewer.playerTabCreate("old-one", oldPlayer, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        secondOldViewer.playerTabCreate("old-two", oldPlayer, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);

        Component newCustomName = Component.text("New session");
        PlayerNameState newState = new PlayerNameState(newCustomName);
        Player reconnectedPlayer = player("ReconnectAlice", newState);
        Sidebar newViewer = sidebar();
        newViewer.playerTabCreate("new", reconnectedPlayer, new SidebarLine(), new SidebarLine(),
                PlayerTab.PushingRule.NEVER, new ConcurrentLinkedQueue<>(), ChatColor.RED);
        assertNull(newState.storedPlayerListName);

        firstOldViewer.removeTab("old-one");
        secondOldViewer.removeTab("old-two");
        assertNull(newState.storedPlayerListName,
                "stale releases from the previous connection must not release the new connection");

        newViewer.removeTab("new");
        assertEquals(newCustomName, newState.storedPlayerListName);
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

    private static PlayerTab tab(Player player, SidebarLine prefix, ChatColor color,
                                 PlayerTab.NameTagVisibility visibility) {
        return new PlayerTab(player.getUniqueId().toString(), player, prefix, new SidebarLine(),
                PlayerTab.PushingRule.NEVER, List.of(), color, visibility);
    }

    private static Player player(String name) {
        return player(name, new PlayerNameState(null));
    }

    private static Player player(String name, PlayerNameState state) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> java.util.UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                    case "playerListName" -> {
                        if (args == null || args.length == 0) {
                            yield state.storedPlayerListName == null
                                    ? Component.text(name)
                                    : state.storedPlayerListName;
                        }
                        state.storedPlayerListName = (Component) args[0];
                        state.setterCalls++;
                        yield null;
                    }
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

    private static final class PlayerNameState {
        private Component storedPlayerListName;
        private int setterCalls;

        private PlayerNameState(Component playerListName) {
            this.storedPlayerListName = playerListName;
        }
    }
}
