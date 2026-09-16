# 5. 权限管理系统 — LuckPerms

**插件 Jar**: ✅ 已安装 `plugins/LuckPerms-Bukkit-5.5.81.jar`（版本 5.5.81，已正常加载）
**数据文件**: `plugins/LuckPerms/luckperms-h2-v2.mv.db`（H2 本地数据库，权限数据完好）
**依赖库**: `plugins/LuckPerms/libs/` 目录下运行时依赖齐全

**官方网站**: https://luckperms.net | **Wiki**: https://luckperms.net/wiki

> ✅ **当前状态（2026-09-16 校准）**：主插件 JAR（5.5.81）已在 `plugins/` 目录并正常加载，存储后端为 H2 本地库（`config.yml` 第86行 `storage-method: h2`），此前「缺少主 JAR、需手动下载」的说明已过时，现删除。

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

## 常用命令

### 组管理

| 命令                                     | 说明       |
| ---------------------------------------- | ---------- |
| `/lp group <组名> create`                | 创建权限组 |
| `/lp group <组名> permission set <权限>` | 设置组权限 |
| `/lp group <组名> parent add <父组>`     | 添加继承   |
| `/lp group <组名> setweight <权重>`      | 设置权重   |
| `/lp group <组名> meta setprefix <前缀>` | 设置前缀   |
| `/lp listgroups`                         | 列出所有组 |

创建权限组的正确命令是 /lp creategroup admin

### 玩家管理

| 命令                                    | 说明             |
| --------------------------------------- | ---------------- |
| `/lp user <玩家> parent add <组>`       | 将玩家加入组     |
| `/lp user <玩家> permission set <权限>` | 设置玩家权限     |
| `/lp user <玩家> info`                  | 查看玩家权限信息 |
| `/lp user <玩家> promote <轨道>`        | 升级玩家         |
| `/lp user <玩家> demote <轨道>`         | 降级玩家         |

### 批量操作

| 命令             | 说明         |
| ---------------- | ------------ |
| `/lp bulkupdate` | 批量更新权限 |
| `/lp export`     | 导出权限数据 |
| `/lp import`     | 导入权限数据 |

例如：

# 给 admin 组分配全部权限

/lp group admin permission set \* true

# 设置组权重（数值越高优先级越高）

/lp group admin meta set weight 100

# 将玩家加入 admin 组

/lp user <玩家名> parent add admin
