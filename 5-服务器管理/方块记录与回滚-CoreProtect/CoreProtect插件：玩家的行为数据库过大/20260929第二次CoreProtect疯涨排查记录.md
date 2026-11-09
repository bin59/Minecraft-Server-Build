# CoreProtect 库「疯涨」技术排查记录

| 项目 | 内容 |
|---|---|
| 排查日期 | 2026-09-29 |
| 服务器 | 腾讯云广州 xxx.xxx.x.x（/data 20G 云硬盘 lhdisk-xxxx） |
| 数据库 | MariaDB / 库 `coreprotect` |
| 触发问题 | `co_entity` 体积异常膨胀，/data 使用率 97%（剩余 588M） |
| 结论 | **非脏数据，是挂机装置导致的真实写入暴增 + 长期未回收的表碎片** |
| 处置 | 已 OPTIMIZE 回收；自动清理任务经用户指示**已关闭** |
| 最终状态 | /data 剩余 **8.0G（58%）** |

---

## 一、问题现象

用户提问：「co_entity 为什么这么多，上次都没有这么多，是不是有脏数据」

排查起点（17:20 前后）：

| 分区 | 已用/总 | 剩余 | 使用率 |
|---|---|---|---|
| /（系统盘 vda2 40G） | 32G | 6.4G | 84% |
| **/data（数据盘 vdb 20G）** | 18G | **588M** | **🔴 97%** |

`co_entity` 表规模：

| 指标 | 数值 |
|---|---|
| 行数 | 1,110,814 |
| 逻辑体积 | 3,281 MB |
| **物理文件 co_entity.ibd** | **6.7 GB** |
| 记录时间跨度 | 09-28 10:55:01 → 09-29 17:14（仅 30 小时） |
| 索引体积 | **0 MB**（仅 PRIMARY(rowid)，无 time 索引） |

---

## 二、排查过程与关键发现

### 2.1 排除「脏数据」

依次验证：

| 检查项 | 结果 |
|---|---|
| 按玩家分布 | ❌ 无法查——`co_entity` 只有 `rowid/time/data` 三列，**无 user 列** |
| 按实体类型分布 | ❌ 无法查——**无 entity 列**，实体种类在独立的 `co_entity_map`（仅 15 条） |
| `data` 内容 | Java 序列化 NBT blob，每行约 2.2KB，内容为 `max_health`/`movement_speed`/`armor` 等实体属性 |
| 行内容重复性 | 无重复模式，每行均含真实实体属性 |

**结论：不是脏数据，是真实写入。**

补充澄清了一个常见误解：Minecraft 的**掉落物/箭/船本来就不在 `co_entity` 里**。击杀记录在 `co_block action=2`（杀掉什么生物），死亡在 `action=3`。CoreProtect 官方仅把 `data` 原样回吐给客户端渲染。

### 2.2 定位疯涨时段

由于 `co_entity` 无 time 索引，直接 `GROUP BY time` 需全表扫 3.3GB（实测跑 2 分钟无结果、60s 超时）。**改用主键分桶法绕开**——利用 `rowid` 与 `time` 正相关，按 `rowid` 每 10 万行分桶统计时间跨度：

| rowid 区间 | 行数 | 时间跨度 | 速率 |
|---|---|---|---|
| 3500000 | 90,902 | 09-28 10:55 ~ 09-29 00:20（13.4 小时） | 1.9 行/秒 |
| 3600000 | 100,000 | 09-29 00:20 ~ 01:24（65 分钟） | **25.8 行/秒** |
| 3700000 | 100,000 | 09-29 01:24 ~ 02:43（79 分钟） | 21.1 行/秒 |
| 3800000 | 100,000 | 09-29 02:43 ~ 03:48（65 分钟） | 25.7 行/秒 |
| 3900000 | 100,000 | 09-29 03:48 ~ 04:53（65 分钟） | 25.6 行/秒 |
| 4000000 | 100,000 | 09-29 04:53 ~ 05:59（65 分钟） | 25.5 行/秒 |
| 4100000 | 100,000 | 09-29 05:59 ~ 07:02（63 分钟） | 26.4 行/秒 |
| 4200000 | 100,000 | 09-29 07:02 ~ 08:04（62 分钟） | 27.1 行/秒 |
| 4300000 | 100,000 | 09-29 08:04 ~ 09:05（62 分钟） | 27.0 行/秒 |
| 4400000 | 100,000 | 09-29 09:05 ~ 10:06（61 分钟） | 27.3 行/秒 |
| 4500000 | 100,000 | 09-29 10:06 ~ 11:06（60 分钟） | 27.6 行/秒 |
| 4600000 | 20,043 | 09-29 11:06 ~ 17:27（6.3 小时） | **0.9 行/秒** |

