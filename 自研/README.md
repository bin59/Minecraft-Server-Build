# 自研插件

本目录是南瓜服自研插件的**唯一一套源码与构建产物**：每个插件一个目录，源码在 `src/main/java/`，配置在 `src/main/resources/`，可直接部署的 jar 在 `target/`（PumpkinFly、PumpkinMail、DailySell、OfflineReward 另在项目根目录存一份）。**例外**：`收纳记录-ClickMobsLogger/` 用 `src/` + `resources/`，无 `pom.xml` / `build.py` / `target/`，重建的 jar 按其 §7 落在自己的插件目录内；**截至 2026-11-19 该插件的成品 jar 不在仓库中（仅存源码与说明文档），且从未部署到实服**。

除 ClickMobsLogger 为 `1.21` 外，所有插件的 `plugin.yml` 中 `api-version` 均为 `26.3`，按当前服务端实跑环境编写与编译。

**文档约定**：每个插件目录根下都有一份说明文档（多数命名为 `使用说明.md`，个别为项目名命名），统一覆盖「定位—命令—配置—数据结构—构建—运维—FAQ—版本沿革」；仓库内所有插件的说明文档已于 2026-11-19 补齐至 100%（清单见下表"说明文档"列）。文档结论以**当时的源码与产物核对结果**为准，改动插件后请同步更新对应文档的版本沿革一节。

---

## 一、插件清单

