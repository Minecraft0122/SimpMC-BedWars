package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import org.bukkit.World;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRulesTest {

    @Test
    void usesVanillaNoonTime() {
        assertEquals(6000L, GameRules.VANILLA_NOON_TIME);
    }

    @Test
    void doesNotSetTimeInDimensionsWithoutAWritableClock() {
        World fixedTimeWorld = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isFixedTime")) return true;
                    throw new AssertionError("Fixed-time world must not call " + method.getName());
                });

        GameRules.enforceFixedTime(fixedTimeWorld);
    }

    @Test
    void redirectsEveryTimeChangeToTheFixedTime() {
        assertEquals(12000L, GameRules.skipAmountToFixedTime(18000L));
        assertEquals(0L, GameRules.skipAmountToFixedTime(6000L));
        assertEquals(23999L, GameRules.skipAmountToFixedTime(6001L));
    }

    @Test
    void calculatesExtremeTimeCorrectionsWithoutOverflow() {
        assertSkipLandsAtNoon(Long.MAX_VALUE);
        assertSkipLandsAtNoon(Long.MAX_VALUE - 1L);
        assertSkipLandsAtNoon(Long.MIN_VALUE);
        assertSkipLandsAtNoon(Long.MIN_VALUE + 1L);
    }

    @Test
    void resolvesModernLocatorBarRegistryKey() {
        assertEquals("locator_bar", GameRules.toRegistryKey("locatorBar"));
        assertEquals("locator_bar", GameRules.toRegistryKey("locator_bar"));
        assertEquals("locator_bar", GameRules.toRegistryKey("minecraft:locatorBar"));
    }

    @Test
    void mapsLegacyEnvironmentNamesToPaper12111RegistryKeys() {
        assertEquals("advance_time", GameRules.toRegistryKey("doDaylightCycle"));
        assertEquals("advance_weather", GameRules.toRegistryKey("do_weather_cycle"));
        assertEquals("spawn_mobs", GameRules.toRegistryKey("doMobSpawning"));
        assertEquals("show_advancement_messages", GameRules.toRegistryKey("announceAdvancements"));
        assertEquals("spawn_phantoms", GameRules.toRegistryKey("doInsomnia"));
        assertEquals("immediate_respawn", GameRules.toRegistryKey("doImmediateRespawn"));
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

    private static void assertSkipLandsAtNoon(long currentFullTime) {
        long correctedFullTime = currentFullTime + GameRules.skipAmountToFixedTime(currentFullTime);
        assertEquals(GameRules.VANILLA_NOON_TIME, Math.floorMod(correctedFullTime, 24000L));
    }
}
