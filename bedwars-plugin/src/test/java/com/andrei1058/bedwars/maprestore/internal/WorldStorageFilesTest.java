package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldStorageFilesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void removesBukkitWorldAndOptionallyItsArchive() throws Exception {
        WorldStorageLayout layout = WorldStorageLayout.forTests(temporaryDirectory.resolve("server"));
        Path world = layout.legacyWorldFolder("arena").toPath();
        Path archive = temporaryDirectory.resolve("arena.zip");
        Files.createDirectories(world.resolve("region"));
        Files.writeString(archive, "cache");

        WorldStorageFiles.deleteWorldFiles(layout, archive.toFile(), "arena", false);
        assertFalse(Files.exists(world));
        assertEquals("cache", Files.readString(archive));

        WorldStorageFiles.deleteWorldFiles(layout, archive.toFile(), "arena", true);
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

}
