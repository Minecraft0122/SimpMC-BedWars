package com.andrei1058.bedwars.maprestore.internal;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class WorldStorageFiles {

    private WorldStorageFiles() {
    }

    static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    static void moveDirectory(File from, File to) throws IOException {
        Path source = from.toPath().toAbsolutePath().normalize();
        Path target = to.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) throw new IOException("Source directory does not exist: " + source);
        if (Files.exists(target)) throw new IOException("Target directory already exists: " + target);
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (IOException ignored) {
            if (Files.exists(target)) throw ignored;
        }
        try {
            Files.move(source, target);
        } catch (IOException exception) {
            if (Files.exists(target)) {
                try {
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                    throw exception;
                }
            }
            try {
                FileUtils.copyDirectory(source.toFile(), target.toFile());
            } catch (IOException copyException) {
                try {
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException cleanupException) {
                    copyException.addSuppressed(cleanupException);
                }
                throw copyException;
            }
            FileUtils.deleteDirectory(source.toFile());
        }
    }

    static void mergeRuntimeIntoLegacy(
            File runtime,
            File legacy,
            File levelTemplate,
            File staging,
            File backup
    ) throws IOException {
        if (!runtime.isDirectory()) {
            throw new IOException("Runtime world directory does not exist: " + runtime);
        }
        if (!new File(runtime, "region").isDirectory()) {
            throw new IOException("Runtime world has no region directory: " + runtime);
        }

        deleteDirectory(staging);
        deleteDirectory(backup);
        if (legacy.isDirectory()) {
            FileUtils.copyDirectory(legacy, staging);
        } else {
            Files.createDirectories(staging.toPath());
            copyLevelData(levelTemplate, staging);
        }

        File[] runtimeChildren = runtime.listFiles();
        if (runtimeChildren != null) {
            for (File child : runtimeChildren) {
                String name = child.getName().toLowerCase(Locale.ROOT);
                if (name.equals("level.dat") || name.equals("level.dat_old")
                        || name.equals("session.lock") || name.equals("uid.dat")) {
                    continue;
                }
                File target = new File(staging, child.getName());
                deletePath(target);
                if (child.isDirectory()) {
                    FileUtils.copyDirectory(child, target);
                } else {
                    Files.copy(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        if (!hasLegacyLevelData(staging)) {
            copyLevelData(levelTemplate, staging);
        }
        if (!hasLegacyLevelData(staging)) {
            throw new IOException("Legacy level.dat is missing after merge: " + staging);
        }
        deleteWorldIdentity(staging);
        if (!new File(staging, "region").isDirectory()) {
            throw new IOException("Runtime world has no region directory: " + runtime);
        }

        boolean sourceBackedUp = false;
        try {
            if (legacy.exists()) {
                moveDirectory(legacy, backup);
                sourceBackedUp = true;
            }
            moveDirectory(staging, legacy);
        } catch (IOException exception) {
            if (sourceBackedUp && backup.exists() && !legacy.exists()) {
                try {
                    moveDirectory(backup, legacy);
                } catch (IOException restoreException) {
                    exception.addSuppressed(restoreException);
                }
            }
            throw exception;
        }
        deleteDirectory(backup);
    }

    static boolean hasLegacyLevelData(File worldFolder) {
        return new File(worldFolder, "level.dat").isFile();
    }

    static boolean isLegacyWorld(File worldFolder) {
        return worldFolder.isDirectory()
                && hasLegacyLevelData(worldFolder)
                && new File(worldFolder, "region").isDirectory();
    }

    private static void copyLevelData(File template, File target) throws IOException {
        for (String name : new String[]{"level.dat", "level.dat_old"}) {
            File source = new File(template, name);
            if (source.isFile()) {
                Files.copy(source.toPath(), new File(target, name).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deletePath(File path) throws IOException {
        if (path.isDirectory()) {
            deleteDirectory(path);
        } else {
            Files.deleteIfExists(path.toPath());
        }
    }

    static void deleteWorldFiles(
            WorldStorageLayout layout,
            File archive,
            String worldName,
            boolean deleteArchive,
            File... temporaryFolders
    ) throws IOException {
        File legacy = layout.legacyWorldFolder(worldName);
        File runtime = layout.runtimeWorldFolder(worldName);
        deleteDirectory(legacy);
        if (!runtime.equals(legacy)) deleteDirectory(runtime);
        for (File temporary : temporaryFolders) deleteDirectory(temporary);
        if (deleteArchive) Files.deleteIfExists(archive.toPath());
    }

    static void deleteWorldIdentity(File worldFolder) throws IOException {
        Files.deleteIfExists(new File(worldFolder, "session.lock").toPath());
        Files.deleteIfExists(new File(worldFolder, "uid.dat").toPath());
        Files.deleteIfExists(new File(worldFolder, "data/paper/metadata.dat").toPath());
        Files.deleteIfExists(new File(worldFolder, "data/paper/metadata.dat_old").toPath());
    }
}
