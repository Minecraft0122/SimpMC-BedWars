# 安装、更新与服务器模式

## 系统要求

- Paper 1.21.11，不能使用 Spigot 或 Folia。
- Java 21 或更新的兼容版本。
- 地图较多时建议使用 SSD；内部地图恢复会压缩和解压完整世界。
- 建议至少保留一份服务端离线备份。

## 获取自动发布版本

默认分支的每次提交都会先执行完整 Maven 验证；只有构建和测试全部成功，才会自动创建 [GitHub Release](https://github.com/Minecraft0122/SimpMC-BedWars/releases)，并附带可直接安装的 JAR 与用于校验下载完整性的 `SHA256SUMS.txt`。

Release 标签格式为 `v版本号-提交前八位`。同一版本号发生纯文档或构建流程更新时仍会创建独立 Release，不会覆盖旧提交的产物；重新运行同一次工作流则不会重复发布。

## 首次安装

1. 从项目构建产物中取得 `SimpMC-BedWars-版本.jar`。
2. 放入服务端 `plugins` 目录。
3. 启动服务器，看到插件成功启用后执行 `stop`。
4. 编辑 `plugins/SimpMC-BedWars/config.yml`。
5. 再次启动服务器。

最常修改的首次配置：

```yaml
serverType: MULTIARENA
language: zh_cn
debug: false
```

## 服务器模式

### MULTIARENA

一个 Paper 实例承载主大厅和多个竞技场。支持选择菜单、告示牌、Citizens NPC 和命令加入。插件会保护大厅，玩家每次进入大厅都被设置为冒险模式。

适合：单服小游戏、测试服、中小型网络的独立 BedWars 子服。

### SHARED

与其他玩法共享同一实例，玩家离开竞技场后恢复进入前的位置和物品。通常通过命令加入，不应把整个服务器当作 BedWars 专用大厅。

### BUNGEE

面向代理网络和自动扩容。竞技场节点、服务器 ID、大厅地址及重启策略在 `bungee-settings` 中配置，并需要匹配的代理端接入方案。

如果 BedWars 服务器位于 BungeeCord 或兼容代理后方，无论使用 MULTIARENA 还是 BUNGEE 模式，都应把 `config.yml` 的 `lobbyServer` 设置为代理配置中的大厅服务器名称，例如 `hub`。该值不是地址、端口或 MotD；必须对应代理服务器列表中的键：

```yaml
# plugins/SimpMC-BedWars/config.yml
lobbyServer: hub
```

Velocity 的 `velocity.toml` 至少需要以下对应配置：

```toml
[servers]
hub = "127.0.0.1:25566"
bedwars = "127.0.0.1:25567"

[advanced]
bungee-plugin-message-channel = true
```

BungeeCord/Waterfall 的 `config.yml` 则应有同名服务器键：

```yaml
servers:
  hub:
    address: 127.0.0.1:25566
  bedwars:
    address: 127.0.0.1:25567
```

玩家必须连接 BungeeCord/Velocity 的监听地址，不能直接连接 BedWars 后端 Paper 端口；后端端口也应通过防火墙限制为只允许代理访问。大厅“回到主大厅”红床会先用官方 `GetServer` 和 `GetServers` 子通道检查代理连接及服务器名称，再通过 `Connect` 子通道切换。配置错误时玩家和控制台都会得到明确提示。

## 可选依赖

- PlaceholderAPI：在消息、计分板等位置解析扩展占位符。
- Vault + 经济插件：启用 `rewards.yml` 金币奖励及 Vault 货币购买。Vault 只是桥接层，不能单独提供余额。
- Citizens：创建加入竞技场的 NPC。
- Parties / PartyAndFriends：接入外部组队系统。

### 正确安装 Vault 经济支持

1. 安装 Vault。
2. 安装一个会向 Vault 注册经济服务的插件，例如带经济模块的 EssentialsX。
3. 安装 SimpMC-BedWars，并完整重启服务端。
4. 在控制台确认出现 `已接入 Vault 经济服务`，后面会同时显示服务名称和提供插件。

插件直接查询 Bukkit 服务注册表，并监听经济服务的延迟注册和注销，不依赖固定的插件文件名。如果控制台显示 `已检测到 Vault API，但没有已注册的经济服务`，说明 Vault 已经可用，但没有经济插件提供玩家余额；继续重复安装 Vault 无法解决此问题。

## 更新插件

1. 完整停止服务器。
2. 备份 `plugins/SimpMC-BedWars` 和竞技场世界。
3. 替换旧 JAR，不要同时保留两个版本。
4. 启动服务器并查看配置迁移日志。
5. 插件会为需要升级的 YAML 创建类似 `.v7.bak` 的备份，再写入新字段和中文注释。
6. 检查控制台后完整测试大厅、加入、开局、死亡、复活和结束恢复。

不要删除刚生成的备份，确认新版本稳定后再按自己的备份策略归档。

## 自行构建

```bash
git clone https://github.com/Minecraft0122/SimpMC-BedWars.git
cd SimpMC-BedWars
mvn -B clean verify
```

最终插件位于 `bedwars-plugin/target/SimpMC-BedWars-版本.jar`。
