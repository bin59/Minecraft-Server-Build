# 5. 权限管理系统 — LuckPerms

本页记录南瓜生存服 LuckPerms 权限插件的安装现状、关键配置与常用命令。LuckPerms 负责管理全服玩家的权限组、命令权限与聊天前缀，数据保存在本地 H2 数据库。正式部署权限前建议先阅读同目录的《权限组设计方案》，理解组架构与继承链后再动手。

**插件 Jar**: ✅ 已安装 `plugins/LuckPerms-Bukkit-5.5.81.jar`（版本 5.5.81，已正常加载）
**数据文件**: `plugins/LuckPerms/luckperms-h2-v2.mv.db`（H2 本地数据库，权限数据完好）
**依赖库**: `plugins/LuckPerms/libs/` 目录下运行时依赖齐全

**官方网站**: https://luckperms.net | **Wiki**: https://luckperms.net/wiki

## 📁 本目录配置文件索引

面向「南瓜生存服」当前插件组合的完整权限方案，按使用顺序阅读：

| 文件 | 用途 | 什么时候用 |
|---|---|---|
| [`权限组设计方案.md`](权限组设计方案.md) | 组架构、权重、前缀、继承链、Residence 组映射、基岩版分组、部署与验收 | **先看这个**，理解设计再动手 |
| [`权限导入脚本.txt`](权限导入脚本.txt) | 一键执行的 `lp` 命令集（控制台粘贴） | 部署时执行 |
| [`config.yml`](config.yml) | LuckPerms 主配置，可直接放入 `plugins/LuckPerms/` | 部署时替换 |
| [`权限节点速查.md`](权限节点速查.md) | 按插件分类的权限节点对照表 + 排错命令 | 日常查节点、排错 |
| 本文件 | 插件基础说明与常用命令 | 了解 LuckPerms 本身 |

