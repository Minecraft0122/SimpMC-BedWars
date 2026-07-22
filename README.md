# SimpMC-BedWars

面向现代 Paper 服务端维护的起床战争插件。玩家需要保护己方床、摧毁敌方床，并淘汰所有对手；床被摧毁后将无法再次复活。

> 当前分支仅支持 **Paper 1.21.11 + Java 21**。不支持 Spigot、Folia、其他 Minecraft 版本或旧版 Java。

## 文档

- [玩家完整指南](docs/zh_CN/player-guide.md)
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
- 提供开局前邀请组队与自动分队，并保证正式开局至少存在两支非空队伍。
- 等待竞技场中的玩家可用 `/bw invite <玩家>` 邀请大厅玩家加入同一场匹配，邀请带 30 秒有效期和点击确认。
- 支持 Shift+左键把主手物品快速存入本队箱子，以及 Shift+右键在商店中按背包货币买到上限。
- 支持掉线后 30 秒内重连；超时后按离开处理。
- 自动跟踪玩家放置的方块，并在每局结束后完整恢复竞技场世界。
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
