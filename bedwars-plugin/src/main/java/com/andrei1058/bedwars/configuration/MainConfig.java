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
import com.andrei1058.bedwars.BungeeNodeRole;
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

    private static final int CONFIG_VERSION = 34;
    private static final int LOBBY_LEAVE_BROKEN_FROM_VERSION = 15;
    private static final int LOBBY_LEAVE_RESTORED_IN_VERSION = 18;
    private static final String LOBBY_LEAVE_PATH = ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH + ".leave";
    private static final Set<String> BUILT_IN_LOBBY_ITEM_FIELDS =
            Set.of("command", "material", "data", "enchanted", "slot");
    private static final double FIREBALL_EXPLOSION_SIZE_DEFAULT = 3.25;
    private static final double FIREBALL_SPEED_MULTIPLIER_DEFAULT = 15.0;
    private static final double FIREBALL_SNEAK_SPEED_MULTIPLIER_DEFAULT = 1.6;
    private static final double FIREBALL_SNEAK_ACCELERATION_MULTIPLIER_DEFAULT = 2.0;
    private static final double FIREBALL_SNEAK_RECOIL_DEFAULT = 0.1;
    private static final double FIREBALL_COOLDOWN_DEFAULT = 0.4;
    private static final double FIREBALL_HORIZONTAL_KNOCKBACK_DEFAULT = 1.15;
    private static final double FIREBALL_VERTICAL_KNOCKBACK_DEFAULT = 0.75;
    private static final double FIREBALL_ENEMY_DAMAGE_DEFAULT = 3.5;
    private static final double TNT_KNOCKBACK_MULTIPLIER_DEFAULT = 0.9;
    private static final double TNT_DAMAGE_TEAMMATES_DEFAULT = 4.0;
    private static final double TNT_DAMAGE_OTHERS_DEFAULT = 8.0;

    public MainConfig(Plugin plugin, String name) {
        super(plugin, name, BedWars.plugin.getDataFolder().getPath());

        YamlConfiguration yml = getYml();

        yml.options().header(plugin.getDescription().getName() + "，由 SimpMC 维护。\n");
        yml.addDefault("serverType", "MULTIARENA");
        yml.addDefault("language", Language.SIMPLIFIED_CHINESE_ISO);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, Collections.emptyList());
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
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_RESTART, 60);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_RE_SPAWN_COUNTDOWN, 5);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BEDS_DESTROY_COUNTDOWN, 360);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_DRAGON_SPAWN_COUNTDOWN, 600);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_GAME_END_COUNTDOWN, 120);
        yml.addDefault(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP, "simpmc.org");
        yml.addDefault(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_POWERED_BY, "SimpMC-BedWars");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, "bw1");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_BWP_TIME_OUT, 23000);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_NODE_ROLE, "ARENA");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_PROXY_SERVER, "");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_ARENA_TEMPLATE, "");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_LISTEN_HOST, "0.0.0.0");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_LISTEN_PORT, 2019);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_SOCKET_SECRET, "");
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_NODE_TIMEOUT_SECONDS, 30);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_DISPATCH_TIMEOUT_SECONDS, 8);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_STATUS_HEARTBEAT_SECONDS, 15);

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
        yml.addDefault(ConfigPath.GENERAL_TNT_KNOCKBACK_MULTIPLIER, TNT_KNOCKBACK_MULTIPLIER_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_SELF, 1);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES, TNT_DAMAGE_TEAMMATES_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS, TNT_DAMAGE_OTHERS_DEFAULT);

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
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_SNEAK_ACCELERATION_MULTIPLIER,
                FIREBALL_SNEAK_ACCELERATION_MULTIPLIER_DEFAULT);
        addFireballFlightRangeDefaults(yml);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL, FIREBALL_SNEAK_RECOIL_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE, false);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, FIREBALL_HORIZONTAL_KNOCKBACK_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, FIREBALL_VERTICAL_KNOCKBACK_DEFAULT);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_COOLDOWN, FIREBALL_COOLDOWN_DEFAULT);
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

        // Match-level statistics are stored in MySQL and never share a
        // transaction with the legacy global statistics tables.
        yml.addDefault(ConfigPath.MATCH_STATISTICS_ENABLED, true);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_TIMEZONE, "Asia/Shanghai");
        yml.addDefault(ConfigPath.MATCH_STATISTICS_REPORT_INTERVAL_SECONDS, 300);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_QUEUE_CAPACITY, 10000);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_RETRY_DELAY_SECONDS, 5);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_FINISH_GRACE_TICKS, 40);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_VIOLATIONS_ENABLED, true);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_VIOLATIONS_WARNING_THRESHOLDS, Arrays.asList(10, 20, 50, 100));
        yml.addDefault(ConfigPath.MATCH_STATISTICS_VIOLATIONS_MATCH_LEAVE_THRESHOLD, 25);
        yml.addDefault(ConfigPath.MATCH_STATISTICS_VIOLATIONS_CROSS_TEAM_ITEM_TRANSFER, true);

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

        // Only Simplified Chinese is bundled and selectable. Keep legacy
        // language files untouched on disk, but never load them as runtime
        // languages.
        String configuredLanguage = yml.getString("language");
        if (!Language.isSimplifiedChineseIso(configuredLanguage)) {
            plugin.getLogger().warning("仅支持简体中文，已将 language 配置从 "
                    + configuredLanguage + " 迁移为 zh_cn。");
            yml.set("language", Language.SIMPLIFIED_CHINESE_ISO);
            save();
        }
        Language def = Language.getLang(Language.SIMPLIFIED_CHINESE_ISO);

        if (def == null) {
            throw new IllegalStateException("未找到简体中文语言配置："
                    + Language.SIMPLIFIED_CHINESE_ISO);
        }
        Language.setDefaultLanguage(def);

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

        String configuredRole = yml.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_NODE_ROLE, "ARENA");
        BungeeNodeRole parsedRole = BungeeNodeRole.parse(configuredRole);
        if (configuredRole != null && !configuredRole.isBlank()
                && parsedRole == BungeeNodeRole.ARENA
                && !configuredRole.trim().equalsIgnoreCase("ARENA")) {
            plugin.getLogger().warning("未知的 BUNGEE 节点角色 " + configuredRole + "，已回退为 ARENA。可选值：ARENA、LOBBY。");
        }
        BedWars.setBungeeNodeRole(parsedRole);

        BedWars.setLobbyWorld(getLobbyWorldName());
    }

    private void addConfigurationComments() {
        setComments("serverType", "服务器运行模式：MULTIARENA、SHARED 或 BUNGEE。", "MULTIARENA/BUNGEE 会把实例内全部世界固定为正午和晴天；SHARED 只处理竞技场、设置世界和配置大厅。", "修改后需要完整重启服务器。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_SERVER,
                "BungeeCord/Velocity 代理 [servers] 中的主大厅服务器名称，不是 IP、端口或 MotD。",
                "名称必须与代理配置一致；Velocity 还需在 velocity.toml 的 [advanced] 中启用 bungee-plugin-message-channel。",
                "大厅里的“回到主大厅”红床会静默直接发送 Connect 请求，不向玩家显示代理信息；默认 hub。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID,
                "BUNGEE 子服在代理和对局数据库中的唯一节点 ID。每个子服必须使用不同值，不能沿用默认 bw1。",
                "该 ID 也用于启动恢复：只会收敛本节点上次异常退出的 RUNNING 对局。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_BWP_TIME_OUT,
                "竞技场子服等待代理登录预加载条目的时间，单位为毫秒；默认 23000。",
                "该值至少会覆盖大厅预加载确认超时并额外保留 15 秒代理切服余量；旧配置中的较小值会自动按此规则抬高。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_NODE_ROLE,
                "BUNGEE 节点角色：ARENA 负责加载地图并运行对局；LOBBY 只监听竞技场节点并负责分配玩家。",
                "旧版 BUNGEE 配置默认使用 ARENA；大厅服必须明确设置为 LOBBY。修改后需要完整重启服务器。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_PROXY_SERVER,
                "本节点在 BungeeCord/Velocity [servers] 中的服务器键名，用于大厅调度后的 Connect。",
                "留空时使用 server-id；每个竞技场节点应与代理配置的键名一致。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_ARENA_TEMPLATE,
                "ARENA 节点负责的地图配置文件名，不含 .yml；同一节点只为此地图自动复制副本。",
                "留空时保留旧版行为并加载 Arenas 目录中的全部地图；新部署建议每个子服明确填写一个地图名。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_LISTEN_HOST,
                "LOBBY 节点监听竞技场状态套接字的绑定地址；建议只绑定内网地址。", "仅 LOBBY 角色使用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_LOBBY_LISTEN_PORT,
                "LOBBY 节点监听竞技场状态套接字的 TCP 端口；每个大厅服端口必须可被竞技场节点访问。", "仅 LOBBY 角色使用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_SOCKET_SECRET,
                "竞技场与大厅套接字共享密钥；非空时双方 HELLO 必须完全一致。", "建议设置随机长字符串并用防火墙限制监听端口；留空仅用于受信任的旧网络兼容。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_NODE_TIMEOUT_SECONDS,
                "大厅判定竞技场节点状态过期的时间，单位为秒；默认 30 秒。", "过期节点不会被新的玩家调度。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_DISPATCH_TIMEOUT_SECONDS,
                "大厅等待竞技场节点确认玩家预加载的时间，单位为秒；默认 8 秒。", "超时会释放预约并提示玩家重试，不会阻塞服务器主线程。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_STATUS_HEARTBEAT_SECONDS,
                "竞技场节点向大厅发送全量状态心跳的间隔，单位为秒；默认 15 秒。", "平时状态变化通过事件立即发送，心跳只用于断线重连后的恢复。");
        setComments("language", "服务器语言固定为 zh_cn（简体中文）；旧语言值会自动迁移。");
        setComments("storeLink", "商店或官方网站链接，可在消息占位符中使用。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, "历史兼容字段；当前版本只提供简体中文，不再加载其他语言。");
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
        setComments(ConfigPath.SB_CONFIG_TAB_HEADER_FOOTER_REFRESH_INTERVAL,
                "TAB 页首页尾动态内容刷新周期，单位为 tick；默认 20 tick（1 秒）。",
                "本局游戏时间复用该任务和内容缓存，不会创建额外计时器或扫描全服玩家；小于 1 时关闭动态刷新。");
        setComments(ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER,
                "仅覆盖大厅 TAB 顶部文字；支持 & 颜色代码及 {serverIp}、{on} 等占位符。",
                "空列表使用语言文件中的加宽大厅页首；自定义文字也会自动保留内置宽度行，页尾不受影响。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_RESTART,
                "游戏结束后竞技场重置倒计时，单位为秒；默认 60 秒。",
                "聊天栏只在 60、30、15、10、5、4、3、2、1、0 秒时广播，避免刷屏。",
                "0 秒后先把所有玩家安全送回大厅；确认竞技场世界无人后才卸载，传送失败不会踢人。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, "玩家掉线后的可重连时间，单位为秒。", "超过该时间未重连将直接视为离开；默认 30 秒。");
        setComments(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE, "治疗池功能设置。");
        setComments(ConfigPath.GENERAL_TNT_JUMP_BARYCENTER_IN_Y,
                "TNT 跳跃、爆炸击退、爆炸保护与伤害设置。",
                "knockback-multiplier 默认 0.90，只缩放 TNT 对玩家的击退，不改变方块爆炸范围；damage-teammates 和 damage-others 的新默认值为 4 和 8。",
                "管理员自定义值会在配置架构升级时保留。");
        setComments(ConfigPath.GENERAL_TNT_KNOCKBACK_MULTIPLIER,
                "TNT 对玩家的击退倍率；默认 0.90。范围为 0 至 4，0 表示不产生 TNT 击退。",
                "该项同时作用于 TNT 跳和 TNT 对其他玩家的原生爆炸冲量，不会改变爆炸破坏范围。");
        setComments(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE,
                "火球爆炸、击退、冷却与伤害设置。2.10.20 略微降低默认爆炸范围、击退和敌方伤害。",
                "迁移器只调整仍使用上一版默认值的配置，不覆盖管理员自定义参数。");
        setComments("database.enable", "是否使用 MySQL；关闭时使用本地 SQLite。", "启用前请正确填写下面的连接信息。");
        setComments(ConfigPath.MATCH_STATISTICS_ENABLED,
                "是否记录按对局拆分的统计数据。此功能需要 database.enable=true 且 MySQL 连接成功。",
                "旧 global_stats 表仍保留给历史 GUI；新数据写入独立的 InnoDB 表，不会在开局事务中锁住旧表。");
        setComments(ConfigPath.MATCH_STATISTICS_TIMEZONE,
                "竞技场统计使用的时区，默认 Asia/Shanghai。",
                "数据库同时保存该时区名称；建议所有子服保持相同配置。无效时区会回退为 Asia/Shanghai 并记录警告。");
        setComments(ConfigPath.MATCH_STATISTICS_REPORT_INTERVAL_SECONDS,
                "进行中的对局上报间隔，单位为秒；默认 300（5 分钟）。上报在异步队列中执行，不阻塞主线程。");
        setComments(ConfigPath.MATCH_STATISTICS_QUEUE_CAPACITY,
                "统计写入队列容量；队列满时记录警告，下一次周期会上报最新快照，结束结算会持续重试。");
        setComments(ConfigPath.MATCH_STATISTICS_RETRY_DELAY_SECONDS,
                "MySQL 写入失败后的重试间隔，单位为秒。短事务失败不会影响新对局开始。");
        setComments(ConfigPath.MATCH_STATISTICS_FINISH_GRACE_TICKS,
                "收到游戏结束事件后等待的 tick 数，再写入最终结算；用于接收同一 tick 内的掉线击杀事件。");
        setComments(ConfigPath.MATCH_STATISTICS_VIOLATIONS_ENABLED,
                "是否启用非法组队和刷人头检测及 VL 保存。",
                "关闭只停止 VL 累加，不影响普通击杀、最终击杀、拆床和死亡统计。");
        setComments(ConfigPath.MATCH_STATISTICS_VIOLATIONS_WARNING_THRESHOLDS,
                "处罚依据累计 VL 严格超过这些值时在控制台告警；默认 10、20、50、100。",
                "每个阈值只在累计值首次跨过时打印一次；处罚重置 punishment_total_vl 后允许再次告警。");
        setComments(ConfigPath.MATCH_STATISTICS_VIOLATIONS_MATCH_LEAVE_THRESHOLD,
                "单局有效 VL 严格超过此值时，下一 tick 将玩家移出当前对局；默认 25。",
                "该动作只作用于当前对局，不会因为历史犯罪记录直接踢出玩家。");
        setComments(ConfigPath.MATCH_STATISTICS_VIOLATIONS_CROSS_TEAM_ITEM_TRANSFER,
                "是否把不同队伍玩家之间重复掉落/拾取铁、金、钻石、绿宝石计为强证据。",
                "默认开启；生成器物品、羊毛、工具和一次性死亡掉落不作为此规则的唯一依据。");
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
        migrateTntDefaults(yml, storedConfigVersion);
        if (migrateSchema28Only(yml, storedConfigVersion)) {
            migrateFireballDefaults(yml, storedConfigVersion);
            removeRetiredFullArenaCountdownSetting(yml);
            return false;
        }
        migrateFireballDefaults(yml, yml.getInt(CONFIG_VERSION_PATH, 0));
        removeRetiredFullArenaCountdownSetting(yml);
        removeRetiredFullArmorSetting(yml);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_CONFIGURATION_RESTART, 45.0, 60.0);
        if (yml.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 300) == 300) {
            yml.set(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, 30);
        }
        migrateTabDisplayDefaults(yml);
        removeShoutCooldownSetting(yml);
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
     * Apply the only schema 28 removal without replaying historical migrations
     * against an already current schema 27 configuration.
     */
    static boolean migrateSchema28Only(YamlConfiguration yml, int storedConfigVersion) {
        if (storedConfigVersion < 27) {
            return false;
        }
        removeShoutCooldownSetting(yml);
        return true;
    }

    static void removeShoutCooldownSetting(YamlConfiguration yml) {
        yml.set(ConfigPath.GENERAL_CONFIGURATION_SHOUT_COOLDOWN, null);
    }

    static void removeRetiredFullArenaCountdownSetting(YamlConfiguration yml) {
        yml.set("countdowns.game-start-shortened", null);
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
        if (storedConfigVersion < 29) {
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_SNEAK_RECOIL, 0.05,
                    FIREBALL_SNEAK_RECOIL_DEFAULT);
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_COOLDOWN, 0.5,
                    FIREBALL_COOLDOWN_DEFAULT);
        }
        if (storedConfigVersion < 30) {
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, 16.0,
                    FIREBALL_SPEED_MULTIPLIER_DEFAULT);
            upgradeLegacyNumber(yml, ConfigPath.GENERAL_FIREBALL_SNEAK_SPEED_MULTIPLIER, 1.5,
                    FIREBALL_SNEAK_SPEED_MULTIPLIER_DEFAULT);
        }
    }

    /**
     * Reduce only the previous built-in TNT damage defaults. A server owner
     * that changed either value keeps that choice during the schema upgrade.
     */
    static void migrateTntDefaults(YamlConfiguration yml, int storedConfigVersion) {
        if (storedConfigVersion >= CONFIG_VERSION) return;
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_TNT_JUMP_DAMAGE_TEAMMATES,
                5.0, TNT_DAMAGE_TEAMMATES_DEFAULT);
        upgradeLegacyNumber(yml, ConfigPath.GENERAL_TNT_JUMP_DAMAGE_OTHERS,
                10.0, TNT_DAMAGE_OTHERS_DEFAULT);
    }

    static void addFireballFlightRangeDefaults(YamlConfiguration yml) {
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MIN, 200.0D);
        yml.addDefault(ConfigPath.GENERAL_FIREBALL_FLIGHT_RANGE_MAX, 300.0D);
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
