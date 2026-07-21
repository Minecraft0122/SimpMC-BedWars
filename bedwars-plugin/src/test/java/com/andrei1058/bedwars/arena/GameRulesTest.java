package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRulesTest {

    @Test
    void resolvesModernLocatorBarRegistryKey() {
        assertEquals("locator_bar", GameRules.toRegistryKey("locatorBar"));
        assertEquals("locator_bar", GameRules.toRegistryKey("locator_bar"));
        assertEquals("locator_bar", GameRules.toRegistryKey("minecraft:locatorBar"));
    }
}
