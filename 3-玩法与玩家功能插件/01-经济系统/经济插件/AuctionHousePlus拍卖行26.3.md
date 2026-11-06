# AuctionHousePlus 拍卖行（v4.0）

## 一、功能定位

- **主交易渠道**：玩家间买卖物品的主要场所，替代系统回收（`/sell` 关闭后）。
- **跨端原生支持**：`softdepend: [LuckPerms, floodgate]`，jar 内带 `GeyserFormManager`，基岩端（Geyser/Floodgate）走原生表单，不依赖 Java 客户端 GUI。
- **可选增强**（默认关闭，按需开启）：网页端（会话/账户两种模式）、Velocity 跨服共享、Discord 推送。
- **纯拍卖形式**：玩家上架物品并设价，其他玩家按一口价购买（4.0 默认模板无竞价，走即时购买）。

## 二、版本与来源

| 项          | 值                                                                       |
| ----------- | ------------------------------------------------------------------------ |
| 插件名      | `AuctionHouse`（4.0 / AuctionHousePlus）                                 |
| 主类        | `com.auctionhouse.AuctionHouse`                                          |
| api-version | `1.21`（适配 Leaf 1.21.11 / 26.3）                                       |
| 作者        | Mateitaa1                                                                |
| 依赖        | `Vault`（**必需**，经济结算走它）                                        |
| 软依赖      | `LuckPerms`（限额按组）、`floodgate`（基岩表单）                         |
| 数据        | 默认 SQLite（`plugins/AuctionHouse/auctions.db`），Velocity 模式需 MySQL |

## 三、⚠️ 与 1.1.4 的核心差异（升级前必读）

### 差异 1：经济模型（最重要）

| 维度      | 1.1.4（iliüs，线上现状）                            | 4.0（AuctionHousePlus，测试目标）                              |
| --------- | --------------------------------------------------- | -------------------------------------------------------------- |
| 上架费    | `listing-fee: 30.0` **固定上架费，卖出/流拍均不退** | **无固定上架费**                                               |
| 成交税    | `sales-tax: 5%`（卖家到手扣）                       | `enable-tax: true` + `tax-rate: 0.05`（成交抽 5%）             |
| 取消/过期 | 上架费不退                                          | `refund-tax-on-cancel: true`：**取消或过期时退还 listing tax** |

**影响**：

- 旧版的"上架即扣 30 南瓜币不退"是一笔**确定性 Sink（货币回收）**，用于抗通胀。升级 4.0 后这笔 Sink **消失**，只剩成交时 5% 的税。
- 玩家上架零成本、取消零损失，上架体验更友好，但**抗通胀能力减弱**。
- 若想保留强回收，需另靠 `EzTax 周持有税`（同目录已有文档）等互补机制，而不是指望拍卖行。

### 差异 2：限额模型

- 1.1.4：`listing-limits.default: 20`（单一默认值）。
- 4.0：`max-listings-per-player: 10`（兜底）+ **`rank-limits` 按 LuckPerms 组限额**（default/member/vip/.../admin）。需要 LuckPerms 才生效分组限额。

### 差异 3：货币显示

- 1.1.4：`currency-format: '{amount} 南瓜币'`。
- 4.0：`currency-symbol: "$"`（仅符号前缀）+ `price-format`（数字格式 COMMAS/SHORT/RAW）。本服部署时**必须改成南瓜币文本**（见第五节）。

### 差异 4：新增能力

- 网页端（session 会话链接 / web-expansion 账户两种）、Velocity 跨服拍卖共享、Discord 实时推送、价格格式 SHORT(`1B`) 等。

---

## 四、命令

