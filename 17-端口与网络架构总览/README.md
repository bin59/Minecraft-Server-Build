# 17. 端口与网络架构总览

## 端口表

| 端口 | 协议 | 用途 | 绑定地址 | 公开 |
|---|---|---|---|---|
| **55551** | TCP | Java 版游戏主端口 | 0.0.0.0 | 是 |
| **19132** | UDP | 基岩版游戏端口 (Geyser) | 0.0.0.0 | 是 |
| **25555** | TCP (HTTP) | OPanel Web 管理面板 | 0.0.0.0 | 建议仅内网 |
| **25576** | TCP | OPanel MCDR Socket | 127.0.0.1 | 否 |
| **3001** | TCP (WS) | NapCat OneBot WebSocket 服务 | 127.0.0.1 | 否 |
| **5000** | TCP (HTTP) | EasyBot Web 管理面板 | 0.0.0.0 | 建议仅内网 |
| **26990** | TCP (WS) | EasyBot Bridge（插件 ←→ 主程序） | 0.0.0.0 | 同机：否 / 跨机：需放行 |
| **14502** | TCP (HTTP) | authlib-injector 本地代理 | 127.0.0.1 | 否 |
| **32217** | TCP (HTTP) | Yggdrasil 认证服务器 | localhost | 否 |
| **25575** | TCP | RCON（未启用） | — | 否 |

## 网络拓扑

### 同机部署（默认）

所有组件在同一台电脑上，EasyBot 插件通过 `127.0.0.1` 直连。

```
QQ 服务器
  │
  ▼
NapCat (QQ 框架) ──(WS :3001)──→ EasyBot 主程序 ──(HTTP :5000)──→ EasyBot Web UI
                                     │
                                     │ (WS :26990)
                                     ▼
                               EasyBot 插件
                                     │
                                     ▼
Yggdrasil 认证 (:32217) ←── authlib-injector (:14502) ←──→ Leaf 服务端 (:55551 TCP / :19132 UDP)
                                                               │
                                                               ├─ Java 版玩家
                                                               ├─ 基岩版玩家 (via Geyser)
                                                               └─ OPanel Web UI (:25555)
```

### 跨机部署（EasyBot 与 MC 服务器分离）

当 QQ 机器人在电脑 A，MC 服务器在电脑 B（如云服务器）。

```
┌── 电脑 A（QQ 端） ──────────────────┐
│                                      │
│  NapCat (QQ框架)                     │
│    │  WS :3001 (本机, 不变)          │
│    ▼                                 │
│  EasyBot 主程序                       │
│    Bridge :26990  ← ⚠需防火墙放行    │
│                                      │
└──────────┬───────────────────────────┘
           │
           │  WS :26990 (局域网/公网/内网穿透)
           │
┌──────────▼───────────────────────────┐
│  电脑 B（MC 服务器）                  │
│                                      │
│  EasyBot 插件                         │
│    config.yml: ws://<电脑A的IP>:26990 │
│                                      │
│  Leaf 服务端 :55551 / :19132         │
│    ├─ Java 版玩家                    │
│    ├─ 基岩版玩家 (via Geyser)         │
│    └─ OPanel Web UI :25555           │
└──────────────────────────────────────┘
```

> 详见 [EasyBot 文档](../10-QQ机器人联动/EasyBot.md#easybot-与-mc-服务器在不同电脑的情况) 中的跨电脑部署完整说明。
