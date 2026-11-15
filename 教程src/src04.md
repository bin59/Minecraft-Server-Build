# 四、玩法与玩家功能（宠物 / 社交 / 小玩具 / 小游戏）

## SimplePets

### 玩家命令

主命令 `/pet`（别名 /sp、/simplepets、/pets），无参直接打开宠物 GUI。

| 命令 | 说明 |
|---|---|
| `/pet` 或 `/pet gui` | 打开宠物选择/管理界面 |
| `/pet summon <类型>` | 召唤指定宠物，如 `/pet summon wolf` |
| `/pet hat` | 把当前宠物戴到头上 |
| `/pet ride` | 骑乘当前宠物 |
| `/pet rename [名字]` | 给宠物改名 |
| `/pet remove [玩家]` | 移除自己/他人宠物 |
| `/pet list` | 列出当前版本可用的宠物类型 |
| `/pet save` | 保存宠物数据以便日后重召 |
| `/pet modify <数据>` | 修改宠物外观/数据 |
| `/pet purchased` | 查看自己已购买的宠物（配合 Vault 经济 Addon） |

### 管理员命令

| 命令 | 说明 |
|---|---|
| `/pet reload` | 重载配置 |
| `/pet addon` | Addon 管理（禁用/启用刷新，如改价后刷新 Vault Addon） |
| `/pet regenerate` | 重新生成宠物实体 |
| `/pet permissions` | 查看权限相关 |
| `/pet debug` / `/pet report` / `/pet database` / `/pet data` | 调试/数据/数据库管理 |

### 权限节点

前缀为 `Pet.`（官方 Wiki 也写作小写 `pet.`，两者等价）：
- `Pet.*` — 全部权限（服主）
- `Pet.type.*` / `Pet.type.<类型>` — 类型总括/单类型
- `Pet.PetToHat` / `Pet.PetToMount` — 帽子/骑乘
- `Pet.name` / `Pet.name.color` / `Pet.name.magic` — 改名
- `Pet.commands.*` 及各子项 — 命令权限
- **Vault Addon 经济联动（本服已启用）**：`pet.vault.bypass`（免付费总开关）、`pet.commands.purchased`、`pet.commands.addon`

### 典型工作流

**玩家视角（养一只狼）**：`/pet` 打开菜单 → 点狼召唤；或 `/pet summon wolf` → 点 Ride 骑乘、点 Hat 戴头上 → `/pet rename 阿黄` 改名 → 下线/重启后 `/pet`（存档栏）重新召唤。

**管理员视角（发宠物+免单）**：`lp group default permission set Pet.type.wolf true` 授权 → 管理组给 `Pet.type.*`、`Pet.name.*`、`pet.vault.bypass` → 调价编辑 Addons/configs/Vault.yml 后 `/pet addon` 刷新。

### 版本信息

本地 R5-B321（26.3 自编译）/ 官方 Modrinth 最新构建约 R5-B315（2026-09）/ 来源 https://modrinth.com/project/yNVORkCB 、https://github.com/brainsynder-Dev/SimplePets

---

## FancyNpcs

### 玩家命令

普通玩家一般无需权限（NPC 由管理员创建）。玩家仅在 NPC 上按配置的左键/右键/任意点击触发交互动作（发消息、执行命令、传送等），无专属玩家管理命令。

### 管理员命令

`/npc`（管理单个 NPC）与 `/fancynpcs`（插件级）两套，均支持 Tab 补全。

| 命令 | 语法 / 示例 | 说明 |
|---|---|---|
| `/npc create` | `/npc create Guide --type PLAYER` | 新建 NPC |
| `/npc remove` | `/npc remove Guide` | 删除 |
| `/npc list` | `/npc list` | 列出全服 NPC |
| `/npc displayname` | `/npc displayname Guide "<green>向导"` | 设显示名（支持 MiniMessage / PlaceholderAPI） |
| `/npc skin` | `/npc skin Guide Notch` | 皮肤 |
| `/npc equipment` | `/npc equipment Guide set head diamond_helmet` | 六格装备 |
| `/npc move_here` | `/npc move_here Guide` | 把 NPC 传到脚下 |
| `/npc rotate` | `/npc rotate Guide 90 0` | 设定朝向 |
| `/npc visibility` | `/npc visibility Guide all` | 全局可见/需权限/手动 |
| `/npc action` | `/npc action Guide RIGHT_CLICK add message "欢迎回来"` | 配置点击触发动作 |
| `/fancynpcs save` | `/fancynpcs save` | 立即把 NPC 写入 npcs.yml |
| `/fancynpcs reload` | `/fancynpcs reload` | 重载语言、配置与全部 NPC |

