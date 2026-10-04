# 16. 服务器配置文件补充（Leaf 运行目录全览）

本文档从 **生产备份目录 `F:\game\pc\MC\开服\服务器数据备份\leaf-1.21.11`**（即 Leaf 1.21.11 实际运行目录的备份）出发，给出完整的**目录树形图**，并逐文件、逐目录说明作用与当前取值。核心覆盖：顶层配置/数据文件（server.properties、bukkit.yml、spigot.yml 等）、`config/` 配置链（Leaf/Paper/Gale）、`plugins/` 插件区（插件 jar 与其配置/数据目录）、`world*` 世界数据，以及 libraries/versions/cache/bluemap/opanel/logs 等运行目录。原「配置文件补充」的详细取值说明（bukkit.yml 生成限制、spigot.yml 中文消息、commands.yml 别名、server.properties MOTD）一并保留于对应章节。

> 备份目录体量参考：总约 **3.5 GB**。其中 `.git`（版本历史）840 MB、`plugins/` 693 MB（约 2 万文件）、`world/` 940 MB、`libraries/` 120 MB、`bluemap/` 118 MB、顶层 jar 约 150 MB。

---

## 一、目录树全览（leaf-1.21.11）

```
leaf-1.21.11/                                  # 服务器运行目录备份（Leaf 1.21.11 生产）
├── .git/                        # Git 版本库（约 840 MB，含全部提交历史）
├── .history/                    # 用户缓存快照（usercache_2026*.json，备份历史）
├── bluemap/                     # BlueMap 3D 地图数据（jar 已停用，改 Dynmap；数据残留）
│   ├── logs/                    #   BlueMap 运行日志
│   └── web/                     #   渲染输出：assets/ lang/ maps/(world) index.html settings.json
├── cache/                       # 客户端 jar 缓存：mojang_1.21.11.jar（约 56 MB）
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
├── versions/                    # 核心 jar 缓存：leaf-1.21.11.jar / paper-1.21.11.jar
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
├── authlib-injector-1.2.7.jar   # 外置登录注入引擎（YggdrasilOfficialProxy 内置）
├── authlib-injector.log         # 外置登录注入日志
├── banned-ips.json              # 已封禁 IP 名单
├── banned-players.json          # 已封禁玩家名单
├── bukkit.yml                   # Bukkit 通用配置
├── commands.yml                 # 命令别名与覆盖
├── eula.txt                     # 最终用户许可协议（=true）
├── help.yml                     # Paper 帮助系统配置（默认空）
├── hs_err_pid6636.log / .mdmp   # JVM 崩溃日志与转储（排障参考）
├── leaf-1.21.11-174.jar         # 当前服务端核心（Leaf 1.21.11）
├── map-color-cache.dat          # 地图颜色缓存
├── ops.json                     # 管理员（OP）名单
├── paper-1.21.11-132.jar        # Paper 核心（对照/备用）
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

| 状态 | 文件 | 作用 |
|---|---|---|
| ✅ 核心 | `server.properties` | 服务器主配置（端口、模式、难度等） |
| ✅ 核心 | `bukkit.yml` | Bukkit 通用配置（区块生成、怪物生成限制等） |
| ✅ 核心 | `spigot.yml` | Spigot 性能配置（实体激活范围、tick 率等） |
| ✅ 核心 | `commands.yml` | 命令别名和覆盖 |
| ✅ 核心 | `permissions.yml` | 权限定义文件（空） |
| ✅ 核心 | `eula.txt` | 最终用户许可协议（必须为 `true`） |
| ✅ 核心 | `help.yml` | Paper 帮助系统配置（当前为默认空配置 `{}`） |
| ⚠️ 遗留 | `purpur.yml` | Purpur 配置 — **Leaf 不读取此文件**，迁移遗留 |
| 📋 数据 | `ops.json` | 服务器管理员（OP）名单 |
| 📋 数据 | `usercache.json` | 玩家用户名缓存（当前 1000 条上限） |
| 📋 数据 | `banned-players.json` | 已封禁玩家列表 |
| 📋 数据 | `banned-ips.json` | 已封禁 IP 列表 |
| 📋 数据 | `version_history.json` | 服务器版本升级记录 |
| 🖼️ 资源 | `server-icon.png` | 服务器图标（64×64，显示在客户端服务器列表） |

### 2.2 顶层运行文件补充（非配置文件）

| 文件 | 作用 |
|---|---|
| `leaf-1.21.11-174.jar` | **当前服务端核心**（Leaf 1.21.11，构建 174），启动脚本入口 |
| `paper-1.21.11-132.jar` | Paper 核心，同版本对照/备用（Leaf 为 Paper 分支，可互换） |
| `YggdrasilOfficialProxy-2.3.0-paperclip.jar` | 外置登录代理（javaagent），配合 `-javaagent` 加载 |
| `YggdrasilOfficialProxy.conf` | 外置登录代理配置（认证源等） |
| `authlib-injector-1.2.7.jar` | 外置登录注入引擎（被代理内置调用） |
| `authlib-injector.log` | 外置登录注入运行日志 |
| `start.bat` | 启动脚本（`java -Xmx4G -Xms1G -javaagent:... -jar leaf-1.21.11-174.jar nogui`） |
| `logs/`、`crash-reports/`、`hs_err_pid*.log/.mdmp` | 运行日志 / 崩溃报告 / JVM 崩溃转储（排障） |
| `map-color-cache.dat` | 地图颜色缓存（服务端自维护，可删，会自动重建） |
| `wepif.yml` | WorldEdit 权限接口配置 |
| `whitelist.json` / `.backup` | 白名单 / 白名单备份 |
| `.console_history` | 控制台命令历史 |
| `usercache.json` / `.history/*.json` | 玩家名缓存 / 历史快照 |
| `.gitignore` | Git 忽略规则（world、logs 等不入库） |

### 2.3 `bukkit.yml` 关键配置

```yaml
spawn-limits:
  monsters: 32        # 怪物上限
  animals: 8          # 动物上限
  water-animals: 4    # 水生动物上限
  ambient: 4          # 环境生物上限

ticks-per:
  animal-spawns: 600  # 动物生成频率
  monster-spawns: 1   # 怪物生成频率
  autosave: 6000      # 自动保存频率(刻)

chunk-gc:
  period-in-ticks: 600 # 区块垃圾回收周期

connection-throttle: 4000  # 连接节流(ms)，Geyser需要较高值
```

### 2.4 `spigot.yml` 消息配置（中文）

```yaml
messages:
  whitelist: "您未被加入白名单！请联系服务器管理员！"
  unknown-command: "未知命令。输入 \"/help\" 获取帮助。"
  server-full: "服务器已满！"
  outdated-client: "客户端过旧！请使用 {0}"
  outdated-server: "服务器过旧！当前版本为 {0}"
  restart: "服务器正在重启。"
```

### 2.5 `permissions.yml`

文件为空，所有权限由 **LuckPerms** 管理。

### 2.6 `commands.yml`

```yaml
command-block-overrides: []
ignore-vanilla-permissions: false
aliases:
  icanhasbukkit:
  - "version $1-"
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

| 片段 | 颜色代码 | 效果 |
|---|---|---|
| 南 / 瓜 / 生 / 存 | `§x§f§f§5§e§1§9` 等十六进制渐变 + `§l` | 橙→金→青→红→金 逐字渐变加粗标题 |
| ▏ | `§8` | 灰色竖分隔线 |
| 归墟纪·墟火 | `§x§f§f§5§e§1§9§l` | 紫色渐变加粗（服务器联动小说项目名） |
| ✦ | `§x§f§f§d§d§6§6` | 金粉色渐变装饰星 |
| 生存 | `§a` | 绿色 |
| 领地 | `§e` | 金色 |
| 养老 | `§d` | 淡紫色 |
| 共建 | `§b` | 青色 |
| · | `§8` | 灰色间隔点 |
| 欢迎加入 | `§7` | 灰色 |

**注意**：

- MOTD / 图标（图标保持原 `server-icon.png` 不变）均由后端 `server.properties` 直接控制，**本服未使用 Velocity 代理**，无需代理端配置。
- 十六进制渐变格式为 `§x§R§R§G§G§B§B`（每个通道重复两次），1.21 支持；修改后**需重启后端实例**才生效。

---

## 三、`config/` 目录（Leaf/Paper/Gale 配置链）

Leaf 继承 Paper → Gale 配置体系，首次启动生成。配置优先级：`leaf-global.yml` > `gale-global.yml` > `paper-global.yml`。

| 文件 | 作用 |
|---|---|
| `leaf-global.yml` | **Leaf 特有全局配置**（异步区块发送 / 异步生物生成 / 异步寻路等，见 `1-服务端核心与网络层/01-服务器核心-Leaf`） |
| `gale-global.yml` | Gale 全局配置（Leaf 上游） |
| `gale-world-defaults.yml` | Gale 分世界默认配置 |
| `paper-global.yml` | Paper 全局配置（继承自 Paper，如区块发送、实体激活等） |
| `paper-world-defaults.yml` | Paper 分世界默认配置 |

> 另在每个世界目录下有 `paper-world.yml`、`gale-world.yml`（分世界覆盖），见第五节。

---

## 四、`plugins/` 目录（插件区）

`plugins/` 顶层同时存在插件 **jar** 与同名**配置/数据目录**；`.paper-remapped/` 是 Paper 的**重映射缓存**（把插件 remap 到当前服务端版本，含 `extra-plugins/` `libraries/` `mappings/` `remap-classpath/` `unknown-origin/`）。`update/` 为插件更新暂存（空）。

> 备份目录里带 `.bak` 后缀的 jar 是**已停用/旧版备份**：`AntiLitematica-7.0.1.jar.bak`（反 Litematica，已关）、`bluemap-5.16-paper.jar.bak` 与 `BlueMap-Residence.jarr.bak`（BlueMap 3D 地图已停用，改 Dynmap）、`DailySell.jar.bak_20260930`（旧版备份）。

### 4.1 插件清单（jar / 作用 / 数据目录）

| 插件 jar | 作用 | 数据/配置目录 |
|---|---|---|
| `AdvZone.jar` | 自研·高级领地（AdvZone） | `AdvZone/`（config.yml） |
| `AntiLitematica-7.0.1.jar.bak` | 反 Litematica 打印机检测（已停用） | `AntiLitematica/`（config/data.db/violations.db） |
| `AuctionHouse-1.1.4.jar` | 拍卖行 | `AuctionHouse/`（auctions.db/categories.yml） |
| `BedrockPlayerSupport-2.1.1-all.jar` | 基岩版玩家支持（原生表单 GUI） | `BedrockPlayerSupport/` |
| `BedrockSkinRecorder-1.0.0.jar` | 自研·基岩皮肤记录（上线自动固化 Xbox 皮肤纹理） | `BedrockSkinRecorder/`（皮肤 PNG 输出） |
| `bluemap-5.16-paper.jar.bak` | BlueMap 3D 网页地图（已停用，改 Dynmap） | `BlueMap/`（core.conf/webserver.conf/maps/） |
| `BlueMap-Residence.jarr.bak` | BlueMap 领地桥接（停用） | `BlueMap_Residence/`（config.yml） |
| `Chunky-Bukkit-1.4.40.jar` | 区块预生成 | `Chunky/`（config.yml/tasks/） |
| `ClickMobs-paper-1.3.1.jar` | 点击收纳生物 | `ClickMobs/` |
| `CMILib1.5.9.9.jar` | 前置依赖库（CMILib） | `CMILib/`（Saves/Translations） |
| `CoreProtect-CE-24.0.jar` | 方块记录与回滚 | `CoreProtect/`（config.yml/language.yml） |
| `customdeathmessages-1.3.jar` | 自定义死亡信息 | `CustomDeathMessages/`（messages.yml） |
| `DailySell.jar` | 自研·每日随机收购 | `DailySell/`（daily.yml/data.yml） |
| `DecentHolograms-2.10.1.jar` | 全息投影 | `DecentHolograms/`（holograms/animations/） |
| `DragonManager-1.0.0.jar` | 自研·末影龙重生与破坏保护 | （根目录无独立目录） |
| `Dynmap-3.8-spigot.jar` | **Dynmap 2D 网页地图（现行）** | `dynmap/`（configuration.txt/worlds.txt/markers/） |
| `EasyBot-2.3.1.jar` | QQ 机器人联动 | `EasyBot/`（config.yml） |
| `EconomyShop-1.1.6.jar` | 经济商店 | `EconomyShop/`（data.db） |
| `EntityCount-1.1.0.jar` | 自研·实体计数占位符 | （无独立目录） |
| `EssentialsX-2.22.1-dev+25-cfb6f12.jar` | EssentialsX 基础指令整合 | `Essentials/`（config.yml/userdata/warps/） |
| `ExcellentEnchants-5.4.3.jar` | 自定义附魔 | `ExcellentEnchants/`（enchants/engine.yml） |
| `EzTax-1.2.0.jar` | 税收 | `EzTax/`（config.yml/stats.yml） |
| `floodgate-spigot.jar` | 基岩版统一注册（配合 Geyser） | `floodgate/`（config.yml/key.pem） |
| `freeminecraftmodels.jar` | 自定义模型物品 | `FreeMinecraftModels/`（models/recipes/resource_pack/） |
| `Geyser-Spigot.jar` | Java↔基岩互通 | `Geyser-Spigot/`（config.yml/packs/cache/） |
| `ImageFrame-2026.1.5.0.jar` | 图片展示 | `ImageFrame/`（data/players/upload/） |
| `leashmod-bukkit-1.3.0.jar` | 拴绳牵玩家 | `LeashablePlayers/`（config.yml） |
| `LootrPlugin-1.2.jar` | 战利品箱（Lootr） | `LootrPlugin/`（playerdata/） |
| `LuckPerms-Bukkit-5.5.81.jar` | 权限管理 | `LuckPerms/`（config.yml + H2 数据库） |
| `MoneyLedger.jar` | 自研·南瓜账本（南瓜币变动按类别记账，2026-10-04 部署） | `MoneyLedger/`（config.yml/ledger.db/export/） |
| `nightcore-2.16.6.jar` | 前置依赖库（NightExpress，ExcellentEnchants 等） | `nightcore/`（config.yml/userdata.yml/data.db） |
| `opanel-bukkit-1.21.9-build-2.0.1.jar` | OPanel Web 管理面板 | `OPanel/`（config.yml） |
| `OpenInv.jar` | 离线背包查看 | `OpenInv/`（profiles.db） |
| `patpat-plugin-1.2.5.jar` | 摸头互动（PatPat） | `PatPatPlugin/`（config.json） |
| `PlaceholderAPI-2.12.3.jar` | 占位符 API | `PlaceholderAPI/`（expansions/） |
| `Plan-5.8-build-3638.jar` | 玩家/服务器数据分析 | `Plan/`（database.db/public_html/） |
| `PlanTop-1.0.0.jar` | 自研·在线时间排行 | `PlanTop/`（config.yml） |
| `PosTracker-1.0.0.jar` | 玩家位置记录 | `PosTracker/`（config.yml/localization/） |
| `ProtocolLib.jar` | 协议库（网络数据包）详见 [09-协议库-ProtocolLib](../09-%E5%8D%8F%E8%AE%AE%E5%BA%93-ProtocolLib/ProtocolLib.md) | `ProtocolLib/`（config.yml） |
| `PumpkinMail.jar` | 自研·南瓜邮箱（活动奖励批量发放/玩家领取，2026-10-04 重部署） | `PumpkinMail/`（config.yml/mailbox.db/名单文件） |
| `Quests-3.16.1-430c34a.jar` | 任务系统 | `Quests/`（quests/playerdata/） |
| `QuickMenu-1.0.0.jar` | 自研·快捷菜单系统 | `QuickMenu/`（config.yml 84KB） |
| `Residence6.0.0.1.jar` | 领地系统 | `Residence/`（config.yml/Save/Backup/） |
| `RideOnHead-1.1.3.jar` | 玩家骑乘（RideOnHead） | `RideOnHead/`（config.yml） |
| `ride-players-1.0-1.21.jar` | 骑玩家（轻量替代） | `ride-players/`（config.yml） |
| `SimplePets.jar` | 宠物系统 | `SimplePets/`（storage.db/Pets/） |
| `sit-1.7.3.jar` | 坐下 | `Sit/`（config.yml） |
| `SkinsRestorer.jar` | 皮肤管理 | `SkinsRestorer/`（cache/skins/players/） |
| `SpacePortal-1.0.0.jar` | 自研·传送阵 | `SpacePortal/`（config.yml/portals.yml） |
| `Svg-Spigot-0.1.4.jar` | SVG 渲染支持库（供图片/模型使用） | （无独立目录） |
| `TAB v6.1.2.jar` | TAB 列表 | `TAB/`（config.yml/groups.yml/users.yml） |
| `UuidMigrate-1.0.0.jar` | 自研·玩家数据迁移（UUID） | `UuidMigrate/`（backups/） |
| `Vault.jar` | 经济前置 API | `Vault/`（config.yml/balances.yml） |
| `ViaVersion-5.12.0-SNAPSHOT.jar` | 跨版本协议兼容 | `ViaVersion/`（config.yml） |
| `voicechat-bukkit-2.6.21.jar` | Simple Voice Chat 语音 | `voicechat/`（voicechat-server.properties） |
| `worldedit-bukkit-7.4.2.jar` | 创世神（WorldEdit） | `WorldEdit/`（schematics/sessions/） |

> `Geyser-Spigot/` 与 `SimpleVoice-Geyser/` 是基岩版互通/语音链路；`bStats`（统计）、`spark`（性能分析）、`Vault`、`PlaceholderAPI`、`ProtocolLib`、`CMILib`、`nightcore` 等为无玩家交互的**基础库/工具**。

### 4.2 部分插件目录结构要点

- **`dynmap/`**（现行地图）：`configuration.txt`（webport=8123、image-format 等）、`worlds.txt`（各世界三视图开关）、`markers/`、`templates/`、`web/`（内置网页输出）。Residence 领地图层由 Residence 原生集成自动注册。
- **`BlueMap/`**（停用残留）：`core.conf`（accept-download=true 需首配）、`webserver.conf`、`maps/`、`storages/`。
- **`Essentials/`**：`config.yml`（56KB）、`userdata/`、`warps/`、`items.json`、`trade.log`。
- **`LuckPerms/`**：`config.yml` + `luckperms-h2-v2.mv.db`（H2 数据库）。
- **`Plan/`**：`database.db`（7.9MB）、`public_html/`（网页）、`GeoLite2-Country.mmdb`（IP 归属）。
- **`Residence/`**：`config.yml`（含 `DynMap: Use: true`，见 `02-领地系统-Residence`）、`Save/`（领地数据）、`Backup/`。
- **`Quests/`**：`quests/`（任务定义）、`playerdata/`（玩家任务进度）。
- **`SkinsRestorer/`**：`cache/` `skins/` `players/` `cooldowns/`。

---

## 五、世界数据目录（world / world_nether / world_the_end）

### 5.1 主世界 `world/`

| 目录/文件 | 作用 | 规模参考 |
|---|---|---|
| `region/` | 区块文件 `r.<x>.<z>.mca`（区块主数据） | 548 个，约 810 MB |
| `playerdata/` | 玩家个人数据（背包、位置等，按 UUID） | 344 个，约 1.6 MB |
| `stats/` | 玩家统计（按 UUID） | 174 个 |
| `advancements/` | 玩家进度（成就，按 UUID） | 174 个 |
| `poi/` | 兴趣点（村庄、床等方块兴趣点） | 324 个，约 16 MB |
| `entities/` | 实体数据（按区块，1.18+） | 501 个，约 101 MB |
| `data/` | 世界级数据（地图、结构等） | 303 个 |
| `datapacks/` | 数据包：`bukkit`（内置）、`传送阵`、`自动开关生物破坏`（自研） | 16 个 |
| `generated/` | 生成缓存 | 2 个 |
| `level.dat` / `level.dat_old` | 世界主存档（含种子、时间、规则）及旧副本 | 各约 1.8 KB |
| `level<epoch>.dat` | 世界存档按时间戳备份 | 1 个 |
| `paper-world.yml` / `gale-world.yml` | 分世界覆盖配置 | — |
| `session.lock` / `uid.dat` | 会话锁 / 世界唯一 ID | — |

### 5.2 下界 `world_nether/` / 末地 `world_the_end/`

- 结构同主世界但简化：各含 `DIM-1/`（下界）或 `DIM1/`（末地）子目录（内存放该维度的 region/poi 等），加顶层 `level.dat`、`level.dat_old`、`session.lock`、`uid.dat`、`paper-world.yml`、`gale-world.yml`。
- 规模小（各约 2 MB），主数据集中在主世界。

---

## 六、其余运行目录

| 目录 | 作用 |
|---|---|
| `libraries/` | 服务端运行时依赖库（按 Maven 组织分组：`com`/`net`/`org`/`io`/`at`/`ca`/`cn`/`dev`/`javax`/`me`/`it` 等），约 120 MB |
| `versions/1.21.11/` | 核心 jar 缓存：`leaf-1.21.11.jar` 与 `paper-1.21.11.jar` |
| `cache/` | 客户端 jar 缓存：`mojang_1.21.11.jar`（约 56 MB，供渲染/外置登录用） |
| `bluemap/` | BlueMap 渲染输出：`logs/` + `web/`（`assets/`、`lang/`、`maps/world/`、`index.html`）——jar 停用后残留，可清理 |
| `opanel/` | OPanel 面板：`tasks.json`、`open-api.json`、`mcp-config.json`、`login-banner.png`、`launch-command.txt`、`.tmp/` |
| `logs/` | 服务器日志：`latest.log` + 每日 `YYYY-MM-DD-N.log.gz` 归档（约 300 份） |
| `crash-reports/` | 崩溃报告 `crash-2026-09-19_19.52.54-server.txt` |
| `.git/` | Git 版本库（约 840 MB，含全部提交历史与各版本快照） |
| `.history/` | 历史快照（`usercache_2026*.json`） |
| `uploaded_skins/` | 上传皮肤暂存目录（当前为空） |
| `codeofconduct/` | 遗留目录（当前为空） |

---

## 七、排错与维护速记

- **改配置**：`server.properties` / `config/*.yml` 修改后需**完整重启**；插件配置 `/xx reload` 热重载（部分不生效需重启）。
- **日志**：报错先看 `logs/latest.log`；崩溃看 `crash-reports/` 与顶层 `hs_err_pid*.log`。
- **磁盘占用大头**：`world/region`（810MB）→ `.git`（840MB）→ `plugins/`（693MB）→ `libraries/`（120MB）→ `bluemap/`（118MB，可清）。
- **已停用残留**：`bluemap/`、`BlueMap/`、`BlueMap_Residence/`、`AntiLitematica/` 及其 `.bak` jar 属停用残留，确认不再需要可一并删除释放空间。
- **白名单**：`whitelist.json` 与其 `.backup` 同时维护，改白名单用 `/whitelist add/remove`。

---
更新记录：2026-10-04 更新——按运行服 `plugins\` 实况补全 4.1 插件清单，新增 `BedrockSkinRecorder-1.0.0.jar`、`MoneyLedger.jar`、`PumpkinMail.jar` 三个自研条目（连同原有 AdvZone/DailySell/DragonManager/EntityCount/PlanTop/QuickMenu/SpacePortal/UuidMigrate，自研 jar 全部在册）；其余未点名内容保留。
