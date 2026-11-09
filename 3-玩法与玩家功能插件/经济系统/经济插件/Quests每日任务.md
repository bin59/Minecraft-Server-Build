# Quests 每日任务

Quests 是本服每日任务插件，作为经济体系的**合规 Faucet（水龙头）**：玩家完成每日任务获得南瓜币，是新手获取初始资金的主要渠道之一。本文档说明任务配置、条件类型、奖励、命令与权限，适用于管理员维护任务文件与排查任务发放。

## 版本与来源

| 项目 | 值 |
|---|---|
| 插件 | **Quests Classic 5.3.2-b570**（PikaMug，`Quests-5.3.2.jar`） |
| 主类 | `me.pikamug.quests.BukkitQuestsPlugin` |
| 项目地址 | https://github.com/PikaMug/Quests |
| 下载 | https://modrinth.com/plugin/quests.classic |
| 文档 | https://pikamug.gitbook.io/quests |
| 软前置 | Vault（发放 `rewards.money`）、PlaceholderAPI、Citizens 等可选 |
| 语言 | 内置多语，`config.yml` 的 `language` 切换（自带 `zh-TW`） |

> Quests 有两个同名但完全不同的插件：本服使用 **PikaMug / Quests Classic**（配置集中在 `storage/quests.yml`，任务 ID 如 `default1`），不是 LMBishop Quests 3.x（`quests/` 每任务一文件）。两者配置、数据、命令、权限互不兼容。

## 功能定位

- **合规 Faucet**：8 个每日任务合计 **350 南瓜币/天**（50+50+40+40+40+50+40+40），计入周 Faucet 统计。
- **防刷设计**：`redo-delay` 控制单个任务的重复冷却（**单位：秒**），`86400` 即 24 小时。注意是「完成后计时」的冷却，不是每日 0 点统一重置。单位已由 Quests 5.3.2 字节码核实：`getInt(redo-delay) * 1000 → Planner.setCooldown(ms)`，与 `System.currentTimeMillis()` 相减。
- **分类展示**：任务在 `quests.yml` 中按 ID 组织，`/quests list` 列出可接任务。
- **配比约束**：若系统发放占比过高（Faucet ÷ 玩家总收入 ≥ 50%），优先降低任务额度。

## 目录结构

```
plugins/Quests/
├── config.yml              # 全局行为（语言、存储、GUI、自动接受等）
├── storage/
│   ├── quests.yml          # 任务定义（集中式，所有任务写在一个文件）
│   ├── actions.yml         # 事件动作（奖励提示、失败、计时器等）
│   └── conditions.yml      # 条件（占位符、骑乘等）
└── data/<UUID>.yml         # 每玩家进度、已完成次数、冷却时间戳
```

`quests.yml` 必须保持 **UTF-8** 编码（文件头自带说明），否则中文任务名会乱码。修改后 `/questadmin reload` 或重启生效。

## 任务配置（quests.yml）

### 结构

```yaml
quests:
  <任务ID>:
    name: '显示名称'
    ask-message: '<yellow>接取时显示的描述'
    finish-message: '<yellow>完成时的提示'
    redo-delay: 86400           # 重复冷却（秒，24 小时）
    requirements:               # 接取前置（可选）
      items:
      - ==: org.bukkit.inventory.ItemStack
        type: STONE_PICKAXE
      remove-items: [false]
    stages:
      ordered:
        1:
          break-block-names:
            - IRON_ORE
            - DEEPSLATE_IRON_ORE
          break-block-amounts:
            - 8
            - 8
    rewards:
      money: 50                 # 经 Vault 直接发放南瓜币
      quest-points: 1
```

要点：

- **任务 ID 用小写无下划线**（`dailyminer` 而非 `daily_miner`），ID 即 `/quest take <ID>` 的参数。
- `stages.ordered` 下按序号排列阶段；多阶段任务依次完成。
- `rewards.money` 走 Vault / EssentialsX 单账本，**不需要** `eco give {player} N` 命令奖励。
- `redo-delay` 与 `requirements`、`rewards` 同级；`ask-message` / `finish-message` 用 MiniMessage 颜色标签。
- 中文文本用单引号包裹，避免 YAML 把 `<` `:` 当作语法。

