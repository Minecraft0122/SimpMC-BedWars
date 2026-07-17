/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.api.configuration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigManager {

    public static final String CONFIG_VERSION_PATH = "config-version";

    private YamlConfiguration yml;
    private File config;
    private String name;
    private final Plugin plugin;
    private boolean firstTime = false;

    /**
     * Create a new configuration file.
     *
     * @param plugin config owner.
     * @param name   config name. Do not include .yml in it.
     */
    public ConfigManager(Plugin plugin, String name, String dir) {
        this.plugin = plugin;
        File d = new File(dir);

        if (!d.exists()) {
            if (!d.mkdirs()) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + d.getPath());
                return;
            }
        }

        config = new File(dir, name + ".yml");
        if (!config.exists()) {
            firstTime = true;
            plugin.getLogger().log(Level.INFO, "Creating " + config.getPath());
            try {
                if (!config.createNewFile()) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create " + config.getPath());
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        yml = YamlConfiguration.loadConfiguration(config);
        yml.options().copyDefaults(true);
        this.name = name;
    }

    /**
     * Reload configuration.
     */
    public void reload() {
        yml = YamlConfiguration.loadConfiguration(config);
    }

    /**
     * Convert a location to an arena location syntax
     */
    public String stringLocationArenaFormat(Location loc) {
        return serializeArenaLocation(loc);
    }

    /**
     * Normalize an arena marker to the center of its block. Arena markers do
     * not have a meaningful facing direction, so yaw and pitch are reset.
     */
    public static Location toArenaBlockCenter(Location loc) {
        if (loc == null) {
            throw new IllegalArgumentException("Arena location cannot be null");
        }
        return new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY(), loc.getBlockZ() + 0.5, 0.0F, 0.0F);
    }

    /**
     * Check a radius without asking Bukkit to compare locations from different worlds.
     */
    public static boolean isSameWorldWithin(Location first, Location second, double radius) {
        if (first == null || second == null || radius < 0 || first.getWorld() == null
                || !first.getWorld().equals(second.getWorld())) {
            return false;
        }
        return first.distanceSquared(second) < radius * radius;
    }

    public static boolean areBothLocationsNearBed(Location spawn, Location respawn, Location bed) {
        return isSameWorldWithin(spawn, bed, 4) && isSameWorldWithin(respawn, bed, 4);
    }

    /**
     * Serialize a centered arena marker without redundant yaw and pitch data.
     */
    public static String serializeArenaLocation(Location loc) {
        Location centered = toArenaBlockCenter(loc);
        return centered.getX() + "," + centered.getY() + "," + centered.getZ();
    }

    /**
     * Upgrade an arena location stored by an older plugin version. This also
     * removes the obsolete yaw and pitch components.
     */
    public static String normalizeArenaLocationString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Arena location cannot be null");
        }
        String[] data = value.replace("[", "").replace("]", "").split(",");
        if (data.length < 3) {
            throw new IllegalArgumentException("Invalid arena location: expected at least x,y,z");
        }
        double x = Math.floor(Double.parseDouble(data[0].trim())) + 0.5;
        double y = Math.floor(Double.parseDouble(data[1].trim()));
        double z = Math.floor(Double.parseDouble(data[2].trim())) + 0.5;
        return x + "," + y + "," + z;
    }

    /**
     * Apply a migration only when the stored configuration schema is older.
     * The operation is idempotent and never downgrades a newer configuration.
     */
    public static boolean applyVersionedMigration(YamlConfiguration configuration, int latestVersion,
                                                   Consumer<YamlConfiguration> migration) {
        if (latestVersion < 1) {
            throw new IllegalArgumentException("Latest configuration version must be positive");
        }
        int currentVersion = configuration.getInt(CONFIG_VERSION_PATH, 0);
        if (currentVersion >= latestVersion) {
            return false;
        }
        migration.accept(configuration);
        configuration.options().copyDefaults(true);
        configuration.set(CONFIG_VERSION_PATH, latestVersion);
        configuration.setComments(CONFIG_VERSION_PATH, List.of("配置文件架构版本，请勿手动修改。"));
        return true;
    }

    /**
     * Back up and upgrade this file to the latest schema, then persist all new
     * defaults and removals in one disk write.
     */
    public boolean updateToLatestVersion(int latestVersion, Consumer<YamlConfiguration> migration) {
        int currentVersion = yml.getInt(CONFIG_VERSION_PATH, 0);
        if (currentVersion > latestVersion) {
            plugin.getLogger().warning("Skipping downgrade of " + config.getName() + " from configuration version "
                    + currentVersion + " to " + latestVersion + '.');
            return false;
        }
        if (currentVersion == latestVersion) {
            return false;
        }

        backupBeforeMigration(currentVersion);
        if (!applyVersionedMigration(yml, latestVersion, migration)) {
            return false;
        }
        save();
        plugin.getLogger().info("Updated " + config.getName() + " configuration from version "
                + currentVersion + " to " + latestVersion + '.');
        return true;
    }

    public boolean updateToLatestVersion(int latestVersion) {
        return updateToLatestVersion(latestVersion, ignored -> { });
    }

    private void backupBeforeMigration(int currentVersion) {
        if (firstTime || !config.isFile() || config.length() == 0) {
            return;
        }
        File backup = new File(config.getParentFile(), config.getName() + ".v" + currentVersion + ".bak");
        if (backup.exists()) {
            return;
        }
        try {
            Files.copy(config.toPath(), backup.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not back up " + config.getPath() + " before migration", e);
        }
    }

    /**
     * Convert a location to a string for general use
     * Use {@link #stringLocationArenaFormat(Location)} for arena locations
     */
    public String stringLocationConfigFormat(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + (double) loc.getYaw() + "," + (double) loc.getPitch() + "," + loc.getWorld().getName();
    }

    /**
     * Upgrade a general location to x,y,z,yaw,pitch,world. Older releases
     * sometimes stored lobby locations without pitch or without a world.
     */
    public static String normalizeConfigLocationString(String value, String fallbackWorld) {
        if (value == null) {
            throw new IllegalArgumentException("Configuration location cannot be null");
        }
        String[] raw = value.replace("[", "").replace("]", "").split(",");
        if (raw.length < 3) {
            throw new IllegalArgumentException("Invalid configuration location: expected at least x,y,z");
        }

        String x = raw[0].trim();
        String y = raw[1].trim();
        String z = raw[2].trim();
        Double.parseDouble(x);
        Double.parseDouble(y);
        Double.parseDouble(z);

        String yaw = "0.0";
        String pitch = "0.0";
        String world = null;
        if (raw.length >= 4) {
            if (isNumber(raw[3])) {
                yaw = raw[3].trim();
            } else {
                world = raw[3].trim();
            }
        }
        if (raw.length >= 5) {
            if (isNumber(raw[4])) {
                pitch = raw[4].trim();
            } else {
                world = raw[4].trim();
            }
        }
        if (raw.length >= 6 && !raw[5].isBlank()) {
            world = raw[5].trim();
        }
        if (world == null || world.isBlank()) {
            world = fallbackWorld;
        }
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("Invalid configuration location: world is missing");
        }
        return x + ',' + y + ',' + z + ',' + yaw + ',' + pitch + ',' + world;
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Save a general location to the config.
     * Use {@link #saveArenaLoc(String, Location)} for arena locations
     */
    public void saveConfigLoc(String path, Location loc) {
        String data = loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + (double) loc.getYaw() + "," + (double) loc.getPitch() + "," + loc.getWorld().getName();
        yml.set(path, data);
        save();
    }

    /**
     * Save a location for arena use
     */
    public void saveArenaLoc(String path, Location loc) {
        yml.set(path, serializeArenaLocation(loc));
        save();
    }

    /**
     * Get a general location
     * Use {@link #getArenaLoc(String)} for locations stored using {@link #saveArenaLoc(String, Location)}
     */
    public Location getConfigLoc(String path) {
        String d = yml.getString(path);
        if (d == null) return null;
        String fallbackWorld = null;
        if (Bukkit.getWorld(name) != null) {
            fallbackWorld = name;
        } else if (!Bukkit.getWorlds().isEmpty()) {
            fallbackWorld = Bukkit.getWorlds().getFirst().getName();
        }
        try {
            String[] data = normalizeConfigLocationString(d, fallbackWorld).split(",", 6);
            return new Location(Bukkit.getWorld(data[5]), Double.parseDouble(data[0]), Double.parseDouble(data[1]),
                    Double.parseDouble(data[2]), Float.parseFloat(data[3]), Float.parseFloat(data[4]));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring invalid location at " + path + " in " + config.getName()
                    + ": " + exception.getMessage());
            return null;
        }
    }

    /**
     * Get a location for arena use
     * Use {@link #getConfigLoc(String)} (String)} for locations stored using {@link #saveConfigLoc(String, Location)} (String, Location)}
     */
    public Location getArenaLoc(String path) {
        String d = yml.getString(path);
        if (d == null) return null;
        String[] data = d.replace("[", "").replace("]", "").split(",");
        return parseArenaLocation(data);
    }

    /**
     * Convert string to arena location syntax
     */
    public Location convertStringToArenaLocation(String string) {
        String[] data = string.split(",");
        return parseArenaLocation(data);

    }

    private Location parseArenaLocation(String[] data) {
        if (data.length < 3) {
            throw new IllegalArgumentException("Invalid arena location: expected at least x,y,z");
        }

        // Keep old five-component configurations readable while new markers
        // intentionally omit their meaningless direction.
        float yaw = data.length >= 5 ? Float.parseFloat(data[3]) : 0.0F;
        float pitch = data.length >= 5 ? Float.parseFloat(data[4]) : 0.0F;
        return new Location(
                Bukkit.getWorld(name),
                Double.parseDouble(data[0]),
                Double.parseDouble(data[1]),
                Double.parseDouble(data[2]),
                yaw,
                pitch
        );
    }

    /**
     * Get list of arena locations at given path
     */
    public List<Location> getArenaLocations(String path) {
        List<Location> l = new ArrayList<>();
        for (String s : yml.getStringList(path)) {
            Location loc = convertStringToArenaLocation(s);
            if (loc != null) {
                l.add(loc);
            }
        }
        return l;
    }

    /**
     * Set data to config
     */
    public void set(String path, Object value) {
        yml.set(path, value);
        save();
    }

    /**
     * Get yml instance
     */
    public YamlConfiguration getYml() {
        return yml;
    }

    /**
     * Add user-facing documentation above a configuration entry.
     */
    public void setComments(String path, String... comments) {
        yml.setComments(path, Arrays.asList(comments));
    }

    /**
     * Save config changes to file
     */
    public void save() {
        try {
            yml.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get list of strings at given path
     *
     * @return a list of string with colors translated
     */
    public List<String> getList(String path) {
        return yml.getStringList(path).stream().map(s -> s.replace("&", "§")).collect(Collectors.toList());
    }

    /**
     * Get boolean at given path
     */
    public boolean getBoolean(String path) {
        return yml.getBoolean(path);
    }

    /**
     * Get Integer at given path
     */
    public int getInt(String path) {
        return yml.getInt(path);
    }

    public double getDouble(String path) {
        return yml.getDouble(path);
    }


    /**
     * Get string at given path
     */
    public String getString(String path) {
        return yml.getString(path);
    }

    /**
     * Check if the config file was created for the first time
     * Can be used to add default values
     */
    public boolean isFirstTime() {
        return firstTime;
    }

    /**
     * Compare two arena locations
     * Return true if same location
     */
    public boolean compareArenaLoc(Location l1, Location l2) {
        return l1.getBlockX() == l2.getBlockX() && l1.getBlockZ() == l2.getBlockZ() && l1.getBlockY() == l2.getBlockY();
    }

    /**
     * Get config name
     */
    public String getName() {
        return name;
    }

    /**
     * Change internal name.
     */
    public void setName(String name) {
        this.name = name;
    }
}
