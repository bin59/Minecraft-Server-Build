# 16. 服务器配置文件补充（Leaf 运行目录全览）

本文档从 **26.3 运行目录 `F:\game\pc\MC\开服\服务器数据备份\leaf-26.3\leaf-26.3`**（Leaf 26.3 实际运行目录的备份）出发，给出完整的**目录树形图**，并逐文件、逐目录说明作用与当前取值。核心覆盖：顶层配置/数据文件（server.properties、bukkit.yml、spigot.yml 等）、`config/` 配置链（Leaf/Paper/Gale）、`plugins/` 插件区（插件 jar 与其配置/数据目录）、`world*` 世界数据，以及 libraries/versions/cache/bluemap/opanel/logs 等运行目录。原「配置文件补充」的详细取值说明（bukkit.yml 生成限制、spigot.yml 中文消息、commands.yml 别名、server.properties MOTD）一并保留于对应章节。

> 备份目录体量参考：总约 **3.5 GB**。其中 `.git`（版本历史）840 MB、`plugins/` 693 MB（约 2 万文件）、`world/` 940 MB、`libraries/` 120 MB、`bluemap/` 118 MB、顶层 jar 约 150 MB。

## 一、目录树全览（leaf-26.3）

```
leaf-26.3/                                     # 服务器运行目录备份（Leaf 26.3 生产）
├── .git/                        # Git 版本库（约 840 MB，含全部提交历史）
├── .history/                    # 用户缓存快照（usercache_2026*.json，备份历史）
├── bluemap/                     # BlueMap 3D 地图数据（本服现行网页地图）
│   ├── logs/                    #   BlueMap 运行日志
│   └── web/                     #   渲染输出：assets/ lang/ maps/(world) index.html settings.json
├── cache/                       # 客户端 jar 缓存：mojang_26.3.jar
├── codeofconduct/               # （空）遗留目录
├── config/                      # Leaf/Paper/Gale 配置链（见第三节）
│   ├── leaf-global.yml          #   Leaf 特有全局配置
│   ├── gale-global.yml          #   Gale 全局配置
│   ├── gale-world-defaults.yml  #   Gale 世界默认配置
│   ├── paper-global.yml         #   Paper 全局配置
│   └── paper-world-defaults.yml #   Paper 世界默认配置
├── crash-reports/               # 崩溃报告：crash-2026-09-19_19.52.54-server.txt
├── libraries/                   # 服务端运行时依赖库（按组织分组：com/net/org/io...）
├── logs/                        # 服务器日志：latest.log + 每日 .log.gz 归档（300+ 份）
├── opanel/                      # OPanel Web 面板数据（见第六节）
├── plugins/                     # 插件区：jar + 各插件配置/数据目录（见第四节）
│   ├── .paper-remapped/         #   Paper 重映射缓存（remap 后的 jar 副本）
│   ├── <各插件>.jar 与 <插件名>/ #   见第四节插件清单表
│   └── update/                  #   插件更新暂存目录（空）
├── uploaded_skins/              # （空）上传皮肤暂存目录
├── versions/                    # 核心 jar 缓存：versions/26.3/
├── world/                       # 主世界（见第五节）
│   ├── region/                  #   区块文件 .mca（548 个，约 810 MB）
│   ├── playerdata/              #   玩家数据（344 个）
│   ├── stats/  advancements/    #   玩家统计 / 进度
│   ├── poi/  entities/          #   兴趣点 / 实体
│   ├── data/  datapacks/        #   世界数据 / 数据包（bukkit、传送阵、自动开关生物破坏）
│   ├── generated/               #   生成缓存
│   └── level.dat 等顶层文件
├── world_nether/                # 下界（DIM-1/ + level.dat + paper/gale-world.yml）
├── world_the_end/               # 末地（DIM1/ + level.dat + paper/gale-world.yml）
│
├── .console_history             # 控制台命令历史
├── .gitignore                   # Git 忽略规则
├── authlib-injector-1.2.8.jar   # 外置登录注入引擎（YggdrasilOfficialProxy 内置）
├── authlib-injector.log         # 外置登录注入日志
├── banned-ips.json              # 已封禁 IP 名单
├── banned-players.json          # 已封禁玩家名单
├── bukkit.yml                   # Bukkit 通用配置
├── commands.yml                 # 命令别名与覆盖
├── eula.txt                     # 最终用户许可协议（=true）
├── help.yml                     # Paper 帮助系统配置（默认空）
├── hs_err_pid6636.log / .mdmp   # JVM 崩溃日志与转储（排障参考）
├── leaf-26.3-18.jar             # 当前服务端核心（Leaf 26.3）
├── map-color-cache.dat          # 地图颜色缓存
├── ops.json                     # 管理员（OP）名单
├── permissions.yml              # 权限定义文件（空，权限由 LuckPerms 管理）
├── purpur.yml                   # Purpur 遗留配置（Leaf 不读取）
├── server-icon.png              # 服务器图标（64×64）
├── server.properties            # 服务器主配置
├── spigot.yml                   # Spigot 性能配置
├── start.bat                    # 启动脚本
├── usercache.json               # 玩家用户名缓存
├── version_history.json         # 版本升级记录
├── wepif.yml                    # WorldEdit 权限接口配置
├── whitelist.json / .backup     # 白名单
└── YggdrasilOfficialProxy-2.3.0-paperclip.jar / .conf   # 外置登录代理 jar 与配置
```

