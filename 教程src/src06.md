# 六、权限 / 数据 / 分析核心

## LuckPerms（权限管理）

### 玩家命令

普通玩家**无**可直接使用的 LuckPerms 命令，权限与组由管理员分配。玩家能"感知"到的功能是：聊天前缀、命令可用性，这些均由管理员的权限配置决定。

### 管理员命令

命令别名：Bukkit 端用 `/lp`（全名 /luckperms）。参数规则：`<必填>` 必写，`[可选]` 可省；含空格参数用双引号包裹；临时时长支持 `1mo3d13h45m`（s/m/h/d/w/mo/y）。

**用户（玩家）相关** —— 格式 `/lp user <玩家> ...`

| 命令 | 说明 | 示例 |
|---|---|---|
| `info` | 查看玩家组、节点、meta | `/lp user Steve info` |
| `permission set <节点> [true|false] [上下文]` | 授予/否定权限 | `/lp user Steve permission set essentials.fly true` |
| `permission unset <节点> [上下文]` | 移除权限 | `/lp user Steve permission unset essentials.fly` |
| `permission settemp <节点> <true|false> <时长> [上下文]` | 临时权限 | `/lp user Steve permission settemp essentials.fly false 2h` |
| `permission check <节点>` | 检查是否拥有及来源 | `/lp user Steve permission check coreprotect.rollback` |
| `parent add <组> [上下文]` | 加入组（继承） | `/lp user Steve parent add admin` |
| `parent remove <组>` | 退出组 | `/lp user Steve parent remove admin` |
| `parent addtemp <组> <时长>` | 临时入组 | `/lp user Steve parent addtemp vip 7d` |
| `meta addprefix <优先级> <前缀>` | 追加前缀（数字越小越靠前） | `/lp user Steve meta addprefix 10 "&8[&7VIP&8] "` |
| `promote <轨道>` / `demote <轨道>` | 沿轨道升降级 | `/lp user Steve promote rank` |

**组相关** —— 格式 `/lp group <组名> ...`

| 命令 | 说明 | 示例 |
|---|---|---|
| `info` | 查看组信息 | `/lp group admin info` |
| `permission set <节点> [true|false] [上下文]` | 给组授权 | `/lp group admin permission set * true` |
| `parent add <组>` | 组继承 | `/lp group admin parent add default` |
| `setweight <权重>` | 设置权重（越高越优先） | `/lp group admin setweight 80` |
| `meta addprefix <优先级> <前缀>` | 组前缀 | `/lp group admin meta addprefix 1 "&8[&c管理&8] "` |
| `listmembers [页]` | 列出成员 | — |

**通用 / 维护类**
- `/lp creategroup <组名> [权重] [显示名]` — 创建权限组（**注意不是** /lp group xxx create）
- `/lp deletegroup <组名>` — 删除权限组
- `/lp listgroups` — 列出所有组
- `/lp editor [类型] [过滤]` — 网页编辑器（类型：all/users/online/groups）
- `/lp verbose on <过滤>` — 实时监听权限判定（on/record/off/upload）
- `/lp search [比较符] <权限>` — 搜索节点
- `/lp export <文件|--upload>` / `/lp import <文件|码 --upload>` — 导出/导入备份
- `/lp sync` — 多端改完数据后重新读取存储

### 权限节点

| 节点 | 说明 |
|---|---|
| `luckperms.*` | 全部（仅服主） |
| `luckperms.info` / `luckperms.sync` / `luckperms.verbose` / `luckperms.editor` | 基础只读/维护 |
| `luckperms.import` / `luckperms.export` | 导入/导出（谨慎） |
| `luckperms.migration` | 存储迁移（谨慎） |

细粒度：`luckperms.<命令>.<子命令>`。普通管理建议只给需要的子节点，**绝不给** `luckperms.*` 给非服主。

### 典型工作流

**新建组并分配权限**：`/lp creategroup vip 50 "&8[&6VIP&8]"` → `/lp group vip setweight 50` → `/lp group vip meta setprefix 1 "&8[&6VIP&8] "` → `/lp group vip permission set essentials.fly true` → `/lp user Steve parent add vip` → 验证 `/lp user Steve permission check essentials.fly`。

**临时处罚（临时禁飞）**：`/lp user Steve permission settemp essentials.fly false 2h`。

### 版本信息

最新版本：LuckPerms v5.5.91（本地部署同为 5.5.91）/ 查证日期 2026-11-13 / 来源 https://luckperms.net/download

---

## Plan（玩家数据分析）

### 玩家命令

玩家主要使用 Web 面板与少量游戏内查询命令：

| 命令 | 语法 | 说明 |
|---|---|---|
| 玩家数据页 | `/plan player <玩家>` | 获取该玩家 Web 分析页链接（别名 inspect） |
| 游戏内查看 | `/plan ingame <玩家>` | 聊天框内直接看数据（别名 qinspect） |
| 搜索玩家 | `/plan search <关键词>` | 按名搜索玩家 |
| 注册面板账号 | `/plan register` | 注册 Web 面板登录账号 |
| 插件状态 | `/plan info` | 插件信息 |

