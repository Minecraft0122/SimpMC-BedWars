package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LobbyItemTextTest {

    @Test
    void customReturnItemGetsChineseFallbackWithoutLanguageWarnings() {
        assertEquals("&e回到主大厅", LobbyItemText.fallbackName("paper", "bw leave", "bw"));
        assertEquals(List.of("&f右键返回主大厅！"),
                LobbyItemText.fallbackLore("paper", "bw leave", "bw"));
    }

    @Test
    void ordinaryCustomItemGetsAnEmptyOptionalLore() {
        assertEquals("&fselector", LobbyItemText.fallbackName("selector", "bw gui", "bw"));
        assertEquals(List.of(), LobbyItemText.fallbackLore("selector", "bw gui", "bw"));
    }

    @Test
    void generatedLanguagePlaceholdersAreRecognized() {
        org.junit.jupiter.api.Assertions.assertTrue(
                LobbyItemText.isGeneratedName("&cName not set at: &flobby-items-paper-name",
                        "lobby-items-paper-name"));
        org.junit.jupiter.api.Assertions.assertTrue(
                LobbyItemText.isGeneratedLore(List.of("&cLore not set at:", " &flobby-items-paper-lore"),
                        "lobby-items-paper-lore"));
        org.junit.jupiter.api.Assertions.assertFalse(
                LobbyItemText.isGeneratedLore(List.of("&f右键返回主大厅！"), "lobby-items-paper-lore"));
        org.junit.jupiter.api.Assertions.assertFalse(
                LobbyItemText.isGeneratedName("&cName not set at: &f自定义说明", "lobby-items-paper-name"));
    }
}