> 树中 `.../` 表示有子目录省略；`plugins/`、`world*` 内部结构见第四、五节。

---

## 二、顶层配置与数据文件

### 2.1 配置文件总览

| 状态    | 文件                   | 作用                                          |
| ------- | ---------------------- | --------------------------------------------- |
| ✅ 核心 | `server.properties`    | 服务器主配置（端口、模式、难度等）            |
| ✅ 核心 | `bukkit.yml`           | Bukkit 通用配置（区块生成、怪物生成限制等）   |
| ✅ 核心 | `spigot.yml`           | Spigot 性能配置（实体激活范围、tick 率等）    |
| ✅ 核心 | `commands.yml`         | 命令别名和覆盖                                |
| ✅ 核心 | `permissions.yml`      | 权限定义文件（空）                            |
| ✅ 核心 | `eula.txt`             | 最终用户许可协议（必须为 `true`）             |
| ✅ 核心 | `help.yml`             | Paper 帮助系统配置（当前为默认空配置 `{}`）   |
| ⚠️ 遗留 | `purpur.yml`           | Purpur 配置 — **Leaf 不读取此文件**，迁移遗留 |
| 📋 数据 | `ops.json`             | 服务器管理员（OP）名单                        |
| 📋 数据 | `usercache.json`       | 玩家用户名缓存（当前 1000 条上限）            |
| 📋 数据 | `banned-players.json`  | 已封禁玩家列表                                |
| 📋 数据 | `banned-ips.json`      | 已封禁 IP 列表                                |
| 📋 数据 | `version_history.json` | 服务器版本升级记录                            |
| 🖼️ 资源 | `server-icon.png`      | 服务器图标（64×64，显示在客户端服务器列表）   |

### 2.2 顶层运行文件补充（非配置文件）

| 文件                                               | 作用                                                                            |
| -------------------------------------------------- | ------------------------------------------------------------------------------- |
| `leaf-26.3-18.jar`                                 | **当前服务端核心**（Leaf 26.3，构建 18），启动脚本入口                          |
| `YggdrasilOfficialProxy-2.3.0-paperclip.jar`       | 外置登录代理（javaagent），配合 `-javaagent` 加载                               |
| `YggdrasilOfficialProxy.conf`                      | 外置登录代理配置（认证源等）                                                    |
| `authlib-injector-1.2.8.jar`                       | 外置登录注入引擎（被代理内置调用）                                              |
| `authlib-injector.log`                             | 外置登录注入运行日志                                                            |
| `start.bat`                                        | 启动脚本（`-Xmx24G -Xms6G` + G1GC + StringDeduplication + `-javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar -jar leaf-26.3-18.jar nogui`） |
| `logs/`、`crash-reports/`、`hs_err_pid*.log/.mdmp` | 运行日志 / 崩溃报告 / JVM 崩溃转储（排障）                                      |
| `map-color-cache.dat`                              | 地图颜色缓存（服务端自维护，可删，会自动重建）                                  |
| `wepif.yml`                                        | WorldEdit 权限接口配置                                                          |
| `whitelist.json` / `.backup`                       | 白名单 / 白名单备份                                                             |
| `.console_history`                                 | 控制台命令历史                                                                  |
| `usercache.json` / `.history/*.json`               | 玩家名缓存 / 历史快照                                                           |
| `.gitignore`                                       | Git 忽略规则（world、logs 等不入库）                                            |

### 2.3 `bukkit.yml` 关键配置

