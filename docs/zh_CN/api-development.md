# API 与附属插件开发

## 引入 API

Maven：

```xml
<dependency>
    <groupId>com.simpmc.bedwars</groupId>
    <artifactId>simpmc-bedwars-api</artifactId>
    <version>2.13.1</version>
    <scope>provided</scope>
</dependency>
```

附属插件的 `plugin.yml` 应声明：

```yaml
depend: [SimpMC-BedWars]
```

## 获取服务

```java
BedWars api = Bukkit.getServicesManager()
        .load(com.andrei1058.bedwars.api.BedWars.class);
if (api == null) {
    throw new IllegalStateException("SimpMC-BedWars API 未注册");
}
```

不要强制转换为插件内部的 `com.andrei1058.bedwars.API`。

## 安全查询竞技场

```java
api.getArenaUtil().findArenaByPlayer(player).ifPresent(arena -> {
    player.sendMessage("当前地图：" + arena.getDisplayName());
});

for (IArena arena : api.getArenaUtil().getArenasSnapshot()) {
    // 这是不可变快照，不会修改插件内部注册表。
}
```

旧版 `getArenas()` 为兼容保留，但会暴露内部可变集合；新代码应使用 `getArenasSnapshot()`。

游戏进入 `playing` 后，可以区分全部配置队伍与本局实际参战队伍：

```java
List<ITeam> configuredTeams = arena.getTeams();
List<ITeam> participatingTeams = arena.getActiveTeamsAtGameStart();
ITeam firstTeam = participatingTeams.getFirst();
int playersAtStart = arena.getTeamSizeAtGameStart(firstTeam);
```

`getActiveTeamsAtGameStart()` 与 `getTeamSizeAtGameStart(ITeam)` 都是开局瞬间的快照，因此队伍后续被淘汰、玩家掉线或重连不会改变结果。第三方 `IArena` 实现若不覆盖这些方法，会分别返回全部配置队伍和队伍当前人数，以保持二进制兼容。

`IArena#getMinInTeam()` 返回每支启用队伍正常开局所需的最少人数，`getMaxInTeam()` 返回容量。只要分配器能够组成至少两支人数位于该范围内的队伍即可开局，不要求达到 `getMaxPlayers()`；未被本局选中的配置队伍可以为空。`/bw start debug` 仍可绕过双队限制。第三方旧实现无需立即修改，接口默认返回 `1`。

2.10.47 起竞技场可以属于多个匹配分组：

```java
String primaryGroup = arena.getGroup();       // 主组，用于组专属玩法配置
List<String> groups = arena.getGroups();      // 全部分组的不可变快照
boolean featured = arena.isInGroup("Featured");
arena.setGroups(List.of("Solo", "Featured"));
```

列表第一项是主组。旧版第三方 `IArena` 实现无需修改：默认 `getGroups()` 会返回只包含 `getGroup()` 的列表，默认 `setGroups()` 会把第一项交给旧 `setGroup()`。PlaceholderAPI 的 `%bw1058_arena_groups_<竞技场>%` 和 `%bw1058_current_arena_groups%` 使用英文逗号连接全部组；原单数占位符继续只返回主组。

## 开局邀请组队 API

```java
PreGameSquad squads = api.getPreGameSquadUtil();
PreGameSquad.Result result = squads.invite(inviter, target);

if (result == PreGameSquad.Result.SUCCESS) {
    inviter.sendMessage("邀请已创建");
}

List<Player> members = squads.getMembers(inviter); // 不可变快照
Player leader = squads.getLeader(inviter);
```

邀请仅在同一竞技场的 waiting/starting 阶段有效，默认 30 秒过期，人数不能超过 `maxInTeam`。游戏开始、竞技场重置、玩家退出或竞技场禁用时会自动清理。

## 配置 API

```java
ConfigManager main = api.getConfigs().getMainConfig();
ConfigManager generators = api.getConfigs().getGeneratorsConfig();
ConfigManager shop = api.getConfigs().getShopConfig();
ConfigManager upgrades = api.getConfigs().getUpgradesConfig();
ConfigManager levels = api.getConfigs().getLevelsConfig();
ConfigManager rewards = api.getConfigs().getRewardsConfig();
ConfigManager sounds = api.getConfigs().getSoundsConfig();
```

这些对象在 SimpMC-BedWars 完成 `onEnable` 后可用。写配置时使用 `set`/`save`，不要直接操作磁盘上的 YAML。

需要在世界尚未加载时读取通用坐标中的世界名，可使用：

```java
String worldName = ConfigManager.getWorldNameFromConfigLocation(
        main.getYml().getString("lobbyLoc"));
```

