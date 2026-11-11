# 27. Velocity 多服（代理）+ Multiverse 多世界（插件）

本文档面向准备把现有单服扩展为多子服网络的服主，给出从单端无代理迁移到 Velocity 代理的完整落地步骤。它涵盖代理选型、velocity.toml 配置、后端子服对接、Geyser/Floodgate 迁移、权限与经济跨服同步、各现有插件逐项适配、安全基线，以及按序执行的迁移 Checklist。

> **一句话**：在现有单服（Leaf + Yggdrasil 外置登录 + Geyser 基岩互通）前面加一层
> **Velocity 代理**，把"一台服务器"升级成"一个网络"，玩家从一个地址进入，
> 可无缝穿梭多个子服（大厅 / 生存 / 小游戏 / 建筑……），换服不掉线。

> ⚠️ **前置说明**：你的项目当前是**单端无代理**（`bungeecord: false`，见 README §5.1）。
> 本章是把单服迁移到 Velocity 网络的**完整落地教程**。若暂时只有一台服务器、
> 暂不需要分区玩法，可跳过本章；一旦要分服（生存/空岛/RPG 并存），本方案是 PaperMC
> 官方推荐的现代替代（Waterfall 已 EOL，官方建议全部迁移到 Velocity）。

> 📌 **2026-11-10 落地校订**：本章方案已在 `leaf-26.3-多服` 副本实装验证，
> 实测版本为 **Velocity 4.3.1-45 / Java 25**（非下文历史示例的 3.3.x / JDK 21）。
> 4.x 新增开关、Floodgate 转发配置、玩家数据与 UUID 影响，以及全部版本差异，
> 统一收录在 **§5.5（原理）**、**§14（落地校订与差异对照）** 与 **§16（资源包策略）**。正文已按实测结果同步修订。

### 0. 先厘清：Velocity 与 Multiverse 是两层，不是二选一

本文档标题是「Velocity + Multiverse」，但两者**不是同类、不是替代关系**。很多人误以为"多世界就要上代理"或"上代理就不用多世界"，先对齐认知：

| | **Multiverse-Core** | **Velocity** |
|---|---|---|
| 它是什么 | 服务端**插件**（Bukkit/Paper 系） | 独立的**代理服务器**（独立进程） |
| 跑在哪 | 塞进单个服务端的 `plugins/` | 单独 Java 进程，夹在「客户端 ↔ 多个服务端」之间 |
| 解决什么 | **一个服务端里开多个世界**（资源界/小游戏/建筑区） | **把多个独立服务端连成一个网络**（大厅/生存/空岛互相跳） |
| 类比 | 一栋楼里隔多个房间 | 多栋楼共用一个大门进出 |
| 26.3 支持 | ✅ 5.8.2 明确支持 1.18.2→26.3 | ✅ 明确支持 1.7.2→26.3（自身需 Java 25） |
| 部署复杂度 | 低：丢 jar、重启 | 高：独立进程 + 每个后端改 forwarding |
| 基岩互通影响 | 不影响，照常走服内 Geyser | 需重规划 Geyser/Floodgate 部署（见 §6） |

**结论**：
- 想要"同一个服里多个世界" → 用 **Multiverse**（本文 §13），零架构改动。
- 想要"多个独立服、一个 IP 进出" → 用 **Velocity**（本文 §1–§12）。
- **两者常配合使用**：大服典型架构是 Velocity 管"服与服之间"，每个后端子服内部再用 Multiverse 管"世界与世界之间"。它们装在不同地方（Multiverse 装子服、Velocity 是独立进程），互不冲突。

> **贴合你当前环境**：你现在是**单个 Leaf 26.3 服**（已正式升级到 26.3，非测试），跨端（Geyser/Floodgate）、ViaVersion、Residence、Plan 都在这一个服上，**且已计划新增小游戏服（即要分服）**。
> - **Multiverse 用得上**：在 26.3 主服上先装 Multiverse 加"资源世界"等，零架构改动（见 §13）。
> - **Velocity 按规划要上**：既然已计划小游戏服（分服），Velocity 代理是必走的网络层。建议节奏：**先在 26.3 主服装 Multiverse 满足单服多世界需求 → 待小游戏服落地时再按 §1–§12 部署 Velocity，把主服 + 小游戏服串成网络**；Geyser/Floodgate/ViaVersion 的迁移留到上 Velocity 那一步统一重做。


## 1. 选型结论：为什么是 Velocity

| 方案 | 开发状态 | 性能 | 转发安全 | 结论 |
|---|---|---|---|---|
| **BungeeCord** | 维护（遗留） | 中 | legacy 明文，可伪造 | ❌ 过时 |
| **Waterfall** | **已停更（EOL）** | 中 | legacy / 部分 modern | ❌ 官方已弃用 |
| **Velocity** | **活跃维护** | **最高** | modern 加密 MAC 校验 | ✅ **采用** |

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

| 认证链路 | 单服现状 | Velocity 架构 |
|---|---|---|
| Java 外置登录 | Leaf 用 `-javaagent:authlib-injector` 承担 | **Velocity 承担**，后端只验 forwarding 密钥 |
| 基岩互通 | Leaf 内嵌 Geyser + Floodgate | **Geyser + Floodgate 迁到 Velocity**，基岩玩家经代理进任意子服 |
| 皮肤 | SkinsRestorer 在各子服 | SkinsRestorer 仍放后端（可跨服同步，见 §8） |

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
├── velocity\                  ← 新建：Velocity 代理（本章主角）
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

### 3.1 完整注释版

Velocity 的核心配置是 **TOML** 格式（不是 BungeeCord 的 YAML）。下面给出适配
"Yggdrasil 外置登录 + Geyser" 的完整注释版：

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

### 3.2 三个必须对齐的端口

| 配置项 | 位置 | 说明 |
|---|---|---|
| `bind` | velocity.toml | 代理对外端口（玩家连这个） |
| 子服 `server-port` | 各子服 server.properties | 代理连接子服用，**改到不冲突的内网端口** |
| Geyser `port` | velocity/plugins/Geyser-Velocity/config.yml | 基岩监听端口（沿用 19132；纯 NetherNet 信令走 TCP、媒体走 UDP，**两个都要放行**） |

> ⚠️ **最常见翻车点**：改造后所有子服的 `server.properties` 要**改成非代理占用、
> 且彼此不重复**的端口（如 30001/30002/30003），再在 velocity.toml 里按这个填。
> 不要沿用旧的对外大端口，避免代理与子服争端口。

