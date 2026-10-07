package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldStorageLayoutTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsLegacyWorldsInTheBukkitContainer() {
        Path container = temporaryDirectory.resolve("server");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container);

        assertEquals(container.resolve("arena").toFile(), layout.legacyWorldFolder("arena"));
        assertTrue(layout.supportsWorldName("起床战争_双人"));
    }

    @Test
    void createsWorldsByTheLegacyName() {
        WorldStorageLayout layout = WorldStorageLayout.forTests(
                temporaryDirectory.resolve("server"));

        UnsafeValues unsafe = Mockito.mock(UnsafeValues.class);
        Mockito.when(unsafe.getMainLevelName()).thenReturn("world");
        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getUnsafe).thenReturn(unsafe);
            var creator = layout.createWorldCreator("bedwars_solo-01");
            assertEquals("bedwars_solo-01", creator.name());
            assertEquals("minecraft:bedwars_solo-01", creator.key().toString());
            assertEquals("minecraft:overworld", layout.createWorldCreator("world").key().toString());
        }
    }
}
