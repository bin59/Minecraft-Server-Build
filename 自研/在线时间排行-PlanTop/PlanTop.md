# PlanTop 在线时间排行插件（自研）

从 **Plan 数据库**读取玩家总在线时间，注册 PAPI 占位符供 DecentHolograms 等展示 Top 30 排行榜。历史在线时间全部保留（读 Plan 的 `plan_sessions` 聚合），不重新计时。

## 为什么自研

- PlayTime 类插件从安装起才计时，无历史数据，排行等于重新开始
- Plan 自带 PAPI 占位符只有 Top 10 且只有名字（`%plan_top_playtime_...%`），满足不了 Top 30 + 时长需求
- 本服 Plan 一直运行：`plan_sessions` 表已累计 **157 名玩家、6656 条会话**（2026-08-31 至今持续记录），是唯一现成的完整历史数据源
- 自研只读不改：Plan 是权威数据源，排行与 Plan 网页口径一致，互不冲突

## 数据源与原理

- 数据库：Plan 的 MySQL（`plugins/Plan/config.yml` → Database 段），表：
  - `plan_sessions`：`id / user_id / server_id / session_start / session_end（bigint 毫秒）/ afk_time / ...`
  - `plan_users`：`id / uuid / name / ...`
  - `plan_servers`：`id / uuid / name / is_installed / ...`
- 聚合 SQL（每 5 分钟异步执行一次）：

```sql
SELECT u.name, SUM(s.session_end - s.session_start) / 1000.0 AS secs
FROM plan_sessions s
JOIN plan_users u ON u.id = s.user_id
WHERE s.server_id = ?
GROUP BY u.id, u.name
ORDER BY secs DESC
LIMIT ?
```

- 结果存内存缓存，占位符读取零 DB 压力；查询失败保留上次成功缓存并记 `%ptop_error%`
- 口径：会话总时长（含 AFK）；Plan 对在线会话每几分钟续写 `session_end`，数据最多延迟几分钟

## 项目结构

```
自研\在线时间排行-PlanTop\
├── PlanTop-1.0.0.jar       # 成品（已内置 MySQL 驱动，直接部署）
├── PlanTop.md              # 本文档
└── src\
    ├── plugin.yml
    ├── config.yml          # 默认配置（首次启动自动导出到 plugins/PlanTop/）
    └── com\plantop\
        ├── PlanTopPlugin.java      # 主类：加载配置、注册占位符、调度
        ├── PlaytimeCache.java      # 异步聚合 + 内存缓存 + 格式化
        └── PlanTopExpansion.java   # PAPI 占位符
```

## 占位符

| 占位符 | 说明 |
| --- | --- |
| `%ptop_<N>_name%` | 第 N 名玩家名（N=1..30，越界返回 `no-data`） |
| `%ptop_<N>_displayname%` | 第 N 名显示名（当前同 name） |
| `%ptop_<N>_time%` | 第 N 名总时长，**只显示分钟**（如 `11963分`，天/小时全部换算成分） |
| `%ptop_updated%` | 最后成功刷新时间 `yyyy-MM-dd HH:mm`（`never` = 还没成功过） |
| `%ptop_error%` | 最近一次错误信息（`ok` = 正常） |

## 配置（plugins/PlanTop/config.yml）

```yaml
database:
  host: "123.207.3.73"      # 与 Plan config.yml 的 Database 段一致
  port: 3306
  database: "Plan"
  user: "host21k5c4"
  password: "xxx"           # ← 填入 Plan 真实密码（本仓库不存明文）
  server-id: 1              # plan_servers 里当前服 id；多服共库必须改对
refresh-seconds: 300        # 聚合刷新间隔（最小 10）
top-size: 30                # 排行人数
query-timeout-seconds: 15   # 单次查询超时
```

> 连接参数每 5 分钟用一次，属于低频；JDBC 直连 + 每次新建连接（用完关闭），不占连接池。`useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=5000` 已内置 URL。

## 编译打包（重建用）

前置：`leaf-api.jar`、`PlaceholderAPI-*.jar`、`adventure-api.jar`、`adventure-key.jar`（编译期）；`mysql-connector-j-*.jar`（shade 进产物）。

```
javac -encoding UTF-8 -cp "leaf-api.jar;PlaceholderAPI.jar;adventure-api.jar;adventure-key.jar" -d build src\com\plantop\*.java
```

打包：`plugin.yml` + `config.yml` + `com/plantop/*.class` + **mysql-connector 全部 class**（保留 `META-INF/services/java.sql.Driver`，去掉签名/MANIFEST/module-info）→ `PlanTop-1.0.0.jar`。驱动已内置，运行服不需要额外装驱动。

## 部署

1. `PlanTop-1.0.0.jar` 放进运行服 `plugins/`，**重启服务器**（占位符才注册）
2. 填 `plugins/PlanTop/config.yml` 数据库密码
3. 验证：`/papi parse me %ptop_1_name%` 返回玩家名；`%ptop_updated%` 返回时间
4. 主城全息部署命令见 `4-玩家信息与数据展示\07-在线时间排行榜（PlanTop）.md`

## 排障

| 现象 | 排查 |
| --- | --- |
| `%ptop_1_name%` 返回原样 | 插件没装 / 没重启 / PAPI 没装 |
| `%ptop_updated%` = `never` 或 `%ptop_error%` 报错 | 数据库连接失败：密码、host/port、`server-id`、远程 MySQL 白名单放行本服 IP |
| 排行数据旧 | 等下一次刷新（最多 5 分钟）；或看 `%ptop_updated%` 时间 |
| 人数不足 30 | `top-size` 改大 + 刷新；当前服实际在册玩家可能少于 30 人上榜 |

## 扩展方向（未实现）

- 活跃时长排行：`SUM(session_end - session_start - afk_time)`（减 AFK），新增 `%ptop_<N>_active_time%`
- 按日/月排行：`WHERE s.session_start >= ?` 加时间窗
- 显示名映射：从 LuckPerms/称号插件取玩家前缀
