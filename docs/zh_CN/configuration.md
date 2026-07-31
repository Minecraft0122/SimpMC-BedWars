# 全部配置文件说明

插件会把管理员可编辑的 YAML 放在 `plugins/SimpMC-BedWars`。`config-version` 由迁移器管理，不要手动降低。新版本会自动备份、补字段并写入中文注释。

`config-version` 是各配置文件的内部架构字段，不是商店分类、升级项或竞技场内容。2.10.16 起各加载器会主动忽略该字段。

2.10.50 起，自动迁移会以新版默认配置为结构模板：`config-version` 放在顶部，已知键按新版顺序放回对应配置节，不再统一追加到文件末尾。管理员或附属插件增加的未知键不会被删除，会留在所属配置节的已知键之后，其相对顺序、配置值和注释均保留。每次架构升级前仍会先生成 `.bak` 备份。

## config.yml

主配置，包含：

- `serverType`：MULTIARENA、SHARED 或 BUNGEE。
- `debug`：详细故障日志开关，默认 `false`；生产环境保持关闭。`/bw start debug` 仅表示单队测试开局，不会修改此项或临时开启日志。
- `lobbyServer`：BungeeCord/Velocity 代理 `[servers]` 中的大厅服务器键名，默认 `hub`；不是 IP、端口或 MotD。主大厅红床会立即静默发送 `Connect`，不查询或向玩家展示代理节点、服务器列表和故障信息；配置问题只通过后端及代理日志排查。
- `arenaGroups`：全局可用的匹配组名称；`Default` 是内置组，无需写入。竞技场自己的 `groups` 列表可以同时引用多个这里声明的组。
- `language`、禁用语言列表：默认语言及玩家可选语言。
- `chat`：全局聊天和插件聊天格式。
- `scoreboard-settings`：大厅/游戏计分板、TAB、队伍颜色、血量和刷新周期。
- `rejoin-time`：掉线重连窗口，默认 30 秒。
- `countdowns`：开局、床消失、决战、结束和复活倒计时。
- `party-settings`：内部及外部组队集成。
- `tnt-jump-settings`、`blast-protection`、`tnt-prime-settings`：TNT 参数。
- `fireball`：火球速度、爆炸、击退、冷却和伤害。普通 `speed-multiplier` 默认从 10 提高到 11，使火球飞得稍远；`sneak-speed-multiplier: 1.25` 让潜行投掷额外加速，`sneak-recoil: 0.05` 给发射者约半格的反向水平后坐力，代码硬限制最大为 0.08。该上限按原版空气阻力的低摩擦情况估算，单次效果不足一格，也不需要逐 tick 跟踪玩家。2.10.20 的平衡默认值仍为范围 3.25、水平击退 1.15、垂直击退 0.75、敌方伤害 3.5。`make-fire` 只决定爆炸处是否生成火焰，竞技场不会允许火势向周围蔓延。
- `database`：MySQL；关闭时使用 SQLite。
- `performance-settings`：Paper 传送、资源旋转等优化。
- `lobby-items`、`pre-game-items`、`spectator-items`：不同阶段的命令物品。主大厅默认提供历史战绩、竞技场选择器和第 9 格的“回到主大厅”红床；大厅红床带有独立目标标记，固定连接代理配置中的 `lobbyServer`，MULTIARENA 模式也会执行代理切服，不传送到本服 `/bw setLobby` 坐标。等待区和观战区红床使用另一目标标记，直接返回本服 BedWars 大厅，不经过命令权限。管理员可以修改显示材质和命令文本，内置 `leave` 项的返回语义仍由其配置节点名确定。完整代理示例见[安装文档](installation.md#bungee)。
- 大厅进入/离开提示只向同样位于 BedWars 大厅的玩家发送；竞技场、观战者和地图设置会话不会收到。大厅世界名直接从 `lobbyLoc` 文本读取，即使该世界在插件加载时尚未加载也能正确识别。大厅和加入 NPC 的旧朝向会自动迁移为最近的 90 度 yaw，pitch 固定为 0。
- 竞技场加入通知只在目标竞技场处于等待或开局倒计时发送，接收者为该竞技场与大厅玩家；已开局和重置阶段不播报。玩家在广播前即从大厅受众移除，不会因异步传送时序重复收到大厅身份消息。
- `arena-gui`、`stats-gui`：竞技场选择和历史战绩菜单。竞技场选择器默认使用 54 格、每页展示 45 张地图，更多地图通过底行按钮分页；旧版 27 格、7 槽位布局会自动扩展。自定义布局继续生效，但当当前尺寸连一页竞技场都放不下时，运行时至少扩展到 54 格，并始终保留可靠的上一页、页码和下一页导航。
- `start-items-per-group`：每个竞技场分组的默认开局物品。
- `allowed-commands`：游戏内允许的命令。
- `game-end`：淘汰玩家和最佳数据展示。

刷新周期通常以 tick 为单位；20 tick 约等于 1 秒。倒计时和重连时间以秒为单位。

游戏开始时实际只有一名玩家的队伍不会启用队内私聊：该玩家直接使用 `format-chat-global` 格式向当前竞技场全部玩家和旁观者发言，不需要 `/shout` 权限，也不进入喊话冷却。判断使用开局人数快照；队友在开局后掉线、重连或被淘汰不会中途切换聊天频道。开局至少两人的队伍继续使用 `format-chat-team` 队内聊天，并可通过 `/shout` 主动发到公屏。

2.10.54 会把仍使用旧默认值 `speed-multiplier: 10` 的配置更新为 11；其他自定义速度不会覆盖。潜行速度倍率和后坐力作为新键插入 `fireball` 配置节。2.10.20 的爆炸范围、击退和伤害迁移规则保持不变。配置升级前会生成旧文件备份，修改火球参数后需要完整重启服务器。

竞技场配置架构 11 会把 Paper 1.21.11 已废弃的 `doFireTick` 自动删除，并写入 `fireSpreadRadiusAroundPlayer:0`。插件还会在运行时拦截 `SPREAD` 原因的点燃事件，防止其他插件或旧世界规则重新开启火势蔓延。火焰与灵魂火不会再被爆炸保护射线当作地图实体方块，因此着火表面的玩家放置方块仍可被火球正常炸毁。

TAB 相关常用项：

- `scoreboard-settings.tab-header-footer.enable`：显示 TAB 顶部和底部内容，默认开启，且不依赖右侧大厅计分板。
- `scoreboard-settings.tab-header-footer.lobby-header`：仅配置大厅 TAB 顶部文字。空列表沿用语言文件中的默认 128 空格宽度模板；非空时逐行填写，系统仍会自动保留内置宽度行。支持 `&` 颜色代码和 `{serverIp}`、`{on}` 等占位符，大厅页尾及竞技场各状态不会被覆盖。
- `scoreboard-settings.player-list.format-lobby-list`：显示大厅玩家前后缀，默认开启。
- `scoreboard-settings.player-list.names-refresh-interval`：TAB 动态文字刷新周期，默认 `1200` tick（60 秒），小于 1 时关闭。2.10.48 起刷新只写入真正发生变化的前后缀、颜色或名称可见性；静态 TAB 保持开启时不会再周期发送重复队伍数据包。
- 游戏进行时恢复原仓库的 `{teamColor}{teamName} ` 队伍前缀，玩家名和头顶名字继续使用所属队伍颜色；队伍按红→黄→绿→深绿→青→蓝→粉排列，非光谱色白→灰→深灰置后。同队的存活与淘汰玩家始终连续，并在整组内按完整玩家名字典序排列；纯旁观者最后。最终淘汰会立即同步给当前竞技场的所有 TAB 查看者。旧 `teammate-color` 配置会自动删除；简体中文语言配置架构 14 只迁移此前内置的空前缀，其他自定义文本保持不变。
- `scoreboard-settings.health.display-in-tab`：在 TAB 中额外显示生命值数字；默认关闭，避免与原版网络延迟图标混淆。头顶生命值由 `scoreboard-settings.health.enable` 单独控制；旁观玩家在两处都不显示生命值。
- 正式开局时就为空的队伍不会出现在游戏中或重置阶段的侧边栏；参战后被淘汰的队伍仍保留。这同时适用于通用 `{team}` 行和显式 `{Team<队伍>...}` 占位符行。

竞技场各状态继续使用原仓库的 104 空格宽度、行数、颜色代码、占位符和动画帧。2.10.18 只将大厅默认宽度扩大到 128 空格；升级时仅写回未修改的旧内置页首，不覆盖管理员自定义文本。运行时不足 128 的自定义宽度会补齐，更宽设置保持不变。

竞技场配置中的 `shop-protection` 和 `upgrades-protection` 控制 NPC 周围保护半径，运行时最小为 1 格。保护按方块坐标对称计算，并覆盖 NPC 脚部与头部周围一格。

`countdowns.game-restart` 默认迁移为 60 秒，聊天栏仅在 `60、30、15、10、5、4、3、2、1、0` 秒时提示。到 0 秒后先把玩家安全送回大厅；只有竞技场世界已经没有玩家且所有异步传送完成，插件才会卸载并恢复地图。传送失败会保持世界加载并重试，不会踢出玩家。

## Arenas/<竞技场>.yml

每张地图一份。主要节点：

- `groups`、`display-name`、`maxInTeam`、`minInTeam`。`groups` 是有序列表，同一竞技场可以同时属于多个匹配组；第一项是主组，生成器、开局物品、升级菜单和计分板等组专属配置读取主组。旧版单值 `group` 会自动迁移并删除。
- `allowSpectate`、`worldBorder`、`y-kill-height`、最大建造高度。
- 出生、商店、升级、生成器保护半径。
- `island-radius`：自动找床、治疗池和陷阱检测范围。
- `game-rules`：`规则:值` 列表。竞技场初始化时执行原版 `/time set noon` 的等价 API 操作，一次性设置正午 `6000 tick`、晴天、`doDaylightCycle:false`、`doWeatherCycle:false`、`doMobSpawning:false` 和 `randomTickSpeed:0`。不再每秒遍历竞技场或反复读写游戏规则；后续偏离 6000 tick 的时间跳跃、开始下雨和开始打雷均由对应事件直接取消，清除天气的事件允许通过。树叶腐烂、作物生长、草地/蘑菇蔓延、结冰融化等自然方块变化仍会被阻止。Locator Bar 仍在插件启动及所有世界初始化、加载时强制关闭。
- `waiting`、`spectator-loc`：等待和观战标点。
- `Team`：所有队伍的颜色、出生点、床、NPC 和岛屿生成器。
- `generator.Diamond`、`generator.Emerald`：中央生成器列表。

多分组示例：

```yaml
groups:
  - Solo       # 主组：读取 Solo 的玩法配置
  - Featured   # 额外匹配组
  - Daily      # 额外匹配组
```

玩家通过 `/bw join Featured`、`/bw join Daily` 或对应竞技场选择器时都能匹配到这张地图。同一次 `Featured+Daily` 查询只会列出和统计一次。

`Team.<队伍>.Color` 的青色填写 `CYAN`。2.10.11 起旧值 `AQUA` 会自动迁移为 `CYAN`，对应床、羊毛、玻璃和陶瓦均使用原版 `CYAN_*` 材质。

2.10.26 起 `/bw autoCreateTeams` 直接使用羊毛的完整 `Material` 做精确映射，不使用模糊名称匹配。亮绿/深绿分别是 `LIME_WOOL → GREEN`、`GREEN_WOOL → DARK_GREEN`；亮灰/深灰分别是 `LIGHT_GRAY_WOOL → GRAY`、`GRAY_WOOL → DARK_GRAY`。

2.10.43 起不再提供钻石/绿宝石生成点的结构扫描。`generator.Diamond`、`generator.Emerald` 仅由 `/bw addGenerator` 和 `/bw removeGenerator` 明确维护，`/bw save` 不会隐式扫描或改写；队伍床位的自动识别保持不变。

队伍名称直接取 `Team.<队伍>` 的节点名并保持原大小写，不经过语言翻译。旧语言文件中可能存在的 `team-name-<竞技场>-<队伍>` 不再参与显示。

坐标由插件生成，建议使用设置命令，不要手写。方向字段与坐标分开，避免破坏方块中心格式。所有玩家、NPC 和大厅传送朝向的 yaw 自动取最近的 90 度倍数，pitch 固定为 `0.0`。商店和升级村民关闭 AI、感知与重力，并逐 tick 只在发生偏移时恢复位置、身体和视线方向，不会自行移动或扭头。

`maxInTeam` 可在高级或引导式设置中用 `/bw setMaxInTeam` 修改；该设置操作会先把 `minInTeam` 同步为相同值。随后可用 `/bw setMinInTeam <1..maxInTeam>` 单独降低每支启用队伍的开局下限。竞技场载入、插件升级和配置迁移不会再覆盖已有 `minInTeam`。正常匹配只要能分成至少两支各自达到 `minInTeam` 且不超过 `maxInTeam` 的队伍便进入倒计时，无需占满全部配置队伍或全部容量。例如 `minInTeam: 1`、`maxInTeam: 2` 时，两名未组队玩家会被分为两支 1 人队并开局；如果这两人已经组成同一个 2 人小队，则必须等待至少一名对手，不能拆队制造对手。合法队伍数由 `max(2, ceil(玩家数 / maxInTeam))` 到 `min(地图队伍数, floor(玩家数 / minInTeam))` 的实时不等式范围计算，不保存配置结果表。分配器会保持正常大小的小队完整，并优先启用能够全部达到下限的最多队伍，再均衡填充未组队玩家；`/bw team` 的真实队伍预选会在不破坏这些约束时生效，冲突选择仅作为偏好。只有超过 `maxInTeam`、本来就无法装入任何单队的外部大队伍才会拆分。未启用的空队不会阻止开局。倒计时结束和 `TeamAssignEvent` 后仍会重新验证所有启用队伍，防止产生单队游戏、缺人队或超员队。`/bw start debug` 继续允许 OP 进行单队地图测试。

合法队伍数量不使用配置组合表。设当前玩家数为 `P`，地图配置队伍数为 `C`，则实时计算下界 `max(2, ceil(P / maxInTeam))` 和上界 `min(C, floor(P / minInTeam))`。上界小于下界时不能开局；否则从上界开始尝试保持小队完整的容量分配，以启用尽可能多的队伍。分配过程只根据当前负载和剩余人数计算，不缓存历史负载组合。

## generators.yml

文件位置：`plugins/SimpMC-BedWars/generators.yml`。它控制铁、金、钻石、绿宝石的：

- `delay`：生成间隔秒数。
- `amount`：每次生成数量。
- `spawn-limit`：单生成点地面物品上限。
- `start`：II/III 级启用时间。
- `stack-items`：合并掉落物以减少实体。

2.10.7 的默认队伍资源配置为：

```yaml
Default:
  iron:
    delay: 1
    amount: 2
    spawn-limit: 32
  gold:
    delay: 4
    amount: 2
    spawn-limit: 7
```

`delay` 就是真实刷新间隔秒数，越小刷新越快；0 或负数会被安全限制为 1 秒。`amount` 越大，每次产出越多。修改后需要完整重启服务器。提高产量后建议把根节点的 `stack-items` 设为 `true`，减少地面物品实体合并前的压力。

复制 `Default` 并改为竞技场 `groups` 第一项的主组名称，可为不同模式设置独立速度。如果已经存在与竞技场主组同名的配置节，游戏会优先读取该节，而不是 `Default`；其他成员组只参与匹配和菜单筛选，不改变玩法参数。

旧版仍使用内置默认值时会自动把铁间隔从 2 秒迁移为 1 秒、金间隔从 6 秒迁移为 4 秒；自定义值及自定义分组不会被覆盖。

## shop.yml

文件位置：`plugins/SimpMC-BedWars/shop.yml`。商品价格路径为：

```text
<分类>.category-content.<商品>.content-tiers.<等级>.tier-settings.cost
```

同级的 `currency` 决定货币。以下示例把 16 个羊毛设为 4 铁：

```yaml
blocks-category:
  category-content:
    wool:
      content-settings:
        content-slot: 19
        is-permanent: false
      content-tiers:
        tier1:
          tier-item:
            material: WHITE_WOOL
            amount: 16
          tier-settings:
            cost: 4
            currency: iron
```

例如 TNT 默认路径是 `utility-category.category-content.tnt.content-tiers.tier1.tier-settings.cost`。修改 `cost` 后完整重启服务器。

袖珍弹出塔的商品 ID 是 `tower`，默认价格路径为 `utility-category.category-content.tower.content-tiers.tier1.tier-settings.cost`。它的显示名称和说明位于语言文件 `shop-items-messages.utility-category.content-item-tower-name` 与 `shop-items-messages.utility-category.content-item-tower-lore`。2.10.40 会自动删除错误的旧 `Compact Pop-up Tower` 语言节点，并把其中的文本迁移到 `tower`；系统生成的 `Name not set`/`Lore not set` 会被正确默认值替换，管理员已经写在正确路径的自定义文本保持不变。

商店永久护甲固定只发放护腿和靴子。玩家开局时仍穿戴完整的本队颜色皮革套装，购买链甲、铁甲或钻石甲后，队伍色皮革头盔与胸甲会继续保留。旧 `config.yml` 中的 `shop-settings.sell-full-armor` 会自动删除；`shop.yml` 升级到架构 5 时会清理所有护甲分类中的头盔、胸甲和鞘翅商品，价格与下半身自定义商品保持不变。

货币支持 `iron`、`gold`、`diamond`、`emerald`；同时安装 Vault 和实际经济服务提供者后，可按现有格式使用经济货币。只安装 Vault 不会创建余额。Material、附魔和药水名称必须适用于 Paper 1.21.11。

团队升级价格不在 `shop.yml`，而在 `plugins/SimpMC-BedWars/upgrades2.yml` 的 `<升级>.tier-<等级>.cost`，例如 `upgrade-forge.tier-1.cost`。

## upgrades2.yml

升级的 `receive` 支持以下动作：

- `enchant-item: 附魔,等级,目标类型`
- `player-effect: 效果,等级,持续秒数,team|base|enemy`
- `generator-edit: 资源,延迟,数量,上限`
- `remove-effect: 效果,enemy`
- `dragon: 数量`

每个 tier 还需要 `cost`、`currency` 和 `display-item`。陷阱按队列顺序触发，队列上限和价格增量位于 `default-upgrades-settings`。

## sounds.yml

每个事件包含：

```yaml
event-name:
  sound: ENTITY_PLAYER_LEVELUP
  volume: 1.0
  pitch: 1.0
```

声音名称使用 Bukkit Sound。无效声音会使用安全回退并在排查时显示日志。

## levels.yml

- `levels`：等级名称和升级经验，支持 `5-10` 范围及 `others` 回退。
- `xp-rewards`：分钟、队友、胜利、破床、普通击杀、最终击杀经验。
- `progress-bar`：进度字符、已完成/未完成颜色和格式。

## rewards.yml

Vault 金币奖励。没有 Vault 或没有向 Vault 注册的经济服务提供者时，文件仍会生成，但奖励不会生效。服务可用状态以启动日志中的 `已接入 Vault 经济服务` 为准。

## signs.yml

- `format`：告示牌四行文本。
- 状态材质：waiting、starting、playing、restarting。
- `locations`：插件自动维护的告示牌坐标，不建议手动编辑。

## Languages/messages_<语言>.yml

玩家可见文本。注意：

- `&` 后接颜色代码。
- `{player}`、`{message}`、`{TeamName}` 等占位符不能删除或改名。
- 聊天运行时统一为 `&f> &7消息`。
- `.bak` 是自动迁移备份，不会加载为语言。
- 语言配置架构 2 会迁移袖珍弹出塔的旧商品文本键；简体中文使用架构 11，并显示完整中文名称、价格与用途说明。

## 配置修改原则

1. 修改前备份。
2. 不要使用 Tab 缩进，只用空格。
3. Material、Sound、PotionEffect 和 Enchantment 使用 Paper 1.21.11 名称。
4. 关键配置修改后完整重启，不使用 `/reload`。
5. 启动后先检查控制台，再进行一局完整测试。
