package com.andrei1058.bedwars.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ZipFileUtil {

    private ZipFileUtil() {
    }

    public static void zipDirectory(File dir, File zipFile) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(zipFile);
             ZipOutputStream zout = new ZipOutputStream(fout)) {
            zipSubDirectory("", dir, zout);
        }
    }

    private static void zipSubDirectory(String basePath, File dir, ZipOutputStream zout) throws IOException {
        byte[] buffer = new byte[4096];
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                String path = basePath + file.getName() + "/";
                zout.putNextEntry(new ZipEntry(path));
                zipSubDirectory(path, file, zout);
                zout.closeEntry();
            } else {
                try (FileInputStream fin = new FileInputStream(file)) {
                    zout.putNextEntry(new ZipEntry(basePath + file.getName()));
                    int length;
                    while ((length = fin.read(buffer)) > 0) {
                        zout.write(buffer, 0, length);
                    }
                    zout.closeEntry();
                }
            }
        }
    }

    public static void unzipFileIntoDirectory(File file, File jiniHomeParentDir) throws IOException {
        if (!file.exists()) return;

        Path destination = jiniHomeParentDir.toPath().toAbsolutePath().normalize();
        Files.createDirectories(destination);

        try (ZipFile zipFile = new ZipFile(file)) {
            // Validate the entire archive before writing anything. This prevents a
            // later malicious entry from leaving a partially extracted world.
            Enumeration<? extends ZipEntry> validationEntries = zipFile.entries();
            while (validationEntries.hasMoreElements()) {
                resolveEntry(destination, validationEntries.nextElement());
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path output = resolveEntry(destination, entry);
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }

                Path parent = output.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (InputStream input = zipFile.getInputStream(entry)) {
                    Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path resolveEntry(Path destination, ZipEntry entry) throws IOException {
        Path output = destination.resolve(entry.getName()).normalize();
        if (!output.startsWith(destination)) {
            throw new IOException("ZIP entry escapes destination directory: " + entry.getName());
        }
        return output;
    }
}
