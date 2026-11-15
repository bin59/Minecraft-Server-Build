# 🎃 南瓜国际服 · 服务器完整文档

> **南瓜国际服** — Minecraft Java / 基岩双端互通服务器的完整技术文档与运维手册
>
> 本文档是一份**从零复现这台服务器**的指南：涵盖服务端选型、组件清单、网络架构、端口规划、玩法与插件、玩家命令、日常运维与故障排查。技术底座沿用本仓库已有的「南瓜生存服」配置（Java 26.3 / Leaf 核心），对外以「南瓜国际服」品牌运营。

> ⚠️ **隐私说明**：本文档中所有敏感信息（公网 IP、Token、密码、密钥、AccessKey）均已替换为占位符（如 `<公网IP>`、`<随机32位字符串>`）。
> 部署时请**自行生成**并妥善保管，切勿将真实凭据提交到公开仓库。

## 目录

- [🎃 南瓜国际服 · 服务器完整文档](#-南瓜国际服--服务器完整文档)
  - [目录](#目录)
  - [一、服务器简介](#一服务器简介)
    - [技术选型一句话总结](#技术选型一句话总结)
  - [二、服务器速览](#二服务器速览)
  - [三、技术架构](#三技术架构)
  - [四、网络与端口总表](#四网络与端口总表)
  - [五、插件与玩法总览](#五插件与玩法总览)
  - [六、玩家命令手册](#六玩家命令手册)
    - [⭐ 先记这 6 个就够用了](#-先记这-6-个就够用了)
    - [📖 快捷菜单（首选，不用记命令）](#-快捷菜单首选不用记命令)
    - [🚀 传送](#-传送)
    - [🌀 空间传送阵（本服特色）](#-空间传送阵本服特色)
    - [🏠 领地系统（保护你的家）](#-领地系统保护你的家)
    - [💰 经济](#-经济)
      - [🪙 每日获取南瓜币的途径](#-每日获取南瓜币的途径)
    - [🎃 南瓜邮箱（活动奖励）](#-南瓜邮箱活动奖励)
    - [🎁 工具包（Kit）](#-工具包kit)
    - [🎨 皮肤](#-皮肤)
    - [💬 聊天与社交](#-聊天与社交)
    - [📱 基岩版玩家专属（免打命令）](#-基岩版玩家专属免打命令)
    - [🏅 不同身份能用什么（速览）](#-不同身份能用什么速览)
    - [❓ 常见问题](#-常见问题)
  - [七、命令速查卡](#七命令速查卡)
  - [八、管理与运维速览](#八管理与运维速览)
  - [九、故障排查](#九故障排查)
  - [十、安全基线](#十安全基线)
  - [十一、文档导航（详细分册）](#十一文档导航详细分册)
  - [十二、仓库文件目录树](#十二仓库文件目录树)
  - [十三、服务器运行目录与配置文件补充（Leaf 26.3）](#十三服务器运行目录与配置文件补充leaf-263)

---

## 一、服务器简介

**南瓜国际服**是一台面向国内外玩家的 Minecraft 生存服务器，核心目标：

| 目标             | 实现方式                                                     |
| ---------------- | ------------------------------------------------------------ |
| **双端互通**     | Java 版与基岩版（手机 / Win10 / Xbox / Switch / PS）同服游玩 |
| **低门槛入服**   | 外置登录（Yggdrasil）+ Floodgate，基岩版玩家免正版验证       |
| **跨版本兼容**   | ViaVersion 允许 1.8 ~ 最新的客户端直连                       |
| **群服互通**     | QQ 群 ↔ 游戏内聊天双向同步（EasyBot + NapCat，jar 当前停用） |
| **可视化运维**   | OPanel Web 面板 + spark 性能分析 + MCSM 进程管理             |
| **长期存档安全** | Residence 领地保护 + CoreProtect 方块审计与回滚              |

### 技术选型一句话总结

> **Leaf**（高性能 Paper 分支）作核心，**YggdrasilOfficialProxy + authlib-injector** 做外置登录，
> **Geyser + Floodgate** 打通基岩版，**ViaVersion** 兜底跨版本，
> 上挂 **Residence / CoreProtect / LuckPerms / Vault / SkinsRestorer** 等生存服标配插件，
> 外接 **EasyBot** 做 QQ 联动、**OPanel** 做 Web 管理。

---

## 二、服务器速览

| 项目            | 值                                                      |
| --------------- | ------------------------------------------------------- |
| 服务器名称      | 南瓜国际服                                              |
| MOTD            | `§e南瓜§f国际服`                                        |
| 服务端核心      | Leaf `26.3`（Paper 分支）                               |
| Minecraft 版本  | Java 26.3                                               |
| 运行模式        | 单端（可扩展为多服 Velocity 网络）                      |
| 正版验证        | `online-mode=true`（经外置登录代理）                    |
| Java 版端口     | **55551**（TCP）                                        |
| 基岩版端口      | **55551**（UDP，跟随 Java 端口）                        |
| Web 面板端口    | **3000**（TCP，建议仅内网）                             |
| 最大玩家数      | 50                                                      |
| 默认游戏模式    | 生存（survival）                                        |
| 难度            | 简单（easy）                                            |
| 视距 / 模拟距离 | 12 / 4 区块                                             |
| 启动内存        | 见 start.bat 的 `-Xms` / `-Xmx`（最大 12G）             |
| 服务端目录      | `F:\game\pc\MC\开服\服务器数据备份\leaf-26.3\leaf-26.3` |
| 文档更新日期    | 2026-11-14                                              |

> ℹ️ MOTD 此处为对外品牌口径；`server.properties` 的实测完整值与配色见[十三、13.2.7](#十三服务器运行目录与配置文件补充leaf-263)。

---

## 三、技术架构

```
                          ┌─────────────────────────────────────────────┐
                          │                 公网 / 内网                    │
                          └───────────────┬─────────────────┬─────────────┘
                                          │                 │
                          Java 版 (TCP 55551)  基岩版 (UDP 55551)
                                  │                 │
                          ┌───────▼──────┐   ┌──────▼────────┐
                          │   Leaf 核心   │   │  Geyser-Spigot │
                          │  (服务端进程)  │   │  + Floodgate    │
                          └───┬───┬───┬──┘   └───────┬────────┘
                              │   │   │               │
            ┌─────────────────┘   │   └────────────────┘
            │                     │
   YggdrasilOfficialProxy    ViaVersion（跨版本协议翻译）
   + authlib-injector        （1.8 ~ 最新客户端直连）
   （外置登录 / 皮肤认证）
            │
   ┌────────▼────────┐
   │  插件层（玩法）   │  Residence · CoreProtect · LuckPerms · Vault
   │                  │  SkinsRestorer · EssentialsX · SimplePets
   │                  │  FancyNpcs · CustomDeathMessages · BlueMap
   │                  │  QuickMenu(自研) · SpacePortal(自研) · MythicMobs
   └────────┬────────┘
            │
   ┌────────▼────────┐
   │  运维与联动层     │  OPanel(Web) · spark · MCSM · EasyBot(QQ)
   │                  │  Plan · TAB  · CMILib · bStats
   └─────────────────┘
```

- **服务端核心**：Leaf（Paper 分支，高性能、反作弊友好、修复大量卡顿）。
- **外置登录**：YggdrasilOfficialProxy 配合 authlib-injector 做字节码注入，实现外置登录与皮肤加载；基岩版经 Floodgate 免正版验证。
- **跨端互通**：Geyser 把基岩版协议翻译成 Java 版协议，Floodgate 负责基岩玩家身份识别（带前缀）。
- **跨版本兼容**：ViaVersion 让老版本客户端也能连入 26.3。
- **多服扩展（可选）**：通过 Velocity 代理把单服升级为多子服网络（详见[文档导航](#十一文档导航详细分册)）。

---

## 四、网络与端口总表

> 📖 完整端口清单（Java/基岩游戏端口、BlueMap 网页地图、Caddy 反代、各 Web 面板、QQ 机器人桥接、认证代理、RCON，含协议 / 绑定地址 / 公开状态）与同机、跨机部署网络拓扑，统一见分册：
> **[`1-服务端核心与网络层/端口与网络架构总览/README.md`](1-服务端核心与网络层/端口与网络架构总览/README.md)**

> 常用端口速记：游戏 `55551`（TCP Java / UDP 基岩）、BlueMap `8100`、OPanel `3000`、Caddy `8080/80`。同机部署时各端口互不冲突；跨机部署（如 Geyser 与核心分离、EasyBot 独立机器）同样参见上述分册。

---

## 五、插件与玩法总览

> 📌 各插件的官方现行文档链接见各自分册开头的「0. 版本与官方文档」小节。

| 分类                 | 插件 / 组件                               | 作用                                                                                                                                       |
| -------------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| 服务端核心           | Leaf 26.3                                 | 高性能服务端内核                                                                                                                           |
| 外置登录             | YggdrasilOfficialProxy + authlib-injector | 外置登录、皮肤认证                                                                                                                         |
| 跨端互通             | Geyser-Spigot + Floodgate                 | Java/基岩同服；基岩免正版                                                                                                                  |
| 跨版本               | ViaVersion                                | 1.8 ~ 最新客户端直连                                                                                                                       |
| 权限                 | LuckPerms                                 | 分组、继承、上下文权限管理                                                                                                                 |
| 经济                 | Vault + EconomyShop/AuctionHouse          | 统一经济 API、商店、拍卖行                                                                                                                 |
| 领地                 | Residence                                 | 圈地、Flags 权限、子区域                                                                                                                   |
| 方块审计             | CoreProtect                               | 记录 / 查询 / 回滚（SQLite 可转 MySQL）                                                                                                    |
| 皮肤                 | SkinsRestorer                             | 外置登录下皮肤加载                                                                                                                         |
| 指令整合             | EssentialsX                               | 传送 / 经济 / 管理 150+ 命令                                                                                                               |
| 死亡信息             | CustomDeathMessages                       | 整活死亡播报（音效 / 粒子 / 标题，扣费已关闭）                                                                                             |
| 宠物                 | SimplePets + Vault Addon                  | 跟随宠物、骑乘 / 帽子、经济联动                                                                                                            |
| 摸头互动             | PatPat（CurseForge / Modrinth）           | Shift+右键摸任意生物，需 Java 客户端装 mod，基岩不可用                                                                                     |
| NPC 系统             | FancyNpcs 2.12.1（Modrinth）              | 数据包 NPC：向导 / 交互动作 / 皮肤与装备，已部署 `FancyNpcs-2.12.1+382.jar`                                                                |
| 小游戏               | BedWars 0.2.44                            | 起床战争竞技场（22:33 重新加入实跑 `plugins/`）                                                                                            |
| 玩家骑乘             | RideOnHead                                | 空手右键骑头、潜行下车、叠罗汉                                                                                                             |
| 网页地图             | BlueMap 5.29                              | 3D 网页地图，本地 `http://127.0.0.1:8100/`                                                                                                 |
| 赛季 / 怪物          | EzSeasons 2.1.2 · MythicMobs 5.14.0       | **赛季重置调度**（本服 `season.enabled: false`，仅作 EzTax 硬依赖 API）、自定义怪物与技能 |
| 附魔 / 模型          | ExcellentEnchants · FreeMinecraftModels   | 额外附魔、自定义模型物品（FMM 手持粒子已关：`particleEffectsEnabled: false`，见分册 §10）                                                    |
| 玩法扩展             | EvenMoreFish · GSit · ClickMobs · OpenShulk | 钓鱼、坐下、点击收纳生物、玩家头颅掉落、背包内右键直开潜影盒                                                                              |
| 实体清理             | ClearLag++ 2.0.0                          | 掉落物清理、每区块实体上限、TPS/MSPT 监控                                                                                                  |
| 世界编辑             | WorldEdit 7.4.6                           | 选区、复制、schematic                                                                                                                      |
| PVP 机器人           | VexBot 3.1.0                              | 智能 PVP 机器人与战斗风格                                                                                                                  |
| 任务 / 税收          | Quests 5.3.2 · EzTax 2.0.0                | 每日任务、周持有税                                                                                                                         |
| 菜单（自研）         | QuickMenu                                 | 整合全部插件的快捷菜单（33 套菜单 / 279 个菜单项；r5 基岩端按钮带图标、两行说明，说明项与需要参数的项都改成原生表单按钮，含世界切换 worldmenu、新人指南、每日任务、南瓜飞入口与管理面板） |
| 邮箱（自研）         | PumpkinMail 1.1.0                        | 南瓜邮箱：活动奖励批量发放（管理）与玩家领取（`/mailbox`）；发放 / 领取 / 失败全量审计（`reward_log`），`/mailbox log` 查流水、`verify` 对账 |
| 传送阵（自研）       | SpacePortal                               | 地面法阵长途传送（本服特色）                                                                                                               |
| 账本（自研）         | MoneyLedger 1.1.0                        | 南瓜账本：Essentials 余额变动**事件同步落盘** + `trade.log` **幂等回填**双保险，金额精确、来源/类别可追溯；`/moneyledger verify` 对账、`backfill` 补记、`export` 导出 CSV |
| 每日收购（自研）     | DailySell                                 | 物品池 110 种；每天抽 8 种候选、每人自选 5 种出售换币（`/ds choose`，基岩端按序号），**900 币/日封顶（单人每日收益上限）**                 |
| 在线排行（自研）     | PlanTop                                   | 基于 Plan 的玩家在线时间排行榜                                                                                                             |
| 飞行（自研）         | PumpkinFly                                | 10 万买永久 / 100 币一分钟，仅飞行中扣秒，限自己领地与主城；限时体验奖励（天/时/分）、全服与单人飞行管理、快捷菜单领取与管理面板（`/pfly`） |
| 在线离线奖励（自研） | OfflineReward                             | 离线 15 币/小时；在线改**阶段发放**：间隔 5→10→15→之后每 30 分钟一档，奖励逐档递增（10…100 币），累计 3 小时领完 8 档、**全天最多 300 币**当天毕业，发到南瓜邮箱（PumpkinMail） |
| 基岩 GUI             | BedrockPlayerSupport                      | 基岩玩家免敲指令的表单界面                                                                                                                 |
| 数据展示             | Plan / TAB                                | 玩家行为分析、炫彩称号、坐标轨迹                                                                                                           |
| 运维                 | OPanel / spark / MCSM / EasyBot           | Web 面板、性能分析、进程管理、QQ 联动                                                                                                      |
| 依赖库               | CMILib / nightcore / ProtocolLib / bStats | 多插件共享底层库、协议库、匿名统计                                                                                                         |

> 注：`plugins/EasyBot-2.4.0.jar.bak` 为已停用备份，数据目录保留；恢复 QQ 联动需去掉 `.bak` 后缀并重启。
> 自研 PumpkinFly 源码与 jar 在 `自研/南瓜飞-PumpkinFly/`；实跑 `plugins/` 中已有同版 `PumpkinFly-1.0.0.jar`（与项目 jar 字节一致）。升级时停服替换 jar 并完整重启，不要用 `/reload`。

---

## 六、玩家命令手册

> 本服大部分功能可以**右键时钟**打开快捷菜单完成，命令是为了让你更顺手。
> 遇到不会的，先点菜单；菜单没有的，再看下面的命令。
> 本手册已按服务器**实际配置**逐条核对。

### ⭐ 先记这 6 个就够用了

| 命令              | 作用               |
| ----------------- | ------------------ |
| `/tpa 玩家名`     | 申请传送到对方身边 |
| `/tpahere 玩家名` | 让对方传送到你身边 |
| `/tpaccept`       | 接受传送请求       |

### 📖 快捷菜单（首选，不用记命令）

- **触发**：手持**指南针**右键（进服自动发放到快捷栏最后一格）
- 弄丢了也没关系，输入 `/qm` 直接打开菜单
- Java 版：弹出箱子界面，点图标即可
- 基岩版：自动弹出点按钮的表单
- 菜单包含：**传送 · 领地 · 空间传送阵 · 经济 · 工具包 · 皮肤 · 社交 · 服务器信息 · 语音 · 南瓜邮箱**

> 💡 菜单里需要填名字的操作（如"传送到谁"），点击后会把命令格式发到聊天框，自己补全参数即可。

### 🚀 传送

| 命令                    | 作用                     |
| ----------------------- | ------------------------ |
| `/tpa 玩家名`           | 申请传送到对方那         |
| `/tpahere 玩家名`       | 让对方传送过来           |
| `/tpaccept` / `/tpdeny` | 接受 / 拒绝传送请求      |
| `/spawn`                | 回到服务器出生点         |
| `/warp`                 | 查看公共传送点列表       |
| `/warp 名称`            | 传送到指定公共传送点     |
| `/near`                 | 查看附近有哪些玩家及距离 |

> ⚠️ 本服**没有**中文指令（如 `/家`、`/传送`），请使用上面的英文命令。

### 🌀 空间传送阵（本服特色）

服务器里有一种**地面法阵**，走进去自动充能，完成后传送到相连的法阵——这是长途移动最快的方式。

| 命令                      | 作用                                                   |
| ------------------------- | ------------------------------------------------------ |
| `/csz` 或 `/csz list`     | 查看所有传送阵（也可用 `/spportal`、`/spaceportal`；`/portal` 属于 Advanced Portals） |
| `/csz create 名字 [颜色]` | 在**脚下**创建一个传送阵                               |

**怎么用**

1. 走到阵法中央，站着别动
2. 自动充能约 3 秒（中途移动会取消）
3. 充能完成 → 传送到相连阵法，落地有特效

| 项目     | 说明                                                        |
| -------- | ----------------------------------------------------------- |
| 传送费用 | 每次消耗 **20 颗钻石**（背包里没有就传不了）                |
| 随行实体 | 坐骑和你的宠物可一起传（每个额外 5 颗钻石），别人的宠物不带 |
| 冷却     | 落地后 5 秒内不能再次触发                                   |
| 阵法命名 | 只能用中英文、数字、下划线，最多 16 个字符                  |
| 自建上限 | 服务器限制了每人可建数量，达到上限会提示                    |
| 连线     | 阵法之间的连接需要**管理**操作，建好后找管理连线            |

> 💡 目前已公开的阵法：**主城 ↔ 矿区**（双向）。新阵法建好后联系管理接入网络。

### 🏠 领地系统（保护你的家）

**圈地三步**：

1. 拿**木锄**（本服圈地工具；若没有可向管理申请）
2. 左键点一个角 → 右键点对角，选中一块区域
3. 输入 `/res create 领地名` 完成圈地

| 命令                               | 作用                       |
| ---------------------------------- | -------------------------- |
| `/res create 领地名`               | 创建领地                   |
| `/res remove 领地名`               | 删除自己的领地（不可恢复） |
| `/res list`                        | 查看自己的全部领地         |
| `/res info`                        | 查看**所在**领地的信息     |
| `/res tp 领地名`                   | 传送到自己的领地           |
| `/res tpset`                       | 把领地的传送点设在脚下     |
| `/res set 领地名 权限 true/false`  | 设置领地内所有访客的权限   |
| `/res pset 玩家名 权限 true/false` | 单独设置某个玩家的权限     |
| `/res subzone 父领地 子领地`       | 在领地内再划一块独立小区域 |
| `/res message 领地名 enter 欢迎语` | 设置进出领地的提示文字     |
| `/res unstuck`                     | 被领地边界卡住时脱困       |
| `/res ?`                           | 查看领地系统完整帮助       |

常用权限：`build`（建造）· `destroy`（破坏）· `use`（使用门/按钮）· `container`（开箱子）· `move`（移动）· `pvp`（PVP）· `tp`（传送）

> 💡 **小技巧**：手持**线**（STRING）右键方块/实体，可以直接查看它属于哪个领地。
> ⚠️ 建议不要关闭 `move`，否则访客会走一步被拉回一步（体验很差）；防盗用 `build`+`container` 就够了。

### 💰 经济

| 命令                 | 作用           |
| -------------------- | -------------- |
| `/bal` 或 `/balance` | 查看余额       |
| `/pay 玩家名 金额`   | 转账给其他玩家 |
| `/baltop`            | 查看财富排行榜 |

> `/sell`、`/sellall`、`/worth` 已命令级禁用（Essentials `disabled-commands`，2026-09-30），量产物品走玩家间交易（`/ah` 拍卖行、`/shop` 玩家商店）。

#### 🪙 每日获取南瓜币的途径

👉 完整汇总（每日任务 / 每日随机收购 / 每日工具包 / 离线奖励 / 在线奖励 / 南瓜邮箱活动奖励，含各途径数量、配置位置与部署状态）见分册：**[`3-玩法与玩家功能插件/经济系统/南瓜币获取方式.md`](3-玩法与玩家功能插件/经济系统/南瓜币获取方式.md)**

### 🎃 南瓜邮箱（活动奖励）

服务器发活动奖励 / 补偿时会放进**南瓜邮箱**，上线有 ActionBar 提示"有待领取奖励"：

| 命令                      | 作用                             |
| ------------------------- | -------------------------------- |
| `/mailbox`（或 `/pmail`） | 打开自己的邮箱，一键领取全部奖励 |

> 管理发放入口：快捷菜单 → 管理面板 → **南瓜邮箱管理**（`/mailbox admin` 发放面板；`/mailbox give <奖品ID> <名单>` 一键批量发放，名单支持 `@online` / `@whitelist` / `@file:路径` / 玩家名逗号分隔）。

### 🎁 工具包（Kit）

新玩家可领**新手包 `starter`**（一次性，每人限领一次）：石头工具一套 + 皮装备 + 基础物资：

| 命令           | 作用                                                                                            |
| -------------- | ----------------------------------------------------------------------------------------------- |
| `/kit`         | 查看可领的工具包列表                                                                            |
| `/kit starter` | 新手包（一次性）：石剑 + 石镐 + 石斧 + 石铲 + 皮帽/皮衣/皮裤/皮靴 + 面包×16 + 火把×16 + 原木×32 |
| `/kit daily`   | 每日包：面包×2 + 南瓜币×5，每 6 小时可领一次（一天最多 4 次）                                   |

> 💡 输入 `/kit` 可以看到每个包还剩多久。菜单入口：快捷菜单 → 工具包。

### 🎨 皮肤

| 命令                 | 作用                         |
| -------------------- | ---------------------------- |
| `/skins`             | 打开皮肤库浏览界面，点着选   |
| `/skin set 皮肤名`   | 使用指定皮肤（填正版玩家名） |
| `/skin url 图片链接` | 用图片链接当皮肤             |
| `/skin random`       | 随机换一个皮肤               |
| `/skin clear`        | 清除皮肤，恢复默认           |
| `/skin undo`         | 撤销上一次换肤               |
| `/skin update`       | 换肤后没生效时刷新一下       |

### 💬 聊天与社交

| 命令               | 作用                                               |
| ------------------ | -------------------------------------------------- |
| `/msg 玩家名 内容` | 私聊指定玩家                                       |
| `/r 内容`          | 快速回复上一条私聊                                 |
| `/ignore 玩家名`   | 屏蔽某人的聊天                                     |
| `/list`            | 查看当前在线玩家                                   |
| `/seen 玩家名`     | 查看对方最后上线时间                               |
| `/whois`           | 查看自己的信息（位置、生命、模式等）               |
| `/motd`            | 查看服务器公告                                     |
| `/rules`           | 查看服务器规则                                     |
| `/nick 昵称`       | 给自己改显示昵称（有权限者可用，`/nick off` 恢复） |

> 📌 **QQ 群绑定**：目前服务器**未开启**游戏内绑定（绑定功能已关闭），群服消息同步由管理统一配置，无需玩家操作。

### 📱 基岩版玩家专属（免打命令）

| 命令       | 作用                                          |
| ---------- | --------------------------------------------- |
| `/tpgui`   | 传送申请表单，选人发送                        |
| `/warpgui` | 公共传送点表单                                |
| `/msggui`  | 私信表单                                      |
| `/kitgui`  | 工具包表单                                    |
| `/paygui`  | 转账表单 ❌ 已关闭（form.money.enable=false） |

> 💡 基岩版收到传送请求时会**自动弹出**接受/拒绝表单；死亡重生后弹出"是否返回死亡点"的表单**已关闭**（BedrockPlayerSupport `form.back.enable=false`，2026-09-27）。

### 🏅 不同身份能用什么（速览）

| 分组      | 聊天前缀 | 说明             |
| --------- | -------- | ---------------- |
| `default` | `[新手]` | 玩家（默认身份） |
| `admin`   | `[管理]` | 管理员           |
| `owner`   | `[服主]` | 服主             |

| 能力             | 玩家 |
| ---------------- | :--: |
| 领地数量（玩家） |  3   |
| `/tpa` 传送      |  ✅  |
| `/skin` 换皮肤   |  ✅  |
| 语音聊天         |  ✅  |
| `/csz` 传送阵    |  ✅  |

> 具体能力由服主在权限系统中配置，以游戏内实际为准。需要更高权限请联系管理。

### ❓ 常见问题

| 问题                         | 解决方法                                                                    |
| ---------------------------- | --------------------------------------------------------------------------- |
| 命令没反应                   | 先看是否拼写正确；部分命令有权限或身份要求                                  |
| 传送被取消                   | 请求发出后对方需要 `/tpaccept`，且等待期间不能移动                          |
| 领地被别人破坏               | 用 `/res pset 玩家名 build false` 禁止对方建造，或找管理用 CoreProtect 回滚 |
| 在别人领地里走不动、总被拉回 | 该领地关闭了 `move` 权限，找领主或管理开放                                  |
| 找不到指南针                 | 输入 `/qm` 直接打开菜单即可，指南针只是个快捷方式                           |
| 传送阵传不了                 | 检查背包是否有 20 颗钻石；落地 5 秒内无法再次触发                           |
| 还是不会                     | 游戏里输入 `/help` 查看帮助，或找带 `[管理]` 前缀的玩家                     |

---

## 七、命令速查卡

本卡浓缩南瓜国际服最常用的玩家命令，按"想做什么—输入什么"一表对照，并简述圈地、语音与基岩版表单的快捷用法。

> 日常功能首选：手持**指南针**右键，打开菜单点就行。下面是最常用命令，一眼扫完。

| 想做什么           | 输入                                   |
| ------------------ | -------------------------------------- |
| 🚶 传送到朋友那    | `/tpa 玩家名`                          |
| 📣 叫朋友过来      | `/tpahere 玩家名`                      |
| ✅ 接受 / 拒绝传送 | `/tpaccept` / `/tpdeny`                |
| 🌍 回出生点        | `/spawn`                               |
| 💰 查余额 / 转账   | `/bal` / `/pay 玩家名 金额`            |
| 🎁 领工具包        | `/kit`（新手包 `starter`）             |
| 🎨 换皮肤          | `/skin set 皮肤名`（或 `/skins` 点选） |
| 💬 私聊 / 回复     | `/msg 玩家名 内容` / `/r 内容`         |
| 🎃 领活动奖励      | `/mailbox`（南瓜邮箱）                 |

**🏠 圈地**：拿**木锄** → 左键点角① → 右键点对角② → `/res create 领地名`
　然后：`/res list` 看领地 · `/res tp 领地名` 传送 · `/res pset 玩家名 build false` 禁别人建造

**🎙️ 语音**：按住 `Caps Lock` 说话 · `M` 临时静音 · `V` 调音量/开群聊

**📱 基岩版**：`/tpgui` 传送 · `/kitgui` 领包（传送请求会自动弹表单）

**ℹ️ 说明**：领地数量按身份不同（玩家 3 个；管理/服主不限）；命令没反应先检查拼写和身份权限。

---

## 八、管理与运维速览

> 以下为管理员/服主高频动作速览，详细配置与排错见[文档导航](#十一文档导航详细分册)。

| 场景         | 动作 / 命令                                                                           |
| ------------ | ------------------------------------------------------------------------------------- |
| 权限调整     | LuckPerms：`/lp user <玩家> parent add <组>` · `/lp group <组> permission set <节点>` |
| 方块回滚     | CoreProtect：`/co lookup <条件>` · `/co rollback <条件>`                              |
| 查看离线背包 | OpenInv：`/inv see <玩家>` · `/enderchest see <玩家>`                                 |
| 性能分析     | spark：`/spark profiler start` · `/spark tps`                                         |
| QQ 联动      | EasyBot 三层架构（jar 当前为 .bak 停用），跨机部署需配置 NapCat 转发                  |
| Web 管理     | OPanel（accessKey 鉴权，建议反代 + 仅内网）                                           |
| 进程管理     | MCSM 控制面板（Windows 部署、分布式节点、导入已有目录）                               |
| 数据迁移     | UuidMigrate：离线 UUID ↔ 在线 UUID 玩家数据迁移                                       |
| 多服网络     | Velocity 代理（见多服分册与 12 步迁移 Checklist）                                     |

---

## 九、故障排查

| 故障               | 排查方向                                                                                                 |
| ------------------ | -------------------------------------------------------------------------------------------------------- |
| 基岩版连不上       | 检查 55551 UDP 放行；基岩客户端地址填 `<IP>:55551`；Geyser `clone-remote-port` 与 Floodgate key.pem 一致 |
| 老版本客户端连不上 | 确认 ViaVersion 已加载且版本支持                                                                         |
| 外置登录失败       | 检查 YggdrasilOfficialProxy 与 authlib-injector 注入是否生效                                             |
| TPS 低 / 卡顿      | spark 采样；检查 region 体积（MCA Selector 瘦身）与实体数量                                              |
| 内存占用高         | 见「服务器内存相关」分册；调整启动内存参数                                                               |
| 玩家数据错乱       | UuidMigrate 迁移；检查 UUID 一致性                                                                       |
| 投影/打印机作弊    | 见「禁用影响平衡的插件功能」分册（方案记录；本服当前未部署检测插件）                                     |

---

## 十、安全基线

- 公网 IP、Token、密码、密钥、AccessKey **一律使用占位符**，禁止提交真实凭据到公开仓库。
- Web 面板（OPanel）与数据库端口**仅内网暴露**，公网访问必须经反代 + HTTPS + 鉴权。
- 服务器正版验证 `online-mode=true`（经外置登录代理），避免离线赝号。
- 定期备份 `world` 与插件数据（Residence / CoreProtect / LuckPerms 等）；回滚优先用 CoreProtect。
- 管理命令权限严格按 LuckPerms 分组授予，遵循最小权限原则。
- 影响平衡的插件功能（Litematica 打印机等）按分册方案禁用或检测。

---

## 十一、文档导航（详细分册）

本仓库文档按**功能域**归入 6 个分类目录，详细配置与深度教程见各分册：

| 分类目录                  | 说明                                                           | 代表分册                                                                                                                                                                                          |
| ------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1-服务端核心与网络层/** | 内核、外置登录、跨端互通、跨版本、代理与网络、配置补充、协议库 | Leaf · Yggdrasil · Geyser/Floodgate · ViaVersion · ViaBackwards · 端口与网络架构 · Velocity 多服 · ProtocolLib                                                                                    |
| **2-运维监控与面板/**     | 性能、统计、依赖库、Web/进程面板、QQ 联动                      | EasyBot · OPanel · spark · CMILib · bStats · MCSM                                                                                                                                                 |
| **3-玩法与玩家功能插件/** | 经济、领地、皮肤、玩家模型、附魔等玩法插件                     | Vault · Residence · SkinsRestorer · EssentialsX · SimplePets · FancyNpcs · 自定义死亡信息 · FreeMinecraftModels · ExcellentEnchants · GSit · ClickMobs · PatPat · EvenMoreFish · VexBot · BedWars · OpenShulk |
| **4-玩家信息与数据展示/** | 行为分析、TAB、称号、占位符、坐标轨迹                          | Plan · TAB · 炫彩多层称号 · PlaceholderAPI · PlanTop                                                                                                                                              |
| **5-服务器管理/**         | 权限、回滚、实体清理、管理命令、数据迁移                       | LuckPerms · CoreProtect · WorldEdit · OpenInv · ClearLag++ · BedrockPlayerSupport · UuidMigrate                                                                                                   |
| **6-工具与常见问题约束/** | 待选插件、FAQ、禁用项                                          | 常见问题排查 · 服务器内存 · region 瘦身 · MCA Selector · 禁用影响平衡方案                                                                                                                         |

> 本页为总导航；各插件官方文档链接见各分册开头的「0. 版本与官方文档」小节。

---

> ⚠️ 本文档依据服务器当前插件配置编写（2026-11-14 核对插件版本基线）。若服务器调整配置，以游戏内实际可用为准。
> 管理命令（封禁、回滚、修改世界等）不属于玩家权限范围，详细见各管理分册。

---

## 十二、仓库文件目录树

```
Minecraft-Server-Build/
├── README.md                          # 本文档：总导航
├── 服务器优化记录.md
├── TODO/                              # 待办、待选插件与历史规划
│   ├── TODO.md
│   ├── 各种插件（待选）.md
│   ├── 下次更新26.2版本.md             # 历史升级规划（已升级到 26.3）
│   └── 刀剑神域.md
│
├── 1-服务端核心与网络层/
│   ├── 服务器核心-Leaf/             Leaf服务端核心.md
│   ├── 外置登录代理-YggdrasilOfficialProxy/   YggdrasilOfficialProxy.md
│   ├── Java-Bedrock互通层-Geyser-Floodgate/   01-Geyser / 02-Floodgate / 03-皮肤排查 / README / NetherNet迁移准备
│   ├── 跨版本协议兼容-ViaVersion/   ViaVersion.md
│   ├── 端口与网络架构总览/         README.md
│   ├── 服务器配置文件补充/         （README 已并入本页第十三章）
│   ├── 服务器资源包-BGM/           部署与配置说明.md + BGM资源包源文件 / 循环BGM数据包
│   ├── 协议库-ProtocolLib/         ProtocolLib.md
│   ├── 向下兼容-ViaBackwards/      README.md
│   └── 多服Velocity +Multiverse/      README.md（导航）+ 迁移方案 / 实跑基准 / 游戏内传送 / 门户分册
│
├── 2-运维监控与面板/
│   ├── QQ机器人联动-EasyBot/       EasyBot.md（jar 当前为 .bak 停用）
│   ├── Web管理面板-OPanel/         OPanel.md
│   ├── 性能分析-spark/             spark.md
│   ├── 核心依赖库-CMILib/          CMILib.md
│   ├── 统计系统-bStats/            bStats.md
│   └── MCSM控制面板/               MCSM控制面板.md / 链接到已有文件夹.md
│
├── 3-玩法与玩家功能插件/
│   ├── 经济系统/                   Vault.md + 经济插件/ +  工具/
│   ├── 领地系统-Residence/         领地系统-Residence.md + 配置文件/
│   ├── 皮肤管理-SkinsRestorer/     SkinsRestorer.md
│   ├── 网页世界地图/               BlueMap.md
│   ├── EssentialsX多功能指令整合/  EssentialsX多功能指令整合（功能说明与完整配置）.md
│   ├── 自定义死亡信息/             自定义死亡信息customdeathmessages.md
│   ├── 宠物系统/                   宠物系统simplepets.md / SimplePets-Vault-Addon经济联动.md / 汉化配置/
│   ├── RideOnHead玩家骑乘/         RideOnHead玩家骑乘.md
│   ├── 自定义模型-Free Minecraft Models/  Free Minecraft Models.md / 免费模型包清单.md / 资源包分发-ResourcePackManager/
│   ├── 自定义附魔-ExcellentEnchants/     ExcellentEnchants自定义附魔.md
│   ├── 坐下插件-GSit/              GSit.md
│   ├── 点击收纳生物-ClickMobs/    ClickMobs.md
│   ├── 快捷潜影盒-OpenShulk/      OpenShulk.md（背包直开潜影盒 + 部署/限制）
│   ├── 玩家头颅掉落-PlayerHeadDrops/     PlayerHeadDrops.md
│   ├── pvp机器人/                  VexBot-智能pvp机器人.md + 官方文档汉化/
│   ├── EvenMoreFish-更多鱼/        README.md
│   ├── FancyNpcs/                     README.md（FancyNpcs 完整说明 + 皮肤贴图素材）
│   ├── 摸摸头-PatPat/                 README.md（PatPat 完整说明 + patpat.webp）
│   └── 尖叫起床-BedWars/              README.md + 官方文档汉化/（12 篇）
│
├── 4-玩家信息与数据展示/
│   ├── 01-玩家信息收集与展示.md
│   ├── plan-服务器、玩家数据分析工具/   01-Plan使用文档.md
│   ├── 03-TAB信息展示.md
│   ├── 04-PlaceholderAPI占位符.md
│   ├── 05-LuckPerms炫彩称号.md
│   ├── 07-在线时间排行榜（PlanTop）.md
│   ├── 09-头顶名牌显示在线时长.md
│   └── 已有称号.md
│
├── 5-服务器管理/
│   ├── 权限管理系统-LuckPerms/    LuckPerms.md / 权限组设计方案.md / 权限节点速查.md
│   ├── 常用管理命令速查/          README.md
│   ├── 方块记录与回滚-CoreProtect/ CoreProtect.md / 一些操作.md / 数据库过大 / 网页端面板部署
│   ├── 离线背包查看-OpenInv/      OpenInv.md
│   ├── BedrockPlayerSupport基岩版GUI表单界面/  BedrockPlayerSupport基岩版GUI表单界面.md
│   ├── 创世神WorldEdit/           创世神WorldEdit.md
│   ├── 玩家数据迁移-UuidMigrate/  UuidMigrate-玩家数据迁移.md
│   └── ClearLag++-清理与优化/     README.md
│
├── 6-工具与常见问题约束/
│   ├── 服务器优化/                性能优化 / RCON加固 / 1.md
│   ├── 常见问题/                  常见问题排查 / UUID踩坑 / 发消息闪退 / 服务器内存相关
│   ├── 禁用影响平衡的插件功能/    1.禁用影响平衡的插件功能.md + AntiLitematica 资料（历史方案）
│   ├── 云数据库备份到本地/           增量备份技术文档 + logs/
│   └── 数据转换/                     使用文档.md
│
└── 自研/                             # 自研插件：文档 + 源码 + 构建产物（见 自研/README.md）
    ├── README.md                     自研插件总览（清单 / 构建 / 部署）
    ├── lib/                          共享编译依赖 jar
    ├── 传送阵/                       SpacePortal 使用说明
    ├── 传送阵-SpacePortal/           SpacePortal 源码 + jar
    ├── 南瓜账本-MoneyLedger/         MoneyLedger 1.1.0（全量流水 + trade.log 回填对账）
    ├── 南瓜邮箱-PumpkinMail/         PumpkinMail
    ├── 南瓜飞-PumpkinFly/          PumpkinFly（付费限时/永久飞行，含 build.py）
    ├── 在线时间排行-PlanTop/         PlanTop
    ├── 建筑评分-BuildRate/           BuildRate
    ├── 快捷菜单系统-QuickMenu/       QuickMenu（主文档 + 与通用菜单对比）
    ├── 每日随机收购-DailySell/       DailySell（使用说明 + config.yml）
    ├── 在线离线奖励-OfflineReward/       OfflineReward（离线 15 币/小时；在线 8 档递增，3 小时封顶 300 币毕业）
    ├── 附魔商店-EnchantShop/         EnchantShop
    └── disabled/                     停用项目
```

> 目录树按 2026-11-09 实际存在的目录重列；已移除插件的文档归档在 `.workbuddy/backup_文档对齐_20261109/removed/`。

---

## 十三、服务器运行目录与配置文件补充（Leaf 26.3）

> 本章原为 `1-服务端核心与网络层/服务器配置文件补充/README.md`，2026-11-14 整合进本页，原分册已删除。
> 本章描述的是 **Leaf 26.3 服务端运行目录**（`F:\game\pc\MC\开服\服务器数据备份\leaf-26.3\leaf-26.3`）的实际文件结构；第十二章的目录树是**本仓库文档目录树**，两者不是同一棵树。
> 各插件官方链接见各分册开头的「0. 版本与官方文档」小节；本章 13.4 是运行目录 jar 与数据目录的对应关系，两者定位不同。
> 13.2.7 的 MOTD 为 `server.properties` 实测值（南瓜生存服 ▏ 归墟纪·墟火）；第二章「服务器速览」表中的 MOTD 为对外品牌口径，以 13.2.7 实测值为准。

本文档从 **26.3 运行目录 `F:\game\pc\MC\开服\服务器数据备份\leaf-26.3\leaf-26.3`**（Leaf 26.3 实际运行目录的备份）出发，给出完整的**目录树形图**，并逐文件、逐目录说明作用与当前取值。核心覆盖：顶层配置/数据文件（server.properties、bukkit.yml、spigot.yml 等）、`config/` 配置链（Leaf/Paper/Gale）、`plugins/` 插件区（插件 jar 与其配置/数据目录）、`world*` 世界数据，以及 libraries/versions/cache/bluemap/opanel/logs 等运行目录。原「配置文件补充」的详细取值说明（bukkit.yml 生成限制、spigot.yml 中文消息、commands.yml 别名、server.properties MOTD）一并保留于对应章节。

> 备份目录体量参考：总约 **3.5 GB**。其中 `.git`（版本历史）840 MB、`plugins/` 693 MB（约 2 万文件）、`world/` 940 MB、`libraries/` 120 MB、`bluemap/` 118 MB、顶层 jar 约 150 MB。

### 13.1 目录树全览（leaf-26.3）

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
├── wepif.yml                    # WorldEdit 旧 WEPIF 权限模板（7.4 权限走 LuckPerms）
├── whitelist.json / .backup     # 白名单
└── YggdrasilOfficialProxy-2.3.0-paperclip.jar / .conf   # 外置登录代理 jar 与配置
```

> 树中 `.../` 表示有子目录省略；`plugins/`、`world*` 内部结构见第四、五节。

---

### 13.2 顶层配置与数据文件

#### 13.2.1 配置文件总览

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

#### 13.2.2 顶层运行文件补充（非配置文件）

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
| `wepif.yml`                                        | WorldEdit 旧 WEPIF 权限接口模板（默认示例内容，7.4 实际权限在 LuckPerms 配 `worldedit.*`，勿在此授权） |
| `whitelist.json` / `.backup`                       | 白名单 / 白名单备份                                                             |
| `.console_history`                                 | 控制台命令历史                                                                  |
| `usercache.json` / `.history/*.json`               | 玩家名缓存 / 历史快照                                                           |
| `.gitignore`                                       | Git 忽略规则（world、logs 等不入库）                                            |

#### 13.2.3 `bukkit.yml` 关键配置

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

#### 13.2.4 `spigot.yml` 消息配置（中文）

```yaml
messages:
  whitelist: '您未被加入白名单！请联系服务器管理员！'
  unknown-command: '未知命令。输入 "/help" 获取帮助。'
  server-full: '服务器已满！'
  outdated-client: '客户端过旧！请使用 {0}'
  outdated-server: '服务器过旧！当前版本为 {0}'
  restart: '服务器正在重启。'
```

#### 13.2.5 `permissions.yml`

文件为空，所有权限由 **LuckPerms** 管理。

#### 13.2.6 `commands.yml`

```yaml
command-block-overrides: []
ignore-vanilla-permissions: false
aliases:
  icanhasbukkit:
    - 'version $1-'
```

#### 13.2.7 `server.properties` MOTD 配置

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

### 13.3 `config/` 目录（Leaf/Paper/Gale 配置链）

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

### 13.4 `plugins/` 目录（插件区）

`plugins/` 顶层同时存在插件 **jar** 与同名**配置/数据目录**；`.paper-remapped/` 是 Paper 的**重映射缓存**（把插件 remap 到当前服务端版本，含 `extra-plugins/` `libraries/` `mappings/` `remap-classpath/` `unknown-origin/`）。`update/` 为插件更新暂存（空）。

> 实跑 `plugins/` 中带 `.bak` 后缀的 jar 只有 `EasyBot-2.4.0.jar.bak`（QQ 联动，当前停用，数据目录 `EasyBot/` 保留）。其余 51 个顶层 jar 均为启用状态。

#### 13.4.1 插件清单（jar / 作用 / 数据目录）

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
| `DailySell-1.2.4.jar` | 自研·每日随机收购（8 选 5，单人每日上限 900 币） | `DailySell/` |
| `EconomyShop-1.1.6.jar` | 玩家商店 | `EconomyShop/` |
| `EnchantShop-1.0.0.jar` | 自研·附魔商店 | `EnchantShop/` |
| `EssentialsX-2.22.1-dev+28-f8724e2.jar` | 基础指令整合与南瓜币经济 | `Essentials/`（config.yml/userdata/warps/） |
| `EvenMoreFish-2.5.3.jar` | 钓鱼玩法 | `EvenMoreFish/`（含 addons/） |
| `ExcellentEnchants-5.4.3.jar` | 自定义附魔 | `ExcellentEnchants/` |
| `EzSeasons-2.1.2.jar` | 赛季重置调度（本服 `season.enabled: false`，仅作 EzTax 硬依赖 API） | `EzSeasons/` |
| `EzTax-2.0.0.jar` | 周持有税 | `EzTax/` |
| `FancyNpcs-2.12.1+382.jar` | NPC 系统 | 首启生成 `FancyNpcs/` |
| `FreeMinecraftModels.jar` | 自定义模型物品 | `FreeMinecraftModels/` |
| `GSit-3.8.0.jar` | 坐下 | `GSit/` |
| `Geyser-Spigot.jar` | Java ↔ 基岩互通 | `Geyser-Spigot/` |
| `LuckPerms-Bukkit-5.5.91.jar` | 权限管理 | `LuckPerms/`（config.yml + libs/） |
| `MoneyLedger-1.1.0.jar` | 自研·南瓜账本（1.1.0 事件同步落盘 + `trade.log` 幂等回填 + `/moneyledger verify` 对账；首次启动自动迁移 `ledger.db` 并补齐历史漏记） | `MoneyLedger/`（`ledger.db`、`config.yml`） |
| `MythicMobs-5.14.0.jar` | 自定义怪物与技能 | `MythicMobs/` |
| `OfflineReward-1.1.3.jar` | 自研·在线离线奖励（离线 15 币/小时；在线阶段发放：8 档递增 10→100 币、3 小时发完，全天封顶 300 币毕业；阶段表全在 config，`max-per-day: 300` 硬封顶；`mode: cycle` 可回退 25 币/小时循环） | `OfflineReward/` |
| `OpenInv.jar` | 离线背包查看 | `OpenInv/` |
| `PlaceholderAPI-2.12.3.jar` | 占位符 API | `PlaceholderAPI/`（expansions/） |
| `Plan-5.9-dev-build-3786.jar` | 玩家/服务器数据分析 | `Plan/`（database.db/libraries/） |
| `PlanTop-1.0.0.jar` | 自研·在线时间排行 | `PlanTop/` |
| `PlayerHeadDrops-2.0.0.jar` | 玩家头颅掉落 | `PlayerHeadDrops/` |
| `ProtocolLib-Spigot.jar` | 协议库 | `ProtocolLib/` |
| `PumpkinFly-1.0.0.jar` | 自研·南瓜飞（付费 / 限时 / 永久飞行、限时体验奖励、全服与单人管理） | `PumpkinFly/`（config.yml + data/） |
| `PumpkinMail-1.1.0.jar` | 自研·南瓜邮箱（1.1.0 全量审计：`reward_log` 表；旧库自动迁移 + 历史回填） | `PumpkinMail/` |
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

#### 13.4.2 部分插件目录结构要点

- **`BlueMap/`**（现行地图）：`core.conf`、`webserver.conf`（port: 8100）、`maps/`、`storages/`。本地访问 `http://127.0.0.1:8100/`。
- **`Essentials/`**：`config.yml`（56KB）、`userdata/`、`warps/`、`items.json`、`trade.log`。
- **`LuckPerms/`**：`config.yml` + `luckperms-h2-v2.mv.db`（H2 数据库）。
- **`Plan/`**：`database.db`（7.9MB）、`public_html/`（网页）、`GeoLite2-Country.mmdb`（IP 归属）。
- **`Residence/`**：`config.yml`（见 `领地系统-Residence`）、`Save/`（领地数据）、`Backup/`。
- **`Quests/`**：`quests/`（任务定义）、`playerdata/`（玩家任务进度）。
- **`SkinsRestorer/`**：`cache/` `skins/` `players/` `cooldowns/`。

---

### 13.5 世界数据目录（world / world_nether / world_the_end）

#### 13.5.1 主世界 `world/`

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

#### 13.5.2 下界 `world_nether/` / 末地 `world_the_end/`

- 结构同主世界但简化：各含 `DIM-1/`（下界）或 `DIM1/`（末地）子目录（内存放该维度的 region/poi 等），加顶层 `level.dat`、`level.dat_old`、`session.lock`、`uid.dat`、`paper-world.yml`、`gale-world.yml`。
- 规模小（各约 2 MB），主数据集中在主世界。

---

### 13.6 其余运行目录

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

### 13.7 排错与维护速记

- **改配置**：`server.properties` / `config/*.yml` 修改后需**完整重启**；插件配置 `/xx reload` 热重载（部分不生效需重启）。
- **日志**：报错先看 `logs/latest.log`；崩溃看 `crash-reports/` 与顶层 `hs_err_pid*.log`。
- **磁盘占用大头**：`world/region`（810MB）→ `.git`（840MB）→ `plugins/`（693MB）→ `libraries/`（120MB）→ `bluemap/`（118MB，现行地图数据，保留）。
- **已停用残留**：`plugins/EasyBot-2.4.0.jar.bak`（QQ 联动）与其 `EasyBot/` 数据目录；恢复联动需去掉 `.bak` 后缀并重启。
- **白名单**：`whitelist.json` 与其 `.backup` 同时维护，改白名单用 `/whitelist add/remove`。

---

更新记录：2026-11-09 更新——整篇参照目录由 `leaf-1.21.11` 改为 `leaf-26.3/leaf-26.3`；4.1 插件清单按实跑 51 个 jar 全部重列（删除 AdvZone / DragonManager / BedrockSkinRecorder / AntiLitematica / Chunky / DecentHolograms / Dynmap / ImageFrame / Lootr / leashmod / Svg-Spigot / sit / voicechat / BlueMap-Residence 等已移除条目，补入 FancyNpcs / EzSeasons / MythicMobs / GSit / VexBot / ViaBackwards 等）；顶层核心 jar、authlib-injector 版本、start.bat 参数、versions/cache 路径同步为 26.3 实际。

> **2026-11-14 整合**：本分册已并入仓库根 `README.md` 第十三章，原 `1-服务端核心与网络层/服务器配置文件补充/README.md` 删除；正文内容未改写，仅标题层级降级并重编号为 13.x。