### 本服每日任务清单

| 任务 ID | 名称 | 条件 | 条件类型（编辑器） | 奖励 |
|---|---|---|---|---|
| `dailyminer` | 每日 · 挖矿人 | 铁矿石 8 + 深板岩铁矿石 8 | Break blocks | 50 |
| `dailyhunter` | 每日 · 猎手 | 击杀僵尸 10 | Kill mobs | 50 |
| `dailyfisher` | 每日 · 渔夫 | 钓鱼 5 条 | Catch fish | 40 |
| `dailywoodcutter` | 每日 · 伐木工 | 砍橡木原木 16 | Break blocks | 40 |
| `dailycook` | 每日 · 厨师 | 熔炉烧炼 16 个物品 | Smelt items | 40 |
| `dailytamer` | 每日 · 驯养师 | 驯服动物 2 只 | Tame mobs | 50 |
| `dailywalker` | 每日 · 远行者 | 放置火把 16 个 | Place blocks | 40 |
| `dailyfarmer` | 每日 · 农夫 | 收获成熟小麦 16 | Break blocks（WHEAT） | 40 |

全部任务 `redo-delay: 86400`，奖励 `money` 走 Vault。

> `dailywalker` 的步行距离在 Quests Classic 中用 **Conditions + PlaceholderAPI 距离占位符**实现（`conditions.yml`），而不是独立的 stage 键；配置时以游戏内编辑器 `/questadmin` 的可用类型为准。

### 已验证的 stage 键

| 键 | 用途 | 示例 |
|---|---|---|
| `break-block-names` / `break-block-amounts` / `break-block-durability` | 破坏指定方块 | `IRON_ORE` + 数量 |
| `mobs-to-kill` / `mob-amounts` | 击杀生物 | `Zombie` / `10` |
| `fish-to-catch` | 钓鱼数量 | `5` |
| `death-event` / `start-event` / `finish-event` | 绑定 `actions.yml` 中的事件动作 | `DeathFail` |

编辑器内另有 Smelt items / Tame mobs / Place blocks / Shear sheep / Milk cows / Brew potions / Enchant items / Consume items / Damage blocks 等类型，对应键名以编辑器生成的 YAML 为准。

### 奖励类型

| 键 | 说明 |
|---|---|
| `money` | 经 Vault 发放南瓜币（本服使用） |
| `quest-points` | 任务点数，用于排行榜与解锁 |
| `items` | 物品（`==: org.bukkit.inventory.ItemStack` + `type`） |
| `exp` | 经验值 |
| `commands` | 完成任务的控制台命令 |

## config.yml 关键项

| 键 | 值 | 说明 |
|---|---|---|
| `allow-command-questing` | `true` | 允许用命令接取/完成任务 |
| `confirm-accept` / `confirm-abandon` | `true` | 接取 / 放弃需二次确认 |
| `generate-files-on-join` | `true` | 玩家进服时生成 `data/<UUID>.yml` |
| `give-journal-item` | `false` | 不给任务日志物品 |
| `language` | `en-US` | 语言包；可切 `zh-TW` |
| `max-quests` | `0` | 同时进行任务数不限 |
| `storage-method.player-data` | `yaml` | 玩家数据用 YAML（可选 MySQL） |
| `storage-data` | — | MySQL 连接参数，仅切数据库时使用 |
| `prevent-exploit` | `true` | 反刷任务保护 |
| `show-requirements` / `show-completed-objectives` | `true` | GUI 显示前置与已完成目标 |

## 命令

