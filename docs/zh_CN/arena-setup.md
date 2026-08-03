# 从零创建竞技场

## 1. 准备世界

- 世界文件夹名称只能使用小写字符，不能包含 `+`。
- 地图中至少准备两支队伍的岛屿、床和安全出生空间。
- 每个队伍出生点附近应有且只有一张目标床，便于自动识别。
- 在地图中预留等待大厅，或者只设置等待出生点而不配置移除区域。

将世界文件夹放到服务端世界容器目录，确保 Paper 能读取它。

## 2. 设置主大厅

MULTIARENA 首次使用时，在希望玩家出现的位置面向目标方向执行：

```text
/bw setLobby
```

主大厅坐标会保存世界、精确坐标和朝向。yaw 自动取最近的 90 度倍数，pitch 固定为 0；玩家重新进入大厅时会恢复该朝向并切换到冒险模式。

## 3. 开始设置会话

```text
/bw setupArena <世界名>
```

选择“引导式设置”适合首次创建；“高级设置”会展示所有项目。设置会话期间可随时执行 `/bw` 或 `/bw cmds` 查看尚未完成的步骤。

设置会话中的管理员可以在当前目标地图内正常放置方块，以便补建岛屿、床台和 NPC 区域。该权限只在本次设置的世界生效，不会放开大厅、其他竞技场或正常游戏中的方块保护。

## 4. 设置等待区域

站在地图内等待点并执行：

```text
/bw setWaitingSpawn
```

如需开局后移除等待大厅，分别站在区域两个对角执行：

```text
/bw waitingPos 1
/bw waitingPos 2
```

设置观战位置：

```text
/bw setSpectSpawn
```

等待点、观战点会保存方块中心坐标和单独朝向。

## 5. 创建队伍

快速自动创建常用队伍：

```text
/bw autoCreateTeams
```

手动创建：

```text
/bw createTeam <名称> <颜色>
```

青色队伍请使用 `CYAN`。插件会从 `CYAN_WOOL` 自动识别青色队伍，旧竞技场配置中的 `AQUA` 会在启动时自动迁移为 `CYAN`；`LIGHT_BLUE_WOOL` 不再被误认为青色。

自动建队严格按羊毛材质一对一识别，并要求目标羊毛周围的 `5×5×5` 范围内至少存在 5 块同色羊毛，孤立的装饰羊毛不会创建队伍：

| 羊毛材质 | 创建的队伍颜色 |
| --- | --- |
| `RED_WOOL` | `RED` |
| `BLUE_WOOL` | `BLUE` |
| `LIME_WOOL` | `GREEN` |
| `GREEN_WOOL` | `DARK_GREEN` |
| `YELLOW_WOOL` | `YELLOW` |
| `CYAN_WOOL` | `CYAN` |
| `WHITE_WOOL` | `WHITE` |
| `PINK_WOOL` | `PINK` |
| `LIGHT_GRAY_WOOL` | `GRAY` |
| `GRAY_WOOL` | `DARK_GRAY` |

这里不会使用名称包含判断，因此 `GREEN_WOOL` 与 `LIME_WOOL`、`GRAY_WOOL` 与 `LIGHT_GRAY_WOOL` 始终是不同队伍颜色。

查看当前队伍：

```text
/bw listTeams
```

删除队伍：

```text
/bw removeTeam <名称>
```

设置每队最大人数：

```text
/bw setMaxInTeam <人数>
```

高级和引导式快速设置都会显示此命令。自动创建或手动新增队伍不会覆盖管理员设置；`setType` 会写入所选类型的标准队伍容量，随后仍可用本命令覆盖。修改容量不会覆盖全场最低开局人数。

设置全场最低开局人数：

```text
/bw setMinPlayers <人数>
```

范围为 `2..地图总容量`。例如设置 `minPlayers: 2`、`maxInTeam: 4` 后，两名未组队玩家可分到两支 1 人队开始游戏；如果两人组成同一小队，则会等待对手而不会强拆小队。旧 `/bw setMinInTeam` 仅作为兼容别名保留，并会提示改用新命令。

