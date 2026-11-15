# 17. 端口与网络架构总览

本文档汇总本服所有对外与内部端口的用途、协议、绑定地址与公开状态，并给出网络拓扑，是端口清单的**唯一权威来源**（主 `README.md` 的「网络与端口总表」指向本文，不再单独维护表格）。数值取自本地 26.3 主服 `server.properties`、`plugins/*/config.yml` 与 `*.conf` 的实际配置及服主确认口径（2026-11-15 核对），不含公网 IP、Token、密码。覆盖 Java / 基岩游戏入口、资源包下发、各 Web 面板、数据库、认证代理、QQ 机器人桥接，以及未启用与多服扩展的预留端口。

## 1. 当前实跑端口

| 端口 | 协议 | 用途 | 绑定地址 | 公开 |
|---|---|---|---|---|
| **55551** | TCP | Java 游戏主端口（`server-port`） | 0.0.0.0 | 是 |
| **19132** | **TCP + UDP** | Geyser 基岩入口：NetherNet 内置信令走 TCP、WebRTC 媒体走 UDP，**同号**。`bedrock.port: 19132`、`transport: nethernet`、`webrtc-port: 0`、`clone-remote-port: false` | 0.0.0.0 | 是 |
| **55552** | TCP (HTTP) | ResourcePackManager 资源包自托管：`selfHostPort: -1` 自动推导为 MC 端口 + `networkHttpOffset-v2: 1` | 0.0.0.0 | 条件：自托管探测通过时对外 |
| **8100** | TCP (HTTP) | BlueMap 内置 webserver（`webserver.conf` → `enabled: true`、`port: 8100`） | 0.0.0.0（5.29 无 `bind` 键） | 建议仅内网 / 经反代 |
| **3000** | TCP (HTTP) | OPanel Web 管理面板（`webServerPort`） | 0.0.0.0 | 建议仅内网 |
| **8804** | TCP (HTTP) | Plan Web 仪表盘（`Webserver.Port`，经域名 `:8804` 访问） | 0.0.0.0 | 是 |
| **8765** | TCP (HTTP) | ClearLag++ 只读网页仪表盘（`web-dashboard`，令牌保护、5 秒刷新） | 0.0.0.0 | 建议仅内网 |
| **25576** | TCP | OPanel MCDR Socket（`mcdrSocketPort`） | 127.0.0.1 | 否 |
| **25595** | TCP | RCON（`enable-rcon: true`、`rcon.port`） | 本机 | 否 |
| **3001** | TCP (WS) | NapCat OneBot WebSocket | 127.0.0.1 | 否 |
| **5000** | TCP (HTTP) | EasyBot Web 管理面板（`web_api.Url`） | 0.0.0.0 | 建议仅内网 |
| **26990** | TCP (WS) | EasyBot Bridge（插件 ←→ 主程序） | 0.0.0.0 | 同机：否 / 跨机：需放行 |
| **14502** | TCP (HTTP) | authlib-injector 本地 httpd | 本机 | 否 |
| **32217** | TCP (HTTP) | Yggdrasil 认证服务器（YggdrasilOfficialProxy） | localhost | 否 |
| **3306** | TCP | MySQL（CoreProtect `use-mysql: true`、Plan、TAB、SkinsRestorer、SimplePets、Vault、AuctionHouse、BedWars、EconomyShop、EzTax、PlanTop、Quests 等） | localhost / 云端数据库 | 否 |

