# FancyNpcs（NPC 系统）

> **一句话**：基于**数据包（packet）**的轻量 NPC 插件。NPC 只存在于插件内存与客户端画面里，不注册成服务端实体，服务器不用为它跑 AI、碰撞、寻路和区块加载，因此大量 NPC 也几乎不占主线程。
>
> **本地 jar**：`所需文件/插件/3-玩法与玩家功能插件/FancyNpcs/FancyNpcs-2.12.1+382.jar`（7.3 MB，版本 `2.12.1+382`）
>
> **数据目录**：`plugins/FancyNpcs/`
>
> **项目主页**：https://modrinth.com/plugin/fancynpcs ｜ **官方文档**：https://fancyinnovations.com/docs/minecraft-plugins/fancynpcs ｜ **源码**：https://github.com/FancyMcPlugins/FancyNpcs ｜ **许可**：MIT
>
> **部署状态**：实跑服 `plugins/` 目前没有该 jar，也没有 `plugins/FancyNpcs/` 数据目录。下文的配置键与默认值取自本地 jar 内的 `FancyNpcsConfigImpl` 与内置语言文件，命令与权限取自 jar 内注解 + 官方命令参考；首次启动后才会生成 `config.yml`。

---

## 一、能做什么、不能做什么

| 能 | 说明 |
|---|---|
| 实体类型 | `PLAYER` 及绝大多数原版生物（牛、猪、僵尸、村民、盔甲架、展示实体等） |
| 皮肤 | 玩家名 / UUID / 占位符 / 图片 URL / 本地 PNG 文件 / `@mirror`（每个观众看到自己的皮肤）/`@none` |
| 外观 | 显示名（MiniMessage + PlaceholderAPI + MiniPlaceholders）、全色发光、六格装备、缩放、姿态与生物变种等属性 |
| 交互 | 左键 / 右键 / 任意点击 / 自定义触发，按顺序执行消息、命令、传送服务器、延时、权限与物品门槛等动作 |
| 可见性 | 全局可见 / 需权限 / 手动控制；可见距离、Tab 列表显示、是否碰撞、是否转向玩家 |
| 管理 | 创建 / 复制 / 删除 / 列表 / 附近 / 详情 / 传送 / 移动 / 居中 / 旋转 / 修复，全部游戏内命令完成，Tab 补全完整 |

| 不能 | 说明 |
|---|---|
| AI / 寻路 / 自主移动 | NPC 是静态虚拟投影，不会走路、不会打怪、不会与方块交互；需要这类能力得用 Citizens 一类插件 |
| Spigot / Bukkit / Sponge / Fabric | 只支持 **Paper 与 Folia**（及其分支，如 Leaf、Purpur、Pufferfish） |
| 直接改 `npcs.yml` | 官方明确不建议手改，坐标与序列化格式容易写坏；一律用命令改，再 `/fancynpcs save` 落盘 |
| 与 Citizens 共用 `/npc` | 两者都会抢 `/npc` 命令，只能二选一；本服未装 Citizens |
| 基岩端无脑生效 | NPC 走 Java 版实体数据包，Geyser 是否完整转发（尤其发光、属性、皮肤）需在互通服真机确认 |

---

## 二、安装

| 项目 | 要求 |
|---|---|
| 服务端 | Paper **1.21.4+** 或其分支；Leaf 26.3 属于 Paper 分支，在官方支持列表内 |
| Java | **25+** |
| Folia | `folia-supported: true`，支持 |
| 硬依赖 | **无**（`paper-plugin.yml` 里全部 `required: false`） |
| 软依赖 | `Citizens`、`MiniPlaceholders`、`PlaceholderAPI`、`PlotSquared`（均 `load: BEFORE`，装了才启用对应集成） |

```bash
# 1) 把 jar 放进 plugins/，停服完整重启（不要 /reload 换 jar）
# 2) 验证安装
/fancynpcs version
# 3) 建第一个 NPC
/npc create Guide --type PLAYER
/npc displayname Guide "<green>主城向导"
/npc skin Guide Notch
/npc move_here Guide
/npc action Guide RIGHT_CLICK add message "欢迎来到南瓜生存服，%player_name%！"
# 4) 落盘
/fancynpcs save
```

> NPC 名称有严格校验：只允许 `A-Z a-z 0-9 _ - /`，中文名要用 `/npc displayname` 设置显示名。

---

## 三、数据与配置文件