> ⚠️ **注意**：Floodgate 本身**不会**向 LuckPerms 注册 `floodgate` 上下文，
> 网上流传的 `... true floodgate=true` 写法在默认安装下不生效。
> 基岩版玩家分组请用 [`权限组设计方案.md` §5](权限组设计方案.md#五基岩版floodgate玩家的特殊处理) 中的独立组方案。

## 功能说明

LuckPerms 是目前 Minecraft 社区最强大的权限管理插件，支持组权限、临时权限、权限继承、上下文条件等高级功能。本服使用 LuckPerms 管理所有玩家的权限组和命令权限。

## 关键配置 (`plugins/LuckPerms/config.yml`)

```yaml
server: global # 全局服务器模式
storage-method: h2 # 使用 H2 本地数据库存储
sync-minutes: -1 # 禁用自动同步
watch-files: true # 监控文件变更
messaging-service: auto # 自动选择消息服务

primary-group-calculation: parents-by-weight # 按权重计算主组
inheritance-traversal-algorithm: depth-first-pre-order # 深度优先遍历

apply-wildcards: true # 启用通配符权限
apply-regex: true # 启用正则权限
apply-shorthand: true # 启用简写权限
apply-bukkit-child-permissions: true # 启用 Bukkit 子权限
apply-bukkit-default-permissions: true # 启用 Bukkit 默认权限

auto-op: false # 不自动授予 OP
enable-ops: true # 允许 OP 存在
commands-allow-op: true # OP 可使用所有 LP 命令

vault-group-use-displaynames: true # Vault 使用组显示名
vault-npc-group: default # NPC 默认组
```

## 常用命令（按官网规格 v5）

**命令别名**：Bukkit 端使用 `/lp`（全名 `/luckperms`；旧别名 `/permissions` `/perms` `/perm` 已废弃）。BungeeCord 为 `/lpb`、Velocity 为 `/lpv`。

**参数规则**：`<必填>` 必须写；`[可选]` 可省略；含空格参数用双引号包裹，如 `"管理员组"`。上下文（context）格式为 `<键>=<值>`，如 `server=survival world=world_nether`。临时时长支持时间串（如 `1mo3d13h45m`，单位 s/m/h/d/w/mo/y）或 Unix 时间戳。

### 通用命令

| 命令 | 说明 |
|---|---|
| `/lp` | 列出当前账号可用命令 |
| `/lp sync` | 重新读取存储数据（多端改完数据后刷新） |
| `/lp info` | 插件信息、调试输出、统计 |
| `/lp editor [类型] [过滤]` | 打开网页编辑器（类型：all/users/online/groups） |
| `/lp verbose <on\|record\|off\|upload> [过滤]` | 监听权限检查（on 聊天提醒 / record 静默记录 / off 关闭 / upload 上传网页） |
| `/lp verbose command <me\|玩家> <命令>` | 以指定玩家身份执行命令并监听权限检查 |
| `/lp tree [根] [玩家]` | 权限树视图（`/lp tree .` 全部） |
| `/lp search [比较符] <权限>` | 全库搜索某权限（比较符：`==` 等于 / `!=` 不等于 / `~~` 相似 / `!~` 不相似） |
| `/lp networksync` | 同步本服数据并通知其他服一起同步 |
| `/lp export <文件\|--upload> [--without-users] [--without-groups]` | 导出权限数据（JSON GZIP 备份；`--upload` 导出网页码） |
| `/lp import <文件\|码 --upload> [--replace]` | 导入权限数据（文件需带 `.json.gz` 后缀；`--replace` 覆盖，不加则合并） |
| `/lp reloadconfig` | 重载配置文件（存储设置等需重启生效） |
| `/lp bulkupdate <类型> <动作> [字段] [值] [约束...]` | 批量修改（**仅控制台**；类型 all/users/groups；动作 update/delete；字段 permission/server/world） |
| `/lp creategroup <组名> [权重] [显示名]` | **创建权限组**（注意：不是 `/lp group xxx create`） |
| `/lp deletegroup <组名>` | 删除权限组 |
| `/lp listgroups` | 列出所有组 |
| `/lp createtrack <轨道名>` | 创建升级轨道 |
| `/lp deletetrack <轨道名>` | 删除轨道 |
| `/lp listtracks` | 列出所有轨道 |
| `/lp translations` | 语言包管理 |

### 用户命令（`/lp user <玩家> ...`）

| 命令 | 说明 |
|---|---|
| `info` | 查看玩家权限信息（组、节点、meta） |
| `permission ...` | 管理玩家权限（见下节） |
| `parent ...` | 管理玩家所在组（见下节） |
| `meta ...` | 管理玩家 meta（见下节） |
| `editor` | 打开该玩家的网页编辑器 |
| `promote <轨道> [上下文]` | 沿轨道升级 |
| `demote <轨道> [上下文]` | 沿轨道降级 |
| `showtracks` | 显示玩家所在轨道及位置 |
| `clear [上下文]` | 清空玩家全部权限数据 |
| `clone <目标玩家>` | 复制权限到另一玩家 |

### 组命令（`/lp group <组名> ...`）

| 命令 | 说明 |
|---|---|
| `info` | 查看组信息（成员、节点、meta） |
| `permission ...` | 管理组权限（见下节） |
| `parent ...` | 管理组继承（见下节） |
| `meta ...` | 管理组 meta（见下节） |
| `editor` | 打开该组的网页编辑器 |
| `listmembers [页]` | 列出组内成员 |
| `setweight <权重>` | 设置组权重（数值越高优先级越高） |
| `setdisplayname <显示名>` | 设置组显示名 |
| `showtracks` | 显示组所在轨道及位置 |
| `clear [上下文]` | 清空组全部权限数据 |
| `rename <新组名>` | 重命名组 |
| `clone <新组名>` | 复制该组 |

### 权限命令（`... permission ...`，用户/组通用）

| 命令 | 说明 |
|---|---|
| `permission info [页] [排序]` | 列出权限节点（排序：priority / !priority / abc / !abc） |
| `permission set <节点> [true\|false] [上下文]` | 授予权限（省略值默认 true；false 为否定） |
| `permission unset <节点> [上下文]` | 移除权限 |
| `permission settemp <节点> <true\|false> <时长> [叠加方式] [上下文]` | 临时权限（叠加方式：accumulate 累加 / replace 保最长 / deny 拒绝重复） |
| `permission unsettemp <节点> [时长] [上下文]` | 移除临时权限（时长可省=全部移除） |
| `permission check <节点>` | 检查玩家/组是否有某权限及来源 |
| `permission clear [上下文]` | 清空该对象全部权限节点 |

### 父组/继承命令（`... parent ...`，用户/组通用）

| 命令 | 说明 |
|---|---|
| `parent info` | 列出所在组/继承关系 |
| `parent add <组> [上下文]` | 加入组（继承） |
| `parent remove <组> [上下文]` | 退出组 |
| `parent set <组> [上下文]` | 清空后只设一个组 |
| `parent addtemp <组> <时长> [叠加方式] [上下文]` | 临时加入组 |
| `parent removetemp <组> [时长] [上下文]` | 移除临时组 |
| `parent settrack <轨道> <组> [上下文]` | 沿轨道设为指定位置 |
| `parent cleartrack <轨道> [上下文]` | 清除轨道上的组 |
| `parent clear [上下文]` | 清空全部组关系 |
| `switchprimarygroup <组>` | 切换主组 |

### Meta 命令（`... meta ...`，用户/组通用）

| 命令 | 说明 |
|---|---|
| `meta info` | 列出全部 meta |
| `meta set <键> <值> [上下文]` | 设置 meta 键值 |
| `meta unset <键> [上下文]` | 移除 meta 键 |
| `meta settemp / unsettemp` | 临时 meta（同权限命令时长语法） |
| `meta addprefix <优先级> <前缀> [上下文]` | 添加前缀（优先级数字越小越靠前） |
| `meta setprefix [优先级] <前缀> [上下文]` | 设置前缀 |
| `meta removeprefix <优先级> [前缀] [上下文]` | 移除前缀 |
| `meta addsuffix / setsuffix / removesuffix` | 后缀（用法同上） |
| `meta addtempprefix / settempprefix / removetempprefix` 等 | 临时前缀/后缀 |
| `meta clear [上下文]` | 清空全部 meta |

### 轨道命令（`/lp track <轨道> ...`）

| 命令 | 说明 |
|---|---|
| `info` / `editor` | 查看 / 编辑轨道 |
| `append <组>` | 轨道末尾追加组 |
| `insert <组> <位置>` | 指定位置插入组 |
| `remove <组>` | 移除组 |
| `clear` | 清空轨道 |
| `rename <新轨道名>` | 重命名 |
| `clone <新轨道名>` | 复制 |

### 日志命令

| 命令 | 说明 |
|---|---|
| `/lp log recent [玩家] [页]` | 最近操作记录 |
| `/lp log search <关键词> [页]` | 搜索操作记录 |
| `/lp log notify [on\|off]` | 是否接收操作通知 |
| `/lp log userhistory <玩家> [页]` | 玩家操作历史 |
| `/lp log grouphistory <组> [页]` | 组操作历史 |
| `/lp log trackhistory <轨道> [页]` | 轨道操作历史 |

## 示例

```
# 创建权限组（官网语法，非 /lp group xxx create）
/lp creategroup admin 100

# 给 admin 组分配全部权限
/lp group admin permission set * true

# 设置组权重（数值越高优先级越高）
/lp group admin setweight 100

# 将玩家加入 admin 组
/lp user <玩家名> parent add admin

# 临时给玩家飞行权限 1 小时
/lp user <玩家名> permission settemp essentials.fly true 1h

# 仅在世界 world_nether 授予权限（上下文）
/lp group admin permission set essentials.fly true world=world_nether

# 批量：把全部数据里 essentials.fly 设为 false（控制台执行）
/lp bulkupdate users update permission false permission=essentials.fly

# 导出/导入备份
/lp export 备份文件
/lp import 备份文件.json.gz
```
