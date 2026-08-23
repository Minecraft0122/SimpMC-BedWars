package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldStorageLayoutTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsLegacyWorldsInTheBukkitContainer() {
        Path container = temporaryDirectory.resolve("server");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container, null);

        assertTrue(!layout.usesDimensionStorage());
        assertEquals(container.resolve("arena").toFile(), layout.legacyWorldFolder("arena"));
        assertEquals(container.resolve("arena").toFile(), layout.runtimeWorldFolder("arena"));
        assertTrue(layout.supportsWorldName("起床战争_双人"));
    }

    @Test
    void keepsPaper26ServerLayoutsOnTheLegacyMapPath() {
        Path container = temporaryDirectory.resolve("server");
        Path level = container.resolve("world");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container, level);

        assertTrue(layout.usesDimensionStorage());
        assertEquals(container.resolve("arena").toFile(), layout.legacyWorldFolder("arena"));
        assertEquals(level.resolve("dimensions/minecraft/arena").toFile(),
                layout.runtimeWorldFolder("arena"));
        assertEquals(level.resolve("dimensions/minecraft").toFile(), layout.runtimeWorldsDirectory());
        assertEquals(level.toFile(), layout.levelDirectory());
        assertTrue(layout.supportsWorldName("bedwars_solo-01"));
        assertTrue(!layout.supportsWorldName("Arena"));
        assertTrue(!layout.supportsWorldName("起床战争_双人"));
    }

    @Test
    void createsWorldsByTheLegacyName() {
        WorldStorageLayout layout = WorldStorageLayout.forTests(
                temporaryDirectory.resolve("server"), temporaryDirectory.resolve("server/world"));

        var creator = layout.createWorldCreator("bedwars_solo-01");

        assertEquals("bedwars_solo-01", creator.name());
        assertEquals("minecraft:bedwars_solo-01", creator.key().toString());
    }
}
