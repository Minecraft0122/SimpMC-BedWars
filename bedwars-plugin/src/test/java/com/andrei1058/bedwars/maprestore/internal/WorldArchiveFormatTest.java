package com.andrei1058.bedwars.maprestore.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldArchiveFormatTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsOnlyArchivesWithLegacyLevelData() throws Exception {
        File legacy = archive("legacy.zip", "level.dat", "region/r.0.0.mca");
        File dimensionOnly = archive("dimension.zip", "region/r.0.0.mca", "data/paper/metadata.dat");
        File nestedLevelData = archive("nested.zip", "world/level.dat", "region/r.0.0.mca");
        File backupOnly = archive("backup-only.zip", "level.dat_old", "region/r.0.0.mca");

        assertTrue(WorldArchiveFormat.containsLegacyLevelData(legacy));
        assertTrue(WorldArchiveFormat.containsDirectory(legacy, "region/"));
        assertTrue(WorldArchiveFormat.isLegacyWorld(legacy));
        assertFalse(WorldArchiveFormat.containsLegacyLevelData(dimensionOnly));
        assertFalse(WorldArchiveFormat.isLegacyWorld(dimensionOnly));
        assertFalse(WorldArchiveFormat.isLegacyWorld(nestedLevelData));
        assertFalse(WorldArchiveFormat.isLegacyWorld(backupOnly));
    }

    private File archive(String name, String... entries) throws Exception {
        File archive = temporaryDirectory.resolve(name).toFile();
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
            for (String entry : entries) {
                output.putNextEntry(new ZipEntry(entry));
                output.write(1);
                output.closeEntry();
            }
        }
        return archive;
    }
}
