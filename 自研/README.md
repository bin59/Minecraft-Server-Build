# 自研插件

本目录是南瓜服自研插件的**唯一一套源码与构建产物**：每个插件一个目录，源码在 `src/main/java/`，配置在 `src/main/resources/`，可直接部署的 jar 在 `target/`（PumpkinFly 另在项目根目录存一份）。**例外**：`收纳记录-ClickMobsLogger/` 用 `src/` + `resources/`，jar 直接放在 `自研/` 根目录，无 `pom.xml` / `build.py` / `target/`。

除 ClickMobsLogger 为 `1.21` 外，所有插件的 `plugin.yml` 中 `api-version` 均为 `26.3`，按当前服务端实跑环境编写与编译。

---

## 一、插件清单

| 目录                      | 产物 jar                  | 功能                                                                | 运行时依赖                             | 说明文档                                                                      |
| ------------------------- | ------------------------- | ------------------------------------------------------------------- | -------------------------------------- | ----------------------------------------------------------------------------- |
| `传送阵-SpacePortal/`     | `SpacePortal-1.0.0.jar`   | 空间传送阵：建阵 / 双向连接 / 踩阵充能传送 / GUI / 随行宠物；命令 `/csz`、`/spportal`，不占 `/portal`         | Geyser + Floodgate（互通服）           | `传送阵-SpacePortal/空间传送阵使用说明.md` |
| `南瓜账本-MoneyLedger/`   | `MoneyLedger-1.1.0.jar`   | **全量准确流水**：余额变动事件同步落盘（不再走内存队列）+ Essentials `trade.log` 幂等回填补齐漏记；BigDecimal 精确金额、真实 UUID、来源/类别修正；`/moneyledger backfill` / `verify` / `stats`，导出 CSV | EssentialsX（`UserBalanceUpdateEvent` + `trade.log`） | `config.yml`（回填开关/周期/起始日） |
| `南瓜邮箱-PumpkinMail/`   | `PumpkinMail-1.1.0.jar`   | 活动奖励批量发放与玩家领取（`/mailbox`）；**1.1.0 起全量审计**：发放/去重/领取/失败写入 `reward_log`，与发放同事务提交，旧库自动迁移并幂等回填历史；`/mailbox log` / `verify` 可查可对账 | SQLite（sqlite-jdbc 由 Leaf 服务端 libraries 提供） | `config.yml`（含审计说明）、`plugin.yml` |
| `南瓜飞-PumpkinFly/`      | `PumpkinFly-1.0.0.jar`    | 付费 / 限时 / 永久飞行，仅飞行中扣秒，限自己领地与主城；限时体验奖励（天/时/分）、全局与单人飞行管理 | Vault、Residence、EssentialsX          | `使用说明.md`                                                                 |
| `在线时间排行-PlanTop/`   | `PlanTop-1.0.0.jar`       | 基于 Plan 数据的在线时间排行榜                                      | PlaceholderAPI、Plan MySQL             | `PlanTop.md`                                                                  |
| `建筑评分-BuildRate/`     | `BuildRate-1.0.0.jar`     | 建筑评分与榜单                                                      | PumpkinMail                            | `使用说明.md`                                                                 |
| `快捷菜单系统-QuickMenu/` | `QuickMenu-1.0.0.jar`     | 33 套菜单 / 279 个菜单项（r5：基岩按钮带图标 + 两行说明 + 34 个参数输入表单；Java 箱子 GUI / 基岩原生 Form 双轨） | Geyser、Floodgate、Residence（软依赖） | `config.yml`（r5 配置）、`快捷菜单系统-QuickMenu.md`、`快捷菜单系统-QuickMenu-与通用菜单对比.md` |
| `每日随机收购-DailySell/` | `DailySell-1.2.4.jar`     | 物品池 110 种；今日 8 种候选、每人自选 5 种出售（选择按日期持久化），单人每日收益上限 900 币，Java 箱子 GUI / 基岩原生表单（免打字）双端可用 | Vault、Floodgate（软依赖） | `使用说明.md`、`config.yml`（实跑基线副本）、`build.py` |
| `在线离线奖励-OfflineReward/` | `OfflineReward-1.1.3.jar` | 离线 15 币/小时；在线**阶段发放**：间隔 5/10/15/30… 共 8 档、奖励逐档递增 10→100 币，累计 3 小时发完、**全天封顶 300 币**毕业；阶段表全在 config（改完 reload 即生效），`max-per-day: 300` 硬封顶；1.1.1 修 admin 组 `*` 通配误豁免；`mode: cycle` 可回退 1.1.2 循环模式 | PumpkinMail                            | `config.yml`（实跑基线副本）、`build.py`                                          |
| `附魔商店-EnchantShop/`   | `EnchantShop-1.0.0.jar`   | 读取 ExcellentEnchants 配置，用经验等级购买附魔                     | ExcellentEnchants（只读其配置）        | `使用说明.md`                                                                 |
| `收纳记录-ClickMobsLogger/` | `ClickMobsLogger-1.0.0.jar`（在 `自研/` 根目录） | 旁路审计 ClickMobs 收纳动物：shift+主手右键、1 tick 后确认实体被移除，写 `pickups.jsonl`（JSONL）；`/cml [玩家名]` 查最近 20 条 | ClickMobs（未声明 softdepend，缺它也能启动） | `使用说明.md`                                                                 |