**分钟级细化**（rowid 3590000~3615000）确认突增起点：

| 时刻 | 行数/分钟 |
|---|---|
| 09-28 23:30 | 3,326 |
| 09-28 23:40 | 7,515 |
| 09-28 23:50 | 12,273 |
| 09-29 00:00 | 15,054 |
| 09-29 00:10 | 14,723 |
| 09-29 00:20 起 | **稳定 1,400~1,600 / 分钟（≈ 25 行/秒）** |
| 09-29 11:07 起 | 掉回 0.9 行/秒 |

**判定：09-29 00:20 起突增，连续 10 小时速率恒定在 25~27 行/秒、毫无波动 —— 这不是玩家在玩，是挂机装置在持续产出实体。**

### 2.3 关联表同步异常

**`co_container`**（容器交互）：

| 时段 | 速率 |
|---|---|
| 09-28 12:00 ~ 23:00 | 数百 ~ 7 万/小时 |
| **09-29 00:00 ~ 11:00** | **10~16 万/小时** |
| 09-29 12:00 后 | 回落 ~5 千/小时 |

来源分析：**91% 为伪用户写入**（非真人）

| 来源 | 次数 | 性质 |
|---|---|---|
| `#hopper` | 6,551 | 漏斗自动传输 |
| `#dispenser` | 662 | 发射器自动运作 |
| 真人（17 个账号） | 1,323 | — |

主因物品 `minecraft:minecart`（5,835 次）—— 矿车在漏斗轨道上循环。`#hopper` 全部集中在 09-29 16:28~17:29。

**`co_block`**：同窗口从常态 10 万/小时升至 26.8 万/小时，但**84% 为真人玩家 `j4rvx` 的 83,243 次操作**（3,260 个不同秒），属正常活跃，非异常。

### 2.4 发现表碎片（关键）

| 表 | 逻辑体积 | 物理文件 | 碎片 |
|---|---|---|---|
| co_entity | 3,281 MB | **6.7 GB** | 3.4 GB |
| co_block | 320 MB | 3.5 GB | 3.2 GB |
| co_container | 219 MB | 1.2 GB | 1.0 GB |
| 合计 | 3.8 GB | **11.4 GB** | **7.6 GB** |

**11.4GB 物理空间里只有 3.8GB 是有效数据** —— 大量空间是历史 DELETE 后未回收的空闲页。

---

## 三、处置与结果

### 3.1 执行的 OPTIMIZE

```bash
mysql --defaults-file=/opt/mcstats/.my.cnf -e \
  "OPTIMIZE TABLE coreprotect.co_entity, coreprotect.co_container, coreprotect.co_block;"
```

执行日志出现一条**易被忽略的错误**：

```
coreprotect.co_entity  optimize  error   The table 'co_entity' is full
coreprotect.co_entity  optimize  status  Operation failed
coreprotect.co_container optimize status OK
coreprotect.co_block     optimize status OK
```

**复盘结论：本次 co_entity 的 OPTIMIZE 其实是失败的。** 原因是**磁盘剩余空间不足**——`OPTIMIZE` 对 InnoDB 实为「重建表 + 分析」，需要额外空间存放副本（6.7GB 的表在只剩 588M 的盘上必然 `full`）。释放出的 4.0GB 实际来自 `co_block`（3.5G→332M）与 `co_container`（1.2G→232M）两张表的成功重建。