> 本文档 OPanel Web 按统一口径记为 **3000**（[OPanel 官方配置文件](https://opanel.cn/docs/configuration) 中 `webServerPort` 的默认值）。本地 26.3 主服 `plugins/OPanel/config.yml` 由服主单独设为 `25555`（2026-11-15），属另一套环境，不据此改端口表。面板公网可达时务必保留 `accessKey` 鉴权，并建议反代 + HTTPS。

> 基岩端口在 2026-11-08 NetherNet 迁移后由「跟随 Java 55551」改为独立的 **19132 TCP+UDP**；`clone-remote-port` 现为 `false`，改 Java 端口不再联动。放行时 TCP 与 UDP 缺一不可，详见 [02-NetherNet迁移准备.md](../Java-Bedrock互通层-Geyser-Floodgate/02-NetherNet迁移准备.md)。

## 2. 可选 / 外部链路

| 端口 | 协议 | 用途 | 说明 |
|---|---|---|---|
| 80 / 443 / 8080 | TCP (HTTP/HTTPS) | nginx / Caddy 反代 | **本地 Windows 主服未部署 Caddy、未绑域名**；仅云端站点侧用于 BlueMap、静态资源包与各 Web 面板的统一 HTTPS，是否公开取决于该侧部署 |
| 443（出站） | TCP (HTTPS) | Geyser 外部信令回退 | `signaling.mode: builtin` 为主；内置信令起不来时回退 `https://agent.warden.cloud`，无需本机放行额外入站端口 |

> BlueMap 5.29 **没有 `webserver.bind` 键**，无法在插件内把 8100 绑到 `127.0.0.1`；限制访问只能靠防火墙或 nginx/Caddy 反代（反代指向 `127.0.0.1:8100`）。详见 [BlueMap.md](../../3-玩法与玩家功能插件/网页世界地图/BlueMap.md)。

## 3. 未启用与配置预留

| 端口 | 状态 | 说明 |
|---|---|---|
| 25565 | 未监听 | `query.port`，`enable-query: false` |
| 25575 | 未监听 | EasyBot `adapter.native_rcon.port`（连接目标），`use_native_rcon: false` 且密码为空；**不是**服务端 RCON 端口 |
| 8123 | 未监听 | CoreProtect `clickhouse-port`，当前 `use-mysql: true` 走 MySQL，ClickHouse 配置仅预留 |
| 19133 | 未启用 | NetherNet `transport: both` 或独立 `webrtc-port` 时才需要；本服 `webrtc-port: 0` 复用 19132 |
| 19134 | 未启用 | 内置信令 TCP 端口冲突时，用 `-DgeyserSignalingPort=19134` 指定的独立信令端口；本服未使用 |
| 0 | 未启用 | `management-server-port: 0`、`management-server-enabled: false` |
| 24454 / 6379 / 5672 / 26379 | 未启用 | 依次为 Simple Voice Chat（多服规划）、TAB Redis、TAB AMQP、LuckPerms Redis 的配置示例值 |

## 4. 多服 / 第二套端口映射（规划）

来自 [多服网络与端口架构.md](<../多服Velocity +Multiverse/多服网络与端口架构.md>) §7，用于同机跑第二套服务端或 Velocity 代理方案；**不是当前实跑值**。

| 主服 | 第二套 / 代理 | 用途 |
|---|---|---|
| 55551 | 55552 | Java 入口 |
| 19132 | 19133 | Geyser 基岩（TCP + UDP） |
| 8100 | 8101 | BlueMap |
| 25576 | 25577 | OPanel MCDR Socket |
| 14502 | 14503 | authlib-injector |
| 8765 | 8766 | ClearLag++ 仪表盘 |
| 32217 | 32218 | 代理端 YOP，与后端必须错开 |
| — | 30001 / 30002… | 各子服，仅 `127.0.0.1` 供 Velocity 接入 |

> ⚠️ 55552 同时是 §1 的 RPM 自托管推导端口。若同机跑第二套服务端，需显式设 `selfHostPort` 或调大 `networkHttpOffset-v2`，避免与 55552 的 HTTP 监听冲突。
> Velocity 方案的安全组只留 `55551 TCP` + `19132 TCP/UDP`；子服、RCON、面板端口全部移除对外放行。本表为**规划映射**，当前实跑端口见 [多服网络与端口架构.md](<../多服Velocity +Multiverse/多服网络与端口架构.md>)，迁移原理见 [多服Velocity +Multiverse.md](<../多服Velocity +Multiverse/多服Velocity +Multiverse.md>)。

## 5. 防火墙放行清单（当前单服）

**必须放行**

- `TCP 55551`（Java）
- `TCP + UDP 19132`（基岩，两条缺一不可）
- `TCP 55552`（仅当 RPM 自托管探测通过、玩家从本机下载资源包时）

**按需放行（建议只开内网）**：`TCP 8100 / 3000 / 8804 / 8765 / 5000 / 26990`

**不要放行**：`TCP 25595`（RCON）、`TCP 3306`（MySQL）、`TCP 25576`、`TCP 3001`、`TCP 14502`、`TCP 32217`

## 6. 网络拓扑

### 6.1 同机部署（默认）

```
Java 客户端 ──(TCP :55551)──────────────────────────────► Leaf 26.3 (:55551)
基岩客户端 ──(TCP 信令 + UDP 媒体 :19132)──► Geyser ─────► Leaf 26.3
                                                             │
   ┌──────────────┬──────────────┬───────────┬──────────────┼──────────────┬────────────┐
   ▼              ▼              ▼           ▼              ▼              ▼            ▼
BlueMap       OPanel Web      Plan     ClearLag++     RPM 自托管      MySQL        RCON
:8100         :3000          :8804    :8765          :55552 HTTP      :3306        :25595
                 │                                                                (本机)
                 └─ MCDR Socket :25576 (127.0.0.1)

外置登录：Leaf ──► authlib-injector :14502 ──► Yggdrasil :32217

QQ 服务器
  │
  ▼
NapCat ──(WS :3001, 127.0.0.1)──► EasyBot 主程序 ──(HTTP :5000)──► EasyBot Web UI
                                        │
                                        │ (WS :26990)
                                        ▼
                                  EasyBot 插件 ──► Leaf 服务端
```

### 6.2 跨机部署（EasyBot 与 MC 服务器分离）

QQ 机器人在电脑 A，MC 服务器在电脑 B。

```
┌── 电脑 A（QQ 端） ─────────────────────────┐
│  NapCat :3001 (WS, 127.0.0.1)              │
│    │                                       │
│    ▼                                       │
│  EasyBot 主程序                             │
│    Bridge :26990  ← ⚠ 需防火墙放行          │
│    Web    :5000                            │
└──────────────┬─────────────────────────────┘
               │  WS :26990（局域网 / 公网 / 内网穿透）
┌──────────────▼─────────────────────────────┐
│  电脑 B（MC 服务器）                        │
│  EasyBot 插件 config.yml:                   │
│    ws://<电脑A的IP>:26990/bridge            │
│                                             │
│  Leaf :55551 TCP        ◄── Java 客户端     │
│  Geyser :19132 TCP+UDP  ◄── 基岩客户端      │
│  OPanel :3000 / Plan :8804 / BlueMap :8100  │
└─────────────────────────────────────────────┘
```

> 详见 [EasyBot 文档](../../2-运维监控与面板/QQ机器人联动-EasyBot/EasyBot.md#easybot-与-mc-服务器在不同电脑的情况) 中的跨电脑部署完整说明。