## 6. 设置每支队伍

以下命令中的 `<队伍>` 使用创建时的名称：

```text
/bw setSpawn <队伍>
/bw setBed <队伍>
/bw setShop <队伍>
/bw setUpgrade <队伍>
/bw setKillDrops <队伍>
```

说明：

- `setSpawn` 同时定义首次出生和复活位置，并保存玩家朝向；yaw 自动取最近的 90 度倍数，pitch 固定为 0。
- 引导式快速模式在设置出生点及最终保存时，会在岛屿半径内自动寻找床；找不到时再使用 `setBed`。高级模式始终要求手动设置床位，插件只校验而不会自动改写。
- 商店和升级村民保存独立 yaw，设置时请大致正对希望村民面向的方向；插件会自动吸附到最近的 90 度倍数，pitch 固定为 0。游戏中 NPC 的位置、身体和视线方向会持续锁定，不会移动或朝玩家扭头。
- `setKillDrops` 是最终死亡后资源集中掉落位置。
- 出生点被遮挡时，插件会搜索附近安全位置；一格高空间会使用趴下姿态。

## 7. 添加生成器

站在生成位置执行：

```text
/bw addGenerator <Iron|Gold|Diamond|Emerald> [队伍]
```

移除错误标点：

```text
/bw removeGenerator <Iron|Gold|Diamond|Emerald> [队伍]
```

铁、金和岛屿绿宝石通常属于队伍；钻石和中央绿宝石通常是全局生成器。具体速度在 `plugins/SimpMC-BedWars/generators.yml` 调整：`delay` 越小刷新越快，`amount` 是每次产出数量。竞技场存在同名分组配置时会优先使用分组值。

钻石和绿宝石生成点不再自动扫描地图结构。请站在实际生成位置使用 `addGenerator` 明确添加；坐标会保存为所在方块中心，即 `x.5, y.0, z.5`，yaw/pitch 固定为 0。`/bw save` 只保存当前设置，不会隐式增加生成点。

## 8. 其他参数

```text
/bw setMaxBuildHeight <Y>
/bw setType <Solo|Doubles|3v3v3v3|4v4v4v4>
```

`setType` 会设置竞技场分组及对应的每队容量，但不会修改 `minPlayers`；需要调整全场开局门槛时使用 `/bw setMinPlayers`。世界边界、虚空高度和保护半径可在保存后编辑 `Arenas/<世界名>.yml`。竞技场初始化时执行原版 `/time set noon` 的等价 API 操作，固定为正午 6000 tick，关闭 Paper 1.21.11 的 `advance_time`、`advance_weather` 和随机方块刻；后续命令、插件、睡眠、游戏规则修改、下雨与雷暴变化均由事件守卫恢复固定状态，不使用周期遍历。

## 9. 保存并启用

```text
/bw save
/bw enableArena <世界名>
```

保存时插件会再次自动检查所有队伍的床，并阻止保存缺少铁或金生成器的队伍，但不会扫描钻石或绿宝石生成器。若提示找不到床，应移动出生点、调整 `island-radius`，或手动执行 `setBed`；若提示缺少队伍生成器，请在对应资源点执行 `addGenerator`。中央生成器仍需使用 `addGenerator` 手动添加。

修改已启用竞技场前，先执行：

```text
/bw disableArena <世界名>
/bw setupArena <世界名>
```

## 10. 验收清单

- 两名玩家能加入并分到两支非空队伍。
- 出生和复活方向正确，遮挡时不会窒息。
- 每队床、商店、升级和生成器正确。
- 普通死亡原地观战，虚空死亡回到队伍出生点。
- 商店 NPC、床和升级 NPC 不显示血量。
- 游戏开始会直接清空本局背包、装备栏和末影箱，再发放默认物品。
- 游戏结束后地图恢复，玩家返回大厅并得到大厅物品。
- 所有大厅、设置世界和竞技场世界的 Locator Bar 均保持关闭；插件启动、世界初始化、世界加载和竞技场初始化都会再次强制应用。