> 玩家也可直接浏览器访问服务器 Web 面板查看在线人数、TPS、自己的会话/击杀/死亡/在线时长等。

### 管理员命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 服务器页 | `/plan server` | 当前服分析页链接 |
| 服务器列表 | `/plan servers` | 列出数据库中的服务器 |
| 网络页 | `/plan network` | 群组聚合页链接 |
| 重载配置 | `/plan reload` | 重载 Plan 配置 |
| 数据管理 | `/plan db ...` | 备份/恢复/合并/清空（谨慎） |

### 权限节点

- `plan.player.self` / `plan.player.other` — 查自己 / 他人数据
- `plan.ingame.self` / `plan.ingame.other` — 游戏内数据视图
- `plan.search` — 搜索玩家
- `plan.register.self` / `.other` — 注册/注销面板账号
- `plan.server` / `plan.servers` / `plan.network` — 服/跨服数据
- `plan.reload` — 重载配置
- `plan.data.*` — 数据管理（仅服主）
- `plan.*` — 全部（服主）

### 典型工作流

**玩家视角**：进服后浏览器打开 Web 面板地址 → 输入游戏名 → 查看在线时长、K/D、活动指数、死亡/击杀记录；或用 `/plan player 自己名` 直接拿链接。

**管理员视角（排查异常玩家）**：`/plan search 关键词` 找到目标 → `/plan player <玩家>` 打开数据页查会话规律 → 必要时游戏内 `/plan ingame <玩家>` 核对。

### 版本信息

最新版本：Plan 5.8 build 3638（发布 2026-09-05）/ 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/plan-player-analytics.32536/

---

## TAB

### 玩家命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 侧边栏开关 | `/sb` | 个人显示/隐藏右侧公告栏 |
| 计分板开关 | `/tab scoreboard off` / `on` | 隐藏/恢复侧边栏 |
| 名牌开关 | `/tab nametag toggle [.other]` | 显示/隐藏头顶称号 |

> 玩家无需任何权限即可开关侧边栏（`tab.scoreboard.toggle` 默认开放）。TAB 列表、头顶前缀等由管理员配置，玩家进服即自动看到。

### 管理员命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 重载配置 | `/tab reload` | 重读所有配置文件 |
| 调试信息 | `/tab debug` | 输出调试 |
| CPU 占用 | `/tab cpu` | 各功能耗时 |
| 解析测试 | `/tab parse <文本>` | 测试占位符解析结果 |
| 设置组前缀 | `/tab group <组名> tabprefix <前缀>` | 为指定权限组设 TAB 前缀 |
| 设置玩家前缀 | `/tab player <玩家> tabprefix <前缀>` | 为单个玩家设前缀 |
| 公告 | `/tab announce bar <文本>` / `send.bar` | 发送 bossbar/计分板公告 |

### 权限节点

- `tab.admin` — 全部管理（`/tab reload` 等）
- `tab.reload` / `tab.cpu` / `tab.group.*` / `tab.change.*` / `tab.bypass` / `tab.seevanished`
- `tab.nametag.toggle`(+`.other`) / `tab.scoreboard.toggle`(+`.other`) — 玩家默认可用
- `tab.announce.bar` / `tab.parse` / `tab.*`

### 典型工作流

**玩家视角**：进服按 Tab 看美化后的玩家列表（含前缀、ping）；右侧公告栏显示在线人数、余额、在线时长、TPS；觉得挡视野就 `/sb` 隐藏。

**管理员视角（调整某组显示）**：确认玩家所属组 → `/tab group admin tabprefix "&4[&c管理&4] "` → 验证 `/tab groupinfo admin` → `/tab reload`。

### 版本信息

最新版本：TAB 6.2.1（发布 2026-10-18）/ 查证日期 2026-11-13 / 来源 https://github.com/NEZNAMY/TAB/releases

---

## PlaceholderAPI（占位符）

### 玩家命令

普通玩家**无**可直接使用的命令——他们只是"被占位符渲染的文本"的读者（例如 TAB 列表、计分板、聊天里的 %vault_eco_balance%）。所有管理命令默认仅 OP 可用。

### 管理员命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 列出扩展 | `/papi list` | 已装扩展及版本 |
| 查占位符 | `/papi info <占位符>` | 查看某占位符来源与示例 |
| 手动解析 | `/papi parse <玩家> <文本>` | 解析含 %...% 的文本（调试） |
| 自己解析 | `/papi parse me <文本>` | 以自己为上下文 |
| 云端浏览器 | `/papi ecloud` | 打开在线扩展列表 |
| 下载扩展 | `/papi ecloud download <名称>` | 安装扩展（如 Vault/Plan/LuckPerms） |
| 重载 | `/papi reload` | 重载配置与扩展 |

> 本服已装 player/server/vault 三个扩展；LuckPerms、TAB、SimplePets 等自带 PAPI 扩展，无需额外下载。

### 权限节点

- `placeholderapi.*` — 全部（管理）
- `placeholderapi.info` / `.list` / `.parse` / `.reload` / `.ecloud` / `.ecloud.*` / `.updatenotify`

