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
}
