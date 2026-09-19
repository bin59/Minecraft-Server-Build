# PosTracker 玩家位置记录（安装与配置）

本文介绍 PosTracker 这款轻量玩家位置追踪插件，用来替代已弃用的 CoordinateLogger：它按可配置秒数间隔记录在线玩家的坐标、世界与时间戳，并支持按半径和时间回溯查询、坐标可点击传送。文档给出 config.yml 各项说明、文件与 MySQL 两种存储模式的建表要点、/pos 命令权限和排查违规、回爆炸现场等使用示例。

> 旧插件 CoordinateLogger 已弃用，本服现使用 **PosTracker（Player Position Tracker）** 作为玩家位置追踪方案。

## 1. 插件简介

PosTracker 是一款轻量级玩家位置追踪插件，按固定间隔（秒）记录在线玩家的坐标（X/Y/Z）、所在世界、时间戳，并支持回溯查询历史轨迹。

核心特性：
- 按可配置间隔（秒）保存玩家位置
- 支持 **文件存储** 与 **MySQL 数据库存储** 两种模式（二选一）
- 查询历史位置时带分页，坐标可点击直接传送
- 多语言界面（en / ru / uk）
- 兼容 Minecraft Java 1.15+（官方实测 1.15 – 1.21）

典型用途：
- **苦力怕炸建筑回溯**：爆炸由另一插件记录时间/地点，用 PosTracker 反查当时在场玩家，即使破坏来自生物也能锁定可疑玩家。
- 走失 / 死亡位置回溯、违规排查。

## 2. 下载与安装

- 下载（CurseForge）：https://www.curseforge.com/minecraft/bukkit-plugins/postracker-player-position-tracker
- 下载（SpigotMC）：https://www.spigotmc.org/resources/122693

安装步骤：
1. 下载最新 `.jar`
2. 放入服务端 `plugins/` 目录
3. 重启（或 reload）服务器，插件会自动生成 `config.yml`

## 3. 配置文件 config.yml

首次启动后生成的配置如下（含说明注释）：

```yaml
# 插件语言：en / uk / ru
language: en

# 位置保存间隔（秒），越小越密、写入越频繁
saveInterval: 3

# 存储开关：useDatabase
#   true  = 写入下方 db 段配置的 MySQL（插件自动建表）
#   false = 写入本地文件 plugins/PosTracker/positions.log（无需数据库）
# 注意：上游曾把 true/false 注释写反，已按字节码真实行为更正
# （true -> setupDatabase()，false -> setupLogFile()）。本服实际为 false（文件模式）。
# 实际值见 plugins/PosTracker/config.yml 第28行
useDatabase: false

# 仅当 useDatabase 为 true 时使用的 MySQL 连接信息
db:
  host: "localhost"
  port: "3306"
  database: "postracker"
  username: "root"
  password: ""
```

参数说明：

| 配置项 | 说明 |
| --- | --- |
| `language` | 插件语言，可选 `en` / `uk` / `ru` |
| `saveInterval` | 两次位置保存之间的秒数 |
| `useDatabase` | `true` 时走 MySQL（下方 `db` 段生效）；`false` 时数据写入本地 `positions.log`。本服当前为 `false` |
| `db` | MySQL 连接信息，仅 `useDatabase: true` 时生效 |

## 4. 数据库配置（MySQL 模式）

> 对应此前问题「配置数据库 PosTracker 需要手动创建吗」：

- **数据表：不用手动建。** 插件首次连接 MySQL 后会**自动建表**。
- **数据库（schema）：一般需要手动建一个空库。** 插件只连接"已存在的库"并在其中建表，不会帮你执行 `CREATE DATABASE`。
- **不想用数据库？** 把 `useDatabase` 设为 `false` 即可完全跳过 MySQL，数据写入本地日志文件（`positions.log`）。

MySQL 模式步骤：
1. 在 MySQL 中手动建立空库，例如：
   ```sql
   CREATE DATABASE postracker CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
2. 确认 `useDatabase: true`（开启 MySQL 存储）
3. 在 `db` 段填好 `host / port / database / username / password`（`database` 填上一步建的库名）
4. 重启服务器，插件自动建表并开始记录

## 5. 命令与权限

| 命令 | 说明 |
| --- | --- |
| `/pos help` | 显示帮助与使用说明 |
| `/pos interval` | 修改位置保存间隔（秒） |
| `/pos language` | 切换插件语言 |
| `/pos radius:<半径> time:<时长> [name:<玩家>] [page:<页码>]` | 查询位置记录 |

- 权限节点：`postracker.admin`（无此权限的玩家不能使用 `/pos` 系列命令）

## 6. 使用示例

- 查询最近 1 小时内、10 格半径内出现过的玩家（按玩家分组列出）：
  ```
  /pos radius:10 time:1h
  ```
- 查询玩家 Steve 最近 30 分钟的全部位置（坐标为可点击传送）：
  ```
  /pos radius:5 time:30m name:Steve
  ```

时间单位支持：`h`（小时）、`m`（分钟）等。

## 7. 注意事项

- 文件模式（`useDatabase: false`）无需任何数据库，适合轻量 / 单机使用；数据量随在线人数与间隔累积，注意定期清理日志文件。
- MySQL 模式请确保数据库账号有该库的建表权限；跨网络部署时注意 `host` 放行与密码安全（不要用空密码跑生产）。
- 插件标注为实验性（Experimental）版本，生产环境建议先在测试服验证。