### 典型工作流

**让 TAB/计分板显示余额**：`/papi list` 看到 Expansion-vault → 若无 `/papi ecloud download Vault` → `/papi reload` → `/papi parse me "%vault_eco_balance%"` 测试 → 在 TAB/计分板配置里用 `%vault_eco_balance%`。

**占位符不显示排查**：显示成字面量 `%xxx_yyy%` → `/papi list` 看扩展是否缺失 → `/papi ecloud download <名>`；装了不生效：放入 expansions/ 后必须 `/papi reload`。

### 版本信息

最新版本：PlaceholderAPI 2.12.3（发布 2026-07-11）/ 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/placeholderapi.6245/

---

## CoreProtect（方块记录与回滚）

### 玩家命令

普通玩家**无**可直接使用的查询/回滚命令——但玩家的行为会被自动记录（无需任何操作）。玩家能感知到的功能是：管理员可查到"谁动了我的箱子/谁挖了我的方块"，以及破坏可被回滚恢复。

### 管理员命令

主命令 `/co`（等价 /coreprotect）；常用别名：`/co i`=inspect、`/co l`=lookup、`/co rb`=rollback、`/co rs`=restore、`/co near`、`/co undo`。

**核心区别**：lookup 只查不改；rollback 真正改动世界；restore 把 rollback 反悔；回滚默认半径 10（以自己为中心）。

**检查员模式（inspect）**
```
/co inspect          # 开启（手持物品右键方块/箱子/告示牌查记录）
/co inspect          # 再敲一次关闭
/co i                # 简写
```

**查询（lookup，只查不改）**

| 命令 | 说明 |
|---|---|
| `/co lookup u:<玩家> t:<时间> r:<半径>` | 查某玩家在范围内操作 |
| `/co lookup b:<方块ID> t:<时间>` | 查特定方块变更 |
| `/co lookup a:<动作类型>` | 查特定操作类型 |
| `/co lookup i:<方块/物品> a:-block` | 查被挖掉的某方块 |
| `/co lookup a:command f:/ban` | 查以 /ban 开头的命令 |
| `/co near` | 查附近（半径 5）最近变更 |

**参数说明**
- `u:` 玩家/实体（伪用户 #creeper #tnt #fire #explosion #water #lava #player）
- `t:` 时间（s/m/h/d/w/y，可连写 t:5d2h、区间 t:1h-2h）
- `r:` 半径（r:20 / r:#global 全服 / r:#world_nether 指定世界 / r:#worldedit 选区）
- `a:` 动作（block / +block / -block / container / +container / -container / item / kill / chat / command / sign / session / username 等）
- `#preview` / `#count` / `#summary` / `#optimize` / `#silent` 哈希标签

**回滚（rollback，真正撤销）**

| 命令 | 说明 |
|---|---|
| `/co rollback u:<玩家> t:<时间> r:<半径>` | 回滚某玩家操作 |
| `/co rollback t:<时间> r:<半径>` | 回滚某时段所有人 |
| `/co rollback u:<玩家> t:<时间> a:-container` | 仅追回被取出的物品 |
| `/co rollback u:#creeper t:1h r:#global` | 全服回滚苦力怕破坏 |
| `/co rollback ... #preview` | 预览影响（不真正执行） |

**还原 / 撤销**
```
/co restore u:<玩家> t:1h          # 把回滚反悔（恢复）
/co undo                           # 撤销上一次 rollback/restore
```

**数据清理（purge）**
```
/co purge t:30d                     # 删 30 天前记录（游戏内仅可删>30天）
/co purge t:30d #optimize           # 删除并优化数据库表
```

**状态 / 重载**
```
/co status            # 版本、数据库类型、记录条数
/co reload            # 改了配置后重载
```

### 权限节点

- `coreprotect.use` — 可使用全部 CoreProtect 命令
- `coreprotect.inspect` — 检查员模式
- `coreprotect.lookup`(+子项) — 查询
- `coreprotect.near` — 附近变更
- `coreprotect.rollback` — 回滚（谨慎）
- `coreprotect.restore` — 撤销回滚（谨慎）
- `coreprotect.purge` — 清库（不可恢复，仅服主）
- `coreprotect.reload` / `coreprotect.status`

### 典型工作流

**现场核查**：`/co inspect`（或 `/co i`），手持物品右键被破坏的方块/箱子/告示牌 → 聊天框显示谁、何时、做了什么 → 再敲一次关闭。

**查破坏者**：`/co lookup u:破坏者 t:2h r:#global a:-block`；`/co lookup u:#explosion r:30 t:2h`；`/co lookup u:小偷 a:-container t:1h`。

**回滚恢复**：`/co rollback u:破坏者 t:1h r:20 #preview` → 确认后 `/co rollback u:破坏者 t:1h r:20`；错了对 `/co undo`。

**定期清理**：`/co purge t:30d #optimize`。

### 版本信息

最新版本：CoreProtect Community Edition v24.1（发布 2026-09-24）/ 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/coreprotect-community-edition.8631/
