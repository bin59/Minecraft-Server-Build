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

- **合规 Faucet**：**14 个每日任务按星期轮换**，每天放出其中 6 个（相邻两天零重叠），**全天合计约 250~260 南瓜币**（周一~周四、周六 260；周五、周日 250），计入周 Faucet 统计。
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

### 本服每日任务清单（14 个，按星期轮换）

每个任务固定出现在一周中的 3 天（快捷菜单 `days:` 条件），**每天恰好 6 个可选、相邻两天零重叠**：

| 任务 ID | 名称 | 条件 | 条件类型（编辑器） | 奖励 | 出现日 |
|---|---|---|---|---|---|
| `dailyminer` | 每日挖矿人 | 铁矿石 8 + 深板岩铁矿石 8 | Break blocks | 50 | 一/三/五 |
| `dailyhunter` | 每日猎手 | 击杀僵尸 10 | Kill mobs | 50 | 二/四/六 |
| `dailyfisher` | 每日渔夫 | 钓鱼 5 条 | Catch fish | 40 | 三/五/日 |
| `dailywoodcutter` | 每日伐木工 | 砍橡木原木 16 | Break blocks | 40 | 四/六/一 |
| `dailycook` | 每日厨师 | 熔炉取出铁锭 16 | Smelt items | 40 | 五/日/二 |
| `dailytamer` | 每日驯养师 | 驯服狼 2 只 | Tame mobs | 50 | 六/一/三 |
| `dailyfarmer` | 每日农夫 | 收获小麦 16 | Break blocks（WHEAT） | 40 | 日/二/四 |
| `dailywalker` | 每日远行者 | 放置火把 16 | Place blocks | 40 | 一/三/五 |
| `dailyquarrier` | 每日采石匠 | 挖石头 16 + 深板岩 16 | Break blocks | 40 | 二/四/六 |
| `dailywarrior` | 每日讨伐者 | 击杀骷髅 10 | Kill mobs | 40 | 三/五/日 |
| `dailyshepherd` | 每日牧羊人 | 剪白色绵羊 5 只 | Shear sheep | 40 | 四/六/一 |
| `dailymilker` | 每日挤奶工 | 挤奶 3 头 | Milk cows | 40 | 五/日/二 |
| `dailycraftsman` | 每日工匠 | 制作箱子 4 个 | Craft items | 40 | 六/一/三 |
| `dailyenchanter` | 每日附魔师 | 附魔铁剑 2 把 | Enchant items | 50 | 日/二/四 |

全部任务 `redo-delay: 86400`，奖励 `money` 走 Vault。每日组合：周一（挖矿/驯养/伐木/远行/牧羊/工匠）、周二（猎手/厨师/农夫/采石/挤奶/附魔）、周三（渔夫/挖矿/驯养/远行/讨伐/工匠）、周四（伐木/牧羊/猎手/采石/农夫/附魔）、周五（厨师/挤奶/渔夫/讨伐/挖矿/远行）、周六（驯养/工匠/伐木/牧羊/猎手/采石）、周日（农夫/附魔/厨师/挤奶/渔夫/讨伐）。

> 原版 `dailywalker`（步行 5000 格）在 Quests Classic 中无累计距离目标，现为「放置火把」替代（见上表）；要精确步行距离需 Conditions + PlaceholderAPI 占位符走 `conditions.yml`。

### 已验证的 stage 键

以下键名全部经 `BukkitQuestYamlStorage` 字节码（javap）实测确认读取方式：

