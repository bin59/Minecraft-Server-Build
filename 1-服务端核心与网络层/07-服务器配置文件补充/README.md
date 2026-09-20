# 16. 服务器配置文件补充

本文档补充列出 Leaf 服务端目录下各类配置与数据文件的作用和当前取值。核心文件包括 server.properties、bukkit.yml、spigot.yml、commands.yml、permissions.yml 等，文档同时标注了 purpur.yml 等迁移遗留文件以及 ops.json、封禁名单等数据文件。文中给出了 bukkit.yml 生成限制、spigot.yml 中文消息、commands.yml 别名等关键片段，供服主调优服务器时查阅。

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

## `server.properties` MOTD 配置

`motd` 为服务器在客户端「多人游戏」列表处展示的标题，支持 `§` 颜色代码与 `\n` 换行（两行）。
当前最终取值（`server.properties` 第 43 行）：

```properties
motd=§x§f§f§5§e§1§9§l南§x§f§f§8§a§2§6§l瓜§x§f§f§b§b§3§3§l生§x§f§f§c§c§4§0§l存§x§f§f§d§d§6§6§l服§r §8▏ §x§f§f§5§e§1§9§l归墟纪·墟火\n§x§f§f§d§d§6§6✦ §a生存 §8· §e领地 §8· §d养老 §8· §b共建 §x§f§f§d§d§6§6✦ §7欢迎加入
```

**玩家实际看到的效果**（两行）：

```
第一行：南瓜生存服 ▏ 归墟纪·墟火
第二行：✦ 生存 · 领地 · 养老 · 共建 ✦ 欢迎加入
```

**配色说明**：

| 片段 | 颜色代码 | 效果 |
|---|---|---|
| 南 / 瓜 / 生 / 存 | `§x§f§f§5§e§1§9` 等十六进制渐变 + `§l` | 橙→金→青→红→金 逐字渐变加粗标题 |
| ▏ | `§8` | 灰色竖分隔线 |
| 归墟纪·墟火 | `§x§f§f§5§e§1§9§l` | 紫色渐变加粗（服务器联动小说项目名） |
| ✦ | `§x§f§f§d§d§6§6` | 金粉色渐变装饰星 |
| 生存 | `§a` | 绿色 |
| 领地 | `§e` | 金色 |
| 养老 | `§d` | 淡紫色 |
| 共建 | `§b` | 青色 |
| · | `§8` | 灰色间隔点 |
| 欢迎加入 | `§7` | 灰色 |

**注意**：

- MOTD / 图标（图标保持原 `server-icon.png` 不变）均由后端 `server.properties` 直接控制，**本服未使用 Velocity 代理**，无需代理端配置。
- 十六进制渐变格式为 `§x§R§R§G§G§B§B`（每个通道重复两次），1.21 起支持；修改后**需重启后端实例**才生效。