**交互动作系统**：`/npc action <npc> <trigger> add <type> [value]`
- 触发器：RIGHT_CLICK / LEFT_CLICK / ANY_CLICK / CUSTOM
- 动作：message、player_command、console_command（{player} 替换为点击者）、send_to_server、wait、need_permission、need_item、play_sound 等
- 示例：`/npc action Guide RIGHT_CLICK add console_command "eco give {player} 5"`

### 权限节点

根前缀 `fancynpcs.command.npc`（给管理员 `fancynpcs.command.*`）。常用：`.create` `.remove` `.list` `.info` `.displayname` `.skin` `.equipment.set` `.move_here` `.rotate` `.visibility` `.action.add` 等。

### 典型工作流

**管理员视角（建一个主城向导）**：`/npc create Guide --type PLAYER` → `/npc displayname Guide "<green>主城向导"` → `/npc skin Guide Notch` → `/npc move_here Guide` → `/npc action Guide RIGHT_CLICK add message "欢迎来到服务器"` → `/fancynpcs save` 落盘。

**玩家视角**：走近 NPC，按配置的左/右键即可触发（如领奖励、看公告、被传送），无需命令。

### 版本信息

本地 2.12.1+382 / 官方 Modrinth 最新约 2.12.0+374（2026-10）/ 来源 https://modrinth.com/plugin/fancynpcs

---

## GSit

### 玩家命令

| 命令 | 说明 |
|---|---|
| `/sit` | 原地坐下（别名 /gsit） |
| `/lay` | 原地躺下（`/layback` 可向后仰躺） |
| `/crawl` | 趴下爬行（1.18+ 可用） |
| `/spin` | 原地旋转 |
| `/sit toggle` | 开关「右键方块坐下」 |
| `/sit playertoggle` | 开关「右键玩家叠坐」 |
| `/crawl toggle` | 开关「双击潜行爬行」 |
| 右键楼梯/台阶/地毯顶部 | 直接坐下（无需命令） |
| 右键玩家 | 坐到对方头顶（叠罗汉，需在配置开启） |

### 管理员命令

- `/gsitreload` — 重载配置与语言文件（别名 /gsitrl）

### 权限节点

- 官方无 `GSit.Use` / `gsit.use` 总开关（旧教程里的该节点无效，勿授予）
- `gsit.sit` / `gsit.lay` / `gsit.crawl` / `gsit.spin`
- `gsit.bypass.world` — 绕过世界黑名单（官方 `GSit.ByPass.World`）
- `GSit.*` — OP 总权限

### 版本信息

本地 3.8.0 / 官方 Spigot 最新 3.8.0（支持至 26.3，2026-11）/ 来源 https://www.spigotmc.org/resources/gsit-modern-sit-seat-and-chair-lay-and-crawl-plugin-1-16-1-21-7.62325

---

## RideOnHead

### 玩家命令

| 命令 | 说明 |
|---|---|
| `/ride` 或 `/ride toggle` | 切换自己是否允许被别人骑到头上 |
| 空手右键玩家 | 骑到对方头顶 |
| 潜行（Shift） | 从被骑者身上下车 |
| `/ride blacklist` | 查看/管理自己的黑名单 |

### 管理员命令

| 命令 | 说明 |
|---|---|
| `/ride blacklist <玩家>` | 把玩家加入/移出某玩家黑名单 |
| `/ride toggle <玩家>` | 强制切换他人是否可被骑 |
| `/ride reload` | 重载配置与语言文件 |

### 权限节点

- `rideonhead.user` — 基础骑乘（默认 true）
- `rideonhead.admin` — 管理命令（默认 OP）

### 版本信息

本地 1.1.3 / 官方 Modrinth 最新 1.1.3（2026-10）/ 来源 https://modrinth.com/plugin/rideonhead

---

## PatPat（摸头）

### 玩家命令

摸头是**客户端 mod 行为**：装了 Java 客户端 PatPat mod 的玩家，潜行（Shift）+ 右键任意活体生物即可摸头；附近同样装了 mod 的玩家能看到动画。玩家**无专属服务端命令**。

> 客户端 mod 有「近距离数据包服务器白名单」，若「别的服能摸、本服不能」，先在本机 mod 设置把本服地址加入白名单。