```yaml
spawn-limits:
  monsters: 32 # 怪物上限
  animals: 8 # 动物上限
  water-animals: 4 # 水生动物上限
  ambient: 4 # 环境生物上限

ticks-per:
  animal-spawns: 600 # 动物生成频率
  monster-spawns: 1 # 怪物生成频率
  autosave: 6000 # 自动保存频率(刻)

chunk-gc:
  period-in-ticks: 600 # 区块垃圾回收周期

connection-throttle: 4000 # 连接节流(ms)，Geyser需要较高值
```

### 2.4 `spigot.yml` 消息配置（中文）

```yaml
messages:
  whitelist: '您未被加入白名单！请联系服务器管理员！'
  unknown-command: '未知命令。输入 "/help" 获取帮助。'
  server-full: '服务器已满！'
  outdated-client: '客户端过旧！请使用 {0}'
  outdated-server: '服务器过旧！当前版本为 {0}'
  restart: '服务器正在重启。'
```

### 2.5 `permissions.yml`

文件为空，所有权限由 **LuckPerms** 管理。

### 2.6 `commands.yml`

```yaml
command-block-overrides: []
ignore-vanilla-permissions: false
aliases:
  icanhasbukkit:
    - 'version $1-'
```

### 2.7 `server.properties` MOTD 配置

`motd` 为服务器在客户端「多人游戏」列表处展示的标题，支持 `§` 颜色代码与 `\n` 换行（两行）。
当前最终取值：

```properties
motd=§x§f§f§5§e§1§9§l南§x§f§f§8§a§2§6§l瓜§x§f§f§b§b§3§3§l生§x§f§f§c§c§4§0§l存§x§f§f§d§d§6§6§l服§r §8▏ §x§f§f§5§e§1§9§l归墟纪·墟火\n§x§f§f§d§d§6§6✦ §a生存 §8· §e领地 §8· §d养老 §8· §b共建 §x§f§f§d§d§6§6✦ §7欢迎加入
```

**玩家实际看到的效果**（两行）：

```
第一行：南瓜生存服 ▏ 归墟纪·墟火
第二行：✦ 生存 · 领地 · 养老 · 共建 ✦ 欢迎加入
```

**配色说明**：

| 片段              | 颜色代码                               | 效果                                 |
| ----------------- | -------------------------------------- | ------------------------------------ |
| 南 / 瓜 / 生 / 存 | `§x§f§f§5§e§1§9` 等十六进制渐变 + `§l` | 橙→金→青→红→金 逐字渐变加粗标题      |
| ▏                 | `§8`                                   | 灰色竖分隔线                         |
| 归墟纪·墟火       | `§x§f§f§5§e§1§9§l`                     | 紫色渐变加粗（服务器联动小说项目名） |
| ✦                 | `§x§f§f§d§d§6§6`                       | 金粉色渐变装饰星                     |
| 生存              | `§a`                                   | 绿色                                 |
| 领地              | `§e`                                   | 金色                                 |
| 养老              | `§d`                                   | 淡紫色                               |
| 共建              | `§b`                                   | 青色                                 |
| ·                 | `§8`                                   | 灰色间隔点                           |
| 欢迎加入          | `§7`                                   | 灰色                                 |

**注意**：

- MOTD / 图标（图标保持原 `server-icon.png` 不变）均由后端 `server.properties` 直接控制，**本服未使用 Velocity 代理**，无需代理端配置。
- 十六进制渐变格式为 `§x§R§R§G§G§B§B`（每个通道重复两次），1.21 支持；修改后**需重启后端实例**才生效。

---

## 三、`config/` 目录（Leaf/Paper/Gale 配置链）

Leaf 继承 Paper → Gale 配置体系，首次启动生成。配置优先级：`leaf-global.yml` > `gale-global.yml` > `paper-global.yml`。

| 文件                       | 作用                                                                                                            |
| -------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `leaf-global.yml`          | **Leaf 特有全局配置**（异步区块发送 / 异步生物生成 / 异步寻路等，见 `1-服务端核心与网络层/服务器核心-Leaf`） |
| `gale-global.yml`          | Gale 全局配置（Leaf 上游）                                                                                      |
| `gale-world-defaults.yml`  | Gale 分世界默认配置                                                                                             |
| `paper-global.yml`         | Paper 全局配置（继承自 Paper，如区块发送、实体激活等）                                                          |
| `paper-world-defaults.yml` | Paper 分世界默认配置                                                                                            |

