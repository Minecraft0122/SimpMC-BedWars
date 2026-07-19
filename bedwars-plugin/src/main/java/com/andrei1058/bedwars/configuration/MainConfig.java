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
import com.andrei1058.bedwars.api.arena.stats.DefaultStatistics;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Misc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;


public class MainConfig extends ConfigManager {

    private static final int CONFIG_VERSION = 12;

    public MainConfig(Plugin plugin, String name) {
        super(plugin, name, BedWars.plugin.getDataFolder().getPath());

        YamlConfiguration yml = getYml();

        yml.options().header(plugin.getDescription().getName() + "，由 SimpMC 维护。\n");
        yml.addDefault("serverType", "MULTIARENA");
        yml.addDefault("language", "en");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, Collections.singletonList("your language iso here"));
        yml.addDefault("storeLink", "https://www.spigotmc.org/resources/authors/39904/");
        yml.addDefault("lobbyServer", "hub");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ENABLE_HALLOWEEN, true);
        yml.addDefault(ConfigPath.GENERAL_CHAT_GLOBAL, yml.get("globalChat", false));
        yml.addDefault(ConfigPath.GENERAL_CHAT_FORMATTING, yml.get("formatChat", true));
        yml.addDefault("debug", false);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_MARK_LEAVE_AS_ABANDON, false);
        // parties category
        yml.addDefault(ConfigPath.GENERAL_ENABLE_PARTY_CMD, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ALLOW_PARTIES, true);
        yml.addDefault(ConfigPath.GENERAL_ALESSIODP_PARTIES_RANK, 10);
        //
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_USE_GAME_SIDEBAR, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_TITLE_REFRESH_INTERVAL, 4);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_PLACEHOLDERS_REFRESH_INTERVAL, 20);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_PLAYING, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_RESTARTING, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_REFRESH, 1200);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_ENABLE, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB, false);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_REFRESH, 300);
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_ENABLE, true);
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_REFRESH_INTERVAL, 20);
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_HEADER, List.of("", "&b&lSimpMC MiniGame - BedWars", ""));
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_FOOTER, List.of("", "&f服务器地址：&a{serverIp}", ""));

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 30);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_RE_SPAWN_INVULNERABILITY, 4000);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_MODE_GAMES_BEFORE_RESTART, 30);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_RESTART_CMD, "restart");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_AUTO_SCALE_LIMIT, 5);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_LOBBY_SERVERS, Collections.singletonList("0.0.0.0:2019"));
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_REGULAR, 40);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_HALF, 25);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_SHORTENED, 5);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_RESTART, 60);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_RE_SPAWN_COUNTDOWN, 5);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BEDS_DESTROY_COUNTDOWN, 360);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DRAGON_SPAWN_COUNTDOWN, 600);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_GAME_END_COUNTDOWN, 120);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN, 30);
        yml.addDefault(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP, "simpmc.org");
        yml.addDefault(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_POWERED_BY, "SimpMC-BedWars");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, "bw1");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_BWP_TIME_OUT, 5000);

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_HUNGER_WAITING, false);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_HUNGER_INGAME, false);

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ALLOW_FIRE_EXTINGUISH, true);

        //heal pool category
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_SEEN_TEAM_ONLY, true);

        // tnt jump category
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_BARYCENTER_IN_Y, 0.5);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_STRENGTH_REDUCTION, 5);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_Y_REDUCTION, 2);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_SELF, 1);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES, 5);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS, 10);

        // tnt block blast resistance
        yml.addDefault(ConfigPath.GENERAL_TNT_PROTECTION_END_STONE_BLAST, 12f);
        yml.addDefault(ConfigPath.GENERAL_TNT_PROTECTION_GLASS_BLAST, 300f);
        yml.addDefault(ConfigPath.GENERAL_TNT_RAY_BLOCKED_BY_GLASS, true);

        // tnt prime settings
        yml.addDefault(ConfigPath.GENERAL_TNT_AUTO_IGNITE, true);
        yml.addDefault(ConfigPath.GENERAL_TNT_FUSE_TICKS, 45);

        // fireball category
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.5);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 10);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE, false);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.25);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.8);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_COOLDOWN, 0.5);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF, 2.0);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 4.0);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES, 0.0);
        //
        yml.addDefault("database.enable", false);
        yml.addDefault("database.host", "localhost");
        yml.addDefault("database.port", 3306);
        yml.addDefault("database.database", "simpmc_bedwars");
        yml.addDefault("database.user", "root");
        yml.addDefault("database.pass", "cheese");
        yml.addDefault("database.ssl", false);

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_SPOIL_TNT_PLAYERS, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_PAPER_FEATURES, true);

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLE_CRAFTING, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLE_ENCHANTING, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLE_FURNACE, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLE_BREWING_STAND, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLE_ANVIL, true);

        /* Multi-Arena Lobby Command Items */
        saveLobbyCommandItem("stats", "bw stats", false, "PLAYER_HEAD", 3, 0);
        saveLobbyCommandItem("arena-selector", "bw gui", true, "CHEST", 5, 4);
        saveLobbyCommandItem("leave", "bw leave", false, "RED_BED", 0, 8);

        /* Pre Game Command Items */
        savePreGameCommandItem("stats", "bw stats", false, "PLAYER_HEAD", 3, 0);
        savePreGameCommandItem("leave", "bw leave", false, "RED_BED", 0, 8);

        /* Spectator Command Items */
        saveSpectatorCommandItem("teleporter", "bw teleporter", false, "PLAYER_HEAD", 3, 0);
        saveSpectatorCommandItem("leave", "bw leave", false, "RED_BED", 0, 8);

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE, 27);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SHOW_PLAYING, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS, "10,11,12,13,14,15,16");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", "waiting"), "LIME_CONCRETE");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", "waiting"), 5);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED.replace("%path%", "waiting"), false);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", "starting"), "YELLOW_CONCRETE");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", "starting"), 4);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED.replace("%path%", "starting"), true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", "playing"), "RED_CONCRETE");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", "playing"), 14);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED.replace("%path%", "playing"), false);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", "skipped-slot"), "BLACK_STAINED_GLASS_PANE");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", "skipped-slot"), 15);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED.replace("%path%", "skipped-slot"), false);

        /* default stats GUI items */
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE, 27);
        if (isFirstTime()) {
            Misc.addDefaultStatsItem(yml, 10, Material.DIAMOND, 0, "wins");
            Misc.addDefaultStatsItem(yml, 11, Material.REDSTONE, 0, "losses");
            Misc.addDefaultStatsItem(yml, 12, Material.IRON_SWORD, 0, "kills");
            Misc.addDefaultStatsItem(yml, 13, Material.valueOf("SKELETON_SKULL"), 0, "deaths");
            Misc.addDefaultStatsItem(yml, 14, Material.DIAMOND_SWORD, 0, "final-kills");
            Misc.addDefaultStatsItem(yml, 15, Material.valueOf("SKELETON_SKULL"), 1, "final-deaths");
            Misc.addDefaultStatsItem(yml, 16, Material.valueOf("RED_BED"), 0, "beds-destroyed");
            Misc.addDefaultStatsItem(yml, 21, Material.valueOf("BLACK_STAINED_GLASS_PANE"), 0, "first-play");
            Misc.addDefaultStatsItem(yml, 22, Material.CHEST, 0, "games-played");
            Misc.addDefaultStatsItem(yml, 23, Material.valueOf("BLACK_STAINED_GLASS_PANE"), 0, "last-play");
        }

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DEFAULT_ITEMS + ".Default", Collections.singletonList("WOODEN_SWORD"));
        yml.addDefault(ConfigPath.CENERAL_CONFIGURATION_ALLOWED_COMMANDS, Arrays.asList("shout", "bw", "leave"));
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ENABLE_GEN_SPLIT, true);

        yml.addDefault(ConfigPath.LOBBY_VOID_TELEPORT_ENABLED, true);
        yml.addDefault(ConfigPath.LOBBY_VOID_TELEPORT_HEIGHT, 0);
        yml.addDefault(ConfigPath.GENERAL_GAME_END_SHOW_ELIMINATED, true);
        yml.addDefault(ConfigPath.GENERAL_GAME_END_TELEPORT_ELIMINATED, true);
        yml.addDefault(ConfigPath.GENERAL_GAME_END_CHAT_TOP_STATISTIC, DefaultStatistics.KILLS.toString());
        yml.addDefault(ConfigPath.GENERAL_GAME_END_CHAT_TOP_HIDE_MISSING, true);

        yml.addDefault(ConfigPath.GENERAL_GAME_END_SB_TOP_STATISTIC, DefaultStatistics.KILLS.toString());
        yml.addDefault(ConfigPath.GENERAL_GAME_END_SB_TOP_HIDE_MISSING, true);
        yml.options().copyDefaults(true);
        addConfigurationComments();
        ChineseConfigDocumentation.main(this);
        updateToLatestVersion(CONFIG_VERSION, MainConfig::migrateLegacyConfig);

        //set default server language
        String whatLang = "en";
        File[] langs = new File(plugin.getDataFolder(), "/Languages").listFiles();
        if (langs != null) {
            for (File f : langs) {
                if (!f.isFile()) continue;
                Optional<String> detectedIso = Language.isoFromFileName(f.getName());
                if (detectedIso.isEmpty()) continue;
                String lang = detectedIso.get();
                if (lang.equalsIgnoreCase(yml.getString("language"))) {
                    whatLang = lang;
                }
                if (Language.getLang(lang) == null) new Language(BedWars.plugin, lang);
            }
        }
        Language def = Language.getLang(whatLang);

        if (def == null) throw new IllegalStateException("Could not found default language: " + whatLang);
        Language.setDefaultLanguage(def);

        //remove languages if disabled
        //server language can t be disabled
        for (String iso : yml.getStringList(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES)) {
            Language l = Language.getLang(iso);
            if (l != null) {
                if (l != def) Language.getLanguages().remove(l);
            }
        }
        //

        BedWars.setDebug(yml.getBoolean("debug"));
        new ConfigManager(plugin, "bukkit", Bukkit.getWorldContainer().getPath()).set("ticks-per.autosave", -1);

        try {
            BedWars.setServerType(ServerType.valueOf(Objects.requireNonNull(yml.getString("serverType")).toUpperCase()));
        } catch (Exception e) {
            if (Objects.requireNonNull(yml.getString("serverType")).equalsIgnoreCase("BUNGEE_LEGACY")) {
                BedWars.setServerType(ServerType.BUNGEE);
                BedWars.setAutoscale(false);
            } else {
                set("serverType", "MULTIARENA");
            }
        }

        BedWars.setLobbyWorld(getLobbyWorldName());
    }

    private void addConfigurationComments() {
        setComments("serverType", "服务器运行模式：MULTIARENA、SHARED 或 BUNGEE。", "修改后需要完整重启服务器。");
        setComments("language", "服务器默认语言代码，例如 zh_cn。");
        setComments("storeLink", "商店或官方网站链接，可在消息占位符中使用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, "不允许玩家选择的语言代码列表。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR, "计分板与 TAB 列表相关设置。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY,
                "是否在大厅 TAB 中显示玩家前后缀；默认开启，且不依赖右侧大厅计分板。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB,
                "是否在 TAB 玩家列表中额外显示生命值数字。默认关闭，只保留原版网络延迟图标，避免被误认为两个 ping。");
        setComments(ConfigPath.SB_CONFIG_TAB_HEADER, "TAB 页首文本列表；支持 & 颜色代码及 {serverIp} 等占位符。", "默认显示 SimpMC MiniGame - BedWars。");
        setComments(ConfigPath.SB_CONFIG_TAB_FOOTER, "TAB 页尾文本列表；支持 & 颜色代码及 {serverIp} 等占位符。", "默认显示服务器地址 simpmc.org。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_RESTART,
                "游戏结束后竞技场重置倒计时，单位为秒；默认 60 秒。",
                "聊天栏只在 60、30、15、10、5、4、3、2、1、0 秒时广播，避免刷屏。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, "玩家掉线后的可重连时间，单位为秒。", "超过该时间未重连将直接视为离开；默认 30 秒。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE, "治疗池功能设置。");
        setComments(ConfigPath.GENERAL_TNT_JUMP_BARYCENTER_IN_Y, "TNT 跳跃、爆炸保护与伤害设置。");
        setComments(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE,
                "火球爆炸、击退、冷却与伤害设置。2.10.5 默认增强爆炸范围、水平/垂直击退和敌方伤害。");
        setComments("database.enable", "是否使用 MySQL；关闭时使用本地 SQLite。", "启用前请正确填写下面的连接信息。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN, "性能优化开关；通常建议保持启用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_DISABLE_CRAFTING, "竞技场内工作方块及合成功能限制。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH,
                "多竞技场大厅固定物品。stats、arena-selector 和 leave 会在旧配置迁移时自动补齐。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE, "竞技场选择菜单设置，大小必须是 9 的倍数。");
        setComments(ConfigPath.LOBBY_VOID_TELEPORT_ENABLED, "大厅掉入虚空时是否传送回大厅出生点。");
    }

    private static void migrateLegacyConfig(YamlConfiguration yml) {
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.0, 3.5);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.0, 1.25);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.65, 0.8);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 2.0, 4.0);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_CONFIGURATION_RESTART, 45.0, 60.0);
        if (yml.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 300) == 300) {
            yml.set(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 30);
        }
        migrateTabDisplayDefaults(yml);
        moveIfAbsent(yml, "formatChat", ConfigPath.GENERAL_CHAT_FORMATTING);
        moveIfAbsent(yml, "globalChat", ConfigPath.GENERAL_CHAT_GLOBAL);
        moveIfAbsent(yml, "bungee-settings.lobby-servers", ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_LOBBY_SERVERS);
        moveIfAbsent(yml, "arenaGui.settings.showPlaying", ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SHOW_PLAYING);
        moveIfAbsent(yml, "arenaGui.settings.size", ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE);
        moveIfAbsent(yml, "arenaGui.settings.useSlots", ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS);

        if (yml.isConfigurationSection("arenaGui")) {
            for (String path : Objects.requireNonNull(yml.getConfigurationSection("arenaGui")).getKeys(false)) {
                if (path.equalsIgnoreCase("settings")) continue;
                String newPath = "skippedSlot".equals(path) ? "skipped-slot" : path;
                moveIfAbsent(yml, "arenaGui." + path + ".itemStack",
                        ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_MATERIAL.replace("%path%", newPath));
                moveIfAbsent(yml, "arenaGui." + path + ".data",
                        ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_DATA.replace("%path%", newPath));
                moveIfAbsent(yml, "arenaGui." + path + ".enchanted",
                        ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_STATUS_ENCHANTED.replace("%path%", newPath));
            }
        }

        moveIfAbsent(yml, "npcLoc", ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE);
        moveIfAbsent(yml, "statsGUI.invSize", ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE);
        moveIfAbsent(yml, "disableCrafting", ConfigPath.GENERAL_CONFIGURATION_DISABLE_CRAFTING);
        migrateLegacyStatsItems(yml);
        moveIfAbsent(yml, "server-name", ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID);
        moveIfAbsent(yml, "lobby-scoreboard", ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR);
        moveIfAbsent(yml, "game-scoreboard", ConfigPath.SB_CONFIG_SIDEBAR_USE_GAME_SIDEBAR);
        moveIfAbsent(yml, "enable-party-cmd", ConfigPath.GENERAL_ENABLE_PARTY_CMD);
        moveIfAbsent(yml, "allow-parties", ConfigPath.GENERAL_CONFIGURATION_ALLOW_PARTIES);
        yml.set("use-experimental-team-assigner", null);

        ensureLobbyItem(yml, "stats", "bw stats", false, "PLAYER_HEAD", 3, 0);
        ensureLobbyItem(yml, "arena-selector", "bw gui", true, "CHEST", 5, 4);
        ensureLobbyItem(yml, "leave", "bw leave", false, "RED_BED", 0, 8);

        for (String obsoletePath : List.of("arenaGui", "statsGUI", "startItems", "generators",
                "bedsDestroyCountdown", "dragonSpawnCountdown", "gameEndCountdown", "npcLoc", "blockedCmds",
                "lobbyScoreboard", "items", "start-items-per-arena", "safeMode", "disableCrafting",
                "fireball.damage-multiplier", "performance-settings.disable-armor-packets",
                "performance-settings.disable-respawn-packets")) {
            yml.set(obsoletePath, null);
        }
        migrateLobbyLocation(yml);
    }

    static void migrateTabDisplayDefaults(YamlConfiguration yml) {
        // Version 10 shipped these opposite defaults. Change them once during
        // the schema-11 migration; administrators can still opt back in later.
        if (!yml.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY, false)) {
            yml.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY, true);
        }
        if (yml.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB, true)) {
            yml.set(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB, false);
        }
        if (!yml.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING, false)) {
            yml.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING, true);
        }
        if (!yml.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING, false)) {
            yml.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING, true);
        }
        yml.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_TEAMMATE_COLOR, null);
        String serverIp = yml.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP);
        if (serverIp == null || serverIp.isBlank() || "yourServer.Com".equalsIgnoreCase(serverIp)) {
            yml.set(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP, "simpmc.org");
        }
    }

    static void upgradeLegacyNumber(YamlConfiguration yml, String path, double oldValue, double newValue) {
        if (Double.compare(yml.getDouble(path), oldValue) == 0) {
            yml.set(path, newValue);
        }
    }

    private static void ensureLobbyItem(YamlConfiguration yml, String name, String command, boolean enchanted,
                                        String material, int data, int slot) {
        setIfMissing(yml, ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_COMMAND.replace("%path%", name), command);
        setIfMissing(yml, ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_MATERIAL.replace("%path%", name), material);
        setIfMissing(yml, ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_DATA.replace("%path%", name), data);
        setIfMissing(yml, ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_ENCHANTED.replace("%path%", name), enchanted);
        setIfMissing(yml, ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT.replace("%path%", name), slot);
    }

    private static void setIfMissing(YamlConfiguration yml, String path, Object value) {
        if (!yml.isSet(path)) yml.set(path, value);
    }

    private static void migrateLobbyLocation(YamlConfiguration yml) {
        if (!yml.isString("lobbyLoc")) {
            return;
        }
        String fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst().getName();
        try {
            yml.set("lobbyLoc", ConfigManager.normalizeConfigLocationString(yml.getString("lobbyLoc"), fallbackWorld));
        } catch (IllegalArgumentException exception) {
            BedWars.plugin.getLogger().warning("Could not migrate lobbyLoc: " + exception.getMessage());
        }
    }

    private static void migrateLegacyStatsItems(YamlConfiguration yml) {
        if (!yml.isConfigurationSection("statsGUI")) {
            return;
        }
        Map<String, String> names = Map.of(
                "gamesPlayed", "games-played",
                "lastPlay", "last-play",
                "firstPlay", "first-play",
                "bedsDestroyed", "beds-destroyed",
                "finalDeaths", "final-deaths",
                "finalKills", "final-kills"
        );
        for (String oldPath : Objects.requireNonNull(yml.getConfigurationSection("statsGUI")).getKeys(false)) {
            String newPath = names.getOrDefault(oldPath, oldPath);
            moveIfAbsent(yml, "statsGUI." + oldPath + ".itemStack",
                    ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_MATERIAL.replace("%path%", newPath));
            moveIfAbsent(yml, "statsGUI." + oldPath + ".data",
                    ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_DATA.replace("%path%", newPath));
            moveIfAbsent(yml, "statsGUI." + oldPath + ".slot",
                    ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_SLOT.replace("%path%", newPath));
        }
    }

    private static void moveIfAbsent(YamlConfiguration yml, String oldPath, String newPath) {
        if (yml.isSet(oldPath) && !yml.isSet(newPath)) {
            yml.set(newPath, yml.get(oldPath));
        }
        yml.set(oldPath, null);
    }

    public String getLobbyWorldName() {
        Location lobby = getConfigLoc("lobbyLoc");
        return lobby == null || lobby.getWorld() == null ? "" : lobby.getWorld().getName();
    }

    /**
     * Add Multi Arena Lobby Command Item To
     * This won't create the item back if you delete it.
     */
    @SuppressWarnings("WeakerAccess")
    public void saveLobbyCommandItem(String name, String cmd, boolean enchanted, String material, int data, int slot) {
        if (isFirstTime()) {
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_COMMAND.replace("%path%", name), cmd);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_MATERIAL.replace("%path%", name), material);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_DATA.replace("%path%", name), data);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_ENCHANTED.replace("%path%", name), enchanted);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT.replace("%path%", name), slot);
            getYml().options().copyDefaults(true);
            save();
        }
    }


    /**
     * Add Pre Game Command Item To
     * This won't create the item back if you delete it.
     */
    @SuppressWarnings("WeakerAccess")
    public void savePreGameCommandItem(String name, String cmd, boolean enchanted, String material, int data, int slot) {
        if (isFirstTime()) {
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_COMMAND.replace("%path%", name), cmd);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_MATERIAL.replace("%path%", name), material);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_DATA.replace("%path%", name), data);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_ENCHANTED.replace("%path%", name), enchanted);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_SLOT.replace("%path%", name), slot);
            getYml().options().copyDefaults(true);
            save();
        }
    }

    /**
     * Add Spectator Command Item To
     * This won't create the item back if you delete it.
     */
    @SuppressWarnings("WeakerAccess")
    public void saveSpectatorCommandItem(String name, String cmd, boolean enchanted, String material, int data, int slot) {
        if (isFirstTime()) {
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_COMMAND.replace("%path%", name), cmd);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_MATERIAL.replace("%path%", name), material);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_DATA.replace("%path%", name), data);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_ENCHANTED.replace("%path%", name), enchanted);
            getYml().addDefault(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_SLOT.replace("%path%", name), slot);
            getYml().options().copyDefaults(true);
            save();
        }
    }
}
