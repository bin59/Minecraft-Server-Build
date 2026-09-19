# Quests 每日任务

本文档介绍南瓜生存服经济体系第二阶段落地的任务插件 Quests 3.16.1（LMBishop 官方版），说明其作为经济体系**合规 Faucet（水龙头）**的定位、每日任务的配置（任务文件、条件与奖励）、玩家命令与权限，以及升级踩坑记录（3.x 与 5.x 差异）。适用于管理员维护任务配置、升级插件前查阅。

> 落地环境：测试服 `C:\mc_serve\1.21.11-test\`（Leaf 1.21.11）｜ 落地日期：2026-09-17 晚

## 功能定位

- **合规 Faucet**：通过完成每日任务获得南瓜币，是新手玩家获取初始资金的主要渠道之一。
- **防刷设计**：任务每天可重复一次（`repeatable: true` + 冷却 1440 分钟），防止反复刷奖励。
- **分类展示**：任务归入 `daily` 分类，GUI 显示"每日任务"（2026-09-19 已删除插件自带的 `examples`、`permissionexample` 两个示例分类——即任务界面里那两个水桶图标）。

## 版本与来源

- 版本：**3.16.1（LMBishop 官方）**，从 `cdn.modrinth.com` 下载（SpigotMC 同源）。
- 配置目录：`plugins/Quests/quests/`（**3.x 格式：每任务一文件**）。

## 任务配置（当前值）

| 任务文件 | 任务 | 条件 | 奖励 |
|---|---|---|---|
| `dailyminer.yml` | 每日 · 挖矿人 | 铁矿石 8 + 深板岩铁矿石 8 | 50 南瓜币 |
| `dailyhunter.yml` | 每日 · 猎手 | 击杀僵尸 10 | 50 南瓜币 |
| `dailyfisher.yml` | 每日 · 渔夫 | 钓鱼 5 条 | 40 南瓜币 |
| `dailywoodcutter.yml` | 每日 · 伐木工 | 砍橡木原木 16 | 40 南瓜币 |
| `dailycook.yml` | 每日 · 厨师 | 熔炉烧炼 16 个物品 | 40 南瓜币 |
| `dailytamer.yml` | 每日 · 驯养师 | 驯服动物 2 只 | 50 南瓜币 |
| `dailywalker.yml` | 每日 · 远行者 | 步行累计 5000 格 | 40 南瓜币 |
| `dailyfarmer.yml` | 每日 · 农夫 | 收获成熟小麦 16 | 40 南瓜币 |

- 全部任务均为 `repeatable: true` + `cooldown 1440 分钟`（每天一次）。
- 奖励走 `eco give {player} N`（EssentialsX 单账本，与全服经济统一）。

## 命令

| 命令 | 用途 |
|---|---|
| `/quests` | 打开任务界面（默认可用） |
| `/quests cancel <任务ID>` | 取消已领取的任务（如 `/quests cancel dailyminer`） |

## GUI 操作（2026-09-19 已调整为基岩版兼容）

| 动作 | 操作 | 说明 |
|---|---|---|
| 接任务 | **左键点击** | 领取任务 |
| 追踪任务 | **右键点击** | Java 版可用；基岩版无需手动追踪（`quest-autotrack: true` 自动追踪） |
| 取消任务 | **按 Q（丢弃键）** | 弹出确认菜单后点确认；**基岩版玩家在容器里长按物品拖出界面即可触发** |

> 历史：原配置取消动作为"右键"（RIGHT），基岩版玩家在容器界面无法触发右键导致任务取消不了；2026-09-19 改为 DROP 键，`track-quest` 相应改为 RIGHT，全局 lore 提示已同步更新。

## 权限（LuckPerms default 组，已授予）

| 权限 | 说明 |
|---|---|
| `quests.command.start` | 接任务 |
| `quests.command.track` | 追踪任务 |
| `quests.command.cancel` | 取消任务 |
| `quests.command.quest` | 查看任务 |

> 3.x 权限节点为 `quests.command.*`（`/quests` 本体默认 true）；非 5.x 的 `quests.quest/take/list`。

## 与经济体系的关系

- **Faucet 项**：每日 8 个任务合计 **350 南瓜币**（50+50+40+40+40+50+40+40；2026-09-19 从 570 下调以压 Faucet），计入周 Faucet 统计（见《经济系统落地实施-测试服.md》§4）。
- **配比约束**：若系统发放占比过高（Faucet ÷ 玩家总收入 ≥ 50%），优先降低任务额度。

## ⚠️ 升级踩坑记录（供后续升级参考）

1. Modrinth `quests.classic` **5.x 线在 Leaf 上不可用**：5.3.2 解析报 `IndexOutOfBoundsException`、5.3.1/5.3.0 报 `quests.yml` 资源缺失。
2. 官方项目（SpigotMC 同源）Quests **3.16.1** 下载成功（当日 CDN 曾持续超时数小时，后恢复）。
3. **3.x 与 5.x 配置格式完全不同**：3.x 为 `quests/` 每任务一文件（tasks/display/rewards/options 结构）；旧 5.x 的 `quests.yml`/`actions.yml`/`conditions.yml`/`lang/`/`storage/` 已移入 `plugins/Quests/_v5-backup-20260917/` 备份。
4. **任务 ID 不允许下划线**（如 `daily_miner` → `dailyminer`）。
5. 启动日志确认：`Successfully hooked into EzTax economy`、`/q a config` 无错误（2026-09-17 初装时为 `3 quests have been registered`；2026-09-19 新增 5 个任务后应为 `8 quests have been registered`）。
6. **正式开服前清理** LuckPerms 中已失效的 5.x 旧权限（`quests.quest/take/list/quit/stats`）。
7. Quests 3.16.1 声明 api-version 1.13，Leaf 1.21.11 下运行正常但属"版本外兼容"，升级服务端前先验证。
