# 5. 权限管理系统 — LuckPerms

**插件 Jar**: ⚠️ **未找到 LuckPerms 主 JAR 文件**（需手动下载）  
**数据文件**: `plugins/LuckPerms/luckperms-h2-v2.mv.db`（权限数据完好）  
**依赖库**: `plugins/LuckPerms/libs/` 目录下 24 个 .jar（运行时依赖齐全）

**官方网站**: https://luckperms.net | **Wiki**: https://luckperms.net/wiki

> ⚠️ **当前状态**: 权限数据库和配置文件均完好，但 `plugins/` 目录下缺少 `LuckPerms-Bukkit-*.jar` 主插件文件。请从 [LuckPerms 下载页](https://luckperms.net/download) 下载 `LuckPerms-Bukkit` 对应版本放入 `plugins/` 目录，否则权限系统无法加载。

## 功能说明

LuckPerms 是目前 Minecraft 社区最强大的权限管理插件，支持组权限、临时权限、权限继承、上下文条件等高级功能。本服使用 LuckPerms 管理所有玩家的权限组和命令权限。

## 关键配置 (`plugins/LuckPerms/config.yml`)

```yaml
server: global                      # 全局服务器模式
storage-method: h2                  # 使用 H2 本地数据库存储
sync-minutes: -1                    # 禁用自动同步
watch-files: true                   # 监控文件变更
messaging-service: auto             # 自动选择消息服务

primary-group-calculation: parents-by-weight   # 按权重计算主组
inheritance-traversal-algorithm: depth-first-pre-order  # 深度优先遍历

apply-wildcards: true               # 启用通配符权限
apply-regex: true                   # 启用正则权限
apply-shorthand: true               # 启用简写权限
apply-bukkit-child-permissions: true    # 启用 Bukkit 子权限
apply-bukkit-default-permissions: true  # 启用 Bukkit 默认权限

auto-op: false                      # 不自动授予 OP
enable-ops: true                    # 允许 OP 存在
commands-allow-op: true             # OP 可使用所有 LP 命令

vault-group-use-displaynames: true  # Vault 使用组显示名
vault-npc-group: default            # NPC 默认组
```

## 常用命令

### 组管理

| 命令 | 说明 |
|---|---|
| `/lp group <组名> create` | 创建权限组 |
| `/lp group <组名> permission set <权限>` | 设置组权限 |
| `/lp group <组名> parent add <父组>` | 添加继承 |
| `/lp group <组名> setweight <权重>` | 设置权重 |
| `/lp group <组名> meta setprefix <前缀>` | 设置前缀 |
| `/lp listgroups` | 列出所有组 |

### 玩家管理

| 命令 | 说明 |
|---|---|
| `/lp user <玩家> parent add <组>` | 将玩家加入组 |
| `/lp user <玩家> permission set <权限>` | 设置玩家权限 |
| `/lp user <玩家> info` | 查看玩家权限信息 |
| `/lp user <玩家> promote <轨道>` | 升级玩家 |
| `/lp user <玩家> demote <轨道>` | 降级玩家 |

### 批量操作

| 命令 | 说明 |
|---|---|
| `/lp bulkupdate` | 批量更新权限 |
| `/lp export` | 导出权限数据 |
| `/lp import` | 导入权限数据 |
