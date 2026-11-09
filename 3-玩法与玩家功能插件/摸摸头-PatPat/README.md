# PatPat [Plugin]（摸摸头）

> **一句话**：服务端侧的摸头数据包中转插件。**玩家仍必须安装 Java 客户端 PatPat mod**，插件自己不产生摸头效果，只负责把装了 mod 的客户端之间的摸头动作互相转发，并在服务端做权限、限流与名单控制。
>
> **本服 jar**：`plugins/patpat-plugin-1.2.5.jar`（插件名 `PatPatPlugin`，版本 `1.2.5+0268419`）
>
> **数据目录**：`plugins/PatPatPlugin/`，内含 `config.json`、`player-list.txt`
>
> **项目主页**：https://www.curseforge.com/minecraft/bukkit-plugins/patpat-plugin （Bukkit 插件页，slug 是 `patpat-plugin`）
> **客户端 mod**：https://www.curseforge.com/minecraft/mc-mods/patpat ｜ https://modrinth.com/plugin/patpat
> **源码**：https://github.com/LopyMine/PatPat-Plugin ｜ **Wiki**：https://wiki.nik51.name/en/docs/patpat-plugin ｜ **许可**：LGPL-3.0

## 一、它能做什么、不能做什么

官方对插件的定位是「为**非 Fabric 服务端**提供 PatPat mod 的服务端侧功能」，并且明确标注 **「It still requires mod at the clients!」**。分工如下：

| 环节 | 由谁负责 |
|---|---|
| 摸头动画、爱心粒子、音效、第一人称手臂动作、自定义动画资源包 | **Java 客户端 mod** |
| 判定摸的是哪个生物、把摸头数据包转发给附近玩家、权限 / 限流 / 名单 / 管理命令 | **本插件（服务端）** |

- **触发方式**：潜行（Shift）+ 右键任意**活体生物**——玩家、村民、猫狗、苦力怕、模组生物都可以；默认要求空手。
- **多人可见**：附近同样装了 mod 的玩家能看到别人的摸头动画。没装 mod 的玩家既摸不了，也看不到动画。
- **自定义动画**：用资源包制作，可发布到 Modrinth / CurseForge 分享，做法见官方 Wiki。
- **基岩端不可用**：Geyser / Floodgate 的基岩玩家装不了 Java mod，摸头对基岩端不显示、也无法触发。
- **无硬依赖**：`plugin.yml` 没有 `depend` / `softdepend`；Adventure API 已 shade 进 jar；`folia-supported: true`。
- **不做的事**：不改变生物行为、不掉落物品、不接经济系统、不给基岩端补效果。

## 二、安装

### 服务端

1. 从 CurseForge 插件页下载 Bukkit 构建（主文件 `PatPat Plugin v1.2.5`），放进 `plugins/`。
2. **完整重启**服务端。换 jar 不要用 `/reload`。
3. 首次启动会自动生成数据目录：
   - `plugins/PatPatPlugin/config.json`
   - `plugins/PatPatPlugin/player-list.txt`（空文件，名单用）
4. 控制台或游戏内执行 `/patpat info`，能返回 Platform / Minecraft / Version 即加载成功。

> 插件主页按 MC 版本线提供多个构建，下载时选与服务端对应的那一个；**服务端插件与客户端 mod 要同一版本线**，否则数据包对不上，表现是「摸了没反应」。

### Java 客户端（玩家必装）

1. 从 mod 项目页下载与客户端加载器匹配的 Fabric / Forge / NeoForge / Quilt 版 mod。
2. 放进客户端 `mods/`，用对应加载器启动游戏。
3. 进入服务器后 **Shift + 右键**生物即可摸头。

> 客户端 mod 里有一项「近距离数据包服务器白名单」：默认按白名单工作，只有列进去的服务器才允许近距离数据包。玩家如果反馈「在别的服能摸、在本服不能摸」，先检查这项设置、把本服地址加进去，别急着改服务端配置。

## 三、命令与权限

主命令 `/patpat`，完整用法：`/patpat (info | list | ratelimit | reload | pat | permission)`。子命令都支持 Tab 补全。