| 目录                          | 产物 jar                                         | 功能                                                                                                                                                                                                                                                                     | 运行时依赖                                            | 说明文档                                                                                         |
| ----------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `传送阵-SpacePortal/`         | `SpacePortal-1.0.0.jar`                          | 空间传送阵：建阵 / 双向连接 / 踩阵充能传送 / GUI / 随行宠物；命令 `/csz`、`/spportal`，不占 `/portal`                                                                                                                                                                    | Geyser + Floodgate（互通服）                          | `传送阵-SpacePortal/空间传送阵使用说明.md`                                                       |
| `南瓜账本-MoneyLedger/`       | `MoneyLedger-1.1.0.jar`                          | **全量准确流水**：余额变动事件同步落盘（不再走内存队列）+ Essentials `trade.log` 幂等回填补齐漏记；BigDecimal 精确金额、真实 UUID、来源/类别修正；`/moneyledger backfill` / `verify` / `stats`，导出 CSV                                                                 | EssentialsX（`UserBalanceUpdateEvent` + `trade.log`） | `使用说明.md`、`config.yml`（回填开关/周期/起始日）                                              |
| `南瓜邮箱-PumpkinMail/`       | `PumpkinMail-1.1.1.jar`（根目录 / `target/` / `lib/` 三份 SHA256 一致，2026-11-19 已同步） | 活动奖励批量发放与玩家领取（`/mailbox`）；**1.1.0 起全量审计**：发放/去重/领取/失败写入 `reward_log`，与发放同事务提交，旧库自动迁移并幂等回填历史，`/mailbox log` / `verify` 可查可对账；**1.1.1 领取链路原子化**：`tryClaim` 先占位再发奖、失败回滚（新增 `CLAIM_ROLLBACK` 事件），堵住缓存重复点击重复得币，并加待领数量缓存 | SQLite（sqlite-jdbc 由 Leaf 服务端 libraries 提供）   | `使用说明.md`（含 1.1.1 细节与版本修正记录）、`config.yml`、`plugin.yml`                          |
| `南瓜飞-PumpkinFly/`          | `PumpkinFly-1.0.0.jar`                           | 付费 / 限时 / 永久飞行，仅飞行中扣秒，限自己领地与主城；限时体验奖励（天/时/分）、全局与单人飞行管理；命令 `/pfly`（别名 `pf` / `pumpkinfly`）                                                                                                                              | **depend**: Vault；**softdepend**: Residence、Essentials、QuickMenu、PlaceholderAPI | `使用说明.md`                                                                                    |
| `在线时间排行-PlanTop/`       | `PlanTop-1.0.0.jar`（**只在 `target/`**，根目录不放；已 shade 内置 MySQL 驱动） | 基于 Plan 数据的在线时间排行榜：异步聚合 `plan_sessions`、5 分钟刷新、Top 30 内存缓存，提供 `%ptop_<N>_name/displayname/time%`、`%ptop_updated%`、`%ptop_error%`；**无命令、无权限节点、无 reload** | PlaceholderAPI（softdepend，未装则不注册占位符）、Plan 的 MySQL | `PlanTop.md`（含占位符表与状态说明）                                                              |
| `建筑评分-BuildRate/`         | `BuildRate-1.0.0.jar`（在 `target/`）               | 建筑评分与榜单：**三态活动**（0 待开始 / 1 进行中 / 2 已截止，`create` 写入 0、`start` 置 1、`stop` 置 2 且自动结算），去最高最低分均分，14 个子命令，箱子面板全操作，Java/基岩双端                                          | **softdepend**: Essentials、PumpkinMail；SQLite 驱动**未打进 jar**，由服务端 `libraries/` 提供 | `使用说明.md`（含状态机与权限矩阵）                                                               |
| `快捷菜单系统-QuickMenu/`     | `QuickMenu-1.0.0.jar`                            | 33 套菜单 / 279 个菜单项（r5：基岩按钮带图标 + 两行说明 + 34 个参数输入表单；Java 箱子 GUI / 基岩原生 Form 双轨）                                                                                                                                                        | Geyser、Floodgate、Residence（软依赖）                | `config.yml`（r5 配置）、`快捷菜单系统-QuickMenu.md`、`快捷菜单系统-QuickMenu-与通用菜单对比.md` |
| `每日随机收购-DailySell/`     | `DailySell-1.2.4.jar`（根目录 + `target/`；**主服已部署 1.2.4**，旧 1.2.3 留作 `DailySell-1.2.3.jar.bak-20261119`） | 物品池 110 种（已核）；今日 8 种候选、每人自选 5 种出售（选择按日期持久化），单人每日收益上限 900 币，Java 箱子 GUI / 基岩原生表单（免打字）双端可用                                                                                                                             | **softdepend**: Vault、floodgate（缺经济提供器则 `onEnable` 自动禁用）；`/ds menu`、`/ds pick` 系 `gui`/`choose` 别名 | `使用说明.md`（含 1.2.x 沿革与部署待办）、`config.yml`（实跑基线副本）、`build.py`                  |
| `在线离线奖励-OfflineReward/` | `OfflineReward-1.1.4.jar`                        | 离线 15 币/小时；在线**阶段发放**：间隔 5/10/15/30… 共 8 档、奖励逐档递增 10→100 币，累计 3 小时发完、**全天封顶 300 币**毕业；阶段表全在 config（改完 reload 即生效），`max-per-day: 300` 硬封顶；1.1.1 修 admin 组 `*` 通配误豁免；`mode: cycle` 可回退 1.1.2 循环模式；**1.1.4 新增 AFK 门控**：挂机（EssentialsX `isAfk()`）不计在线时长、不发奖，`ignore-afk` 可关，EssentialsX 缺失自动降级 | PumpkinMail（softdepend）/ Essentials（AFK 判定，缺失可降级） | `使用说明.md`、`config.yml`（实跑基线副本）、`build.py`                                                         |
| `附魔商店-EnchantShop/`       | `EnchantShop-1.0.0.jar`                          | 读取 ExcellentEnchants 配置，用经验等级购买附魔                                                                                                                                                                                                                          | ExcellentEnchants（只读其配置）                       | `使用说明.md`                                                                                    |
| `收纳记录-ClickMobsLogger/`   | **产物缺失**（历史 jar 曾置于 `自研/` 根目录，现已不在仓库，按其 §7 可原样重建） | 旁路审计 ClickMobs 收纳动物：shift+主手右键、1 tick 后确认实体被移除，写 `pickups.jsonl`（JSONL）；`/cml [玩家名]` 查最近 20 条                                                                                                                                          | ClickMobs（未声明 softdepend，缺它也能启动）；**实服未部署、未运行过** | `使用说明.md`                                                                                    |

