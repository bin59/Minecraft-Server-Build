# Velocity 多服（代理）+ Multiverse 多世界 · 迁移方案

> **本文职责**：讲清楚「为什么这样分层、从单服迁到多服要做哪些事」，是**通用迁移教程**。
> 本服当前实跑的端口、密钥、防火墙配置**不在本文**，见 [多服网络与端口架构.md](./多服网络与端口架构.md)（唯一真源）；
> 玩家在游戏内怎么跨服见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md)；

本文档面向准备把现有单服扩展为多子服网络的服主，给出从单端无代理迁移到 Velocity 代理的完整落地步骤：代理选型、`velocity.toml` 配置、后端子服对接、Geyser/Floodgate 迁移、权限与经济跨服同步、现有插件逐项适配，以及安全基线。

> **一句话**：在现有单服（Leaf + Yggdrasil 外置登录 + Geyser 基岩互通）前面加一层
> **Velocity 代理**，把"一台服务器"升级成"一个网络"，玩家从一个地址进入，
> 可无缝穿梭多个子服（大厅 / 生存 / 小游戏 / 建筑……），换服不掉线。

> ⚠️ **示例值说明**：文中的 `velocity.toml`、目录结构、端口、密钥都是**教学示例**，用于说明配置结构。
> 本服已实装多服，实际取值一律以 [多服网络与端口架构.md](./多服网络与端口架构.md) 为准
> （Java `55552`、基岩 `19134`、后端 `30002~30004` 等），不要照抄本文示例。

> 📌 **版本基线**：本服实测为 **Velocity 4.3.1-45 / Java 25**；4.x 新增开关与外置登录注意事项已写入本文 §3.1 / §5.5 / §6.4。

### 0. 先厘清：Velocity 与 Multiverse 是两层，不是二选一

本文档标题是「Velocity + Multiverse」，但两者**不是同类、不是替代关系**。很多人误以为"多世界就要上代理"或"上代理就不用多世界"，先对齐认知：

|              | **Multiverse-Core**                                | **Velocity**                                             |
| ------------ | -------------------------------------------------- | -------------------------------------------------------- |
| 它是什么     | 服务端**插件**（Bukkit/Paper 系）                  | 独立的**代理服务器**（独立进程）                         |
| 跑在哪       | 塞进单个服务端的 `plugins/`                        | 单独 Java 进程，夹在「客户端 ↔ 多个服务端」之间          |
| 解决什么     | **一个服务端里开多个世界**（资源界/小游戏/建筑区） | **把多个独立服务端连成一个网络**（大厅/生存/空岛互相跳） |
| 类比         | 一栋楼里隔多个房间                                 | 多栋楼共用一个大门进出                                   |
| 26.3 支持    | ✅ 5.8.2 明确支持 1.18.2→26.3                      | ✅ 明确支持 1.7.2→26.3（自身需 Java 25）                 |
| 部署复杂度   | 低：丢 jar、重启                                   | 高：独立进程 + 每个后端改 forwarding                     |
| 基岩互通影响 | 不影响，照常走服内 Geyser                          | 需重规划 Geyser/Floodgate 部署（见 §6）                  |

**结论**：

- 想要"同一个服里多个世界" → 用 **Multiverse**（本文 §11），零架构改动。
- 想要"多个独立服、一个 IP 进出" → 用 **Velocity**（本文 §1–§10）。
- **两者常配合使用**：大服典型架构是 Velocity 管"服与服之间"，每个后端子服内部再用 Multiverse 管"世界与世界之间"。它们装在不同地方（Multiverse 装子服、Velocity 是独立进程），互不冲突。

> **本服的分工**：Velocity 管服与服；survival / minigames 子服内部用 Multiverse 管多世界；
> skyblock 用 BentoBox 管岛屿世界，**不装 Multiverse**。三者边界见
> [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) §1。

## 1. 选型结论：为什么是 Velocity

| 方案           | 开发状态          | 性能     | 转发安全             | 结论          |
| -------------- | ----------------- | -------- | -------------------- | ------------- |
| **BungeeCord** | 维护（遗留）      | 中       | legacy 明文，可伪造  | ❌ 过时       |
| **Waterfall**  | **已停更（EOL）** | 中       | legacy / 部分 modern | ❌ 官方已弃用 |
| **Velocity**   | **活跃维护**      | **最高** | modern 加密 MAC 校验 | ✅ **采用**   |