| 命令 | 别名 | 权限 | 用途 |
|---|---|---|---|
| `/quests` | `/qs` | `quests.quests` | 插件帮助 / 任务界面 |
| `/quests list` | — | `quests.list` | 列出可接任务 |
| `/quests take <ID>` | — | `quests.take` | 接取指定任务 |
| `/quests quit` | — | `quests.quit` | 放弃当前任务 |
| `/quest` | `/q` | `quests.quest` | 查看当前任务目标 |
| `/quest stats` / `top` / `info` / `journal` | — | 对应节点 | 统计 / 排行 / 信息 / 任务日志 |
| `/questadmin` | `/qa`, `/questsadmin` | `quests.admin` | 管理命令（重载、编辑、发放、跳阶段等） |

> 基岩版玩家经 Geyser 使用命令更稳妥：`/quests take dailyminer`、`/quest`、`/quests quit` 不依赖 GUI 点击，避免容器交互差异。

## 权限（LuckPerms）

`plugin.yml` 中玩家节点默认 `true`，管理节点默认 `op`：

| 节点 | 默认 | 说明 |
|---|---|---|
| `quests.quest` / `questinfo` / `quests` | true | 查看目标 / 任务信息 / 帮助 |
| `quests.list` / `take` / `quit` | true | 列出 / 接取 / 放弃 |
| `quests.stats` / `top` / `info` | true | 统计 / 排行 / 插件信息 |
| `quests.journal` / `compass` / `choice` | true | 任务日志 / 指南针 / 对话选择 |
| `quests.admin.*` | op | 全部管理功能 |
| `quests.editor.*` / `events.*` / `actions.*` / `conditions.*` | op | 编辑器与事件动作 |
| `quests.*` | op | 全部功能的父节点 |

> 本服不需要为 default 组额外授权玩家节点（默认已放开）；仅需给管理员 `quests.admin.*`。

## 与经济体系的关系

- **Faucet 项**：8 个任务合计 350 南瓜币/天，计入周 Faucet 统计。
- **防刷**：`redo-delay: 86400` + `prevent-exploit: true`，避免同日重复领奖。
- **账本统一**：`rewards.money` 经 Vault 写入 EssentialsX 单账本，与全服经济、南瓜币明细一致。
- **监控**：`/questadmin stats` 查看完成次数；配合 `/tax stats` 观察 Faucet/Sink 配比。

## 注意事项与排错

- 任务 ID 不含下划线；重命名 ID 会让玩家 `data/<UUID>.yml` 中的进度失配。
- `quests.yml` 保持 UTF-8；中文名称用单引号包裹。
- 任务未出现在 `/quests list` → 检查 `stages` 是否为空、ID 是否重复、`/questadmin reload` 是否执行。
- 奖励未到账 → 确认 Vault 与 EssentialsX 已启用，`rewards.money` 为正数。
- 重复冷却不生效 → 确认 `redo-delay` 写在任务层级（与 `rewards` 同级），**单位为秒**；写 `1440` 只有 24 分钟，24 小时必须写 `86400`。
- 与 Plan 网页端任务展示配合时，注意 Plan 5.9 扩展对 Quests 5.3.x 的方法签名要求；如遇玩家退出报 `NoSuchMethodError`，关闭 `Plan/config.yml` 的 `Quests.Enabled` 或应用对应兼容补丁。

---

## 26.3 测试服迁移落地（2026-11-08）

1.21.11 正式服备份里是 **LMBishop Quests 3.16.1** 的 8 个 `plugins/Quests/quests/daily*.yml`；26.3 装的是 **PikaMug Quests 5.3.2**。两者配置、数据、命令互不兼容，**不能直接复制目录**。已按 jar 内 `BukkitQuestYamlStorage` / `BukkitQuestFactory` 字节码实测的键名，重写为集中式 `plugins/Quests/storage/quests.yml`。

| 旧（LMBishop 3.x） | 新（Quests 5.3.2） |
| --- | --- |
| 每任务一个 yml，`tasks.<id>.type` | 单文件 `quests.<id>.stages.ordered.1.*` |
| `options.cooldown.time: 1440`（分钟） | `redo-delay: 86400`（秒） |
| `rewards: ["eco give {player} 40"]` | `rewards.money: 40`（走 Vault / EssentialsX 单账本） |
| `display.*` + `options.category: daily` | `name` + `gui-display.type`（无 category 概念） |

