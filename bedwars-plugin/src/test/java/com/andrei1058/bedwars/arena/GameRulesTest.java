package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRulesTest {

    @Test
    void usesTheRequestedFixedDaytime() {
        assertEquals(1000L, GameRules.BEDWARS_DAY_TIME);
    }

    @Test
    void resolvesModernLocatorBarRegistryKey() {
        assertEquals("locator_bar", GameRules.toRegistryKey("locatorBar"));
        assertEquals("locator_bar", GameRules.toRegistryKey("locator_bar"));
        assertEquals("locator_bar", GameRules.toRegistryKey("minecraft:locatorBar"));
    }

    @Test
    void resolvesModernFireSpreadRadiusRegistryKey() {
        assertEquals("fire_spread_radius_around_player",
                GameRules.toRegistryKey("fireSpreadRadiusAroundPlayer"));
    }
}