关键优势（来源：[AsiaGB 对比](https://asiagb.com/content/minecraft-velocity-network-en.html)）：

1. **PaperMC 团队官方出品**，与你的 Leaf（Paper 分支）同源，兼容性最好。
2. **modern 转发**：玩家信息用二进制 + MAC 密钥加密，后端校验密钥后才接受连接，
   彻底杜绝"绕过代理直连后端、伪造管理员名字"的攻击。BungeeCord 的 legacy
   转发是明文，这是历史最大的安全漏洞。
3. **Java 25**（Velocity 4.x 最低要求；3.3/3.4 时代才是 Java 17+ / 推荐 21），本机已装 Java 25，与 §0 表格一致。
4. 单代理即可承载数千在线，天然支持负载均衡与分区隔离——重启某个子服不影响其他人。

### 1.1 为什么你的场景特别适合用 Velocity

你的架构里有 **Yggdrasil 外置登录** 和 **Geyser 基岩互通** 两条认证链路，
单服时它们"挤"在 Leaf 一个进程里。一旦上了代理，**认证责任全部上移到 Velocity**，
后端彻底变"纯游戏逻辑"：

| 认证链路      | 单服现状                                   | Velocity 架构                                                  |
| ------------- | ------------------------------------------ | -------------------------------------------------------------- |
| Java 外置登录 | Leaf 用 `-javaagent:authlib-injector` 承担 | **Velocity 承担**，后端只验 forwarding 密钥                    |
| 基岩互通      | Leaf 内嵌 Geyser + Floodgate               | **Geyser + Floodgate 迁到 Velocity**，基岩玩家经代理进任意子服 |
| 皮肤          | SkinsRestorer 在各子服                     | SkinsRestorer 仍放后端（可跨服同步，见 §8）                    |

好处：**子服可以随便加**，每一台都只跑同一种玩法，互不干扰；新增子服时
只需在 Velocity 注册一行地址，无需重复配置认证。

---

## 2. 下载与安装

### 2.1 下载 Velocity

从 PaperMC 官方下载页获取稳定版（**2026-11 实测：Velocity 4.3.1-45**，默认端口 25565）：

- 官网：<https://papermc.io/downloads/velocity>
- **下载 API 已迁到 Fill v3**：`https://fill.papermc.io/v3/projects/velocity/versions/<版本>/builds/latest`
  （响应 JSON 的 `downloads` 段给出 jar 直链）。⚠️ 旧文里的
  `api.papermc.io/v2/projects/velocity/...` 自 2026-07 起返回 **410 Gone**，脚本里别再用。
- 项目文档：<https://docs.papermc.io/velocity/>

**务必下载 `velocity-x.y.z.jar`（代理端），不要下成其余构建产物。**
Velocity **4.x 要求 Java 25**（3.x 才是 Java 17+）。

### 2.2 目录规划（建议独立文件夹）

```
C:\mc_serve\
├── velocity\                  ← 新建：Velocity 代理（本文主角）
│   ├── velocity-4.3.1-45.jar
│   ├── start.bat
│   ├── velocity.toml          ← 首次启动自动生成
│   ├── forwarding.secret      ← 自动生成，需复制给所有子服
│   └── plugins\               ← Geyser / Floodgate / LuckPerms（可选）
│
├── survival\                  ← 现有 Leaf 服（作为"生存子服"）
│   └── config\paper-global.yml   ← 开启 velocity 支持
├── lobby\                     ← 将来可加"大厅子服"
└── ...
```

### 2.3 首次启动

```powershell
# start.bat
@echo off
title Velocity
java -Xms512M -Xmx512M -XX:+UseG1GC -jar velocity-4.3.1-45.jar
pause
```

双击后会自动生成 `velocity.toml` 与 `forwarding.secret`，然后 Ctrl+C 关闭再编辑。

---

## 3. velocity.toml 完整配置

### 3.1 完整注释版（教学示例）

Velocity 的核心配置是 **TOML** 格式（不是 BungeeCord 的 YAML）。下面给出适配
"Yggdrasil 外置登录 + Geyser" 的完整注释版，**仅用于说明配置结构与各开关含义**：

> ⚠️ 示例中的 `bind = "0.0.0.0:55551"`、`[servers]` 只列三台、`try` 含 `lobby` 都是教学取值；
> 本服实际为 `55552`、三后端 `30002~30004`、`try = ["survival"]`，见
> [多服网络与端口架构.md](./多服网络与端口架构.md) §4.1。
> 本服还额外启用了 `[commands]` 命令转发段与 `bungee-plugin-message-channel`，那是游戏内跨服入口的前置，
> 见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) §3。

```toml
# ========== velocity.toml ==========

# 代理对外监听的地址。0.0.0.0 = 所有网卡。
# ⚠️ 端口建议沿用你现在的对外入口（如 55551）或标准 25565，并做好映射/域名。
bind = "0.0.0.0:55551"

# 服务器列表里显示的 MOTD（支持 MiniMessage 语法，不是 & 颜色码）
motd = "<gradient:#FFD700:#FF8C00><bold>南瓜国际服</bold></gradient>\n<gray>生存 | 大厅 | 小游戏</gray>"

# 显示的最大玩家数（纯视觉，不影响实际）
show-max-players = 500

# ★★★ 外置登录（Yggdrasil）必须为 true ★★★
# Velocity 会用 authlib-injector 对玩家做 Yggdrasil 认证，
# 后端全部交由 forwarding 密钥校验，不再各自验证。
online-mode = true

# ★★★ Velocity 4.x 新增：外置登录必须设为 false ★★★
# 4.x 默认强制校验 Mojang 公钥签名，而 Yggdrasil 外置登录没有该签名，
# 开着会把玩家全部踢掉。3.x 无此选项；从 3.x 升级到 4.x 时务必补上。
force-key-authentication = false

# ★ 转发模式：modern 最安全，Leaf/Paper 原生支持
# ⚠️ 绝不能改成 "none"：那样后端拿到的是离线模式 UUID，全体玩家数据"换号"。
player-info-forwarding-mode = "modern"

# forwarding.secret 的路径
forwarding-secret-file = "forwarding.secret"

# 是否在服务器列表响应中显示子服信息（建议 true，利于排查）
show-ping-requests = true

[servers]
# 子服列表：名字 = "地址:端口"
#   端口必须与改后的 server.properties 的 server-port 一致
lobby = "127.0.0.1:30001"
survival = "127.0.0.1:30002"
minigames = "127.0.0.1:30003"

# 玩家首次连接时尝试进入的子服（按优先级依次尝试）
try = ["survival", "lobby"]

# 按"子服务器名称"可执行的自定义指令（可选，实现 /lobby /survival 快速换服）
[server-lobby]
enabled = true

[forced-hosts]
# 若用 SRV 记录分流到不同子服（高级用法），此处留空即可

[advanced]
# 是否把代理的指令补全同步给客户端（建议 true，/server Tab 才能补全）
announce-proxy-commands = true
```