已迁移 7 个：`dailyminer` / `dailyhunter` / `dailyfisher` / `dailywoodcutter` / `dailycook` / `dailytamer` / `dailyfarmer`，合计 **310 南瓜币/天**。原 `default1~4` 官方示例已删除（`default1` 单次发 1000 南瓜币，是刷钱漏洞）。

三处语义收窄（插件能力差异，非配置疏漏）：

- `dailycook`：旧「熔炉烧炼任意物品 16 个」→ 新只能指定产物，固定 `IRON_INGOT x16`（`SMELT_ITEM` 只比对熔炉/烟熏炉 RESULT 槽取出的物品）。
- `dailytamer`：旧「任意可驯服生物 2 只」→ 新按生物类型分别累计，固定 `Wolf x2`；列多种会变成「每种都要达标」。
- `dailyfarmer`：旧「成熟小麦 16」→ 26.x 已无 block data / durability，`WHEAT x16` 不区分生长阶段。

`dailywalker`（步行 5000 格）**无法 1:1 迁移**：Quests 5.3.2 的 102 个配置键中没有任何累计距离目标（`share-distance` 是多人共享参数、`reach-location-*` 是抵达坐标）。`quests.yml` 末尾附了两个可直接取消注释的原生替代（吃 8 面包 / 插 16 火把），启用后 Faucet 回到 **350 南瓜币/天**；要精确「今日步行距离」需注册 `custom-objectives` 或由自研插件暴露 PAPI 占位符再走 `conditions.yml`。

### 每日限领网关（2026-11-10 新增）

Quests Classic **没有「每天从 N 个每日任务里选领 M 个」的机制**，`max-quests: 0` 表示同时持有任务数不限，因此玩家可一次接走全部每日任务。为落实「8 选 5」：

- 实跑 `quests.yml` 末尾已取消注释其一（插 16 火把），`name: '每日 · 远行者'`，作为第 8 个每日任务（日 faucet 上限回到 350/天，但实际每天仅可领 5 个）。
- 新增自研插件 **DailyQuestPick**（`自研/每日任务限领-DailyQuestPick/`）：在玩家与 Quests 之间加网关，按自然日（服务器 0 点）计数，每天最多领 `daily-limit: 5` 个；「领了即占槽、完成/放弃不退槽」。
  - 入口：`/dq take <id>`（菜单按钮）与直接 `/quests take <名称>`（经 `PlayerCommandPreprocessEvent` 拦截，绕过菜单也受限）。
  - 数据：`plugins/DailyQuestPick/data/<uuid>.yml`（`last-date` + `claimed-today`）。
  - 命令：`/dq`（查余量）、`/dq reset [玩家]`、`/dq reload`（均 `dailyquestpick.admin`）。
- QuickMenu `quests-daily` 菜单 8 个按钮的 `actions` 由 `[player] quests take <名称>` 改为 `[player] dq take <id>`，由网关统一计数。

配套把 `plugins/Quests/config.yml` 的 `language` 从 `en-US` 改为 `zh-CN`（jar 内 `lang/zh-CN/strings.yml` 与官方 `strings.yml` 同为 802 键，完整）。

**验证记录**：SnakeYAML 2.6 严格解析通过、UTF-8 正常；全部 stage 键在 102 键白名单内且列表长度配对；12 个 Material + 2 个 EntityType 用 Leaf `leaf-api-26.3.build.18-alpha.jar` 的 `javap` 确认存在；生物名经 `BukkitMiscUtil.getProperMobType` 大小写不敏感匹配。生效方式 `/questadmin reload` 或重启，随后 `/quests list` 应列出 8 个任务（每日经 **DailyQuestPick** 限领 5 个），接一个完成后核对 Vault 余额与 24 小时冷却。
