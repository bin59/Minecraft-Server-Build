# 10. QQ 机器人联动 — EasyBot

**文件**: `plugins/EasyBot-2.3.1.jar` (3.75 MB)

## 功能说明

EasyBot 是一个将 Minecraft 服务器与 QQ 群聊互通的插件。它需要配合 EasyBot 主程序使用，通过 WebSocket 连接实现以下功能：

- QQ 群与游戏内聊天**双向同步**
- QQ 群内执行服务器命令
- 玩家账号与 QQ 号**绑定**（白名单/验证）
- 群内 @ 消息游戏内提醒

## 关键配置 (`plugins/EasyBot/config.yml`)

```yaml
service:
  url: 'ws://127.0.0.1:26990/bridge' # WebSocket 连接到本地 EasyBot 主程序
  token: 'G1qUrrQF0p01foojbF0sK8nKE7CYxdrn' # 鉴权 Token
  ignore_error: false # 连接失败时阻止玩家登录

command:
  allow_bind: true # 允许玩家使用绑定命令

event:
  enable_success_event: false # 绑定成功事件已禁用
  on_at:
    enable: true # 启用 @ 提醒
    title: '§a有人@你'
    sub_title: '§a请及时处理'
    find: true # 模糊匹配 @
    play_sound: true # @ 时播放声音
    sound: 0 # 声音类型: 铁砧落地

geyser:
  ignore_prefix: false # 不忽略 Floodgate 前缀

sync:
  chat_image_support: true # 支持 ChatImage 图片同步

skip_options:
  skip_join: false # 同步玩家加入消息
  skip_quit: false # 同步玩家退出消息
  skip_chat: false # 同步聊天消息
  skip_death: false # 同步死亡消息
```

## 外部依赖 — 完整连接架构

EasyBot 系统由三层组成，通过 WebSocket 串联：**NapCat（QQ 框架）→ EasyBot 主程序 → EasyBot 插件（Minecraft）**。

### 连接架构图

```
NapCat (QQ框架)
  │  启动后开启 OneBot WebSocket 服务端
  │  监听: ws://127.0.0.1:3001/
  │  AccessToken: EBPeG30Pm6ig992M
  │
  ▼  (EasyBot 主程序主动连接 NapCat)
  │
EasyBot 主程序 (C:\mc_serve\EasyBot\EasyBot.exe)
  │  连接 NapCat:      OnebotOptions    → ws://127.0.0.1:3001/
  │  对外监听 Bridge:  appsettings.json → 0.0.0.0:26990
  │  Web 管理面板:     appsettings.json → http://0.0.0.0:5000
  │
  ▼  (Minecraft 插件主动连接 EasyBot 主程序)
  │
EasyBot 插件 (plugins/EasyBot-2.3.1.jar)
  │  连接主程序: config.yml → ws://127.0.0.1:26990/bridge
  │  Token: G1qUrrQF0p01foojbF0sK8nKE7CYxdrn
  │
  ▼
Minecraft 服务端 (Leaf)
```

### 层级 1：NapCat → EasyBot 主程序（OneBot 协议）

**文件**: `C:\mc_serve\EasyBot\options\OnebotOptions`

```json
{
  "WsUrl": "ws://127.0.0.1:3001/",
  "AccessToken": "EBPeG30Pm6ig992M",
  "HeartBeatInterval": 30000,
  "UseHeartBeat": true
}
```

| 配置项              | 值                     | 说明                             |
| ------------------- | ---------------------- | -------------------------------- |
| `WsUrl`             | `ws://127.0.0.1:3001/` | NapCat 的 OneBot WebSocket 地址  |
| `AccessToken`       | `EBPeG30Pm6ig992M`     | 鉴权令牌（需与 NapCat 配置一致） |
| `HeartBeatInterval` | `30000`                | 心跳间隔（毫秒）                 |
| `UseHeartBeat`      | `true`                 | 启用心跳保活                     |

> **NapCat** 是一款基于 NTQQ 的 OneBot 协议机器人框架。EasyBot 主程序通过 OneBot 标准协议（正向 WebSocket）连接 NapCat，实现 QQ 消息收发。

### 层级 2：EasyBot 主程序（本地服务）

