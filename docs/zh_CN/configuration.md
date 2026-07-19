# 全部配置文件说明

插件会把管理员可编辑的 YAML 放在 `plugins/SimpMC-BedWars`。`config-version` 由迁移器管理，不要手动降低。新版本会自动备份、补字段并写入中文注释。

## config.yml

主配置，包含：

- `serverType`：MULTIARENA、SHARED 或 BUNGEE。
- `language`、禁用语言列表：默认语言及玩家可选语言。
- `chat`：全局聊天和插件聊天格式。
- `scoreboard-settings`：大厅/游戏计分板、TAB、队伍颜色、血量和刷新周期。
- `rejoin-time`：掉线重连窗口，默认 30 秒。
- `countdowns`：开局、床消失、决战、结束和复活倒计时。
- `party-settings`：内部及外部组队集成。
- `tnt-jump-settings`、`blast-protection`、`tnt-prime-settings`：TNT 参数。
- `fireball`：火球速度、爆炸、击退、冷却和伤害。2.10.5 的增强默认值为范围 3.5、水平击退 1.25、垂直击退 0.8、敌方伤害 4.0。
- `database`：MySQL；关闭时使用 SQLite。
- `performance-settings`：Paper 传送、资源旋转等优化。
- `lobby-items`、`pre-game-items`、`spectator-items`：不同阶段的命令物品。
- `arena-gui`、`stats-gui`：竞技场选择和历史战绩菜单。
- `start-items-per-group`：每个竞技场分组的默认开局物品。
- `allowed-commands`：游戏内允许的命令。
- `game-end`：淘汰玩家和最佳数据展示。

刷新周期通常以 tick 为单位；20 tick 约等于 1 秒。倒计时和重连时间以秒为单位。

TAB 相关常用项：

- `scoreboard-settings.tab-header-footer.enable`：显示 TAB 顶部和底部内容，默认开启，且不依赖右侧大厅计分板。
- `scoreboard-settings.tab-header-footer.header`：经典 TAB 页首，按列表逐行配置，支持颜色代码与 `{serverIp}`。
- `scoreboard-settings.tab-header-footer.footer`：经典 TAB 页尾，按列表逐行配置，支持颜色代码与 `{serverIp}`。
- `scoreboard-settings.player-list.format-lobby-list`：显示大厅玩家前后缀，2.10.10 起默认开启。
- 游戏进行时，玩家列表和玩家头顶名字统一使用其所属队伍颜色；旧 `teammate-color` 配置会自动删除。
- `scoreboard-settings.health.display-in-tab`：在 TAB 中额外显示生命值数字；默认关闭，避免与原版网络延迟图标混淆。头顶生命值由 `scoreboard-settings.health.enable` 单独控制。

2.10.12 起恢复经典 TAB 页首/页尾，不再插入人为撑宽的横线。`countdowns.game-restart` 默认迁移为 60 秒，聊天栏仅在 `60、30、15、10、5、4、3、2、1、0` 秒时提示。

## Arenas/<竞技场>.yml

每张地图一份。主要节点：

- `group`、`display-name`、`maxInTeam`。
- `allowSpectate`、`worldBorder`、`y-kill-height`、最大建造高度。
- 出生、商店、升级、生成器保护半径。
- `island-radius`：自动找床、治疗池和陷阱检测范围。
- `game-rules`：`规则:值` 列表。
- `waiting`、`spectator-loc`：等待和观战标点。
- `Team`：所有队伍的颜色、出生点、床、NPC 和岛屿生成器。
- `generator.Diamond`、`generator.Emerald`：中央生成器列表。

`Team.<队伍>.Color` 的青色填写 `CYAN`。2.10.11 起旧值 `AQUA` 会自动迁移为 `CYAN`，对应床、羊毛、玻璃和陶瓦均使用原版 `CYAN_*` 材质。

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
