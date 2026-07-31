package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static IArena arena() {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