**文件**: `C:\mc_serve\EasyBot\appsettings.json`

```json
{
  "ServerOptions": {
    "Host": "0.0.0.0",
    "Port": 26990,
    "HeartbeatInterval": "0.00:02:00"
  },
  "Kestrel": {
    "Endpoints": {
      "web_api": {
        "Url": "http://0.0.0.0:5000",
        "Protocols": "Http1"
      }
    }
  }
}
```

| 配置项              | 值                    | 说明                                 |
| ------------------- | --------------------- | ------------------------------------ |
| `Host`              | `0.0.0.0`             | 监听所有网络接口                     |
| `Port`              | `26990`               | Bridge 端口（供 Minecraft 插件连接） |
| `HeartbeatInterval` | `2 分钟`              | 与插件的连接心跳间隔                 |
| `web_api.Url`       | `http://0.0.0.0:5000` | EasyBot Web 管理面板地址             |

**鉴权 Token 文件**: `C:\mc_serve\EasyBot\token.txt`

```
G1qUrrQF0p01foojbF0sK8nKE7CYxdrn
```

此 Token 与 Minecraft 插件端 `config.yml` 中的 `service.token` 必须一致。

### 配置文件速查

| 配置文件         | 路径                                        | 作用                           |
| ---------------- | ------------------------------------------- | ------------------------------ |
| OnebotOptions    | `C:\mc_serve\EasyBot\options\OnebotOptions` | EasyBot → NapCat 连接配置      |
| appsettings.json | `C:\mc_serve\EasyBot\appsettings.json`      | EasyBot 主程序端口和 Web 面板  |
| token.txt        | `C:\mc_serve\EasyBot\token.txt`             | EasyBot 鉴权 Token             |
| config.yml       | `plugins/EasyBot/config.yml`                | Minecraft 插件端连接和功能配置 |
| robot.json       | `C:\mc_serve\EasyBot\dp\robot.json`         | QQ 机器人信息（QQ 号、昵称）   |
| sync.json        | `C:\mc_serve\EasyBot\dp\sync.json`          | 消息同步规则配置               |
| command.json     | `C:\mc_serve\EasyBot\dp\command.json`       | QQ 端命令映射                  |
| bind_config.json | `C:\mc_serve\EasyBot\dp\bind_config.json`   | 玩家-QQ 绑定规则               |

## EasyBot 与 MC 服务器在不同电脑的情况

当你的 QQ 机器人和 MC 服务器不在同一台电脑上时（例如：QQ 挂在家里的电脑上，MC 服务器租用云服务器），只需调整连接地址即可。

### 部署示意图

```
┌── 电脑 A（运行 QQ + EasyBot 主程序） ──────────────────┐
│                                                         │
│  NapCat (QQ框架)                                        │
│    │  ws://127.0.0.1:3001/ (本机，不变)                  │
│    ▼                                                    │
│  EasyBot 主程序 (EasyBot.exe)                            │
│    │  监听 Bridge: 0.0.0.0:26990 (本机，不变)            │
│    │  Web 管理: http://0.0.0.0:5000                     │
│    │                                                    │
│    │  ⚠ 防火墙需放行 26990 端口                          │
│    │                                                    │
├────┼────────────────────────────────────────────────────┤
│    │                                                    │
│    │  网络连接 (局域网 / 公网)                             │
│    │                                                    │
├────┼────────────────────────────────────────────────────┤
│    │                                                    │
│    ▼                                                    │
│  ┌── 电脑 B（MC 服务器） ───────────────────────────┐   │
│  │                                                   │   │
│  │  EasyBot 插件 (plugins/EasyBot-2.3.1.jar)         │   │
│  │    config.yml → ws://<电脑A的IP>:26990/bridge     │   │
│  │    ⬆ 这里要改成电脑 A 的 IP 地址                    │   │
│  │                                                   │   │
│  │  MC 服务端 (Leaf/Paper)                            │   │
│  │                                                   │   │
│  └───────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 需要修改的配置

#### 电脑 B — MC 服务器端（仅需改 1 处）

**文件**: `plugins/EasyBot/config.yml`

```yaml
service:
  # 将 127.0.0.1 改为电脑 A 的 IP 地址
  url: 'ws://<电脑A的IP>:26990/bridge'
  token: 'G1qUrrQF0p01foojbF0sK8nKE7CYxdrn' # 不变
  ignore_error: false
