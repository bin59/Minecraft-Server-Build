# 1. 服务器核心 — Leaf

## 1.1 Leaf 1.21.11

**文件**: `leaf-1.21.11-174.jar`

**项目地址**: https://github.com/Winds-Studio/Leaf | **下载**: [GitHub Releases](https://github.com/Winds-Studio/Leaf/releases/tag/ver-1.21.11)

### 简介

Leaf 是 [Gale](https://github.com/GaleMC/Gale) 的一个高性能分支，而 Gale 本身又是 PaperMC 的分支。Leaf 在 Gale 的基础上进一步优化，目标是**在性能、原版体验和稳定性之间找到最佳平衡**。它完全兼容 Paper/Spigot/Bukkit 插件生态，可以作为 Paper 的**即插即用替代品**。

### 相比 Paper 的性能提升

| 优化领域 | 具体改进 |
|---|---|
| **实体处理** | 多线程实体 AI、路径查找异步化、实体激活范围优化 |
| **区块系统** | 异步区块加载/保存、区块 tick 优化、光照引擎优化 |
| **红石** | 红石更新优化（Eigencraft 补丁）、减少不必要的红石更新 |
| **内存管理** | 更高效的内存分配策略、减少 GC 压力 |
| **网络** | 数据包压缩优化、Netty 线程池调优 |
| **原版修复** | 继承 Paper 所有 bug 修复，同时保持原版特性 |

> **版本说明**: `leaf-1.21.11-174.jar` 对应 Minecraft 1.21.1，Leaf 构建号 174。此版本与 Paper 1.21.11-132 同属一个 Minecraft 版本，可直接替换。

### 从 Paper 迁移注意事项

- Leaf 是 Paper 的直接分支，**所有 Paper 插件完全兼容**，无需重新配置
- 世界数据、玩家数据、插件数据全部兼容，直接替换 JAR 即可
- `server.properties`、`bukkit.yml`、`spigot.yml`、`paper-global.yml`、`paper-world-defaults.yml` 等配置文件均保持兼容
- 首次启动会自动生成 Leaf 特有配置文件（如 `leaf-global.yml`）
- YggdrasilOfficialProxy（paperclip 变体）**完全兼容** Leaf，无需更换

### 服务器属性 (`server.properties` 关键配置)

| 配置项 | 值 | 说明 |
|---|---|---|
| `server-port` | `55551` | Java 版服务端口 |
| `online-mode` | `true` | 正版验证开启（Java 玩家需正版） |
| `max-players` | `50` | 最大同时在线玩家数 |
| `gamemode` | `survival` | 默认游戏模式：生存 |
| `difficulty` | `easy` | 游戏难度：简单 |
| `motd` | `§e南瓜§f生存服` | 服务器列表显示的标题 |
| `view-distance` | `12` | 视距 (区块) |
| `simulation-distance` | `4` | 实体模拟距离 (区块) |
| `allow-flight` | `false` | 禁止飞行（防作弊） |
| `enforce-whitelist` | `true` | 强制白名单验证 |
| `white-list` | `false` | 白名单功能未启用 |
| `spawn-protection` | `16` | 出生点保护半径 |
| `enforce-secure-profile` | `false` | 不强制安全档案（兼容离线/基岩版） |
| `rate-limit` | `0` | 无速率限制（基岩版兼容需要） |
| `network-compression-threshold` | `64` | 网络压缩阈值 |
| `region-file-compression` | `deflate` | 区域文件压缩算法 |

### Bukkit/Spigot 配置 (`spigot.yml` 关键项)

| 配置项 | 值 | 说明 |
|---|---|---|
| `bungeecord` | `false` | 未启用代理模式 |
| `restart-on-crash` | `true` | 崩溃后自动重启 |
| `netty-threads` | `4` | 网络线程数 |
| `user-cache-size` | `1000` | 用户缓存大小 |
| `connection-throttle` | `4000` | 连接节流(ms)，基岩互通需要较高值 |

### 运行时配置目录

Leaf 继承了 Paper → Gale 的配置体系，首次启动后自动生成 `config/` 目录：

```
config/
├── leaf-global.yml          # Leaf 特有全局配置（异步生物生成/寻路等）
├── gale-global.yml           # Gale 全局配置（Leaf 上游）
├── gale-world-defaults.yml   # Gale 世界默认配置
├── paper-global.yml          # Paper 全局配置（继承）
└── paper-world-defaults.yml  # Paper 世界默认配置（继承）
```

> **配置层级**: `leaf-global.yml` > `gale-global.yml` > `paper-global.yml`，Leaf 特有配置会覆盖上游默认值。修改配置后需重启或使用 `/leaf reload` 热重载。

#### `leaf-global.yml` 关键配置

```yaml
async:
  async-chunk-send:
    enabled: false          # 异步区块发送（大量玩家同时加载时提升性能）
  async-mob-spawning:
    enabled: true           # 异步生物生成（实体多的服最高 +15% 性能）
  async-pathfinding:
    enabled: false          # 异步寻路（需配合 max-threads 调优）
    max-threads: 0          # 0 = 自动根据 CPU 核心数
```

> ⚠️ `async-mob-spawning` 需要 Paper 配置中 `per-player-mob-spawns: true` 才能生效。

#### 旧版遗留配置

| 文件 | 状态 | 说明 |
|---|---|---|
| `purpur.yml` | ❌ 不再读取 | 从 Purpur 迁移后的遗留文件，Leaf 不读取此配置 |
| `spigot.yml` | ✅ 仍有效 | Leaf 继承 Spigot → Paper 的配置链 |
| `bukkit.yml` | ✅ 仍有效 | Leaf 继承 Bukkit 的配置链 |

### 启动脚本 (`start.bat`)

```batch
@Echo off
java -Xmx4G -Xms1G -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar -jar leaf-1.21.11-174.jar nogui
pause
```

**启动参数说明**:
- `-Xmx4G`: 最大堆内存 4GB
- `-Xms1G`: 初始堆内存 1GB
- `-javaagent:YggdrasilOfficialProxy...`: 加载 Yggdrasil 外置登录代理
- `nogui`: 无图形界面模式
