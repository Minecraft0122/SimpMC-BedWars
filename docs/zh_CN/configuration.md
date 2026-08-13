# 全部配置文件说明

插件会把管理员可编辑的 YAML 放在 `plugins/SimpMC-BedWars`。`config-version` 由迁移器管理，不要手动降低。新版本会自动备份、补字段并写入中文注释。

`config-version` 是各配置文件的内部架构字段，不是商店分类、升级项或竞技场内容。2.10.16 起各加载器会主动忽略该字段。

2.10.50 起，自动迁移会以新版默认配置为结构模板：`config-version` 放在顶部，已知键按新版顺序放回对应配置节，不再统一追加到文件末尾。管理员或附属插件增加的未知键不会被删除，会留在所属配置节的已知键之后，其相对顺序、配置值和注释均保留。每次架构升级前仍会先生成 `.bak` 备份。

## config.yml

主配置，包含：

- `serverType`：MULTIARENA、SHARED 或 BUNGEE。
- `debug`：详细故障日志开关，默认 `false`；生产环境保持关闭。`/bw start debug` 仅表示单队测试开局，不会修改此项或临时开启日志。
- `lobbyServer`：BungeeCord/Velocity 代理 `[servers]` 中的大厅服务器键名，默认 `hub`；不是 IP、端口或 MotD。主大厅红床会立即静默发送 `Connect`，不查询或向玩家展示代理节点、服务器列表和故障信息；配置问题只通过后端及代理日志排查。
- `arenaGroups`：全局可用的匹配组名称；`Default` 是内置组，无需写入。每张竞技场地图只能选择其中一个组。
- `language`、禁用语言列表：默认语言及玩家可选语言。
- `chat`：全局聊天和插件聊天格式。
- `scoreboard-settings`：大厅/游戏计分板、TAB、队伍颜色、血量和刷新周期。
- `rejoin-time`：普通断线或网络超时的重连窗口，默认 30 秒；服务器命令、封禁或反作弊造成的踢出不会保留重连资格。
- `countdowns`：开局、床消失、决战、结束和复活倒计时。
- `party-settings`：内部及外部组队集成。
- `tnt-jump-settings`、`blast-protection`、`tnt-prime-settings`：TNT 参数。
- `fireball`：火球速度、射程、爆炸、击退、冷却和伤害。`speed-multiplier: 16` 对应普通初速度 1.6 格/tick；潜行时 `sneak-speed-multiplier: 1.5` 使初速度达到 2.4 格/tick，`sneak-acceleration-multiplier: 2.0` 再把持续加速度由每 tick 0.1 提高到 0.2。每次发射会在 `flight-range.min: 200` 与 `flight-range.max: 300` 之间随机一次最大飞行距离，并按实际路径累计；碰撞、世界边界、服务端视距和未加载区块仍可能让火球提前结束。`sneak-recoil: 0.10` 会沿火球发射速度的完整三维反方向推动玩家，代码硬限制最大为 0.20；`cooldown: 0.4` 的持续射速约为每秒 2.5 发，一个 1 秒窗口内通常可发 2 至 3 个。2.10.20 的平衡默认值仍为爆炸范围 3.25、水平击退 1.15、垂直击退 0.75、敌方伤害 3.5。`make-fire` 只决定爆炸处是否生成火焰，竞技场不会允许火势向周围蔓延。
- `database`：MySQL；关闭时使用 SQLite。
- `performance-settings`：Paper 传送、资源旋转等优化。
- `lobby-items`、`pre-game-items`、`spectator-items`：不同阶段的命令物品。主大厅默认提供历史战绩、竞技场选择器和第 9 格的“回到主大厅”红床；大厅红床带有独立目标标记，固定连接代理配置中的 `lobbyServer`，MULTIARENA 模式也会执行代理切服，不传送到本服 `/bw setLobby` 坐标。等待区和观战区红床使用另一目标标记，直接返回本服 BedWars 大厅，不经过命令权限。管理员可以修改显示材质和命令文本，内置 `leave` 项的返回语义仍由其配置节点名确定。4.0.8 起，删除整个物品节点后，后续配置升级不会再次生成；旧架构 15 曾误删的自定义 `leave` 会在当前值仍为内置默认值时，从架构 15 删除前的最后快照，或架构 15–17 中重新配置过的最新 `config.yml.v*.bak` 自动恢复；架构 18 后的删除或改写快照会否决旧值。玩家进入大厅时会立即替换旧 BedWars 命令物品，并在 15 tick 后做一次带实时上下文校验的选择性复核，不再由延迟任务清空正常流程的整个背包；经传送门或附属插件跨世界进入大厅也走同一入口。无效物品只跳过自身，同槽位配置会输出中文警告，代理返回项具有稳定优先级。完整代理示例见[安装文档](installation.md#bungee)。
- 大厅进入/离开提示只向同样位于 BedWars 大厅的玩家发送；竞技场、观战者和地图设置会话不会收到。大厅世界名直接从 `lobbyLoc` 文本读取，即使该世界在插件加载时尚未加载也能正确识别。大厅和加入 NPC 的旧朝向会自动迁移为最近的 90 度 yaw，pitch 固定为 0。
- 竞技场加入通知只在目标竞技场处于等待或开局倒计时发送，接收者为该竞技场与大厅玩家；已开局和重置阶段不播报。玩家在广播前即从大厅受众移除，不会因异步传送时序重复收到大厅身份消息。
- `arena-gui`、`stats-gui`：竞技场选择和历史战绩菜单。竞技场选择器默认使用 54 格、每页展示 45 张地图，更多地图通过底行按钮分页；旧版 27 格、7 槽位布局会自动扩展。自定义布局继续生效，但当当前尺寸连一页竞技场都放不下时，运行时至少扩展到 54 格，并始终保留可靠的上一页、页码和下一页导航。
- `start-items-per-group`：每个竞技场分组的默认开局物品。
- `allowed-commands`：游戏内允许的命令。
- `game-end`：淘汰玩家和最佳数据展示。

刷新周期通常以 tick 为单位；20 tick 约等于 1 秒。倒计时和重连时间以秒为单位。

游戏开始时实际只有一名玩家的队伍不会启用队内私聊：该玩家直接使用 `format-chat-global` 格式向当前竞技场全部玩家和旁观者发言，不需要 `/shout` 权限。判断使用开局人数快照；队友在开局后掉线、重连或被淘汰不会中途切换聊天频道。开局至少两人的队伍继续使用 `format-chat-team` 队内聊天，并可通过 `/shout` 主动发到公屏。7.0.0 起 `/shout` 与 `!消息` 都没有插件冷却；主配置架构 28 会删除旧 `shout-cmd-cooldown`，管理员无需再维护该值。

3.0.0 的主配置架构 25 会把仍使用上一版默认值 `speed-multiplier: 11`、`sneak-speed-multiplier: 1.25` 的配置更新为 16 和 1.5；更早仍为默认 10 的配置会直接迁移到 16。管理员设置的其他速度不会被覆盖。5.0.0 的主配置架构 27 只补入 `flight-range.min/max`，不会改写已有火球参数。8.0.0 的主配置架构 29 新增 `sneak-acceleration-multiplier: 2.0`，并只把仍为旧内置值的 `sneak-recoil: 0.05`、`cooldown: 0.5` 分别迁移到 0.10 和 0.4；管理员自定义值保持不变。Paper 1.21.11 的 Bukkit `setAcceleration` 实际会覆盖当前速度，因此插件通过版本支持层直接设置持续加速度字段，避免抹掉 1.6/2.4 格/tick 初速度；射程任务随单个火球实体运行，不做全服扫描，也不为远距离飞行强制加载区块。配置升级前会生成旧文件备份，修改火球参数后需要完整重启服务器。

竞技场配置架构 11 会把 Paper 1.21.11 已废弃的 `doFireTick` 自动删除，并写入 `fireSpreadRadiusAroundPlayer:0`。插件还会在运行时拦截 `SPREAD` 原因的点燃事件，防止其他插件或旧世界规则重新开启火势蔓延。火焰与灵魂火不会再被爆炸保护射线当作地图实体方块，因此着火表面的玩家放置方块仍可被火球正常炸毁。

TAB 相关常用项：

- `scoreboard-settings.tab-header-footer.enable`：显示 TAB 顶部和底部内容，默认开启，且不依赖右侧大厅计分板。
- `scoreboard-settings.tab-header-footer.refresh-interval`：TAB 页首页尾动态内容刷新周期，默认 `20` tick。游戏时间复用此任务和内容缓存；小于 1 时按现有配置语义关闭动态刷新。
- `scoreboard-settings.tab-header-footer.lobby-header`：仅配置大厅 TAB 顶部文字。空列表沿用语言文件中的默认内容；非空时逐行填写，系统仍会自动保留内置宽度行。支持 `&` 颜色代码和 `{serverIp}`、`{on}` 等占位符，大厅页尾及竞技场各状态不会被覆盖。
- `scoreboard-settings.player-list.format-lobby-list`：显示大厅玩家前后缀，默认开启。
- `scoreboard-settings.player-list.names-refresh-interval`：TAB 动态文字刷新周期，默认 `1200` tick（60 秒），小于 1 时关闭周期刷新。scoreboard Team 元数据仍只在真正变化时写入；玩家行平时由登录、切换可见性、选队和游戏状态变化等事件即时同步，周期刷新只重放可能被其他插件或 PlayerInfo 重建覆盖的 nullable 名称状态，并检查实际绑定的 scoreboard 是否发生漂移。关闭周期刷新不会关闭这些事件同步。
- 游戏内玩家行不再显示 `{teamName}` 或 `{teamLetter}` 队伍标签；这两个占位符只在玩家行前后缀的运行时渲染中按空字符串处理，不会改写磁盘上的管理员模板，也不会影响 TAB 页首、页尾或其他状态文字。受管 PlayerInfo 的自定义名称保持为空，由每个查看者的 scoreboard Team 同时渲染 TAB 的队伍色玩家名和头顶名牌，避免两条状态互相覆盖。最终淘汰者和纯旁观者只在对应查看者的 PlayerInfo 中标记为 `SPECTATOR`，服务端真实模式仍由竞技场逻辑管理（通常为 ADVENTURE），因此旁观菜单和交互不受影响；离场时会恢复真实模式及第三方 nullable 名称。即使关闭侧边栏、页首页尾及对应状态的完整 TAB 格式，竞技场仍建立最小队伍色/观察者上下文，且不会关闭队伍排序。等待或倒计时阶段的 GUI/`/bw team` 预选会立即更新颜色与排序。队伍按红→黄→绿→深绿→青→蓝→粉排列，非光谱色白→灰→深灰置后。同队的存活与淘汰玩家始终连续，并在整组内按完整玩家名字典序排列；纯旁观者最后。最终淘汰会立即同步给当前竞技场的所有 TAB 查看者。旧 `teammate-color` 配置会自动删除；`format-tab.playing.game-time` 继续保留管理员已有的自定义文本。
- 存活、已淘汰和纯旁观三种游戏中页首都会插入本局已进行时间。`{gameTime}` 显示 `MM:SS`，超过一小时显示 `HH:MM:SS`；`{time}` 仍表示下一事件倒计时。等待、开局倒计时和重置阶段不会显示继续增长的本局时间。将语言文件的 `format-tab.playing.game-time` 设为空字符串可隐藏该行；若自定义页首已经直接包含 `{gameTime}`，插件不会重复插入。
- `scoreboard-settings.health.display-in-tab`：在 TAB 中额外显示生命值数字；默认关闭，避免与原版网络延迟图标混淆。头顶生命值由 `scoreboard-settings.health.enable` 单独控制；旁观玩家在两处都不显示生命值。
- 正式开局时就为空的队伍不会出现在游戏中或重置阶段的侧边栏；参战后被淘汰的队伍仍保留。这同时适用于通用 `{team}` 行和显式 `{Team<队伍>...}` 占位符行。

大厅和竞技场全部状态统一保证至少 128 空格宽度；旧语言文件中 104 空格的 BedWars1058 页首会在发送时自动拓宽，自定义的更宽页首保持不变。除宽度行外，原有行数、颜色代码、占位符和动画帧不变。

竞技场配置中的 `shop-protection` 和 `upgrades-protection` 控制 NPC 周围保护半径，运行时最小为 1 格。保护按方块坐标对称计算，并覆盖 NPC 脚部与头部周围一格。

`countdowns.game-restart` 默认迁移为 60 秒，聊天栏仅在 `60、30、15、10、5、4、3、2、1、0` 秒时提示。到 0 秒后先把玩家安全送回大厅；只有竞技场世界已经没有玩家且所有异步传送完成，插件才会卸载并恢复地图。传送失败会保持世界加载并重试，不会踢出玩家。

## Arenas/<竞技场>.yml

每张地图一份。主要节点：

- `group`、`display-name`、`minPlayers`、`maxInTeam`。每张竞技场只能属于一个组；该值同时用于匹配、生成器、开局物品、升级菜单和计分板等组专属配置。2.13.x 的 `groups` 列表会保留第一项并自动迁移回单值。
- `allowSpectate`、`worldBorder`、`y-kill-height`、最大建造高度。
- 出生、商店、升级、生成器保护半径。
- `island-radius`：自动找床、治疗池和陷阱检测范围。
- `game-rules`：`规则:值` 列表。Paper 1.21.11 使用 `advance_time`、`advance_weather` 和 `spawn_mobs`；旧配置中的 `doDaylightCycle`、`doWeatherCycle`、`doMobSpawning`、`announceAdvancements`、`doInsomnia` 和 `doImmediateRespawn` 会在运行时显式映射到现代注册键，无需手动改写。竞技场初始化时设置正午 `6000 tick`、晴天、关闭昼夜与天气推进，并禁用生物自然生成和随机方块刻。后续 `/time`、睡眠、插件跳时、`/gamerule`、下雨和雷暴均由事件守卫改回固定状态，不进行周期扫描。树叶腐烂、作物生长、草地/蘑菇蔓延、结冰融化等竞技场自然方块变化仍会被阻止。正午与晴天保证普通主世界露天的最高自然天光，不等于洞穴、下界或末地 Fullbright。
- `waiting`、`spectator-loc`：等待和观战标点。
- `Team`：所有队伍的颜色、出生点、床、NPC 和岛屿生成器。
- `generator.Diamond`、`generator.Emerald`：中央生成器列表。

分组示例：

```yaml
group: Solo
```

玩家通过 `/bw join Solo` 或对应竞技场选择器匹配这张地图。`/bw arenaGroup set <竞技场> <分组>` 会覆盖原分组，不再支持追加到多个组。

`Team.<队伍>.Color` 的青色填写 `CYAN`。2.10.11 起旧值 `AQUA` 会自动迁移为 `CYAN`，对应床、羊毛、玻璃和陶瓦均使用原版 `CYAN_*` 材质。

2.10.26 起 `/bw autoCreateTeams` 直接使用羊毛的完整 `Material` 做精确映射，不使用模糊名称匹配。亮绿/深绿分别是 `LIME_WOOL → GREEN`、`GREEN_WOOL → DARK_GREEN`；亮灰/深灰分别是 `LIGHT_GRAY_WOOL → GRAY`、`GRAY_WOOL → DARK_GRAY`。

2.10.43 起不再提供钻石/绿宝石生成点的结构扫描。`generator.Diamond`、`generator.Emerald` 仅由 `/bw addGenerator` 和 `/bw removeGenerator` 明确维护，`/bw save` 不会隐式扫描或改写；队伍床位的自动识别保持不变。

队伍名称直接取 `Team.<队伍>` 的节点名并保持原大小写，不经过语言翻译。旧语言文件中可能存在的 `team-name-<竞技场>-<队伍>` 不再参与显示。

坐标由插件生成，建议使用设置命令，不要手写。方向字段与坐标分开，避免破坏方块中心格式。所有玩家、NPC 和大厅传送朝向的 yaw 自动取最近的 90 度倍数，pitch 固定为 `0.0`。商店和升级村民关闭 AI、感知与重力，并逐 tick 只在发生偏移时恢复位置、身体和视线方向，不会自行移动或扭头。

`minPlayers` 是整个竞技场进入倒计时所需的最低人数，可用 `/bw setMinPlayers <人数>` 修改，最小为 2；`maxInTeam` 是每支队伍容量，可用 `/bw setMaxInTeam` 修改。两者互不覆盖。正常匹配在总人数达到 `minPlayers`、不超过地图总容量，且能形成至少两支非空队伍时开始；每支队伍只需不超过 `maxInTeam`，不再要求达到额外的每队下限。例如 `minPlayers: 2`、`maxInTeam: 4` 时，两名未组队玩家会分到两队并开局；同一个两人小队则等待至少一名对手。分配器实时根据当前小队、选队偏好、地图队伍数和容量计算，尽量分散并均衡玩家，不使用配置组合表或历史状态表。正常大小的小队保持完整，只有超过 `maxInTeam` 的外部大队伍才会拆分。倒计时结束及 `TeamAssignEvent` 后会再次校验最低人数、至少两队和容量；`/bw start debug` 仍可供 OP 单队测试。

## generators.yml

文件位置：`plugins/SimpMC-BedWars/generators.yml`。它控制铁、金、钻石、绿宝石的：

- `delay`：生成间隔秒数。
- `amount`：每次生成数量。
- `spawn-limit`：单生成点地面物品总数上限；开启堆叠后仍按堆内物品数量计算，不按实体数量计算。
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

`delay` 就是真实刷新间隔秒数，越小刷新越快；0 或负数会被安全限制为 1 秒。`amount` 越大，每次产出越多。修改后需要完整重启服务器。建议把根节点的 `stack-items` 设为 `true`；插件会在生成时直接按材质最大堆叠数合并，同样产量只创建最少数量的地面物品实体。

复制 `Default` 并改为竞技场 `group` 名称，可为不同模式设置独立速度。如果已经存在与竞技场组同名的配置节，游戏会优先读取该节，而不是 `Default`。

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

回城卷轴的商品 ID 是 `recall-scroll`，默认价格为 3 个钻石，价格路径为 `utility-category.category-content.recall-scroll.content-tiers.tier1.tier-settings.cost`。购买物品中的 `bedwars-item: recall-scroll` 是核心行为标识；可以修改价格、槽位、显示材质和名称，但不要删除或改写该标识。

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

`upgrade-swords.tier-1` 到 `tier-4` 对应锋利 I–IV，按顺序购买。默认价格为 2、4、8、14 钻石。`upgrades2.yml` 架构 9 会根据旧文件的架构，只把四级整组仍为上一版内置价格 4、6、9、14 或更早内置价格 4、8、16、32 的配置迁移到新价格；只要任一级被自定义，整组价格都保持不变。

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
- 语言配置架构 2 会迁移袖珍弹出塔的旧商品文本键。8.1.0 的简体中文内置文件使用架构 17，其他内置语言使用架构 7；自定义语言文件使用通用架构 5，不会被强制套用内置玩家行模板。
- 架构 16/6 只会把完全等于已知历史内置值的 TAB 玩家行前缀或后缀迁移为“不显示队伍名称/首字母”的新默认值，包括游戏中、胜利、失败和淘汰样式。只要某一列表与内置旧值不完全相同，就视为管理员自定义并在磁盘上原样保留；运行时仅把玩家行中的 `{teamName}`、`{teamLetter}` 解析为空，其他文字、颜色和占位符继续生效。`format-tab.playing.game-time`、自定义聊天、未知键和其他自定义模板不会被覆盖。迁移前仍会生成 `.bak` 备份。

## 配置修改原则

1. 修改前备份。
2. 不要使用 Tab 缩进，只用空格。
3. Material、Sound、PotionEffect 和 Enchantment 使用 Paper 1.21.11 名称。
4. 关键配置修改后完整重启，不使用 `/reload`。
5. 启动后先检查控制台，再进行一局完整测试。
