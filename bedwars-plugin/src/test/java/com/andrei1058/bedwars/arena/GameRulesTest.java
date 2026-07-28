package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRulesTest {

    @Test
    void usesTheRequestedFixedDaytime() {
        assertEquals(6000L, GameRules.BEDWARS_DAY_TIME);
    }

    @Test
    void allowsOnlyTimeSkipsThatEndAtTheFixedDaytime() {
        assertTrue(GameRules.reachesBedWarsDayTime(18000L, 12000L));
        assertTrue(GameRules.reachesBedWarsDayTime(6000L, 24000L));
        assertFalse(GameRules.reachesBedWarsDayTime(18000L, 6000L));
        assertFalse(GameRules.reachesBedWarsDayTime(6000L, 1L));
    }

    @Test
    void evaluatesExtremeTimeSkipsWithoutOverflow() {
        long skipToNoon = Math.floorMod(6000L - Math.floorMod(Long.MAX_VALUE, 24000L), 24000L);
        assertTrue(GameRules.reachesBedWarsDayTime(Long.MAX_VALUE, skipToNoon));
        assertFalse(GameRules.reachesBedWarsDayTime(Long.MAX_VALUE, Long.MIN_VALUE));
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

    @Test
    void resolvesRandomTickSpeedRegistryKey() {
        assertEquals("random_tick_speed", GameRules.toRegistryKey("randomTickSpeed"));
    }
}
