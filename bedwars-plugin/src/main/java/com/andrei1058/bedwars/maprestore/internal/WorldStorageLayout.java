package com.andrei1058.bedwars.maprestore.internal;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.WorldCreator;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Keeps the BedWars source map and cache in the legacy Bukkit world layout.
 * Paper 26+ still needs a dimension directory for its loaded runtime copy,
 * but that directory is never treated as a map source or archive format.
 */
public final class WorldStorageLayout {

    private static final Pattern DIMENSION_KEY_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private final File worldContainer;
    private final Path levelDirectory;

    private WorldStorageLayout(File worldContainer, Path levelDirectory) {
        this.worldContainer = Objects.requireNonNull(worldContainer, "worldContainer")
                .getAbsoluteFile();
        this.levelDirectory = levelDirectory == null ? null : levelDirectory.toAbsolutePath().normalize();
    }

    public static WorldStorageLayout detect() {
        return detect(Bukkit.getServer());
    }

    static WorldStorageLayout detect(Server server) {
        Objects.requireNonNull(server, "server");
        Path levelDirectory = null;
        try {
            Method method = server.getClass().getMethod("getLevelDirectory");
            Object value = method.invoke(server);
            if (value instanceof Path path) levelDirectory = path;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Paper before 26.1 uses the legacy world container only.
        }
        return new WorldStorageLayout(server.getWorldContainer(), levelDirectory);
    }

    static WorldStorageLayout forTests(Path worldContainer, Path levelDirectory) {
        return new WorldStorageLayout(worldContainer.toFile(), levelDirectory);
    }

    public boolean usesDimensionStorage() {
        return levelDirectory != null;
    }

    /** The authoritative BedWars map directory, always the legacy path. */
    public File legacyWorldFolder(String worldName) {
        return new File(worldContainer, requireSafeName(worldName));
    }

    /** The directory Paper loads while a 26+ world is active. */
    public File runtimeWorldFolder(String worldName) {
        String safeName = requireSafeName(worldName);
        if (!usesDimensionStorage()) return new File(worldContainer, safeName);
        return levelDirectory.resolve("dimensions/minecraft")
                .resolve(toDimensionKey(safeName)).toFile();
    }

    public File runtimeWorldsDirectory() {
        if (!usesDimensionStorage()) return worldContainer;
        return levelDirectory.resolve("dimensions/minecraft").toFile();
    }

    public File levelDirectory() {
        return usesDimensionStorage() ? levelDirectory.toFile() : worldContainer;
    }

    public boolean supportsWorldName(String worldName) {
        if (!WorldNameValidator.isSafe(worldName)) return false;
        return !usesDimensionStorage() || DIMENSION_KEY_PATTERN.matcher(worldName).matches();
    }

    public WorldCreator createWorldCreator(String worldName) {
        String safeName = requireSafeName(worldName);
        if (!usesDimensionStorage()) return new WorldCreator(safeName);
        String keyName = toDimensionKey(safeName);
        try {
            Method method = WorldCreator.class.getMethod("ofKey", NamespacedKey.class);
            return (WorldCreator) method.invoke(null, NamespacedKey.minecraft(keyName));
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw creatorFailure(safeName, exception);
        }
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

    private static String toDimensionKey(String worldName) {
        String key = worldName.toLowerCase(Locale.ENGLISH).replace(' ', '_');
        if (!DIMENSION_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Paper 26+ requires an ASCII world name: " + worldName);
        }
        return key;
    }

    private static IllegalStateException creatorFailure(String worldName, Exception exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation
                ? invocation.getCause() : exception;
        return new IllegalStateException("Unable to create a Paper 26+ world creator for " + worldName, cause);
    }
}
