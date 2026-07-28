package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRulesTest {

    @Test
    void usesTheRequestedFixedTime() {
        assertEquals(12000L, GameRules.BEDWARS_FIXED_TIME);
    }

    @Test
    void allowsOnlyTimeSkipsThatEndAtTheFixedTime() {
        assertTrue(GameRules.reachesBedWarsFixedTime(18000L, 18000L));
        assertTrue(GameRules.reachesBedWarsFixedTime(12000L, 24000L));
        assertFalse(GameRules.reachesBedWarsFixedTime(18000L, 6000L));
        assertFalse(GameRules.reachesBedWarsFixedTime(12000L, 1L));
    }

    @Test
    void evaluatesExtremeTimeSkipsWithoutOverflow() {
        long skipToFixedTime = Math.floorMod(12000L - Math.floorMod(Long.MAX_VALUE, 24000L), 24000L);
        assertTrue(GameRules.reachesBedWarsFixedTime(Long.MAX_VALUE, skipToFixedTime));
        assertFalse(GameRules.reachesBedWarsFixedTime(Long.MAX_VALUE, Long.MIN_VALUE));
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