`disabled/` 存放已停用的项目，不参与部署；SpacePortal 的源码与使用说明都在 `传送阵-SpacePortal/`。

---

## 二、目录结构

```
自研/
├─ README.md                     本文件
├─ lib/                          共享编译依赖（leaf-api / PumpkinMail / floodgate-api / Residence / EssentialsX）
├─ <插件名>/
│  ├─ pom.xml                    IDE / Maven 工程描述（可选）
│  ├─ build.py                   仅 PumpkinFly：一键离线编译
│  ├─ src/main/java/             源码
│  ├─ src/main/resources/        plugin.yml、config.yml 等
│  └─ target/                    编译产物（jar + classes）
├─ 传送阵-SpacePortal/           SpacePortal 源码 + 使用说明
├─ 收纳记录-ClickMobsLogger/     例外：src/ + resources/，无 pom/build.py，jar 在 自研/ 根目录
└─ disabled/                     停用项目
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
- 涉及 PumpkinMail 的 BuildRate / OfflineReward，构建时先保证 `lib/PumpkinMail.jar` 存在。
- **ClickMobsLogger** 现有 jar 由 JDK 25 **默认参数**编译（class major 69，未加 `--release 21`），只依赖 Bukkit API；完整 classpath、重编译与打包命令见 [`收纳记录-ClickMobsLogger/使用说明.md`](收纳记录-ClickMobsLogger/使用说明.md) §7。

---

## 四、部署

1. 把对应 `target/*.jar` 复制到服务器 `plugins/`。
2. **停服后完整重启**，不要用 `/reload` 换 jar。
3. 首次启动会在 `plugins/<插件>/` 生成配置与数据文件；按需修改配置后执行对应 reload 命令。

> 替换 jar **不会覆盖**已有的 `plugins/<插件>/config.yml`。新增配置键使用代码内缺省值，若要看到新配置注释，先备份再删除 `config.yml` 重启（会丢掉手改过的参数）。

---

## 五、命令与文档速查

| 插件        | 主命令                                                                                          | 详细文档                                           |
| ----------- | ----------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| SpacePortal | `/csz` / `/spportal` / `/spaceportal`（`gui` / `create` / `link2way` / `item` / `reload` …）                                   | `传送阵-SpacePortal/空间传送阵使用说明.md`                     |
| PumpkinFly  | `/pfly`（`buy` / `claim` / `trial` / `global` / `player` / `hud` / `status` / `records` / `give` / `take` / `setperm` / `reload`；时长支持 `30m` / `2h` / `3d`） | `南瓜飞-PumpkinFly/使用说明.md`                    |
| QuickMenu   | `/qm`（`open` / `reload` / `give` …）                                                           | `快捷菜单系统-QuickMenu/快捷菜单系统-QuickMenu.md` |
| DailySell  | `/ds`（`choose` / `today` / `sell` / `sellall` / `reload` / `reroll`）                            | `每日随机收购-DailySell/使用说明.md`                |
| PumpkinMail | `/mailbox`；管理 `log` / `verify` / `stats`                                            | 见插件目录源码                                     |
| ClickMobsLogger | `/cml [玩家名]`（查最近 20 条拾取记录，权限 `clickmobslogger.use`）                      | `收纳记录-ClickMobsLogger/使用说明.md`               |
| 其余        | 见各自 `plugin.yml` 与使用文档                                                                  | 见上表                                             |

---

## 六、注意事项

- 全部插件只使用 Bukkit / Paper API 与公开插件 API，无 NMS 反射；`Class.forName` 仅用于 SQLite 驱动与插件间软依赖探测。
- 软依赖未安装时，相关功能会自动跳过，不影响插件启动。
- 数据均落在 `plugins/<插件>/`：SpacePortal 用 `portals.yml` + `override.yml`，PumpkinFly 用 `data/{players,purchases,sessions,global}.yml`（`global.yml` 存全服飞行开关与全服体验），QuickMenu 用 `config.yml`，DailySell 用 `daily.yml`（今日清单）+ `data.yml`（已售数量 / `__earned` 收益累计 / `choices` 自选记录），ClickMobsLogger 用 `config.yml` + `pickups.jsonl`（每行一条 JSON 的追加日志，无轮转）。