### 3.2 三处必须对齐的配置（概念）

| 配置项                | 位置                                          | 说明                                                                             |
| --------------------- | --------------------------------------------- | -------------------------------------------------------------------------------- |
| `bind`                | `velocity/velocity.toml`                      | 代理对外端口，玩家连这个                                                         |
| 子服 `server-port`    | 各子服 `server.properties`                    | 代理连接子服用，必须是**只绑回环、彼此不重复、且不与其它本地服务冲突**的内网端口 |
| Geyser `bedrock.port` | `velocity/plugins/Geyser-Velocity/config.yml` | 基岩监听端口；纯 NetherNet 信令走 TCP、媒体走 UDP，**两个协议都要放行**          |

- `[servers]` 里的地址端口必须与各子服 `server.properties` 逐字一致，改名 / 改端口要同时改两处。
- 每份 YggdrasilOfficialProxy agent 都会起一个本地 HTTP 服务，**每个 JVM 的 `server.port` 必须互不相同**。
- 最常见的翻车点是沿用旧的对外大端口，导致代理与子服或本地 HTTP 服务争端口。

> **本服实际取值**（Java 入口、基岩入口、三后端、RCON、四个 YOP 端口、RSPM 自托管 HTTP 端口）
> 统一见 [多服网络与端口架构.md](./多服网络与端口架构.md) §3，本文不再维护第二套端口表。

---

## 4. 网络与端口规划（原则）

### 4.1 暴露面与隔离原则

玩家与基岩客户端只连 **Velocity**，后端全部 **只绑 `127.0.0.1` 或内网 IP，绝不对外**。

1. **唯一入口**：代理是对外暴露的唯一进程，Java 与基岩各一个监听；后端子服、RCON、认证 agent、面板一律不进公网安全组。
2. **防直连**：所有子服 `server.properties` 设 `server-ip=127.0.0.1`（或内网 IP），并从云安全组 / 防火墙的对外放行里移除。否则有人直连后端，配合 `online-mode=false` 就能伪造任意玩家（含 OP）进入——这是最致命的漏洞。
3. **modern 转发兜底**：后端启用 `proxies.velocity` 后，绕过代理直连的连接因缺代理签名被拒绝。
4. **本地服务端口互不冲突**：每份 YggdrasilOfficialProxy agent、RSPM 自托管 HTTP、各子服端口都不能互相撞号，先启动的实例会先占住端口。
5. **协议别漏**：纯 NetherNet 的基岩入口需要 **TCP + UDP 同号**都放行，只开 UDP 会连不上。

> **本服实际端口清单、NAT 映射与防火墙放行表**见 [多服网络与端口架构.md](./多服网络与端口架构.md) §3 / §6 / §7；
> 本文不再维护第二套端口数值，避免两份文档口径打架。

### 4.2 迁移后的连通性变化

```
玩家(Java) ──TCP 代理对外端口──► Velocity ──TCP 127.0.0.1:子服端口──► 各后端(Leaf)
基岩玩家  ──TCP+UDP 基岩端口──► Geyser-Velocity(代理) ─┴────────────► 同前
```

改造前玩家直连 Leaf 的对外端口，改造后玩家只连 Velocity，Leaf 变成不对外暴露的后端。
当前实跑的完整拓扑图（各实例端口、堆内存、YOP 端口、基岩链路）见 [多服网络与端口架构.md](./多服网络与端口架构.md) §2。

---

## 5. 后端子服对接（Leaf / Paper）

### 5.1 改 server.properties

在每个子服（含现有 Leaf）里修改 `server.properties`：

```properties
# 关闭自身的正版验证：认证责任已交给 Velocity
online-mode=false

# 改为不冲突的内网端口（不要再占用对外端口）
server-port=30002

# ★ 关键：只绑定本机，避免被公网直连
server-ip=127.0.0.1

# 不用 BungeeCord 模式（Velocity modern 走 paper-global.yml，不是 bungeecord）
# 保持 bungeecord=false
```

> ⚠️ `spigot.yml` 里的 `settings.bungeecord` 保持 `false`，
> modern forwarding 用的是 paper-global.yml 的 velocity 段，两者互斥。

### 5.2 开启 Velocity 支持（paper-global.yml）

现有 Leaf 是 Paper 分支，配置在 `config/paper-global.yml`：

```yaml
proxies:
  velocity:
    enabled: true # 开启 modern forwarding
    online-mode: true # ★ 必须与 velocity.toml 的 online-mode 一致（都是 true）
    secret: '<粘贴 forwarding.secret 的完整内容>'
```

> **为什么这里是 true**（Paper 官方定义）："Instructs the server how to handle
> player UUIDs and data when behind Velocity. Always set to match your proxy's
> online-mode setting." 即它**镜像的是代理端 `velocity.toml` 的 `online-mode=true`**，
> 告诉后端按在线模式处理 UUID 与玩家数据；不是让后端再做一次认证（后端认证已由
> `server.properties` 的 `false` 关闭）。后端是否信任转发数据由 `secret` 的 MAC 验签决定，
> 与这个开关无关。两者取值不同源，并不矛盾。三个 `online-mode` 的完整对照见 **§5.5**。

### 5.3 每个子服都加 Yggdrasil agent（外置登录关键）