> 另在每个世界目录下有 `paper-world.yml`、`gale-world.yml`（分世界覆盖），见第五节。

---

## 四、`plugins/` 目录（插件区）

`plugins/` 顶层同时存在插件 **jar** 与同名**配置/数据目录**；`.paper-remapped/` 是 Paper 的**重映射缓存**（把插件 remap 到当前服务端版本，含 `extra-plugins/` `libraries/` `mappings/` `remap-classpath/` `unknown-origin/`）。`update/` 为插件更新暂存（空）。

> 实跑 `plugins/` 中带 `.bak` 后缀的 jar 只有 `EasyBot-2.4.0.jar.bak`（QQ 联动，当前停用，数据目录 `EasyBot/` 保留）。其余 51 个顶层 jar 均为启用状态。

### 4.1 插件清单（jar / 作用 / 数据目录）

> 按 2026-11-10 实跑 `plugins/` 顶层 **52 个 jar** 逐个核对；另有 `spark`（Leaf 内置，见 `libraries/`）、`bStats/`、`EasyBot-2.4.0.jar.bak`（停用）。

| 插件 jar | 作用 | 数据/配置目录 |
| --- | --- | --- |
| `AuctionHouse-1.1.4.jar` | 玩家拍卖行 | `AuctionHouse/`（auctions.db/categories.yml） |
| `BedrockPlayerSupport-2.1.1-all.jar` | 基岩版表单 GUI | `BedrockPlayerSupport/` |
| `BedWars-0.2.44.jar` | 起床战争小游戏 | 首启生成 `BedWars/` |
| `BlueMap-5.29-paper.jar` | **3D 网页地图（现行）** | `BlueMap/`（core.conf/webserver.conf/maps/） |
| `BuildRate-1.0.0.jar` | 自研·建筑评分 | `BuildRate/` |
| `CMILib1.6.1.2.jar` | 前置依赖库 | `CMILib/` |
| `ClearLag-2.0.0-VER-1.18-26.3.jar` | ClearLag++ 实体清理与性能优化 | `ClearLag/` |
| `ClickMobs-paper-1.3.1.jar` | 点击收纳生物 | `ClickMobs/` |
| `CoreProtect-CE-24.1.jar` | 方块记录与回滚 | `CoreProtect/` |
| `DailySell-1.2.1.jar` | 自研·每日随机收购 | `DailySell/` |
| `EconomyShop-1.1.6.jar` | 玩家商店 | `EconomyShop/` |
| `EnchantShop-1.0.0.jar` | 自研·附魔商店 | `EnchantShop/` |
| `EssentialsX-2.22.1-dev+28-f8724e2.jar` | 基础指令整合与南瓜币经济 | `Essentials/`（config.yml/userdata/warps/） |
| `EvenMoreFish-2.5.3.jar` | 钓鱼玩法 | `EvenMoreFish/`（含 addons/） |
| `ExcellentEnchants-5.4.3.jar` | 自定义附魔 | `ExcellentEnchants/` |
| `EzSeasons-2.1.2.jar` | 季节更替 | `EzSeasons/` |
| `EzTax-2.0.0.jar` | 周持有税 | `EzTax/` |
| `FancyNpcs-2.12.1+382.jar` | NPC 系统 | 首启生成 `FancyNpcs/` |
| `FreeMinecraftModels.jar` | 自定义模型物品 | `FreeMinecraftModels/` |
| `GSit-3.8.0.jar` | 坐下 | `GSit/` |
| `Geyser-Spigot.jar` | Java ↔ 基岩互通 | `Geyser-Spigot/` |
| `LuckPerms-Bukkit-5.5.91.jar` | 权限管理 | `LuckPerms/`（config.yml + libs/） |
| `MoneyLedger-1.0.0.jar` | 自研·南瓜账本 | `MoneyLedger/` |
| `MythicMobs-5.14.0.jar` | 自定义怪物与技能 | `MythicMobs/` |
| `OfflineReward-1.0.0.jar` | 自研·离线奖励 | `OfflineReward/` |
| `OpenInv.jar` | 离线背包查看 | `OpenInv/` |
| `PlaceholderAPI-2.12.3.jar` | 占位符 API | `PlaceholderAPI/`（expansions/） |
| `Plan-5.9-dev-build-3786.jar` | 玩家/服务器数据分析 | `Plan/`（database.db/libraries/） |
| `PlanTop-1.0.0.jar` | 自研·在线时间排行 | `PlanTop/` |
| `PlayerHeadDrops-2.0.0.jar` | 玩家头颅掉落 | `PlayerHeadDrops/` |
| `ProtocolLib-Spigot.jar` | 协议库 | `ProtocolLib/` |
| `PumpkinFly-1.0.0.jar` | 自研·南瓜飞（付费 / 限时 / 永久飞行、限时体验奖励、全服与单人管理） | `PumpkinFly/`（config.yml + data/） |
| `PumpkinMail-1.0.0.jar` | 自研·南瓜邮箱 | `PumpkinMail/` |
| `Quests-5.3.2.jar` | 任务系统 | `Quests/` |
| `QuickMenu-1.0.0.jar` | 自研·快捷菜单 | `QuickMenu/`（config.yml） |
| `Residence6.0.4.2.jar` | 领地系统 | `Residence/` |
| `RideOnHead-1.1.3.jar` | 玩家骑乘 | `RideOnHead/` |
| `SimplePets.jar` | 宠物系统 | `SimplePets/`（含 `Addons/VaultAddon.jar`） |
| `SkinsRestorer.jar` | 皮肤管理 | `SkinsRestorer/` |
| `SpacePortal-1.0.0.jar` | 自研·传送阵 | `SpacePortal/` |
| `TAB_v6.2.1.jar` | TAB 列表 | `TAB/` |
| `UuidMigrate-1.0.0.jar` | 玩家数据迁移（UUID） | `UuidMigrate/` |
| `Vault.jar` | 经济前置 API | `Vault/` |
| `ViaBackwards-5.12.2.jar` | 老客户端连入新服 | `ViaBackwards/` |
| `ViaVersion-5.12.2.jar` | 跨版本协议兼容 | `ViaVersion/` |
| `customdeathmessages-1.7.0-modern.jar` | 自定义死亡信息 | `CustomDeathMessages/` |
| `floodgate-spigot.jar` | 基岩玩家身份识别 | `floodgate/`（key.pem） |
| `nightcore-2.16.6.jar` | 前置依赖库（NightExpress） | `nightcore/` |
| `opanel-paper-26.1-build-2.2.8.jar` | OPanel Web 管理面板 | `OPanel/` |
| `patpat-plugin-1.2.5.jar` | 摸头互动（PatPat） | `PatPatPlugin/`（config.json） |
| `vexbot-paper-3.1.0paper-mc26.3.jar` | 智能 PVP 机器人 | `VexBot/`（+ 顶层 `vexbot_kits/` 等） |
| `worldedit-bukkit-7.4.6.jar` | 创世神 WorldEdit | `WorldEdit/`（schematics/） |

