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

package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.configuration.GameMainOverridable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ArenaConfig extends ConfigManager {

    private static final int CONFIG_VERSION = 1;

    @SuppressWarnings({"SpellCheckingInspection"})
    private List<String> cachedGameOverridables = new ArrayList<>();

    public ArenaConfig(Plugin plugin, String name, String dir) {
        super(plugin, name, dir);

        YamlConfiguration yml = getYml();
        yml.options().header(plugin.getName() + " arena configuration file for Paper 1.21.11 servers.");
        yml.addDefault("group", "Default");
        yml.addDefault(ConfigPath.ARENA_DISPLAY_NAME, "");
        yml.addDefault("minPlayers", 2);
        yml.addDefault("maxInTeam", 1);
        yml.addDefault("allowSpectate", true);
        yml.addDefault(ConfigPath.ARENA_SPAWN_PROTECTION, 5);
        yml.addDefault(ConfigPath.ARENA_SHOP_PROTECTION, 1);
        yml.addDefault(ConfigPath.ARENA_UPGRADES_PROTECTION, 1);
        yml.addDefault(ConfigPath.ARENA_GENERATOR_PROTECTION, 1);
        yml.addDefault(ConfigPath.ARENA_ISLAND_RADIUS, 17);
        yml.addDefault("worldBorder", 300);
        yml.addDefault(ConfigPath.ARENA_Y_LEVEL_KILL, -1);
        //yml.addDefault("disableGeneratorsOnOrphanIslands", false);
        yml.addDefault(ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y, 180);
        yml.addDefault(ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS, false);
        yml.addDefault(ConfigPath.ARENA_DISABLE_NPCS_FOR_EMPTY_TEAMS, true);
        yml.addDefault(ConfigPath.ARENA_NORMAL_DEATH_DROPS, false);
        yml.addDefault(ConfigPath.ARENA_USE_BED_HOLO, true);
        yml.addDefault(ConfigPath.ARENA_ALLOW_MAP_BREAK, false);
        ArrayList<String> rules = new ArrayList<>();
        rules.add("doDaylightCycle:false");
        rules.add("announceAdvancements:false");
        rules.add("doInsomnia:false");
        rules.add("doImmediateRespawn:true");
        rules.add("doWeatherCycle:false");
        rules.add("doFireTick:false");
        rules.add("doMobSpawning:false");
        yml.addDefault(ConfigPath.ARENA_GAME_RULES, rules);
        yml.options().copyDefaults(true);
        updateToLatestVersion(CONFIG_VERSION, config -> migrateLegacyConfig(plugin, config));

        cachedGameOverridables = getGameOverridables();
    }

    private static void migrateLegacyConfig(Plugin plugin, YamlConfiguration config) {
        moveIfAbsent(config, "spawnProtection", ConfigPath.ARENA_SPAWN_PROTECTION);
        moveIfAbsent(config, "shopProtection", ConfigPath.ARENA_SHOP_PROTECTION);
        moveIfAbsent(config, "upgradesProtection", ConfigPath.ARENA_UPGRADES_PROTECTION);
        moveIfAbsent(config, "islandRadius", ConfigPath.ARENA_ISLAND_RADIUS);
        config.set("voidKill", null);
        config.set(ConfigPath.GENERAL_CONFIGURATION_ENABLE_GEN_SPLIT, null);

        List<String> gameRules = new ArrayList<>(config.getStringList(ConfigPath.ARENA_GAME_RULES));
        addRuleIfMissing(gameRules, "doDaylightCycle", false);
        addRuleIfMissing(gameRules, "doMobSpawning", false);
        config.set(ConfigPath.ARENA_GAME_RULES, gameRules);

        for (String path : List.of("waiting.Loc", ConfigPath.ARENA_WAITING_POS1,
                ConfigPath.ARENA_WAITING_POS2, ConfigPath.ARENA_SPEC_LOC)) {
            normalizeLocation(plugin, config, path);
        }
        normalizeLocationList(plugin, config, "generator.Diamond");
        normalizeLocationList(plugin, config, "generator.Emerald");

        ConfigurationSection teams = config.getConfigurationSection("Team");
        if (teams == null) {
            return;
        }
        for (String team : teams.getKeys(false)) {
            String root = "Team." + team + '.';
            for (String path : List.of("Spawn", "Bed", "Shop", "Upgrade", ConfigPath.ARENA_TEAM_KILL_DROPS_LOC)) {
                normalizeLocation(plugin, config, root + path);
            }
            for (String generator : List.of("Iron", "Gold", "Emerald")) {
                normalizeLocationList(plugin, config, root + generator);
            }
        }
    }

    private static void moveIfAbsent(YamlConfiguration config, String oldPath, String newPath) {
        if (config.isSet(oldPath) && !config.isSet(newPath)) {
            config.set(newPath, config.get(oldPath));
        }
        config.set(oldPath, null);
    }

    private static void addRuleIfMissing(List<String> rules, String ruleName, boolean value) {
        if (rules.stream().noneMatch(rule -> rule.regionMatches(true, 0, ruleName + ':', 0, ruleName.length() + 1))) {
            rules.add(ruleName + ':' + value);
        }
    }

    private static void normalizeLocation(Plugin plugin, YamlConfiguration config, String path) {
        if (!config.isString(path)) {
            return;
        }
        try {
            config.set(path, ConfigManager.normalizeArenaLocationString(config.getString(path)));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not migrate invalid arena location at " + path + ": " + exception.getMessage());
        }
    }

    private static void normalizeLocationList(Plugin plugin, YamlConfiguration config, String path) {
        if (!config.isList(path)) {
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String value : config.getStringList(path)) {
            try {
                normalized.add(ConfigManager.normalizeArenaLocationString(value));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Could not migrate invalid arena location at " + path + ": " + exception.getMessage());
                normalized.add(value);
            }
        }
        config.set(path, normalized);
    }

    @SuppressWarnings({"SpellCheckingInspection"})
    private @NotNull List<String> getGameOverridables() {
        List<String> paths = new ArrayList<>();
        for (Field field : ConfigPath.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(GameMainOverridable.class)) {
                try {
                    Object value = field.get(field);
                    if (value instanceof String) {
                        paths.add((String) value);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }

        return paths;
    }

    public boolean isGameOverridable(String path) {
        return cachedGameOverridables.contains(path);
    }

    public Object getGameOverridableValue(String path) {
        if (!isGameOverridable(path)) {
            throw new RuntimeException("Given path is not game-overridable: "+path);
        }

        Object value = getYml().get(path, null);
        if (null == value){
            return BedWars.config.getYml().get(path);
        }
        return value;
    }

    public Boolean getGameOverridableBoolean(String path) {
        Object value = getGameOverridableValue(path);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public String getGameOverridableString(String path) {
        Object value = getGameOverridableValue(path);
        return value instanceof String ? (String) value : "invalid";
    }
}