### 管理员命令

主命令 `/patpat`：

| 命令 | 说明 |
|---|---|
| `/patpat reload` | 重载 config.json、player-list.txt 与语言文件 |
| `/patpat pat <玩家|实体>` | 服务端主动对目标触发一次摸头 |
| `/patpat list set <DISABLED|WHITELIST|BLACKLIST>` | 切换名单模式 |
| `/patpat list add <玩家>` / `remove <玩家>` | 加/移名单 |
| `/patpat ratelimit enable` / `set <increment|interval|limit> <值>` | 开关/设限流 |
| `/patpat permission set <节点>` | 更换摸头所需权限节点 |

### 权限节点

- 命令权限：`patpat.command.reload` / `.pat` / `.list.*` / `.ratelimit.*` / `.permission.*`（默认仅 OP）
- 功能权限：`patpat.pat`（开启权限限制后发起摸头者必须拥有）、`patpat.ratelimit.bypass`

### 版本信息

本地插件 1.2.5 / 官方 Modrinth 最新 mod+plugin v1.3.2（2026-09-23）/ 来源 https://modrinth.com/plugin/patpat

---

## ClickMobs（收纳生物）

### 玩家命令

无命令界面，纯交互操作（服务端点对点功能，玩家无需装任何东西）：

| 操作 | 效果 |
|---|---|
| 手持物品 + 潜行(Shift) + 右键生物 | 把生物收进背包（变成对应生物头颅） |
| 手持对应生物头颅放置 | 生物在原地恢复 |
| 手持头颅右键船/矿车 | 直接把生物放进载具 |

> 默认敌对生物不可捡；与 PatPat 共存：手持物品时 Shift+右键触发收纳，空手时触发摸头，互不冲突。

### 管理员命令

插件的收纳/放置均为玩家交互，无玩家管理命令；如需重载可用对应命令（以服内实际为准）。

### 版本信息

本地 1.2.4 / 官方 Modrinth 最新插件版 1.3.1（2026-03-26）/ 来源 https://modrinth.com/plugin/clickmobs

---

## PlayerHeadDrops（玩家头颅掉落）

### 玩家命令

该插件为死亡自动触发型，**玩家无日常操作命令**；头颅在玩家死亡时按配置概率自动掉落（皮肤嵌入物品，重启不丢）。

### 管理员命令

| 命令 | 说明 |
|---|---|
| `/phd reload` | 重载配置（别名 /phd、/headdrops） |
| `/phd give [receiver]` | 手动给予某玩家的头颅 |

### 权限节点

- `playerheaddrops.boost` — 掉落概率乘倍数
- `playerheaddrops.bypass` — 该玩家死亡**永不掉头**

### 版本信息

本地 2.0.0 / 官方 Spigot/CurseForge 最新 2.0.0（2026-08-08）/ 来源 https://www.spigotmc.org/resources/playerheaddrops.135728/

---

## EvenMoreFish（更多鱼）

### 玩家命令

命令前缀 `/emf`（全名 /evenmorefish）：

| 命令 | 说明 |
|---|---|
| `/emf gui` | 打开图鉴/钓鱼菜单 |
| `/emf next` | 查看下一场钓鱼比赛时间 |
| `/emf top` | 查看当前/最近比赛排行榜 |
| `/emf shop` | 打开卖鱼商店，单条卖鱼换南瓜币 |
| `/emf sellall` | 一键把背包里所有鱼卖掉 |
| `/emf applybaits` | 打开鱼饵涂抹菜单 |
| `/emf toggle` | 开关自己的钓鱼奖励 |
| `/emf journal` | 打开鱼类日记 |

### 管理员命令

| 命令 | 说明 |
|---|---|
| `/emf admin reload` | 重载配置 |
| `/emf admin competition` | 比赛管理（手动开赛等） |
| `/emf admin bait <玩家>` | 发放鱼饵 |

### 权限节点

默认玩家组建议授予：`emf.use`、`emf.gui`、`emf.shop`、`emf.journal`、`emf.applybaits`、`emf.competition`、`emf.toggle`、`emf.next`、`emf.top`、`emf.sellall`；管理员：`emf.admin`。

### 典型工作流

**玩家视角（钓鱼赚钱）**：拿钓鱼竿垂钓 → 钓上自定义鱼 → `/emf gui` 查看图鉴 → 背包满了 `/emf sellall` 一键全卖换南瓜币 → `/emf applybaits` 涂鱼饵提升掉率 → `/emf next` 看比赛时间，`/emf top` 看排行。