告知用户时曾表述为「三张大表已回收」，**该说法不准确，特此更正**：仅 co_container 与 co_block 成功。

### 3.2 空间回收结果

| 表 | 逻辑 | 物理文件 | 状态 |
|---|---|---|---|
| co_entity | 3,281 MB | 6.7G → **3.4G** | 🟡 磁盘充足后补跑成功 |
| co_block | 320 MB | 3.5G → **332M** | 🟢 |
| co_container | 219 MB | 1.2G → **232M** | 🟢 |

**关键教训：磁盘快满时 OPTIMIZE 大表会因空间不足失败。** 正确顺序是**先 OPTIMIZE 小表腾出空间，再处理最大的表**；或先清理数据再 OPTIMIZE。本次「co_entity 未回收 → 腾出 4G」后，磁盘宽裕时补跑 `OPTIMIZE TABLE coreprotect.co_entity;` 返回 **status OK**，物理文件 6.7G 降至 3.4G。

### 3.3 最终状态

| 分区 | 已用/总 | 剩余 | 使用率 |
|---|---|---|---|
| **/data** | 11G/20G | **8.0G** | **🟢 58%**（原 588M / 97%） |
| /（系统盘） | 32G/40G | 6.4G | 🟡 84% |

**净回收约 7.4GB**（588M 剩余 → 8.0G 剩余）。

---

## 四、数据保护措施

清理前对**体量小但属纯运营数据**的 5 张表做了备份：

| 文件 | 内容 |
|---|---|
| `/data/backup/coreprotect_20260929/cp_core_tables.sql`（159K） | co_session / co_chat / co_command / co_sign / co_username_log |

**已验证可还原**（试导入临时库 `cp_restore_test` 后核对行数并清理）：

| 表 | 行数 |
|---|---|
| co_chat | 666 |
| co_session | 1,618 |
| co_command | 464 |

清理范围严格限于膨胀的 `co_entity` / `co_container` / `co_block` 三张表；**会话、聊天、指令、告示牌作为运营数据一律不删**。

---

## 五、自动清理任务（已按用户指示关闭）

排查中曾创建每日清理任务，**后经用户明确要求「自动清理关闭」，已删除**：

```bash
sudo rm -f /etc/cron.d/cp-burst-cleanup
```

关闭后复查确认无残留：

| 检查项 | 结果 |
|---|---|
| `/etc/cron.d/cp-burst-cleanup` | 🗑️ 已删除 |
| root / ubuntu crontab | 🟢 无引用 |
| `/etc/cron*` 全量搜索 | 🟢 无 `cp_burst_cleanup` 引用 |

**保留但未启用**：

| 文件 | 用途 |
|---|---|
| `/home/ubuntu/scripts/cp_burst_cleanup.sh` | 手动执行：`sudo bash cp_burst_cleanup.sh [天数]`（分批 5 万行 + READ-COMMITTED 防死锁 + flock 单实例锁） |
| `/data/backup/coreprotect_20260929/` | 运营数据备份 |

> **风险提示（如实告知）**：自动清理关闭后，`co_entity` 将重新累积。上次爆发速率 25~27 行/秒 ≈ **一天 4~5GB**；当前 /data 余量 8.0G，若再次爆发约 **1.5~2 天**可能吃满。

---

## 六、未解决 / 需用户在自己服务端处理

本机（xxx.xxx.x.x）**未安装任何 CoreProtect 的 jar**（`/opt` 下仅 MCSManager、mcstats、omg、napcat 等），插件运行在**用户自己的国际服服务端**上，因此以下三项无法在本机完成：

| 项 | 建议 |
|---|---|
| **根治写入量** | 服务端 `config.yml` 关闭实体记录 / 调低 `rollback-entities` 相关项 |
| **定位装置** | 排查 09-29 00:20 在线玩家（`co_block` 显示 **j4rvx** 该时段 83,243 次操作），检查其基地有无挂机刷怪/实体密集装置 |
| **补建索引** | `co_entity` 无 time 索引，导致任何按时间查询全表扫。若需长期保留，可评估加索引（写入开销上升） |