> `YggdrasilOfficialProxy-2.3.0-paperclip.jar` 位于**运行目录根层**，由 `start.bat` 以 `-javaagent` 加载，不在 `plugins/`。

### 4.2 部分插件目录结构要点

- **`BlueMap/`**（现行地图）：`core.conf`、`webserver.conf`（port: 8100）、`maps/`、`storages/`。本地访问 `http://127.0.0.1:8100/`。
- **`Essentials/`**：`config.yml`（56KB）、`userdata/`、`warps/`、`items.json`、`trade.log`。
- **`LuckPerms/`**：`config.yml` + `luckperms-h2-v2.mv.db`（H2 数据库）。
- **`Plan/`**：`database.db`（7.9MB）、`public_html/`（网页）、`GeoLite2-Country.mmdb`（IP 归属）。
- **`Residence/`**：`config.yml`（见 `领地系统-Residence`）、`Save/`（领地数据）、`Backup/`。
- **`Quests/`**：`quests/`（任务定义）、`playerdata/`（玩家任务进度）。
- **`SkinsRestorer/`**：`cache/` `skins/` `players/` `cooldowns/`。

---

## 五、世界数据目录（world / world_nether / world_the_end）

### 5.1 主世界 `world/`

| 目录/文件                            | 作用                                                           | 规模参考          |
| ------------------------------------ | -------------------------------------------------------------- | ----------------- |
| `region/`                            | 区块文件 `r.<x>.<z>.mca`（区块主数据）                         | 548 个，约 810 MB |
| `playerdata/`                        | 玩家个人数据（背包、位置等，按 UUID）                          | 344 个，约 1.6 MB |
| `stats/`                             | 玩家统计（按 UUID）                                            | 174 个            |
| `advancements/`                      | 玩家进度（成就，按 UUID）                                      | 174 个            |
| `poi/`                               | 兴趣点（村庄、床等方块兴趣点）                                 | 324 个，约 16 MB  |
| `entities/`                          | 实体数据（按区块，1.18+）                                      | 501 个，约 101 MB |
| `data/`                              | 世界级数据（地图、结构等）                                     | 303 个            |
| `datapacks/`                         | 数据包：`bukkit`（内置）、`传送阵`、`自动开关生物破坏`（自研） | 16 个             |
| `generated/`                         | 生成缓存                                                       | 2 个              |
| `level.dat` / `level.dat_old`        | 世界主存档（含种子、时间、规则）及旧副本                       | 各约 1.8 KB       |
| `level<epoch>.dat`                   | 世界存档按时间戳备份                                           | 1 个              |
| `paper-world.yml` / `gale-world.yml` | 分世界覆盖配置                                                 | —                 |
| `session.lock` / `uid.dat`           | 会话锁 / 世界唯一 ID                                           | —                 |

