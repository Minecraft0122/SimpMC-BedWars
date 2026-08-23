package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyWorldSourceGuardTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void restoresTheLegacyLobbyAfterPaperMigratesItsDisposableCopy() throws Exception {
        Path container = temporaryDirectory.resolve("server");
        Path level = container.resolve("world");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container, level);
        Path source = layout.legacyWorldFolder("lobby").toPath();
        Path runtime = layout.runtimeWorldFolder("lobby").toPath();
        File staging = LegacyWorldSourceGuard.lobbyStagingFolder(layout, "lobby");
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("level.dat"), "authoritative-level");
        Files.writeString(source.resolve("region/r.0.0.mca"), "authoritative-region");
        Files.createDirectories(runtime.resolve("region"));
        Files.writeString(runtime.resolve("region/stale.mca"), "stale-runtime");

        LegacyWorldSourceGuard.prepare(layout, "lobby", staging);

        assertTrue(WorldStorageFiles.isLegacyWorld(source.toFile()));
        assertTrue(WorldStorageFiles.isLegacyWorld(staging));
        assertTrue(LegacyWorldSourceGuard.hasRecoverableLobbySource(layout, "lobby"));
        assertFalse(Files.exists(runtime));

        WorldStorageFiles.deleteDirectory(source.toFile());
        Files.createDirectories(runtime.resolve("region"));
        Files.writeString(runtime.resolve("region/current.mca"), "runtime-copy");
        LegacyWorldSourceGuard.restore(layout, "lobby", staging);

        assertEquals("authoritative-level", Files.readString(source.resolve("level.dat")));
        assertEquals("authoritative-region", Files.readString(source.resolve("region/r.0.0.mca")));
        assertFalse(staging.exists());
        assertTrue(Files.exists(runtime.resolve("region/current.mca")));
    }

    @Test
    void recognizesAnInterruptedStagingDirectoryAsTheAuthoritativeLobbySource() throws Exception {
        Path container = temporaryDirectory.resolve("interrupted-server");
        Path level = container.resolve("world");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container, level);
        File staging = LegacyWorldSourceGuard.lobbyStagingFolder(layout, "lobby");
        Files.createDirectories(staging.toPath().resolve("region"));
        Files.writeString(staging.toPath().resolve("level.dat"), "authoritative-level");

        assertTrue(LegacyWorldSourceGuard.hasRecoverableLobbySource(layout, "lobby"));
    }

    @Test
    void leavesLegacyServersUntouched() throws Exception {
        Path container = temporaryDirectory.resolve("legacy-server");
        WorldStorageLayout layout = WorldStorageLayout.forTests(container, null);
        Path source = layout.legacyWorldFolder("lobby").toPath();
        File staging = LegacyWorldSourceGuard.lobbyStagingFolder(layout, "lobby");
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("level.dat"), "level");

        LegacyWorldSourceGuard.prepare(layout, "lobby", staging);
        LegacyWorldSourceGuard.restore(layout, "lobby", staging);

        assertTrue(WorldStorageFiles.isLegacyWorld(source.toFile()));
        assertFalse(staging.exists());
    }
}