| 命令                       | 别名              | 用途                                                                               | 权限                                        |
| -------------------------- | ----------------- | ---------------------------------------------------------------------------------- | ------------------------------------------- |
| `/auctionhouse`            | `/ah`             | 主命令：`/ah [sell\|cancel\|expired\|listings\|help\|reload\|limit\|web\|session]` | `auctionhouse.use`                          |
| `/ah sell <价格>`          | —                 | 手持物品上架，设一口价                                                             | `auctionhouse.sell`                         |
| `/ah cancel`               | —                 | 取消自己的在售物品                                                                 | `auctionhouse.cancel`                       |
| `/ah expired`              | —                 | 取回已过期的物品                                                                   | `auctionhouse.expired`                      |
| `/ah listings`             | —                 | 查看自己的在售/待领取                                                              | `auctionhouse.use`                          |
| `/ah limit`                | —                 | 查看当前上架额度                                                                   | `auctionhouse.use`                          |
| `/ah reload`               | —                 | 重载配置（管理员）                                                                 | `auctionhouse.reload`（op）                 |
| `/ah web` / `/ah session`  | —                 | 生成网页访问链接（需开 sessions/web）                                              | `auctionhouse.web` / `auctionhouse.session` |
| `/ahplus`                  | —                 | 网页命令：`[verify\|web\|status\|claim\|pending\|session]`                         | `auctionhouse.web`                          |
| `/auctionhouselink <code>` | —                 | 绑定 Discord 账号                                                                  | `auctionhouse.use`                          |
| `/vah`                     | `vahweb`, `ahweb` | 打开网页接口：`[session\|help]`                                                    | `auctionhouse.web`                          |

> 玩家日常只用 `/ah`（开界面后点按钮即可上架/购买/取回），命令行仅 `/ah sell <价>` 最常用。

## 五、权限

| 权限                        | 默认      | 说明                                   |
| --------------------------- | --------- | -------------------------------------- |
| `auctionhouse.*`            | —         | 全部，含 admin（admin 子项默认 false） |
| `auctionhouse.use`          | **true**  | 使用拍卖行                             |
| `auctionhouse.sell`         | **true**  | 上架                                   |
| `auctionhouse.cancel`       | **true**  | 取消自己在售                           |
| `auctionhouse.expired`      | **true**  | 取回过期物品                           |
| `auctionhouse.web`          | **true**  | 网页功能                               |
| `auctionhouse.session`      | **true**  | 生成网页会话链接                       |
| `auctionhouse.crossbuy`     | **true**  | 跨服购买（Velocity 模式）              |
| `auctionhouse.crosssell`    | **op**    | 跨服出售（Velocity 模式）              |
| `auctionhouse.admin`        | **op**    | 管理全部上架                           |
| `auctionhouse.bypass.limit` | **false** | 突破上架限额                           |
| `auctionhouse.bypass.tax`   | **false** | 免成交税                               |
| `auctionhouse.reload`       | **op**    | 重载配置                               |

> 普通玩家默认即可用，无需授权；仅 admin/reload/bypass 需 op 或显式授予。

## 六、关键配置（4.0 默认 `config.yml` + 本服需改项）

> 以下为 4.0 **默认值**。带「🔧 本服需改」的是上线前必须按本服情况调整的项。

| 键                        | 默认                      | 说明                                              | 本服处理                                                          |
| ------------------------- | ------------------------- | ------------------------------------------------- | ----------------------------------------------------------------- |
| `prefix`                  | `&8[&6AuctionHouse&8] &r` | 消息前缀                                          | 可改成南瓜主题                                                    |
| `currency-symbol`         | `$`                       | 货币符号前缀                                      | 🔧 改 `南瓜币 ` 或 `¤`（配合 price-format 显示如 `南瓜币 1,000`） |
| `enable-tax`              | `true`                    | 是否抽成交税                                      | 保持 true                                                         |
| `tax-rate`                | `0.05`                    | 成交税率 5%                                       | 保持 5%（如需更强回收可调，但注意无上架费 Sink）                  |
| `refund-tax-on-cancel`    | `true`                    | 取消/过期退 tax                                   | 保持 true（对玩家友好）                                           |
| `listing-duration-hours`  | `48`                      | 上架时长（小时）                                  | 可按需调（如 24/72）                                              |
| `max-listings-per-player` | `10`                      | 兜底限额                                          | 配合 rank-limits                                                  |
| `price-format`            | `COMMAS`                  | 数字格式：`COMMAS`(1,000)/`SHORT`(1B)/`RAW`(1000) | 推荐 COMMAS                                                       |
| `rank-limits`             | 按组 3~50                 | LuckPerms 组分限额                                | 🔧 按本服真实组名映射（default/member/vip/...）                   |
| `server-name`             | `survival`                | Velocity 跨服唯一标识                             | 🔧 仅跨服时改唯一名                                               |
| `table-prefix`            | `ah`                      | 数据库表前缀                                      | 默认即可                                                          |

限额分组（`rank-limits`）默认示例（需 LuckPerms 组匹配才生效）：

```yaml
rank-limits:
  default: 3 # 默认组
  member: 3
  vip: 5
  premium: 8
  legend: 12
  hero: 15
  titan: 20
  admin: 50
```

