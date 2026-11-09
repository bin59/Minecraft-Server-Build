# EvenMoreFish — 更多鱼（钓鱼扩展）

> 把原版钓鱼变成有图鉴、稀有度、比赛和经济循环的玩法：钓上自定义鱼种、按尺寸/稀有度评分、参加定时钓鱼比赛、把鱼卖给商店换南瓜币。

## 基本信息

| 项目     | 内容                                                                                 |
| -------- | ------------------------------------------------------------------------------------ |
| 插件名   | EvenMoreFish                                                                         |
| 命令前缀 | `/emf`（全名 `/evenmorefish`）                                                       |
| 数据目录 | `plugins/EvenMoreFish/`                                                              |
| 配置文件 | `config.yml`、`rarities.yml`、`fish.yml`、`competitions.yml`、`baits.yml`、`locale/` |
| 前提     | 需要经济插件（Vault/南瓜币 economy）来支撑卖鱼与比赛奖金                             |

## 它提供什么

- **自定义鱼**：开箱内置 70+ 种鱼，可用任意物品或玩家头颅（base64）当鱼模型，配名称、lore、长度（cm）。
- **稀有度**：在 `rarities.yml` 自定义分档（颜色、权重、掉落），比如普通/稀有/史诗/传说。
- **图鉴 GUI**：钓到的鱼自动记入图鉴，`/emf gui` 打开查看已收集/未收集。
- **钓鱼比赛**：定时开赛（`competitions.yml`），bossbar 显示倒计时与实时排行榜，按"最长的鱼"或"最多鱼"评比，结束发奖。
- **经济商店**：每条鱼有售价，`/emf shop` 单卖或 `/emf sellall` 一键全卖。
- **鱼饵系统**：`baits.yml` 给特定稀有度/鱼种加权重加成（`+N`/`-N`/`*N`/`/N` 调整掉率），引导玩家定向钓某类鱼。
- **生态/维度限制**：鱼可限定只在某些生物群系（biome-sets）钓到；支持在岩浆、末地虚空等特殊维度钓鱼。
- **奖励命令**：钓到稀有鱼可执行控制台命令（发南瓜币、发物品、跑权限）。

## 玩家命令

| 命令              | 作用                    | 需要权限         |
| ----------------- | ----------------------- | ---------------- |
| `/emf`            | 显示帮助                | `emf.use`        |
| `/emf gui`        | 打开图鉴/钓鱼菜单       | `emf.gui`        |
| `/emf next`       | 查看下一场比赛时间      | `emf.next`       |
| `/emf top`        | 查看当前/最近比赛排行榜 | `emf.top`        |
| `/emf shop`       | 打开卖鱼商店            | `emf.shop`       |
| `/emf sellall`    | 把背包里所有鱼一键卖掉  | `emf.sellall`    |
| `/emf applybaits` | 打开鱼饵涂抹菜单        | `emf.applybaits` |
| `/emf toggle`     | 开关自己的钓鱼奖励      | `emf.toggle`     |
| `/emf journal`    | 打开鱼类日记            | `emf.journal`    |

> **注意**：新版 EvenMoreFish（2.5+）使用 MiniMessage 消息格式，如果 `messages.yml` 里有旧的 `[noPrefix]` 标签会导致命令报错。已删除全部 31 处 `[noPrefix]` 修复此问题。

## 玩家权限配置

默认玩家组需要添加以下权限才能正常使用钓鱼功能：

```
/lp group default permission set emf.use true
/lp group default permission set emf.gui true
/lp group default permission set emf.shop true
/lp group default permission set emf.journal true
/lp group default permission set emf.applybaits true
/lp group default permission set emf.competition true
/lp group default permission set emf.toggle true
/lp group default permission set emf.next true
/lp group default permission set emf.top true
/lp group default permission set emf.sellall true

```

## 管理员常用配置

- **`rarities.yml`**：改稀有度颜色、权重、长度区间。越稀有权重越低、颜色越亮。
- **`fish.yml`**：加/改鱼种——用什么物品当模型、多少长度、在哪个 biome、售价、钓到执行什么奖励命令。
- **`competitions.yml`**：比赛开始时间、时长、评比规则（最长 vs 最多）、冠军/前 N 名奖励。
- **`baits.yml`**：调鱼饵对掉率的加成。
- **改完重载**：`/emf admin reload`（或重启）。

## 与服务器经济的衔接

- 卖鱼所得走 Vault economy，即南瓜币；
- 比赛奖金建议直接配 `/eco give <player> <金额>` 或调用南瓜邮箱发奖，记入南瓜账本；
- 稀有鱼奖励命令可以接南瓜邮箱，让奖励"下次上线领取"，避免钓鱼瞬间刷屏。

## 注意事项

1. **配置 key 用英文**，显示名（中文名）写在 fish/rarities 的 display name 字段，别把 key 本身改成中文，否则 baits.yml 引用会对不上。
2. **生物群系限定**：鱼设了 biome-sets 后，不在对应水域就钓不到；加新鱼时先确认玩家常钓鱼的水域属于哪个群系。
3. **比赛别和活动撞车**：定时比赛会占 bossbar 与聊天播报，和建筑评分/其他全服活动错开时间。
4. **本地备份当前未见该插件 jar 与数据目录**，若已在云端启用，请把 `plugins/EvenMoreFish/` 同步回本地备份，再按上面逐项核对配置。

## 卖鱼价格调整（价格公式 + 整体降价）

> 以下基于运行服 `plugins/EvenMoreFish/` 实测配置（2026-10）。

### 价格怎么算

```
最终卖价 = 鱼重量(cm) × 稀有度 worth-multiplier × economy 全局 multiplier
```

⚠️ **价格不在 `evenmorefish.db`**：数据库只存玩家钓鱼记录（钓过的鱼 / 重量 / 统计），改它不影响卖价。价格全部来自配置文件。

### 第一层：稀有度 `worth-multiplier`（`rarities/*.yml`）

每个稀有度一个文件，改 `worth-multiplier` 即改该档**所有鱼**的价格。本服实测当前系数：

| 稀有度 | 文件 | worth-multiplier |
| --- | --- | --- |
| 普通 | `rarities/common.yml` | 0.1 |
| 稀有 | `rarities/rare.yml` | 0.2 |
| 史诗 | `rarities/epic.yml` | 0.15 |
| 传说 | `rarities/legendary.yml` | 0.2 |
| 垃圾 | `rarities/junk.yml` | 0 |

> ⚠️ 史诗(0.15)比稀有(0.2)便宜、传说与稀有同价——疑似默认值未调好，建议调成合理梯度（普通 < 稀有 < 史诗 < 传说）。

**单条鱼改价**：可在该稀有度的 `fish:` 条目下给单条鱼覆盖 `worth-multiplier`（精确语法以 `rarities/_example.yml` 为准）。

### 第二层：整体降价首选 — `config.yml` 的 `economy` 全局倍数

`plugins/EvenMoreFish/config.yml` → `economy.vault.multiplier`（本服启用 Vault，当前为 `1.0`）：

| 想降到 | 改 multiplier 为 |
| --- | --- |
| 原价 | `1.0` |
| 7 折 | `0.7` |
| 半价 | `0.5` |
| 3 折 | `0.3` |

**整体缩放只改这一个值，不动档间差价**；要精细控制档间比例才去改第一层。

### 生效

改完保存后 `/emf admin reload`（或完整重启服务端）生效。

已汉化
