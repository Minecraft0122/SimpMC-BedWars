package com.andrei1058.bedwars.api.configuration;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @Test
    void arenaLocationUsesBlockCenterAndDropsDirection() {
        Location source = new Location(null, 12.13, 64.92, 8.99, 135.0F, -32.0F);

        Location centered = ConfigManager.toArenaBlockCenter(source);

        assertEquals(12.5, centered.getX());
        assertEquals(64.0, centered.getY());
        assertEquals(8.5, centered.getZ());
        assertEquals(0.0F, centered.getYaw());
        assertEquals(0.0F, centered.getPitch());
        assertEquals("12.5,64.0,8.5", ConfigManager.serializeArenaLocation(source));
    }

    @Test
    void arenaLocationCentersNegativeCoordinatesUsingBlockCoordinates() {
        Location source = new Location(null, -0.01, 70.0, -12.99, 45.0F, 20.0F);

        Location centered = ConfigManager.toArenaBlockCenter(source);

        assertEquals(-0.5, centered.getX());
        assertEquals(70.0, centered.getY());
        assertEquals(-12.5, centered.getZ());
    }

    @Test
    void oldArenaLocationIsCenteredAndDirectionIsRemovedDuringMigration() {
        assertEquals("-0.5,70.0,-12.5",
                ConfigManager.normalizeArenaLocationString("-0.01,70.9,-12.99,45.0,20.0"));
    }

    @Test
    void versionedMigrationRunsOnceAndCopiesNewDefaults() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("legacy-setting", "custom-value");
        configuration.addDefault("new-setting", true);

        boolean migrated = ConfigManager.applyVersionedMigration(configuration, 1, yml -> {
            yml.set("renamed-setting", yml.get("legacy-setting"));
            yml.set("legacy-setting", null);
        });

        assertTrue(migrated);
        assertEquals(1, configuration.getInt(ConfigManager.CONFIG_VERSION_PATH));
        assertEquals("custom-value", configuration.getString("renamed-setting"));
        assertNull(configuration.get("legacy-setting"));
        assertTrue(configuration.getBoolean("new-setting"));
        assertFalse(ConfigManager.applyVersionedMigration(configuration, 1,
                yml -> yml.set("must-not-run", true)));
        assertFalse(configuration.contains("must-not-run"));
        assertTrue(configuration.saveToString().contains("配置文件架构版本"));
    }

    @Test
    void generalLocationMigrationAddsMissingWorld() {
        assertEquals("12.25,64.0,-3.75,90.0,0.0,world",
                ConfigManager.normalizeConfigLocationString("12.25,64.0,-3.75,83.4312,15.0", "world"));
    }

    @Test
    void generalLocationMigrationSupportsWorldWithoutPitch() {
        assertEquals("12.25,64.0,-3.75,-180.0,0.0,lobby",
                ConfigManager.normalizeConfigLocationString("12.25,64.0,-3.75,-179.231,lobby", "world"));
    }

    @Test
    void snapsYawExamplesToNearestCardinalDirection() {
        assertEquals(-90.0F, ConfigManager.snapYawToCardinal(-88.328));
        assertEquals(-180.0F, ConfigManager.snapYawToCardinal(-179.231));
        assertEquals(90.0F, ConfigManager.snapYawToCardinal(83.4312));
        assertEquals(-90.0F, ConfigManager.snapYawToCardinal(270.0));
    }

    @Test
    void serializesGeneralLocationWithCardinalYawAndFlatPitch() {
        Location source = new Location(world("lobby"), 1.25, 64.0, -2.75, -88.328F, 34.5F);

        assertEquals("1.25,64.0,-2.75,-90.0,0.0,lobby",
                ConfigManager.serializeConfigLocation(source));
    }

    @Test
    void readsConfiguredWorldNameBeforeBukkitLoadsThatWorld() {
        assertEquals("separate_lobby", ConfigManager.getWorldNameFromConfigLocation(
                "12.25,64.0,-3.75,90.0,15.0,separate_lobby"));
        assertEquals("legacy_lobby", ConfigManager.getWorldNameFromConfigLocation(
                "12.25,64.0,-3.75,legacy_lobby"));
    }

    @Test
    void radiusCheckRejectsDifferentWorldsWithoutThrowing() {
        Location lobby = new Location(world("lobby"), 0, 64, 0);
        Location arena = new Location(world("arena"), 0, 64, 0);

        assertFalse(ConfigManager.isSameWorldWithin(lobby, arena, 4));
    }

    @Test
    void radiusCheckIncludesConfiguredBoundary() {
        World world = world("arena");
        Location center = new Location(world, 0, 64, 0);
        Location boundary = new Location(world, 3, 68, 0);

        assertTrue(ConfigManager.isSameWorldWithin(center, boundary, 5));
        assertFalse(ConfigManager.isSameWorldWithin(center, boundary, 4.99));
        assertFalse(ConfigManager.isSameWorldWithin(center, center, -1));
    }

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
