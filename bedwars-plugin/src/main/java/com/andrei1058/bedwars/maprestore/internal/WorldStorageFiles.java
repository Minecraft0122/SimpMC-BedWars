package com.andrei1058.bedwars.maprestore.internal;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class WorldStorageFiles {

    private WorldStorageFiles() {
    }

    static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    static boolean hasLegacyLevelData(File worldFolder) {
        return new File(worldFolder, "level.dat").isFile();
    }

    static boolean isLegacyWorld(File worldFolder) {
        return worldFolder.isDirectory()
                && hasLegacyLevelData(worldFolder)
                && new File(worldFolder, "region").isDirectory();
    }

    static void deleteWorldFiles(
            WorldStorageLayout layout,
            File archive,
            String worldName,
            boolean deleteArchive
    ) throws IOException {
        File legacy = layout.legacyWorldFolder(worldName);
        deleteDirectory(legacy);
        if (deleteArchive) Files.deleteIfExists(archive.toPath());
    }

    static void deleteWorldIdentity(File worldFolder) throws IOException {
        Files.deleteIfExists(new File(worldFolder, "session.lock").toPath());
        Files.deleteIfExists(new File(worldFolder, "uid.dat").toPath());
        Files.deleteIfExists(new File(worldFolder, "data/paper/metadata.dat").toPath());
        Files.deleteIfExists(new File(worldFolder, "data/paper/metadata.dat_old").toPath());
    }
}
