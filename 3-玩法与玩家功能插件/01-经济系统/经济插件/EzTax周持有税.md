# EzTax 周持有税

EzTax 是南瓜生存服经济体系的税务插件，作为**强制货币流动工具**征收周持有税、交易税、死亡费和闲置费，并支持按权限组分档税率。本文档说明配置文件、命令、权限与日常运维，适用于管理员对照配置、调整税率与排查启动问题。

## 版本与依赖

| 项目 | 值 |
|---|---|
| 插件版本 | **EzTax 2.0.0**（`EzTax-2.0.0.jar`） |
| API / 服务端 | `api-version: 26.1`，Leaf 26.3 可运行 |
| Java | **JDK 25** |
| 硬依赖 | **EzSeasons 2.1.2**（`EzSeasons-2.1.2.jar`） |
| 软依赖 | Vault（经济结算）、EzEconomy |
| 数据来源 | 26.3 测试服 `plugins/EzTax/config.yml` 实跑配置 |

> EzTax 2.0.0 在 bootstrap 阶段直接引用 `com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration`（EzSeasons 的 API 类）。缺 EzSeasons 会抛 `NoClassDefFoundError` 导致插件无法启用，两者必须同时安装。

## 功能定位

- **逼货币流动**：对高余额账户征收持有税，防止货币沉淀在少数账户中。
- **税款定向回收**：`tax-sink.destination` 决定税款去向——销毁、转入指定账户、均分在线玩家或执行控制台命令。
- **四类税种**：周/日持有税（存量）、交易税（流转）、死亡费、闲置费。
- **分档税率**：`group-taxes` 可按 Vault 权限组覆盖默认税率，未分组玩家走 `fallback-rate`。

## 配置文件

```
plugins/EzTax/
├── config.yml         # 税率、税款去向、存储（主配置）
├── exemptions.yml     # 免税名单
├── gui.yml            # 游戏内配置界面
├── messages.yml       # 消息文本（MiniMessage 格式）
└── stats.yml          # 累计税务统计（storage.type: yml 时生成）
```

配置修改后执行 `/tax reload` 生效，无需重启。

## 关键配置（当前值）

以下为 `config.yml` 的实跑取值：

| 配置段 | 键 | 值 | 说明 |
|---|---|---|---|
| `wealth-tax` | `enabled` | `true` | 持有税开关 |
| | `threshold` | `250000` | 余额超过 25 万南瓜币起征 |
| | `percentage` | `2.0` | 超出部分按 2% 征收 |
| | `interval` | `DAILY` | 每天征收一次（可选 `WEEKLY`） |
| | `brackets` | 注释示例 | 可选累进税率，按 `limit` / `percent` 分段 |
| `transaction-tax` | `enabled` | `true` | 交易税开关 |
| | `percentage` | `1.0` | 转账抽 1% |
| | `apply-on-withdraw` | `true` | 转出方承担 |
| | `apply-on-deposit` | `false` | 存入方不承担 |
| | `minimum-fee` | `1.0` | 最低手续费 |
| | `commands` | `[pay]` | 纳入交易税的命令 |
| | `capture-commands` | `false` | 走 Vault 经济包装，不走命令拦截 |
| `death-fee` | `enabled` | `true` | 死亡费开关 |
| | `percentage` | `0.5` | 死亡扣 0.5% |
| | `max-loss` | `5000` | 单次最多扣 5000 南瓜币 |
| | `disabled-worlds` | `[]` | 不豁免任何世界 |
| `inactivity-fee` | `enabled` | `true` | 闲置费开关 |
| | `days-inactive` | `30` | 连续 30 天未登录起征 |
| | `flat-fee` | `500` | 固定收取 500 |
| | `percentage` | `0.0` | 不按比例加收 |
| `group-taxes` | `enabled` | `false` | 分组税率当前关闭 |
| | `fallback-rate` | `1.0` | 无分组玩家的兜底税率 |
| `tax-sink` | `destination` | `player` | 税款转入指定账户 |
| | `target-player` | `PumpkinVegetable` | 收款账户 |
| `storage` | `type` | `yml` | 统计落在 `stats.yml`（可选 `mysql`） |

### 配置样例

```yaml
wealth-tax:
  enabled: true
  threshold: 250000       # 余额 > 25 万起征
  percentage: 2.0         # 超出部分 2%
  interval: DAILY

transaction-tax:
  enabled: true
  apply-on-withdraw: true
  apply-on-deposit: false
  percentage: 1.0
  minimum-fee: 1.0
  commands: [pay]
  capture-commands: false

death-fee:
  enabled: true
  percentage: 0.5
  max-loss: 5000
  disabled-worlds: []

inactivity-fee:
  enabled: true
  days-inactive: 30
  flat-fee: 500
  percentage: 0.0

tax-sink:
  destination: player
  target-player: 'PumpkinVegetable'
  command: ''

storage:
  type: yml
  file: stats.yml
```

### 税款去向（`tax-sink.destination`）

| 取值 | 行为 | 配套键 |
|---|---|---|
| `burn` | 税款直接销毁，纯 Sink，减少货币总量 | — |
| `player` | 转入指定账户（本服现行） | `target-player` |
| `pool` | 均分给当前所有在线玩家 | — |
| `command` | 执行控制台命令，`%amount%` 为税额占位符 | `command` |

