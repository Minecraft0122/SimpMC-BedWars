# SimpMC-BedWars 完整中文教程

本教程对应 SimpMC-BedWars `2.10.x`，目标运行环境是 **Paper 1.21.11 + Java 21**。其他 Minecraft 版本、Spigot、Folia 和旧版 Java 不在支持范围内。仓库面向维护者和服主的说明、Issue/PR 模板及工作流显示文本均使用中文。

## 文档导航

1. [安装、更新与服务器模式](installation.md)
2. [从零创建竞技场](arena-setup.md)
3. [全部配置文件说明](configuration.md)
4. [命令与权限](commands-permissions.md)
5. [API 与附属插件开发](api-development.md)
6. [代码质量审计与 API 设计说明](code-quality.md)
7. [安全审计与部署加固](security-audit.md)
8. [常见问题与排错](troubleshooting.md)

## 最短开服流程

1. 安装 Java 21，创建 Paper 1.21.11 服务端。
2. 将插件 JAR 放入 `plugins`，启动并等待配置生成。
3. 停服，将 `plugins/SimpMC-BedWars/config.yml` 的 `language` 改为 `zh_cn`，确认 `serverType`。
4. MULTIARENA 模式进入主大厅执行 `/bw setLobby`。
5. 把竞技场世界文件夹放到服务端世界目录，执行 `/bw setupArena <世界名>`。
6. 按聊天中的引导设置等待点、队伍、出生点、NPC 和生成器。
7. 执行 `/bw save`，然后 `/bw enableArena <世界名>`。
8. 使用 `/bw join <世界名>` 或 `/bw gui` 测试完整一局。

## 重要约定

- 坐标标点保存为方块中心 `x.5,y.0,z.5`；玩家出生类位置的朝向单独保存。
- `Spawn` 同时用于出生和复活，不需要配置第二份 `Respawn`。
- 每局至少需要两名玩家以及两支非空队伍。
- 配置自动升级前会生成 `.bak` 备份；备份文件不会被当作语言或竞技场加载。
- 不要使用 `/reload` 或插件热重载。替换 JAR 或修改关键配置后请完整重启。
- 从 `2.10.0` 之后只递增最后一位版本号。

## 玩家快捷操作

- 游戏中按住 Shift 左键点击本队岛屿的箱子，会把主手物品直接存入；箱子容量不足时只存入可容纳的部分。永久装备和出生物品不会被存入。
- 在商店 GUI 中按住 Shift 右键点击普通商品，会花费背包内对应货币购买可负担的最大整数数量。
- 商店中的 Shift 左键仍用于添加或移除快速购买槽位；永久、多级、命令、免费和 Vault 商品每次只购买一次。
