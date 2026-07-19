package com.andrei1058.bedwars.sidebar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BwSidebarTest {

    @Test
    void emptyLobbyOverrideKeepsTheLanguageHeader() {
        List<String> languageHeader = List.of(" ".repeat(BwSidebar.LOBBY_TAB_MIN_WIDTH), "{serverIp}");
        assertSame(languageHeader, BwSidebar.selectLobbyHeader(List.of(), languageHeader));
    }

    @Test
    void configuredLobbyOverrideKeepsTheBuiltInWidthSpacer() {
        List<String> configured = List.of("&b自定义大厅", "&f在线：{on}");
        assertEquals(List.of(" ".repeat(BwSidebar.LOBBY_TAB_MIN_WIDTH), "&b自定义大厅", "&f在线：{on}"),
                BwSidebar.selectLobbyHeader(configured, List.of("built-in")));
    }

    @Test
    void configuredWidthIsNotDuplicated() {
        List<String> configured = List.of(" ".repeat(BwSidebar.LOBBY_TAB_MIN_WIDTH + 16), "&b自定义大厅");
        assertSame(configured, BwSidebar.selectLobbyHeader(configured, List.of("built-in")));
    }

    @Test
    void oldLanguageWidthIsExpandedAtRuntime() {
        List<String> selected = BwSidebar.selectLobbyHeader(List.of(), List.of(" ".repeat(104), "&a{serverIp}"));
        assertEquals(BwSidebar.LOBBY_TAB_MIN_WIDTH, selected.getFirst().length());
        assertEquals("&a{serverIp}", selected.get(1));
    }
}