### 5.2 下界 `world_nether/` / 末地 `world_the_end/`

- 结构同主世界但简化：各含 `DIM-1/`（下界）或 `DIM1/`（末地）子目录（内存放该维度的 region/poi 等），加顶层 `level.dat`、`level.dat_old`、`session.lock`、`uid.dat`、`paper-world.yml`、`gale-world.yml`。
- 规模小（各约 2 MB），主数据集中在主世界。

---

## 六、其余运行目录

| 目录                | 作用                                                                                                                 |
| ------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `libraries/`        | 服务端运行时依赖库（按 Maven 组织分组：`com`/`net`/`org`/`io`/`at`/`ca`/`cn`/`dev`/`javax`/`me`/`it` 等），约 120 MB |
| `versions/26.3/`    | 核心 jar 缓存：`leaf-26.3-18.jar`                                                                                           |
| `cache/`            | 客户端 jar 缓存：`mojang_26.3.jar`（供渲染/外置登录用）                                                               |
| `bluemap/`          | BlueMap 渲染输出：`logs/` + `web/`（`assets/`、`lang/`、`maps/world/`、`index.html`）——本服现行网页地图，保留        |
| `opanel/`           | OPanel 面板：`tasks.json`、`open-api.json`、`mcp-config.json`、`login-banner.png`、`launch-command.txt`、`.tmp/`     |
| `logs/`             | 服务器日志：`latest.log` + 每日 `YYYY-MM-DD-N.log.gz` 归档（约 300 份）                                              |
| `crash-reports/`    | 崩溃报告 `crash-2026-09-19_19.52.54-server.txt`                                                                      |
| `.git/`             | Git 版本库（约 840 MB，含全部提交历史与各版本快照）                                                                  |
| `.history/`         | 历史快照（`usercache_2026*.json`）                                                                                   |
| `uploaded_skins/`   | 上传皮肤暂存目录（当前为空）                                                                                         |
| `codeofconduct/`    | 遗留目录（当前为空）                                                                                                 |

---

## 七、排错与维护速记

- **改配置**：`server.properties` / `config/*.yml` 修改后需**完整重启**；插件配置 `/xx reload` 热重载（部分不生效需重启）。
- **日志**：报错先看 `logs/latest.log`；崩溃看 `crash-reports/` 与顶层 `hs_err_pid*.log`。
- **磁盘占用大头**：`world/region`（810MB）→ `.git`（840MB）→ `plugins/`（693MB）→ `libraries/`（120MB）→ `bluemap/`（118MB，现行地图数据，保留）。
- **已停用残留**：`plugins/EasyBot-2.4.0.jar.bak`（QQ 联动）与其 `EasyBot/` 数据目录；恢复联动需去掉 `.bak` 后缀并重启。
- **白名单**：`whitelist.json` 与其 `.backup` 同时维护，改白名单用 `/whitelist add/remove`。

---

更新记录：2026-11-09 更新——整篇参照目录由 `leaf-1.21.11` 改为 `leaf-26.3/leaf-26.3`；4.1 插件清单按实跑 51 个 jar 全部重列（删除 AdvZone / DragonManager / BedrockSkinRecorder / AntiLitematica / Chunky / DecentHolograms / Dynmap / ImageFrame / Lootr / leashmod / Svg-Spigot / sit / voicechat / BlueMap-Residence 等已移除条目，补入 FancyNpcs / EzSeasons / MythicMobs / GSit / VexBot / ViaBackwards 等）；顶层核心 jar、authlib-injector 版本、start.bat 参数、versions/cache 路径同步为 26.3 实际。
