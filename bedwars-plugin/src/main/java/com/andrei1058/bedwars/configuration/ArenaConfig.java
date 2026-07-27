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
import com.andrei1058.bedwars.arena.ArenaGroupMembership;
import com.andrei1058.bedwars.arena.NpcFacing;
import com.andrei1058.bedwars.arena.PlayerFacing;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ArenaConfig extends ConfigManager {

    private static final int CONFIG_VERSION = 15;

    @SuppressWarnings({"SpellCheckingInspection"})
    private List<String> cachedGameOverridables = new ArrayList<>();

    public ArenaConfig(Plugin plugin, String name, String dir) {
        super(plugin, name, dir);

        YamlConfiguration yml = getYml();
        yml.options().header(plugin.getName() + " 竞技场配置，适用于 Paper 1.21.11 服务器。");
        yml.addDefault(ArenaGroupMembership.GROUPS_PATH, List.of(ArenaGroupMembership.DEFAULT_GROUP));
        yml.addDefault(ConfigPath.ARENA_DISPLAY_NAME, "");
        yml.addDefault("maxInTeam", 1);
        yml.addDefault("minInTeam", 1);
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
        rules.add("fireSpreadRadiusAroundPlayer:0");
        rules.add("doMobSpawning:false");
        rules.add("locatorBar:false");
        yml.addDefault(ConfigPath.ARENA_GAME_RULES, rules);
        yml.options().copyDefaults(true);
        setComments(ArenaGroupMembership.GROUPS_PATH,
                "竞技场所属的全部匹配分组；同一竞技场可以同时出现在多个组中。",
                "列表第一项是主组，生成器、开局物品、升级菜单和计分板等组专属配置读取主组。");
        setComments(ConfigPath.ARENA_DISPLAY_NAME, "玩家看到的竞技场名称；留空时使用世界名。");
        setComments("maxInTeam", "每支队伍的最大玩家数；高级和引导式设置均可使用 /bw setMaxInTeam 修改。", "创建队伍不会自动覆盖此值；setType 会写入所选类型的标准容量。");
        setComments("minInTeam", "正常开局时每支实际参赛队伍的最少玩家数。", "范围为 1 到 maxInTeam，命令补全会按当前最大人数生成；/bw start debug 可临时绕过此限制。");
        setComments(ConfigPath.ARENA_ISLAND_RADIUS, "队伍岛屿检测半径，用于治疗池和床位自动识别。");
        setComments("worldBorder", "世界边界半径，单位为方块。");
        setComments(ConfigPath.ARENA_Y_LEVEL_KILL, "玩家低于该 Y 坐标时判定掉入虚空。");
        setComments(ConfigPath.ARENA_GAME_RULES, "载入竞技场时应用的游戏规则，格式为 规则:值。", "竞技场时间固定为 1000 tick，并强制禁止昼夜变化、天气变化、火势蔓延、生物自然生成和 Locator Bar。", "Paper 1.21.11 使用 fireSpreadRadiusAroundPlayer:0；旧 doFireTick 项会自动删除。");
        ChineseConfigDocumentation.arena(this);
        updateToLatestVersion(CONFIG_VERSION, config -> migrateLegacyConfig(plugin, config));

        cachedGameOverridables = getGameOverridables();
    }

    @Override
    public void save() {
        ChineseConfigDocumentation.arena(this);
        super.save();
    }

    private static void migrateLegacyConfig(Plugin plugin, YamlConfiguration config) {
        moveIfAbsent(config, "spawnProtection", ConfigPath.ARENA_SPAWN_PROTECTION);
        moveIfAbsent(config, "shopProtection", ConfigPath.ARENA_SHOP_PROTECTION);
        moveIfAbsent(config, "upgradesProtection", ConfigPath.ARENA_UPGRADES_PROTECTION);
        moveIfAbsent(config, "islandRadius", ConfigPath.ARENA_ISLAND_RADIUS);
        config.set("voidKill", null);
        config.set(ConfigPath.GENERAL_CONFIGURATION_ENABLE_GEN_SPLIT, null);
        config.set("minPlayers", null);
        normalizeTeamLimits(config);
        migrateArenaGroups(config);

        List<String> gameRules = new ArrayList<>(config.getStringList(ConfigPath.ARENA_GAME_RULES));
        forceBooleanRule(gameRules, "doDaylightCycle", false);
        addRuleIfMissing(gameRules, "doMobSpawning", false);
        forceNoFireSpread(gameRules);
        forceBooleanRule(gameRules, "locatorBar", false);
        config.set(ConfigPath.ARENA_GAME_RULES, gameRules);

        migratePlayerFacing(plugin, config, "waiting.Loc", ConfigPath.ARENA_WAITING_FACING);
        migratePlayerFacing(plugin, config, ConfigPath.ARENA_SPEC_LOC, ConfigPath.ARENA_SPEC_FACING);
        config.setComments(ConfigPath.ARENA_WAITING_FACING,
                List.of("玩家进入竞技场等待区时的朝向；yaw 取最近的 90 度倍数，pitch 固定为 0。"));
        config.setComments(ConfigPath.ARENA_SPEC_FACING,
                List.of("玩家进入观战点时的朝向；yaw 取最近的 90 度倍数，pitch 固定为 0。"));
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
        migrateLegacyTeamColors(config, teams);
        for (String team : teams.getKeys(false)) {
            String root = "Team." + team + '.';
            config.set(root + "Respawn", null);
            migratePlayerFacing(plugin, config, root + "Spawn", root + ConfigPath.ARENA_TEAM_SPAWN_FACING);
            migrateNpcFacing(plugin, config, root + "Shop", root + ConfigPath.ARENA_TEAM_SHOP_FACING,
                    root + "Spawn");
            migrateNpcFacing(plugin, config, root + "Upgrade", root + ConfigPath.ARENA_TEAM_UPGRADE_FACING,
                    root + "Spawn");
            for (String path : List.of("Spawn", "Bed", "Shop", "Upgrade", ConfigPath.ARENA_TEAM_KILL_DROPS_LOC)) {
                normalizeLocation(plugin, config, root + path);
            }
            config.setComments(root + ConfigPath.ARENA_TEAM_SHOP_FACING,
                    List.of("商店村民的水平朝向；yaw 取最近的 90 度倍数，pitch 固定为 0。"));
            config.setComments(root + ConfigPath.ARENA_TEAM_UPGRADE_FACING,
                    List.of("升级村民的水平朝向；yaw 取最近的 90 度倍数，pitch 固定为 0。"));
            config.setComments(root + ConfigPath.ARENA_TEAM_SPAWN_FACING,
                    List.of("玩家出生和复活时的朝向；yaw 取最近的 90 度倍数，pitch 固定为 0。"));
            for (String generator : List.of("Iron", "Gold", "Emerald")) {
                normalizeLocationList(plugin, config, root + generator);
            }
        }
    }

    /**
     * AQUA was historically backed by LIGHT_BLUE blocks even though its
     * legacy data value represented cyan. Keep old arenas loadable while
     * making CYAN the single name written by current versions.
     */
    static void migrateLegacyTeamColors(YamlConfiguration config, ConfigurationSection teams) {
        for (String team : teams.getKeys(false)) {
            String path = "Team." + team + ".Color";
            if ("AQUA".equalsIgnoreCase(config.getString(path))) {
                config.set(path, "CYAN");
            }
        }
    }

    static void normalizeTeamLimits(YamlConfiguration config) {
        int maximum = Math.max(1, config.getInt("maxInTeam", 1));
        int minimum = Math.max(1, Math.min(config.getInt("minInTeam", 1), maximum));
        config.set("maxInTeam", maximum);
        config.set("minInTeam", minimum);
    }

    static void migrateArenaGroups(YamlConfiguration config) {
        config.set(ArenaGroupMembership.GROUPS_PATH, ArenaGroupMembership.read(config));
        config.set(ArenaGroupMembership.LEGACY_GROUP_PATH, null);
    }

    private static void migratePlayerFacing(Plugin plugin, YamlConfiguration config, String locationPath,
                                            String facingPath) {
        if (!config.isSet(facingPath) && config.isString(locationPath)) {
            try {
                String[] location = locationParts(config.getString(locationPath));
                float yaw = location.length >= 5 ? Float.parseFloat(location[3].trim()) : 0.0F;
                float pitch = location.length >= 5 ? Float.parseFloat(location[4].trim()) : 0.0F;
                config.set(facingPath, PlayerFacing.serialize(yaw, pitch));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Could not migrate player facing at " + locationPath + ": "
                        + exception.getMessage());
            }
        }
        if (config.isString(facingPath)) {
            try {
                String[] facing = config.getString(facingPath).replace("[", "").replace("]", "").split(",");
                config.set(facingPath, PlayerFacing.serialize(Float.parseFloat(facing[0].trim()), 0.0F));
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
                plugin.getLogger().warning("Could not normalize player facing at " + facingPath + ": "
                        + exception.getMessage());
            }
        }
    }

    public void savePlayerArenaLocation(String locationPath, String facingPath, Location location) {
        getYml().set(locationPath, stringLocationArenaFormat(location));
        getYml().set(facingPath, PlayerFacing.serialize(location));
        getYml().setComments(facingPath,
                List.of("玩家传送到此位置时使用的朝向；yaw 取最近的 90 度倍数，pitch 固定为 0。"));
        save();
    }

    private static void migrateNpcFacing(Plugin plugin, YamlConfiguration config, String locationPath,
                                         String facingPath, String fallbackTargetPath) {
        if (config.isSet(facingPath)) {
            try {
                config.set(facingPath, NpcFacing.normalize(Float.parseFloat(config.get(facingPath).toString())));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Could not normalize NPC facing at " + facingPath + ": "
                        + exception.getMessage());
            }
            return;
        }
        if (!config.isString(locationPath)) return;

        try {
            String[] npc = locationParts(config.getString(locationPath));
            if (npc.length >= 4) {
                config.set(facingPath, NpcFacing.normalize(Float.parseFloat(npc[3].trim())));
                return;
            }
            if (!config.isString(fallbackTargetPath)) return;
            String[] target = locationParts(config.getString(fallbackTargetPath));
            config.set(facingPath, NpcFacing.toward(
                    Double.parseDouble(npc[0].trim()), Double.parseDouble(npc[2].trim()),
                    Double.parseDouble(target[0].trim()), Double.parseDouble(target[2].trim())
            ));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not migrate NPC facing at " + locationPath + ": "
                    + exception.getMessage());
        }
    }

    private static String[] locationParts(String value) {
        if (value == null) throw new IllegalArgumentException("location is missing");
        String[] parts = value.replace("[", "").replace("]", "").split(",");
        if (parts.length < 3) throw new IllegalArgumentException("expected at least x,y,z");
        return parts;
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

    static void forceBooleanRule(List<String> rules, String ruleName, boolean value) {
        String canonicalName = canonicalRuleName(ruleName);
        rules.removeIf(rule -> {
            int separator = rule.indexOf(':');
            String name = separator < 0 ? rule : rule.substring(0, separator);
            return canonicalRuleName(name).equals(canonicalName);
        });
        rules.add(ruleName + ':' + value);
    }

    static void forceNoFireSpread(List<String> rules) {
        rules.removeIf(rule -> {
            int separator = rule.indexOf(':');
            String name = separator < 0 ? rule : rule.substring(0, separator);
            String canonicalName = canonicalRuleName(name);
            return canonicalName.equals("dofiretick")
                    || canonicalName.equals("firespreadradiusaroundplayer");
        });
        rules.add("fireSpreadRadiusAroundPlayer:0");
    }

    private static String canonicalRuleName(String name) {
        return name == null ? "" : name.replace("_", "")
                .replace("-", "").replace(" ", "").toLowerCase(java.util.Locale.ROOT);
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