## 七、汉化说明（4.0 与 1.1.4 不同）

- **1.1.4**：语言文件在 `lang/zh_CN.yml`（同目录已有 `AuctionHouse汉化/zh_CN.yml`）。
- **4.0**：语言直接在根目录 **`messages.yml`** 编辑（jar 内自带英文版，首次运行释放到 `plugins/AuctionHouse/messages.yml`）。**旧的 `lang/zh_CN.yml` 不适用于 4.0**。
- 汉化方式：编辑 `messages.yml` 里 `general / sell / notifications / settings / sort / ...` 各节点的英文为中文即可；占位符 `{prefix}` `{item}` `{amount}` `{price}` `{tax}` `{currency}` `{buyer}` `{count}` `{current}` `{max}` `{rate}` 保留。
- 基岩端（Geyser 表单）文本同样走 `messages.yml`，无需单独处理。

## 八、新特性（默认均关闭，按需开启）

1. **网页端（sessions / web-expansion）**
   - `sessions.enabled: true` + `sessions.base-url`：生成**临时会话链接**（默认 20 分钟有效），玩家点链接在浏览器买卖，免注册、较安全。
   - `web-expansion.enabled: true`：传统**账户制**网页（需 `web-config.yml` 配端口/域名/主题）。
   - 相关命令：`/ah web`、`/ah session`、`/ahplus`、`/vah`。
2. **Velocity 跨服共享**
   - `velocity.enabled: true` + MySQL 配置：多服共用同一拍卖池，玩家可跨服买（`crossbuy`）/跨服卖（`crosssell`）。
   - 每台服 `server-name` 必须唯一。
3. **Discord 集成**
   - `discord.enabled: true` + bot-token / 频道 ID：实时推送在售列表与成交日志，支持 `/auctionhouselink` 绑定账号取回离线交易。

> 单服、不打算上网页/跨服/Discord 时，这三项保持默认 `false` 即可，不影响核心拍卖。

## 九、文件与数据库

```
plugins/AuctionHouse/
├── config.yml          # 主配置
├── messages.yml        # 语言（4.0 汉化改这里）
├── web-config.yml      # 网页端配置（开 web 时）
├── auctions.db         # SQLite 拍卖数据（默认）
├── guis/               # GUI 布局
└── ...
```

- 默认 SQLite；Velocity 模式改用 MySQL（配置在 `velocity.database`）。
- 表前缀 `ah`，避免与其他插件冲突。

## 十、基岩端（Geyser/Floodgate）

- `softdepend: floodgate`，jar 内含 `GeyserFormManager`：基岩玩家自动走原生表单界面，与 Java 的箱子 GUI 等价。
- 无需额外配置；确保 Geyser/Floodgate 已装且优先加载即可。

## 十一、升级注意事项（1.1.4 → 4.0）

1. **经济模型变化**：上架费 Sink 消失，只剩成交税 5%。升级前评估通胀影响，必要时用 `EzTax` 等补充回收。
2. **配置不兼容**：1.1.4 的 `listing-fee` / `sales-tax` / `currency-format` / `listing-limits` 在 4.0 不存在或改名（`tax-rate` / `currency-symbol` / `rank-limits`）。**不要直接沿用旧 config.yml**，用 4.0 默认配置按本服重填。
3. **数据迁移**：`auctions.db` 结构可能变化。升级前**备份**旧 `auctions.db`；升级后旧在售单可能需手动取回或丢失。建议低峰期、清空在售后切换。
4. **汉化文件**：旧 `lang/zh_CN.yml` 无效，改编辑 4.0 的 `messages.yml`。
5. **限额**：4.0 限额依赖 LuckPerms 组名，确认本服组名与 `rank-limits` 对应。
6. **依赖**：确保 `Vault` 在；`LuckPerms` / `floodgate` 按需。

## 十二、运营建议

- 货币显示：把 `currency-symbol` 设为南瓜币文本，`price-format: COMMAS`，保持与全服一致的「南瓜币」观感。
- 防通胀：因 4.0 无上架费，把成交税 `tax-rate` 设为 5% 仅是弱回收；强回收靠 `EzTax 周持有税`（同目录文档）。
- 限额：用 `rank-limits` 给 VIP 更高额度，普通玩家靠 `max-listings-per-player` 兜底。
- 监控：把成交税计入周 Sink 统计（同经济监控流程）。