### 版本信息

官方 Modrinth 最新 2.5.2（支持 26.3，2026-10）/ 来源 https://modrinth.com/plugin/evenmorefish

---

## BedWars（尖叫起床）

### 玩家命令

| 命令 | 语法 | 说明 | 权限 |
| --- | --- | --- | --- |
| 帮助 | `/bw help` | 显示命令列表 | 无 |
| 加入竞技场 | `/bw join [arena]` | 加入指定竞技场；省略时随机分配 | `bw.cmd.join` |
| 离开竞技场 | `/bw leave` | 离开当前所在竞技场 | `bw.cmd.leave` |
| 列出竞技场 | `/bw list` | 列出所有可用竞技场 | `bw.cmd.list` |
| 个人统计 | `/bw stats` | 显示自己的统计数据 | `bw.cmd.stats` |
| 排行榜 | `/bw leaderboard` | 显示服务器顶尖玩家 | `bw.cmd.leaderboard` |
| 快速重赛 | `/bw rejoin` | 加入最近玩过的一局 | `bw.cmd.rejoin` |
| 随机加入 | `/bw autojoin` | 加入随机一局游戏 | `bw.cmd.autojoin` |

### 管理员命令（竞技场编辑，节选常用）

| 命令 | 语法 | 说明 |
| --- | --- | --- |
| 新建竞技场 | `/bw admin <arena> add` | 创建新竞技场 |
| 设置大厅点 | `/bw admin <arena> lobby` | 把大厅位置设为当前站立点 |
| 竞技场边界 | `/bw admin <arena> pos1` / `pos2` | 两极点 |
| 最少玩家 | `/bw admin <arena> minplayers <minimum>` | 开局所需最少玩家数 |
| 游戏时长 | `/bw admin <arena> time <seconds>` | 单局游戏时长 |
| 添加队伍 | `/bw admin <arena> team add <team> <color> <max players>` | 添加一支队伍 |
| 队伍出生点 | `/bw admin <arena> team spawn <team>` | 队伍出生点 |
| 设置床/目标 | `/bw admin <arena> team bed <team> [looking_at|standing_on]` | 把队伍目标方块设为准星所指 |
| 添加刷怪点 | `/bw admin <arena> spawner add <bronze|gold|iron|...>` | 在当前位置添加资源刷怪点 |
| 添加商店 | `/bw admin <arena> store add` | 在当前位置添加交易村民 |
| 保存并启用 | `/bw admin <arena> save` | 保存游戏并激活 |
| 删除竞技场 | `/bw admin <arena> remove` | 移除竞技场 |
| 重载插件 | `/bw reload` | 重载插件 |

### 权限节点

- `bw.admin` — 创建/编辑竞技场、重载插件、使用作弊命令
- `bw.cmd.join` / `bw.cmd.leave` / `bw.cmd.stats` / `bw.cmd.list` / `bw.cmd.leaderboard` / `bw.cmd.rejoin` / `bw.cmd.autojoin` — 玩家命令（默认放行）
- `bw.otherstats` — 查看其他玩家统计

### 典型工作流

**玩家视角**：`/bw list` 查看竞技场 → `/bw join <arena>` 加入 → 到资源刷怪点收集资源 → 到商店村民处右键购买装备 → 破坏敌方床后击杀获胜 → `/bw stats` 看战绩。

**管理员视角（创建竞技场）**：`/bw admin <arena> add` → 站到两角 `pos1`/`pos2` → `lobby` → `spec` → `team add` 建队 → 逐队 `team spawn` + `team bed` → `spawner add` 资源点 → `store add` 商店 → 设 `minplayers`/`time` → `save`。

### 版本信息

最新版本 0.2.44（支持 MC 1.8.8–26.2）/ 查证日期 2026-11-13 / 来源 https://mcreference.com/plugins/screamingbedwars

---

## VexBot（PvP 机器人）

### 玩家命令

VexBot 没有玩家专属命令——机器人由操作员生成后，玩家直接在游戏内与机器人对战/陪练即可。玩家可感知的功能：陪练 PvP（近战/远程/水晶/鞘翅+锤等多种风格）、FFA 混战、难度梯度（EASY→PERFECT）、机动与补给。

### 管理员命令（默认需权限等级 2 / gamemaster，OP 即可）

