package com.andrei1058.bedwars.sidebar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class BwSidebarTest {

    @Test
    void emptyLobbyOverrideKeepsTheOriginalLanguageHeader() {
        List<String> languageHeader = List.of("original-width", "{serverIp}");
        assertSame(languageHeader, BwSidebar.selectLobbyHeader(List.of(), languageHeader));
    }

    @Test
    void configuredLobbyOverrideOnlyReplacesTheHeader() {
        List<String> configured = List.of("&b自定义大厅", "&f在线：{on}");
        assertSame(configured, BwSidebar.selectLobbyHeader(configured, List.of("original")));
    }
}
