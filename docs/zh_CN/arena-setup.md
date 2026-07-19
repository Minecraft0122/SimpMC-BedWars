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

主大厅坐标会保存世界、精确坐标、yaw 和 pitch。玩家重新进入大厅时会恢复该朝向并切换到冒险模式。

## 3. 开始设置会话

```text
/bw setupArena <世界名>
```

选择“引导式设置”适合首次创建；“高级设置”会展示所有项目。设置会话期间可随时执行 `/bw` 或 `/bw cmds` 查看尚未完成的步骤。

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

插件固定在至少两名玩家时开始倒计时，并在正式开局前再次确认至少两支非空队伍。旧配置中的 `minPlayers` 会自动删除。

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

- `setSpawn` 同时定义首次出生和复活位置，并保存玩家朝向。
- 设置出生点后插件会在岛屿半径内自动寻找床；找不到时再使用 `setBed`。
- 商店和升级村民保存独立 yaw，设置时请正对希望村民面向的方向。
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

铁、金和岛屿绿宝石通常属于队伍；钻石和中央绿宝石通常是全局生成器。具体速度在 `generators.yml` 调整。

## 8. 其他参数

```text
/bw setMaxBuildHeight <Y>
/bw setType <Solo|Doubles|3v3v3v3|4v4v4v4>
```

`setType` 会设置竞技场分组及对应的每队人数。最少开局人数、世界边界、虚空高度、保护半径和游戏规则可在保存后编辑 `Arenas/<世界名>.yml`。

## 9. 保存并启用

```text
/bw save
/bw enableArena <世界名>
```

保存时插件会再次自动检查所有队伍的床。若提示找不到床，应移动出生点、调整 `island-radius`，或手动执行 `setBed`。

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