```
plugins/FancyNpcs/
├─ config.yml                 全局配置（首次启动生成，见下表 20 个键）
├─ npcs.yml                   NPC 数据库（每个 NPC 一段，勿手改）
├─ languages/
│  └─ default.yml             消息模板；复制改名（如 zh.yml）→ 改 language_name
│                            → 把 config.yml 的 language 改成对应值 → /fancynpcs reload
└─ skins/                     本地皮肤 PNG，命令里只填文件名
```

`npcs.yml` 结构示意（**只读参考**）：

```yaml
Guide:
  type: PLAYER
  world: world
  x: 10.5
  y: 64.0
  z: -25.5
  yaw: 90.0
  pitch: 0.0
  glowing: false
  turn_to_player: true
  skin:
    value: "<Mojang 签名纹理>"
    signature: "<签名>"
  actions:
    RIGHT_CLICK:
      - type: MESSAGE
        value: "欢迎光临"
```

---

## 四、`config.yml` 逐键说明（20/20）

默认值来自 jar 内 `FancyNpcsConfigImpl` 字节码，首次启动按这些值写入配置文件。

| 键 | 默认值 | 说明 |
|---|---|---|
| `language` | `default` | 使用的语言文件，对应 `languages/` 下的 `language_name` |
| `skip_invisible_npcs` | `true` | 对隐身 NPC 不向玩家发包，省带宽 |
| `disable_interaction_cooldown_message` | `false` | 是否关掉「交互冷却中」提示 |
| `mute_version_notification` | `false` | 是否静音版本更新提醒 |
| `enable_autosave` | `true` | 是否定时自动保存 NPC |
| `autosave_interval` | `15` | 自动保存间隔，**分钟** |
| `npc_update_interval` | `60` | 皮肤/显示名使用占位符时的刷新间隔，**秒** |
| `npc_update_visibility_interval` | `20` | 可见性刷新间隔，**tick** |
| `turn_to_player_tracker_interval` | `3` | 「转向玩家」追踪器间隔，**tick** |
| `register_commands` | `true` | 是否注册 `/npc`、`/fancynpcs` 命令；关掉后只能用 API/其他插件调用 |
| `turn_to_player_distance` | `5` | NPC 开始转向玩家的距离（格） |
| `turn_to_player_reset_to_initial_direction` | `false` | 玩家走出转向范围后，NPC 是否转回初始朝向 |
| `visibility_distance` | `20` | NPC 可见距离（格） |
| `spawn_delay_on_join` | `40` | 玩家进服后延迟多少 **tick** 才给他生成 NPC；填负数会告警并回退 40 |
| `mineskin_api_key` | `""` | MineSkin API Key，用于加快 URL 皮肤签名；在 https://mineskin.org/account 申请 |
| `mineskin_timeout` | `3000` | MineSkin 请求超时，**毫秒** |
| `remove_npcs_from_playerlist_delay` | `2000` | 从 Tab 列表移除 NPC 的延迟，**毫秒**；皮肤加载异常可调大，不用 `show_in_tab` 可设 `-1` |
| `swing_arm_on_update` | `true` | NPC 刷新时是否摆动手臂 |
| `blocked_commands` | `[op, ban]` | 禁止 NPC 动作里执行的命令（安全防护） |
| `max-npcs` | 见下 | 按权限限制玩家可建的 NPC 数量，仅 `player-npcs` 特性标志开启时生效 |

```yaml
# max-npcs 默认写法（list of map，不是普通 map）
max-npcs:
  - fancynpcs.max-npcs.5: 5
  - fancynpcs.max-npcs.10: 10
```

**调优方向**：NPC 多、主城卡顿时先调 `visibility_distance`（默认 20）、`npc_update_visibility_interval`（20 tick）、`turn_to_player_tracker_interval`（3 tick）；用占位符的 NPC 多则调大 `npc_update_interval`。这三项直接决定发包频率。

---

## 五、命令

### 5.1 管理命令 `/fancynpcs`

| 命令 | 权限 | 说明 |
|---|---|---|
| `/fancynpcs version` | `fancynpcs.command.fancynpcs.version` | 显示版本并检查更新 |
| `/fancynpcs reload` | `fancynpcs.command.fancynpcs.reload` | 重载语言、配置与全部 NPC |
| `/fancynpcs save` | `fancynpcs.command.fancynpcs.save` | 立即把 NPC 写入 `npcs.yml` |
| `/fancynpcs feature_flags` | `fancynpcs.command.fancynpcs.feature_flags` | 查看特性标志状态与 NPC 总数 |
| `/fancynpcs run_tests` | `fancynpcs.command.fancynpcs.run_tests` | 运行内置自检；另有 `skin_system` 子命令（清缓存/清队列/重启皮肤调度器/查看状态） |