---

## 4. 网络与端口规划

### 4.1 对外暴露

玩家与基岩客户端只连 **Velocity**，后端全部 **只绑 127.0.0.1 或内网 IP，绝不对外**。

| 端口 | 协议 | 绑定 | 是否公开 | 用途 |
|---|---|---|---|---|
| **55551** | TCP | `0.0.0.0` | ✅ | Velocity 对外（Java 版入口） |
| **19132** | **TCP + UDP** | `0.0.0.0` | ✅ | Geyser 基岩入口（装在代理）。纯 NetherNet：信令 TCP、媒体 UDP，缺一不可 |
| **24454** | UDP | 视方案 | ⚠️ | Simple Voice Chat 语音（见 §8.7） |
| 30001/30002… | TCP | `127.0.0.1` | ❌ | 各子服（仅代理可达） |
| 32217 / 32218 | TCP | 本机 | ❌ | YggdrasilOfficialProxy：后端 32217 / 代理 32218，**双进程必须错开** |
| 25595 | TCP | 本机 | ❌ | 后端 RCON（Multiverse 资源世界重置用，见 §13.4） |

**安全铁律**：所有子服的 `server.properties` 设 `server-ip=127.0.0.1`（或内网 IP），
并把它们**从云安全组 / 防火墙的对外放行里移除**。否则一旦有人直连 30002，
配合 `online-mode=false` 就能伪造任意玩家（含 OP）进入——这是最致命的安全漏洞。

> ⚠️ **双进程 YOP 端口冲突**：代理与后端都加载 YggdrasilOfficialProxy javaagent 时，
> 两者默认都抢 32217，后启动的进程会起不来。必须把 `velocity/YggdrasilOfficialProxy.conf`
> 里的 `server.port` 改成 **32218**（后端保留 32217），只改代理端即可。

### 4.2 迁移后的连通性变化

```
玩家(Java) ──TCP 55551──► Velocity ──TCP 127.0.0.1:30002──► survival(Leaf)
基岩玩家  ──UDP 19132──► Geyser(代理) ─┴──────────────────► 同前
```

改造前玩家直连 Leaf:55551，改造后玩家只连 Velocity，Leaf 变成不对外暴露的后端。

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
    enabled: true            # 开启 modern forwarding
    online-mode: true        # ★ 必须与 velocity.toml 的 online-mode 一致（都是 true）
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

> ⚠️ 别忘把 `velocity/YggdrasilOfficialProxy.conf` 的 `server.port` 改成 **32218**（见 §4.1）。
> 实测 **Velocity 4.3.1 + YOP 2.3.0 + authlib-injector 1.2.8** 可正常启动，
> 但**真实外置登录认证流程尚未实测**。若 4.x 下认证异常，可备一个走原生
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

| 位置 | 值 | 含义 |
|---|---|---|
| `velocity/velocity.toml` | `true` | 唯一真正做认证的地方 |
| 后端 `server.properties` | `false` | 后端不再自己查 session server |
| 后端 `config/paper-global.yml` → `proxies.velocity.online-mode` | `true` | 镜像**代理端**的 online-mode；告诉后端按在线模式处理 UUID / 数据，见 §5.2 |

前两个不矛盾，因为说的是两个进程；第三个是镜像配置，不是"后端是否在线"。

#### UUID 与玩家数据影响

| 账号类型 | UUID 来源 | 迁移后 |
|---|---|---|
| 正版 Mojang | Velocity 认证结果 | 不变 |
| LittleSkin 外置登录 | YOP 从 `littleskin.cn/api/yggdrasil` 返回 | 理论不变；代理 / 后端 conf 需完全一致（`edited=true`、`official-first=true`、同一 api） |
| 基岩 Floodgate | `key.pem` + Xbox XUID → `00000000-0000-0000-0009-*` | 不变，前提是**前后端同一份 key.pem、同版本 Floodgate、`username-prefix` 一致** |

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
3. 连代理 55551 登录一次，确认后端**没有**新冒出一个 UUID 文件，余额 / 背包 / 领地对得上。
4. Java 外置登录、正版、基岩各测一次；基岩重点看 `.` 前缀与 Floodgate UUID 是否一致。

---

## 6. 基岩互通迁移：Geyser + Floodgate 装到代理

### 6.1 架构决策

