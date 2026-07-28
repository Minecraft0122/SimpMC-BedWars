package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Centralized Chinese documentation for every administrator-facing YAML file.
 */
public final class ChineseConfigDocumentation {

    private ChineseConfigDocumentation() {
    }

    public static void main(ConfigManager config) {
        comment(config, "serverType", "服务器模式：MULTIARENA（多竞技场）、SHARED（共享服务器）或 BUNGEE（代理网络）。", "修改后必须完整重启服务器。");
        comment(config, "language", "默认语言代码，例如 zh_cn；玩家仍可使用 /bw lang 单独选择语言。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_DISABLED_LANGUAGES, "禁止玩家选择的语言代码列表。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS,
                "可用于匹配和竞技场选择器的全局分组名称；Default 是内置组，无需填写。",
                "竞技场 groups 列表可以同时引用多个这里声明的组。");
        comment(config, "storeLink", "服务器商店或官方网站地址，可用于消息占位符。");
        comment(config, "lobbyServer",
                "BungeeCord/Velocity 代理 [servers] 中的主大厅服务器名称，不是 IP、端口或 MotD。",
                "名称必须与代理配置一致；Velocity 需在 velocity.toml 的 [advanced] 中设置 bungee-plugin-message-channel = true。",
                "MULTIARENA 和 BUNGEE 模式下的主大厅返回物品都会静默直接连接此服务器。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_ENABLE_HALLOWEEN, "是否启用万圣节季节效果。");
        comment(config, ConfigPath.GENERAL_CHAT_GLOBAL, "聊天设置：global 控制不同竞技场是否互通，format 控制是否使用插件聊天格式。");
        comment(config, "debug", "调试日志开关；排查问题时临时开启，正常运行建议关闭。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_MARK_LEAVE_AS_ABANDON, "玩家主动离开进行中的游戏时，是否记为中途退出。");
        comment(config, ConfigPath.GENERAL_ENABLE_PARTY_CMD, "组队系统设置：是否启用命令、是否允许组队以及外部 Parties 的最低等级。");
        comment(config, ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR, "计分板、TAB 玩家列表、血量显示及刷新周期设置。", "TAB 页首和页尾按游戏状态读取玩家语言文件。", "刷新周期单位为 tick，20 tick 约等于 1 秒。");
        comment(config, ConfigPath.SB_CONFIG_SIDEBAR_LIST_REFRESH,
                "TAB 动态文字刷新周期，默认 1200 tick（60 秒）；小于 1 时关闭。",
                "周期刷新只同步发生变化的队伍显示属性，不会重复写入静态内容。");
        comment(config, ConfigPath.SB_CONFIG_TAB_LOBBY_HEADER, "仅自定义大厅 TAB 顶部文字；空列表沿用语言文件中的加宽样式。", "自定义文字会自动保留内置宽度行；支持 & 颜色代码及 {serverIp}、{on} 等占位符，不会改变大厅页尾或其他游戏状态。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME, "掉线玩家允许重连的时间，单位为秒；超时后直接视为离开。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_BUNGEE_MODE_GAMES_BEFORE_RESTART, "BUNGEE/自动扩容相关设置：重启场次、重启命令、节点 ID、超时及大厅地址。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_LOBBY_SERVERS, "大厅套接字地址列表，格式为 主机:端口。", "协议没有身份认证和加密，只能填写受信任的内网地址，并使用防火墙禁止公网访问。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_REGULAR, "游戏各阶段倒计时，单位为秒。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_RESTART,
                "游戏结束后的竞技场重置倒计时，单位为秒。",
                "倒计时结束后先安全返回大厅；确认世界无人后才卸载，传送失败会重试而不是踢出玩家。");
        comment(config, ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP, "内置占位符显示的服务器地址和品牌文本。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_HUNGER_WAITING, "是否允许等待阶段和游戏阶段消耗饥饿值。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_ALLOW_FIRE_EXTINGUISH, "玩家是否可以扑灭竞技场中的火焰。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE, "队伍基地治疗池设置。");
        comment(config, ConfigPath.GENERAL_TNT_JUMP_BARYCENTER_IN_Y, "TNT 跳跃力度、伤害与重心修正参数。");
        comment(config, ConfigPath.GENERAL_TNT_PROTECTION_END_STONE_BLAST, "TNT 对末地石、玻璃等方块的爆炸抗性参数。");
        comment(config, ConfigPath.GENERAL_TNT_AUTO_IGNITE, "TNT 放置后是否自动点燃，以及引信持续 tick 数。");
        comment(config, ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE,
                "火球爆炸和击退生效范围；2.10.20 默认值略微降低为 3.25。");
        comment(config, ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER, "火球飞行速度倍率。");
        comment(config, ConfigPath.GENERAL_FIREBALL_MAKE_FIRE, "火球爆炸后是否在命中处生成火焰；竞技场始终禁止火势向周围蔓延。");
        comment(config, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL, "火球水平击退强度；2.10.20 默认值略微降低为 1.15。");
        comment(config, ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL, "火球垂直击退强度；2.10.20 默认值略微降低为 0.75。");
        comment(config, ConfigPath.GENERAL_FIREBALL_COOLDOWN, "连续使用火球的冷却时间，单位为秒。");
        comment(config, ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF, "火球对发射者造成的伤害。");
        comment(config, ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY, "火球对敌人造成的伤害；2.10.20 默认值略微降低为 3.5。");
        comment(config, ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES, "火球对队友造成的伤害；0 表示不伤害队友。");
        comment(config, "database.enable", "数据存储设置：关闭时使用 SQLite，开启时连接 MySQL。", "修改连接信息后必须完整重启，切勿公开数据库密码。");
        comment(config, "database.pass", "MySQL 专用账户密码；不要沿用示例值，也不要提交到公开仓库。");
        comment(config, "database.ssl", "MySQL 连接是否启用 TLS；远程数据库建议开启并限制允许连接的来源地址。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN, "性能设置；Paper 优化通常建议保持开启。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_DISABLE_CRAFTING, "竞技场内合成台、附魔台、熔炉、酿造台和铁砧的禁用设置。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH,
                "MULTIARENA 大厅物品：默认包含历史战绩、竞技场选择器和返回主大厅红床。",
                "每项可配置命令、材质、数据值、附魔外观和背包槽位；升级只补缺失字段，不覆盖自定义值。",
                "leave 节点固定标记为代理大厅；等待区和旁观区的 leave 节点固定返回本服 BedWars 大厅。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH, "竞技场等待阶段物品；格式与大厅物品相同。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH, "观战状态物品；格式与大厅物品相同。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_ARENA_SELECTOR_SETTINGS_SIZE, "竞技场选择菜单设置；菜单大小必须是 9 的倍数，槽位使用英文逗号分隔。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE, "历史战绩菜单大小和每个统计项的材质、槽位。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_DEFAULT_ITEMS, "按竞技场分组配置开局默认物品，填写 Bukkit Material 名称。");
        comment(config, ConfigPath.CENERAL_CONFIGURATION_ALLOWED_COMMANDS, "游戏中允许玩家使用的命令名称，不要填写开头的斜杠。");
        comment(config, ConfigPath.GENERAL_CONFIGURATION_ENABLE_GEN_SPLIT, "是否允许同队玩家共享生成器掉落物。");
        comment(config, ConfigPath.LOBBY_VOID_TELEPORT_ENABLED, "大厅虚空传送开关和触发高度。");
        comment(config, ConfigPath.GENERAL_GAME_END_SHOW_ELIMINATED, "游戏结束时淘汰玩家、传送、聊天最佳数据和计分板最佳数据的显示设置。");
    }

    public static void arena(ConfigManager config) {
        comment(config, "groups", "竞技场所属的全部匹配分组；可以同时填写多个。", "第一项是主组，生成器、开局物品、升级菜单和计分板等组专属配置读取主组。", "旧版 group 字段会自动迁移并删除。");
        comment(config, ConfigPath.ARENA_DISPLAY_NAME, "玩家可见名称；留空时使用竞技场世界名。");
        comment(config, "maxInTeam", "每支队伍可容纳的最大玩家数；所有设置模式均可使用 /bw setMaxInTeam 修改。", "创建队伍不会自动覆盖此值；setType 会写入所选类型的标准容量。");
        comment(config, "minInTeam", "正常开局时每支实际参赛队伍的最少玩家数。", "必须介于 1 和 maxInTeam 之间，Tab 补全按当前最大人数生成；调试开局不受此限制。");
        comment(config, "allowSpectate", "是否允许玩家在游戏开始后进入观战。");
        comment(config, ConfigPath.ARENA_SPAWN_PROTECTION, "队伍出生点保护半径，单位为方块。");
        comment(config, ConfigPath.ARENA_SHOP_PROTECTION, "商店 NPC 周围保护半径，最小为 1 格；按方块坐标对称保护。");
        comment(config, ConfigPath.ARENA_UPGRADES_PROTECTION, "升级 NPC 周围保护半径，最小为 1 格；按方块坐标对称保护。");
        comment(config, ConfigPath.ARENA_GENERATOR_PROTECTION, "资源生成器周围保护半径。");
        comment(config, ConfigPath.ARENA_ISLAND_RADIUS, "队伍岛屿识别半径，用于陷阱、治疗池和自动找床。");
        comment(config, "worldBorder", "世界边界直径配置，单位为方块。");
        comment(config, ConfigPath.ARENA_Y_LEVEL_KILL, "低于此 Y 坐标时判定为掉入虚空。");
        comment(config, ConfigPath.ARENA_CONFIGURATION_MAX_BUILD_Y, "玩家允许放置方块的最大 Y 坐标。");
        comment(config, ConfigPath.ARENA_DISABLE_GENERATOR_FOR_EMPTY_TEAMS, "空队伍是否停用岛屿资源生成器。");
        comment(config, ConfigPath.ARENA_DISABLE_NPCS_FOR_EMPTY_TEAMS, "空队伍是否不生成商店和升级 NPC。");
        comment(config, ConfigPath.ARENA_NORMAL_DEATH_DROPS, "是否使用原版死亡掉落；关闭时由插件管理资源掉落。");
        comment(config, ConfigPath.ARENA_USE_BED_HOLO, "是否在床上方显示床状态全息文字。");
        comment(config, ConfigPath.ARENA_ALLOW_MAP_BREAK, "是否允许破坏地图原有方块；关闭时通常只能破坏玩家放置的方块。");
        comment(config, ConfigPath.ARENA_GAME_RULES, "进入竞技场时应用的游戏规则，格式为 规则:值。", "竞技场固定为 1000 tick 的白天，并阻止昼夜、天气、火势蔓延、生物自然生成和 Locator Bar。", "Paper 1.21.11 使用 fireSpreadRadiusAroundPlayer:0；旧 doFireTick 会自动删除，昼夜变化、火势蔓延与 Locator Bar 运行时始终强制关闭。");
        comment(config, "waiting.Loc", "等待大厅出生坐标，使用 x.5,y,z.5 的方块中心格式。");
        comment(config, ConfigPath.ARENA_WAITING_FACING, "进入等待大厅时使用的朝向；yaw 自动取最近的 90 度倍数，pitch 固定为 0。");
        comment(config, ConfigPath.ARENA_WAITING_POS1, "开局后移除地图内等待大厅区域的第一个角点。");
        comment(config, ConfigPath.ARENA_WAITING_POS2, "开局后移除地图内等待大厅区域的第二个角点。");
        comment(config, ConfigPath.ARENA_SPEC_LOC, "观战与虚空死亡回退位置。");
        comment(config, ConfigPath.ARENA_SPEC_FACING, "进入观战位置时使用的朝向；yaw 自动取最近的 90 度倍数，pitch 固定为 0。");
        comment(config, "generator.Diamond", "地图中央钻石生成器坐标列表。");
        comment(config, "generator.Emerald", "地图中央绿宝石生成器坐标列表。");
        comment(config, "Team", "队伍配置；每队包含颜色、出生点、床、NPC 和岛屿生成器。");

        ConfigurationSection teams = config.getYml().getConfigurationSection("Team");
        if (teams == null) return;
        for (String team : teams.getKeys(false)) {
            String root = "Team." + team + '.';
            comment(config, root + "Color", "队伍颜色，填写 TeamColor 枚举名称；青色使用 CYAN。", "旧值 AQUA 会自动迁移为 CYAN。");
            comment(config, root + "Spawn", "队伍出生和复活坐标。");
            comment(config, root + ConfigPath.ARENA_TEAM_SPAWN_FACING, "出生和复活时使用的朝向；yaw 自动取最近的 90 度倍数，pitch 固定为 0。");
            comment(config, root + "Bed", "队伍床的坐标；插件也可在设置出生点时自动识别。");
            comment(config, root + "Shop", "商店村民坐标。");
            comment(config, root + ConfigPath.ARENA_TEAM_SHOP_FACING, "商店村民朝向；yaw 自动取最近的 90 度倍数，pitch 固定为 0。");
            comment(config, root + "Upgrade", "升级村民坐标。");
            comment(config, root + ConfigPath.ARENA_TEAM_UPGRADE_FACING, "升级村民朝向；yaw 自动取最近的 90 度倍数，pitch 固定为 0。");
            comment(config, root + ConfigPath.ARENA_TEAM_KILL_DROPS_LOC, "队员最终死亡时资源掉落的位置。");
            comment(config, root + "Iron", "队伍岛屿铁生成器坐标列表。");
            comment(config, root + "Gold", "队伍岛屿金生成器坐标列表。");
            comment(config, root + "Emerald", "队伍岛屿绿宝石生成器坐标列表。");
        }
    }

    public static void generators(ConfigManager config) {
        comment(config, "Default", "默认分组的生成器参数；可以复制本节并将 Default 改为竞技场分组名。", "delay/start 单位为秒，amount 是每次生成数量，spawn-limit 是地面物品上限。");
        comment(config, ConfigPath.GENERATOR_STACK_ITEMS, "是否把同类生成资源合并堆叠，可减少物品实体数量。");
        String root = "Default.";
        comment(config, root + ConfigPath.GENERATOR_IRON_DELAY, "铁资源生成间隔，单位为秒；2.10.7 默认值为 1 秒。");
        comment(config, root + ConfigPath.GENERATOR_IRON_AMOUNT, "每次生成的铁数量。");
        comment(config, root + ConfigPath.GENERATOR_IRON_SPAWN_LIMIT, "单个生成点允许存在的铁物品上限。");
        comment(config, root + ConfigPath.GENERATOR_GOLD_DELAY, "金资源生成间隔，单位为秒；2.10.7 默认值为 4 秒。");
        comment(config, root + ConfigPath.GENERATOR_GOLD_AMOUNT, "每次生成的金数量。");
        comment(config, root + ConfigPath.GENERATOR_GOLD_SPAWN_LIMIT, "单个生成点允许存在的金物品上限。");
        commentTier(config, root, "diamond", "钻石");
        commentTier(config, root, "emerald", "绿宝石");
    }

    private static void commentTier(ConfigManager config, String root, String resource, String label) {
        for (String tier : List.of("tierI", "tierII", "tierIII")) {
            String path = root + resource + "." + tier + '.';
            comment(config, path + "delay", label + " " + tier + " 生成间隔，单位为秒。");
            comment(config, path + "amount", label + " " + tier + " 每次生成数量。");
            comment(config, path + "spawn-limit", label + " " + tier + " 地面物品上限。");
            if (!tier.equals("tierI")) comment(config, path + "start", "本等级在开局后多少秒启用。");
        }
    }

    public static void levels(ConfigManager config) {
        comment(config, "levels", "等级显示名称和升级经验；键支持单个等级或 5-10 形式的范围。", "name 支持 & 颜色代码和 {number} 等级占位符。");
        comment(config, "levels.1.name", "该等级在聊天和计分板中的显示格式。");
        comment(config, "levels.1.rankup-cost", "升到下一级所需经验值。");
        comment(config, "xp-rewards.per-minute", "每游戏分钟获得的经验。");
        comment(config, "xp-rewards.per-teammate", "每名队友带来的经验奖励。");
        comment(config, "xp-rewards.game-win", "获胜经验奖励。");
        comment(config, "xp-rewards.bed-destroyed", "摧毁床的经验奖励。");
        comment(config, "xp-rewards.regular-kill", "普通击杀经验奖励。");
        comment(config, "xp-rewards.final-kill", "最终击杀经验奖励。");
        comment(config, "progress-bar.symbol", "经验进度条使用的字符。");
        comment(config, "progress-bar.unlocked-color", "已完成进度的颜色。");
        comment(config, "progress-bar.locked-color", "未完成进度的颜色。");
        comment(config, "progress-bar.format", "进度条整体格式，{progress} 会替换为进度字符。");
    }

    public static void rewards(ConfigManager config) {
        comment(config, "money-rewards", "Vault 与实际经济服务提供者均可用时发放的金币奖励；只安装 Vault 不会创建余额。");
        comment(config, "money-rewards.per-minute", "每游戏分钟奖励的金币。");
        comment(config, "money-rewards.per-teammate", "每名队友带来的金币奖励。");
        comment(config, "money-rewards.game-win", "游戏获胜金币奖励。");
        comment(config, "money-rewards.bed-destroyed", "摧毁床的金币奖励。");
        comment(config, "money-rewards.final-kill", "最终击杀金币奖励。");
        comment(config, "money-rewards.regular-kill", "普通击杀金币奖励。");
    }

    public static void signs(ConfigManager config) {
        comment(config, "format", "竞技场加入告示牌的四行文本。", "占位符：[arena] [on] [max] [type] [status]。");
        comment(config, ConfigPath.SIGNS_STATUS_BLOCK_WAITING_MATERIAL, "等待状态的告示牌背板 Bukkit Material。");
        comment(config, ConfigPath.SIGNS_STATUS_BLOCK_STARTING_MATERIAL, "倒计时状态的告示牌背板 Bukkit Material。");
        comment(config, ConfigPath.SIGNS_STATUS_BLOCK_PLAYING_MATERIAL, "游戏中状态的告示牌背板 Bukkit Material。");
        comment(config, ConfigPath.SIGNS_STATUS_BLOCK_RESTARTING_MATERIAL, "重置中状态的告示牌背板 Bukkit Material。");
        comment(config, "locations", "已注册告示牌的位置列表，由插件自动维护，不建议手动编辑。");
    }

    public static void sounds(ConfigManager config) {
        for (String path : config.getYml().getKeys(false)) {
            if (path.equals(ConfigManager.CONFIG_VERSION_PATH)) continue;
            comment(config, path, "事件声音 “" + path + "”：sound 为 Bukkit Sound，volume 为音量，pitch 为音调。", "将调用方的声音路径设为 none 可关闭对应声音。");
        }
    }

    public static void upgrades(ConfigManager config) {
        comment(config, "default-upgrades-settings", "升级菜单布局和陷阱规则：menu-content 为 元素,槽位；价格货币支持 iron、gold、diamond、emerald 或 Vault。");
        comment(config, "upgrade-swords", "锋利升级：每个 tier 包含价格、货币、显示物品和 receive 动作。");
        comment(config, "upgrade-armor", "护甲保护升级。");
        comment(config, "upgrade-miner", "挖掘急迫升级。");
        comment(config, "upgrade-forge", "岛屿资源生成器升级。");
        comment(config, "upgrade-heal-pool", "基地治疗池升级。");
        comment(config, "upgrade-dragon", "决战阶段额外末影龙升级。");
        comment(config, "category-traps", "陷阱分类及其菜单槽位。");
        comment(config, "trap-slot-first", "第一个陷阱队列槽位。");
        comment(config, "trap-slot-second", "第二个陷阱队列槽位。");
        comment(config, "trap-slot-third", "第三个陷阱队列槽位。");
        for (int index = 1; index <= 4; index++) {
            comment(config, "base-trap-" + index, "陷阱 " + index + " 的显示物品和触发动作。");
        }
        comment(config, "separator-glass", "升级菜单的装饰分隔物品。");
        comment(config, "separator-back", "返回按钮及玩家/控制台点击命令。");
    }

    public static void shop(ConfigManager config) {
        comment(config, "shop-settings", "商店菜单的快捷购买按钮、空槽和分类分隔物品设置。");
        comment(config, ConfigPath.SHOP_SPECIAL_SILVERFISH_ENABLE, "蠹虫道具：启用状态、材质、生命、伤害、速度和存活秒数。");
        comment(config, ConfigPath.SHOP_SPECIAL_IRON_GOLEM_ENABLE, "铁傀儡道具：启用状态、材质、生命、速度和存活秒数。");
        comment(config, ConfigPath.SHOP_SPECIAL_TOWER_ENABLE, "防御塔道具：启用状态和触发材质。");
        comment(config, ConfigPath.SHOP_QUICK_DEFAULTS_PATH, "新玩家快捷购买栏；path 指向商品，slot 是菜单槽位。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_BLOCKS, "方块分类。分类含菜单槽位、显示物品和 category-content 商品。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_MELEE, "近战武器分类。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_ARMOR,
                "永久护甲分类；商店固定只出售护腿和靴子，保留本队颜色的皮革头盔与胸甲。",
                "weight 用于阻止购买更低等级护甲；旧版上半身商品会自动删除。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_TOOLS, "工具分类；permanent/downgradable 控制永久保留和死亡降级。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_RANGED, "弓箭分类。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_POTIONS, "药水分类；potion 格式为 效果,持续秒数,等级。");
        comment(config, ConfigPath.SHOP_PATH_CATEGORY_UTILITY, "实用道具分类，包括 TNT、火球、珍珠、搭桥蛋和袖珍弹出塔。", "袖珍弹出塔的稳定商品 ID 为 tower；显示名称与说明在语言文件的对应 tower 路径中修改。");
    }

    private static void comment(ConfigManager config, String path, String... lines) {
        config.setComments(path, lines);
    }
}
