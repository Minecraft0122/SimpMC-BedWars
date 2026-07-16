package com.andrei1058.bedwars.api.configuration;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

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
    }
}
