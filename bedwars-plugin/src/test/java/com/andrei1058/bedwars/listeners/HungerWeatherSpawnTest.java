package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.server.ServerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungerWeatherSpawnTest {

    @Test
    void sharedServersLockWeatherOnlyInBedWarsWorlds() {
        assertTrue(HungerWeatherSpawn.shouldLockWeather(ServerType.SHARED, true, false));
        assertTrue(HungerWeatherSpawn.shouldLockWeather(ServerType.SHARED, false, true));
        assertFalse(HungerWeatherSpawn.shouldLockWeather(ServerType.SHARED, false, false));
    }

    @Test
    void dedicatedModesKeepEveryWorldClear() {
        assertTrue(HungerWeatherSpawn.shouldLockWeather(ServerType.MULTIARENA, false, false));
        assertTrue(HungerWeatherSpawn.shouldLockWeather(ServerType.BUNGEE, false, false));
    }
}
