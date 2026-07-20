# 命令与权限

主命令别名：`/bw`、`/bedwars`、`/simpmcbedwars`、`/simpmcbw`。

从 2.10.17 起，每个一级命令都有稳定权限点，格式为 `bw.command.<小写命令名>`。权限默认不向普通玩家开放，建议用 LuckPerms 给默认组授予玩家权限集合：

```text
/lp group default permission set bw.player true
```

`bw.player` 只包含常用玩家命令，不包含强制开局、竞技场设置、重载、删除地图等管理能力。代码会直接识别该权限，不依赖 LuckPerms 是否启用 Bukkit 子权限继承。

## 玩家命令

| 命令 | 独立权限 | 作用 |
| --- | --- | --- |
| `/bw` | `bw.command.help` | 查看有权使用的命令 |
| `/bw cmds` | `bw.command.cmds` | 查看玩家命令帮助 |
| `/bw join <竞技场/分组/random>` | `bw.command.join` | 加入竞技场 |
| `/bw leave`、`/leave` | `bw.command.leave` | 离开当前竞技场 |
| `/bw gui [分组+分组]` | `bw.command.gui` | 打开竞技场选择菜单 |
| `/bw stats` | `bw.command.stats` | 查看历史战绩 |
| `/bw lang <语言代码>` | `bw.command.lang` | 切换个人语言 |
| `/bw team ...` | `bw.command.team` | 管理开局前的固定队友邀请 |
| `/bw invite ...` | `bw.command.invite` | 邀请大厅玩家加入等待中的竞技场 |
| `/bw teleporter` | `bw.command.teleporter` | 打开旁观传送菜单 |
| `/bw upgradesmenu` | `bw.command.upgradesmenu` | 打开本队升级菜单；主要由 NPC 调用 |
| `/shout <消息>` | `bw.command.shout` | 游戏内全体喊话 |
| `/rejoin` | `bw.command.rejoin` | 在有效窗口内重连 |
| `/party ...` | `bw.command.party` | 使用插件内置组队命令 |

以上权限全部包含在 `bw.player` 中。

## 管理命令

| 命令 | 独立权限 | 兼容的旧权限 |
| --- | --- | --- |
| `/bw start` | `bw.command.start` | `bw.forcestart` |
| `/bw start debug` | `bw.command.start` + `bw.command.start.debug` | 仅 OP 或明确授权的管理员可绕过单队限制 |
| `/bw forceStart` | `bw.command.forcestart` | `bw.forcestart` |
| `/bw setLobby` | `bw.command.setlobby` | `bw.setup` |
| `/bw setupArena <世界>` | `bw.command.setuparena` | `bw.setup` |
| `/bw arenaList` | `bw.command.arenalist` | 无 |
| `/bw delArena <世界>` | `bw.command.delarena` | `bw.delete` |
| `/bw enableArena <世界>` | `bw.command.enablearena` | `bw.enableRotation` |
| `/bw disableArena <世界>` | `bw.command.disablearena` | `bw.disable` |
| `/bw cloneArena <源> <目标>` | `bw.command.clonearena` | `bw.clone` |
| `/bw arenaGroup ...` | `bw.command.arenagroup` | `bw.groups` |
| `/bw build` | `bw.command.build` | `bw.build` |
| `/bw level ...` | `bw.command.level` | `bw.level` |
| `/bw reload` | `bw.command.reload` | `bw.reload` |
| `/bw npc ...` | `bw.command.npc` | `bw.npc` |
| `/bw tp ...` | `bw.command.tp` | `bw.tp` |

## 竞技场设置命令

设置会话中的命令仍兼容旧权限 `bw.setup`，同时各自拥有独立节点：

| 命令 | 独立权限 |
| --- | --- |
| `autoCreateTeams` | `bw.command.autocreateteams` |
| `setWaitingSpawn` | `bw.command.setwaitingspawn` |
| `setSpectSpawn` | `bw.command.setspectspawn` |
| `createTeam` | `bw.command.createteam` |
| `listTeams` | `bw.command.listteams` |
| `waitingPos` | `bw.command.waitingpos` |
| `removeTeam` | `bw.command.removeteam` |
| `setMaxInTeam` | `bw.command.setmaxinteam` |
| `setMaxBuildHeight` | `bw.command.setmaxbuildheight` |
| `setSpawn` | `bw.command.setspawn` |
| `setBed` | `bw.command.setbed` |
| `setShop` | `bw.command.setshop` |
| `setUpgrade` | `bw.command.setupgrade` |
| `addGenerator` | `bw.command.addgenerator` |
| `removeGenerator` | `bw.command.removegenerator` |
| `setType` | `bw.command.settype` |
| `save` | `bw.command.save` |
| `setKillDrops` | `bw.command.setkilldrops` |

## 聚合及其他权限

| 权限 | 作用 |
| --- | --- |
| `bw.player` | 常用玩家命令集合，推荐授予默认玩家组 |
| `bw.command.*` | 所有命令，但不包含聊天颜色、VIP 等非命令特权 |
| `bw.*` | 插件全部权限；默认仅 OP 拥有 |
| `bw.vip` | VIP 满服加入和相关特权 |
| `bw.chatcolor` | 允许在聊天消息中使用 `&` 颜色代码 |
| `bw.shout` | 旧版 `/shout` 权限，继续兼容 |
| `bw.rejoin` | 旧版重连权限，继续兼容 |
| `bw.cmd.bypass` | 绕过游戏内命令限制 |

不要把 `bw.*` 或 `bw.command.*` 授予普通玩家。若只希望开放个别功能，可以直接授予对应的 `bw.command.<命令>`。
