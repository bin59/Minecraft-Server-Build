# 16. 服务器配置文件补充

## 配置文件总览

| 状态 | 文件 | 作用 |
|---|---|---|
| ✅ 核心 | `server.properties` | 服务器主配置（端口、模式、难度等） |
| ✅ 核心 | `bukkit.yml` | Bukkit 通用配置（区块生成、怪物生成限制等） |
| ✅ 核心 | `spigot.yml` | Spigot 性能配置（实体激活范围、tick 率等） |
| ✅ 核心 | `commands.yml` | 命令别名和覆盖 |
| ✅ 核心 | `permissions.yml` | 权限定义文件 |
| ✅ 核心 | `eula.txt` | 最终用户许可协议（必须为 `true`） |
| ✅ 核心 | `help.yml` | Paper 帮助系统配置（当前为默认空配置 `{}`) |
| ⚠️ 遗留 | `purpur.yml` | Purpur 配置 — **Leaf 不读取此文件**，迁移遗留 |
| 📋 数据 | `ops.json` | 服务器管理员（OP）名单 |
| 📋 数据 | `usercache.json` | 玩家用户名缓存（当前 1000 条上限） |
| 📋 数据 | `banned-players.json` | 已封禁玩家列表 |
| 📋 数据 | `banned-ips.json` | 已封禁 IP 列表 |
| 📋 数据 | `version_history.json` | 服务器版本升级记录 |
| 🖼️ 资源 | `server-icon.png` | 服务器图标（64×64，显示在客户端服务器列表） |

## `bukkit.yml` 关键配置

```yaml
spawn-limits:
  monsters: 32        # 怪物上限
  animals: 8          # 动物上限
  water-animals: 4    # 水生动物上限
  ambient: 4          # 环境生物上限

ticks-per:
  animal-spawns: 600  # 动物生成频率
  monster-spawns: 1   # 怪物生成频率
  autosave: 6000      # 自动保存频率(刻)

chunk-gc:
  period-in-ticks: 600 # 区块垃圾回收周期

connection-throttle: 4000  # 连接节流(ms)，Geyser需要较高值
```

## `spigot.yml` 消息配置（中文）

```yaml
messages:
  whitelist: "您未被加入白名单！请联系服务器管理员！"
  unknown-command: "未知命令。输入 \"/help\" 获取帮助。"
  server-full: "服务器已满！"
  outdated-client: "客户端过旧！请使用 {0}"
  outdated-server: "服务器过旧！当前版本为 {0}"
  restart: "服务器正在重启。"
```

## `permissions.yml`

文件为空，所有权限由 **LuckPerms** 管理。

## `commands.yml`

```yaml
command-block-overrides: []
ignore-vanilla-permissions: false
aliases:
  icanhasbukkit:
  - "version $1-"
```