`feature_flags` 会列出三项：`Player NPCs`（玩家自建 NPC）、`Native threads`、`Debug mode`。另有 `/npcconvert citizens`（权限 `fancynpcs.command.npcconvert.citizens`）用于从 Citizens 迁移。

### 5.2 `/npc` 基础管理

| 命令 | 语法 | 说明 |
|---|---|---|
| `help` | `/npc help [page]` | 分页帮助（内置点击直达官方文档） |
| `create` | `/npc create <name> [--position <x y z>] [--world <world>] [--type <type>]` | 新建 NPC，名称限 `A-Z a-z 0-9 _ - /` |
| `copy` | `/npc copy <npc> <new_name>` | 复制一个 NPC |
| `remove` | `/npc remove <npc>` | 删除 |
| `list` | `/npc list [--type <type>] [--sort <sort>]` | 列出全服 NPC，可筛选排序 |
| `nearby` | `/npc nearby [--radius <n>] [--type <type>] [--sort <sort>]` | 列出本世界附近的 NPC |
| `info` | `/npc info <npc>` | 查看单个 NPC 全部属性 |
| `fix` | `/npc fix <npc>` | NPC 异常时重新生成（目前等价于 respawn） |

### 5.3 外观

| 命令 | 语法 | 说明 |
|---|---|---|
| `type` | `/npc type <npc> <type>` | 改实体类型 |
| `displayname` | `/npc displayname <npc> (@none \| <name>)` | 显示名，支持 MiniMessage / PlaceholderAPI / MiniPlaceholders；`@none` 清空 |
| `skin` | `/npc skin <npc> (@none \| @mirror \| name \| uuid \| placeholder \| url \| file) [--slim]` | 皮肤来源；`--slim` 仅对 URL / 文件皮肤有效 |
| `glowing` | `/npc glowing <npc> [disabled \| <color>]` | 发光及颜色（原版 16 色） |
| `equipment` | `/npc equipment <npc> (set \| clear \| list)`，`set <slot> (@hand \| @none \| <material>)` | 六格装备：`main_hand`/`off_hand`/`head`/`chest`/`legs`/`feet` |
| `scale` | `/npc scale <npc> <factor>` | 模型缩放（1.20.5+，本服可用） |
| `attribute` | `/npc attribute <npc> (set <attr> <value> \| list)` | 姿态、变种等实体属性；可用项随类型变化，用 `list` 查 |
| `show_in_tab` | `/npc show_in_tab <npc> [state]` | 是否进 Tab 列表，仅 `PLAYER` 类型；改动后可能要重连 |
| `collidable` | `/npc collidable <npc> [state]` | 是否与其他实体碰撞 |

### 5.4 位置、朝向与可见性

| 命令 | 语法 | 说明 |
|---|---|---|
| `move_here` | `/npc move_here <npc>` | 把 NPC 传到你脚下（需要 `move_here` 权限） |
| `move_to` | `/npc move_to <npc> <x> <y> <z> [world] [--look-in-my-direction]` | 把 NPC 传到坐标 |
| `teleport` | `/npc teleport <npc>` | 把**你**传到 NPC（方向别和 `move_to` 搞反） |
| `rotate` | `/npc rotate <npc> <yaw> <pitch>` | 设定朝向 |
| `center` | `/npc center <npc>` | 把 NPC 对齐到所在方块中心 |
| `turn_to_player` | `/npc turn_to_player <npc> [state]` | 玩家靠近时是否转身看向玩家 |
| `turn_to_player_distance` | `/npc turn_to_player_distance <npc> <state>` | 触发转向的距离（jar 内置帮助写作 `state`，以 Tab 补全为准） |
| `visibility` | `/npc visibility <npc> (all \| permission_required \| manual)` | 全局可见 / 需权限 / 手动控制 |
| `visibility_distance` | `/npc visibility_distance <npc> (always_visible \| default \| not_visible \| distance)` | 单个 NPC 的可见距离策略 |
| `interaction_cooldown` | `/npc interaction_cooldown <npc> (disabled \| <time>)` | 交互冷却，时间带单位：`ms`/`s`/`min`/`h`/`d`/`mo`/`y`，如 `2min` |

### 5.5 交互动作 `/npc action`