**这是外置登录 + Velocity 与"正版 + Velocity"最关键的区别**：因为 Yggdrasil
不是 Mojang，Velocity 无法靠 `online-mode=true` 直接用 Mojang 认证——它自己
也要加载 authlib-injector 才知道去哪认证。而且**每个子服也要加载**，
这样后端解析玩家 profile / 皮肤时才认识外置登录的 UUID 结构。

来源：[authlib-injector 官方文档（HelloSkin 镜像）](https://docs.helloskin.cn/yggdrasil/authlib-injector.html)
明确要求：代理端与所有子服**都**加载 authlib-injector。

**Velocity 启动脚本**追加（在 `-jar` 前）：

```powershell
java "-Dauthlibinjector.disableHttpd" -Xms512M -Xmx512M ^
  -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar ^
  -jar velocity-4.3.1-45.jar
```

**每个子服（Leaf）启动脚本**：在现有 `-javaagent:YggdrasilOfficialProxy...` 基础上
原样保留即可（你现有 02 章已把 YggdrasilOfficialProxy 打包成可注入的 jar），
代理端也加同一个 agent。

> ⚠️ 每个 JVM 的 `YggdrasilOfficialProxy.conf` → `server.port` 必须互不相同，否则后启动的实例绑不上端口、
> 外置登录整体失效。本服四份 conf 的实际取值见 [多服网络与端口架构.md](./多服网络与端口架构.md) §3.3。
> 上线前用真实 LittleSkin 外置账号点验一次完整认证链路；若 4.x 下认证异常，可备一个走原生
> authlib-injector 直连 LittleSkin 的启动脚本作为兜底。

### 5.4 换服指令

Velocity 自带 `/server <子服名>`，权限节点 `velocity.command.server`。
玩家输入 `/server survival` 即从大厅瞬移到生存服，不掉线。
可用 `velocity.command.list` 查看子服列表，`velocity.command.send` 管理用。

### 5.5 为什么后端 `online-mode=false`，以及玩家数据会不会变

这是迁移时最容易误解、后果也最严重的一节。

#### 认证只能发生一次

客户端登录时只与 **Velocity** 做一次加密握手：Velocity 生成 `serverId`，客户端拿它向
Mojang / LittleSkin 的 session server 换认证，Velocity 自己也查一次 `hasJoined` 拿到
UUID 和皮肤属性。等 Velocity 把连接代理到后端时，客户端**早已进入 Play 状态**，
不会再为后端做第二次加密响应，后端也没有自己的 `serverId`。此时后端若仍开
`online-mode=true`，就会去查一个不存在的 session → 玩家被踢
（`Failed to verify username!`）。

[Velocity 官方原文](https://docs.papermc.io/velocity/player-information-forwarding/)：

> First, you need to disable the `online-mode` setting in the `server.properties` file.
> **This prevents the server from authenticating players, which the proxy will do instead.**

#### 关掉认证 ≠ 变成离线服

`modern` 转发在登录阶段用 `forwarding.secret` 做 MAC 签名，把代理认证得到的
**UUID、用户名、IP、皮肤属性**一起注入后端；Paper 验签后直接采用，跳过 Mojang 查询。
所以后端拿到的仍是**认证后的真实 UUID**，不是
`UUID.nameUUIDFromBytes("OfflinePlayer:" + name)` 派生出来的离线 UUID。

反证就在 `velocity.toml` 的默认注释里——只有 `none` 模式才写：

> `none`: No forwarding will be done. All players will appear to be connecting
> from the proxy and **will have offline-mode UUIDs**.

**真正会让玩家数据全变的是把转发改成 `none`（或 secret 不符导致转发未生效），
不是后端 `online-mode=false`。**

#### 三个 `online-mode` 分别指什么

| 位置                                                            | 值      | 含义                                                                      |
| --------------------------------------------------------------- | ------- | ------------------------------------------------------------------------- |
| `velocity/velocity.toml`                                        | `true`  | 唯一真正做认证的地方                                                      |
| 后端 `server.properties`                                        | `false` | 后端不再自己查 session server                                             |
| 后端 `config/paper-global.yml` → `proxies.velocity.online-mode` | `true`  | 镜像**代理端**的 online-mode；告诉后端按在线模式处理 UUID / 数据，见 §5.2 |

前两个不矛盾，因为说的是两个进程；第三个是镜像配置，不是"后端是否在线"。

#### UUID 与玩家数据影响

| 账号类型            | UUID 来源                                           | 迁移后                                                                                  |
| ------------------- | --------------------------------------------------- | --------------------------------------------------------------------------------------- |
| 正版 Mojang         | Velocity 认证结果                                   | 不变                                                                                    |
| LittleSkin 外置登录 | YOP 从 `littleskin.cn/api/yggdrasil` 返回           | 理论不变；代理 / 后端 conf 需完全一致（`edited=true`、`official-first=true`、同一 api） |
| 基岩 Floodgate      | `key.pem` + Xbox XUID → `00000000-0000-0000-0009-*` | 不变，前提是**前后端同一份 key.pem、同版本 Floodgate、`username-prefix` 一致**          |

数据核查清单（按 UUID 索引的文件，只要 UUID 不变就不会"换号"）：

- `plugins/Essentials/userdata/<uuid>.yml` —— 余额 / 家 / 传送点（南瓜币真源）
- `usercache.json`、`ops.json`、`banned-players.json` —— 均按 UUID 存
- `world/players/data`、`advancements`、`stats`（Leaf 26.3 的新路径，**不是**旧的 `world/playerdata/`）—— 背包 / 进度 / 统计
- LuckPerms、Residence、Plan、CoreProtect、SkinsRestorer —— 按 UUID 或用户名存
- ⚠️ **复制项目做改造时最容易漏掉 `world/` 目录**。只复制 `plugins/` 会让所有
  原版存档"消失"，后端启动后生成一个全新空世界——这不是 UUID 变了，是数据没带过来。

> ⚠️ `UuidMigrate` 一类插件若仍启用且 `operation: COPY`，检测到新 UUID 时会自动
> 把旧存档复制一份。它既是安全网，也可能产生重复数据，上线前确认其行为。

#### 上线前验证

1. 从原服把 `world/`（含 `world_nether` / `world_the_end` 等）完整复制过来。
2. 记下某玩家旧服 `Essentials/userdata/<uuid>.yml` 的文件名。
3. 连代理对外端口登录一次，确认后端**没有**新冒出一个 UUID 文件，余额 / 背包 / 领地对得上。
4. Java 外置登录、正版、基岩各测一次；基岩重点看 `.` 前缀与 Floodgate UUID 是否一致。

---

## 6. 基岩互通迁移：Geyser + Floodgate 装到代理

### 6.1 架构决策

**在 Velocity 架构下，Geyser + Floodgate 应装在代理端，而不是后端。**
官方明确指引（来源：[SetupMC Geyser-Floodgate-Velocity 指南](https://setupmc.com/guides/geyser-floodgate-velocity-proxy-docker/)、
[GeyserMC 官网](https://geysermc.org/)）：

- Geyser 只装代理 → 基岩玩家从代理的基岩入口进代理，再由代理分发到目标子服（该入口需 TCP + UDP 同号放行）
- Floodgate 装代理 → 负责基岩账号认证
- **后端仅在插件需要 Floodgate API（如识别基岩玩家）时才加装 Floodgate**
  （你的 BedrockPlayerSupport / QuickMenu 需读 Floodgate，见 §8.1）

### 6.2 下载 Velocity 版插件

- Geyser：`https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/velocity`
- Floodgate：`https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/velocity`

放入 `velocity/plugins/`。**注意是 velocity 版，不要下成 spigot/bungee 版。**

### 6.3 Geyser 配置（velocity/plugins/Geyser-Velocity/config.yml）

> ✅ **2026-11-10 实测推荐做法：整份复用，不要按下面的模板重写。**
> Geyser 的 Velocity 版与 Spigot 版 `config.yml` **键集合完全一致**（78 键 /
> `config-version: 12`）。直接把后端 `plugins/Geyser-Spigot/config.yml` 复制过来，
> 确认 `java.auth-type: floodgate` 即可，这样能保留你调过的全部自定义项：
> `transport: nethernet`、基岩监听端口设置（本服实际值见
> [多服网络与端口架构.md](./多服网络与端口架构.md) §3.1）、`default-locale: zh_cn`、
> `forward-player-ping` 等。按简化模板重写会丢掉这些调优。
> ⚠️ 实际目录名是 `Geyser-Velocity`（首字母大写）；新版认证段已由 `remote:` 更名为 `java:`。

首次从零生成时，最小化关键项如下（**端口为教学示例**，本服实际基岩端口见
[多服网络与端口架构.md](./多服网络与端口架构.md) §3.1；复用旧配置时仅作对照）：

```yaml
bedrock:
  # 监听所有网卡（供基岩客户端连）
  address: 0.0.0.0
  port: 19132
  # 若 Velocity 本身在 25565，把克隆关掉避免占同端口
  clone-remote-port: false

java:
  # ★ 用 Floodgate 认证基岩玩家，而不是自己问 Java 账号
  auth-type: floodgate

# 透传代理的 MOTD 与在线人数（显示统一）
passthrough-motd: true
passthrough-player-counts: true
```

### 6.4 Floodgate 配置（velocity/plugins/floodgate/config.yml）

```yaml
# 非绑定基岩玩家的名字前缀（用"."避免与 Java 玩家撞名）
username-prefix: '.'

# ★★★ 后端装了 floodgate-spigot 时必须开（实测）★★★
# 把基岩玩家的 Floodgate 数据经 Velocity 转发给后端，否则
# BedrockPlayerSupport / QuickMenu 依赖的 isFloodgatePlayer 判定失效，玩家可能被踢。
send-floodgate-data: true

# 账号绑定：让基岩玩家的 UUID 与 Java 外置登录一致，跨子服保持同一身份
player-link:
  enable-own-linking: false # false = 用 Floodgate 官方全局绑定
  # enable-own-linking: true  # 若走本地绑定数据库再开，并装 floodgate-sqlite
```

> ⚠️ **key 必须同源**：`velocity/plugins/floodgate/key.pem` 与后端
> `plugins/floodgate/key.pem` 必须是**同一份文件**（不是各生成一份），且前后端
> Floodgate 版本一致（实测同为 `2.2.5-SNAPSHOT b141-81b65cc`）。
> Floodgate UUID 由 key + XUID 推导，**key 不同 = 全体基岩玩家 UUID 全变 =
> 背包 / 余额 / 领地全部对不上**。详见 §5.5。

### 6.5 基岩玩家如何被后端识别

后端若装了 Floodgate-bukkit，基岩玩家的 `isFloodgatePlayer` 仍为 true。
迁移后基岩玩家在代理统一认证，UUID 稳定，跨子服保持同一身份——
正好解决"基岩玩家在子服间 UUID 不一致"的隐患。

---

## 7. 权限与数据跨服同步（LuckPerms / 经济 / 领地）

### 7.1 LuckPerms 多服同步

LuckPerms **天然支持多服**，关键是所有子服 + 代理共用**同一个存储后端**：

1. **把 LuckPerms 存储切到 MySQL**（默认 h2 是单服文件，跨服不共享）：
   `config.yml` → `storage-method: mysql`，并配好 `data` 段的地址/库/账号。
   你 05 章已规划 MySQL 迁移步骤，此处落地即可。
2. **所有子服装 LuckPerms**，`config.yml` 设 `server: <本服名>`（如 `survival`），
   存到同一 MySQL。权限一处改，处处生效。
3. **（可选）代理端也装 LuckPerms-velocity**，管理 `/server` 等代理命令权限，
   与子服权限数据同源。

同步机制：改动后执行 `/lp sync`（或开 `sync-minutes` 定时拉取）即可在所有服生效。

> 📌 **落地判断**：只有当多个子服需要**同一套权限**时才必须切共享存储。
> 若各子服权限刻意隔离（例如小游戏服不给生存服的管理权限），保持各自 `h2` 反而更清晰。
> 本服当前各子服数据独立，是否切 MySQL 属于部署决策，相关记录见

### 7.2 Vault 经济跨服

Vault 只是 API，真正的经济实现要选**支持跨服**的：

- 若你现在用 EssentialsX 的经济，它是**单服数据库**，跨服会各自为政。
- 多服建议改用 **CMI 经济 / CoinsEngine / 或带 SQL 存储的 Vault 经济插件**，
  或把 EssentialsX 切到共享 MySQL（部分版本支持）。
- 简单做法：选一个"经济主服"，其余子服用菜单/命令网关调用主服 API；
  更稳妥是直接上共享 MySQL 的 Vault 实现。

### 7.3 领地 / 家园 / 记录类插件

| 插件              | 跨服策略                                                   |
| ----------------- | ---------------------------------------------------------- |
| Residence         | 每服独立；想"领地地图全局共享"可切 Residence 的 MySQL 存储 |
| CoreProtect       | 每服独立即可；想全局查询再切 MySQL                         |
| EssentialsX 家/点 | 默认单服；多服共享用 EssentialsX 的 SQL 存储并共库         |
| SkinsRestorer     | 后端都装，连同一 MySQL 皮肤库即可全局生效                  |

> 迁移原则：**凡是数据想"全网一份"的插件都切到同一个 MySQL**；玩法想隔离的
> （如每服独立小游戏积分）保持各自文件/SQLite。

---

## 8. 现有插件在 Velocity 下的逐项适配

| 插件                         | 装代理    | 后端保留         | 说明                                    |
| ---------------------------- | --------- | ---------------- | --------------------------------------- |
| **Geyser**                   | ✅ 装代理 | ❌ 移除          | 见 §6                                   |
| **Floodgate**                | ✅ 装代理 | ⚠️ 需要 API 才装 | BPS / QuickMenu 需要则后端也装          |
| **Yggdrasil agent**          | ✅        | ✅               | 见 §5.3                                 |
| LuckPerms                    | ✅(可选)  | ✅               | 切 MySQL 跨服同步，见 §7.1              |
| Vault / EssentialsX          | ❌        | ✅               | 经济切共享后端                          |
| Residence / CoreProtect      | ❌        | ✅               | 按需切 MySQL                            |
| SkinsRestorer                | ❌        | ✅               | 后端装，共 MySQL                        |
| Simple Voice Chat            | ❌        | ✅               | 本服 26.3 未部署，历史方案              |
| EasyBot                      | ❌        | ✅（主服）       | 见 §8.2                                 |
| QuickMenu                    | ❌        | ✅               | 见 §8.1                                 |
| BedrockPlayerSupport         | ❌        | ✅               | 见 §8.1                                 |
| spark                        | ❌        | ✅（各服自用）   | 代理端也可装 spark-velocity 看代理负载  |
| ViaVersion                   | ❌        | ✅               | 后端必须有；代理端也可装以提前协商版本  |
| OpenInv / OPanel / WorldEdit | ❌        | ✅               | 单服自用，无需代理（Chunky 本服未部署） |

### 8.1 BedrockPlayerSupport / QuickMenu 与基岩玩家识别

这两个插件在**后端**运行时通过 Floodgate API 判断"是不是基岩玩家"。
迁移后基岩玩家的真实身份由代理认证，后端读取 Floodgate 数据的做法：

1. **后端也装 floodgate-bukkit**，并配置它指向与代理同一个 key，让
   `isFloodgatePlayer` 在后端为 true。这是 QuickMenu 基岩分发
   `[bedrock-player]` 生效的前提。
2. 若不装后端 Floodgate，后端无法用 Floodgate API 区分基岩玩家，QuickMenu
   的"基岩走原生表单、Java 走箱子"会退化成全走 Java 分支。

> 结论：QuickMenu 与 BedrockPlayerSupport 要在多服下继续按客户端分流，
> 后端子服必须装 **floodgate-bukkit**、保持与代理同一 key，且代理端要开
> `send-floodgate-data: true`（见 §6.4）——三者缺一，后端都收不到基岩数据。

### 8.2 EasyBot（QQ 联动）

EasyBot 桥接的是"一个 MC 服务端 ↔ QQ"。多服下**只让它在主服跑**即可，
QQ 群看到的就是主服聊天；若要群消息广播到全子服，需配合各服的跨服聊天插件
（或让 EasyBot 做网关）。按当前部署，EasyBot 留在主服不动即可。

---

## 9. 性能优化与安全基线

### 9.1 Aikar 内存 / GC 参数（代理与子服通用）

代理本身很轻，512M~1G 足够。子服沿用你现在的 Aikar flags 即可。示例：

```powershell
java -Xms2G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled ^
  -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions ^
  -XX:+DisableExplicitGC -XX:+AlwaysPreTouch ^
  -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M ^
  -jar leaf.jar nogui
```

### 9.2 安全基线清单

| 项                                               | 必须 | 说明                                                                        |
| ------------------------------------------------ | ---- | --------------------------------------------------------------------------- |
| 子服绑 `127.0.0.1` + 从安全组移除                | ✅   | 防直连伪造                                                                  |
| `proxies.velocity.enabled=true` + secret 一致    | ✅   | modern 转发密钥（1.18.2+ 的配置名；旧文 `velocity-support.enabled` 已废弃） |
| `paper-global.yml` 的 `online-mode` 镜像代理     | ✅   | 值必须一致，见 §5.2                                                         |
| `force-key-authentication=false`（4.x 外置登录） | ✅   | 开着会踢掉所有外置登录玩家，见 §3.1                                         |
| `send-floodgate-data=true` + 前后端同一 key.pem  | ✅   | 后端 floodgate API 生效前提，见 §6.4                                        |
| 每个 JVM 的 YOP `server.port` 互不相同           | ✅   | 端口撞号会让后启动实例绑不上、外置登录失效，见基准文档 §3.3                 |
| 全端加载 Yggdrasil agent                         | ✅   | 外置登录识别                                                                |
| `minecraft.command.op` 不给任何组                | ✅   | 见 05 章                                                                    |
| forwarding.secret 保密                           | ✅   | 泄露等同能伪造玩家                                                          |

> 防火墙的具体放行 / 拒绝清单见 [多服网络与端口架构.md](./多服网络与端口架构.md) §6。

### 9.3 常见性能/配置误配

- 子服仍开 `spigot.yml bungeecord: true` → 与 modern 转发冲突，登录异常。应 `false`。
- Velocity 4.x 忘关 `force-key-authentication` → 外置登录玩家被公钥签名校验踢出。应 `false`。
- 把 `player-info-forwarding-mode` 设成 `none` → 后端拿到离线 UUID，全体玩家"换号"。必须 `modern`。
- 前后端 Floodgate key.pem 不是同一份 → 基岩玩家 UUID 全变，背包 / 余额 / 领地全丢。
- 代理内存给太大没意义；代理吃的是网络吞吐不是内存。
- 忘了在安全组放行**基岩入口的 TCP + UDP** → 基岩连不上（纯 NetherNet 两条都要；Java 入口不受影响）。实际端口见 [多服网络与端口架构.md](./多服网络与端口架构.md) §3.1。

---

## 10. 本目录文档导航

| 文档                                                                 | 唯一职责                                                             |
| -------------------------------------------------------------------- | -------------------------------------------------------------------- |
| 本文                                                                 | 从单服迁到 Velocity / Multiverse 的通用原理与步骤                    |
| [多服网络与端口架构.md](./多服网络与端口架构.md)                     | 当前实跑的端口、转发密钥、认证、防火墙、启动方式（**配置唯一真源**） |
| [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) | 三服菜单、NPC、传送门入口、跨服权限三件套、小游戏地图                |
| [传送门-Advanced Portals.md](./传送门-Advanced Portals.md)           | Advanced Portals 2.8.0 插件分册（命令 / 标签 / 配置文件）            |

### 相关章节（仓库其它目录）

| 章节                                                                           | 关系                                                |
| ------------------------------------------------------------------------------ | --------------------------------------------------- |
| [服务器核心-Leaf](../服务器核心-Leaf/)                                         | Leaf 改造为 Velocity 的后端子服                     |
| [外置登录代理](../外置登录代理-YggdrasilOfficialProxy/)                        | 全端加载 Yggdrasil agent，Velocity 承担认证         |
| [Java-Bedrock互通层-Geyser-Floodgate](../Java-Bedrock互通层-Geyser-Floodgate/) | Geyser + Floodgate 迁到代理端，后端按需装 floodgate |
| [权限管理-LuckPerms](../../5-服务器管理/权限管理系统-LuckPerms/)               | LuckPerms 切 MySQL 实现跨服权限同步                 |
| [端口与网络架构总览](../端口与网络架构总览/README.md)                          | 单服端口总表；多服实跑值以本目录基准文档为准        |
| [快捷菜单系统](../../自研/快捷菜单系统-QuickMenu/快捷菜单系统-QuickMenu.md)    | 后端需装 floodgate-bukkit 才能继续按客户端分流      |
| [SpacePortal 使用说明](../../自研/传送阵-SpacePortal/空间传送阵使用说明.md)    | 单服内坐标传送，与跨服入口分工                      |

> **演进路线图**：单服 → Velocity 代理 → 多子服分区 → 共享 MySQL 数据层。本文是"单服 → 网络"的桥。

---

## 11. 单服多世界：Multiverse（与 Velocity 互补，非替代）

> **一句话**：Multiverse-Core 是一个服务端插件，让你在**同一个 Leaf 进程里**开多个独立世界（主世界 / 资源界 / 小游戏 / 建筑区），
> 玩家用 `/mv tp` 或传送门在它们之间穿梭；它跟 Velocity 不冲突——Velocity 管"服与服"，Multiverse 管"世界与世界"。

### 11.1 版本与 26.3 支持（已核实）

| 项         | 信息                                                                             |
| ---------- | -------------------------------------------------------------------------------- |
| 最新版     | **Multiverse-Core 5.8.2**（2026-10-26）                                          |
| MC 版本    | 明确支持 **1.18.2 → 26.3**（Paper / Purpur / Spigot / Bukkit）                   |
| 许可       | BSD-3-Clause                                                                     |
| 热度       | Modrinth 89 万+ 下载，活跃维护                                                   |
| 玩家要装吗 | 不用，纯服务端插件                                                               |
| 依赖       | 无强依赖；可选 Multiverse-Portals / -Inventories / -NetherPortals / -SignPortals |

> 你的 26.3 主服（Leaf 26.3-18 / Java 25）可直接装 5.8.2，无版本顾虑。

### 11.2 与你的架构怎么配合

| 你的现状                            | 怎么用 Multiverse                                                                                                                                              |
| ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **当前单 Leaf 服**（未上 Velocity） | 直接给 Leaf 装 Multiverse-Core，开资源世界/小游戏世界，**零架构改动**，不影响 Geyser/ViaVersion/Residence                                                      |
| **已上 Velocity 多服**（见 §1–§10） | **Multiverse 装在"后端子服"里，不装代理**。每个子服内部用 Multiverse 管自己的多世界；跨子服换服用 Velocity 的 `/server`，世界内换世界用 Multiverse 的 `/mv tp` |

> ⚠️ **Multiverse 永远装在子服（业务服务端），绝不装在 Velocity 代理进程**——代理只做转发，不加载游戏世界。

### 11.3 安装与基础命令

1. 下载 `multiverse-core-5.8.2.jar`（Modrinth / Hangar），丢进 Leaf 的 `plugins/`，重启。
2. 首次启动自动生成 `plugins/Multiverse-Core/` 配置。
3. 常用命令（权限节点见 §11.6）：

```bash
/mv create resource normal -t NORMAL     # 新建"资源世界"（normal 维度）
/mv create games normal -t NORMAL         # 新建"小游戏世界"
/mv tp resource                           # 传送到资源世界
/mv list                                  # 列出所有世界
/mv set spawn resource                    # 设置某世界出生点
/mv remove <世界名>                        # 删除世界（谨慎）
```

> 资源世界建议单独生成一个干净维度，挖完即弃；小游戏世界可加载独立地形生成器。

### 11.4 资源世界自动重置（重点）

Multiverse-Core **本身不含"定时自动重置"**，需要外部触发。三种可行方案，按可靠度排序：

**方案 A：系统计划任务 + RCON 命令（最稳，版本无关）**
写一个重置脚本，在维护窗口由系统 cron（Linux）/ 任务计划程序（Windows）调用，经 RCON 对 Leaf 执行：

```bash
# reset_resource.sh （经 mcrcon 或类似工具）
mv delete resource
mv create resource normal -t NORMAL
```

> 前提：Leaf 开启 `enable-rcon=true` 并配 `rcon.password`。这是不受插件更新影响的兜底方案。

**方案 B：加装"定时命令"插件**
如支持 26.3 的定时任务插件（`TaskScheduler` / `ServerUtils` 等），在里面周期性执行控制台命令 `/mv delete resource` + `/mv create resource ...`。

> ⚠️ 装前必须确认该插件声明支持 26.3（你现有插件清单里未列此类，需单独核查）。

**方案 C：专用资源世界重置插件**
社区有 `WorldReset` / `AutoReset` 类插件，但需逐一确认 26.3 兼容与维护状态，**不推荐作为首选**。

> 推荐 **方案 A**：简单、可控、不受插件版本限制；重置频率（每日/每周）由你的维护窗口决定。

### 11.5 配套插件

| 插件                         | 作用                            | 多服注意                                                                          |
| ---------------------------- | ------------------------------- | --------------------------------------------------------------------------------- |
| **Multiverse-Portals**       | 用方块搭传送门在世界间穿梭      | 装子服，不装代理                                                                  |
| **Multiverse-Inventories**   | 让不同世界有**独立背包/末影箱** | ⚠️ 这是"子服内分世界背包"，与 §7 的"跨服共享背包"是两码事；多服下仍按子服各自独立 |
| **Multiverse-NetherPortals** | 地狱门自动分流到对应世界的下界  | 装子服                                                                            |
| **Multiverse-SignPortals**   | 用牌子点击传送                  | 装子服                                                                            |

### 11.6 权限节点（LuckPerms）

Multiverse-Core 权限粒度细，常用节点（给对应玩家/组）：

| 节点                         | 作用                  |
| ---------------------------- | --------------------- |
| `multiverse.*`               | 全部权限（管理员）    |
| `multiverse.access.*`        | 进入所有世界          |
| `multiverse.teleport.self.*` | 自己传送到各世界      |
| `multiverse.create.*`        | 创建世界              |
| `multiverse.core.manage`     | 管理世界（删除/重载） |

> 高级权限模式（如 ClearLag 检测到 LuckPerms 后走的那种）下权限需显式授予，非 op 玩家默认拿不到；op 默认拥有。

### 11.7 与 Velocity 共存要点（速记）

- **装法**：Multiverse 装后端子服；Velocity 是独立代理进程，不碰 Multiverse。
- **换服 vs 换世界**：跨子服用 `/server`（Velocity）；同服内跨世界用 `/mv tp`（Multiverse）。两条命令各管一层，不冲突。
- **数据**：Multiverse 的世界数据在各子服本地 `worlds/` 目录；跨服共享数据请走 §7 的 MySQL 方案，与 Multiverse 无关。

---

> **实装之后看哪份**：本服当前配置以 [多服网络与端口架构.md](./多服网络与端口架构.md) 为准；
> 游戏内跨服入口见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md)；
