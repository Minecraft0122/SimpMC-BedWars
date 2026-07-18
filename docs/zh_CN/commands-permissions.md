# 命令与权限

主命令别名：`/bw`、`/bedwars`、`/simpmcbedwars`、`/simpmcbw`。

## 玩家命令

| 命令 | 作用 |
| --- | --- |
| `/bw join <竞技场/分组/random>` | 加入竞技场 |
| `/bw leave` | 离开当前竞技场 |
| `/bw gui [分组+分组]` | 打开竞技场选择菜单 |
| `/bw stats [玩家]` | 查看历史战绩 |
| `/bw lang <语言代码>` | 切换个人语言 |
| `/bw team list` | 查看开局邀请队伍 |
| `/bw team invite <玩家>` | 邀请同一竞技场的玩家 |
| `/bw team accept <玩家>` | 接受邀请 |
| `/bw team decline <玩家>` | 拒绝邀请 |
| `/bw team leave` | 退出开局队伍 |
| `/shout <消息>` | 游戏内全体喊话 |
| `/rejoin` | 在有效窗口内重连 |

## 管理命令

| 命令 | 权限 | 作用 |
| --- | --- | --- |
| `/bw setLobby` | `bw.setup` | 设置主大厅 |
| `/bw setupArena <世界>` | `bw.setup` | 创建/编辑竞技场 |
| `/bw arenaList` | 管理员命令列表 | 查看竞技场 |
| `/bw enableArena <世界>` | `bw.enableRotation` | 启用竞技场 |
| `/bw disableArena <世界>` | `bw.disable` | 禁用竞技场 |
| `/bw delArena <世界>` | `bw.delete` | 删除竞技场配置 |
| `/bw cloneArena <源> <目标>` | `bw.clone` | 克隆竞技场 |
| `/bw arenaGroup ...` | `bw.groups` | 管理竞技场分组 |
| `/bw build` | `bw.build` | 切换大厅建造会话 |
| `/bw npc ...` | `bw.npc` | 管理 Citizens 加入 NPC |
| `/bw reload` | `bw.reload` | 重读部分配置；生产环境仍建议重启 |
| `/bw start` | `bw.forcestart` | 强制开始当前竞技场 |
| `/bw level ...` | `bw.level` | 管理玩家等级 |
| `/bw tp ...` | `bw.tp` | 管理员传送 |

竞技场设置会话内的 `setWaitingSpawn`、`createTeam`、`listTeams`、`setSpawn`、`setBed`、`setShop`、`setUpgrade`、生成器和保存命令统一要求 `bw.setup`。

## 其他权限

| 权限 | 作用 |
| --- | --- |
| `bw.*` | BedWars 全部权限 |
| `bw.vip` | VIP 满服加入和相关特权 |
| `bw.chatcolor` | 允许在聊天消息中使用 `&` 颜色代码 |
| `bw.shout` | 使用 `/shout` |
| `bw.rejoin` | 使用重连命令 |
| `bw.cmd.bypass` | 绕过游戏内命令限制 |

权限节点来自当前代码；如果使用权限插件，应只授予实际需要的管理权限，不建议普通玩家拥有 `bw.*`。
