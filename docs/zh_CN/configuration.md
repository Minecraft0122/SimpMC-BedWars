# 全部配置文件说明

插件会把管理员可编辑的 YAML 放在 `plugins/SimpMC-BedWars`。`config-version` 由迁移器管理，不要手动降低。新版本会自动备份、补字段并写入中文注释。

`config-version` 是各配置文件的内部架构字段，不是商店分类、升级项或竞技场内容。2.10.16 起各加载器会主动忽略该字段。

## config.yml

主配置，包含：

- `serverType`：MULTIARENA、SHARED 或 BUNGEE。
- `lobbyServer`：BungeeCord/Velocity 代理配置中的大厅服务器名称，默认 `hub`；不是 IP 地址。主大厅红床点击后会向代理发送 `Connect` 请求切换到该服务器。
- `language`、禁用语言列表：默认语言及玩家可选语言。
- `chat`：全局聊天和插件聊天格式。
- `scoreboard-settings`：大厅/游戏计分板、TAB、队伍颜色、血量和刷新周期。
- `rejoin-time`：掉线重连窗口，默认 30 秒。
- `countdowns`：开局、床消失、决战、结束和复活倒计时。
- `party-settings`：内部及外部组队集成。
- `tnt-jump-settings`、`blast-protection`、`tnt-prime-settings`：TNT 参数。
- `fireball`：火球速度、爆炸、击退、冷却和伤害。2.10.20 的平衡默认值为范围 3.25、水平击退 1.15、垂直击退 0.75、敌方伤害 3.5。
- `database`：MySQL；关闭时使用 SQLite。
- `performance-settings`：Paper 传送、资源旋转等优化。
- `lobby-items`、`pre-game-items`、`spectator-items`：不同阶段的命令物品。主大厅默认提供历史战绩、竞技场选择器和第 9 格的“回到主大厅”红床；2.10.30 起大厅红床固定连接代理配置中的 `lobbyServer`，不再传送到本服 `/bw setLobby` 坐标。等待区和观战区的离开红床仍返回本服 BedWars 大厅；手动 `/leave` 按官方规则无需权限。
- `arena-gui`、`stats-gui`：竞技场选择和历史战绩菜单。
- `start-items-per-group`：每个竞技场分组的默认开局物品。
- `shop-settings.sell-full-armor`：全服护甲售卖模式；`true` 为全身四件套，`false` 为仅护腿和靴子。该开关只影响商店，正式开局始终发放队伍色皮革四件套。
- `allowed-commands`：游戏内允许的命令。
- `game-end`：淘汰玩家和最佳数据展示。

刷新周期通常以 tick 为单位；20 tick 约等于 1 秒。倒计时和重连时间以秒为单位。

2.10.20 会把仍使用上一版默认值的火球参数自动调整到新平衡值；已经自定义的爆炸范围、击退或伤害保持原值。配置升级前会生成旧文件备份，修改火球参数后需要完整重启服务器。

TAB 相关常用项：

- `scoreboard-settings.tab-header-footer.enable`：显示 TAB 顶部和底部内容，默认开启，且不依赖右侧大厅计分板。
- `scoreboard-settings.tab-header-footer.lobby-header`：仅配置大厅 TAB 顶部文字。空列表沿用语言文件中的默认 128 空格宽度模板；非空时逐行填写，系统仍会自动保留内置宽度行。支持 `&` 颜色代码和 `{serverIp}`、`{on}` 等占位符，大厅页尾及竞技场各状态不会被覆盖。
- `scoreboard-settings.player-list.format-lobby-list`：显示大厅玩家前后缀，默认开启。
- 游戏进行时，玩家列表和玩家头顶名字只使用其所属队伍颜色渲染玩家名本身，名字前不再添加队伍名称或队伍字母；同队玩家连续显示并按玩家名字典序排列，淘汰玩家仍留在原队伍分组；旧 `teammate-color` 配置会自动删除。简体中文语言配置架构 9 会自动清理仍等于插件旧默认值的游戏队伍前缀，其他自定义文本保持不变。
- `scoreboard-settings.health.display-in-tab`：在 TAB 中额外显示生命值数字；默认关闭，避免与原版网络延迟图标混淆。头顶生命值由 `scoreboard-settings.health.enable` 单独控制。