| 键 | 读取方式 | 用途 | 示例 |
|---|---|---|---|
| `break-block-names` / `break-block-amounts` / `break-block-durability` | 字符串/整数/整数列表 | 破坏指定方块 | `IRON_ORE` + 数量 |
| `place-block-names` / `place-block-amounts` / `place-block-durability` | 同上 | 放置指定方块 | `TORCH` + `16` |
| `mobs-to-kill` / `mob-amounts` | 生物名/整数列表 | 击杀生物 | `Zombie` / `10` |
| `mobs-to-tame` / `mob-tame-amounts` | 生物名/整数列表 | 驯服生物 | `Wolf` / `2` |
| `fish-to-catch` | 整数 | 钓鱼数量 | `5` |
| `items-to-smelt` / `items-to-craft` / `items-to-enchant` / `items-to-consume` | **ItemStack 列表**（`==: org.bukkit.inventory.ItemStack` + `type` + `amount`） | 熔炼取出 / 合成 / 附魔 / 食用 | `IRON_INGOT` x16 |
| `cows-to-milk` | **纯整数**（getInt，非列表） | 挤奶头数 | `3` |
| `sheep-to-shear` / `sheep-amounts` | **颜色字符串列表**（DyeColor 名）/ 整数列表 | 剪指定颜色绵羊 | `WHITE` / `5` |
| `death-event` / `start-event` / `finish-event` | 字符串 | 绑定 `actions.yml` 中的事件动作 | `DeathFail` |

编辑器内另有 Brew potions（`items-to-brew`，同为 ItemStack 列表）/ Damage blocks / Use blocks（`use-block-*`）/ Cut blocks（`cut-block-*`）等类型可用。

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
| `confirm-accept` | `false` | **2026-11-10 关闭**：QuickMenu 按钮点一下直接接任务，不再要求聊天栏打字二次确认 |
| `confirm-abandon` | `true` | 放弃任务仍需二次确认（防误点） |
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

- **Faucet 项**：14 个任务按星期轮换，每天 6 个可选、全天约 250~260 南瓜币，计入周 Faucet 统计。
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

### 每日轮换（2026-11-10 重做，弃用 DailyQuestPick）

Quests Classic **没有「每天从 N 个任务里选领 M 个」的原生机制**（`max-quests: 0` 只是同时持有数不限）。最初为此做了自研限领网关插件 DailyQuestPick（`/dq take` + 拦截 `/quests take`），当晚即弃用并删除——改为**任务本身按星期轮换**，不需要任何中间插件：

- `quests.yml` 扩到 **14 个任务**（上表），全部 `redo-delay: 86400`。
- **轮换由 QuickMenu 实现**（`自研/快捷菜单系统-QuickMenu/`）：菜单项新增 `days:` 星期条件（如 `days: [MON, WED, FRI]`），按服务器本地日期在 Java 箱子界面与基岩原生表单**同步过滤**，跨天后重新打开菜单即更新，无需 reload。
- 快捷菜单 `quests-daily` 的按钮直连 Quests 原生命令 `/quests take <显示名>`（连写名如 `每日挖矿人`），不再经过任何网关。
- 每个任务一周出现 3 天：**每天恰好 6 个可选、相邻两天零重叠、全天 Faucet 约 250~260**（周五/周日 250，其余 260——数学上 14×3 配对结构无法做到每天严格相等，除非改动已实跑确立的原任务奖励，故保留 10 币星期级微差）。
- **边界说明**：`/quests list` 会列出全部 14 个任务，`/quests take <名>` 在任务「不在当日轮换」时也能接取——这是 Quests 原生行为，菜单轮换只控制展示入口，属预期而非漏洞；日常玩家经菜单领取即受轮换约束。

配置要点：

- `plugins/Quests/config.yml` 的 `max-quests` 保持 `0`（只约束同时持有数，与轮换无关）。
- 任务名保持**无空格连写**（`/quests take` 只允许 1 个任务名参数）。
- 要调整某任务的出现日：改 QuickMenu `config.yml` 里 `quests-daily.items.<任务ID>.days`（保持「同日可见任务槽位不冲突、每天 6 个」的约束），`/qm reload` 生效；任务目标/奖励改 `quests.yml` 后 `/questadmin reload`。

**验证记录**（SnakeYAML 2.2 Java 校验器，`/.workbuddy/quest_validate/`）：`quests.yml` 14 个任务全部通过（名称唯一 / redo-delay 86400 / stage 键在 102 键白名单 / 列表长度配对）；三份 QuickMenu `config.yml` 通过（14 个 `days:` 按钮 / 每天恰好 6 个可见 / 无槽位冲突 / 相邻日零重叠 / 奖励合计 250~260）。生效方式 `/questadmin reload` + `/qm reload`（或重启），随后 `/quests list` 应列出 **14 个任务**，菜单每天显示 6 个且与上表出现日一致。
