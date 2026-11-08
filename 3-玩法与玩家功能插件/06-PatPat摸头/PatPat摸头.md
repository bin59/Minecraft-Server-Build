# 9. PatPat 摸头 — PatPat [Mod & Plugin]

摸头互动插件（作者 LopyMine / LopyTwich）：**Shift + 右键**摸任意活体生物（猫、狗、村民、苦力怕都能摸），触发摸头动画 + 爱心粒子 + 音效。动画、粒子与音效由 **Java 客户端 mod** 呈现，本插件只负责服务端侧的数据包中转与权限/限流/名单控制；玩家不装 mod 就摸不了也看不到。

**下载**: 服务端插件 https://www.curseforge.com/minecraft/bukkit-plugins/patpat-plugin ｜ https://modrinth.com/plugin/patpat ；客户端 mod https://www.curseforge.com/minecraft/mc-mods/patpat 。同一个项目里按平台选 jar：服务端装 Bukkit/Paper 版，Java 客户端装 Fabric/Forge/NeoForge/Quilt 版。

**平台**: Bukkit/Spigot/Paper/Purpur/Folia（服务端）+ Fabric/Forge/NeoForge/Quilt（客户端）；无硬依赖，Adventure API 已 shade 进 jar；LGPL-3.0。完整说明见 `../摸摸头-PatPat/README.md`。

## 功能

- **Shift + 右键**摸任意活体生物（原版及模组生物均可）
- 摸头动画 + 爱心粒子 + 音效（动画可通过资源包自定义，可上传 Modrinth/CurseForge 分享）
- **多人支持**：服务器装了插件后，装 mod 的玩家可以互相摸、一起摸宠物
- **权限限制**（可开关）：需要 `patpat.pat` 权限才能摸
- **限流系统**（可开关）：token 制防刷屏，可豁免
- **名单模式**（可开关）：按玩家 UUID 白名单 / 黑名单控制谁可摸
- **API 开关**：`api: true` 时对外暴露 Bukkit 事件 `PatPacketReceiveEvent` 与 `PatPatPluginAPI.patEntity()`，供其他插件调用；不控制客户端 mod 通信，关掉不影响玩家摸头

## 安装

### 服务端（已装）

1. 从 CurseForge 插件页（https://www.curseforge.com/minecraft/bukkit-plugins/patpat-plugin）或 Modrinth 下载 **Bukkit/Paper 版** jar 放入 `plugins/`
2. 完整重启服务器
3. 生成 `plugins/PatPatPlugin/config.json` 与 `plugins/PatPatPlugin/player-list.txt`（配置格式版本 1.0.1，不是插件版本）

### Java 客户端（玩家必装）

1. 从 Modrinth（同一项目）下载 **Fabric/Forge 版** mod jar
2. 放入客户端 `mods/` 文件夹（Fabric/Forge 加载器）
3. 进入服务器后 **Shift + 右键**生物即可摸头

> 服务端/客户端版本需匹配（同 MC 版本线）；装 mod 的玩家才能摸，未装 mod 的玩家摸不了也看不到动画。

## 命令与权限

主命令用法：`/patpat (info | list | ratelimit | reload | pat | permission)`。

| 命令 | 说明 |
|---|---|
| `/patpat info` | 查看平台 / MC 版本 / 插件版本（报 bug 时用） |
| `/patpat reload` | 重载 `config.json`、`player-list.txt` 与语言文件 |
| `/patpat pat <玩家\|实体>` | 服务端主动触发一次摸头（调试 / 展示） |
| `/patpat list info` | 查看名单模式与内容 |
| `/patpat list set <DISABLED\|WHITELIST\|BLACKLIST>` | 切换名单模式 |
| `/patpat list add <玩家>` / `remove <玩家>` | 增删名单 |
| `/patpat ratelimit info` | 查看开关、参数与各玩家 token |
| `/patpat ratelimit enable` / `disable` | 限流开关 |
| `/patpat ratelimit set <increment\|interval\|limit> <值>` | 设置限流参数（token 增量/间隔/上限） |
| `/patpat permission info` | 查看权限限制状态与节点 |
| `/patpat permission enable` / `disable` | 开关权限限制 |
| `/patpat permission set <节点>` | 更换摸头所需权限节点 |