竞技场各状态继续使用原仓库的 104 空格宽度、行数、颜色代码、占位符和动画帧。2.10.18 只将大厅默认宽度扩大到 128 空格；升级时仅写回未修改的旧内置页首，不覆盖管理员自定义文本。运行时不足 128 的自定义宽度会补齐，更宽设置保持不变。

竞技场配置中的 `shop-protection` 和 `upgrades-protection` 控制 NPC 周围保护半径，运行时最小为 1 格。保护按方块坐标对称计算，并覆盖 NPC 脚部与头部周围一格。

`countdowns.game-restart` 默认迁移为 60 秒，聊天栏仅在 `60、30、15、10、5、4、3、2、1、0` 秒时提示。

## Arenas/<竞技场>.yml

每张地图一份。主要节点：

- `group`、`display-name`、`maxInTeam`。
- `allowSpectate`、`worldBorder`、`y-kill-height`、最大建造高度。
- 出生、商店、升级、生成器保护半径。
- `island-radius`：自动找床、治疗池和陷阱检测范围。
- `game-rules`：`规则:值` 列表。插件会自动补入 `locatorBar:false`，并在所有世界加载和竞技场初始化时强制关闭 Locator Bar。
- `waiting`、`spectator-loc`：等待和观战标点。
- `Team`：所有队伍的颜色、出生点、床、NPC 和岛屿生成器。
- `generator.Diamond`、`generator.Emerald`：中央生成器列表。

`Team.<队伍>.Color` 的青色填写 `CYAN`。2.10.11 起旧值 `AQUA` 会自动迁移为 `CYAN`，对应床、羊毛、玻璃和陶瓦均使用原版 `CYAN_*` 材质。

2.10.26 起 `/bw autoCreateTeams` 直接使用羊毛的完整 `Material` 做精确映射，不使用模糊名称匹配。亮绿/深绿分别是 `LIME_WOOL → GREEN`、`GREEN_WOOL → DARK_GREEN`；亮灰/深灰分别是 `LIGHT_GRAY_WOOL → GRAY`、`GRAY_WOOL → DARK_GRAY`。

2.10.29 起只有引导式快速设置中的 `/bw autoDetectGenerators` 会扫描并写入 `generator.Diamond`、`generator.Emerald`，`/bw save` 不再隐式扫描。底层可为 `3×3` 同种资源块实心层，或只有中心一格为对应资源块；中层必须是楼梯环和中心空气，顶层必须全为空气。识别结果只追加缺失点位，不覆盖已有列表。队伍床位仍会在引导式设置中自动识别。

队伍名称直接取 `Team.<队伍>` 的节点名并保持原大小写，不经过语言翻译。旧语言文件中可能存在的 `team-name-<竞技场>-<队伍>` 不再参与显示。

坐标由插件生成，建议使用设置命令，不要手写。方向字段与坐标分开，避免破坏方块中心格式。

`minPlayers` 已被删除并会从旧竞技场配置中自动清理。竞技场固定在至少两名玩家时开始倒计时，正式开局前还会再次确认至少有两支非空队伍。

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

复制 `Default` 并改为竞技场 `group` 名称，可为不同模式设置独立速度。如果已经存在与竞技场 `group` 同名的配置节，游戏会优先读取该节，而不是 `Default`。

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

全服护甲售卖模式在 `config.yml` 中设置：

```yaml
shop-settings:
  # true：头盔、胸甲、护腿、靴子；false：仅护腿、靴子
  sell-full-armor: true
```

修改后需要完整重启服务器。该开关只决定商店购买永久护甲时实际发放哪些槽位，不影响开局的队伍色皮革四件套，也不会覆盖 `shop.yml` 的价格或自定义商品。`shop.yml` 的 `config-version` 会自动升级到 4：只有仍使用内置护腿与靴子材质的旧商品会补齐上装；已删除的商品、自定义下装材质和已有自定义上装不会被覆盖。旧 `config.yml` 也会自动备份并升级到架构 16，补入默认值和中文注释。

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

## 配置修改原则

1. 修改前备份。
2. 不要使用 Tab 缩进，只用空格。
3. Material、Sound、PotionEffect 和 Enchantment 使用 Paper 1.21.11 名称。
4. 关键配置修改后完整重启，不使用 `/reload`。
5. 启动后先检查控制台，再进行一局完整测试。
