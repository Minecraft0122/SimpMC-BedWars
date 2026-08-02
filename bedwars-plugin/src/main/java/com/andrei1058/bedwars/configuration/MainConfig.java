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
import com.andrei1058.bedwars.arena.ArenaSelectorPagination;
import com.andrei1058.bedwars.arena.Misc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;


public class MainConfig extends ConfigManager {

    private static final int CONFIG_VERSION = 26;
    private static final int LOBBY_LEAVE_BROKEN_FROM_VERSION = 15;
    private static final int LOBBY_LEAVE_RESTORED_IN_VERSION = 18;
    private static final String LOBBY_LEAVE_PATH = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
    private static final Set<String> BUILT_IN_LOBBY_ITEM_FIELDS =
            Set.of("command", "material", "data", "enchanted", "slot");
    private static final double FIREBALL_EXPLOSION_SIZE_DEFAULT = 3.25;
    private static final double FIREBALL_SPEED_MULTIPLIER_DEFAULT = 16.0;
    private static final double FIREBALL_SNEAK_SPEED_MULTIPLIER_DEFAULT = 1.5;
    private static final double FIREBALL_HORIZONTAL_KNOCKBACK_DEFAULT = 1.15;
    private static final double FIREBALL_VERTICAL_KNOCKBACK_DEFAULT = 0.75;
    private static final double FIREBALL_ENEMY_DAMAGE_DEFAULT = 3.5;

