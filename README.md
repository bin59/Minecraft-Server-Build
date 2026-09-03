# Minecraft-Server-Build

> **南瓜生存服** — Minecraft Java / 基岩双端互通服务器 完整技术文档与运维手册

本文档是一份**从零复现这台服务器**的完整指南：涵盖服务端选型、组件清单、网络架构、端口规划、配置速查、部署流程、日常运维与故障排查。

> ⚠️ **隐私说明**：本文档中所有敏感信息（公网 IP、Token、密码、密钥、AccessKey）均已替换为占位符（如 `<公网IP>`、`<随机32位字符串>`）。
> 部署时请**自行生成**并妥善保管，切勿将真实凭据提交到公开仓库。

---

## 目录

- [一、项目简介](#一项目简介)
- [二、服务器速览](#二服务器速览)
- [三、文档导航](#三文档导航)
- [四、组件清单](#四组件清单)
- [五、系统架构](#五系统架构)
- [六、端口总表](#六端口总表)
- [七、从零部署](#七从零部署)
- [八、核心配置速查](#八核心配置速查)
- [九、插件依赖与加载顺序](#九插件依赖与加载顺序)
- [十、日常运维](#十日常运维)
- [十一、故障排查速查](#十一故障排查速查)
- [十二、已知问题](#十二已知问题)
- [十三、安全基线](#十三安全基线)
- [十四、资源链接](#十四资源链接)

---

## 一、项目简介

**南瓜生存服**是一台面向国内玩家的 Minecraft 生存服务器，核心目标是：

| 目标             | 实现方式                                                     |
| ---------------- | ------------------------------------------------------------ |
| **双端互通**     | Java 版与基岩版（手机 / Win10 / Xbox / Switch / PS）同服游玩 |
| **低门槛入服**   | 外置登录（Yggdrasil）+ Floodgate，基岩版玩家免正版验证       |
| **跨版本兼容**   | ViaVersion 允许 1.8 ~ 最新的客户端直连                       |
| **群服互通**     | QQ 群 ↔ 游戏内聊天双向同步（EasyBot + NapCat）               |
| **可视化运维**   | OPanel Web 面板 + spark 性能分析 + MCSM 进程管理             |
| **长期存档安全** | Residence 领地保护 + CoreProtect 方块审计与回滚              |

### 技术选型一句话总结

> **Leaf**（高性能 Paper 分支）作核心，**YggdrasilOfficialProxy + authlib-injector** 做外置登录，
> **Geyser + Floodgate** 打通基岩版，**ViaVersion** 兜底跨版本，
> 上挂 **Residence / CoreProtect / LuckPerms / Vault / SkinsRestorer** 等生存服标配插件，
> 外接 **EasyBot** 做 QQ 联动、**OPanel** 做 Web 管理。

---

## 二、服务器速览

| 项目            | 值                                    |
| --------------- | ------------------------------------- |
| 服务器名称      | 南瓜生存服                            |
| MOTD            | `§e南瓜§f生存服`                      |
| 服务端核心      | Leaf `1.21.11-174`（Paper 分支）      |
| Minecraft 版本  | Java 1.21.11                          |
| 运行模式        | 单端（无 BungeeCord / Velocity 代理） |
| 正版验证        | `online-mode=true`（经外置登录代理）  |
| Java 版端口     | **55551**（TCP）                      |
| 基岩版端口      | **19132**（UDP）                      |
| Web 面板端口    | **25555**（TCP，建议仅内网）          |
| 最大玩家数      | 50                                    |
| 默认游戏模式    | 生存（survival）                      |
| 难度            | 简单（easy）                          |
| 视距 / 模拟距离 | 12 / 4 区块                           |
| 启动内存        | 初始 1GB / 最大 4GB                   |
| 服务端目录      | `C:\mc_serve\1.21.11-test`            |
| 文档更新日期    | 2026-09-01                            |

---

## 三、文档导航

### 3.1 服务端与核心组件

| #    | 章节              | 路径                                                                                                        | 说明                                      |
| ---- | ----------------- | ----------------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| 01   | 服务器核心        | [01-服务器核心-Leaf/](01-服务器核心-Leaf/Leaf服务端核心.md)                                                 | Leaf 核心、性能特性、Paper 迁移注意事项   |
| 02   | 外置登录代理      | [02-外置登录代理-YggdrasilOfficialProxy/](02-外置登录代理-YggdrasilOfficialProxy/YggdrasilOfficialProxy.md) | authlib-injector 字节码注入原理与认证链路 |
| 03   | Java-Bedrock 互通 | [03-Java-Bedrock互通层-Geyser-Floodgate/](03-Java-Bedrock互通层-Geyser-Floodgate/README.md)                 | Geyser 协议转换 + Floodgate 基岩认证      |
| 03.1 | └ Geyser-Spigot   | [01-Geyser-Spigot.md](03-Java-Bedrock互通层-Geyser-Floodgate/01-Geyser-Spigot.md)                           | 端口、连接地址、调试命令                  |
| 03.2 | └ Floodgate       | [02-Floodgate.md](03-Java-Bedrock互通层-Geyser-Floodgate/02-Floodgate.md)                                   | 玩家识别前缀、key.pem 密钥管理            |
| 04   | 跨版本兼容        | [04-跨版本协议兼容-ViaVersion/](04-跨版本协议兼容-ViaVersion/ViaVersion.md)                                 | 1.8 ~ 最新客户端协议翻译                  |

### 3.2 玩法与管理插件

| #    | 章节             | 路径                                                                                                                               | 说明                            |
| ---- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| 05   | 权限管理         | [05-权限管理系统-LuckPerms/](05-权限管理系统-LuckPerms/LuckPerms.md)                                                               | 组/玩家权限、继承、上下文       |
| 06   | 经济系统         | [06-经济系统-Vault/](06-经济系统-Vault/Vault.md)                                                                                   | Vault 经济 API 与占位符         |
| 07   | 方块审计         | [07-方块记录与回滚-CoreProtect/](07-方块记录与回滚-CoreProtect/CoreProtect.md)                                                     | 记录/查询/回滚，SQLite 转 MySQL |
| 07.1 | └ 数据库过大处理 | [1.行为数据库过大](07-方块记录与回滚-CoreProtect/CoreProtect插件：玩家的行为数据库过大/1.CoreProtect插件：玩家的行为数据库过大.md) | 清理与自动清理方案              |
| 07.2 | └ SQLite → MySQL | [2.切换到MySQL](07-方块记录与回滚-CoreProtect/CoreProtect插件：玩家的行为数据库过大/2.将CoreProtect从SQLite切换到MySQL.md)         | 生产环境迁移步骤                |
| 08   | 领地系统         | [08-领地系统-Residence/](08-领地系统-Residence/Residence.md)                                                                       | 圈地、Flags 权限、配置排查      |
| 08.1 | └ 玩家使用指南   | [领地插件使用指南（玩家篇）.md](08-领地系统-Residence/领地插件使用指南（玩家篇）.md)                                               | 面向玩家的图文教程              |
| 09   | 皮肤管理         | [09-皮肤管理-SkinsRestorer/](09-皮肤管理-SkinsRestorer/SkinsRestorer.md)                                                           | 外置登录下的皮肤加载            |
| 10   | QQ 机器人联动    | [10-QQ机器人联动-EasyBot/](10-QQ机器人联动-EasyBot/EasyBot.md)                                                                     | 三层架构、跨机部署、绑定规则    |
| 11   | Web 管理面板     | [11-Web管理面板-OPanel/](11-Web管理面板-OPanel/OPanel.md)                                                                          | accessKey 鉴权与安全建议        |
| 12   | 离线背包         | [12-离线背包查看-OpenInv/](12-离线背包查看-OpenInv/OpenInv.md)                                                                     | 查看/编辑离线玩家背包           |
| 20   | 创世神           | [20.创世神WorldEdit/](20.创世神WorldEdit/创世神WorldEdit.md)                                                                       | WorldEdit 安装与常用指令        |

### 3.3 依赖库与辅助组件

| #   | 章节       | 路径                                                    | 说明                      |
| --- | ---------- | ------------------------------------------------------- | ------------------------- |
| 13  | 性能分析   | [13-性能分析-spark/](13-性能分析-spark/spark.md)        | TPS / 采样分析 / 健康报告 |
| 14  | 核心依赖库 | [14-核心依赖库-CMILib/](14-核心依赖库-CMILib/CMILib.md) | 多插件共享的底层库        |
| 15  | 统计系统   | [15-统计系统-bStats/](15-统计系统-bStats/bStats.md)     | 匿名使用统计（可关闭）    |

### 3.4 运维与部署

| #    | 章节             | 路径                                                                               | 说明                                   |
| ---- | ---------------- | ---------------------------------------------------------------------------------- | -------------------------------------- |
| 16   | 配置文件补充     | [16-服务器配置文件补充/](16-服务器配置文件补充/README.md)                          | bukkit.yml / spigot.yml / 数据文件     |
| 17   | 端口与网络架构   | [17-端口与网络架构总览/](17-端口与网络架构总览/README.md)                          | 端口表 + 同机/跨机拓扑                 |
| 18   | 命令速查         | [18-常用管理命令速查/](18-常用管理命令速查/README.md)                              | 各插件高频管理命令                     |
| 19   | MCSM 控制面板    | [19-MCSM控制面板/](19-MCSM控制面板/MCSM控制面板.md)                                | Windows 部署、分布式节点、导入已有目录 |
| 19.1 | └ 链接已有文件夹 | [链接到已有文件夹.md](19-MCSM控制面板/链接到已有文件夹.md)                         | 不迁移文件直接接管旧服务端             |
| 21   | 玩家信息展示     | [21-玩家信息收集与展示/](21-玩家信息收集与展示/0.玩家信息收集与展示.md)            | TAB / Plan / 全息榜等选型对比          |
| 21.1 | └ TAB 配置       | [2.TAB信息展示.md](21-玩家信息收集与展示/2.TAB信息展示.md)                         | TAB 列表与动画配置                     |
| 21.2 | └ TAB 完整示例   | [2.1.TAB配置完整示例.md](21-玩家信息收集与展示/2.1.TAB配置完整示例.md)             | 可直接粘贴的配置模板                   |
| 21.3 | └ Plan 联动      | [Plan 和 TAB 联动配置示例.md](<21-玩家信息收集与展示/Plan 和 TAB 联动配置示例.md>) | 数据面板与 TAB 联动                    |

### 3.5 云服务器与外围服务

| #   | 章节           | 路径                                                            | 说明                             |
| --- | -------------- | --------------------------------------------------------------- | -------------------------------- |
| C0  | 总览           | [云服务器配置/00\_总览.md](云服务器配置/00_总览.md)             | 云主机信息、域名、端口、凭据清单 |
| C1  | Caddy          | [01_Caddy.md](云服务器配置/01_Caddy.md)                         | 全站 HTTPS 反代与路由表          |
| C2  | MCSM 面板      | [02_MCSM面板.md](云服务器配置/02_MCSM面板.md)                   | 面板实例清单与 API 调用          |
| C3  | NapCat         | [03_NapCat.md](云服务器配置/03_NapCat.md)                       | QQ 机器人框架双实例              |
| C4  | EasyBot        | [04_EasyBot.md](云服务器配置/04_EasyBot.md)                     | 群服互通主程序部署               |
| C5  | 网易租赁服     | [05\_网易租赁服.md](云服务器配置/05_网易租赁服.md)              | NeOmega 基岩租赁服接入           |
| C6  | 网站与数据库   | [06\_网站与数据库.md](云服务器配置/06_网站与数据库.md)          | 主站、MySQL、邮件、备份          |
| C7  | QQ 官方机器人  | [07_QQ官方机器人.md](云服务器配置/07_QQ官方机器人.md)           | 官方 Bot 消息推送                |
| C8  | 监控系统       | [08\_监控系统.md](云服务器配置/08_监控系统.md)                  | 双通道告警                       |
| C9  | 系统与权限     | [09\_系统与权限.md](云服务器配置/09_系统与权限.md)              | 用户体系、SSH、目录权限、Swap    |
| C10 | SQLite → MySQL | [10_SQLite转MySQL工具.md](云服务器配置/10_SQLite转MySQL工具.md) | 数据迁移工具                     |
| C11 | Caddy 压缩优化 | [11_Caddy压缩优化.md](云服务器配置/11_Caddy压缩优化.md)         | Gzip/Zstd 提速                   |

### 3.6 附录与专题

| 章节           | 路径                                                                                                                     | 说明               |
| -------------- | ------------------------------------------------------------------------------------------------------------------------ | ------------------ |
| 常见问题排查   | [常见问题/常见问题排查.md](常见问题/常见问题排查.md)                                                                     | 高频故障排查       |
| 服务器内存     | [常见问题/服务器内存相关/](常见问题/服务器内存相关/服务器内存相关.md)                                                    | 内存占用分析与调优 |
| region 瘦身    | [1.region文件夹过大](常见问题/服务器内存相关/region文件夹：生成和保存的区块过大/1.region文件夹：生成和保存的区块过大.md) | 存档体积治理       |
| MCA Selector   | [MCASelector.md](常见问题/服务器内存相关/region文件夹：生成和保存的区块过大/MCASelector/MCASelector.md)                  | 区块裁剪与建筑迁移 |
| 反投影作弊     | [禁用影响平衡的插件功能/](禁用影响平衡的插件功能/1.禁用影响平衡的插件功能.md)                                            | 方案对比与选型     |
| AntiLitematica | [1.说明.md](禁用影响平衡的插件功能/AntiLitematica-保姆级Litematica、打印机检测与阻断器/1.说明.md)                        | 检测原理           |
| └ 部署配置     | [2.安装部署与配置.md](禁用影响平衡的插件功能/AntiLitematica-保姆级Litematica、打印机检测与阻断器/2.安装部署与配置.md)    | 完整部署教程       |

---

## 四、组件清单

> 全部组件的版本、体积、下载源与依赖关系总表。**不含任何真实凭据**，部署时请自行生成密钥与 Token。

**更新日期**：2026-09-01 | **服务端目录**：`C:\mc_serve\1.21.11-test` | **MC 版本**：1.21.11

### 4.1 总览速查

| 类别                  | 数量 | 说明                                      |
| --------------------- | ---- | ----------------------------------------- |
| 服务端核心            | 1    | Leaf 1.21.11-174                          |
| Java Agent / 外置登录 | 2    | YggdrasilOfficialProxy + authlib-injector |
| 已安装插件            | 12   | 见 [4.4](#44-已安装插件)                  |
| 外部程序              | 4    | NapCat / EasyBot 主程序 / MCSM / Caddy    |
| 规划 / 备选           | 5    | 见 [4.6](#46-规划--备选插件)              |

### 4.2 服务端核心

| 组件     | 版本          | 文件                   | 体积   | 说明                                 | 状态    |
| -------- | ------------- | ---------------------- | ------ | ------------------------------------ | ------- |
| **Leaf** | `1.21.11-174` | `leaf-1.21.11-174.jar` | ~50 MB | 服务端核心（Paper 分支，高性能优化） | ✅ 正常 |

**Leaf 相比 Paper 的性能提升**：

| 优化领域 | 具体改进                                        |
| -------- | ----------------------------------------------- |
| 实体处理 | 多线程实体 AI、路径查找异步化、实体激活范围优化 |
| 区块系统 | 异步区块加载/保存、区块 tick 优化、光照引擎优化 |
| 红石     | Eigencraft 补丁、减少不必要的红石更新           |
| 内存管理 | 更高效的内存分配策略、降低 GC 压力              |
| 网络     | 数据包压缩优化、Netty 线程池调优                |

> Leaf 是 Paper 的即插即用替代品，**所有 Paper/Spigot/Bukkit 插件完全兼容**，可直接替换 JAR 迁移。
> 详见 [01-服务器核心-Leaf](01-服务器核心-Leaf/Leaf服务端核心.md)。

**运行时配置目录**（首次启动自动生成）：

```
config/
├── leaf-global.yml          # Leaf 特有全局配置（异步生物生成/寻路）
├── gale-global.yml          # Gale 全局配置（Leaf 上游）
├── gale-world-defaults.yml  # Gale 世界默认配置
├── paper-global.yml         # Paper 全局配置（继承）
└── paper-world-defaults.yml # Paper 世界默认配置（继承）
```

### 4.3 外置登录组件

| #   | 组件                       | 版本  | 文件                                         | 体积     | 角色                                                | 状态 |
| --- | -------------------------- | ----- | -------------------------------------------- | -------- | --------------------------------------------------- | ---- |
| 1   | **YggdrasilOfficialProxy** | 2.3.0 | `YggdrasilOfficialProxy-2.3.0-paperclip.jar` | 17.09 MB | 外层 Java Agent 封装（`-javaagent` 加载）           | ✅   |
| 2   | **authlib-injector**       | 1.2.7 | `authlib-injector-1.2.7.jar`                 | 336 KB   | 核心字节码注入引擎（由 YOP 内部调用，无需单独配置） | ✅   |

**被注入修改的类**：

| 目标类                                                       | 注入目的                  |
| ------------------------------------------------------------ | ------------------------- |
| `com.mojang.authlib.properties.Property`                     | 替换 Mojang 公钥          |
| `com.mojang.authlib.HttpAuthenticationService`               | 修正认证 URL 拼接         |
| `com.mojang.authlib.yggdrasil.YggdrasilEnvironment`          | 替换认证服务器地址        |
| `net.minecraft.server.network.ServerLoginPacketListenerImpl` | 兼容中文等特殊字符用户名  |
| `net.skinsrestorer.shared.connections.MojangAPIImpl`         | 皮肤 API 走外置认证       |
| `org.geysermc.floodgate.util.MojangUtils`                    | Floodgate 基岩认证兼容    |
| `org.geysermc.geyser.skin.SkinProvider`                      | Geyser 皮肤加载走外置认证 |

> 本地端口：authlib-injector httpd `:14502` → 转发至 Yggdrasil 认证服务器 `localhost:32217`。

### 4.4 已安装插件

| #   | 组件               | 版本  | 文件                                           | 体积      | 分类                  | 状态            |
| --- | ------------------ | ----- | ---------------------------------------------- | --------- | --------------------- | --------------- |
| 1   | **CMILib**         | 最新  | `plugins/CMILib/`                              | —         | 核心依赖库            | ✅              |
| 2   | **Vault**          | 1.6.2 | `plugins/Vault/`                               | —         | 经济 API              | ✅              |
| 3   | **LuckPerms**      | 最新  | `plugins/LuckPerms/`                           | —         | 权限管理              | ⚠️ **缺主 JAR** |
| 4   | **ViaVersion**     | 5.9.1 | `plugins/ViaVersion.jar`                       | 6.08 MB   | 跨版本协议兼容        | ✅              |
| 5   | **Geyser-Spigot**  | 最新  | `plugins/Geyser-Spigot.jar`                    | 18.26 MB  | Java-Bedrock 协议转换 | ✅              |
| 6   | **Floodgate**      | 最新  | `plugins/floodgate-spigot.jar`                 | 11.03 MB  | 基岩版认证            | ✅              |
| 7   | **CoreProtect CE** | 23.2  | `plugins/CoreProtect.jar`                      | 2.07 MB   | 方块记录/回滚         | ✅              |
| 8   | **Residence**      | 最新  | `plugins/Residence/`                           | —         | 领地保护              | ✅              |
| 9   | **SkinsRestorer**  | 最新  | `plugins/SkinsRestorer.jar`                    | 7.64 MB   | 皮肤管理              | ✅              |
| 10  | **OpenInv**        | 最新  | `plugins/OpenInv.jar`                          | 347.81 KB | 离线背包查看          | ✅              |
| 11  | **EasyBot**        | 2.3.1 | `plugins/EasyBot-2.3.1.jar`                    | 3.75 MB   | QQ 机器人联动         | ✅              |
| 12  | **OPanel**         | 2.0.1 | `plugins/opanel-bukkit-1.21.9-build-2.0.1.jar` | 78.62 MB  | Web 管理面板          | ✅              |
| —   | **spark**          | 最新  | （服务端内置）                                 | —         | 性能分析              | ✅              |
| —   | **bStats**         | 最新  | （多插件内嵌）                                 | —         | 匿名统计              | ✅              |

**插件关键配置路径**：

| 插件          | 配置文件                                     | 关键项                                                          |
| ------------- | -------------------------------------------- | --------------------------------------------------------------- |
| LuckPerms     | `plugins/LuckPerms/config.yml`               | `server: global`、`storage-method: h2`、`auto-op: false`        |
| Vault         | `plugins/Vault/config.yml`                   | 经济实现、占位符                                                |
| ViaVersion    | `plugins/ViaVersion/config.yml`              | 旧版兼容、1.21 特定修复                                         |
| Geyser-Spigot | `plugins/Geyser-Spigot/config.yml`           | `bedrock.port=19132`、`clone-remote-port`、`floodgate-key-file` |
| Floodgate     | `plugins/floodgate/config.yml`               | `key.pem` 路径、玩家名前缀（默认 `.`）                          |
| CoreProtect   | `plugins/CoreProtect/config.yml`             | 记录项开关、SQLite / MySQL 存储                                 |
| Residence     | `plugins/Residence/config.yml` + `flags.yml` | 领地默认权限、全局 Flags                                        |
| SkinsRestorer | `plugins/SkinsRestorer/config.yml`           | 皮肤源、缓存                                                    |
| OpenInv       | `plugins/OpenInv/config.yml`                 | 跨世界背包                                                      |
| EasyBot       | `plugins/EasyBot/config.yml`                 | `service.url`、`service.token`、消息同步开关                    |
| OPanel        | `plugins/OPanel/config.yml`                  | `accessKey`、`salt`、`webServerPort=25555`                      |
| spark         | `plugins/spark/config.json`                  | 采样参数                                                        |
| bStats        | `plugins/bStats/config.yml`                  | `enabled`（可关闭匿名统计）                                     |

**各插件文档**：

| 插件          | 文档                                                                             |
| ------------- | -------------------------------------------------------------------------------- |
| CMILib        | [14-核心依赖库-CMILib](14-核心依赖库-CMILib/CMILib.md)                           |
| Vault         | [06-经济系统-Vault](06-经济系统-Vault/Vault.md)                                  |
| LuckPerms     | [05-权限管理系统-LuckPerms](05-权限管理系统-LuckPerms/LuckPerms.md)              |
| ViaVersion    | [04-跨版本协议兼容](04-跨版本协议兼容-ViaVersion/ViaVersion.md)                  |
| Geyser-Spigot | [03.1 Geyser-Spigot](03-Java-Bedrock互通层-Geyser-Floodgate/01-Geyser-Spigot.md) |
| Floodgate     | [03.2 Floodgate](03-Java-Bedrock互通层-Geyser-Floodgate/02-Floodgate.md)         |
| CoreProtect   | [07-方块记录与回滚](07-方块记录与回滚-CoreProtect/CoreProtect.md)                |
| Residence     | [08-领地系统-Residence](08-领地系统-Residence/Residence.md)                      |
| SkinsRestorer | [09-皮肤管理](09-皮肤管理-SkinsRestorer/SkinsRestorer.md)                        |
| OpenInv       | [12-离线背包查看](12-离线背包查看-OpenInv/OpenInv.md)                            |
| EasyBot       | [10-QQ机器人联动](10-QQ机器人联动-EasyBot/EasyBot.md)                            |
| OPanel        | [11-Web管理面板](11-Web管理面板-OPanel/OPanel.md)                                |
| spark         | [13-性能分析-spark](13-性能分析-spark/spark.md)                                  |
| bStats        | [15-统计系统-bStats](15-统计系统-bStats/bStats.md)                               |

### 4.5 外部程序（非插件）

这些程序独立运行，通过 WebSocket / HTTP 与 MC 服务端通信。

| 组件                            | 版本  | 平台            | 默认端口                         | 用途                        | 状态 |
| ------------------------------- | ----- | --------------- | -------------------------------- | --------------------------- | ---- |
| **NapCat**                      | 最新  | Windows / Linux | WS `3001`、WebUI `6099`/`6100`   | QQ 机器人框架（OneBot v11） | ✅   |
| **EasyBot 主程序**              | 2.3.1 | Windows / Linux | Bridge `26990`、Web `5000`       | MC ↔ QQ 桥接服务            | ✅   |
| **MCSManager (Panel + Daemon)** | 最新  | Windows / Linux | `23333` / `24444`                | 进程管理与 Web 控制台       | ✅   |
| **Caddy**                       | 2.x   | Linux           | `80` / `443` / `10873` / `10874` | HTTPS 反代（云主机）        | ✅   |

**EasyBot 三层连接**：

```
NapCat (WS :3001)  ←──  EasyBot 主程序 (Bridge :26990 / Web :5000)  ←──  EasyBot 插件
```

| 配置文件                     | 路径                                  | 作用                                         |
| ---------------------------- | ------------------------------------- | -------------------------------------------- |
| `OnebotOptions`              | `<EasyBot目录>/options/OnebotOptions` | EasyBot → NapCat 连接（WsUrl + AccessToken） |
| `appsettings.json`           | `<EasyBot目录>/appsettings.json`      | Bridge 端口与 Web 面板地址                   |
| `token.txt`                  | `<EasyBot目录>/token.txt`             | 鉴权 Token（须与插件端一致）                 |
| `config.yml`                 | `plugins/EasyBot/config.yml`          | 插件端连接与功能开关                         |
| `bind_config.json`           | `<EasyBot目录>/dp/bind_config.json`   | 玩家-QQ 绑定规则                             |
| `sync.json` / `command.json` | `<EasyBot目录>/dp/`                   | 消息同步规则 / QQ 端命令映射                 |

> 跨机部署时，仅需把插件端 `config.yml` 的 `service.url` 改为 EasyBot 主机的 IP 或域名，并放行 TCP 26990。

### 4.6 规划 / 备选插件

以下组件在本仓库中已有调研文档，但**尚未确认为当前服务器已安装**。安装前请先在测试服验证。

| 组件                        | 版本           | 用途                            | 前置依赖            | 文档                                                                                                       | 状态                   |
| --------------------------- | -------------- | ------------------------------- | ------------------- | ---------------------------------------------------------------------------------------------------------- | ---------------------- |
| **WorldEdit**               | 最新（1.21.x） | 创世神，批量方块编辑            | 无                  | [20.创世神WorldEdit](20.创世神WorldEdit/创世神WorldEdit.md)                                                | 📋 规划                |
| **TAB**                     | 5.5.0          | 自定义 TAB 列表、前缀后缀、动画 | 建议 PlaceholderAPI | [21 玩家信息展示](21-玩家信息收集与展示/2.TAB信息展示.md)                                                  | 📋 规划                |
| **Plan (Player Analytics)** | 5.6 build 2965 | 玩家行为分析 + Web 仪表盘       | 无                  | [1.Plan](<21-玩家信息收集与展示/1.Plan (Player Analytics).md>)                                             | 📋 规划                |
| **AntiLitematica**          | 最新           | 阻断投影「快速放置」与打印机    | **ProtocolLib**     | [部署文档](禁用影响平衡的插件功能/AntiLitematica-保姆级Litematica、打印机检测与阻断器/2.安装部署与配置.md) | 📋 建议                |
| **ProtocolLib**             | 最新           | 数据包级开发库                  | 无                  | —                                                                                                          | 📋 AntiLitematica 前置 |
| **CyuTag**                  | 最新           | 头顶多行信息展示                | PlaceholderAPI      | [21 选型对比](21-玩家信息收集与展示/0.玩家信息收集与展示.md)                                               | 💡 可选                |
| **BlueMap**                 | 最新           | 3D 网页地图                     | 无                  | [21 选型对比](21-玩家信息收集与展示/0.玩家信息收集与展示.md)                                               | 💡 可选                |

---

## 五、系统架构

### 5.1 同机部署（默认，推荐起步）

所有组件运行在同一台机器上，EasyBot 插件通过 `127.0.0.1` 直连主程序。

```
                         ┌──────────────────────────────────────┐
   QQ 服务器  ──────────►│  NapCat（QQ 框架）                    │
                         │    OneBot WS  :3001  (127.0.0.1)     │
                         └───────────────┬──────────────────────┘
                                         │  WS
                         ┌───────────────▼──────────────────────┐
                         │  EasyBot 主程序                       │
                         │    Bridge     :26990                 │
                         │    Web UI     :5000                  │
                         └───────────────┬──────────────────────┘
                                         │  WS :26990/bridge
┌────────────────────────────────────────▼──────────────────────────────────┐
│  Leaf 服务端  (java -javaagent:YggdrasilOfficialProxy -jar leaf.jar)      │
│                                                                           │
│   ┌─────────────┐   ┌──────────────┐   ┌────────────────────────────┐   │
│   │ authlib-    │   │  Geyser      │   │  业务插件层                 │   │
│   │ injector    │   │  +Floodgate  │   │  Residence / CoreProtect / │   │
│   │ :14502      │   │  UDP :19132  │   │  LuckPerms / Vault /       │   │
│   └──────┬──────┘   └──────────────┘   │  SkinsRestorer / OpenInv / │   │
│          │                              │  EasyBot / ViaVersion ...  │   │
│   ┌──────▼──────┐                       └────────────────────────────┘   │
│   │ Yggdrasil  │                                                         │
│   │ 认证 :32217│   ┌──────────────────────────────────────────────┐     │
│   └─────────────┘   │  OPanel Web 面板  HTTP :25555                │     │
│                     │  MCDR Socket      :25576 (127.0.0.1)        │     │
│                     └──────────────────────────────────────────────┘     │
│                                                                           │
│   Java 玩家 ──TCP :55551──►  [ ViaVersion 协议翻译 ]  ──►  Leaf          │
│   基岩玩家 ──UDP :19132──►  [ Geyser 协议转换 + Floodgate 认证 ] ──►     │
└───────────────────────────────────────────────────────────────────────────┘
```

### 5.2 跨机部署（QQ 机器人与 MC 服务器分离）

适用于「QQ 挂在家用机、MC 服务器放云服务器」的场景。**只需改 1 处配置**：
MC 端 `plugins/EasyBot/config.yml` 的 `service.url` 从 `127.0.0.1` 改为电脑 A 的 IP/域名，
并在电脑 A 防火墙放行 TCP 26990。

详见 [EasyBot 跨机部署](10-QQ机器人联动-EasyBot/EasyBot.md#easybot-与-mc-服务器在不同电脑的情况)。

---

## 六、端口总表

### 6.1 游戏服务端（本地 / 云主机）

| 端口      | 协议       | 用途                      | 绑定地址    | 是否公开      |
| --------- | ---------- | ------------------------- | ----------- | ------------- |
| **55551** | TCP        | Java 版游戏主端口         | `0.0.0.0`   | ✅ 是         |
| **19132** | UDP        | 基岩版游戏端口（Geyser）  | `0.0.0.0`   | ✅ 是         |
| **25555** | TCP (HTTP) | OPanel Web 管理面板       | `0.0.0.0`   | ⚠️ 建议仅内网 |
| **25576** | TCP        | OPanel MCDR Socket        | `127.0.0.1` | ❌ 否         |
| **14502** | TCP (HTTP) | authlib-injector 本地代理 | `127.0.0.1` | ❌ 否         |
| **32217** | TCP (HTTP) | Yggdrasil 认证服务器      | `localhost` | ❌ 否         |
| **25575** | TCP        | RCON                      | —           | ❌ 未启用     |

### 6.2 QQ 联动

| 端口      | 协议       | 用途                            | 绑定地址    | 是否公开            |
| --------- | ---------- | ------------------------------- | ----------- | ------------------- |
| **3001**  | TCP (WS)   | NapCat OneBot WebSocket         | `127.0.0.1` | ❌ 否               |
| **5000**  | TCP (HTTP) | EasyBot Web 管理面板            | `0.0.0.0`   | ⚠️ 建议仅内网       |
| **26990** | TCP (WS)   | EasyBot Bridge（插件 ↔ 主程序） | `0.0.0.0`   | 同机否 / 跨机需放行 |

### 6.3 云主机（若使用云服务器）

| 端口               | 服务                        | 是否公开        |
| ------------------ | --------------------------- | --------------- |
| 22                 | SSH / SFTP                  | ✅              |
| 80 / 443           | Caddy（HTTPS 全站）         | ✅              |
| 10874 / 10873      | Caddy HTTPS / HTTP 镜像入口 | ✅              |
| 23333 / 24444      | MCSM 面板 / daemon          | ❌ 走反代       |
| 6099 / 6100        | NapCat WebUI                | ✅              |
| 3001 / 3002 / 3003 | NapCat OneBot WS            | ✅              |
| 26990 / 5000       | EasyBot bridge / 管理       | ✅              |
| 3306               | MySQL                       | ❌ 仅 127.0.0.1 |

> **原则**：游戏端口必须公开；MCSM、MySQL、RCON、Bridge 端口**永不直接暴露公网**，统一走 Caddy 反代或 VPN。

---

## 七、从零部署

### 7.1 环境要求

| 组件     | 要求                        | 备注                                         |
| -------- | --------------------------- | -------------------------------------------- |
| 操作系统 | Windows 10+ / Ubuntu 22.04+ | 本文档以 Windows 为主，云主机为 Ubuntu 24.04 |
| Java     | **JDK 21**                  | MC 1.21+ 强制要求，低于 21 无法启动          |
| 内存     | ≥ 6 GB（服务端分配 4G）     | 双端互通场景建议 8 GB                        |
| 磁盘     | ≥ 40 GB SSD                 | 存档 + CoreProtect 数据库增长快              |
| 网络     | 上行 ≥ 10 Mbps              | 每玩家约需 50–150 Kbps                       |

### 7.2 目录结构

```
C:\mc_serve\1.21.11-test\
├── leaf-1.21.11-174.jar              # 服务端核心
├── YggdrasilOfficialProxy-2.3.0-paperclip.jar   # 外置登录 Agent
├── start.bat                          # 启动脚本
├── eula.txt                           # 必须改为 eula=true
├── server.properties                  # 主配置
├── bukkit.yml / spigot.yml / commands.yml / permissions.yml
├── server-icon.png                    # 64×64 服务器图标
├── authlib-injector.log               # 注入日志
├── config\                            # Paper/Gale/Leaf 运行时配置
│   ├── leaf-global.yml
│   ├── gale-global.yml
│   ├── gale-world-defaults.yml
│   ├── paper-global.yml
│   └── paper-world-defaults.yml
├── libraries\                         # MC 运行库（Leaf 自动管理）
├── plugins\                           # 插件目录
│   ├── Geyser-Spigot.jar
│   ├── floodgate-spigot.jar
│   ├── ViaVersion.jar
│   ├── LuckPerms-Bukkit-*.jar         # ⚠️ 需手动补齐
│   ├── CoreProtect.jar
│   ├── Residence\
│   ├── SkinsRestorer\
│   ├── EasyBot-2.3.1.jar
│   ├── opanel-bukkit-1.21.9-build-2.0.1.jar
│   ├── OpenInv.jar
│   ├── Vault\
│   ├── CMILib\
│   └── bStats\
├── opanel\                            # OPanel 辅助文件
├── world\  world_nether\  world_the_end\
├── logs\  crash-reports\
└── cache\  versions\
```

**其他文件与目录**：

| 路径                       | 说明                                    |
| -------------------------- | --------------------------------------- |
| `cache/mojang_1.21.11.jar` | Mojang 映射缓存（Leaf 自动生成）        |
| `libraries/`（203 文件）   | Minecraft 运行时依赖库（Leaf 自动管理） |
| `versions/`（2 文件）      | 版本历史 jar 归档                       |
| `logs/` `crash-reports/`   | 日志与崩溃报告（建议定期清理）          |
| `codeofconduct/`           | 行为准则目录（空）                      |

**OPanel 辅助文件**：

| 文件                        | 说明                               |
| --------------------------- | ---------------------------------- |
| `opanel/mcp-config.json`    | MCDReforged 通信配置（本服已禁用） |
| `opanel/launch-command.txt` | 面板启动命令记录                   |
| `opanel/open-api.json`      | OpenAPI 接口定义                   |
| `opanel/tasks.json`         | 定时任务配置                       |
| `opanel/login-banner.png`   | 登录页面横幅图片                   |

**核心配置文件清单**：

| 文件                                      | 作用                                         |
| ----------------------------------------- | -------------------------------------------- |
| `server.properties`                       | 服务器主配置（端口、模式、难度、MOTD）       |
| `bukkit.yml`                              | 生成限制、自动保存、区块 GC                  |
| `spigot.yml`                              | 实体激活范围、netty 线程、中文提示语         |
| `commands.yml`                            | 命令别名与覆盖                               |
| `permissions.yml`                         | 权限定义（**本服为空**，全部交给 LuckPerms） |
| `eula.txt`                                | 必须为 `true`                                |
| `help.yml`                                | Paper 帮助系统（当前为默认 `{}`）            |
| `purpur.yml`                              | ⚠️ **遗留文件，Leaf 不读取**                 |
| `ops.json` / `usercache.json`             | OP 名单 / 玩家名缓存（上限 1000 条）         |
| `banned-players.json` / `banned-ips.json` | 封禁名单                                     |
| `version_history.json`                    | 版本升级记录                                 |

### 7.3 部署步骤

#### 第 1 步：准备目录与核心

```powershell
mkdir C:\mc_serve\1.21.11-test
cd C:\mc_serve\1.21.11-test
```

1. 下载 `leaf-1.21.11-174.jar` 放入根目录
2. 下载 `YggdrasilOfficialProxy-2.3.0-paperclip.jar` 放入根目录
3. 新建 `eula.txt`，写入 `eula=true`

#### 第 2 步：首次启动（生成配置）

```batch
java -Xmx4G -Xms1G -jar leaf-1.21.11-174.jar nogui
```

首次启动会下载运行库、生成世界与全部配置文件，完成后输入 `stop` 关闭。

#### 第 3 步：修改 `server.properties`

关键项（详见 [八、核心配置速查](#八核心配置速查)）：

```properties
server-port=55551
online-mode=true
max-players=50
gamemode=survival
difficulty=easy
motd=§e南瓜§f生存服
view-distance=12
simulation-distance=4
allow-flight=false
network-compression-threshold=64
```

#### 第 4 步：安装插件

按顺序放入 `plugins\`（依赖关系见 [九、插件依赖与加载顺序](#九插件依赖与加载顺序)）：

```
CMILib → Vault → LuckPerms → ViaVersion → Geyser-Spigot → floodgate
→ CoreProtect → Residence → SkinsRestorer → OpenInv → EasyBot → OPanel → spark
```

#### 第 5 步：配置基岩互通

- Geyser `config.yml`：`bedrock.port=19132`，`floodgate-key-file` 指向 floodgate 的 `key.pem`
- 确认 `plugins\floodgate\key.pem` 已生成，且 Geyser 与 Floodgate **共用同一份密钥**
- 防火墙放行 **UDP 19132**

#### 第 6 步：配置外置登录

修改启动脚本，加入 `-javaagent`：

```batch
@Echo off
java -Xmx4G -Xms1G -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar -jar leaf-1.21.11-174.jar nogui
pause
```

保持 `online-mode=true`，Java 版玩家走 Yggdrasil 认证，基岩版由 Floodgate 绕过。

#### 第 7 步：配置 QQ 联动

1. 启动 NapCat → 开启 OneBot WS（`:3001`），记下 AccessToken
2. 启动 EasyBot 主程序 → 配置 `options\OnebotOptions` 指向 NapCat
3. 插件端 `plugins\EasyBot\config.yml`：

```yaml
service:
  url: 'ws://127.0.0.1:26990/bridge' # 跨机时改为电脑 A 的 IP
  token: '<随机32位字符串>' # 必须与 EasyBot\token.txt 一致
  ignore_error: false
```

#### 第 8 步：启动验收

按顺序启动：**NapCat → EasyBot 主程序 → MC 服务器**。

### 7.4 首次启动验收清单

| #   | 检查项                     | 预期结果                                               | 排查文档                                                                        |
| --- | -------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------- |
| 1   | 控制台无 `ERROR` / `Fatal` | 出现 `Done (x.xs)! For help, type "help"`              | —                                                                               |
| 2   | `/pl` 插件全绿             | 无红色插件名                                           | [四、组件清单](#四组件清单)                                                     |
| 3   | authlib-injector 注入成功  | 日志含 `Authentication server: http://localhost:32217` | [02 外置登录](02-外置登录代理-YggdrasilOfficialProxy/YggdrasilOfficialProxy.md) |
| 4   | Java 玩家可进服            | 客户端直连 `<你的IP>:55551` 成功                       | —                                                                               |
| 5   | 基岩玩家可进服             | 基岩版添加服务器 `<你的IP>:19132` 成功                 | [常见问题 Q1](常见问题/常见问题排查.md)                                         |
| 6   | 基岩玩家名带 `.` 前缀      | 如 `.Steve`                                            | [03.2 Floodgate](03-Java-Bedrock互通层-Geyser-Floodgate/02-Floodgate.md)        |
| 7   | QQ 群 ↔ 游戏聊天同步       | 双向可见                                               | [常见问题 Q2](常见问题/常见问题排查.md)                                         |
| 8   | `/spark tps` 正常          | TPS ≥ 19                                               | [13 spark](13-性能分析-spark/spark.md)                                          |
| 9   | `/co inspect` 可查询       | 点击方块返回记录                                       | [07 CoreProtect](07-方块记录与回滚-CoreProtect/CoreProtect.md)                  |
| 10  | OPanel 可登录              | `http://<你的IP>:25555`                                | [11 OPanel](11-Web管理面板-OPanel/OPanel.md)                                    |
| 11  | 皮肤正常显示               | 基岩/Java 皮肤均加载                                   | [09 SkinsRestorer](09-皮肤管理-SkinsRestorer/SkinsRestorer.md)                  |
| 12  | 领地可创建                 | `/res create <名>` 成功                                | [08 Residence](08-领地系统-Residence/Residence.md)                              |

---

## 八、核心配置速查

### 8.1 `server.properties`

| 配置项                          | 值               | 说明                                 |
| ------------------------------- | ---------------- | ------------------------------------ |
| `server-port`                   | `55551`          | Java 版服务端口                      |
| `online-mode`                   | `true`           | 正版验证开启（外置登录代理依赖此项） |
| `max-players`                   | `50`             | 最大同时在线玩家数                   |
| `gamemode`                      | `survival`       | 默认游戏模式                         |
| `difficulty`                    | `easy`           | 游戏难度                             |
| `motd`                          | `§e南瓜§f生存服` | 服务器列表标题                       |
| `view-distance`                 | `12`             | 视距（区块）                         |
| `simulation-distance`           | `4`              | 实体模拟距离（区块）                 |
| `allow-flight`                  | `false`          | 禁止飞行（防作弊）                   |
| `enforce-whitelist`             | `true`           | 强制白名单验证                       |
| `white-list`                    | `false`          | 白名单功能未启用                     |
| `spawn-protection`              | `16`             | 出生点保护半径                       |
| `enforce-secure-profile`        | `false`          | 不强制安全档案（兼容离线/基岩版）    |
| `rate-limit`                    | `0`              | 无速率限制（基岩互通需要）           |
| `network-compression-threshold` | `64`             | 网络压缩阈值                         |
| `region-file-compression`       | `deflate`        | 区域文件压缩算法                     |

### 8.2 `spigot.yml` / `bukkit.yml`

| 配置项                     | 值      | 说明                               |
| -------------------------- | ------- | ---------------------------------- |
| `bungeecord`               | `false` | 未启用代理模式                     |
| `restart-on-crash`         | `true`  | 崩溃后自动重启                     |
| `netty-threads`            | `4`     | 网络线程数                         |
| `user-cache-size`          | `1000`  | 用户缓存大小                       |
| `connection-throttle`      | `4000`  | 连接节流(ms)，**基岩互通需较高值** |
| `ticks-per.autosave`       | `6000`  | 自动保存频率（刻）                 |
| `chunk-gc.period-in-ticks` | `600`   | 区块垃圾回收周期                   |
| `spawn-limits.monsters`    | `32`    | 怪物上限                           |

### 8.3 配置层级

Leaf 继承 Paper → Gale 的配置体系，**层级越靠前优先级越高**：

```
leaf-global.yml  >  gale-global.yml  >  paper-global.yml
```

修改后需重启，或使用 `/leaf reload` 热重载。

> ⚠️ `purpur.yml` 是历史遗留文件，**Leaf 不读取**。详见 [16 配置文件补充](16-服务器配置文件补充/README.md)。

### 8.4 启动参数

```batch
java -Xmx4G -Xms1G ^
     -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar ^
     -jar leaf-1.21.11-174.jar nogui
```

| 参数             | 说明                        |
| ---------------- | --------------------------- |
| `-Xmx4G`         | 最大堆内存 4GB              |
| `-Xms1G`         | 初始堆内存 1GB              |
| `-javaagent:...` | 加载 Yggdrasil 外置登录代理 |
| `nogui`          | 无图形界面模式              |

> 生产环境建议追加 Aikar flags（`-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15`）。

---

## 九、插件依赖与加载顺序

### 9.1 依赖矩阵

| 插件           | 硬依赖                          | 软依赖 / 集成                      |
| -------------- | ------------------------------- | ---------------------------------- |
| CMILib         | —                               | 被 Residence 等调用                |
| Vault          | —                               | 经济 API 提供方                    |
| LuckPerms      | —                               | 为 Vault 提供权限组信息            |
| ViaVersion     | —                               | 与 Geyser 协同处理基岩协议         |
| Geyser-Spigot  | —                               | 与 Floodgate **共用 `key.pem`**    |
| Floodgate      | Geyser（可选）                  | 与 ViaVersion 协同                 |
| CoreProtect    | —                               | 可选 MySQL                         |
| Residence      | **CMILib**                      | 可选 Vault（领地出租 / 出售）      |
| SkinsRestorer  | —                               | 经 authlib-injector 走外置皮肤 API |
| OpenInv        | —                               | —                                  |
| EasyBot        | **EasyBot 主程序**（WS :26990） | 与 Floodgate 协同识别基岩玩家      |
| OPanel         | —                               | 内置 Web 前端                      |
| spark          | —                               | —                                  |
| bStats         | —                               | 被多数插件内嵌                     |
| AntiLitematica | **ProtocolLib**                 | —                                  |
| TAB            | —                               | PlaceholderAPI                     |

### 9.2 安装顺序（重要）

```
①  CMILib        ②  Vault         ③  LuckPerms     ④  ViaVersion
⑤  Geyser-Spigot ⑥  floodgate     ⑦  CoreProtect   ⑧  Residence
⑨  SkinsRestorer ⑩  OpenInv       ⑪  EasyBot       ⑫  OPanel
⑬  spark
```

> Geyser 必须在 Floodgate **之前**放置，以便生成并共享 `key.pem`。
> 首次安装按此顺序可减少一轮重启；Bukkit 启动时会自行解析依赖，顺序错误不会导致插件失效。

---

## 十、日常运维

### 10.1 高频命令

#### 性能与诊断

```bash
/spark tps                          # 查看 TPS
/spark health                       # 健康报告
/spark profiler start --timeout 300 # 5 分钟采样分析
```

#### 玩家与权限

```bash
/lp user <玩家> info                # 查看权限详情
/lp user <玩家> parent add <组>     # 加入权限组
/openinv <玩家>                     # 查看/编辑背包（支持离线）
/openender <玩家>                   # 查看末影箱
```

#### 方块审计

```bash
/co inspect                         # 开启检查模式（点击方块查记录）
/co lookup u:<玩家> t:1d r:50       # 查某玩家 24h 内 50 格范围操作
/co rollback u:<玩家> t:1h r:50     # 回滚某玩家 1 小时内操作
/co purge t:30d                     # 清理 30 天前数据
/co near                            # 查看附近最近变更
```

#### 经济

```bash
/eco give <玩家> <金额>             # 给予金钱
/eco take <玩家> <金额>             # 扣除金钱
/vaultop                            # 财富排行榜
/vault reload                       # 重载经济配置
```

#### 基岩互通与皮肤

```bash
/geyser dump                        # 生成调试信息
/geyser statistics                  # 查看统计
/geyser reload                      # 重载配置
/sr reload                          # 重载皮肤插件
/sr props <玩家>                    # 查看皮肤属性
```

#### 领地

```bash
/resadmin                           # 管理员模式
/res list <玩家>                    # 列出玩家所有领地
/res message <领地> enter <消息>    # 设置进入提示
```

#### QQ 联动

```bash
/bind <验证码>                      # 玩家绑定 QQ
/ebot reload                        # 重载 EasyBot 并重连 Bridge
```

> 完整速查见 [18-常用管理命令速查](18-常用管理命令速查/README.md)。

### 10.2 备份策略

| 数据        | 路径                                          | 频率     | 方式                       |
| ----------- | --------------------------------------------- | -------- | -------------------------- |
| 世界存档    | `world\`、`world_nether\`、`world_the_end\`   | 每日     | 停服后打包 / MCSM 计划任务 |
| 插件配置    | `plugins\**\*.yml`                            | 每次改动 | Git 或压缩包               |
| 权限数据    | `plugins\LuckPerms\luckperms-h2-v2.mv.db`     | 每周     | 或 `/lp export` 导出       |
| CoreProtect | `plugins\CoreProtect\database.db` 或 MySQL 库 | 每周     | `mysqldump`                |
| 领地数据    | `plugins\Residence\**\*.yml`                  | 每周     | 打包                       |

> **注意**：热备份（不停服）可能导致区块数据与数据库不一致。CoreProtect 若已迁 MySQL，请用 `mysqldump --single-transaction`。

### 10.3 监控与告警

- **游戏内**：`/spark tps`、`/spark health`
- **云主机**：[08\_监控系统.md](云服务器配置/08_监控系统.md) 提供双通道（QQ + 邮件）告警
- **Web**：OPanel 面板实时查看

### 10.4 例行维护

| 周期   | 任务                                                                                                                      |
| ------ | ------------------------------------------------------------------------------------------------------------------------- |
| 每周   | `/co purge t:30d` 清理审计数据                                                                                            |
| 每周   | 检查 `logs\` 与 `crash-reports\`，清理 30 天前日志                                                                        |
| 每月   | 用 [MCA Selector](常见问题/服务器内存相关/region文件夹：生成和保存的区块过大/MCASelector/MCASelector.md) 裁剪跑图冗余区块 |
| 每月   | 检查插件更新（Leaf / Geyser / ViaVersion 需同步跟进 MC 版本）                                                             |
| 每季度 | 全量备份 + 恢复演练                                                                                                       |

---

## 十一、故障排查速查

| 现象                            | 优先检查                                                                         | 详细文档                                                                                                                                                         |
| ------------------------------- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **基岩版玩家无法连接**          | ① UDP 19132 防火墙 ② Geyser `bedrock.port` ③ `key.pem` 路径一致性                | [常见问题 Q1](常见问题/常见问题排查.md)                                                                                                                          |
| **QQ 消息不同步**               | ① EasyBot 主程序是否运行 ② `service.url` 地址 ③ Token 一致性 ④ TCP 26990 放行    | [常见问题 Q2](常见问题/常见问题排查.md)                                                                                                                          |
| **服务器卡顿**                  | ① `/spark profiler` 采样 ② 视距是否过高 ③ 内存是否充足                           | [常见问题 Q3](常见问题/常见问题排查.md)                                                                                                                          |
| **CoreProtect 数据库过大**      | ① `/co purge` ② 迁 MySQL                                                         | [常见问题 Q4](常见问题/常见问题排查.md) / [07.1](07-方块记录与回滚-CoreProtect/CoreProtect插件：玩家的行为数据库过大/1.CoreProtect插件：玩家的行为数据库过大.md) |
| **权限不生效**                  | ① LuckPerms 主 JAR 是否存在 ② 组权重 ③ 子区域继承                                | [05 LuckPerms](05-权限管理系统-LuckPerms/LuckPerms.md)                                                                                                           |
| **领地配置不生效**              | ① 旧领地缓存 ② `groups.yml` 覆盖 ③ 父子区域继承                                  | [08 Residence](08-领地系统-Residence/Residence.md)                                                                                                               |
| **region 文件夹膨胀**           | 跑图产生的冗余区块                                                               | [region 瘦身](常见问题/服务器内存相关/region文件夹：生成和保存的区块过大/1.region文件夹：生成和保存的区块过大.md)                                                |
| **Java 版无法登录（认证失败）** | ① authlib-injector 是否注入 ② Yggdrasil 认证服务 :32217 是否可达 ③ `online-mode` | [02 外置登录](02-外置登录代理-YggdrasilOfficialProxy/YggdrasilOfficialProxy.md)                                                                                  |
| **皮肤不显示**                  | ① SkinsRestorer 配置 ② authlib-injector 是否拦截了 SkinProvider                  | [09 SkinsRestorer](09-皮肤管理-SkinsRestorer/SkinsRestorer.md)                                                                                                   |
| **玩家用投影/打印机作弊**       | 安装 AntiLitematica（需 ProtocolLib）                                            | [禁用影响平衡的插件功能](禁用影响平衡的插件功能/1.禁用影响平衡的插件功能.md)                                                                                     |

---

## 十二、已知问题

### 🔴 P0 — 必须处理

| 问题                   | 详情                                                                                                                                                     | 解决方式                                                                                                                        |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **LuckPerms 缺主 JAR** | `plugins/LuckPerms/` 下有配置文件、数据库 `luckperms-h2-v2.mv.db` 和 24 个依赖库 jar，**但缺少主插件 JAR**（`LuckPerms-Bukkit-*.jar`），权限系统无法加载 | 从 [LuckPerms 下载页](https://luckperms.net/download) 下载 `LuckPerms-Bukkit` 放入 `plugins/`，重启即可（现有权限数据不会丢失） |

### 🟡 P1 — 建议处理

| 问题                       | 详情                                     | 解决方式                                                                                                                                                                 |
| -------------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **CoreProtect 数据库膨胀** | SQLite 模式下 `database.db` 增长极快     | ① 定期 `/co purge t:30d`；② 生产环境迁移 MySQL，见 [迁移文档](07-方块记录与回滚-CoreProtect/CoreProtect插件：玩家的行为数据库过大/2.将CoreProtect从SQLite切换到MySQL.md) |
| **region 文件夹过大**      | 玩家跑图产生大量冗余区块                 | 用 [MCA Selector](常见问题/服务器内存相关/region文件夹：生成和保存的区块过大/MCASelector/MCASelector.md) 按「停留时长」筛选裁剪                                          |
| **OPanel 暴露风险**        | `webServerPort` 默认绑定 `0.0.0.0:25555` | 改为绑定 `127.0.0.1`，对外用 Caddy / Nginx 反代 + HTTPS                                                                                                                  |

### 🔵 P2 — 提示

| 问题              | 详情                                                                                                     |
| ----------------- | -------------------------------------------------------------------------------------------------------- |
| `purpur.yml` 失效 | 从 Purpur 迁移遗留，**Leaf 不读取**；`spigot.yml`、`bukkit.yml` 仍有效                                   |
| 版本号空白        | CMILib / Residence / OpenInv / spark / bStats 记录为「最新」，建议锁定具体版本号以便复现                 |
| 备份一致性        | 热备份（不停服）可能导致区块数据与 CoreProtect 数据库不一致，MySQL 请用 `mysqldump --single-transaction` |

---

## 十三、安全基线

### 13.1 必须执行

| #   | 项目                   | 要求                                                                                     |
| --- | ---------------------- | ---------------------------------------------------------------------------------------- |
| 1   | **OPanel accessKey**   | 部署后立即改为随机 32 位字符串；`webServerPort` 尽量绑定 `127.0.0.1`，对外走反代 + HTTPS |
| 2   | **EasyBot Token**      | 主程序 `token.txt` 与插件 `config.yml` 保持一致，使用随机 32 位字符串                    |
| 3   | **NapCat AccessToken** | 修改默认 Token，WS 端口不直接暴露公网                                                    |
| 4   | **MCSM Daemon key**    | 修改 `global.json` 默认密钥，**绝不暴露 24444 到公网**                                   |
| 5   | **MySQL**              | 仅监听 `127.0.0.1`，使用强密码，为每个插件建独立库与用户                                 |
| 6   | **RCON**               | 保持关闭；如必须启用，使用强密码并限制来源 IP                                            |
| 7   | **SSH**                | 禁用密码登录，改用密钥；可改非 22 端口                                                   |
| 8   | **定期备份**           | 异地保存，定期演练恢复                                                                   |

### 13.2 凭据管理规范

- 所有真实凭据**只存于本机/服务器**，不进入 Git 仓库
- 文档中使用 `<占位符>` 表示；如需记录，放入 `.env` 或带权限控制的密码管理器
- 泄露后立即轮换：OPanel accessKey → EasyBot Token → NapCat Token → MCSM key → 数据库密码

### 13.3 网络最小化原则

```
公网可访问：  55551/TCP   19132/UDP   80/443（Caddy）
仅内网/VPN：  25555(OPanel)  5000(EasyBot UI)  24444(MCSM Daemon)  3306(MySQL)
绝不暴露：    25576(OPanel Socket)  14502(authlib)  32217(Yggdrasil)  25575(RCON)
按需放行：    26990(EasyBot Bridge，仅跨机场景)
```

---

## 十四、资源链接

### 14.1 服务端与核心

| 项目             | 链接                                            |
| ---------------- | ----------------------------------------------- |
| Leaf             | https://github.com/Winds-Studio/Leaf/releases   |
| Paper            | https://papermc.io                              |
| authlib-injector | https://github.com/yushijinhun/authlib-injector |
| Geyser           | https://geysermc.org/download                   |
| ViaVersion       | https://viaversion.com                          |

### 14.2 插件

| 项目          | 链接                                                   |
| ------------- | ------------------------------------------------------ |
| LuckPerms     | https://luckperms.net/download                         |
| Vault         | https://www.spigotmc.org/resources/vault.34315/        |
| CoreProtect   | https://modrinth.com/plugin/coreprotect                |
| Residence     | https://www.spigotmc.org/resources/residence.11480/    |
| SkinsRestorer | https://skinsrestorer.net                              |
| OpenInv       | https://github.com/Jikoo/OpenInv/releases              |
| EasyBot       | https://github.com/DreamVoid/EasyBot                   |
| OPanel        | https://opanel.cc                                      |
| spark         | https://spark.lucko.me/download                        |
| CMILib        | https://www.spigotmc.org/resources/cmi-lib.87610/      |
| ProtocolLib   | https://www.spigotmc.org/resources/protocollib.1997/   |
| WorldEdit     | https://modrinth.com/plugin/worldedit                  |
| TAB           | https://www.spigotmc.org/resources/tab-1-7-1-21.57806/ |
| Plan          | https://modrinth.com/plugin/plan                       |

| 插件                     | 官方下载页面                                                                                          | 说明                                             |
| ------------------------ | ----------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| **EssentialsX**          | [EssentialsX 官方下载页](https://essentialsx.net/downloads)                                           | 需下载 EssentialsX 核心 + Chat + Spawn 模块      |
| **Geyser-Spigot**        | [Geyser 官方下载页](https://geysermc.org/download)                                                    | 选择 Spigot 版本（作为插件安装）                 |
| **Floodgate**            | [Floodgate 官方下载页](https://geysermc.org/download)                                                 | 与 Geyser 同一下载页，选择 Floodgate-Spigot 版本 |
| **BedrockPlayerSupport** | [BedrockPlayerSupport SpigotMC 页面](https://www.spigotmc.org/resources/bedrockplayersupport.114738/) | 作者 DongShao，当前最新版 v2.1.1                 |

---

### 14.3 运维工具

| 项目         | 链接                                 |
| ------------ | ------------------------------------ |
| MCSManager   | https://mcsmanager.com               |
| Caddy        | https://caddyserver.com              |
| NapCat       | https://github.com/NapNeko/NapCatQQ  |
| MCA Selector | https://github.com/Querz/mcaselector |

---

## 维护记录

| 日期       | 变更                                                                                                                                                     |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-08-09 | 初始文档生成（基于 `C:\mc_serve\1.21.11-test` 实际配置）                                                                                                 |
| 2026-08-11 | 补充云服务器配置章节（Caddy / MCSM / NapCat / 监控）                                                                                                     |
| 2026-09-01 | 重写 README 为完整运维手册：新增组件清单、端口表、部署流程、依赖矩阵、安全基线；敏感信息改为占位符；同步整理插件清单                                     |
| 2026-09-01 | 将原 `插件清单.md`（附录 A）合并进 README 第四章组件清单，下载源并入资源链接，目录文件说明并入第七章，已知问题独立成第十二章；删除重复文件 `插件清单.md` |

---

> 本仓库仅记录公开可复现的技术方案。所有凭据、IP、密钥均为占位符，实际值请在本机配置文件中查看。