| 命令 | 语法 | 权限 |
|---|---|---|
| `add` | `/npc action <npc> <trigger> add <type> [value]` | `fancynpcs.command.npc.action.add` |
| `add_before` | `/npc action <npc> <trigger> add_before <index> <type> [value]` | `…action.addBefore` |
| `add_after` | `/npc action <npc> <trigger> add_after <index> <type> [value]` | `…action.addAfter` |
| `set` | `/npc action <npc> <trigger> set <number> <type> [value]` | `…action.set` |
| `remove` | `/npc action <npc> <trigger> remove <number>` | `…action.remove` |
| `move_up` / `move_down` | `/npc action <npc> <trigger> move_up\|move_down <number>` | `…action.moveUp` / `…moveDown` |
| `clear` | `/npc action <npc> <trigger> clear` | `…action.clear` |
| `list` | `/npc action <npc> <trigger> list` | `…action.list` |

> 添加动作还要额外持有按类型细分的权限：`fancynpcs.command.npc.action.add.<type>` 或通配 `…action.add.*`。

---

## 六、权限节点

根命令：`fancynpcs.command.npc`。用 LuckPerms 可给管理员 `fancynpcs.command.*`，玩家一般一个都不给。

| 分类 | 权限节点 |
|---|---|
| 基础管理 | `.help` `.create` `.copy` `.remove` `.list` `.info` `.nearby` `.fix` |
| 外观 | `.type` `.displayname` `.skin` `.glowing` `.scale` `.show_in_tab` `.collidable` |
| 装备 / 属性 | `.equipment.set` `.equipment.clear` `.equipment.list` `.attribute.set` `.attribute.list` |
| 位置朝向 | `.move_here` `.move_to` `.teleport` `.rotate` `.center` `.turn_to_player` `.turn_to_player_distance` |
| 可见性 / 冷却 | `.visibility` `.visibility_distance` `.interaction_cooldown` |
| 动作 | `.action.add` `.action.addBefore` `.action.addAfter` `.action.set` `.action.remove` `.action.moveUp` `.action.moveDown` `.action.clear` `.action.list`、`action.add.<type>` / `action.add.*` |

以上节点前缀均为 `fancynpcs.command.npc`。`/fancynpcs` 系列见 5.1，皮肤系统排障为 `fancynpcs.command.fancynpcs.skin_system.*`（`clear_cache` / `clear_queues` / `clear_uuid_cache` / `restart_schedulers` / `scheduler_status`）。

---

## 七、交互动作系统

**触发器（trigger）**：`ANY_CLICK`、`LEFT_CLICK`、`RIGHT_CLICK`、`CUSTOM`（自定义，供 API/其他插件触发）。

**动作类型（type）**，取自 jar 内 12 个 `NpcAction` 实现：

| type | 作用 |
|---|---|
| `message` | 给点击者发消息，支持 MiniMessage、PlaceholderAPI |
| `player_command` | 以玩家身份执行命令 |
| `player_command_as_op` | 临时提权为 OP 后以玩家身份执行 —— **官方明确标注有安全风险，未来版本会移除，别用** |
| `console_command` | 控制台执行，`{player}` 会替换为点击者名字 |
| `send_to_server` | 通过 BungeeCord / Velocity 把玩家送到其他子服 |
| `wait` | 等待若干 tick，用来串成对话节奏 |
| `block_until_done` | 当前动作序列跑完前禁止再次点击 |
| `need_permission` | 权限门槛，常与下一条 `player_command` 组合 |
| `need_item` | 需要持有指定数量物品（语言文件里有对应失败提示） |
| `play_sound` | 播放音效 |
| `execute_random_action` | 随机执行一个动作 |
| `unknown_action` | 未知动作的占位实现 |

同一 trigger 下的动作**按顺序执行**，用 `list` 查编号、`move_up`/`move_down` 调顺序。

```bash
# 主城向导：右键先问好 → 等 2 秒 → 控制台给 5 南瓜币 → 传送到商店
/npc action Guide RIGHT_CLICK add message "<green>欢迎回来，%player_name%！"
/npc action Guide RIGHT_CLICK add wait 40
/npc action Guide RIGHT_CLICK add console_command "eco give {player} 5"
/npc action Guide RIGHT_CLICK add player_command "warp shop"
/npc action Guide RIGHT_CLICK list
/fancynpcs save
```

> `blocked_commands` 默认拦 `op`、`ban`；动作里**不要**用 `player_command_as_op`。jar 内置语言文件会在管理员进服时主动告警列出所有用到该动作的 NPC。

---

## 八、皮肤