`disabled/` 存放已停用的项目，不参与部署；SpacePortal 的源码与使用说明都在 `传送阵-SpacePortal/`。

---

## 二、目录结构

```
自研/
├─ README.md                     本文件
├─ lib/                          共享编译依赖（leaf-api / PumpkinMail / floodgate-api / Residence / EssentialsX）
├─ <插件名>/
│  ├─ 使用说明.md                插件说明文档（各插件根目录必备，命名见清单表）
│  ├─ pom.xml                    IDE / Maven 工程描述（可选）
│  ├─ build.py                   仅 PumpkinFly：一键离线编译
│  ├─ src/main/java/             源码
│  ├─ src/main/resources/        plugin.yml、config.yml 等
│  └─ target/                    编译产物（jar + classes）
├─ 传送阵-SpacePortal/           SpacePortal 源码 + 使用说明
├─ 收纳记录-ClickMobsLogger/     例外：src/ + resources/，无 pom/build.py，jar 在 自研/ 根目录
└─ disabled/                     停用项目（含 基岩皮肤记录-BedrockSkinRecorder + 使用说明.md）
```

---

## 三、构建

**编译命令**：JDK 25 的 `javac --release 21`（ClickMobsLogger 例外，见下）。leaf-api 是 Java 25 字节码，低版本 JDK 无法读取；`--release 21` 保证产物仍是 Java 21 字节码，可被服务端加载。

```bash
# 通用形式：classpath 需包含 leaf-api 及其显式声明的传递依赖
javac --release 21 -encoding UTF-8 -d out -cp "<leaf-api>;<外置依赖>;<adventure-api>;<annotations>" $(find src/main/java -name '*.java')
```

- `lib/` 下的 jar 供 `pom.xml` 的 `systemPath` 引用；systemPath 依赖不带传递依赖，各 pom 已显式声明 `adventure-api`、`annotations`。
- **PumpkinFly** 自带 `lib/`（leaf-api / Vault / Residence / EssentialsX 四个实跑 jar）和 `build.py`，直接 `python build.py` 即可产出 `target/PumpkinFly-1.0.0.jar` 并同步到项目根目录。
- 其余插件的日常部署以 `target/` 内已编译的 jar 为准；`pom.xml` 主要用于 IDE 代码补全与依赖声明。
- 涉及 PumpkinMail 的 BuildRate / OfflineReward，构建时先保证 `lib/PumpkinMail.jar` 存在，**且与实跑 jar 同版本**（当前 1.1.1）：改完 PumpkinMail 要**先覆盖 `lib/PumpkinMail.jar`，再构建下游**，否则下游编译到旧签名会报错。
- **OfflineReward 1.1.4** 的 AFK 门控走反射（不 import Essentials 类），编译期无需 `lib/EssentialsX.jar`；运行期由服上的 EssentialsX 提供 `isAfk()`，缺失时自动降级为非 AFK（照常计时）。`lib/EssentialsX.jar` 仅用于本地核对 API 签名。
- **ClickMobsLogger** 历史上由 JDK 25 **默认参数**编译（class major 69，未加 `--release 21`），只依赖 Bukkit API；该 jar 目前不在仓库内，重建命令与完整 classpath 见 [`收纳记录-ClickMobsLogger/使用说明.md`](收纳记录-ClickMobsLogger/使用说明.md) §7（路径已随服务端目录迁移到 `../服务器数据备份/leaf-26.3-多服/libraries`，5 个传递依赖已逐一核实存在）。
- **服务端运行目录**：`服务器数据备份/leaf-26.3-多服/`（多服同机：主服 `plugins/`、`minigames/`、`skyblock/`）。旧路径 `leaf-26.3/leaf-26.3/` 已不存在，各插件文档里的 classpath 均已按新路径校正。

