# 8. 领地系统 — Residence

Residence 是 Minecraft 中流行的领地保护插件，允许玩家创建私人领地并细粒度设置建造、破坏、容器、PVP 等权限，支持领地买卖、租赁与子区域。本文档汇总了圈地、传送、领地频道等常用命令，config.yml 与 flags.yml 的实际取值，并针对配置不生效给出旧领地缓存、权限组覆盖、子区域继承三层排查思路。供管理员管理领地权限与排查问题时使用。

[Github Residence](https://github.com/Zrips/Residence)

**文件**: 内置于 `plugins/Residence/` 目录

## 功能说明

Residence 是 Minecraft 最流行的领地保护插件，允许玩家创建私人领地，在领地内设置各种权限（如禁止破坏、禁止 PVP、禁止移动等）。支持领地买卖、租赁、子区域管理。

## 常用命令

刷新配置文件：`/res reload [config(配置)/lang(语言)/groups(组)/flags(权限)]`

### 选区与创建

| 命令 | 说明 |
|---|---|
| `/res select <x> <y> <z>` | 选择领地顶点坐标 |
| `/res select vert` | 纵向扩展到天空/基岩 |
| `/res select size` | 查看当前选区大小 |
| `/res auto <名称> <半径>` | 自动创建领地（以自己为中心） |
| `/res create <名称>` | 创建领地（基于当前选区） |
| `/res subzone <父领地> <子领地>` | 创建子领地 |
| `/res expand <数量>` | 向玩家面对方向扩展领地 |
| `/res expand north/south/east/west/up/down <数量>` | 向指定方向扩展 |
| `/res contract <数量>` | 向玩家面对方向缩小领地 |
| `/res contract north/south/east/west/up/down <数量>` | 向指定方向缩小 |
| `/res move north/south/east/west/up/down <数量>` | 平移领地 |
| `/res mirror` | 镜像对称扩展领地 |
| `/res rename <旧名> <新名>` | 重命名领地 |
| `/res renamearea <领地名> <旧区域> <新区域>` | 重命名子区域 |
| `/res remove <名称>` | 删除自己的领地 |
| `/res info` | 查看当前所在领地信息 |
| `/res info <领地名>` | 查看指定领地信息 |
| `/res list` | 列出自己的所有领地 |
| `/res list <玩家名>` | 列出指定玩家的领地（需权限） |
| `/res listall` | 列出所有领地（需权限） |
| `/res listhidden` | 列出隐藏领地（需权限） |
| `/res listallhidden` | 列出所有隐藏领地（需权限） |
| `/res current` | 查看当前所在领地名称 |
| `/res limits` | 查看自己的领地数量/大小上限 |
| `/res area` | 查看当前选区区域信息 |

### 成员与权限设置

| 命令 | 说明 |
|---|---|
| `/res padd <玩家>` | 将玩家加入为领地成员 |
| `/res padd <玩家> true` | 加入并给予全部权限（build/destroy/use/container） |
| `/res padd <领地> <玩家>` | 指定领地添加成员 |
| `/res pdel <玩家>` | 移除领地成员 |
| `/res pdel <领地> <玩家>` | 指定领地移除成员 |
| `/res plist` | 查看领地成员列表 |
| `/res setadmin <玩家>` | 将玩家设为领地管理员（拥有 admin 权限） |
| `/res removeadmin <玩家>` | 移除领地管理员 |
| `/res admin` | 切换领地管理员模式（临时获得全部权限） |
| `/res set <领地> <flag> true/false` | 设置领地全局旗帜 |
| `/res pset <领地> <玩家> <flag> true/false` | 设置玩家特定旗帜 |
| `/res gset <领地> <组名> <flag> true/false` | 设置权限组旗帜 |
| `/res lset <领地> <世界> <flag> true/false` | 设置世界级旗帜 |
| `/res flags` | 查看可用旗帜列表 |
| `/res clearflags <领地名>` | 清除领地所有自定义旗帜 |
| `/res setdefaultflags` | 恢复领地默认旗帜 |
| `/res command add <命令>` | 添加领地允许执行的命令 |
| `/res command remove <命令>` | 移除领地允许执行的命令 |
| `/res command block add <命令>` | 添加领地禁用的命令 |
| `/res command block remove <命令>` | 移除领地禁用的命令 |

### 常用 Flags

| 旗帜名 | 说明 |
|---|---|
| `build` | 建造权限 |
| `destroy` | 破坏权限 |
| `use` | 使用（门/按钮/拉杆等）权限 |
| `container` | 容器访问权限（箱子/熔炉/酿造台等） |
| `move` | 移动权限（能否进入领地） |
| `tp` | 传送权限（能否传送到领地） |
| `pvp` | PVP 权限 |
| `damage` | 伤害（掉落/火焰/怪物伤害） |
| `mobkilling` | 击杀生物权限 |
| `animalkilling` | 击杀动物权限 |
| `shear` | 剪羊毛权限 |
| `tnt` | TNT 爆炸破坏 |
| `explode` | 爆炸破坏（通用） |
| `creeper` | 苦力怕爆炸破坏 |
| `fireball` | 火焰弹爆炸 |
| `firespread` | 火焰蔓延 |
| `ignite` | 点火权限 |
| `flow` | 液体流动（水/岩浆） |
| `piston` | 活塞推动 |
| `pistonprotection` | 活塞保护（防活塞机器） |
| `trample` | 踩坏耕地 |
| `monsters` | 怪物生成 |
| `animals` | 动物生成 |
| `nomobs` | 阻止生物进入领地 |
| `witherdestruction` | 凋灵破坏 |
| `dragongrief` | 末影龙破坏 |
| `bed` | 使用床 |
| `brew` | 使用酿造台 |
| `enchant` | 使用附魔台 |
| `anvil` | 使用铁砧 |
| `grindstone` | 使用砂轮 |
| `loom` | 使用织布机 |
| `smithing` | 使用锻造台 |
| `stonecutter` | 使用切石机 |
| `fly` | 飞行权限 |
| `keepinv` | 死亡保留物品 |
| `admin` | 领地管理权限（改旗帜/加成员） |
| `bank` | 使用领地银行 |

### 传送与定位

| 命令 | 说明 |
|---|---|
| `/res tp <领地>` | 传送到领地 |
| `/res tpset` | 设置当前站立位置为领地传送点 |
| `/res rt` | 随机传送（到野外） |
| `/res unstuck` | 卡住时脱困回安全位置 |
| `/res compass` | 指南针指向最近的领地 |
| `/res tpconfirm` | 确认领地传送（避免误触） |
| `/res setmain <领地名>` | 设置默认主领地 |

### 领地提示与聊天

| 命令 | 说明 |
|---|---|
| `/res message <领地> enter <消息>` | 设置进入领地提示 |
| `/res message <领地> leave <消息>` | 设置离开领地提示 |
| `/res message remove <领地> enter` | 移除进入提示 |
| `/res message remove <领地> leave` | 移除离开提示 |
| `/res rc <消息>` | 在领地频道发送消息 |
| `/res rc join` | 加入领地频道 |
| `/res rc leave` | 离开领地频道 |
| `/res chatcolor <颜色>` | 设置领地聊天颜色 |
| `/res chatprefix <前缀>` | 设置领地聊天前缀 |

### 领地经济与买卖

| 命令 | 说明 |
|---|---|
| `/res bank <领地名>` | 打开领地银行 |
| `/res resbank <领地名>` | 领地银行操作 |
| `/res market` | 打开领地市场（买卖/租赁） |
| `/res shop` | 领地商店 |
| `/res contract` | 领地合约 |
| `/res lease` | 领地租赁 |

### 工具与可视化

| 命令 | 说明 |
|---|---|
| `/res tool` | 切换选区工具 |
| `/res show <领地名>` | 显示领地边界 |
| `/res area` | 查看当前选区区域信息 |
| `/res signconvert` | 转换领地木牌 |
| `/res signupdate` | 更新领地木牌 |
| `/res siege` | 领地围攻（PVP 玩法） |
| `/res material` | 设置领地图标材质 |
| `/res gui` | 打开领地 GUI 编辑器 |

### 管理员命令（Admin）

以下命令需 `residence.admin` 权限（op 或 LuckPerms 给 `residence.admin.*`）。注意本服管理命令用单词 `/resadmin`（非 `/res admin` 两词）。

| 命令 | 说明 |
|---|---|
| `/resadmin setowner <领地名> <玩家名>` | 将领地归属转移给指定玩家（改主人） |
| `/resadmin remove <领地名>` | 管理员删除任意领地 |
| `/resadmin removeall <玩家名>` | 删除某玩家的全部领地 |
| `/resadmin removeworld <世界名>` | 删除某世界所有领地 |
| `/resadmin server <领地名>` | 将领地设为服务器所有 |
| `/resadmin setall <flag> <true/false>` | 批量设置所有领地的某权限（如 `setall build false`，操作前备份） |
| `/resadmin set` | 管理员版设置旗帜 |
| `/resadmin pset` | 管理员版设置玩家旗帜 |
| `/resadmin gset` | 管理员版设置组旗帜 |
| `/resadmin lset` | 管理员版设置世界旗帜 |
| `/resadmin give <领地名> <玩家名>` | 把领地送给玩家 |
| `/resadmin move <领地名> <玩家名>` | 移动领地位置 |
| `/resadmin tp <领地名>` | 传送到任意领地 |
| `/resadmin rename` | 管理员版重命名 |
| `/resadmin renamearea` | 管理员版重命名区域 |
| `/resadmin reset <领地名>` | 重置领地旗帜 |
| `/resadmin check <领地名>` | 检查领地状态 |
| `/resadmin confirm` | 确认管理员操作 |
| `/resadmin version` | 查看 Residence 版本 |
| `/resadmin info <领地名>` | 查看任意领地详细信息 |
| `/resadmin list` | 列出所有领地 |
| `/resadmin listhidden` | 列出隐藏领地 |
| `/resadmin listall` | 列出所有玩家的所有领地 |
| `/resadmin listallhidden` | 列出所有隐藏领地 |
| `/resadmin limits <玩家名>` | 查看玩家的领地数量上限 |
| `/resadmin group <玩家名> <组名>` | 设置玩家的领地权限组 |
| `/resadmin bank <领地名>` | 管理员操作领地银行 |
| `/resadmin resbank <领地名>` | 管理员操作领地租赁银行 |
| `/resadmin market` | 管理员查看领地市场 |
| `/resadmin shop` | 管理员查看领地商店 |
| `/resadmin contract` | 管理员查看领地合约 |
| `/resadmin lease` | 管理员查看领地租赁 |
| `/resadmin siege` | 管理员管理领地围攻 |
| `/resadmin signconvert` | 管理员转换领地木牌 |
| `/resadmin signupdate` | 管理员更新领地木牌 |
| `/resadmin material` | 管理员设置领地图标材质 |
| `/resadmin gui` | 管理员打开领地 GUI 编辑器 |
| `/resadmin tool` | 管理员切换选区工具 |
| `/resadmin show <领地名>` | 管理员显示任意领地边界 |
| `/resadmin area` | 管理员查看选区区域信息 |

## 关键配置 (`plugins/Residence/config.yml`)

```yaml
Global:
  Language: Chinese # 中文语言
  SelectionToolId: WOODEN_HOE # 选择工具: 木锄
  InfoToolId: STRING # 信息查看工具: 线
  DefaultWorld: world # 默认世界
  EnableEconomy: false # 经济系统已禁用
  EnablePermissions: true # 权限系统启用
  ResidenceChatEnable: true # 领地聊天启用
  ResidenceChatColor: DARK_PURPLE # 领地聊天颜色: 深紫
  TeleportDelay: 3 # 传送延迟: 3 秒
  SaveInterval: 10 # 保存间隔: 10 分钟
  TimeZone: Asia/Shanghai # 时区: 上海

  AntiGreef:
    RangeGaps:
      - all-8 # 领地间距: 8 格
    BlockFall:
      Use: true # 下落方块防护启用

  Visualizer:
    Use: true # 启用可视化边界
    Range: 16 # 可视化范围
    Selected:
      Frame: dust:125,150,150 # 选中框颜色
      Sides: dust:150,255,200 # 选中面颜色
    Overlap:
      Frame: dust:255,0,255 # 冲突框颜色
      Sides: dust:255,100,100 # 冲突面颜色

  GUI:
    Enabled: true # 启用 GUI 旗帜编辑器
    setTrue: GREEN_WOOL # 开启状态: 绿色羊毛
    setFalse: RED_WOOL # 关闭状态: 红色羊毛
    setRemove: LIGHT_GRAY_WOOL # 移除状态: 灰色羊毛

  DynMap:
    Use: true # DynMap 地图支持启用
    ShowFlags: true # 显示旗帜信息
  Pl3xMap:
    Use: true # Pl3xMap 地图支持启用
    ShowFlags: true # 显示旗帜信息
  # 在线地图：本服采用 Dynmap（Residence 原生内置 DynMap 集成，已启用）

  # 以下功能均已禁用:
  UseLeaseSystem: false # 租赁系统
  EnableRentSystem: false # 出租系统
  Sell.Subzone: false # 子区域出售
```

> **经济开关实际值**：本服领地当前 `EnableEconomy: false`、`Type: Vault`、`UseLeaseSystem: false`、`EnableRentSystem: false`——圈地不扣钱、不可买卖/出租。

## flags.yml 权限配置文件

全局默认权限，主要定义了游戏世界和领地的默认行为规则。以下是该文件的核心配置：

### 🌍 全局世界规则 (Global Flags)

这部分定义了玩家**不在任何领地内**时的世界默认行为。

- **核心保护**：野外**允许**玩家建造 (`build: true`)、使用 (`use: true`)；全局 `Global` 段未单独列出 `destroy`。
- **PVP 与伤害**：野外**开启**玩家对战 (`pvp: true`) 和生物伤害 (`damage: true`)。
- **爆炸与火灾**：野外**允许** TNT 爆炸 (`tnt: true`)、火焰蔓延 (`firespread: true`)、点燃 (`ignite: true`)，苦力怕爆炸 (`creeper: true`) 亦为允许。
- **结论**：本服野外（无人圈地区域）几乎不做规则限制，保护完全靠玩家自建领地实现。

### 🚩 权限标志管理 (FlagPermission)

定义了哪些权限标志（Flags）默认对所有玩家组开放，除非在组权限中特别拒绝。

- **玩家管理**：允许玩家管理自己的领地，如设置传送点 (`tp: true`)、修改旗帜 (`admin: true`)、使用领地银行 (`bank: true`)。
- **功能使用**：允许使用床 (`bed: true`)、酿造台 (`brew: true`)、附魔台 (`enchant: true`) 等大部分功能性方块。
- **特殊限制**：禁止玩家飞行 (`fly: false`) 和保留物品栏 (`keepinv: false`)。

### 🏠 领地默认权限 (ResidenceDefault)

当玩家创建一个新的领地时，该领地会自动应用以下默认权限：

- **基础操作**：新领地默认**禁止**建造 (`build: false`)、破坏 (`destroy: false`)、使用容器 (`container: false`)、使用 (`use: false`) 和 PVP (`pvp: false`)。
- **生物与动物**：禁止动物捕杀 (`animalkilling: false`) 和剪羊毛 (`shear: false`)。
- **特殊保护**：禁止 TNT 爆炸 (`tnt: false`)、禁止爆炸 (`explode: false`)、火焰蔓延 (`firespread: false`)，开启活塞保护 (`pistonprotection: true`)。

### 👤 创建者与租客权限 (CreatorDefault & RentedDefault)

- **领地创建者**：在自己创建的领地内拥有所有权限，包括建造、破坏、使用、PVP、传送等（全为 `true`）。
- **领地租客**：拥有与创建者几乎相同的完整权限，包括管理权限 (`admin: true`)。

### 🎨 界面与列表配置

- **GUI 图标**：定义了在游戏内领地管理界面（GUI）中，每个权限标志对应的显示物品（如 `build` 对应砖块 `BRICKS`，`tnt` 对应 TNT 方块）。
- **物品列表**：定义了一个名为 `DefaultList` 的物品黑名单，包含 `LAVA`（岩浆）、`WATER`（水）等，可用于限制特定物品的使用或放置。

## 推荐区域配置

### 主城 = 安全区 + 活动区

```bash
# 允许玩家传送到主城领地内
/res set zhucheng tp true

# 保护建筑：禁止破坏和放置 改为 允许
/res set zhucheng build true
/res set zhucheng destroy true

# 允许玩家正常活动：开关门/按钮/箱子、自由移动
/res set zhucheng use true
/res set zhucheng container true
/res set zhucheng move true

# 安全区：禁 PVP、禁伤害、禁怪物、禁爆炸破坏
/res set zhucheng pvp false
/res set zhucheng damage false
/res set zhucheng monster-zhucheng false
/res set zhucheng animal-zhucheng false
/res set zhucheng fire false
/res set zhucheng flow false

# 禁所有爆炸破坏主城建筑
/res set zhucheng explode false

# 再单独关掉各类爆炸源（更保险）
/res set zhucheng creeper false     # 苦力怕
/res set zhucheng tnt false         # TNT
/res set zhucheng fireball false    # 恶魂火球/火焰弹
/res set zhucheng witherdestruction false   # 凋灵破坏
/res set zhucheng dragongrief false         # 末影龙破坏

# 禁领地内所有伤害（怪物打你、掉落、火焰等 → 安全区无敌效果）  改为允许
/res set zhucheng damage true
# `damage false` 是**禁领地内所有实体伤害**—— 不只是怪物打玩家，**玩家打怪物也一起禁了**（一刀切）。所以玩家攻击不掉血。

# 如果还想连怪物生成也禁掉（彻底安全区）
/res set zhucheng monsters false     # 禁怪物生成
/res set zhucheng nmonsters false    # 禁自然怪物生成
/res set zhucheng nomobs true        # 阻止怪物走进领地

# 防踩坏耕地
/res set zhucheng trample false
```

### 玩家摆摊 / 开店（自由市场）怎么开放

主城禁了 build/destroy 后，摆摊玩家放不了展示框和商店。三种做法：

**方案 A：划一块摆摊区开放建造（推荐）**
在 zhucheng 里圈子区域，单独给它开 build：

```bash
# 先选好摆摊区，创建子区域
/res subzone zhucheng market
/res set spawn.market build true
/res set spawn.market destroy true
```

方案 B：只给指定玩家开放

```bash
/res pset zhucheng <玩家名> build true
/res pset zhucheng <玩家名> destroy true

```

方案 C：给 default 组全开放（主城变自由建造区，不保护建筑时用）

```bash
/res gset zhucheng default build true
/res gset zhucheng default destroy true

```

## 配置不生效时

修改默认配置后依然无效，说明问题可能不在“新领地”的生成规则上，而是**旧领地的权限残留**，或者是**更上层的权限组**在作祟。

请按照以下 3 个步骤进行深度排查，这通常能解决 99% 的“配置不生效”问题：

### 1. 排查“旧领地”缓存（最常见原因）

**现象**：你修改了配置文件，但玩家**之前已经创建过**的领地，其权限数据是保存在 `plugins/Residence/Save/residences.yml` 里的。
**原理**：配置文件只决定“新出生”的领地。对于已经存在的领地，系统会读取存档文件。如果存档里写着 `build: true`，它会无视你的新配置。
**验证方法**：

- 去一个**全新**的、没人圈过的地方，重新圈一块地。
- 测试这块**新地**是否受保护。
- **结果**：如果新地生效了，旧地没生效，说明是旧数据问题。
- **解决**：你需要使用管理员指令强制刷新旧领地：
  `/resadmin setall build false`
  `/resadmin setall destroy false`
  `/resadmin setall use false`
  _(注意：这会强制关闭服务器内所有领地的这些权限，操作前请备份)_

### 2. 排查“权限组”覆盖 (groups.yml)

**现象**：你在 `flag.yml` 里设置了 `false`，但玩家在 `groups.yml` 里被赋予了 `true`。
**原理**：Residence 的权限优先级通常是：`玩家单独设置(pset)` > `权限组设置(groups.yml)` > `全局默认设置(flag.yml)`。
**排查**：

- 打开 `plugins/Residence/groups.yml`。
- 找到 `Default`（默认组）或者玩家所在的权限组。
- 检查里面是否有类似下面的配置：
  ```yaml
  Groups:
    Default:
      Residence:
        Flags:
          build: true # <--- 检查这里！如果是 true，会覆盖 config 的设置
          destroy: true
          use: true
  ```
- **解决**：确保 `groups.yml` 里没有显式地把这些权限设为 `true`，或者直接删除这几行让它们继承全局设置。

### 3. 排查”子区域”或”父区域”继承

**现象**：大领地（父区域）设置了允许，小领地（子区域）即使设置了禁止，有时也会因为继承关系出问题（反之亦然）。
**排查**：

- 站在领地中，输入 `/res info`。
- 查看输出信息，确认当前所在的具体领地名称。
- 有时候玩家是在”子区域”里，而父区域的权限设置不同。

#### 子领地隔离：大领地授权的人，子领地怎么单独 deny

**现象**：大领地 `/res padd` 或 `/res pset` 给了某人建造/使用/容器权限，这个权限**默认延伸到所有子领地**。你在子领地里只想让自己能动、别人不能动，但子领地怎么设置都不生效——别人还是能在子领地里建造/开门/开箱。

**原理**：Residence 权限是**叠加继承**，不是自动隔离。大领地 `build: allow` → 子领地没显式设置 = 继承 allow。子领地必须**显式反向声明** `false` 才能压过大领地的 allow，只在子领地设自己的权限没用。

**解决**：在子领地里把那个人的权限显式 deny 掉：

```bash
# 假设大领地叫 home，子领地叫 home.cave，朋友叫 bob
/res pset home.cave bob build false
/res pset home.cave bob destroy false
/res pset home.cave bob use false
/res pset home.cave bob container false
/res pset home.cave bob move false      # 连进都进不来（私密房间）
```

**批量 deny 所有被大领地授权的人**（子领地默认谁都不能动，只再单独给自己 allow）：

```bash
/res pset home.cave default build false
/res pset home.cave default container false
/res pset home.cave default move false
```

> **关键点**：子领地权限**不会**自动从大领地”隔离”出来，必须反向写 `false`。只在子领地 `/res pset home.cave 自己 build true` 是不够的，大领地给别人的 allow 仍然会穿透到子领地。


### 💡 终极排查手段：开启调试模式

如果以上都查不出问题，请开启 Residence 的调试模式，看看到底是谁“放行”了玩家。

1.  在游戏内输入：`/res debug on`
2.  让玩家去尝试破坏方块或开箱子。
3.  看**服务器后台控制台**输出的日志。
4.  日志会明确告诉你：`Player [名字] tried to [动作] in [领地名]. Result: [ALLOWED/DENIED] by Flag: [权限名]`。
    - 如果显示 `ALLOWED`，它会告诉你具体是哪个 Flag 或者是哪个 Group 给了权限。

**建议操作顺序**：先试第 1 步（新圈地测试），如果新地是好的，那就是旧数据问题，用 `resadmin setall` 解决最快。

---

## Owner 把自己权限删了怎么办？

**现象**：玩家执行了 `/res pset 领地名 自己 admin false` 或 `/res pdel 领地名 自己`，结果领地还是他的（`/res list` 显示），但他自己不能建造、不能开门、不能管理权限。

**原因**：Residence 把"领地所有权"和"操作权限"分成两层：

| 层级 | 说明 | 怎么改 |
|---|---|---|
| **Owner（所有权）** | 领地的"主人"字段 | 只有 `/res remove`（删除领地）或 `/resadmin setowner`（转移所有权）才会改变 |
| **权限（Flags/Perms）** | 谁能建造、谁能开门、谁能管理领地 | `/res pset`、`/res set`、`/res padd`、`/res pdel` 都能改 |

`/res pset`、`/res padd`、`/res pdel` 这些命令对**任何人**都生效，包括 owner 自己。Residence 没有"owner 不能修改自己权限"的保护机制。

**解决方法：**

1. **管理员恢复**：`/resadmin pset 领地名 玩家名 admin true`
2. **自己恢复**（如果还能进领地）：`/res setdefaultflags`
3. **删了重建**：`/res remove 领地名`，然后重新圈地创建

**预防**：提醒玩家不要对自己用 `/res pset 领地名 你自己 权限 false`。