---

## 七、方法论沉淀

### 7.1 无 time 索引大表的时段分析 —— 主键分桶法

`co_entity` 无 time 索引，`GROUP BY time` 全表扫 3.3GB 必然超时。**利用 `rowid` 与 `time` 正相关，按 rowid 分桶绕开全表扫描**：

```sql
SELECT FLOOR(rowid/100000)*100000 AS bucket,
       COUNT(*) n,
       FROM_UNIXTIME(MIN(time)) t_min,
       FROM_UNIXTIME(MAX(time)) t_max,
       ROUND(MAX(time)-MIN(time)) span_s,
       ROUND(COUNT(*)/GREATEST(MAX(time)-MIN(time),1),3) rows_per_s
FROM co_entity
WHERE rowid >= 3500000
GROUP BY bucket ORDER BY bucket;
```

把「不可能的全表扫描」变成「主键范围扫描」，秒级出结果。**同类无索引表的时段分析可通用。**

### 7.2 表结构先于查询 —— 别写注定失败的 SQL

排查前先 `DESCRIBE` 确认列名，避免反复试错。`co_entity` 只有 3 列，因此：

| 需求 | 能否实现 | 原因 |
|---|---|---|
| 各生物/掉落物/箭/船排行 | ❌ | 无 entity 列，`data` 是 Java 序列化 blob，无关联键 |
| 击杀生物排行 | ✅ | 走 `co_block action=2` JOIN `co_entity_map` |
| 被何生物杀死 | ✅ | 走 `co_block action=3` |
| 掉落物/箭/船 | ❌ | 表中根本无此类记录 |

### 7.3 速率恒定 = 自动装置

判断依据：**连续 10 小时、每分钟 1,400~1,600 条、无波动**。真人操作必然有起伏与停顿；恒定速率是机械重复的指纹。辅以「同秒 15~28 条」可进一步确认。

### 7.4 OPTIMIZE 的顺序陷阱

磁盘紧张时，**必须先 OPTIMIZE 小表腾出空间，再处理最大的表**。否则大表重建会因 `The table ... is full` 失败，且该错误只出现在 `OPTIMIZE TABLE` 输出的 `Msg_type=error` 行里，**`OPTIMIZE rc=$?` 仍返回 0**，极易误判为成功。**验收必须看物理文件大小变化，不能只看返回码。**

### 7.5 复用的命令速查

```bash
# 库凭据（chmod 600，密码不进 ps）
mysql --defaults-file=/opt/mcstats/.my.cnf coreprotect -e "..."

# 表逻辑体积（含所有表排序）
mysql --defaults-file=/opt/mcstats/.my.cnf -e "SELECT table_name, table_rows,
 ROUND((data_length+index_length)/1024/1024,1) mb FROM information_schema.tables
 WHERE table_schema='coreprotect' ORDER BY (data_length+index_length) DESC LIMIT 12;"

# 表物理文件真实大小（判断碎片）
sudo ls -lh /data/mysql/coreprotect/*.ibd | sort -k5 -h

# 确认无残留定时任务
sudo ls /etc/cron.d/ | grep -i "cp-burst"
```

---

## 附：核心结论一句话

> **不是脏数据。** `co_entity` 是 CoreProtect 的实体 NBT 记录表（每行约 2.2KB），09-29 00:20 起被挂机装置推到 27 行/秒并持续 10 小时，一天写入 100 万行；叠加三张表累计 7.6GB 未回收碎片，把 /data 顶到 97%。已通过 OPTIMIZE 回收 **7.4GB**（/data 剩余 588M → **8.0G / 58%**），运营数据已备份并验证可还原，自动清理任务按用户要求**已关闭**，根治需在用户自己的服务端关闭实体记录并排查装置。

*记录生成：2026-09-29*
