package com.andrei1058.bedwars.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipFileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsPathTraversalBeforeExtractingAnyEntry() throws IOException {
        Path archive = tempDir.resolve("malicious.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            write(output, "region/safe.mca", "safe");
            write(output, "../escaped.txt", "escaped");
        }

        Path destination = tempDir.resolve("world");
        assertThrows(IOException.class,
                () -> ZipFileUtil.unzipFileIntoDirectory(archive.toFile(), destination.toFile()));
        assertFalse(Files.exists(tempDir.resolve("escaped.txt")));
        assertFalse(Files.exists(destination.resolve("region/safe.mca")));
    }

    @Test
    void extractsValidatedWorldFiles() throws IOException {
        Path archive = tempDir.resolve("world.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            write(output, "region/r.0.0.mca", "region-data");
        }

        Path destination = tempDir.resolve("world");
        ZipFileUtil.unzipFileIntoDirectory(archive.toFile(), destination.toFile());

        assertEquals("region-data", Files.readString(destination.resolve("region/r.0.0.mca")));
    }

    private static void write(ZipOutputStream output, String name, String value) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