> `server-treasury.enabled: true` 只控制**累计收税统计**，不改变税款去向；两者互不影响。

## 命令

主命令为 `/eztax`，`/tax` 是等价别名；另有 `/exempt`、`/transactiontax` 两个独立管理命令。

| 命令 | 用途 | 权限 |
|---|---|---|
| `/tax` | 打开税务主界面 / 帮助 | `eztax.command.use` |
| `/tax check` | 预览自己下次应纳税额 | `eztax.command.check` |
| `/tax stats` | 查看累计税务统计（`Total removed`） | `eztax.command.stats` |
| `/tax show` / `/tax fines` | 查看自己的税额 / 罚金明细 | `eztax.command.showtax` / `showfines` |
| `/tax pay` / `/tax payfine` | 补缴税款 / 罚金 | `eztax.command.paytax` / `payfine` |
| `/tax history [玩家]` | 纳税历史（查他人需额外权限） | `eztax.command.history` |
| `/tax sinks` | 查看当前 sink 配置 | `eztax.command.sinks` |
| `/tax config` | 打开游戏内配置 GUI | `eztax.command.config` |
| `/tax reload` | 重载配置 | `eztax.command.reload` |
| `/tax runwealthtax` | 手动触发持有税（测试用） | `eztax.command.runwealthtax` |
| `/tax runtaxpayment` | 手动触发交易税结算（测试用） | `eztax.command.runtaxpayment` |
| `/tax settaxrate [组]` | 按分组设置税率 | `eztax.command.settaxrate` |
| `/tax exempt` / `unexempt` / `exemptions` | 免税管理 | `eztax.command.exempt` / `unexempt` / `exemptions` |
| `/exempt <玩家>` | 免税（独立命令） | `eztax.command.exempt` |
| `/transactiontax <view\|setpercent\|setmin\|enable\|disable>` | 交易税配置（独立命令） | `eztax.command.transactiontax.view` / `manage` |

> 玩家日常用 `/tax check`、`/tax show`、`/tax pay`；运营用 `/tax stats`、`/tax reload`、`/tax runwealthtax`。

## 权限（LuckPerms）

权限节点统一为 `eztax.command.*` 体系：

| 节点 | 默认 | 说明 |
|---|---|---|
| `eztax.command.use` | true | `/eztax` 基础帮助 |
| `eztax.command.check` | true | 查自己税额 |
| `eztax.command.paytax` / `payfine` | true | 补缴税款 / 罚金 |
| `eztax.command.showtax` / `showfines` | true | 自己的税额 / 罚金明细 |
| `eztax.command.history` | true | 自己的纳税历史 |
| `eztax.command.stats` | op | 全局统计 |
| `eztax.command.sinks` | op | 查看 sink 配置 |
| `eztax.command.reload` | op | 重载配置 |
| `eztax.command.runwealthtax` / `runtaxpayment` | op | 手动触发征税 |
| `eztax.command.settaxrate` | op | 分组税率 |
| `eztax.command.transactiontax.view` / `manage` | op | 交易税查看 / 修改 |
| `eztax.command.exempt` / `unexempt` / `exemptions` | op | 免税管理 |
| `eztax.command.paytax.others` / `history.others` | op | 代他人缴税 / 查他人历史 |

> 普通玩家默认即可自助使用，管理员命令需要 op。`eztax.exempt`（若有）为永久免所有税，慎授。

## 存储与统计

- 默认 `storage.type: yml`，统计写入 `plugins/EzTax/stats.yml`。
- 改为 `mysql` 时填写 `host` / `port` / `database` / `user` / `password` / `table`，插件自动建表并迁移 schema。
- `/tax stats` 的 `Total removed` 是周 Sink 统计的监控项；切换 `tax-sink.destination` 不影响该累计值。

## 与经济体系的关系

- **Sink 项**：持有税（>25 万部分 2%，每日）+ 交易税（转出方 1%）+ 死亡费（0.5%，上限 5000）+ 闲置费（30 天未登录 500）。
- **税款去向**：本服现行 `player` 模式，税款转入 `PumpkinVegetable` 账户，属于**账户间转移**而非销毁；若需纯 Sink，把 `tax-sink.destination` 改为 `burn`。
- **监控**：`/tax stats` 输出计入周 Sink；调参时优先改 `wealth-tax.percentage` 或 `threshold`。
- **基岩端**：经 Vault 结算，Geyser/Floodgate 无特殊配置。

## 注意事项与排错

- 起征 25 万南瓜币，只影响高余额账户，普通玩家无感。
- CustomDeathMessages 的扣费保持禁用（`cost 0.0`），避免与 `death-fee` 重复扣款。
- 启动报 `NoClassDefFoundError: .../seasons/api/SeasonsIntegration` → 缺 **EzSeasons 2.1.2**，装上即可。
- 配置不生效 → `/tax reload`；仍不生效检查 `storage.type` 与 `messages.yml` 是否为 MiniMessage 格式。
- 分组税率不生效 → 确认 `group-taxes.enabled: true` 且 Vault 权限组名称与 `groups` 下的键一致。