**在 Velocity 架构下，Geyser + Floodgate 应装在代理端，而不是后端。**
官方明确指引（来源：[SetupMC Geyser-Floodgate-Velocity 指南](https://setupmc.com/guides/geyser-floodgate-velocity-proxy-docker/)、
[GeyserMC 官网](https://geysermc.org/)）：

- Geyser 只装代理 → 基岩玩家从 UDP 19132 进代理，再由代理分发到目标子服
- Floodgate 装代理 → 负责基岩账号认证
- **后端仅在插件需要 Floodgate API（如识别基岩玩家）时才加装 Floodgate**
  （你的 BedrockPlayerSupport / QuickMenu 需读 Floodgate，见 §8.5）

### 6.2 下载 Velocity 版插件

- Geyser：`https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/velocity`
- Floodgate：`https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/velocity`

放入 `velocity/plugins/`。**注意是 velocity 版，不要下成 spigot/bungee 版。**

### 6.3 Geyser 配置（velocity/plugins/Geyser-Velocity/config.yml）

> ✅ **2026-11-10 实测推荐做法：整份复用，不要按下面的模板重写。**
> Geyser 的 Velocity 版与 Spigot 版 `config.yml` **键集合完全一致**（78 键 /
> `config-version: 12`）。直接把后端 `plugins/Geyser-Spigot/config.yml` 复制过来，
> 确认 `java.auth-type: floodgate` 即可，这样能保留你调过的全部自定义项：
> `transport: nethernet`、`webrtc-port: 19132`、`default-locale: zh_cn`、
> `forward-player-ping` 等。按简化模板重写会丢掉这些调优。
> ⚠️ 实际目录名是 `Geyser-Velocity`（首字母大写）；新版认证段已由 `remote:` 更名为 `java:`。

首次从零生成时，最小化关键项如下（复用旧配置时仅作对照）：

```yaml
bedrock:
  # 监听所有网卡（供基岩客户端连）
  address: 0.0.0.0
  port: 19132
  # 若 Velocity 本身在 25565，把克隆关掉避免占同端口
  clone-remote-port: false

remote:
  # ★ 用 Floodgate 认证基岩玩家，而不是自己问 Java 账号
  auth-type: floodgate

# 透传代理的 MOTD 与在线人数（显示统一）
passthrough-motd: true
passthrough-player-counts: true
```

### 6.4 Floodgate 配置（velocity/plugins/floodgate/config.yml）

```yaml
# 非绑定基岩玩家的名字前缀（用"."避免与 Java 玩家撞名）
username-prefix: "."

# ★★★ 后端装了 floodgate-spigot 时必须开（实测）★★★
# 把基岩玩家的 Floodgate 数据经 Velocity 转发给后端，否则
# BedrockPlayerSupport / QuickMenu 依赖的 isFloodgatePlayer 判定失效，玩家可能被踢。
send-floodgate-data: true

# 账号绑定：让基岩玩家的 UUID 与 Java 外置登录一致，跨子服保持同一身份
player-link:
  enable-own-linking: false   # false = 用 Floodgate 官方全局绑定
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

> 📌 **2026-11-10 落地状态**：本次部署**尚未切换 MySQL**，LuckPerms 仍是
> `storage-method: h2` 单服存储。当前只有 survival 一个子服，所以权限暂时没有跨服问题；
> **一旦加入第二个子服，必须先完成本节迁移**，否则各服权限各自为政。
> 同理，Essentials 余额 / 家 / 领地目前也都只在 survival 本地，加子服前按 §7.2 / §7.3 规划。

### 7.2 Vault 经济跨服

Vault 只是 API，真正的经济实现要选**支持跨服**的：
- 若你现在用 EssentialsX 的经济，它是**单服数据库**，跨服会各自为政。
- 多服建议改用 **CMI 经济 / CoinsEngine / 或带 SQL 存储的 Vault 经济插件**，
  或把 EssentialsX 切到共享 MySQL（部分版本支持）。
- 简单做法：选一个"经济主服"，其余子服用菜单/命令网关调用主服 API；
  更稳妥是直接上共享 MySQL 的 Vault 实现。

### 7.3 领地 / 家园 / 记录类插件

| 插件 | 跨服策略 |
|---|---|
| Residence | 每服独立；想"领地地图全局共享"可切 Residence 的 MySQL 存储 |
| CoreProtect | 每服独立即可；想全局查询再切 MySQL |
| EssentialsX 家/点 | 默认单服；多服共享用 EssentialsX 的 SQL 存储并共库 |
| SkinsRestorer | 后端都装，连同一 MySQL 皮肤库即可全局生效 |

> 迁移原则：**凡是数据想"全网一份"的插件都切到同一个 MySQL**；玩法想隔离的
> （如每服独立小游戏积分）保持各自文件/SQLite。

---

## 8. 现有插件在 Velocity 下的逐项适配

| 插件 | 装代理 | 后端保留 | 说明 |
|---|---|---|---|
| **Geyser** | ✅ 装代理 | ❌ 移除 | 见 §6 |
| **Floodgate** | ✅ 装代理 | ⚠️ 需要 API 才装 | BPS / QuickMenu 需要则后端也装 |
| **Yggdrasil agent** | ✅ | ✅ | 见 §5.3 |
| LuckPerms | ✅(可选) | ✅ | 切 MySQL 跨服同步，见 §7.1 |
| Vault / EssentialsX | ❌ | ✅ | 经济切共享后端 |
| Residence / CoreProtect | ❌ | ✅ | 按需切 MySQL |
| SkinsRestorer | ❌ | ✅ | 后端装，共 MySQL |
| Simple Voice Chat | ❌ | ✅ | 见 §8.7（本服 26.3 未部署，历史方案） |
| EasyBot | ❌ | ✅（主服） | 见 §8.6 |
| QuickMenu（26 章） | ❌ | ✅ | 见 §8.5 |
| BedrockPlayerSupport | ❌ | ✅ | 见 §8.5 |
| spark | ❌ | ✅（各服自用） | 代理端也可装 spark-velocity 看代理负载 |
| ViaVersion | ❌ | ✅ | 后端必须有；代理端也可装以提前协商版本 |
| OpenInv / OPanel / WorldEdit | ❌ | ✅ | 单服自用，无需代理（Chunky 本服未部署） |

### 8.5 BedrockPlayerSupport / QuickMenu 与基岩玩家识别

这两个插件在**后端**运行时通过 Floodgate API 判断"是不是基岩玩家"。
迁移后基岩玩家的真实身份由代理认证，后端读取 Floodgate 数据的做法：

1. **后端也装 floodgate-bukkit**，并配置它指向与代理同一个 key，让
   `isFloodgatePlayer` 在后端为 true。这是 QuickMenu 基岩分发
   `[bedrock-player]` 生效的前提。
2. 若不装后端 Floodgate，后端无法用 Floodgate API 区分基岩玩家，QuickMenu
   的"基岩走原生表单、Java 走箱子"会退化成全走 Java 分支。

> 结论：QuickMenu（26 章）与 BPS（25 章）要在多服下继续按客户端分流，
> 后端子服必须装 **floodgate-bukkit**、保持与代理同一 key，且代理端要开
> `send-floodgate-data: true`（见 §6.4）——三者缺一，后端都收不到基岩数据。

### 8.6 EasyBot（QQ 联动）

EasyBot 桥接的是"一个 MC 服务端 ↔ QQ"。多服下**只让它在主服跑**即可，
QQ 群看到的就是主服聊天；若要群消息广播到全子服，需配合各服的跨服聊天插件
（或让 EasyBot 做网关）。按当前部署，EasyBot 留在主服不动即可。

### 8.7 Simple Voice Chat（语音）

Simple Voice Chat 是**点对点 UDP**，不经过 Velocity 的 TCP 转发。跨子服说话
依赖各服共享语音服务器。多服架构下建议：
- 各子服独立语音：各自开 UDP 24454 并放行对应端口；
- 需跨服互通语音：用 SVC 的集中 `voicechat-server` 模式（较复杂，按需）。

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

| 项 | 必须 | 说明 |
|---|---|---|
| 子服绑 `127.0.0.1` + 从安全组移除 | ✅ | 防直连伪造 |
| `proxies.velocity.enabled=true` + secret 一致 | ✅ | modern 转发密钥（1.18.2+ 的配置名；旧文 `velocity-support.enabled` 已废弃） |
| `paper-global.yml` 的 `online-mode` 镜像代理 | ✅ | 值必须一致，见 §5.2 |
| `force-key-authentication=false`（4.x 外置登录） | ✅ | 开着会踢掉所有外置登录玩家，见 §3.1 |
| `send-floodgate-data=true` + 前后端同一 key.pem | ✅ | 后端 floodgate API 生效前提，见 §6.4 |
| YOP 端口代理 / 后端错开（32218 / 32217） | ✅ | 双进程抢 32217 会让后起的 agent 失败 |
| 全端加载 Yggdrasil agent | ✅ | 外置登录识别 |
| `minecraft.command.op` 不给任何组 | ✅ | 见 05 章 |
| forwarding.secret 保密 | ✅ | 泄露等同能伪造玩家 |

### 9.3 常见性能/配置误配

- 子服仍开 `spigot.yml bungeecord: true` → 与 modern 转发冲突，登录异常。应 `false`。
- Velocity 4.x 忘关 `force-key-authentication` → 外置登录玩家被公钥签名校验踢出。应 `false`。
- 把 `player-info-forwarding-mode` 设成 `none` → 后端拿到离线 UUID，全体玩家"换号"。必须 `modern`。
- 前后端 Floodgate key.pem 不是同一份 → 基岩玩家 UUID 全变，背包 / 余额 / 领地全丢。
- 代理内存给太大没意义；代理吃的是网络吞吐不是内存。
- 忘了在安全组放行 **19132 TCP + UDP** → 基岩连不上（纯 NetherNet 两条都要；Java 走 55551 不受影响）。

---

## 10. 排错与备选方案

### 10.1 排错速查

| 现象 | 原因 | 处理 |
|---|---|---|
| 连代理显示"无法验证用户名/离线" | velocity `online-mode` 或 agent 没配好 | 确认 `online-mode=true` 且代理加载 Yggdrasil agent |
| 进子服被踢 "requires you to connect with Velocity" | 子服没开 velocity 支持或 secret 不符 | 改 paper-global.yml `enabled:true` + secret 与 forwarding.secret 一致 |
| 玩家名字对但 UUID 全变 | forwarding 未生效（`none` 模式 / secret 不符 / paper velocity 段没开） | 确认 `modern` + secret 一致 + `proxies.velocity.enabled=true`；见 §5.5 |
| 基岩能进代理进不了子服 | 后端没装 floodgate-bukkit 或 key 不一致 | 后端装 floodgate-bukkit 并指到代理同 key |
| `/server` Tab 无补全 | `announce-proxy-commands=false` | 设 true |
| 外置登录玩家被踢 "invalid public key signature" | 4.x `force-key-authentication` 仍为 true | 设 `false` |
| 基岩玩家 UUID 全变 / 数据丢失 | 前后端 key.pem 不同源，或 Floodgate 版本不一致 | 同步同一 key + 同版本，见 §6.4 |
| 代理起不来，报 32217 端口占用 | 后端与代理的 YOP 抢同一端口 | 代理 conf 改 32218 |
| 进服后背包是空的、世界是新的 | 复制项目时漏了 `world/` 目录 | 从原服补齐世界数据，见 §5.5 |
| 从子服换服后物品/背包乱 | 各子服数据独立属正常 | 用跨服背包插件按需 |

### 10.2 备选：MultiLogin 统一登录

若希望**一处配置对接多个外置登录站**（而非每端塞 agent），可在代理端用
**MultiLogin** 插件替代"全端同 agent"。代价是换皮肤/背包 UUID 逻辑不同，
对已定型的 YggdrasilOfficialProxy 方案改动较大，一般不推荐直接切。

### 10.3 备选：Floodgate 本地绑定数据库

若不想依赖 Floodgate 官方全局绑定服务器（隐私 / 离线场景），可在代理装
**floodgate-sqlite-database**，并把 Floodgate 的 `player-link` 配成
`type: sqlite`、`enable-own-linking: true`。绑定数据落在代理本地
`linked_players.db`。

---

## 11. 与其他章节的关系

| 章节 | 关系 |
|---|---|
| [服务器核心-Leaf](../服务器核心-Leaf/) | Leaf 改造为 Velocity 的后端子服 |
| [外置登录代理](../外置登录代理-YggdrasilOfficialProxy/) | 全端加载 Yggdrasil agent，Velocity 承担认证 |
| [Java-Bedrock互通层-Geyser-Floodgate](../Java-Bedrock互通层-Geyser-Floodgate/) | Geyser+Floodgate 迁到代理端，后端按需装 floodgate |
| [权限管理-LuckPerms](../../5-服务器管理/权限管理系统-LuckPerms/) | LuckPerms 切 MySQL 实现跨服权限同步 |
| [端口与网络架构总览](../端口与网络架构总览/README.md) | 端口表需按本章重规划（对外仅 Velocity+Geyser） |
| [快捷菜单系统](../../自研/快捷菜单系统-QuickMenu/快捷菜单系统-QuickMenu.md) | 后端需装 floodgate-bukkit 才能继续按客户端分流 |
| Multiverse 多世界 | 见 §13：单服加资源世界 / 每个后端子服内部再用 Multiverse 管多世界 |
| 本次实装记录 | 副本 `.workbuddy/memory/2026-11-10.md`（部署日志）与本章 **§14**（落地校订与差异对照） |

> **演进路线图**（对齐 README §15）：单服 → 本章 Velocity → 多子服分区 →
> 共享 MySQL 数据层。本章是"单服 → 网络"的桥。

---

## 12. 迁移操作顺序（Checklist）

按序执行可把翻车率降到最低：

1. [ ] 下载 Velocity 4.x 稳定版（Fill v3 API）+ Velocity 版 Geyser / Floodgate jar
2. [ ] 确认 **Java 25**，建 `velocity/` 目录，首次启动生成配置后关闭
3. [ ] 改 velocity.toml：`online-mode=true`、**`force-key-authentication=false`**、`modern` 转发
4. [ ] 复制 `forwarding.secret` 备用；把代理 `YggdrasilOfficialProxy.conf` 端口改 **32218**
5. [ ] 改现有 Leaf：`online-mode=false`、`server-port=30002`、`server-ip=127.0.0.1`
6. [ ] 改 Leaf 的 paper-global.yml：`proxies.velocity.enabled=true` + `online-mode=true` + secret
7. [ ] 给 Velocity 与 Leaf 的启动脚本都加 Yggdrasil agent
8. [ ] 从 Leaf 移除内嵌 Geyser（改 `.jar.disabled` 便于回滚），后端 floodgate 按 §8.5 保留
9. [ ] 配置代理端 Geyser（**优先整份复用后端 config.yml**，`auth-type: floodgate`）+ Floodgate（`send-floodgate-data: true`、**同一 key.pem**）
10. [ ] 若用 QuickMenu/BPS，给后端装 floodgate-bukkit 并同 key
11. [ ] **补齐 `world/` 世界数据**（复制项目时最容易漏），按 §5.5 比对 UUID
12. [ ] 后端装 Multiverse-Core 5.8.2（见 §13），按需建资源世界
13. [ ] 启动 Velocity → 启动子服 → Java 外置登录 / 正版 / 基岩各测一次进入与 `/server` 换服
14. [ ] 端口：安全组只留 55551 TCP + **19132 TCP/UDP**；子服与 RCON 全部移除对外放行
15. [ ] 加第二个子服前，先完成 §7 的 LuckPerms / 经济共享 MySQL 迁移

---

## 13. 单服多世界：Multiverse（与 Velocity 互补，非替代）

> **一句话**：Multiverse-Core 是一个服务端插件，让你在**同一个 Leaf 进程里**开多个独立世界（主世界 / 资源界 / 小游戏 / 建筑区），
> 玩家用 `/mv tp` 或传送门在它们之间穿梭；它跟 Velocity 不冲突——Velocity 管"服与服"，Multiverse 管"世界与世界"。

### 13.1 版本与 26.3 支持（已核实）

| 项 | 信息 |
|---|---|
| 最新版 | **Multiverse-Core 5.8.2**（2026-10-26） |
| MC 版本 | 明确支持 **1.18.2 → 26.3**（Paper / Purpur / Spigot / Bukkit） |
| 许可 | BSD-3-Clause |
| 热度 | Modrinth 89 万+ 下载，活跃维护 |
| 玩家要装吗 | 不用，纯服务端插件 |
| 依赖 | 无强依赖；可选 Multiverse-Portals / -Inventories / -NetherPortals / -SignPortals |

> 你的 26.3 主服（Leaf 26.3-18 / Java 25）可直接装 5.8.2，无版本顾虑。

### 13.2 与你的架构怎么配合

| 你的现状 | 怎么用 Multiverse |
|---|---|
| **当前单 Leaf 服**（未上 Velocity） | 直接给 Leaf 装 Multiverse-Core，开资源世界/小游戏世界，**零架构改动**，不影响 Geyser/ViaVersion/Residence |
| **已上 Velocity 多服**（见 §1–§12） | **Multiverse 装在"后端子服"里，不装代理**。每个子服内部用 Multiverse 管自己的多世界；跨子服换服用 Velocity 的 `/server`，世界内换世界用 Multiverse 的 `/mv tp` |

> ⚠️ **Multiverse 永远装在子服（业务服务端），绝不装在 Velocity 代理进程**——代理只做转发，不加载游戏世界。

### 13.3 安装与基础命令

1. 下载 `multiverse-core-5.8.2.jar`（Modrinth / Hangar），丢进 Leaf 的 `plugins/`，重启。
2. 首次启动自动生成 `plugins/Multiverse-Core/` 配置。
3. 常用命令（权限节点见 §13.6）：

```bash
/mv create resource normal -t NORMAL     # 新建"资源世界"（normal 维度）
/mv create games normal -t NORMAL         # 新建"小游戏世界"
/mv tp resource                           # 传送到资源世界
/mv list                                  # 列出所有世界
/mv set spawn resource                    # 设置某世界出生点
/mv remove <世界名>                        # 删除世界（谨慎）
```

> 资源世界建议单独生成一个干净维度，挖完即弃；小游戏世界可加载独立地形生成器。

### 13.4 资源世界自动重置（重点）

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

### 13.5 配套插件

| 插件 | 作用 | 多服注意 |
|---|---|---|
| **Multiverse-Portals** | 用方块搭传送门在世界间穿梭 | 装子服，不装代理 |
| **Multiverse-Inventories** | 让不同世界有**独立背包/末影箱** | ⚠️ 这是"子服内分世界背包"，与 §7 的"跨服共享背包"是两码事；多服下仍按子服各自独立 |
| **Multiverse-NetherPortals** | 地狱门自动分流到对应世界的下界 | 装子服 |
| **Multiverse-SignPortals** | 用牌子点击传送 | 装子服 |

### 13.6 权限节点（LuckPerms）

Multiverse-Core 权限粒度细，常用节点（给对应玩家/组）：

| 节点 | 作用 |
|---|---|
| `multiverse.*` | 全部权限（管理员） |
| `multiverse.access.*` | 进入所有世界 |
| `multiverse.teleport.self.*` | 自己传送到各世界 |
| `multiverse.create.*` | 创建世界 |
| `multiverse.core.manage` | 管理世界（删除/重载） |

> 高级权限模式（如 ClearLag 检测到 LuckPerms 后走的那种）下权限需显式授予，非 op 玩家默认拿不到；op 默认拥有。

### 13.7 与 Velocity 共存要点（速记）

- **装法**：Multiverse 装后端子服；Velocity 是独立代理进程，不碰 Multiverse。
- **换服 vs 换世界**：跨子服用 `/server`（Velocity）；同服内跨世界用 `/mv tp`（Multiverse）。两条命令各管一层，不冲突。
- **数据**：Multiverse 的世界数据在各子服本地 `worlds/` 目录；跨服共享数据请走 §7 的 MySQL 方案，与 Multiverse 无关。

---

## 14. 落地校订与差异对照（2026-11-10 实测）

> 本章方案已在 `F://game//pc//MC//开服//服务器数据备份//leaf-26.3//leaf-26.3-多服`
> （从 26.3 单服复制出的副本）完整实装。本节记录**文档规划 vs 实际落地**的差异，
> 作为以后再次部署或新增子服时的校订依据。逐日执行日志见该副本
> `.workbuddy/memory/2026-11-10.md`。

### 14.1 实测环境与端口

| 项 | 文档规划 | 实际落地 |
|---|---|---|
| Velocity | 3.3.x / 3.4.x | **4.3.1-45**（Fill v3 API，config-version 2.9） |
| Java | 17+ / 推荐 21 | **Java 25**（4.x 最低要求） |
| Geyser / Floodgate | Velocity 版 | Geyser 2.12.1-b1298 / Floodgate 2.2.5-SNAPSHOT b141-81b65cc |
| Multiverse-Core | 5.8.2 | 5.8.2（装入后端 `plugins/`） |
| 对外 Java | 55551 TCP | 55551 TCP（沿用原端口） |
| 对外基岩 | 19132 UDP | **19132 TCP + UDP**（NetherNet 信令 / 媒体各一路） |
| 后端 survival | 127.0.0.1:30002 | 同左 |
| YOP | 未提 | 后端 32217 / 代理 **32218**（必须错开） |
| RCON | 未提 | 25595（保留，供 §13.4 资源世界重置） |
| 子服数量 | lobby / survival / minigames | **仅 survival**，`try = ["survival"]`；另两个在 toml 里注释预留 30001 / 30003 |

### 14.2 关键差异逐项

| # | 差异点 | 文档原写法 | 实测结论 |
|---|---|---|---|
| 1 | `force-key-authentication` | 未提 | 4.x 新增，外置登录必须 `false`，否则玩家被公钥签名校验踢出（已补 §3.1） |
| 2 | Floodgate `send-floodgate-data` | 未提 | 后端保留 floodgate-spigot 时必须 `true`（已补 §6.4） |
| 3 | Geyser 配置 | 按简化模板重写 | **整份复用后端 config.yml**（78 键 / cv12 完全一致），保住 NetherNet、zh_cn、ping 调优（已改 §6.3） |
| 4 | Floodgate key | 只说"同一 key" | 前后端必须**同一份文件**且**同版本**；副本 167 份 Essentials 数据里 139 份属基岩玩家，key 错 = 全体换号（已补 §6.4 / §5.5） |
| 5 | paper-global 配置项名 | `velocity-support.enabled` | 1.18.2+ 实为 `proxies.velocity.enabled`（已改 §9.2） |
| 6 | `proxies.velocity.online-mode` 释义 | "后端才会信任" | 官方定义是"如何处理 UUID / 数据"，镜像代理端；信任由 secret 验签决定（已改 §5.2） |
| 7 | LuckPerms | 规划共享 MySQL | 本次**仍是 h2 单服存储**；当前只有一个子服无影响，加第二个前必须切（已在 §7.1 标注） |
| 8 | 玩家数据 / UUID | 全文未覆盖 | 新增 **§5.5**：认证只做一次、offline 不等于离线 UUID、三类账号判定、数据核查清单、验证方法 |
| 9 | 下载 API | `api.papermc.io/v2` | 2026-07 起 **410 Gone**，改用 `fill.papermc.io/v3`（已改 §2.1） |
| 10 | 19132 协议 | 只写 UDP | 纯 NetherNet 需 **TCP + UDP** 同时放行（已改 §4.1 / §9.3） |
| 11 | Geyser-Velocity jar 下载 | 单连接 | 约 50MB 单连接 ~100KB/s 会超时，实测用 12 段 Range 并行拉取后校验 zip 完整性 |

### 14.3 验证边界与回滚

- **已验证**：31 项静态一致性校验全部 PASS；代理带全套插件试跑正常（55551 监听、Geyser 19132 信令启动、无 ERROR / Exception）。
- **未验证**：真实玩家登录。Velocity 4.x + YOP 2.3.0 的外置登录认证流程、基岩 Floodgate 端到端、皮肤回退、`/server` 换服，都还需要客户端与账号实测。
- **上线前必办**：补齐 `world/` 目录（副本里没有）、防火墙只放行 55551 TCP + 19132 TCP/UDP、按 §5.5 比对 UUID。
- **回滚**：副本 `_backup_单服配置_20261110/` 内有 5 个原配置文件；覆盖回原位、把
  `Geyser-Spigot.jar.disabled` 改回 `.jar`、删除 `velocity/` 与
  `plugins/multiverse-core-5.8.2.jar`，即可退回单服。
- **文档备份**：本次修订前的原文保存在同目录
  `多服Velocity +Multiverse.md.bak_20261110`。


---

## 15. 小游戏子服落地（2026-11-10）

> 承接 §14 的「仅 survival」架构，本次新增 `minigames` / `skyblock` 两个后端子服。
> 生产环境为云端**同一台**机器：32G 内存 / Xeon Gold 6148（20C40T @ 2.4GHz）/ **12M 公网带宽**。
> 本地副本为配置演练，上线时按同一套规则同步。

### 15.1 架构决策与资源预算

| 进程 | 端口 | 绑定 | 堆内存 | 视距 / 模拟 | 玩法 |
|---|---|---|---|---|---|
| Velocity | 55551 TCP、19132 TCP+UDP | `0.0.0.0` | 512M | — | Geyser / Floodgate / YOP 32218 |
| survival | 30002 | `127.0.0.1` | **10G / 4G**（原 24G） | **8** / 4（原 12） | 生存主服，YOP 32217 |
| minigames | **30003** | `127.0.0.1` | 3G / 1G | 6 / 4 | 起床战争 + 幸运之柱，YOP 32219 |
| skyblock | **30004** | `127.0.0.1` | 3G / 1G | 6 / 4 | 空岛 / 单方块，YOP 32220 |

合计约 **16.5G / 32G**；磁盘新增约 0.9G（每服精简复制 core + libraries + versions + cache，不带 bluemap 与日志）。

**为什么是「折中 2 服」而不是单服多世界、也不是 3 服 + 大厅**：

- BedWars / FortunePillars 是短局玩法，会频繁重建竞技场；BSkyBlock 是持久岛屿资产。同一进程下一次竞技场重建占满主线程会连带卡住空岛玩家。Xeon 6148 单核仅 2.4GHz（比本机 i5-13600KF 弱 40–50%），主线程竞争比高主频机器更明显，所以隔离是必要的。
- 多个 JVM 主线程可分散到不同物理核；**代理 → 后端全走 `127.0.0.1` 回环，不增加公网带宽**。
- 没拆到 3 服 + 大厅：12M 带宽把总在线锁在 20–30 人，继续拆的吞吐收益有限，却要多付 4 套 secret / 端口 / key 与跨服 MySQL 成本。`lobby 30001` 继续注释预留，将来可一行加上。

### 15.2 带宽是唯一硬瓶颈（与架构无关）

12 Mbps ≈ **1.5 MB/s 出方向**。服务器发给玩家的数据走公网，按 MC 经验值估算（非实测）：

| 视距 | 每玩家稳态下行 | 12M 理论上限 | 扣 20% 给网页 / 资源包 / Geyser |
|---|---|---|---|
| 12（原值） | 50–150 KB/s | 15–30 人 | **12–20 人** |
| 8（survival 现值） | 25–60 KB/s | 30–60 人 | 25–45 人 |
| 6（子服现值） | 15–40 KB/s | 40–100 人 | 35–70 人 |

进服 / 跨世界 / 跨服时区块爆发 1–3 MB/s/人，12M 下同时两三人换场景即打满。跨服与跨世界的**带宽差 <5%**（都要重载区块），差别只在多一次 TCP 握手的 1–2 秒体感卡顿。

必做优化：① 子服 `view-distance=6`、survival 降到 8；② BlueMap / Plan 只在 survival 开，小游戏服不装；③ 资源包保持 RPM `autoHost` 远程托管（实测本来就不占这 12M），但必须把 `selfHostPort` 固定为 **30005**，否则本地兜底会抢占 minigames 的 30003 —— 详见 **§16**；④ 公网只放行 55551 TCP + 19132 TCP/UDP。

### 15.3 插件选型与 MC 26.3 实测

| 玩法 | 实现 | 26.3 实测 |
|---|---|---|
| 起床战争 | Screaming BedWars **0.2.44**（原服已有） | ✅ enable；⚠️ 警告「26.3 newer than latest supported (26.2)」，走 26.2 fallback NMS，属未声明支持 |
| 幸运之柱 | **FortunePillars 4.6.0**（Paper 插件，0.7MB） | ✅ 完整 enable（SQLite / 命令 / 监听器），自称只兼容 1.21.1–1.21.8，实测 26.3 可用 |
| 空岛 / 单方块 | BentoBox **3.23.6** + BSkyBlock **1.20.0** + AOneBlock **1.28.0** | ✅ 两个 addon 均加载，官方明确支持 Paper 1.21.5–26.3 / Java 25+ |
| 世界管理 | Multiverse-Core 5.8.2 | ✅ |
| 共存 | 四套插件同一进程 | ✅ `Done (129s)`，无 Exception / 插件冲突 |

> **幸运之柱的坑**：国内原版「幸运之柱」（Lucky-Pillar / JK's Pillars of Fortune）是 **Fabric / 数据包**（1.21.11），不是 Bukkit 插件；Modrinth 上的 Paper 插件版 FortunePillars 也只声明到 1.21.9。本机隔离试跑确认插件版在 26.3 可加载，故采用插件版，无需另起 Fabric 服或把全服数据包挂进 `world/datapacks`。
> BentoBox 首启会通过 SpigotLibraryLoader 拉 10 个 JDBC 驱动与约 100 个 `.pom`（耗时约 1分44秒）；已全部缓存进 `skyblock/libraries/`，二次启动免联网。

### 15.4 配置要点

- 两个子服一律 `online-mode=false` + modern 转发 + 只绑 `127.0.0.1`；`paper-global.yml` 的 secret 与 `velocity/forwarding.secret` 完全一致。
- YOP 四进程端口错开：32217 survival / 32218 velocity / 32219 minigames / 32220 skyblock。
- **四端 Floodgate `key.pem` 是同一份**（md5 `902d4f90…`），基岩玩家 UUID 不会换号。
- 两个子服 `level-name` 独立（`mg_hub` / `skyblock_hub`），不共享 survival 任何玩家存档；子服关闭 RCON。
- `try` 仍只指向 `survival`，玩家用 `/server minigames` / `/server skyblock` 进入；`bungee-plugin-message-channel = true` 已开，BedWars 的 Velocity 集成可直接用。
- ⚠️ Leaf 首启会规范化 `paper-global.yml`（去掉 secret 多余引号与自定义注释），**secret 值本身不变**，校验时应按解析值比对而非字符串全等。

### 15.5 验证边界与待办

- **已验证**：25 项一致性校验全 PASS；minigames 试跑 `Done (13.1s)`、skyblock `Done (108.4s)`、Velocity 解析新 toml `Done (1.23s)`，三者均无 ERROR / Exception。
- **未验证 / 需人工**：
  1. 三个玩法都还没建场地：BedWars `/bw admin add`、FortunePillars `/tof`、BSkyBlock `/island`（或 `/is`）。
  2. 真实玩家登录（外置登录 / 正版 / 基岩）、`/server` 跨服、皮肤回退。
  3. 各子服玩家数据 / Essentials 余额 / 背包互相独立；LuckPerms 仍是 `h2`，加更多子服前按 §7 切共享 MySQL。
  4. 子服未装 ViaVersion / PlaceholderAPI / LuckPerms，按需补。
  5. BedWars `bungee.enabled` 默认 false；若要「进服自动开局 + 结束送回 survival」，按知识库 `尖叫起床-BedWars/官方文档汉化/BungeeCord模式` 配置。
  6. 副本仍缺 survival 的 `world/` 目录，上线前从原服补齐（见 §5.5）。

### 15.6 回滚

`_backup_小游戏部署前_20261110/` 内 6 个文件覆盖回原位（含 `velocity.toml`、`start.bat`、`server.properties`）；删除 `minigames/` 与 `skyblock/`；即可退回单 survival 架构。本次修订前的文档备份为同目录 `多服Velocity +Multiverse.md.bak_小游戏部署前_20261110`。


---

## 16. 资源包策略：玩家可选择删除（2026-11-11）

> 需求：玩家可以选择拒绝 / 删除资源包，不做强制。
> 涉及代理端 Geyser、survival 的 ResourcePackManager（RPM），以及 Velocity 迁移后断开的基岩资源包链路。

### 16.1 改前现状

| 端 | 控制点 | 改前 | 玩家能删 |
|---|---|---|---|
| Java · 三个后端 | `server.properties` | `require-resource-pack=false`、`resource-pack=` 空 | ✅ 后端不推 |
| Java · survival | `ResourcePackManager/config.yml` | `forceResourcePack: false`、提示语 `Use recommended resource pack?` | ✅ 可拒绝、可删客户端缓存 |
| Java · minigames / skyblock | 未装 RPM / FMM | 不推包 | ✅ 天然无包 |
| **基岩（全局）** | `velocity/plugins/Geyser-Velocity/config.yml` | `force-resource-packs: true` + `enable-integrated-pack: true` | ❌ **拒绝下载即断线** |

Geyser 配置注释明说：`If enabled [integrated pack], force-resource-packs will be enabled.` 所以**只改 `force-resource-packs` 无效**，必须两项一起关。

### 16.2 落地改动

| 文件 | 项 | 改前 → 改后 | 说明 |
|---|---|---|---|
| `velocity/plugins/Geyser-Velocity/config.yml` | `force-resource-packs` | `true` → **`false`** | 基岩玩家可拒绝下载也能进服 |
| 同上 | `enable-integrated-pack` | `true` → **`false`** | 关掉 Geyser 自带的强制包；否则会顶回上一项 |
| 同上 | `enable-custom-content` | 保持 `true` | Velocity 用 transfer packet，Geyser 官方建议保留 |
| `plugins/ResourcePackManager/config.yml` | `selfHostPort` | `-1` → **`30005`** | 见 §16.4 |
| 同上 | `networkHttpOffset-v2` | `1` → **`3`** | 即使日后改回 `-1`，推导仍是 30005 |
| `plugins/ResourcePackManager/.rspm-geyser-mapping-target` | 内容 | 原服路径 → 副本内路径 | 见 §16.3 |

Java 端的 `forceResourcePack: false` / `autoHost: true` / 三个后端的 `require-resource-pack=false` **均未改动**——它们本来就是可选的。注意「删缓存」≠「不再提示」：RPM 每次进服仍弹一次提示，选「否」即可；要彻底不弹得停用 RPM 推送。

### 16.3 迁移断点：RPM 的基岩产物没送到代理端

Geyser 从后端搬到代理后，`Geyser-Spigot.jar` 改名为 `.jar.disabled`，但 RPM 自动部署的产物仍全部留在**已停用的后端目录**里，代理端 `packs/` 是空的：

| 源（后端 survival） | 目标（代理端 Geyser-Velocity） | 大小 |
|---|---|---|
| `plugins/Geyser-Spigot/extensions/ResourcePackManager.jar` | `velocity/plugins/Geyser-Velocity/extensions/` | 10.8 MB |
| `plugins/Geyser-Spigot/custom_mappings/rspm_geyser_mappings.json` | `velocity/plugins/Geyser-Velocity/custom_mappings/` | 28 KB |
| `plugins/ResourcePackManager/output/ResourcePackManager_Bedrock.zip` | `velocity/plugins/Geyser-Velocity/packs/` | 84 KB |

三件已同步（md5 全一致）。按 [Geyser 官方 wiki](https://geysermc.org/wiki/geyser/packs/)，`packs/` 里的 `.zip` / `.mcpack` 在 Geyser 重启后下发给连接玩家；配合 `force-resource-packs: false`，就成了「可拒绝的自定义模型包」。

新增根目录 **`sync-bedrock-pack.bat`**：每次 `/rspm reload`、RPM 更新扩展或改动自定义模型后跑一次，然后**重启 Velocity**（Geyser 只在启动时加载 extension 与 `custom_mappings`）。脚本已实测跑通。

> ⚠️ 没有把 `bedrockGeyserFolder` 改成指向代理端的绝对路径：RPM 的 `GeyserDeployer` 有 `invalidPath` / `Refusing to record invalid Geyser mapping target` 校验，只认同目录树 `plugins/Geyser-*` 下的目标，跨树路径不可靠。改用幂等同步脚本，不赌插件内部行为。
> `.rspm-geyser-mapping-target` 原本指向**另一个项目** `leaf-26.3\plugins\...`（复制项目时带过来的陈旧记录，RPM 启动时会尝试清理旧目标，有误删原服 mappings 的风险），已改指副本自身。

### 16.4 带宽结论更正与端口冲突

§15.2 原写「资源包走 CDN 不占这 12M」。实测 RPM `autoHost: true` **已把 96KB 的 Java 包托管在 magmaguy.com**，本来就不走云端出口。真正的风险是 `selfHostEnabled: true` 在远程托管失败时回退到本地 HTTP：

```
selfHostPort: -1  +  networkHttpOffset-v2: 1  →  30002 + 1 = 30003
```

而 **30003 正是 minigames 子服端口**。目前没触发（`preferSelfHost` 的公网可达验证在后端绑 `127.0.0.1` 时会失败、自动回退远程托管），但一旦显式设了 `selfHostExternalHost` 或 magmaguy.com 不可用，就会抢占 30003 导致 minigames 起不来。已固定为 30005，并把 offset 改为 3 作双保险。

### 16.5 代价与限制

- 关掉 integrated pack 后，基岩玩家若拒绝 / 删除资源包，自定义方块 / 物品 / 头颅映射失去贴图支撑，冷却指示器可能出现黑块（可在基岩「辅助功能 → 文本背景不透明度 = 0」缓解）。
- Geyser 官方明确：**代理模式下无法按后端子服分别下发资源包**（Bedrock 只在初次连接时添加 / 移除 pack）。想按服区分需 transfer packet + GeyserPackSync 之类扩展。所以「进 minigames 不加载、进 survival 才加载」做不到；好在跨服时已加载的 pack 会保留、不重复弹窗。
- 小游戏两子服不装 RPM / FMM，天然不推包。
- Geyser **不会**把 Java 资源包转成基岩包，这正是 RPM `bedrockConversionEnabled` 的价值。

### 16.6 验证边界与回滚

- **已验证**：27 项一致性校验全 PASS；Velocity 试跑 `Done (3.17s)`、监听 55551、Geyser 加载扩展 `ResourcePackManagerGeyserBridge`、`Registered 128 custom items`（自定义映射生效）、无 ERROR / Exception；`sync-bedrock-pack.bat` 实测 `[OK]` 三项。
- **未验证**：真实基岩客户端拒绝下载后能否正常进服、自定义模型端到端显示。试跑日志里扩展 health 为 `loadedDefinitions=0 packUuid=none`，模型定义是否推送需要玩家登录后才能确认。
- **回滚**：`_backup_资源包修复前_20261111/` 内 3 个文件覆盖回原位；删除代理端 `extensions/ResourcePackManager.jar`、`custom_mappings/rspm_geyser_mappings.json`、`packs/ResourcePackManager_Bedrock.zip` 与根目录 `sync-bedrock-pack.bat`。文档修订前原文为 `.bak_资源包修复前_20261111`。
