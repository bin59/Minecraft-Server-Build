# 9. PatPat 摸头 — PatPat [Mod & Plugin]

摸头互动插件（作者 LopyMine / LopyTwich）：**Shift + 右键**摸任意活体生物（猫、狗、村民、苦力怕都能摸），触发摸头动画 + 爱心粒子 + 音效。**客户端 mod + 服务端插件**双形态。

**下载**: https://modrinth.com/plugin/patpat（同项目内下载 jar 时选平台：服务端装 Bukkit/Paper 版，Java 客户端装 Fabric/Forge 版）

**兼容**: MC 1.16.x ~ 1.21.x（含 1.21.11）；平台 Bukkit/Spigot/Paper/Purpur/Folia（服务端）+ Fabric/Forge/NeoForge/Quilt（客户端）；LGPL-3.0

> ⚠️ **核心机制（重要）**：服务端插件**只负责数据包中转与权限/限流控制**，**必须搭配客户端 mod 才能触发摸头**（作者原文："The plugin does not work without the client mod"）。所以：
> - **Java 玩家**：客户端装 PatPat mod（Fabric/Forge）→ 游戏里 Shift + 右键摸生物
> - **基岩玩家**：无法安装 Java 客户端 mod，**用不了摸头功能**

## 功能

- **Shift + 右键**摸任意活体生物（原版及模组生物均可）
- 摸头动画 + 爱心粒子 + 音效（动画可通过资源包自定义，可上传 Modrinth/CurseForge 分享）
- **多人支持**：服务器装了插件后，装 mod 的玩家可以互相摸、一起摸宠物
- **权限限制**（可开关）：需要 `patpat.pat` 权限才能摸
- **限流系统**（可开关）：token 制防刷屏，可豁免
- **名单模式**（可开关）：按玩家 UUID 白名单 / 黑名单控制谁可摸
- **API 开关**：`api: true` 允许 mod 客户端与服务端通信（必须保持 true）

## 安装

### 服务端（已装）

1. 从 Modrinth 下载 **Bukkit/Paper 版** jar 放入 `plugins/`（当前 config 版本 1.0.1）
2. 完整重启服务器
3. 生成 `plugins/PatPatPlugin/config.yml`

### Java 客户端（玩家必装）

1. 从 Modrinth（同一项目）下载 **Fabric/Forge 版** mod jar
2. 放入客户端 `mods/` 文件夹（Fabric/Forge 加载器）
3. 进入服务器后 **Shift + 右键**生物即可摸头

> 服务端/客户端版本需匹配（同 MC 版本线）；装 mod 的玩家才能摸，未装 mod 的玩家摸不了也看不到动画。

## 命令与权限

| 命令 | 说明 |
|---|---|
| `/patpat info` | 查看服务端版本信息（报 bug 时用） |
| `/patpat reload` | 重载配置 |
| `/patpat ratelimit [info \| set \| enable \| disable]` | 限流查看/设置/开关 |
| `/patpat ratelimit set [increment \| interval \| limit]` | 设置限流参数（token 增量/间隔/上限） |

**权限节点**（用 LuckPerms 分配）：

| 权限 | 说明 |
|---|---|
| `patpat.pat` | 摸头权限（`permissionRestrictions.enabled: true` 时必须有才能摸） |
| `patpat.ratelimit.bypass` | 限流豁免（无视 token 限制） |

> 管理命令默认 op 级。若开启权限限制，给全体玩家：
> ```
> lp group default permission set patpat.pat true
> ```

## 配置（`plugins/PatPatPlugin/config.yml`，本服实际内容）

```json
{
    "_info": {
        "_comment": "DON'T CHANGE ANYTHING IN THIS SECTOR!",
        "_doc": "Documentation: https://github.com/LopyMine/PatPat-Plugin/blob/main/doc/en/config.md",
        "version": "1.0.1"
    },
    "debug": false,
    "api": true,
    "listMode": "DISABLED",
    "rateLimit": {
        "enabled": false,
        "tokenLimit": 20,
        "tokenIncrement": 1,
        "tokenInterval": "1sec",
        "permissionBypass": "patpat.ratelimit.bypass"
    },
    "permissionRestrictions": {
        "enabled": false,
        "permissionForPat": "patpat.pat"
    }
}
```

**字段含义**：

| 字段 | 值 | 含义 |
|---|---|---|
| `debug` | false | 调试日志开关（排查问题时开） |
| `api` | true | 服务端 API/数据包中转开关。**必须保持 true**，关掉后 mod 客户端无法通信 |
| `listMode` | DISABLED | 名单模式：`DISABLED`=所有人可摸；`WHITELIST`=仅 `player-list.json` 列出的玩家可摸；`BLACKLIST`=列出的玩家不可摸（名单文件在插件数据目录，按 UUID 填） |
| `rateLimit.enabled` | false | 限流开关 |
| `rateLimit.tokenLimit` | 20 | 玩家拥有的摸头 token 上限 |
| `rateLimit.tokenIncrement` | 1 | 每个间隔补充的 token 数 |
| `rateLimit.tokenInterval` | "1sec" | 补充间隔（`1sec`/`2min`/`1hour`/`1day`） |
| `rateLimit.permissionBypass` | `patpat.ratelimit.bypass` | 豁免权限节点 |
| `permissionRestrictions.enabled` | false | 摸头权限限制开关 |
| `permissionRestrictions.permissionForPat` | `patpat.pat` | 摸头所需权限节点 |

## 调优建议（本服）

- **保持 `api: true`**——这是服务端与 mod 客户端通信的通道，改 false 摸头直接失效
- 默认 `listMode: DISABLED` + `permissionRestrictions.enabled: false` = 全服都能摸；想限制"只有装了 mod 的会员能摸"也做不到（mod 是功能前提，权限只是服务端把关）
- 想防刷屏：`rateLimit.enabled: true`，默认 token 20、每秒补 1 个，基本够日常玩；觉得太快可把 `tokenLimit` 调低
- 想让管理/特定玩家无视限流：`lp user <玩家> permission set patpat.ratelimit.bypass true`
- 某玩家恶意骚扰（对他人狂摸）：临时 `lp user <玩家> permission unset patpat.pat true`（若已开权限限制）或加进 BLACKLIST

## 说明

- 服务端插件 + 客户端 mod 版本需一致（同一 MC 版本线），否则数据包不兼容、摸头无反应
- 摸头动画可用**资源包**自定义并上传 Modrinth/CurseForge 分享（见官方 Wiki）
- **基岩版玩家无法使用本功能**（客户端 mod 无法在基岩端安装）；如需基岩也能互动，考虑纯服务端方案（如右键粒子插件），或接受"仅 Java 玩家可用"
