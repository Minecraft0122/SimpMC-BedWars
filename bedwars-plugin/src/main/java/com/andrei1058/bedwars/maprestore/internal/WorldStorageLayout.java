package com.andrei1058.bedwars.maprestore.internal;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.WorldCreator;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/** Paper 1.21.11 的 Bukkit 世界目录。 */
public final class WorldStorageLayout {
    private final File worldContainer;

    private WorldStorageLayout(File worldContainer) {
        this.worldContainer = Objects.requireNonNull(worldContainer, "worldContainer").getAbsoluteFile();
    }

    public static WorldStorageLayout detect() {
        return detect(Bukkit.getServer());
    }

    static WorldStorageLayout detect(Server server) {
        return new WorldStorageLayout(Objects.requireNonNull(server, "server").getWorldContainer());
    }

    static WorldStorageLayout forTests(Path worldContainer) {
        return new WorldStorageLayout(worldContainer.toFile());
    }

    /** The authoritative BedWars map directory, always the legacy path. */
    public File legacyWorldFolder(String worldName) {
        return new File(worldContainer, requireSafeName(worldName));
    }

    public boolean supportsWorldName(String worldName) {
        return WorldNameValidator.isSafe(worldName);
    }

    public WorldCreator createWorldCreator(String worldName) {
        return new WorldCreator(requireSafeName(worldName));
    }

    public File getWorldContainer() {
        return worldContainer;
    }

    private static String requireSafeName(String worldName) {
        if (!WorldNameValidator.isSafe(worldName)) {
            throw new IllegalArgumentException("Unsafe world name: " + worldName);
        }
        return worldName;
    }

}
