# Plan (Player Analytics) 使用文档

Plan 是服务器数据分析工具：自动采集玩家活动、服务器性能、地理分布与留存率，内置 Web 仪表盘可视化展示（本服地址 `http://plan.mchappyhut.club:8804`）。

## 安装

1. GitHub Releases 下载 `Plan.jar` → 放入 `plugins/`
2. 重启服务器，自动生成 `plugins/Plan/`
3. 浏览器访问 `http://服务器IP:8804` 打开 Web 面板

## 核心配置（plugins/Plan/config.yml）

### Web 服务器

- 端口默认 `8804`；本服用域名 `plan.mchappyhut.club:8804` 访问
- 未启用 SSL（面板无需登录；Plan 只读，只能取数据不能控制服务器，不加密无风险）

### 数据库（本服实际：MySQL）

```yaml
Database:
  Type: MySQL
  MySQL:
    Host: 'localhost'
    Port: 3306
    Database: 'Plan'
    User: 'root'
    Password: 'xxx'   # 真实密码不写入文档
    Launch_options: '?rewriteBatchedStatements=true&serverTimezone=UTC'
    Max_connections: 8
```

- 单服可用默认 SQLite；**Velocity 群组必须 MySQL**，所有子服连同一个库
- **建库流程**：先执行 `CREATE DATABASE Plan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;` → 改配置（库名完全一致 `Plan`）→ **重启服务器**（`/plan reload` 只重载配置、不切库）→ 验证：`SHOW TABLES FROM Plan;` 有表且 Web 面板数据正常
- **⚠️ 降级行为**：MySQL 连接失败时 Plan 自动退回 SQLite（`plugins/Plan/database.db`）继续记录——"网页正常" ≠ "MySQL 生效"，以上面验证为准

### 数据收集（当前配置）

```yaml
Data_gathering:
  Geolocations: true            # 玩家地理位置（国家级）
  Server_performance: true      # TPS / CPU / 内存
  World_time_tracking: true     # 各世界活动时间分布
  AFK_threshold:                # 180 秒无操作判为 AFK
    Time: 3
    Unit: MINUTES
  Ping: true
  Client_info: true             # 客户端品牌/版本

Analysis:
  Activity_index:               # 每周在线 30 分钟 → 活动指数约 3
    Playtime_threshold:
      Time: 30
      Unit: MINUTES
```

## Web 面板功能

- **概览**：实时在线人数、TPS/CPU/内存、今日新增/回归/总在线时长
- **玩家分析**：会话记录、PVP/PVE 击杀与死亡、地理、客户端、活动时间热力图
- **服务器分析**：性能历史趋势（天/周/月）、活动日历、留存率（次日/3/7/30 日）
- **查询**：`/plan search` 语法，如 `players online:100-200`、`deaths:1000-`
- **网络视图**（群组模式）：跨子服数据聚合

## PlaceholderAPI 常用占位符（官方）

| 占位符 | 说明 |
| --- | --- |
| `%plan_player_time_total%` | 玩家总在线时长 |
| `%plan_player_kdr%` | 玩家 K/D |
| `%plan_player_activity_index%` | 玩家活动指数（0-5） |
| `%plan_server_players_online%` | 服务器当前在线人数 |
| `%plan_server_players_unique_day%` | 今日独立玩家数 |
| `%plan_server_players_registered_day%` | 今日新注册玩家数 |
| `%plan_server_tps_day%` | 今日平均 TPS |

> TAB 里首次使用 Plan 占位符可能不显示：占位符有缓存，需**替换两次**生效，或把占位符加入 `Plugins.PlaceholderAPI.Load_these_placeholders_on_join` 配置。

## 常用指令（官方命令表）

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `/plan` | 主命令（只显示有权限的子命令） | `plan.command` |
| `/plan player <玩家>` | 玩家页链接（别名 inspect） | `plan.player.self` / `.other` |
| `/plan ingame <玩家>` | 游戏内查看玩家数据（别名 qinspect） | `plan.ingame.self` / `.other` |
| `/plan search <关键词>` | 查询玩家名 | `plan.search` |
| `/plan servers` | 列出数据库中的服务器 | `plan.servers` |
| `/plan network` | 网络页链接 | `plan.network` |
| `/plan register` | 注册 Web 面板账号 | `plan.register.self` / `.other` |
| `/plan info` | 插件状态信息 | `plan.info` |
| `/plan reload` | 重载配置 | `plan.reload` |
| `/plan db ...` | 数据库操作（backup/restore/merge/clear 等） | `plan.data.*` |

## 群组模式（Velocity）

代理端与各子服都安装 Plan 并连接同一个 MySQL → 子服自动关闭自身 Web 服务器，只访问代理端面板查看聚合数据。

## 排障

- **Plan 没有余额功能**：官方数据库 schema 无余额/交易表（唯一经济相关是第三方 Tebex 赞助表 `plan_tebex_payments`），Plan 不存余额也不显示余额。实时余额用 `/bal` 或 `%vault_eco_balance%`（PAPI Vault 扩展直读 EssentialsX）；流水看 `plugins/Essentials/logs/trade.log`（见经济系统设计手册 §9.1）
- **日志 `SocketException / ConnectException`** 多为检查 GitHub 更新失败（国内网络不稳定），与数据库无关；判断数据库连通以「建库 + SHOW TABLES」为准
- **数据导出**：Web 图表可导出 JSON；`/player/<name>/raw`、`/server/<name>/raw` 获取原始数据链接

## 相关链接

| 资源 | 链接 |
| --- | --- |
| SpigotMC | https://www.spigotmc.org/resources/player-analytics-plan.32536/ |
| GitHub | https://github.com/plan-player-analytics/Plan |
| 官方 Wiki | https://github.com/plan-player-analytics/Plan/wiki |
