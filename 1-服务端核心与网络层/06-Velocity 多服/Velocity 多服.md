# 27. Velocity 多服（代理 / Proxy）

本文档面向准备把现有单服扩展为多子服网络的服主，给出从单端无代理迁移到 Velocity 代理的完整落地步骤。它涵盖代理选型、velocity.toml 配置、后端子服对接、Geyser/Floodgate 迁移、权限与经济跨服同步、各现有插件逐项适配、安全基线，以及按序执行的迁移 Checklist。

> **一句话**：在现有单服（Leaf + Yggdrasil 外置登录 + Geyser 基岩互通）前面加一层
> **Velocity 代理**，把"一台服务器"升级成"一个网络"，玩家从一个地址进入，
> 可无缝穿梭多个子服（大厅 / 生存 / 小游戏 / 建筑……），换服不掉线。

> ⚠️ **前置说明**：你的项目当前是**单端无代理**（`bungeecord: false`，见 README §5.1）。
> 本章是把单服迁移到 Velocity 网络的**完整落地教程**。若暂时只有一台服务器、
> 暂不需要分区玩法，可跳过本章；一旦要分服（生存/空岛/RPG 并存），本方案是 PaperMC
> 官方推荐的现代替代（Waterfall 已 EOL，官方建议全部迁移到 Velocity）。

---

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
3. **Java 17+ 即可跑**（官方推荐 Java 21），你的 JDK 21 完全满足。
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

从 PaperMC 官方下载页获取稳定版（当前主流 **3.3.x / 3.4.x**，默认端口 25565）：

- 官网：<https://papermc.io/downloads/velocity>
- API 直链（自动取最新构建）：`https://api.papermc.io/v2/projects/velocity/versions/<版本>/builds/latest/downloads`
- 项目文档：<https://docs.papermc.io/velocity/>

**务必下载 `velocity-x.y.z.jar`（代理端），不要下成其余构建产物。**
Velocity 要求 Java 17+，推荐 Java 21。

### 2.2 目录规划（建议独立文件夹）

```
C:\mc_serve\
├── velocity\                  ← 新建：Velocity 代理（本章主角）
│   ├── velocity-3.3.0.jar
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
java -Xms512M -Xmx512M -XX:+UseG1GC -jar velocity-3.3.0.jar
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

# ★ 转发模式：modern 最安全，Leaf/Paper 原生支持
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
| Geyser `port` | velocity/plugins/geyser/config.yml | 基岩监听 UDP 端口（沿用 19132） |

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
| **19132** | UDP | `0.0.0.0` | ✅ | Geyser 基岩入口（装在代理） |
| **24454** | UDP | 视方案 | ⚠️ | Simple Voice Chat 语音（见 §8.7） |
| 30001/30002… | TCP | `127.0.0.1` | ❌ | 各子服（仅代理可达） |

**安全铁律**：所有子服的 `server.properties` 设 `server-ip=127.0.0.1`（或内网 IP），
并把它们**从云安全组 / 防火墙的对外放行里移除**。否则一旦有人直连 30002，
配合 `online-mode=false` 就能伪造任意玩家（含 OP）进入——这是最致命的安全漏洞。

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

> **为什么这里是 true**：虽然子服 `server.properties` 是 `online-mode=false`，
> 但 paper-global.yml 的 `velocity.online-mode` 要**镜像代理端**的 `online-mode=true`，
> 这样后端才会信任来自 Velocity 的"已认证玩家"信息。两个值必须一致。

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
  -jar velocity-3.3.0.jar
```

**每个子服（Leaf）启动脚本**：在现有 `-javaagent:YggdrasilOfficialProxy...` 基础上
原样保留即可（你现有 02 章已把 YggdrasilOfficialProxy 打包成可注入的 jar），
代理端也加同一个 agent。

### 5.4 换服指令

Velocity 自带 `/server <子服名>`，权限节点 `velocity.command.server`。
玩家输入 `/server survival` 即从大厅瞬移到生存服，不掉线。
可用 `velocity.command.list` 查看子服列表，`velocity.command.send` 管理用。

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

### 6.3 Geyser 配置（velocity/plugins/geyser/config.yml）

首次启动生成后编辑关键项：

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
# 非绑定基岩玩家的名字前缀（用". "避免与 Java 玩家撞名）
username-prefix: "."

# 账号绑定：让基岩玩家的 UUID 与 Java 外置登录一致，跨子服保持同一身份
player-link:
  enable-own-linking: false   # false = 用 Floodgate 官方全局绑定
  # enable-own-linking: true  # 若走本地绑定数据库再开，并装 floodgate-sqlite
