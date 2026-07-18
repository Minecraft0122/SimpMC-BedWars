# 全部配置文件说明

插件会把管理员可编辑的 YAML 放在 `plugins/SimpMC-BedWars`。`config-version` 由迁移器管理，不要手动降低。新版本会自动备份、补字段并写入中文注释。

## config.yml

主配置，包含：

- `serverType`：MULTIARENA、SHARED 或 BUNGEE。
- `language`、禁用语言列表：默认语言及玩家可选语言。
- `chat`：全局聊天和插件聊天格式。
- `scoreboard-settings`：大厅/游戏计分板、TAB、队友颜色、血量和刷新周期。
- `rejoin-time`：掉线重连窗口，默认 30 秒。
- `countdowns`：开局、床消失、决战、结束和复活倒计时。
- `party-settings`：内部及外部组队集成。
- `tnt-jump-settings`、`blast-protection`、`tnt-prime-settings`：TNT 参数。
- `fireball-settings`：火球速度、爆炸、击退、冷却和伤害。
- `database`：MySQL；关闭时使用 SQLite。
- `performance-settings`：Paper 传送、资源旋转等优化。
- `lobby-items`、`pre-game-items`、`spectator-items`：不同阶段的命令物品。
- `arena-gui`、`stats-gui`：竞技场选择和历史战绩菜单。
- `start-items-per-group`：每个竞技场分组的默认开局物品。
- `allowed-commands`：游戏内允许的命令。
- `game-end`：淘汰玩家和最佳数据展示。

刷新周期通常以 tick 为单位；20 tick 约等于 1 秒。倒计时和重连时间以秒为单位。

## Arenas/<竞技场>.yml

每张地图一份。主要节点：

- `group`、`display-name`、`minPlayers`、`maxInTeam`。
- `allowSpectate`、`worldBorder`、`y-kill-height`、最大建造高度。
- 出生、商店、升级、生成器保护半径。
- `island-radius`：自动找床、治疗池和陷阱检测范围。
- `game-rules`：`规则:值` 列表。
- `waiting`、`spectator-loc`：等待和观战标点。
- `Team`：所有队伍的颜色、出生点、床、NPC 和岛屿生成器。
- `generator.Diamond`、`generator.Emerald`：中央生成器列表。

坐标由插件生成，建议使用设置命令，不要手写。方向字段与坐标分开，避免破坏方块中心格式。

## generators.yml

控制铁、金、钻石、绿宝石的：

- `delay`：生成间隔秒数。
- `amount`：每次生成数量。
- `spawn-limit`：单生成点地面物品上限。
- `start`：II/III 级启用时间。
- `stack-items`：合并掉落物以减少实体。

复制 `Default` 并改为竞技场 `group` 名称，可为不同模式设置独立速度。

## shop.yml

商店由分类、商品、tier 和实际发放物品组成：

```yaml
example-category:
  category-slot: 1
  category-item:
    material: EMERALD
    amount: 1
  category-content:
    example-item:
      content-slot: 19
      is-permanent: false
      tiers:
        tier1:
          item:
            material: TNT
            amount: 1
          cost: 4
          currency: gold
```

货币支持 `iron`、`gold`、`diamond`、`emerald`；安装 Vault 后可按现有格式使用经济货币。Material、附魔和药水名称必须适用于 Paper 1.21.11。

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

Vault 金币奖励。没有 Vault 经济插件时文件仍会生成，但奖励不会生效。

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
