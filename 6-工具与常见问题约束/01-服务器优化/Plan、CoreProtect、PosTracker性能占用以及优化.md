# Plan、CoreProtect、PosTracker 性能占用以及优化

本文分析 Plan（Player Analytics）、CoreProtect、PosTracker 三个插件在**改用另一台服务器的 MySQL/MariaDB 数据库**时的性能占用、网络带宽影响，并给出完整的配置优化方案（含架构建议、连接参数、按插件调优、迁移注意事项）。

> **一句话结论**：带宽几乎不是瓶颈；真正的风险是「网络延迟 × 写入是否同步阻塞主线程」和「数据库单点依赖」。优先把数据库放在与 MC 服务器**同一机房的内网**，比任何配置调优都有效。

---

## 1. 现状盘点（三插件当前数据库状态）

| 插件 | 当前配置 | 现状 |
| --- | --- | --- |
| **Plan** | `Type: MySQL`，库名 `Plan`（本机 localhost:3306 / root / xxx，密码不写入文档） | ⚠️ MySQL 里**还没有建 `Plan` 库** → 实际降级运行在 SQLite（`plugins/Plan/database.db`，约 696KB 持续写入） |
| **CoreProtect** | 文档含「SQLite → MySQL」完整切换教程；若已切换则是本机模式 | 数据量/IO 三插件中最重 |
| **PosTracker** | `useDatabase: false`（文件模式，写 `positions.log`） | 每 3 秒/人 一条位置记录，写入频率最高 |

> 判断插件是否真的连上了 MySQL，不能只看配置：Plan 连接失败会**自动降级回 SQLite**（网页正常 ≠ MySQL 生效）；PosTracker 真实行为：`true` 走 MySQL、`false` 走文件。

---

## 2. 性能影响分析

### 2.1 带宽占用（估算）

假设在线 30 人、PosTracker 每条记录约 100B、CoreProtect 每条约 180B、Plan 写入约 50KB/h：

| 场景 | 估算带宽 | 备注 |
| --- | --- | --- |
| PosTracker（saveInterval=3s × 30 人） | ≈ 3.6 MB/h（≈1 KB/s） | 三插件中**持续写入频率最高** |
| CoreProtect 平时 | ≈ 3.2 MB/h（≈0.9 KB/s） | 破坏/放置/交互记录 |
| CoreProtect 活跃（30 条/s） | ≈ 19.4 MB/h（≈5.4 KB/s） | 集体建设/挖矿 |
| CoreProtect 极端（100 条/s） | ≈ 65 MB/h（≈18 KB/s） | 大规模战斗/建筑场景 |
| Plan 写入 | ≈ 0.05 MB/h | 会话/性能快照，几乎可忽略 |

**结论**：三插件合计峰值约 **90 MB/h（≈25 KB/s）**。千兆内网只占带宽的万分之一；即使走 10Mbps 公网也只用约 20%。**带宽不是瓶颈。**

> 真正的大流量是 **Plan Web 面板的查询**（打开仪表盘时按需读取大量数据），属于突发读取，不占持续带宽，但需要限制访问来源（见 4.2）。

### 2.2 网络延迟影响（RTT）

| 部署距离 | 单次往返延迟（典型参考） | 感知 |
| --- | --- | --- |
| 本机 localhost | < 0.1 ms | 无感 |
| 同机房内网 | 0.1 – 1 ms | 无感 |
| 同城 / 同地域 | 5 – 15 ms | 基本无感 |
| 公网跨地域 | 30 – 100 ms | 有感知 |
| 公网跨境 | 100 – 200 ms+ | 明显卡顿 |

**关键判断：写入是否阻塞主线程。** 若插件的数据库写入在 Bukkit 主线程同步执行，每次写入的 RTT 会直接变成游戏卡顿：

- **CoreProtect**：异步批量写（内部队列 + 合并 INSERT），远程延迟只影响「数据可见时间」，不卡服 ✅
- **Plan**：异步收集 ✅
- **PosTracker**：小插件，**是否异步写入未确认** ⚠️——若是同步 JDBC 写，`saveInterval: 3` 且远程 RTT 50ms，就是每 3 秒卡顿一次，需要实测确认（见 4.3）

### 2.3 数据库单点依赖

三个插件全部连远程库后，DB 服务器一旦宕机/抖动，三插件同时「失联」：

- **Plan**：自动降级回 SQLite 继续记录 → 造成**数据分裂**（MySQL 与 SQLite 两套数据，恢复后不自动合并）
- **CoreProtect**：写入队列积压 → 内存上涨
- **PosTracker**：写入失败可能**丢记录**（位置轨迹出现空洞）

---

## 3. 配置优化方案

### 3.1 架构层（优先级最高）

1. **同机房内网直连**：DB 与 MC 服务器放同一机房，RTT <1ms、带宽免费、无公网暴露——比任何调优都有效
2. **公网方案必须走隧道**：WireGuard / Tailscale / SSH 端口转发，**绝不把 MySQL 3306 直接暴露公网**（公网 MySQL = 被扫描/爆破重灾区）
3. **三库同实例**：`Plan` / `coreprotect` / `postracker` 三个 schema 放同一个 MySQL/MariaDB 实例；每个插件独立账号、host 限定 MC 服务器 IP、只授本库权限（`GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON <库>.*`），避免 `'%'`

### 3.2 连接串统一参数（三插件通用）

```
?rewriteBatchedStatements=true&useSSL=false&serverTimezone=Asia/Shanghai
```

- `rewriteBatchedStatements=true`：把批量 INSERT 合并成多值语句，远程写入吞吐明显提升（Plan 的 `Launch_options` 已带该项）
- `useSSL=false`：仅限内网/隧道环境；公网直连（不推荐）应改用 `useSSL=true&requireSSL=true`
- `serverTimezone=Asia/Shanghai`：避免时区错乱

### 3.3 按插件调优

| 插件 | 改动 | 理由 |
| --- | --- | --- |
| **PosTracker** | `saveInterval: 3 → 10`（`/pos interval 10` 可随时调）；**先实测写入是否异步** | 写入频率降 3 倍；10 秒间隔的轨迹密度对回溯/排查仍足够 |
| **CoreProtect** | 确认批量写入开启；定期 `/co purge t:180d` 归档旧数据；同实例多服时用不同 `table-prefix` 区分 | 控制表体积，回滚/查询更快 |
| **Plan** | `Max_connections: 8 → 3`；按需关闭 `Geolocations` / `Client_info`；Web 面板限内网或加反向代理 | 写入本就低频，省连接数；降低面板突发查询压力 |
| **数据库侧** | `innodb_buffer_pool_size` 给到库体积以上；`max_connections` 相应调大（三插件各 2–5 条连接）；开启慢查询日志 | 三库共用实例，缓冲区是关键 |

### 3.4 迁移注意事项

- **Plan**：SQLite 旧数据**不会自动迁移**到 MySQL，切库后从空库重新积累；旧数据保留在 `database.db`，切库前先备份
- **PosTracker**：文件模式（`positions.log`）转 DB **不迁移历史轨迹**，同样先备份
- **CoreProtect**：可用内置 `/co migrate-db mysql`（Patreon 版）或 dump 脚本迁移，迁移前备份整个 `plugins/CoreProtect/database.db`
- 切换远程库前建议：备份 → 挑维护窗口 → 重启服务器 → 用 `SHOW TABLES` / `/co lookup` 验证生效
