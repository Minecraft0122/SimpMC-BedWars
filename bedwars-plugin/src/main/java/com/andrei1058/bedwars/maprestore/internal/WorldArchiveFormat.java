package com.andrei1058.bedwars.maprestore.internal;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class WorldArchiveFormat {

    private WorldArchiveFormat() {
    }

    static boolean containsLegacyLevelData(File archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            return zip.stream()
                    .map(ZipEntry::getName)
                    .map(WorldArchiveFormat::normalize)
                    .anyMatch(name -> name.equals("level.dat"));
        }
    }

    static boolean isLegacyWorld(File archive) throws IOException {
        boolean hasLevelData = false;
        boolean hasRegion = false;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = normalize(entries.nextElement().getName());
                if (name.equals("level.dat")) hasLevelData = true;
                if (name.startsWith("region/")) hasRegion = true;
                if (hasLevelData && hasRegion) return true;
            }
        }
        return false;
    }

    static boolean containsDirectory(File archive, String directory) throws IOException {
        String normalized = normalize(directory);
        try (ZipFile zip = new ZipFile(archive)) {
            return zip.stream()
                    .map(ZipEntry::getName)
                    .map(WorldArchiveFormat::normalize)
                    .anyMatch(name -> name.startsWith(normalized));
        }
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }
}