```

| 场景 | `service.url` 示例 | 说明 |
|------|-------------------|------|
| 同局域网 | `ws://192.168.1.100:26990/bridge` | 电脑 A 的局域网 IP |
| 公网（有公网 IP） | `ws://1.2.3.4:26990/bridge` | 电脑 A 的公网 IP |
| 公网（有 DDNS 域名） | `ws://myhome.ddns.net:26990/bridge` | 电脑 A 的 DDNS 域名 |
| 内网穿透（frp/nps 等） | `ws://bot.yourdomain.com:26990/bridge` | 穿透后的域名或 IP |

#### 电脑 A — EasyBot 端（基本无需改）

- **`appsettings.json`**: `Host` 已经是 `0.0.0.0`，表示监听所有网络接口，**无需修改**。
- **`OnebotOptions`**: `WsUrl` 指向 `127.0.0.1:3001`，因为 NapCat 和 EasyBot 仍在同一台电脑上，**无需修改**。
- **其他配置文件**（`token.txt`, `dp/` 下的文件）：**无需修改**。

### 网络要求

| 条件 | 做法 |
|------|------|
| **防火墙放行** | 电脑 A 的防火墙必须放行 **TCP 26990** 端口入站连接 |
| **同一局域网** | 直接用局域网 IP 即可，无需额外配置 |
| **不同网络（公网）** | 电脑 A 需要公网 IP，并在路由器上做 **端口映射**（26990 → 电脑 A 局域网 IP） |
| **没有公网 IP** | 使用内网穿透工具（frp / nps / SakuraFrp），将电脑 A 的 26990 端口暴露出去 |
| **电脑 A IP 会变动** | 建议配置 DDNS（动态域名解析），MC 服端填写域名而非 IP |

### 启动顺序

1. **电脑 A**: 先启动 NapCat（QQ 登录）→ 再启动 EasyBot 主程序（确保 Bridge 已就绪）
2. **电脑 B**: 最后启动 MC 服务器（EasyBot 插件会自动连接电脑 A 的 Bridge）

> **注意**: 如果 MC 服务器先启动而 EasyBot 主程序还没就绪，插件会连接失败。可在 EasyBot 主程序启动后，在游戏内执行 `/ebot reload` 重连，或重启 MC 服务器。

### 故障排查

| 问题 | 可能原因 | 解决方法 |
|------|----------|----------|
| 插件连不上 Bridge | IP 地址不对或防火墙拦截 | ① 在电脑 B 上用 `ping <电脑A的IP>` 测试连通性；② 检查电脑 A 防火墙是否放行 26990；③ 如果是公网，检查路由器端口映射 |
| 连接后频繁断开 | 网络不稳定 | 检查 `appsettings.json` 的 `HeartbeatInterval` 是否合理（建议 2 分钟） |
| 内网穿透连不上 | 穿透隧道未建立 | 检查穿透客户端是否在线，端口映射是否正确 |
| 连接成功但消息不同步 | Token 不一致 | 确认电脑 A 的 `token.txt` 和电脑 B 的 `config.yml` 中 `token` 一致 |

### 安全建议

- Bridge 端口（26990）**不要直接暴露在公网上**，建议使用 VPN 或内网穿透工具限制访问来源。
- 如果两台电脑在同一局域网，这是最理想和安全的方式。
- Token 务必保持复杂且两台电脑一致，防止未授权连接。

---

## 常用命令

| 命令             | 说明              |
| ---------------- | ----------------- |
| `/bind <验证码>` | 绑定 QQ 账号      |
| `/ebot reload`   | 重载 EasyBot 配置 |

## 关闭EasyBot QQ验证码绑定

- `EasyBot\dp\bind_config.json`: `bind_required_servers":["G1qUrrQF0p01foojbF0sK8nKE7CYxdrn"]` 清空为 `[]`（不再强制此服务器绑定）
- `plugins\EasyBot\config.yml`: `allow_bind` → `false`（插件端禁用绑定）
- 重启 Minecraft 服务器 + EasyBot 主程序后生效