命令前缀均为 `/vexbot`，支持 Tab 补全。

**召唤 / 管理**
- `/vexbot` — 打开主 GUI，点选类型/kit 在脚下生成（主推方式）
- `/vexbot spawn [<name>] [<pos>] [<kit>]` — 一条指令带名字/坐标/kit 召唤
- `/vexbot mass_spawn <count> [<pos>]` — 一次生成多个机器人
- `/vexbot kill [<name>]` — 移除指定（或所有）机器人
- `/vexbot list` — 列出当前机器人

**战斗 / 难度**
- `/vexbot attack (stop <bot>|<player> <bot>)` — 让机器人攻击玩家
- `/vexbot follow <bot> (stop|<player>)` — 让机器人跟随玩家
- `/vexbot ffa (start|stop|config ...)` — 开启/关闭 FFA 混战
- `/vexbot difficulty preset <level>` — 设全局难度档（EASY..PERFECT）
- `/vexbot playstyle (list|<style> (all|<targets>))` — 指定战斗风格

**Kit 系统**
- `/vexbot createkit <name>` — 把当前背包+护甲+副手+末影箱存为 kit
- `/vexbot updatekit <name>` / `deletekit [list|<kit>]` / `renamekit <kit> <newName>` — 管理 kit
- `/vexbot givekit [list|<kit> [<player>]]` — 把 kit 发给玩家

> 套用 kit 召唤的前置：需 `/vexbot settings advancedspawn on`（默认 OFF）。参数顺序为 名字 → 坐标 → kit；省略坐标时写 `/vexbot spawn <名字> <kit名>`。

### 权限节点

官方未定义细粒度权限节点；默认执行命令需**权限等级 2（gamemaster）**，即 OP 可执行。若安装 LuckPerms，可按权限组分配谁能使用 /vexbot。

### 典型工作流

**管理员（生成机器人陪练）**：手持/身穿好装备 → `/vexbot createkit <kit名>` 存 kit → `/vexbot settings advancedspawn on` → `/vexbot spawn <名字> <kit名>` → `/vexbot difficulty preset PERFECT` → 练完 `/vexbot kill <名字>` 清空。

**管理员（开启 FFA 混战）**：`/vexbot ffa config randomdifficulties` → `/vexbot ffa start` 自动生成一批机器人开混战 → `/vexbot ffa stop`。

### 版本信息

最新版本 3.1.15+paper-mc26.3（发布 2026-10-25，支持 MC 1.21–26.3）/ 查证日期 2026-11-13 / 来源 https://mcreference.com/mods/vexbot

---

## OpenShulk（背包直开潜影盒）

### 玩家命令

无命令，纯交互操作（服务端插件，玩家无需装任何东西）：

| 操作 | 效果 |
| --- | --- |
| 打开背包，**右键**点击背包里的潜影盒 | 直接弹出 27 格潜影盒界面，不用放到地上 |
| 在界面内存取物品后关闭界面 | 内容写回背包里那个潜影盒 |
| 左键 / Shift+点击 / 数字键换栏 | 不触发打开，走原版行为 |

> 支持普通 + 16 种染色潜影盒；只认**自己背包**里的点击，箱子/末影箱界面里的不算；手持潜影盒对空气右键**不会**打开。

### 管理员命令

插件不注册任何命令，也没有 reload 命令；升级或改动需完整重启服务端。

### 权限节点

`plugin.yml` 未声明权限节点，**所有玩家默认可用**，无需 LuckPerms 授权，也无法按组关掉。

### 典型工作流

**随身仓库**：背包里放几个染色潜影盒分类装物资 → 右键打开对应盒子 → 存取 → 关闭界面 → 盒子继续带着走，省掉「找空地放下 → 挖回」两步。

> 基岩玩家注意：本服经 Geyser 接入，Geyser 无法区分背包 GUI 内的左键 / 右键，基岩端右键可能不生效；打不开时按原版流程把潜影盒放到地上再打开。这类「不落地」打开走自定义界面，**不进 CoreProtect `co_container`**，物品纠纷无法用 `/co` 追溯。

### 版本信息

最新版本 1.3.0（发布 2026-10-08，Paper 26.1–26.3；jar 内 `plugin.yml` 写 1.2.0）/ 查证日期 2026-11-15 / 来源 https://modrinth.com/mod/openshulk · https://hangar.papermc.io/StiviKM/OpenShulk