该方法只解析配置文本，不调用 `Bukkit#getWorld`，适合插件加载阶段和世界加载器使用；无效或缺少世界名时抛出 `IllegalArgumentException`。

2.10.44 起可通过以下 API 使用与插件一致的方向规则：

```java
float yaw = ConfigManager.snapYawToCardinal(-88.328); // -90.0
String stored = ConfigManager.serializeConfigLocation(location);
```

`serializeConfigLocation` 会保留坐标和世界，把 yaw 吸附到最近的 90 度倍数，并把 pitch 固定为 0。`SubCommand#getTabComplete(CommandSender)` 可生成与发送者或设置会话相关的动态补全；旧扩展只实现无参方法仍保持兼容。

## 大厅状态

```java
api.getArenaUtil().applyLobbyState(player);
```

该方法应用冒险模式、飞行/拾取状态和大厅物品。调用前应确保玩家已经成功传送到配置的大厅世界。仅刷新物品可使用兼容方法 `sendLobbyCommandItems`。

## 常用事件

- `PlayerJoinArenaEvent` / `PlayerLeaveArenaEvent`
- `GameStateChangeEvent` / `GameEndEvent`
- `TeamAssignEvent`
- `PlayerFirstSpawnEvent` / `PlayerReSpawnEvent`
- `PlayerBedBreakEvent` / `PlayerKillEvent`
- `ShopBuyEvent` / `UpgradeBuyEvent`（商店事件的 `getPurchaseCount()` 可区分普通单次购买与 Shift+右键批量购买）
- `ArenaEnableEvent` / `ArenaDisableEvent`

示例：

```java
@EventHandler
public void onState(GameStateChangeEvent event) {
    if (event.getNewState() == GameState.playing) {
        getLogger().info(event.getArena().getArenaName() + " 已开始");
    }
}
```

## 自定义队伍分配器

`IArena#setTeamAssigner(ITeamAssigner)` 可替换单个竞技场的分配逻辑。实现必须遵守：

- 不超过 `arena.getMaxInTeam()`。
- 普通开局至少产生两支非空队伍，否则开局会被阻止。管理员执行 `/bw start debug` 时，`arena.getStartingTask().isSingleTeamDebugStart()` 返回 `true`，此时允许恰好一支非空队伍，但仍不得产生零队伍。
- 分配前调用 `TeamAssignEvent` 并尊重取消结果。
- 只在主线程修改 Bukkit 玩家和队伍。

## 队伍颜色 API

青色队伍使用 `TeamColor.CYAN`。读取管理员配置时建议调用 `TeamColor.fromName(value)`，它会把 2.10.11 以前的旧值 `AQUA` 安全规范为 `CYAN`。列出可创建颜色时使用 `TeamColor.selectableValues()`，避免展示为附属插件二进制兼容而保留的弃用别名 `AQUA`。

`TeamColor.CYAN.woolMaterial()` 返回 `Material.CYAN_WOOL`；Minecraft 没有独立的青色聊天代码，因此 `chat()` 仍返回最接近的 `ChatColor.AQUA`。

需要从地图方块识别队伍颜色时，使用 `TeamColor.fromWool(material)`。该方法只接受精确支持的羊毛材质，不做前缀、子字符串或近似颜色匹配；例如 `LIME_WOOL` 返回 `GREEN`，`GREEN_WOOL` 返回 `DARK_GREEN`，`LIGHT_GRAY_WOOL` 返回 `GRAY`，`GRAY_WOOL` 返回 `DARK_GRAY`。不支持的羊毛或非羊毛材质返回 `null`。`setupName()` 返回自动创建队伍时使用的稳定英文名称。

## 玩家放置方块 API

附属插件通过代码生成可破坏方块时，必须调用 `arena.addPlacedBlock(block)`；方块被插件移除时调用 `arena.removePlacedBlock(block)`。读取状态可使用：

```java
Set<Vector> placedBlocks = arena.getPlacedBlocksSnapshot();
```

返回值是与竞技场内部索引分离的只读快照。旧方法 `getPlaced()` 已弃用，修改其返回列表不会再改变竞技场状态；这样可以避免附属插件误把地图原生方块标记成玩家方块。所有 Bukkit 方块操作和所有权更新都应在主线程完成。

## API 兼容原则

- 只依赖 `simpmc-bedwars-api`，不要导入 `bedwars-plugin` 内部类。
- 对可能不存在的竞技场使用 `Optional` 查询。
- 不要修改旧版暴露的内部集合。
- Bukkit 实体操作在主线程执行。
- 监听竞技场禁用和玩家离开事件，及时清理自己的缓存。