| 来源 | 写法 | 备注 |
|---|---|---|
| 无皮肤 | `@none` | 移除 |
| 镜像观众 | `@mirror` | 每个玩家看到自己的皮肤 |
| 正版玩家 | 玩家名 / UUID | 走 Mojang 会话服务器，有速率限制，改名换肤会变 |
| 占位符 | PlaceholderAPI 占位符 | 配合 `npc_update_interval` 刷新 |
| 图片 URL | 完整 `https://…png` | 需经 MineSkin 签名；配 `mineskin_api_key` 更快，`mineskin_timeout` 控制超时 |
| 本地文件 | 只填文件名，如 `guide.png` | PNG 放 `plugins/FancyNpcs/skins/`；`--slim` 用 Alex 细手臂模型 |

本目录里的 PNG 即为皮肤贴图素材，需要用时复制到服务器 `plugins/FancyNpcs/skins/`，再执行 `/npc skin <npc> <文件名>`。

---

## 九、运维要点与插件联动

- **ClearLag++**：本服清理插件对 FancyNpcs **免疫**（NPC 不受实体清理影响），见 `5-服务器管理/09-ClearLag++-清理与优化/README.md` 第七节。因为 NPC 不是真实实体，不要把它加进清理白名单/排除表，也不需要。
- **Geyser / Floodgate**：NPC 是 Java 版数据包实体，基岩端能否完整显示（尤其 Item Display、发光颜色、实体属性）需互通服真机确认；出现「Java 端看得到、基岩端看不到」优先排查 Geyser 的实体映射，而不是视距或配置。
- **PlaceholderAPI**：装了之后显示名、消息动作可用 `%player_name%` 等变量；`MiniPlaceholders` 同理。`PlotSquared` 与 `player-npcs` 特性标志联动（限建在自己的地皮、按 `max-npcs` 限制数量）。
- **保存与备份**：改动只存在内存里，靠 `enable_autosave`（默认 15 分钟）或 `/fancynpcs save` 落盘。备份服务器时把整个 `plugins/FancyNpcs/` 一起带走；恢复前先停服，避免自动保存覆盖。
- **热重载**：改 `config.yml` / 语言文件用 `/fancynpcs reload`；**换 jar 必须完整重启**，不要 `/reload`。
- **匿名统计**：插件含可关闭的遥测（不收集 IP、服务器名），说明与退出方式见 https://fancyinnovations.com/docs/minecraft-plugins/data-collection 。
- **Citizens 冲突**：若以后装 Citizens，两者会抢 `/npc`，需二选一或用 `/fancynpcs` 命名空间区分；`/npcconvert citizens` 可迁移旧数据。

---

## 十、常见问题

| 现象 | 处理 |
|---|---|
| NPC 不显示 / 忽隐忽现 | 查 `/npc visibility` 与 `visibility_distance`；再查全局 `visibility_distance`（默认 20）、`spawn_delay_on_join`（40 tick） |
| 皮肤是默认 Steve | URL 皮肤签名失败 → 配 `mineskin_api_key` 或改用本地 PNG；`show_in_tab` 相关的加载不全可调大 `remove_npcs_from_playerlist_delay` |
| 基岩玩家看不到 | 见第九节 Geyser 说明 |
| 命令提示没权限 | 按第六节补节点；添加动作还要 `action.add.<type>` 或 `action.add.*` |
| 建 NPC 报名字非法 | 名称只允许 `A-Z a-z 0-9 _ - /`，中文请放到 `displayname` |
| 改了配置没生效 | `/fancynpcs reload`；换 jar 则必须重启 |
| 动作只执行了一半 | 用 `list` 看顺序，检查 `need_permission` / `need_item` 门槛和 `wait` 时长 |
| 与 Citizens 抢命令 | 见第九节 |

---

## 十一、出处

- 项目主页（下载 / 版本 / 更新日志）：https://modrinth.com/plugin/fancynpcs
- 官方文档：https://fancyinnovations.com/docs/minecraft-plugins/fancynpcs
- 命令参考：https://fancyinnovations.com/docs/minecraft-plugins/fancynpcs/commands/npc
- 入门：https://fancyinnovations.com/docs/minecraft-plugins/fancynpcs/getting-started
- 开发者 API / Javadoc：https://fancyinnovations.com/docs/minecraft-plugins/fancynpcs/api/getting-started ｜ https://repo.fancyinnovations.com/javadoc/releases/de/oliver/FancyNpcs/latest
- 源码：https://github.com/FancyMcPlugins/FancyNpcs ｜ Discord：https://discord.gg/ZUgYCEJUEx
- 本地校验依据：`FancyNpcs-2.12.1+382.jar` 内 `paper-plugin.yml`、`version.yml`、`languages/default.yml`、`FancyNpcsConfigImpl.class` 与 `commands/lampCommands/**` 注解
