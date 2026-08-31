# 安装、更新与服务器模式

## 系统要求

- Paper 1.21.11 或 Paper 26.2，不能使用 Spigot 或 Folia。
- Paper 1.21.11 使用 Java 21 或更新的兼容版本；Paper 26.2 必须使用 Java 25。
- 地图较多时建议使用 SSD；内部地图恢复会压缩和解压完整世界。
- 建议至少保留一份服务端离线备份。

Paper 26.2 会把自定义世界运行数据放入主 `level` 目录下的 `dimensions`，但 SimpMC-BedWars 强制把竞技场原图和唯一缓存来源保存在 1.21.11 使用的服务端根目录 `<世界名>`；维度目录只作为 Paper 加载期间的运行副本，设置会话保存后才同步回原图并更新 `plugins/SimpMC-BedWars/Cache/<世界名>.zip`。有效原图和 ZIP 必须同时包含根级 `level.dat` 与 `region/`；只有维度内容、嵌套 `level.dat` 或仅有 `level.dat_old` 的目录/缓存会被拒绝，不会自动转换为另一种地图格式。不要安装 Multiverse-Core 作为必需依赖，也不要手工移动竞技场原图。

## 获取自动发布版本

默认分支的每次提交都会先执行完整 Maven 验证；只有构建和测试全部成功，才会自动创建 [GitHub Release](https://github.com/Minecraft0122/SimpMC-BedWars/releases)，并附带可直接安装的 JAR 与用于校验下载完整性的 `SHA256SUMS.txt`。

Release 标签格式为 `v版本号-提交前八位`。同一版本号发生纯文档或构建流程更新时仍会创建独立 Release，不会覆盖旧提交的产物；重新运行同一次工作流则不会重复发布。

## 首次安装

1. BUNGEE 网络分别取得 `SimpMC-BedWars-Lobby-版本.jar` 和 `SimpMC-BedWars-Arena-版本.jar`；大厅服只安装 Lobby 包，竞技场子服只安装 Arena 包。单服 `MULTIARENA`/`SHARED` 应使用对应的单体兼容发行包。
2. 将角色包放入对应服务端的 `plugins` 目录。
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

该模式视为专用 BedWars 实例，全部已加载、新建和恢复世界都会固定在正午 `6000 tick` 并保持晴天。

适合：单服小游戏、测试服、中小型网络的独立 BedWars 子服。

### SHARED

与其他玩法共享同一实例，玩家离开竞技场后恢复进入前的位置和物品。通常通过命令加入，不应把整个服务器当作 BedWars 专用大厅。

该模式只锁定已启用/待加载竞技场、地图设置世界和 `/bw setLobby` 明确保存的 BedWars 大厅；生存服等无关世界的时间与天气保持原样。

### BUNGEE

面向代理网络和自动扩容。6.0.0 起 BUNGEE 发布为两个独立 JAR：`SimpMC-BedWars-Lobby-<版本>.jar` 只负责大厅、远程竞技场目录和跨服调度；`SimpMC-BedWars-Arena-<版本>.jar` 只负责地图副本、对局和状态上报。两个包会固定 `serverType: BUNGEE` 及自身角色；旧的 `node-role` 冲突值会被写回正确值。两种角色都应连接同一个 MySQL 数据库，但只有 ARENA 节点记录本地对局统计。

该模式同样视为专用 BedWars 节点，实例内全部世界固定正午与晴天。

每个 ARENA 子服建议只承载一张地图；`bungee-settings.arena-template` 填写 `Arenas/<地图>.yml` 的文件名（可省略 `.yml`），`auto-scale-clone-limit` 控制该子服同时运行的同名地图副本上限。玩家通过大厅按分组随机选择处于 waiting/starting 且有容量的副本，指定竞技场时则使用指定运行实例。副本由插件从地图缓存自动复制，结束后按既有重启策略回收，不需要为每个副本手写 YAML。

#### 大厅服配置

大厅服不加载 `Arenas` 目录，也不运行竞技场世界；`lobby-listen` 是 ARENA 子服连接的 TCP 监听地址，不是代理端口。大厅服数量建议保持单活；当前预约表在大厅进程内存中，多个大厅同时调度会重复预约，后续可改用 Redis/SQL 租约。两个 JAR 使用相同的 Bukkit 插件名，因此从旧单体包升级时可继续使用 `plugins/SimpMC-BedWars` 配置目录；每个 Paper 实例只能安装符合自身职责的一个角色包。

```yaml
serverType: BUNGEE
bungee-settings:
  node-role: LOBBY
  server-id: bw-lobby-01
  lobby-listen:
    host: 10.0.0.10
    port: 2019
  socket-secret: "请替换为大厅与所有竞技场共用的随机长字符串"

database:
  enable: true
  host: 10.0.0.20
  port: 3306
  database: simpmc_bedwars
  user: bedwars
  pass: "请替换为实际密码"
  ssl: true
```

#### 竞技场子服配置

每个子服都要设置不同的 `server-id`，`proxy-server` 必须是 BungeeCord/Velocity `[servers]` 中该后端的键名；所有节点的 `database` 指向同一个 MySQL，`socket-secret` 与大厅完全一致：

```yaml
serverType: BUNGEE
bungee-settings:
  node-role: ARENA
  server-id: bw-arena-castle-01    # 其他子服使用不同 ID
  proxy-server: bw-arena-castle-01 # 代理中的后端键名
  arena-template: castle            # 对应 Arenas/castle.yml
  auto-scale-clone-limit: 5         # 本子服同名地图最多同时运行的副本数
  lobby-sockets:
    - 10.0.0.10:2019                # 大厅服 lobby-listen 地址
  socket-secret: "请替换为与大厅相同的随机长字符串"

database:
  enable: true
  host: 10.0.0.20
  port: 3306
  database: simpmc_bedwars
  user: bedwars
  pass: "请替换为实际密码"
  ssl: true
```

复制多个 ARENA 子服时，只需为每台服务器复制同一张地图配置和缓存，修改 `server-id`、`proxy-server` 与代理地址；想承载另一张地图则使用另一台子服并修改 `arena-template`。旧版 BUNGEE 配置若不填写 `node-role`，仍按 ARENA 节点处理；`BUNGEE_LEGACY` 仍保留单实例兼容路径。6.0.0 的专用大厅/竞技场包只用于 BUNGEE 网络；继续使用单服 `MULTIARENA` 或 `SHARED` 时应选择与该模式兼容的历史单体发行包。

大厅按以下顺序调度：筛选新鲜心跳、按 `group:` 或 `arena:` 选择、锁定一个空闲副本、向子服发送预加载请求，收到所有队员的确认后才通过代理发送 `Connect`。预加载超时或代理消息无法发出会释放预约；代理插件消息本身没有回执，因此实际切服失败仍应检查代理日志。

常用加入方式：`/bw join random`、`/bw join group:<组名>`、`/bw join arena:<运行时竞技场名>`；大厅 `/bw gui` 会显示远程目录，左键加入 waiting/starting 副本，右键观战允许观战的 playing 副本。

竞技场与大厅套接字是受信任内网的明文 TCP 控制通道，必须使用随机 `socket-secret`、防火墙只允许 ARENA 子服访问 `lobby-listen.port`，禁止直接暴露公网。大厅接受旧协议版本用于兼容，但只有当前协议版本的节点会进入新调度目录。

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

玩家必须连接 BungeeCord/Velocity 的监听地址，不能直接连接 BedWars 后端 Paper 端口；后端端口也应通过防火墙限制为只允许代理访问。大厅“回到主大厅”红床只会立即、静默地发送 `Connect <lobbyServer>`，不会先查询代理节点或把代理配置、服务器列表和连接诊断显示给玩家。若没有切服，请检查 BedWars 后端控制台和代理日志。

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

最终插件位于 `bedwars-lobby/target/SimpMC-BedWars-Lobby-版本.jar` 和 `bedwars-arena/target/SimpMC-BedWars-Arena-版本.jar`。
