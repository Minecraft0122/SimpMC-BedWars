package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldStorageFilesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void removesLegacyRuntimeStagingAndArchiveCopiesTogether() throws Exception {
        Path container = temporaryDirectory.resolve("server");
        Path level = container.resolve("world");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container, level);
        Path legacy = layout.legacyWorldFolder("arena").toPath();
        Path runtime = layout.runtimeWorldFolder("arena").toPath();
        Path staging = temporaryDirectory.resolve("plugin/Cache/.source-staging/arena");
        Path archive = temporaryDirectory.resolve("plugin/Cache/arena.zip");
        Files.createDirectories(legacy.resolve("region"));
        Files.createDirectories(runtime.resolve("region"));
        Files.createDirectories(staging.resolve("region"));
        Files.createDirectories(archive.getParent());
        Files.writeString(archive, "cache");

        Path saveBackup = temporaryDirectory.resolve("plugin/Cache/.save-backup/arena");
        Path saveStaging = temporaryDirectory.resolve("plugin/Cache/.save-staging/arena");
        Files.createDirectories(saveBackup.resolve("region"));
        Files.createDirectories(saveStaging.resolve("region"));

        WorldStorageFiles.deleteWorldFiles(layout, archive.toFile(), "arena", true,
                staging.toFile(), saveStaging.toFile(), saveBackup.toFile());

        assertFalse(Files.exists(legacy));
        assertFalse(Files.exists(runtime));
        assertFalse(Files.exists(staging));
        assertFalse(Files.exists(saveStaging));
        assertFalse(Files.exists(saveBackup));
        assertFalse(Files.exists(archive));
    }

    @Test
    void removesWorldIdentityWithoutDeletingRegionOrLegacyLevelData() throws Exception {
        Path world = temporaryDirectory.resolve("arena");
        Files.createDirectories(world.resolve("region"));
        Files.createDirectories(world.resolve("data/paper"));
        Files.writeString(world.resolve("level.dat"), "level");
        Files.writeString(world.resolve("uid.dat"), "uuid");
        Files.writeString(world.resolve("session.lock"), "lock");
        Files.writeString(world.resolve("data/paper/metadata.dat"), "metadata");

        WorldStorageFiles.deleteWorldIdentity(world.toFile());

        assertTrue(Files.exists(world.resolve("level.dat")));
        assertTrue(Files.exists(world.resolve("region")));
        assertFalse(Files.exists(world.resolve("uid.dat")));
        assertFalse(Files.exists(world.resolve("session.lock")));
        assertFalse(Files.exists(world.resolve("data/paper/metadata.dat")));
    }

    @Test
    void mergesPaperRuntimeBackIntoTheLegacyDirectory() throws Exception {
        Path legacy = temporaryDirectory.resolve("server/arena");
        Path runtime = temporaryDirectory.resolve("server/world/dimensions/minecraft/arena");
        Path staging = temporaryDirectory.resolve("server/world/.save-staging/arena");
        Path backup = temporaryDirectory.resolve("server/world/.save-backup/arena");
        Files.createDirectories(legacy.resolve("region"));
        Files.createDirectories(legacy.resolve("data/paper"));
        Files.writeString(legacy.resolve("level.dat"), "legacy-level");
        Files.writeString(legacy.resolve("region/stale.mca"), "stale");
        Files.writeString(legacy.resolve("uid.dat"), "old-identity");
        Files.writeString(legacy.resolve("data/paper/metadata.dat"), "old-metadata");
        Files.createDirectories(runtime.resolve("region"));
        Files.createDirectories(runtime.resolve("data/paper"));
        Files.writeString(runtime.resolve("region/current.mca"), "current");
        Files.writeString(runtime.resolve("data/paper/metadata.dat"), "runtime-metadata");

        WorldStorageFiles.mergeRuntimeIntoLegacy(runtime.toFile(), legacy.toFile(),
                temporaryDirectory.resolve("unused-template").toFile(), staging.toFile(), backup.toFile());

        assertEquals("legacy-level", Files.readString(legacy.resolve("level.dat")));
        assertEquals("current", Files.readString(legacy.resolve("region/current.mca")));
        assertFalse(Files.exists(legacy.resolve("region/stale.mca")));
        assertFalse(Files.exists(legacy.resolve("uid.dat")));
        assertFalse(Files.exists(legacy.resolve("data/paper/metadata.dat")));
        assertFalse(Files.exists(staging));
        assertFalse(Files.exists(backup));
        assertTrue(Files.exists(runtime.resolve("region/current.mca")));
        assertTrue(WorldStorageFiles.isLegacyWorld(legacy.toFile()));
    }

    @Test
    void rejectsRuntimeCopiesWithoutRegionDataBeforeChangingTheLegacySource() throws Exception {
        Path legacy = temporaryDirectory.resolve("server/arena");
        Path runtime = temporaryDirectory.resolve("server/world/dimensions/minecraft/arena");
        Path staging = temporaryDirectory.resolve("server/world/.save-staging/arena");
        Path backup = temporaryDirectory.resolve("server/world/.save-backup/arena");
        Files.createDirectories(legacy.resolve("region"));
        Files.writeString(legacy.resolve("level.dat"), "legacy-level");
        Files.createDirectories(runtime.resolve("data"));

        assertThrows(java.io.IOException.class, () -> WorldStorageFiles.mergeRuntimeIntoLegacy(
                runtime.toFile(), legacy.toFile(), temporaryDirectory.toFile(),
                staging.toFile(), backup.toFile()));

        assertEquals("legacy-level", Files.readString(legacy.resolve("level.dat")));
        assertTrue(Files.isDirectory(legacy.resolve("region")));
        assertFalse(Files.exists(staging));
        assertFalse(Files.exists(backup));
    }
}
