package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.stats.GameStatistic;
import com.andrei1058.bedwars.api.arena.stats.GameStatisticProvider;
import com.andrei1058.bedwars.api.arena.stats.GameStatsHolder;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.arena.stats.StatisticsOrdered;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwSidebarTest {

    @Test
    void emptyLobbyOverrideKeepsTheLanguageHeader() {
        List<String> languageHeader = List.of(" ".repeat(BwSidebar.TAB_MIN_WIDTH), "{serverIp}");
        assertSame(languageHeader, BwSidebar.selectLobbyHeader(List.of(), languageHeader));
    }

    @Test
    void configuredLobbyOverrideKeepsTheBuiltInWidthSpacer() {
        List<String> configured = List.of("&b自定义大厅", "&f在线：{on}");
        assertEquals(List.of(" ".repeat(BwSidebar.TAB_MIN_WIDTH), "&b自定义大厅", "&f在线：{on}"),
                BwSidebar.selectLobbyHeader(configured, List.of("built-in")));
    }

    @Test
    void configuredWidthIsNotDuplicated() {
        List<String> configured = List.of(" ".repeat(BwSidebar.TAB_MIN_WIDTH + 16), "&b自定义大厅");
        assertSame(configured, BwSidebar.selectLobbyHeader(configured, List.of("built-in")));
    }

    @Test
    void oldLanguageWidthIsExpandedAtRuntime() {
        List<String> selected = BwSidebar.selectLobbyHeader(List.of(), List.of(" ".repeat(104), "&a{serverIp}"));
        assertEquals(BwSidebar.TAB_MIN_WIDTH, selected.getFirst().length());
        assertEquals("&a{serverIp}", selected.get(1));
    }

    @Test
    void arenaHeadersUseTheSameExpandedWidthAsTheLobby() {
        List<String> selected = BwSidebar.ensureTabWidth(List.of(" ".repeat(104), "&a等待中"));

        assertEquals(BwSidebar.TAB_MIN_WIDTH, selected.getFirst().length());
        assertEquals("&a等待中", selected.get(1));
    }

    @Test
    void playingHeadersInsertGameTimeBeforeTheNextEvent() {
        String gameTime = "&7游戏时间：&a{gameTime}";
        List<String> alive = List.of("地图", "{nextEvent} {time}");
        List<String> eliminated = List.of("地图", "{nextEvent} {time}", "已淘汰");
        List<String> spectator = List.of("地图", "{nextEvent} {time}", "旁观中");

        assertEquals(List.of("地图", gameTime, "{nextEvent} {time}"),
                BwSidebar.insertGameTimeLine(alive, gameTime, GameState.playing));
        assertEquals(List.of("地图", gameTime, "{nextEvent} {time}", "已淘汰"),
                BwSidebar.insertGameTimeLine(eliminated, gameTime, GameState.playing));
        assertEquals(List.of("地图", gameTime, "{nextEvent} {time}", "旁观中"),
                BwSidebar.insertGameTimeLine(spectator, gameTime, GameState.playing));
        assertEquals(List.of("地图", "{nextEvent} {time}"), alive,
                "the configured language list must remain unchanged");
    }

    @Test
    void nonPlayingHeadersAndIntegratedCustomHeadersStayUnchanged() {
        List<String> starting = List.of("{time} 秒后开始");
        List<String> custom = List.of("本局 {gameTime}", "{nextEvent}");

        assertSame(starting, BwSidebar.insertGameTimeLine(
                starting, "游戏时间 {gameTime}", GameState.starting));
        assertSame(custom, BwSidebar.insertGameTimeLine(
                custom, "游戏时间 {gameTime}", GameState.playing));
    }

    @Test
    void insertedGameTimeKeepsTheWidthSpacerFirst() {
        List<String> selected = BwSidebar.ensureTabWidth(BwSidebar.insertGameTimeLine(
                List.of("地图", "{nextEvent}"), "游戏时间 {gameTime}", GameState.playing));

        assertEquals(BwSidebar.TAB_MIN_WIDTH, selected.getFirst().length());
        assertEquals(List.of("地图", "游戏时间 {gameTime}", "{nextEvent}"), selected.subList(1, 4));
    }

    @Test
    void resynchronizesTeamsWhenArenaOrGameStateChanges() {
        IArena firstArena = arena();
        IArena secondArena = arena();

        assertFalse(BwSidebar.shouldResynchronizeTabContext(
                firstArena, GameState.waiting, firstArena, GameState.waiting));
        assertTrue(BwSidebar.shouldResynchronizeTabContext(
                firstArena, GameState.waiting, firstArena, GameState.starting));
        assertTrue(BwSidebar.shouldResynchronizeTabContext(
                firstArena, GameState.playing, secondArena, GameState.playing));
        assertTrue(BwSidebar.shouldResynchronizeTabContext(
                firstArena, GameState.restarting, null, null));
    }

    @Test
    void restartingSidebarResolvesTopPlaceholdersWithoutDependingOnGameEndChat() {
        IArena arena = arena();
        StatisticsOrdered statistics = emptyStatistics();
        AtomicInteger creations = new AtomicInteger();

        StatisticsOrdered resolved = BwSidebar.resolveTopStatistics(
                null, arena, GameState.restarting, null, () -> {
                    creations.incrementAndGet();
                    return statistics;
                });

        assertSame(statistics, resolved);
        assertEquals(1, creations.get());
        assertEquals("Nobody - 0", resolved.newParser().parseString(
                "{topTeamColor}{topPlayerDisplayName} - {topValue}", null, "Nobody"));

        assertSame(statistics, BwSidebar.resolveTopStatistics(
                arena, arena, GameState.restarting, statistics, () -> {
                    creations.incrementAndGet();
                    return null;
                }));
        assertEquals(1, creations.get(), "an attached game-end snapshot must be reused");
        assertNull(BwSidebar.resolveTopStatistics(
                arena, arena, GameState.playing, statistics, () -> statistics));
    }

    @Test
    void restartingSidebarDoesNotReuseStatisticsFromAnotherArena() {
        IArena firstArena = arena();
        IArena secondArena = arena();
        StatisticsOrdered firstStatistics = emptyStatistics();
        StatisticsOrdered secondStatistics = emptyStatistics();

        assertSame(secondStatistics, BwSidebar.resolveTopStatistics(
                firstArena, secondArena, GameState.restarting, firstStatistics,
                () -> secondStatistics));
    }

    @Test
    void restartingSidebarNeverLeaksWinnerPlaceholdersWhenAGameHasNoWinner() {
        String template = "{winnerTeamColor}{winnerTeamName} / {winnerTeamLetter}";

        assertEquals("Nobody / ",
                BwSidebar.replaceWinnerPlaceholders(template, null, null, "Nobody"));
        assertEquals(ChatColor.RED + "Red / " + ChatColor.RED + "R",
                BwSidebar.replaceWinnerPlaceholders(template, "Red", ChatColor.RED, "Nobody"));
    }

    private static IArena arena() {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static StatisticsOrdered emptyStatistics() {
        AtomicReference<GameStatsHolder> holderReference = new AtomicReference<>();
        IArena arena = (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getStatsHolder" -> holderReference.get();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        GameStatisticProvider<GameStatistic<?>> provider = new GameStatisticProvider<>() {
            @Override
            public String getIdentifier() {
                return "kills";
            }

            @Override
            public Plugin getOwner() {
                return null;
            }

            @Override
            public GameStatistic<?> getDefault() {
                return null;
            }

            @Override
            public String getVoidReplacement(Language language) {
                return "0";
            }
        };
        GameStatsHolder holder = (GameStatsHolder) Proxy.newProxyInstance(
                GameStatsHolder.class.getClassLoader(), new Class<?>[]{GameStatsHolder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasStatistic" -> true;
                    case "getOrderedBy" -> List.<Optional<com.andrei1058.bedwars.api.arena.stats.PlayerGameStats>>of();
                    case "getRegistered" -> List.of("kills");
                    case "getProvider" -> provider;
                    case "getArena" -> arena;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        holderReference.set(holder);
        return new StatisticsOrdered(arena, "kills");
    }
}
