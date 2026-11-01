# CHANGELOG — PumpkinMail（南瓜邮箱）

## 2026-10-31 · 变更：待领取提醒改为「进入时提醒一次」

### 需求
原来上线提示一次 + 在线期间每 30 秒轮询提示一次，玩家嫌刷屏，改为**只在进入服务器时提醒一次**。

### 改动

`config.yml` 新增开关：

```yaml
hint:
  enabled: true            # 总开关；关闭后连上线提示也不会发
  on-join-only: true       # true = 只在玩家进入服务器时提醒一次，不再定时循环刷屏
  interval-seconds: 30     # 仅当 on-join-only: false 时生效；设为 0 同样表示只提醒一次
```

`HintTask`：
- 默认不再注册定时轮询任务（省掉每 30 秒对全部在线玩家的 `countPending` 查询）
- 上线提示改为**延迟 2 秒（40 tick）**发送，避开进服瞬间其他插件的 ActionBar 覆盖
- 抽出 `sendHint(Player)` 复用；数量为 0 时静默

配置组合行为：

| enabled | on-join-only | interval-seconds | 行为 |
|---|---|---|---|
| false | — | — | 完全不提示（连上线也不提示） |
| true | **true（默认）** | — | **只在进入时提醒一次** |
| true | false | 30 | 进入提示 + 每 30 秒循环提示 |
| true | false | 0 | 只在进入时提醒一次 |

### 顺带修复
`hint.enabled: false` 时**上线提示仍会触发** —— 原代码把 `registerEvents` 写在 enabled 判断外面，
开关只管住了定时轮询。现在总开关关闭后两者都不再执行。

### 注意
线上已有的 `plugins/PumpkinMail/config.yml` **不会被 jar 覆盖**，需手动加入 `on-join-only: true` 一行
（或删掉旧配置让其重新生成）。不加也能跑 —— 缺省值就是 true，行为已经是"只提醒一次"。

---

## 2026-10-31 · 修复：物品奖励领取失败 + 可重复领取漏洞

### 现象

玩家 `PumpkinVegetable` 点击邮箱里的物品奖励，后台刷屏：

```
[ERROR]: Could not pass event InventoryClickEvent to PumpkinMail v1.0.0
CommandException: Unhandled exception executing 'minecraft:give PumpkinVegetable DIAMOND 64'
Caused by: CommandSyntaxException: Unknown item 'minecraft:' at position 32: ...Vegetable <--[HERE]
```

同一条错误重复 4 次（玩家点了 4 下），界面无刷新、玩家无任何提示。

### 根因

三件事叠在一起：

1. **直接触发**：`commands.yml` 里把 `give` 别名强制指向原版 `minecraft:give`（`give: - minecraft:give $1-`）。
   原版资源名只接受 `a-z0-9_.-`，**大写是非法字符**。`DIAMOND:64` 这种写法解析时遇到 `D` 立即停止，
   物品 path 变空 → 拼出 `minecraft:` → `Unknown item 'minecraft:'`。
2. **放大器**：`RewardApplier.apply` 里 `dispatchCommand` **没有 try-catch**，
   `CommandException`（RuntimeException）一路抛穿 `claimReward`，导致后面的 `markClaimed()` /
   提示消息 / 界面刷新**全部被跳过** → 奖励永远停在"未领取"，玩家可以反复点击重复领取。
3. **隐患**：`data.split(":", 2)` 按第一个冒号切，遇到 `minecraft:diamond:64` 会把材质名切成 `minecraft`。

### 修复

| 项 | 改法 |
|---|---|
| 物品发放 | 不再拼 `give` 命令，改为 `player.getInventory().addItem()` 直接塞背包；塞不下的掉在脚边，不凭空消失 |
| 异常兜底 | `apply` 整体 try-catch，任何异常都吞掉并 `return false`，上层正常提示"领取失败"并刷新界面 |
| 命令执行 | `money` / `command` 类型走 `dispatch()` 包装，命令抛异常也返回 false |
| 物品解析 | 新增 `splitMaterialAndAmount()`：数量从**最后一个冒号**切，自动剥离 `minecraft:` 命名空间 |
| 图标解析 | `GuiManager.rewardIcon` 复用同一解析器，修掉同类 `split(":", 2)` bug |

改后 `apply` 返回 false 时，`claimReward` 会走到 else 分支：提示"领取失败，请联系管理员" + 刷新界面，
**不会再卡在未领取状态**，重复领取漏洞随之关闭。

### 支持的物品写法（已逐一实测）

| 输入 | 解析结果 |
|---|---|
| `DIAMOND:64` / `diamond:64` | 材质 DIAMOND，数量 64 |
| `minecraft:diamond:64` | 材质 diamond，数量 64 |
| `minecraft:diamond` | 材质 diamond，数量 1 |
| `golden_apple:2` | 材质 golden_apple，数量 2 |
| `DIAMOND` | 材质 DIAMOND，数量 1 |
| `DIAMOND:0` / `DIAMOND:999` | 数量截断到 1 / 物品最大堆叠 |
| `""` / `null` | null（解析失败） |

### 构建

无现成 build 脚本，按以下方式重建（依赖在 `lib/`，sqlite 类从 `_build/staging` 复用）：

```bash
find src/main/java -name "*.java" > /tmp/pm_srcs.txt
javac -encoding UTF-8 -nowarn -cp "lib/*" -d _build/classes_new @/tmp/pm_srcs.txt
# staging = classes_new + _build/staging/org（sqlite）+ src/main/resources
jar cfm PumpkinMail.jar /dev/null -C _build/staging_new .
```

产物 `PumpkinMail.jar`（约 12 MB，含内嵌 sqlite-jdbc 126 个类），旧版备份 `PumpkinMail.jar.bak`。

### 未改动

- 数据结构、数据库 Schema 均未变动，旧数据直接兼容
- `money` 类型仍走 `eco give` 命令，以保留南瓜账本（MoneyLedger）的来源备注挂钩

### 部署

替换 `plugins/PumpkinMail.jar` 后重启。玩家那条没领到的奖励仍是"未领取"，**重启后直接再点一次即可**（现在会成功）。
