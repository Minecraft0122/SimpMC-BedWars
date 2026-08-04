# SimpMC-BedWars

面向现代 Paper 服务端维护的起床战争插件。玩家需要保护己方床、摧毁敌方床，并淘汰所有对手；床被摧毁后将无法再次复活。

> 当前分支仅支持 **Paper 1.21.11 + Java 21**。不支持 Spigot、Folia、其他 Minecraft 版本或旧版 Java。

## 文档

- [玩家使用说明](docs/zh_CN/player-guide.md)
- [完整中文教程](docs/zh_CN/README.md)
- [安装、更新与服务器模式](docs/zh_CN/installation.md)
- [从零创建竞技场](docs/zh_CN/arena-setup.md)
- [全部配置文件说明](docs/zh_CN/configuration.md)
- [命令与权限](docs/zh_CN/commands-permissions.md)
- [API 与附属插件开发](docs/zh_CN/api-development.md)
- [常见问题与排错](docs/zh_CN/troubleshooting.md)
- [更新记录](CHANGELOG.md)
- [BUG 记录](BUGS.md)
- [自动发布的 Releases](https://github.com/Minecraft0122/SimpMC-BedWars/releases)

## 主要功能

- 支持 MULTIARENA、SHARED、BUNGEE 和 BUNGEE-LEGACY 运行模式。
- 提供竞技场选择菜单、加入告示牌、Citizens NPC 和命令加入方式。
- 支持单独配置竞技场分组、队伍、生成器、商店、团队升级、陷阱和初始物品。
- 默认每 1 秒生成 2 铁、每 4 秒生成 2 金，并支持在 `generators.yml` 中按竞技场分组覆盖。
- 内置中文语言、玩家独立语言、计分板、TAB、历史战绩和等级系统。
- 游戏内 TAB 只把玩家名染成所属队伍颜色，不再重复显示队伍名称；玩家头顶名牌同步使用同一队伍颜色，最终淘汰者和纯旁观者在 TAB 中使用原版观察者样式。
- 游戏进行中的 TAB 页首显示本局已进行时间，存活、淘汰和旁观视角保持一致。
- 提供开局前真实队伍选择、固定队友邀请与自动均衡分队；每张竞技场可分别设置全场最低开局人数和每队容量，并保证正常开局至少存在两支非空队伍。
- 商店护甲固定只出售护腿和靴子，保留本队颜色的皮革头盔与胸甲。
- 等待竞技场中的玩家可用 `/bw invite <玩家>` 邀请大厅玩家加入同一场匹配，邀请带 30 秒有效期和点击确认。
- 支持 Shift+左键把主手物品快速存入本队箱子，以及 Shift+右键在商店中按背包货币买到上限。
- 火球普通初速度约为 1.6 格/tick；按住潜行键投掷时提高到约 2.4 格/tick，每颗火球在 200–300 格之间随机最大射程。
- 支持普通掉线后 30 秒内重连；被服务器或反作弊踢出时立即放弃本局，不能用重连占位阻止比赛结束。
- 自动跟踪玩家放置的方块；所有权以事件结束后的真实世界状态为准，玩家方块即使位于地图保护区内也始终可破坏，并在每局结束后完整恢复竞技场世界。
- 所有专用 BedWars 世界始终保持在正午 6000 tick 与晴天；SHARED 模式只锁定竞技场、设置世界和明确配置的 BedWars 大厅。
- 插件启动、世界初始化、世界加载和竞技场初始化都会对全部地图强制关闭 Locator Bar。
- 大厅、出生点、观战点及 NPC 朝向统一吸附到最近的 90 度 yaw，pitch 固定为 0。
- 支持 PlaceholderAPI、Vault、Citizens、Parties 和 PartyAndFriends 等可选依赖。
- 提供公开 API，附属插件可以查询竞技场、控制大厅状态、操作预组队和登记玩家放置方块。

## 安装

1. 准备 Paper 1.21.11 服务端和 Java 21。
2. 将 `SimpMC-BedWars-版本.jar` 放入 `plugins` 目录。
3. 首次启动生成配置后完整停服。
4. 按[安装教程](docs/zh_CN/installation.md)配置服务器模式和大厅。
5. 重新启动并使用 `/bw setupArena <世界名>` 创建竞技场。

插件会自动迁移旧版配置：升级前创建 `.bak` 备份，删除已废弃字段，补充新字段和中文注释。不要使用 `/reload` 或插件热重载。

默认分支的每次提交都必须先通过完整 Maven 验证，随后自动上传 JAR 和 `SHA256SUMS.txt` 到 GitHub Releases。标签格式为 `v版本号-提交前八位`，因此即使同一插件版本只有文档或工作流更新，也不会发生标签冲突；工作流重跑不会重复创建 Release。

## Vault 经济支持

Vault 是经济接口桥接层，本身不会创建玩家余额。要启用金币奖励或 Vault 货币购买，必须同时安装：

1. Vault；
2. 一个向 Vault 注册经济服务的经济插件，例如带经济模块的 EssentialsX；
3. SimpMC-BedWars。

插件以 Bukkit 服务注册表为准，不再依赖名为 `Vault` 的固定插件名称，并会监听服务的延迟注册与注销。控制台显示“已检测到 Vault API，但没有已注册的经济服务”时，缺少的是经济服务提供者，不是 Vault 桥接层。

## 运行模式

- `MULTIARENA`：一个 Paper 实例承载大厅和多张竞技场，适合独立小游戏服。
- `SHARED`：与其他玩法共享实例，玩家离开竞技场后恢复进入前状态。
- `BUNGEE`：代理网络的多竞技场自动扩容模式，需要匹配的代理端接入方案。
- `BUNGEE-LEGACY`：一张竞技场占用一个后端实例的传统代理模式。

## 自行构建

```bash
git clone https://github.com/Minecraft0122/SimpMC-BedWars.git
cd SimpMC-BedWars
mvn -B clean verify
```

构建产物位于 `bedwars-plugin/target/SimpMC-BedWars-版本.jar`。

## 参与贡献

提交问题前请阅读 [BUGS.md](BUGS.md)，并提供插件版本、Paper 构建、Java 版本、复现步骤、完整日志和相关竞技场配置。提交代码前请阅读[贡献指南](CONTRIBUTING.md)，所有改动都应通过 `mvn -B clean verify`。

## 第三方组件

- [bStats](https://bstats.org/getting-started/include-metrics)
- Paper/Bukkit 原生计分板接口
- [Commons IO](https://commons.apache.org/proper/commons-io/)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [SLF4J](https://www.slf4j.org/)

本项目采用 [GPL-3.0](LICENSE) 许可证。第三方组件遵循各自许可证。