**功能权限节点**（用 LuckPerms 分配）：

| 权限 | 说明 |
|---|---|
| `patpat.pat` | `permissionRestrictions.enabled: true` 时，发起摸头的玩家必须拥有此节点，摸头数据包才会被处理并广播（中文提示为「玩家必须拥有该权限才能让其他玩家看到他抚摸」） |
| `patpat.ratelimit.bypass` | 限流豁免（无视 token 限制） |

命令权限按 `patpat.command.<子命令>` 生成（如 `patpat.command.reload`、`patpat.command.list.set`、`patpat.command.ratelimit.set.limit`），默认仅 OP 可用。

> 管理命令默认 op 级。若开启权限限制，给全体玩家：
> ```
> lp group default permission set patpat.pat true
> ```

## 配置（`plugins/PatPatPlugin/config.json`，本服实际内容）

> 是 **JSON 文件**，不是 `config.yml`。官方 `doc/en/config.md` 里写的 `player-list.json`、`rateLimit.tokenIncrementInterval` 与实际生成的文件名/字段名不一致；以本服的 `player-list.txt`、`tokenInterval` 为准。

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
| `api` | true | 是否对外暴露插件 API：开启时触发 Bukkit 事件 `PatPacketReceiveEvent`，允许其他插件调用 `PatPatPluginAPI.patEntity()`。**不控制 mod 客户端与服务端之间的通信**，关掉后玩家之间照常摸头 |
| `listMode` | DISABLED | 名单模式：`DISABLED`=所有人可摸；`WHITELIST`=仅 `player-list.txt` 列出的玩家可摸；`BLACKLIST`=列出的玩家不可摸（每行 `<UUID> <昵称>`，空格分隔） |
| `rateLimit.enabled` | false | 限流开关 |
| `rateLimit.tokenLimit` | 20 | 玩家拥有的摸头 token 上限 |
| `rateLimit.tokenIncrement` | 1 | 每个间隔补充的 token 数 |
| `rateLimit.tokenInterval` | "1sec" | 补充间隔（`1sec`/`2min`/`1hour`/`1day`） |
| `rateLimit.permissionBypass` | `patpat.ratelimit.bypass` | 豁免权限节点 |
| `permissionRestrictions.enabled` | false | 摸头权限限制开关 |
| `permissionRestrictions.permissionForPat` | `patpat.pat` | 摸头所需权限节点 |

## 调优建议（本服）

- **`api` 只影响二次开发钩子，不影响玩家摸头**——没有插件调 PatPat API 时，改成 false 也不会让摸头失效；接入了自研插件才需要保持 `true`
- 默认 `listMode: DISABLED` + `permissionRestrictions.enabled: false` = 全服都能摸；想限制"只有装了 mod 的会员能摸"也做不到（mod 是功能前提，权限只是服务端把关）
- 想防刷屏：`rateLimit.enabled: true`，默认 token 20、每秒补 1 个，基本够日常玩；觉得太快可把 `tokenLimit` 调低
- 想让管理/特定玩家无视限流：`lp user <玩家> permission set patpat.ratelimit.bypass true`
- 某玩家恶意骚扰（对他人狂摸）：临时 `lp user <玩家> permission unset patpat.pat`（若已开权限限制），或切 `BLACKLIST` 后 `/patpat list add <玩家>`

## 说明

- 服务端插件 + 客户端 mod 版本需一致（同一 MC 版本线），否则数据包不兼容、摸头无反应
- 摸头动画可用**资源包**自定义并上传 Modrinth/CurseForge 分享（见官方 Wiki）
- **基岩版玩家无法使用本功能**（客户端 mod 无法在基岩端安装）；如需基岩也能互动，考虑纯服务端方案（如右键粒子插件），或接受"仅 Java 玩家可用"