    public MainConfig(Plugin plugin, String name) {
        super(plugin, name, BedWars.plugin.getDataFolder().getPath());

        YamlConfiguration yml = getYml();

        yml.options().header(plugin.getDescription().getName() + "，由 SimpMC 维护。\n");
        yml.addDefault("serverType", "MULTIARENA");
        yml.addDefault("language", "en");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, Collections.singletonList("your language iso here"));
        yml.addDefault("storeLink", "https://www.spigotmc.org/resources/authors/39904/");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER, "hub");
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
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING, false);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING, false);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_PLAYING, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_RESTARTING, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_LIST_REFRESH, 1200);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_ENABLE, true);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB, false);
        yml.addDefault(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_REFRESH, 300);
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_ENABLE, true);
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_REFRESH_INTERVAL, 20);
        yml.addDefault(ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER, Collections.emptyList());

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
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, FIREBALL_EXPLOSION_SIZE_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, FIREBALL_SPEED_MULTIPLIER_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER,
                FIREBALL_SNEAK_SPEED_MULTIPLIER_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL, 0.05);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE, false);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, FIREBALL_HORIZONTAL_KNOCKBACK_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, FIREBALL_VERTICAL_KNOCKBACK_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_COOLDOWN, 0.5);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF, 2.0);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, FIREBALL_ENEMY_DAMAGE_DEFAULT);
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

        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE,
                ArenaSelectorPagination.DEFAULT_SIZE);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SHOW_PLAYING, true);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS,
                ArenaSelectorPagination.DEFAULT_CONTENT_SLOTS);
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
        int storedConfigVersion = yml.getInt(CONFIG_VERSION_PATH, 0);
        LegacyLobbyItemHistory legacyLobbyItemHistory = findLegacyLobbyItemHistory(
                plugin.getDataFolder(), storedConfigVersion);
        updateToLatestVersion(CONFIG_VERSION, config -> {
            if (migrateLegacyConfig(config, legacyLobbyItemHistory)) {
                plugin.getLogger().info("已从 " + legacyLobbyItemHistory.fileName()
                        + " 恢复曾被旧版本迁移误删的大厅返回物品配置。");
            }
        });

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
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER,
                "BungeeCord/Velocity 代理 [servers] 中的主大厅服务器名称，不是 IP、端口或 MotD。",
                "名称必须与代理配置一致；Velocity 还需在 velocity.toml 的 [advanced] 中启用 bungee-plugin-message-channel。",
                "大厅里的“回到主大厅”红床会静默直接发送 Connect 请求，不向玩家显示代理信息；默认 hub。");
        setComments("language", "服务器默认语言代码，例如 zh_cn。");
        setComments("storeLink", "商店或官方网站链接，可在消息占位符中使用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, "不允许玩家选择的语言代码列表。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS,
                "可用于匹配和竞技场选择器的全局分组名称。Default 是内置组，无需填写。",
                "每张地图在 Arenas/<地图>.yml 的 group 中引用一个名称。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR, "计分板与 TAB 列表相关设置。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY,
                "是否在大厅 TAB 中显示玩家前后缀；默认开启，且不依赖右侧大厅计分板。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_LIST_REFRESH,
                "TAB 动态文字刷新周期，单位为 tick；1200 tick 等于 60 秒，小于 1 时关闭。",
                "刷新只写入真正变化的文字、颜色或名称可见性；静态内容不会重复发送队伍数据包。");
        setComments(ConfigPath.SB_CONFIG_SIDEBAR_HEALTH_IN_TAB,
                "是否在 TAB 玩家列表中额外显示生命值数字。默认关闭，只保留原版网络延迟图标，避免被误认为两个 ping。");
        setComments(ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER,
                "仅覆盖大厅 TAB 顶部文字；支持 & 颜色代码及 {serverIp}、{on} 等占位符。",
                "空列表使用语言文件中的加宽大厅页首；自定义文字也会自动保留内置宽度行，页尾不受影响。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_RESTART,
                "游戏结束后竞技场重置倒计时，单位为秒；默认 60 秒。",
                "聊天栏只在 60、30、15、10、5、4、3、2、1、0 秒时广播，避免刷屏。",
                "0 秒后先把所有玩家安全送回大厅；确认竞技场世界无人后才卸载，传送失败不会踢人。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, "玩家掉线后的可重连时间，单位为秒。", "超过该时间未重连将直接视为离开；默认 30 秒。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE, "治疗池功能设置。");
        setComments(ConfigPath.GENERAL_TNT_JUMP_BARYCENTER_IN_Y, "TNT 跳跃、爆炸保护与伤害设置。");
        setComments(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE,
                "火球爆炸、击退、冷却与伤害设置。2.10.20 略微降低默认爆炸范围、击退和敌方伤害。",
                "迁移器只调整仍使用上一版默认值的配置，不覆盖管理员自定义参数。");
        setComments("database.enable", "是否使用 MySQL；关闭时使用本地 SQLite。", "启用前请正确填写下面的连接信息。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN, "性能优化开关；通常建议保持启用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_DISABLE_CRAFTING, "竞技场内工作方块及合成功能限制。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH,
                "多竞技场大厅固定物品。默认提供历史战绩、竞技场选择器和“回到主大厅”红床。",
                "已有节点升级时只补齐缺失字段；删除整个节点后不会被后续架构升级重新创建。",
                "4.0.8 会从架构 15 删除前的最后快照，或架构 15–17 中重新配置过的快照，一次性恢复被误删的自定义 leave 节点。",
                "leave 节点会写入代理大厅目标标记；等待区和旁观区的 leave 节点则固定返回本服大厅。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE,
                "竞技场选择菜单设置；默认 54 格、每页显示 45 个竞技场，竞技场更多时自动分页。",
                "大小必须是 9 的倍数；自定义 use-slots 时翻页按钮固定使用底行左、中、右三个槽位。");
        setComments(ConfigPath.LOBBY_VOID_TELEPORT_ENABLED, "大厅掉入虚空时是否传送回大厅出生点。");
    }

    private static boolean migrateLegacyConfig(YamlConfiguration yml, LegacyLobbyItemHistory legacyLobbyItemHistory) {
        int storedConfigVersion = yml.getInt(CONFIG_VERSION_PATH, 0);
        migrateFireballDefaults(yml, yml.getInt(CONFIG_VERSION_PATH, 0));
        removeRetiredFullArmorSetting(yml);
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
        migrateArenaSelectorDefaults(yml, yml.getInt(CONFIG_VERSION_PATH, 0));

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

        boolean restoredLobbyItem = migrateLobbyItems(yml, storedConfigVersion, legacyLobbyItemHistory);

        for (String obsoletePath : List.of("arenaGui", "statsGUI", "startItems", "generators",
                "bedsDestroyCountdown", "dragonSpawnCountdown", "gameEndCountdown", "npcLoc", "blockedCmds",
                "lobbyScoreboard", "items", "start-items-per-arena", "safeMode", "disableCrafting",
                "fireball.damage-multiplier", "performance-settings.disable-armor-packets",
                "performance-settings.disable-respawn-packets")) {
            yml.set(obsoletePath, null);
        }
        migrateLobbyLocation(yml);
        migrateNpcLocations(yml);
        return restoredLobbyItem;
    }

    /**
     * Complete existing lobby item definitions without recreating nodes an
     * administrator deliberately removed. Versions 15-17 are the sole
     * exception: schema 15 deleted the leave node unconditionally, so those
     * versions need a bounded repair.
     */
    static boolean migrateLobbyItems(YamlConfiguration yml, int storedConfigVersion,
                                     LegacyLobbyItemHistory history) {
        boolean historicalDeletion = history != null && history.deleted();
        boolean restored = !historicalDeletion && recoverLegacyLobbyReturnItem(yml,
                history == null ? null : history.configuration(), storedConfigVersion);

        if (historicalDeletion && storedConfigVersion >= LOBBY_LEAVE_RESTORED_IN_VERSION
                && isBuiltInLobbyReturnItem(yml)) {
            yml.set(LOBBY_LEAVE_PATH, null);
        }

        ensureExistingLobbyItem(yml, "stats", "bw stats", false, "PLAYER_HEAD", 3, 0);
        ensureExistingLobbyItem(yml, "arena-selector", "bw gui", true, "CHEST", 5, 4);
        ensureExistingLobbyItem(yml, "leave", "bw leave", false, "RED_BED", 0, 8);

        if (!yml.isConfigurationSection(LOBBY_LEAVE_PATH)
                && storedConfigVersion >= LOBBY_LEAVE_BROKEN_FROM_VERSION
                && storedConfigVersion < LOBBY_LEAVE_RESTORED_IN_VERSION
                && !historicalDeletion
                && !hasLobbyReturnItem(yml)
                && !isLobbySlotUsed(yml, 8)) {
            ensureLobbyItem(yml, "leave", "bw leave", false, "RED_BED", 0, 8);
        }
        return restored;
    }

    private static void ensureExistingLobbyItem(YamlConfiguration yml, String name, String command,
                                                boolean enchanted, String material, int data, int slot) {
        String path = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + '.' + name;
        if (yml.isConfigurationSection(path)) {
            ensureLobbyItem(yml, name, command, enchanted, material, data, slot);
        }
    }

    static boolean recoverLegacyLobbyReturnItem(YamlConfiguration current, YamlConfiguration legacyBackup,
                                                int storedConfigVersion) {
        if (legacyBackup == null || storedConfigVersion < LOBBY_LEAVE_BROKEN_FROM_VERSION
                || !legacyBackup.isConfigurationSection(LOBBY_LEAVE_PATH)
                || isBuiltInLobbyReturnItem(legacyBackup)) {
            return false;
        }

        boolean missing = !current.isConfigurationSection(LOBBY_LEAVE_PATH);
        boolean missingInBrokenWindow = missing && storedConfigVersion < LOBBY_LEAVE_RESTORED_IN_VERSION;
        boolean autoRestoredDefault = storedConfigVersion >= LOBBY_LEAVE_RESTORED_IN_VERSION
                && isBuiltInLobbyReturnItem(current);
        if (!missingInBrokenWindow && !autoRestoredDefault) return false;

        copyConfigurationSection(legacyBackup, current, LOBBY_LEAVE_PATH);
        ensureLobbyItem(current, "leave", "bw leave", false, "RED_BED", 0, 8);
        return true;
    }

    private static boolean isBuiltInLobbyReturnItem(YamlConfiguration yml) {
        ConfigurationSection section = yml.getConfigurationSection(LOBBY_LEAVE_PATH);
        if (section == null || !section.getKeys(false).equals(BUILT_IN_LOBBY_ITEM_FIELDS)) return false;
        return "RED_BED".equalsIgnoreCase(yml.getString(LOBBY_LEAVE_PATH + ".material", ""))
                && yml.getInt(LOBBY_LEAVE_PATH + ".data", Integer.MIN_VALUE) == 0
                && !yml.getBoolean(LOBBY_LEAVE_PATH + ".enchanted", true)
                && yml.getInt(LOBBY_LEAVE_PATH + ".slot", Integer.MIN_VALUE) == 8
                && "bw leave".equalsIgnoreCase(yml.getString(LOBBY_LEAVE_PATH + ".command", "").trim());
    }

    private static boolean hasLobbyReturnItem(YamlConfiguration yml) {
        ConfigurationSection section = yml.getConfigurationSection(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH);
        if (section == null) return false;
        for (String item : section.getKeys(false)) {
            if (item.equalsIgnoreCase("leave")) return true;
            String command = section.getString(item + ".command", "");
            if (command.trim().equalsIgnoreCase("bw leave")) return true;
        }
        return false;
    }

    private static boolean isLobbySlotUsed(YamlConfiguration yml, int slot) {
        ConfigurationSection section = yml.getConfigurationSection(
                ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH);
        if (section == null) return false;
        for (String item : section.getKeys(false)) {
            String path = item + ".slot";
            if (section.isInt(path) && section.getInt(path) == slot) return true;
        }
        return false;
    }

    private static void copyConfigurationSection(YamlConfiguration source, YamlConfiguration target, String path) {
        ConfigurationSection section = source.getConfigurationSection(path);
        if (section == null) return;
        target.set(path, null);
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                target.set(path + '.' + key, section.get(key));
            }
        }
    }

    static LegacyLobbyItemHistory findLegacyLobbyItemHistory(File dataFolder, int storedConfigVersion) {
        File[] files = dataFolder.listFiles((directory, name) ->
                name.startsWith("config.yml.v") && name.endsWith(".bak"));
        if (files == null) return null;

        SortedMap<Integer, File> snapshots = new TreeMap<>();
        for (File file : files) {
            int version = backupVersion(file.getName());
            if (version < 0 || version >= storedConfigVersion) continue;
            snapshots.put(version, file);
        }

        LegacyLobbyItemHistory candidate = null;
        for (Map.Entry<Integer, File> entry : snapshots.entrySet()) {
            int version = entry.getKey();
            File file = entry.getValue();
            YamlConfiguration snapshot = YamlConfiguration.loadConfiguration(file);
            boolean exists = snapshot.isConfigurationSection(LOBBY_LEAVE_PATH);
            boolean custom = exists && !isBuiltInLobbyReturnItem(snapshot);
            LegacyLobbyItemHistory customState = new LegacyLobbyItemHistory(
                    file.getName(), version, snapshot, false);
            LegacyLobbyItemHistory deletedState = new LegacyLobbyItemHistory(
                    file.getName(), version, snapshot, true);

            if (version < LOBBY_LEAVE_BROKEN_FROM_VERSION) {
                // Every pre-15 snapshot is authoritative. Missing or exact
                // default state resets still older customization; missing also
                // records a deletion that later fallback repair must respect.
                candidate = custom ? customState : exists ? null : deletedState;
            } else if (version < LOBBY_LEAVE_RESTORED_IN_VERSION) {
                // Schema 15-17 deleted the item on every migration, so missing
                // snapshots are neutral. Existing state was explicitly added.
                if (exists) candidate = custom ? customState : null;
            } else if (!exists) {
                // After schema 18, missing state records a new deletion and can
                // remove a default item recreated by a later old migration.
                candidate = deletedState;
            } else if (custom) {
                // Post-repair customization vetoes historical recovery. If the
                // current item is now default, it is newer than this snapshot.
                candidate = null;
            }
        }
        return candidate;
    }

    static int backupVersion(String fileName) {
        String prefix = "config.yml.v";
        String suffix = ".bak";
        if (fileName == null || !fileName.startsWith(prefix) || !fileName.endsWith(suffix)) return -1;
        try {
            return Integer.parseInt(fileName.substring(prefix.length(), fileName.length() - suffix.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    record LegacyLobbyItemHistory(String fileName, int version, YamlConfiguration configuration, boolean deleted) {
    }

    static void migrateArenaSelectorDefaults(YamlConfiguration yml, int storedConfigVersion) {
        if (storedConfigVersion >= 24) return;
        String oldSlots = "10,11,12,13,14,15,16";
        if (yml.getInt(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE, 27) == 27
                && oldSlots.equals(yml.getString(
                ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS, oldSlots))) {
            yml.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE,
                    ArenaSelectorPagination.DEFAULT_SIZE);
            yml.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_USE_SLOTS,
                    ArenaSelectorPagination.DEFAULT_CONTENT_SLOTS);
        }
    }

    static void migrateFireballDefaults(YamlConfiguration yml, int storedConfigVersion) {
        // Fireball enhancement defaults were introduced by configuration schema 10.
        // Treat earlier values as defaults only for files that predate that schema;
        // otherwise an administrator may have deliberately chosen those numbers.
        if (storedConfigVersion < 10) {
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.0,
                    FIREBALL_EXPLOSION_SIZE_DEFAULT);
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.0,
                    FIREBALL_HORIZONTAL_KNOCKBACK_DEFAULT);
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.65,
                    FIREBALL_VERTICAL_KNOCKBACK_DEFAULT);
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 2.0,
                    FIREBALL_ENEMY_DAMAGE_DEFAULT);
        }
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE, 3.5,
                FIREBALL_EXPLOSION_SIZE_DEFAULT);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, 1.25,
                FIREBALL_HORIZONTAL_KNOCKBACK_DEFAULT);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, 0.8,
                FIREBALL_VERTICAL_KNOCKBACK_DEFAULT);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, 4.0,
                FIREBALL_ENEMY_DAMAGE_DEFAULT);
        if (storedConfigVersion < 23) {
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 10.0,
                    FIREBALL_SPEED_MULTIPLIER_DEFAULT);
        }
        if (storedConfigVersion < 25) {
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 11.0,
                    FIREBALL_SPEED_MULTIPLIER_DEFAULT);
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER, 1.25,
                    FIREBALL_SNEAK_SPEED_MULTIPLIER_DEFAULT);
        }
    }

    static void removeRetiredFullArmorSetting(YamlConfiguration yml) {
        yml.set(ConfigPath.GENERAL_CONFIGURATION_SHOP_SELL_FULL_ARMOR, null);
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
        boolean usedTemporaryGlobalTab = yml.isList(ConfigPath.SB_CONFIG_TAB_HEADER)
                || yml.isList(ConfigPath.SB_CONFIG_TAB_FOOTER);
        if (usedTemporaryGlobalTab) {
            yml.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING, false);
            yml.set(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING, false);
        }
        yml.set(ConfigPath.SB_CONFIG_TAB_HEADER, null);
        yml.set(ConfigPath.SB_CONFIG_TAB_FOOTER, null);
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

    static void ensureLobbyItem(YamlConfiguration yml, String name, String command, boolean enchanted,
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
        String storedLobby = getYml().getString("lobbyLoc");
        if (storedLobby == null || storedLobby.isBlank()) return "";
        try {
            return ConfigManager.getWorldNameFromConfigLocation(storedLobby);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static void migrateNpcLocations(YamlConfiguration yml) {
        if (!yml.isList(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE)) return;
        String fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst().getName();
        List<String> normalized = new ArrayList<>();
        for (String entry : yml.getStringList(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE)) {
            try {
                normalized.add(normalizeNpcLocationEntry(entry, fallbackWorld));
            } catch (IllegalArgumentException exception) {
                BedWars.plugin.getLogger().warning("Could not migrate join NPC location: "
                        + exception.getMessage());
                normalized.add(entry);
            }
        }
        yml.set(ConfigPath.GENERAL_CONFIGURATION_NPC_LOC_STORAGE, normalized);
    }

    static String normalizeNpcLocationEntry(String entry, String fallbackWorld) {
        if (entry == null) throw new IllegalArgumentException("NPC location cannot be null");
        String[] fields = entry.split(",");
        if (fields.length < 10) throw new IllegalArgumentException("expected 10 NPC fields");
        String location = ConfigManager.normalizeConfigLocationString(
                String.join(",", Arrays.copyOfRange(fields, 0, 6)), fallbackWorld);
        return location + ',' + String.join(",", Arrays.copyOfRange(fields, 6, fields.length));
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
