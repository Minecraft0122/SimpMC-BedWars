# 命令与权限

主命令别名：`/bw`、`/bedwars`、`/simpmcbedwars`、`/simpmcbw`。

从 2.10.28 起，命令的默认权限行为与 [BedWars1058 官方权限表](https://wiki.andrei1058.com/docs/BedWars1058/configuration/permissions/) 一致。官方标记为无需权限的命令不需要给默认组授予任何节点；`bw.command.<命令>`、`bw.command.*` 和 `bw.player` 仅作为旧版 SimpMC-BedWars 配置的兼容节点保留。

## 玩家命令

| 命令 | 官方权限 | 作用 |
| --- | --- | --- |
| `/bw`、`/bw cmds` | 无 | 查看命令帮助 |
| `/bw join <竞技场/分组/random>` | 无 | 加入竞技场 |
| `/bw leave`、`/leave` | 无 | 离开当前竞技场 |
| `/bw gui [分组+分组]` | 无 | 打开竞技场选择菜单 |
| `/bw stats` | 无 | 查看历史战绩 |
| `/bw lang <语言代码>` | 无 | 切换个人语言 |
| `/bw team ...` | 无 | 管理开局前的固定队友邀请 |
| `/bw invite ...` | 无 | 邀请大厅玩家加入等待中的竞技场 |
| `/bw teleporter` | 无 | 打开旁观传送菜单 |
| `/bw upgradesmenu` | 无 | 打开本队升级菜单；主要由 NPC 调用 |
| `/bw arenaList` | 无 | 查看竞技场列表 |
| `/party ...` | 无 | 使用插件内置组队命令 |
| `/shout <消息>`、`!消息` | `bw.shout` | 游戏内全体喊话；两种入口使用相同权限检查 |
| `/rejoin` | `bw.rejoin` | 在有效窗口内重连 |

`bw.player` 不再是普通玩家使用基础命令的前置条件，也不会授予 `bw.shout` 或 `bw.rejoin`。

## 管理命令

| 命令 | 官方权限 | 兼容的细粒度/旧权限 |
| --- | --- | --- |
| `/bw start`、`/bw forceStart` | `bw.forcestart` | `bw.command.start`、`bw.command.forcestart` |
| `/bw start debug` | 仅 OP | 允许本局以单支非空队伍开始；不启用详细日志。其他权限（包括 `bw.*`）不能代替 OP 状态 |
| `/bw setLobby` | `bw.setup` | `bw.command.setlobby` |
| `/bw setupArena <世界>` | `bw.setup` | `bw.command.setuparena` |
| `/bw delArena <世界>` | `bw.delete` | `bw.command.delarena` |
| `/bw enableArena <世界>` | `bw.enable` | `bw.command.enablearena`、旧别名 `bw.enableRotation` |
| `/bw disableArena <世界>` | `bw.disable` | `bw.command.disablearena` |
| `/bw cloneArena <源> <目标>` | `bw.clone` | `bw.command.clonearena` |
| `/bw arenaGroup ...` | `bw.groups` | `bw.command.arenagroup` |
| `/bw build` | `bw.build` | `bw.command.build` |
| `/bw level ...` | `bw.level` | `bw.command.level` |
| `/bw reload` | `bw.reload` | `bw.command.reload` |
| `/bw npc ...` | `bw.npc` | `bw.command.npc` |
| `/bw tp ...` | `bw.tp` | `bw.command.tp` |

官方 Wiki 曾把启用竞技场写成 `bw.enable`，而旧源码使用 `bw.enableRotation`。本插件以文档节点 `bw.enable` 为主，同时接受旧节点，方便现有 LuckPerms 数据平滑迁移。

## 竞技场设置命令

设置会话中的命令统一使用官方权限 `bw.setup`。以下细粒度节点继续兼容，但不是必需配置：

| 命令 | 兼容的细粒度权限 |
| --- | --- |
| `autoCreateTeams` | `bw.command.autocreateteams` |
| `setWaitingSpawn` | `bw.command.setwaitingspawn` |
| `setSpectSpawn` | `bw.command.setspectspawn` |
| `createTeam` | `bw.command.createteam` |
| `listTeams` | `bw.command.listteams` |
| `waitingPos` | `bw.command.waitingpos` |
| `removeTeam` | `bw.command.removeteam` |
| `setMaxInTeam` | `bw.command.setmaxinteam` |
| `setMinInTeam` | `bw.command.setmininteam` |
| `setMaxBuildHeight` | `bw.command.setmaxbuildheight` |
| `setSpawn` | `bw.command.setspawn` |
| `setBed` | `bw.command.setbed` |
| `setShop` | `bw.command.setshop` |
| `setUpgrade` | `bw.command.setupgrade` |
| `autoDetectGenerators` | `bw.command.autodetectgenerators` |
| `addGenerator` | `bw.command.addgenerator` |
| `removeGenerator` | `bw.command.removegenerator` |
| `setType` | `bw.command.settype` |
| `save` | `bw.command.save` |
| `setKillDrops` | `bw.command.setkilldrops` |

## 聚合及其他权限

| 权限 | 作用 |
| --- | --- |
| `bw.*` | 官方全命令权限；默认 OP 拥有，但不能让非 OP 使用 `/bw start debug` |
| `bw.command.*` | SimpMC-BedWars 兼容的细粒度全命令集合，不包含调试开局 |
| `bw.player` | 旧版普通玩家集合；基础命令现已无需此权限 |
| `bw.vip` | VIP 满服加入和相关特权 |
| `bw.chatcolor` | 允许在聊天消息中使用 `&` 颜色代码 |
| `bw.cmd.bypass` | 绕过游戏内命令限制 |
| `bw.shout.bypass` | 绕过喊话冷却；仍然必须拥有 `bw.shout` 或兼容命令权限 |

不要把 `bw.*` 或 `bw.command.*` 授予普通玩家。通常只需按需授予 `bw.shout`、`bw.rejoin` 等官方节点。
