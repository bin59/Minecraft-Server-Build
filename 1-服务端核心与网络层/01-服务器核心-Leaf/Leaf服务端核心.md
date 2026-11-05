# 1. 服务器核心 — Leaf

本文档说明本服选用的服务端核心 Leaf（leaf-1.21.11-174.jar）的定位与部署配置。Leaf 是 Paper/Gale 链路上的高性能分支，完全兼容 Bukkit/Spigot 插件生态，可作为 Paper 的即插即用替代。文档列出了相比 Paper 的性能优化点、从 Paper 迁移的注意事项，以及 server.properties、spigot.yml、leaf-global.yml 的关键取值和启动脚本，供服主与管理员在搭建或替换核心时参考。

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

> **版本**: `leaf-1.21.11-174.jar`（构建号 174），与 Paper 同 Minecraft 版本，可直接替换。

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
| `network-compression-threshold` | `256` | 网络压缩阈值（原版默认；<256 字节的小包不压缩，省 CPU、略增带宽） |
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

#### 配置文件有效性

| 文件 | 状态 | 说明 |
|---|---|---|
| `purpur.yml` | ❌ 不再读取 | 遗留文件，Leaf 不读取此配置 |
| `spigot.yml` | ✅ 仍有效 | Leaf 继承 Spigot → Paper 的配置链 |
| `bukkit.yml` | ✅ 仍有效 | Leaf 继承 Bukkit 的配置链 |

### 启动脚本 (`start.bat`)

```batch
@Echo off
java -Xmx12G -Xms12G -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:MaxTenuringThreshold=1 -Xlog:gc*:logs/gc.log:time,uptime,level,tags:filecount=5,filesize=50M -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar -jar leaf-1.21.11-174.jar nogui
pause
```

> 该脚本基于 [Aikar's flags](https://mcflags.emc.gs) 的 G1GC 调优模板，并按本服硬件（云服务器 **8 核 CPU / 32G 内存**）将堆上调至 12G。

**关键启动参数**:
- `-Xmx12G -Xms12G`: 堆上限 = 初始 = 12GB。两者相等可避免运行时动态扩堆带来的碎片与停顿。
- `-XX:+UseG1GC`: 使用 G1 垃圾回收器（Leaf 1.21.11 需 **JDK 21**）。
- `-XX:+UseStringDeduplication`: G1 字符串去重。MC 存在海量重复字符串（方块/物品命名空间 ID、NBT 标签名、玩家名、配置键等），去重让内容相同的 `String` 共享同一份底层字符数组，通常可回收 **10%~30%** 的字符串内存；12G 堆下可省数百 MB ~ 1G+。生效是渐进的，跑过几次 GC 后才逐步体现。
- `-Xlog:gc*:logs/gc.log:time,uptime,level,tags:filecount=5,filesize=50M`: 滚动 GC 日志（5 个文件 × 50M，含时间/耗时/级别/标签）。重启后生成 `logs/gc.log`，用于排查 TPS 抖动、频繁 Full GC。
- `-XX:+DisableExplicitGC`: 禁止 `System.gc()`，防止插件误触发 Full GC 卡服。
- `-XX:+AlwaysPreTouch`: 启动即真正占满 12G 物理内存，避免运行时缺页卡顿。
- `-javaagent:YggdrasilOfficialProxy...`: 加载 Yggdrasil 外置登录代理（要求该 jar 与 `start.bat` 同目录）。
- `nogui`: 无图形界面模式。

**内存规划（本服 32G / 8 核）**:
- 12G 堆 + 堆外开销（Netty/Geyser 的 direct buffer、元空间、线程栈、JIT 缓存，约 1~3G）≈ 15G 进程占用，物理内存剩 **~17G** 余量，**不会触发 swap**。
- 8 核 CPU 对 G1 并行回收友好，`MaxGCPauseMillis=200` 目标在 12G 堆下可压住，单次 GC 停顿可控。
- 堆越大单次 GC 停顿略长（回收对象更多），但 12G 以内 G1 足够；超过 12G 才需考虑 ZGC（JDK 21+）。
- ⚠️ 加内存**解决不了跨公网 MySQL 连接池耗尽 / Watchdog 杀服这类 IO 问题**，那类问题要先查网络与连接池，而非继续加堆。
- ⚠️ 启动前确认服务器 `java -version` 为 **JDK 21**，否则 `-Xlog` 与字符串去重所在特性无法支持，启动会直接报错。