```

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
| Simple Voice Chat | ❌ | ✅ | 见 §8.7 |
| EasyBot | ❌ | ✅（主服） | 见 §8.6 |
| QuickMenu（26 章） | ❌ | ✅ | 见 §8.5 |
| BedrockPlayerSupport | ❌ | ✅ | 见 §8.5 |
| spark | ❌ | ✅（各服自用） | 代理端也可装 spark-velocity 看代理负载 |
| ViaVersion | ❌ | ✅ | 后端必须有；代理端也可装以提前协商版本 |
| OpenInv / OPanel / Chunky / WorldEdit | ❌ | ✅ | 单服自用，无需代理 |

### 8.5 BedrockPlayerSupport / QuickMenu 与基岩玩家识别

这两个插件在**后端**运行时通过 Floodgate API 判断"是不是基岩玩家"。
迁移后基岩玩家的真实身份由代理认证，后端读取 Floodgate 数据的做法：

1. **后端也装 floodgate-bukkit**，并配置它指向与代理同一个 key，让
   `isFloodgatePlayer` 在后端为 true。这是 QuickMenu 基岩分发
   `[bedrock-player]` 生效的前提。
2. 若不装后端 Floodgate，后端无法用 Floodgate API 区分基岩玩家，QuickMenu
   的"基岩走原生表单、Java 走箱子"会退化成全走 Java 分支。

> 结论：QuickMenu（26 章）与 BPS（25 章）要在多服下继续按客户端分流，
> 后端子服必须装 **floodgate-bukkit** 并保持与代理 Floodgate 同一 key。

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
| `velocity-support.enabled=true` + secret 一致 | ✅ | modern 转发密钥 |
| `paper-global.yml` 的 `online-mode` 镜像代理 | ✅ | 值必须一致 |
| 全端加载 Yggdrasil agent | ✅ | 外置登录识别 |
| `minecraft.command.op` 不给任何组 | ✅ | 见 05 章 |
| forwarding.secret 保密 | ✅ | 泄露等同能伪造玩家 |

### 9.3 常见性能/配置误配

- 子服仍开 `spigot.yml bungeecord: true` → 与 modern 转发冲突，登录异常。应 `false`。
- 代理内存给太大没意义；代理吃的是网络吞吐不是内存。
- 忘了在安全组放行 UDP 19132 → 基岩连不上（Java 正常）。

---

## 10. 排错与备选方案

### 10.1 排错速查

| 现象 | 原因 | 处理 |
|---|---|---|
| 连代理显示"无法验证用户名/离线" | velocity `online-mode` 或 agent 没配好 | 确认 `online-mode=true` 且代理加载 Yggdrasil agent |
| 进子服被踢 "requires you to connect with Velocity" | 子服没开 velocity 支持或 secret 不符 | 改 paper-global.yml `enabled:true` + secret 与 forwarding.secret 一致 |
| 玩家名字对但 UUID 全变 | 子服 properties 与 paper velocity 段 online-mode 不一致 | 统一：properties=false，paper velocity online-mode=true |
| 基岩能进代理进不了子服 | 后端没装 floodgate-bukkit 或 key 不一致 | 后端装 floodgate-bukkit 并指到代理同 key |
| `/server` Tab 无补全 | `announce-proxy-commands=false` | 设 true |
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
| [01 服务器核心-Leaf](../1-服务端核心与网络层/01-服务器核心-Leaf/) | Leaf 改造为 Velocity 的后端子服 |
| [02 外置登录代理](../1-服务端核心与网络层/02-外置登录代理-YggdrasilOfficialProxy/) | 全端加载 Yggdrasil agent，Velocity 承担认证 |
| [03 Java-Bedrock互通层-Geyser-Floodgate](../1-服务端核心与网络层/03-Java-Bedrock互通层-Geyser-Floodgate/) | Geyser+Floodgate 迁到代理端，后端按需装 floodgate |
| [05 权限管理-LuckPerms](../../5-服务器管理/01-权限管理系统-LuckPerms/) | LuckPerms 切 MySQL 实现跨服权限同步 |
| [17 端口与网络架构总览](../1-服务端核心与网络层/05-端口与网络架构总览/README.md) | 端口表需按本章重规划（对外仅 Velocity+Geyser） |
| [26 快捷菜单系统](../../ai写的/快捷菜单系统/快捷菜单系统-QuickMenu.md) | 后端需装 floodgate-bukkit 才能继续按客户端分流 |

> **演进路线图**（对齐 README §15）：单服 → 本章 Velocity → 多子服分区 →
> 共享 MySQL 数据层。本章是"单服 → 网络"的桥。

---

## 12. 迁移操作顺序（Checklist）

按序执行可把翻车率降到最低：

1. [ ] 下载 Velocity 稳定版 + Velocity 版 Geyser / Floodgate jar
2. [ ] 建 `velocity/` 目录，首次启动生成配置后关闭
3. [ ] 改 velocity.toml：`online-mode=true`、`player-info-forwarding-mode="modern"`
4. [ ] 复制 `forwarding.secret` 备用
5. [ ] 改现有 Leaf：`online-mode=false`、`server-port=30002`、`server-ip=127.0.0.1`
6. [ ] 改 Leaf 的 paper-global.yml：开 velocity 支持 + secret
7. [ ] 给 Velocity 与 Leaf 的启动脚本都加 Yggdrasil agent
8. [ ] 从 Leaf 移除内嵌 Geyser/Floodgate（迁到代理）
9. [ ] 配置代理端 Geyser(`auth-type:floodgate`)+Floodgate
10. [ ] 若用 QuickMenu/BPS，给后端装 floodgate-bukkit 并同 key
11. [ ] 启动 Velocity → 启动子服 → Java/基岩各测一次进入与 `/server` 换服
12. [ ] 端口：安全组只留 Velocity TCP + Geyser UDP；子服全部移除