---

## 四、部署

1. 把对应 `target/*.jar` 复制到服务器 `plugins/`。
2. **停服后完整重启**，不要用 `/reload` 换 jar。
3. 首次启动会在 `plugins/<插件>/` 生成配置与数据文件；按需修改配置后执行对应 reload 命令。

> 替换 jar **不会覆盖**已有的 `plugins/<插件>/config.yml`。新增配置键使用代码内缺省值，若要看到新配置注释，先备份再删除 `config.yml` 重启（会丢掉手改过的参数）。

---

## 五、命令与文档速查

| 插件            | 主命令                                                                                                                                                           | 详细文档                                           |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| SpacePortal     | `/csz` / `/spportal` / `/spaceportal`（`gui` / `create` / `link2way` / `item` / `reload` …）                                                                     | `传送阵-SpacePortal/空间传送阵使用说明.md`         |
| PumpkinFly      | `/pfly`（`buy` / `claim` / `trial` / `global` / `player` / `hud` / `status` / `records` / `give` / `take` / `setperm` / `reload`；时长支持 `30m` / `2h` / `3d`） | `南瓜飞-PumpkinFly/使用说明.md`                    |
| QuickMenu       | `/qm`（`open` / `reload` / `give` …）                                                                                                                            | `快捷菜单系统-QuickMenu/快捷菜单系统-QuickMenu.md` |
| DailySell       | `/ds`（`choose` / `today` / `sell` / `sellall` / `reload` / `reroll`）                                                                                           | `每日随机收购-DailySell/使用说明.md`               |
| MoneyLedger     | `/moneyledger`（`recent` / `summary` / `export` / `stats` / `backfill [full]` / `verify`；别名 `moneylog`，全部需 `moneyledger.admin`）                             | `南瓜账本-MoneyLedger/使用说明.md`                 |
| PumpkinMail     | `/mailbox`（`give` / `preset add\|list\|remove` / `admin` / `list` / `stats` / `log` / `verify` / `reload`；别名 `pmail`）                                          | `南瓜邮箱-PumpkinMail/使用说明.md`                 |
| ClickMobsLogger | `/cml [玩家名]`（查最近 20 条拾取记录，权限 `clickmobslogger.use`）                                                                                              | `收纳记录-ClickMobsLogger/使用说明.md`             |
| 其余            | 见各自 `plugin.yml` 与使用文档                                                                                                                                   | 见上表                                             |

---

## 六、注意事项

- 全部插件只使用 Bukkit / Paper API 与公开插件 API，无 NMS 反射；`Class.forName` 仅用于 SQLite 驱动与插件间软依赖探测。
- 软依赖未安装时，相关功能会自动跳过，不影响插件启动。
- 数据均落在 `plugins/<插件>/`：SpacePortal 用 `portals.yml` + `override.yml`，PumpkinFly 用 `data/{players,purchases,sessions,global}.yml`（`global.yml` 存全服飞行开关与全服体验），QuickMenu 用 `config.yml`，DailySell 用 `daily.yml`（今日清单）+ `data.yml`（已售数量 / `__earned` 收益累计 / `choices` 自选记录），ClickMobsLogger 用 `config.yml` + `pickups.jsonl`（每行一条 JSON 的追加日志，无轮转），MoneyLedger 用 `ledger.db` + `ledger-failed.log` + `export/*.csv`，PumpkinMail 用 `mailbox.db`，BuildRate 用 `data.db`，OfflineReward 用 `offline.db`。**这些都是运行数据，整理与汉化时一律不动。**

---

## 七、实跑版本与字节码对照（2026-11-19 逐 jar 核对）

