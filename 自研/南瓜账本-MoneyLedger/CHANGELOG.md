# MoneyLedger 变更记录

## 2026-11-12 · 1.1.0：全量准确流水（事件加固 + trade.log 幂等回填）

**诊断依据**：本地主服 `ledger.db` 18240 条 + `Essentials/trade.log` 72126 行实测。

- **10-30 ~ 11-08 连续 10 天账本零记录**，同期 `trade.log` 有 5000+ 笔真实变动 → 单一事件源不可靠。
- `SourceDetector` 旧映射失效：Quests 5.3.2 包名已改 `me.pikamug.quests`，任务奖励全归 `OTHER`；`DailySell` 0 命中；PumpkinFly / Residence 归 `OTHER`；`/eco give` 来源被抓成 Brigadier 的 `context/ContextChain`。
- `stats()` 把总数当待领、`setReason(null)` 无法清除备注、命令发放来源无执行者。
- 写库走内存队列，`SQLException` 只 printStackTrace 后整批丢弃；`onBalanceUpdate` 用空 `catch(Throwable)` 吞掉所有异常。
- 金额存 double：`0.18000000000000682` 这类脏小数；715 条疑似名称兜底（复核后多为真实 8 位基岩名）。

**改动**

| 模块 | 内容 |
| --- | --- |
| `LedgerStore` | 默认同步落盘（WAL + NORMAL），移除内存队列；写失败 Base64 转存 `ledger-failed.log`，启动重放；旧库幂等加 6 列（`old_exact`/`new_exact`/`delta_exact`/`origin`/`dedup_base`/`dedup_key`）+ `backfill_state` 表；历史记录补去重键，原 10 字段不改写 |
| `TradeLogBackfiller`（新） | 解析 `trade.log` 的 `API,Add/Subtract` 与 `Command,Pay`（后者按相邻 `Update,Set` 还原付款人/收款人两笔余额）；自转账跳过；日志里写成 UUID 的名字用 `Essentials/userdata` 反查真实账号名；去重键 = 玩家+秒+金额+结果余额，按已处理行数增量，重复运行幂等；`min-date` 可限定范围 |
| `MoneyLedger` | `setReason(null)` 真正清除；监听命令事件记录「执行者: 命令」进 source（5 秒 TTL）；金额 BigDecimal 精确文本；名字优先按 UUID 读 `userdata` 的 `last-account-name`；异常记 severe 日志不再静默 |
| `SourceDetector` | 全栈扫描优先命中已知插件（Quests/DailySell/PumpkinFly/PumpkinMail/OfflineReward/BuildRate/QuickMenu/Residence/EconomyShop/AuctionHouse/EzTax），跳过 Vault / Brigadier / 命令框架；新增 `FLIGHT`/`RESIDENCE`/`BACKFILL`/`ADJUST` 类别 |
| `MoneyLedgerCommand` | 新增 `backfill [full]`、`verify`；`stats` 分来源计数；`export` 加 uuid/origin/精确金额列并做 CSV 转义；所有子命令校验 `moneyledger.admin` |

**产物与验证**

- `MoneyLedger-1.1.0.jar` 46194 字节 / md5 `7e28d69f`；项目 target、项目根、本地主服三份一致；主服旧 `1.0.0` 改名 `.bak-20261112`，`config.yml` 同步（旧版备份同名）。
- db 副本端到端：首启迁移 + 全量回填 15708 笔（含 `/pay` 214 笔）→ 33948 笔；增量与全量重跑均 0 新增；历史 18240 条原字段 0 改写；`dedup_key` 无重复、delta 全部自洽、负余额 0、对账缺口 0。
- 如实保留的缺口：35 条补记 UUID 为空（userdata 无档案，不猜）、2223 条独占 `Update,Set` 行（日志只给变动后余额，未编造变动前余额；运行期已由事件直记）、1169 处余额衔接不连续（早期直接 setMoney），均由 `/moneyledger verify` 报告。
- 未启服、未动 `ledger.db`；首次启动自动迁移与回填。多服 `leaf-26.3-多服` 与云端仍为 1.0.0。
