package com.andrei1058.bedwars.maprestore.internal;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Preserves an authoritative legacy world directory while Paper 26+ migrates
 * a disposable copy into its runtime dimension directory.
 */
public final class LegacyWorldSourceGuard {

    private static final String LOBBY_STAGING_DIRECTORY = ".simpmc-bedwars-lobby-source-staging";

    private LegacyWorldSourceGuard() {
    }

    public static File lobbyStagingFolder(WorldStorageLayout layout, String worldName) {
        layout.legacyWorldFolder(worldName);
        return new File(layout.levelDirectory(), LOBBY_STAGING_DIRECTORY + "/" + worldName);
    }

    public static boolean hasRecoverableLobbySource(WorldStorageLayout layout, String worldName) {
        if (WorldStorageFiles.isLegacyWorld(layout.legacyWorldFolder(worldName))) return true;
        return layout.usesDimensionStorage()
                && WorldStorageFiles.isLegacyWorld(lobbyStagingFolder(layout, worldName));
    }

    public static void prepare(WorldStorageLayout layout, String worldName, File staging) throws IOException {
        if (!layout.usesDimensionStorage()) return;
        if (!layout.supportsWorldName(worldName)) {
            throw new IOException("Paper 26+ does not support this world name: " + worldName);
        }

        File source = layout.legacyWorldFolder(worldName);
        recoverStagedSource(source, staging);
        if (!WorldStorageFiles.isLegacyWorld(source)) {
            throw new IOException("World is not in legacy Bukkit format: " + source);
        }

        WorldStorageFiles.deleteDirectory(layout.runtimeWorldFolder(worldName));
        File parent = staging.getParentFile();
        if (parent != null) parent.mkdirs();
        WorldStorageFiles.moveDirectory(source, staging);
        try {
            FileUtils.copyDirectory(staging, source);
        } catch (IOException exception) {
            try {
                WorldStorageFiles.deleteDirectory(source);
                WorldStorageFiles.moveDirectory(staging, source);
            } catch (IOException restoreException) {
                exception.addSuppressed(restoreException);
            }
            throw exception;
        }
    }

    public static void restore(WorldStorageLayout layout, String worldName, File staging) throws IOException {
        if (!layout.usesDimensionStorage() || !staging.exists()) return;
        File source = layout.legacyWorldFolder(worldName);
        WorldStorageFiles.deleteDirectory(source);
        WorldStorageFiles.moveDirectory(staging, source);
    }

    private static void recoverStagedSource(File source, File staging) throws IOException {
        if (!staging.exists()) return;
        WorldStorageFiles.deleteDirectory(source);
        WorldStorageFiles.moveDirectory(staging, source);
    }
}