对照口径：**源码 `plugin.yml` ↔ 插件根目录 jar ↔ `target/` jar ↔ 主服 `plugins/*.jar`** 四者逐一比对（内嵌 `plugin.yml` 版本 + class major）。

### 7.1 对齐情况：11 个在册插件里 10 个零落差

`MoneyLedger 1.1.0`、`PumpkinMail 1.1.1`、`PumpkinFly 1.0.0`（根/`target`/实跑三份 SHA256 相同 `89774fed4b…`）、`PlanTop 1.0.0`、`OfflineReward 1.1.4`、`BuildRate 1.0.0`、`QuickMenu 1.0.0`、`SpacePortal 1.0.0`、`EnchantShop 1.0.0`、**`DailySell 1.2.4`（2026-11-19 已由 1.2.3 升级部署）** —— 源码、产物、实跑一致。

> `SpacePortal` 的 `plugin.yml` 写的是 `${project.version}`，**这不是错误**：`pom.xml` 开了 `<filtering>true</filtering>` + `<version>1.0.0</version>`，jar 内已固化为 `version: '1.0.0'`。

### 7.2 落差清单（DailySell 已修复，ClickMobsLogger 待处理）

| 插件 | 状态 | 后果 / 处置 |
|---|---|---|
| **DailySell** | **已修复**：1.2.3 改为 `DailySell-1.2.3.jar.bak-20261119`，`1.2.4` 已复制进主服 `plugins/`（SHA256 `e8304af7ee75…`，major 65），数据目录未动 | **1.2.4 修的「基岩自选表单第一个候选选不中、选中却变成别的物品」（Cumulus 响应把开头 `label` 也算一位）已随 jar 就位**，但需下次启动服务端才加载；上线验证与回滚步骤见 `每日随机收购-DailySell/使用说明.md` §十一 |
| **ClickMobsLogger** | **待处理**：仓库无产物 jar，且从未部署到实服 | 只有源码 + 说明文档；`自研/` 根目录已无该 jar（历史 md5 `092abcb4…`）。需要时按其 §7 命令重建，产物落在自己插件目录内 |

### 7.3 ⚠️ 字节码不统一：PumpkinMail 是 major 69

| 插件 | class major | 说明 |
|---|---|---|
| `PumpkinMail-1.1.1.jar`（实跑 + `lib/` + 根目录三份同哈希） | **69 = Java 25** | 偏离仓库总则（`javac --release 21`）。当前服务端实跑 Java 25 能加载；**一旦服务端降回 Java 21，PumpkinMail 会第一个抛 `UnsupportedClassVersionError`，连带 BuildRate / OfflineReward 的发奖链路瘫痪** |
| 其余全部（MoneyLedger / PumpkinFly / PlanTop / OfflineReward / BuildRate / DailySell / QuickMenu / SpacePortal / EnchantShop） | 65 = Java 21 | 符合总则 |

**处置决定（2026-11-19）**：本轮**暂不重编**。风险留档在此——只要服务端一直跑 Java 25 就无影响；若将来要降级到 Java 21，必须先重编 PumpkinMail（`--release 21`）并**先同步 `自研/lib/PumpkinMail.jar`、再构建 BuildRate 与 OfflineReward**（顺序颠倒会让下游编译到旧签名），否则邮箱与两个下游插件的发奖链路会一起失效。

### 7.4 运行目录已更名

服务端实跑目录现为 **`服务器数据备份/leaf-26.3-多服/`**（主服 `plugins/` + `minigames/` + `skyblock/` 多服同机）。旧路径 `leaf-26.3/leaf-26.3/` 已不存在，涉及 classpath 的 `build.py` 与文档中的路径均已按新目录校正；实跑数据里 `PumpkinFly/lib` 之外，`DailySell` 的 `build.py` 会直接抓实服 `plugins/` 下的 `Vault.jar` 与 `Geyser-Spigot.jar`，迁移目录后要同步改脚本路径。