| 命令 | 说明 |
|---|---|
| `/patpat info` | 输出平台、MC 版本、插件版本，可点击复制调试信息（报 issue 时贴这段） |
| `/patpat reload` | 重载 `config.json`、`player-list.txt` 与语言文件，无需重启 |
| `/patpat pat <玩家\|实体>` | 服务端主动对目标触发一次摸头，用于调试 / 活动展示；目标不存在、不在线或不是活体实体会给出中文报错 |
| `/patpat list info` | 查看当前名单模式与名单内容 |
| `/patpat list set <DISABLED\|WHITELIST\|BLACKLIST>` | 切换名单模式 |
| `/patpat list add <玩家>` | 把玩家 UUID 加入名单 |
| `/patpat list remove <玩家>` | 从名单移除 |
| `/patpat ratelimit info` | 查看限流开关、三项参数、各玩家剩余 token |
| `/patpat ratelimit enable` / `disable` | 开关限流 |
| `/patpat ratelimit set <increment\|interval\|limit> <值>` | 设置令牌增量 / 补充间隔 / 上限；`interval` 用 `1sec`、`2min`、`1hour`、`1day` 这类格式 |
| `/patpat permission info` | 查看权限限制是否开启、当前所需权限节点 |
| `/patpat permission enable` / `disable` | 开关摸头权限限制 |
| `/patpat permission set <节点>` | 更换摸头所需权限节点（仅允许小写字母、下划线与点，不能含空格） |

**命令权限**按 `patpat.command.<子命令>` 生成：

| 权限节点 | 对应命令 |
|---|---|
| `patpat.command.info` | `/patpat info` |
| `patpat.command.reload` | `/patpat reload` |
| `patpat.command.pat` | `/patpat pat` |
| `patpat.command.list.info` / `list.set` / `list.add` / `list.remove` | `/patpat list ...` |
| `patpat.command.ratelimit.info` / `ratelimit.toggle` | 限流查看与开关 |
| `patpat.command.ratelimit.set.increment` / `set.interval` / `set.limit` | 限流三项参数 |
| `patpat.command.permission.info` / `permission.toggle` / `permission.set` | 权限限制查看、开关、改节点 |

`plugin.yml` 只注册了 `patpat` 命令、没有声明 `permissions` 段，按 Bukkit 默认规则这些节点仅 OP 可用。要给非 OP 管理员放开，用 LuckPerms 显式授权：

```
lp group admin permission set patpat.command.reload true
lp group admin permission set patpat.command.list.set true
```

**功能权限**（不是命令权限，作用于摸头本身）：

| 权限节点 | 生效条件 | 作用 |
|---|---|---|
| `patpat.pat` | `permissionRestrictions.enabled: true` | 发起摸头的玩家必须拥有此节点，摸头数据包才会被服务端正常处理并广播。中文语言包的原话是「玩家必须拥有该权限才能让其他玩家看到他抚摸」 |
| `patpat.ratelimit.bypass` | `rateLimit.enabled: true` | 无视 token 限制，限流信息里显示为「绕过」 |

> 若开启权限限制，记得先给全体玩家发节点，否则会出现「全服都摸不了」：
> `lp group default permission set patpat.pat true`

## 四、配置

配置文件是 **`plugins/PatPatPlugin/config.json`**（JSON，不是 `config.yml`）。本服实际内容：

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

| 字段 | 本服值 | 含义 |
|---|---|---|
| `_info` | — | 配置格式元信息，**整块不要改**。`version: "1.0.1"` 是配置格式版本，不是插件版本，插件靠它做配置迁移 |
| `debug` | `false` | 调试日志开关，排查摸头包问题时临时打开 |
| `api` | `true` | 是否对外暴露插件 API：开启时服务端收到摸头包会触发 Bukkit 事件 `PatPacketReceiveEvent`，并允许其他插件调用 `PatPatPluginAPI.patEntity()` 主动触发摸头。**它不控制客户端 mod 与服务端之间的通信**——关掉后玩家之间照常摸头，只是二次开发钩子失效 |
| `listMode` | `DISABLED` | 名单模式：`DISABLED` 所有人可摸；`WHITELIST` 仅名单内玩家可摸；`BLACKLIST` 名单内玩家不可摸。名单文件见第五节 |
| `rateLimit.enabled` | `false` | 限流开关 |
| `rateLimit.tokenLimit` | `20` | 玩家拥有的摸头 token 上限 |
| `rateLimit.tokenIncrement` | `1` | 每个间隔补充的 token 数 |
| `rateLimit.tokenInterval` | `"1sec"` | 补充间隔，格式如 `1sec` / `2min` / `1hour` / `1day` |
| `rateLimit.permissionBypass` | `patpat.ratelimit.bypass` | 限流豁免权限节点 |
| `permissionRestrictions.enabled` | `false` | 摸头权限限制开关 |
| `permissionRestrictions.permissionForPat` | `patpat.pat` | 摸头所需权限节点 |

> ⚠️ **官方文档与实跑实现有三处名字不一致**：官方 `doc/en/config.md` 写的是名单文件 `player-list.json`、字段 `rateLimit.tokenIncrementInterval`；而 1.2.5 的 jar 与服务端实际生成的是 **`player-list.txt`** 和 **`tokenInterval`**，以本服实际文件为准，照抄官方文档的字段名会被忽略或导致配置读取失败。第三处是 `_info._doc` 里带的文档链接指向 `main` 分支，官方仓库实际分支是 **`master`**，正确地址是 `https://github.com/LopyMine/PatPat-Plugin/blob/master/doc/en/config.md`。

改完配置执行 `/patpat reload` 即可生效；用 `/patpat ratelimit set`、`/patpat list set`、`/patpat permission ...` 改的值也会即时生效，改完用对应的 `info` 子命令确认一次。

## 五、名单文件

`plugins/PatPatPlugin/player-list.txt`，每行一条，格式是 **`<UUID> <昵称>`**（空格分隔）：

```
00000000-0000-0000-0009-01f66567385a .PumpkinVeget
069a79f4-44e9-4726-a5be-fca90e38aaf5 Notch
```

- 第一列必须是合法 UUID，插件用 `UUID.fromString` 解析；格式不对会在日志里报 `Failed to parse line N in PlayerListConfig`。
- 第二列是昵称备注，只用于 `list info` 展示，不参与判定。
- 本服当前是 **空文件** + `listMode: DISABLED`，等于不限制任何人。
- 推荐用 `/patpat list add <玩家>` / `remove <玩家>` 维护，别手敲 UUID；手动编辑后要 `/patpat reload`。

## 六、运维要点与插件联动

- **默认配置是全放开**：`listMode: DISABLED` + `permissionRestrictions.enabled: false` + `rateLimit.enabled: false`，所有装了客户端 mod 的玩家都能摸。
- **防刷屏**：`rateLimit.enabled: true`，默认上限 20、每秒补 1 个 token；嫌太闹可以把 `tokenLimit` 调小或把 `tokenInterval` 拉长。管理账号给 `patpat.ratelimit.bypass`。
- **处理骚扰**：把玩家塞进黑名单（`listMode: BLACKLIST` + `/patpat list add <玩家>`），比临时收权限更精准。
- **`api` 不是「必须开」**：纯玩服关掉不影响摸头；只有接入了调用 PatPat API 的自研插件时才需要保持 `true`。
- **与 ClickMobs 共存**：两者都用 Shift + 右键，但 PatPat 要求**空手**，ClickMobs 收纳不要求空手。因此手持物品时触发收纳、空手时触发摸头，天然分流；详见 `../点击收纳生物-ClickMobs/ClickMobs.md`。
- **语言**：jar 内置 130+ 语言文件（含 `zh_CN.json`），管理命令的提示是中文的。
- **效果截图**：`patpat.webp`（同目录）。

## 七、常见问题

| 现象 | 排查方向 |
|---|---|
| 摸了完全没反应 | ① 客户端没装 mod；② 客户端 mod 与服务端插件 / MC 版本线不匹配；③ 客户端 mod 的「近距离数据包服务器白名单」没加本服 |
| 自己能摸，别人看不到 | 开了 `permissionRestrictions.enabled` 但没给 `patpat.pat`；或对方客户端没装 mod |
| 摸一会儿就不动了 | 开了限流，token 耗尽；`/patpat ratelimit info` 看剩余量，或给 `patpat.ratelimit.bypass` |
| 基岩玩家用不了 | 正常，Java 客户端 mod 装不进基岩端；目前没有基岩侧替代 |
| 改了 `config.json` 不生效 | 用 `/patpat reload`；确认改的是 `plugins/PatPatPlugin/config.json` 而不是别处的 `config.yml` |
| 名单加了人没效果 | 检查 `listMode` 是否真的切到 `WHITELIST` / `BLACKLIST`（`/patpat list info`），改完名单要 reload |
| 日志刷 `Failed to parse line N in PlayerListConfig` | `player-list.txt` 有非法行，第一列必须是标准 UUID |
| 换 jar 后命令不识别 | 用了 `/reload`；停服完整重启，并确认 `plugins/` 里只留一个 PatPat jar |
