# 8. 领地系统 — Residence

[Github Residence](https://github.com/Zrips/Residence)

**文件**: 内置于 `plugins/Residence/` 目录

## 功能说明

Residence 是 Minecraft 最流行的领地保护插件，允许玩家创建私人领地，在领地内设置各种权限（如禁止破坏、禁止 PVP、禁止移动等）。支持领地买卖、租赁、子区域管理。

## 关键配置 (`plugins/Residence/config.yml`)

```yaml
Global:
  Language: Chinese                       # 中文语言
  SelectionToolId: WOODEN_HOE             # 选择工具: 木锄
  InfoToolId: STRING                      # 信息查看工具: 线
  DefaultWorld: world                     # 默认世界
  EnableEconomy: false                    # 经济系统已禁用
  EnablePermissions: true                 # 权限系统启用
  ResidenceChatEnable: true               # 领地聊天启用
  ResidenceChatColor: DARK_PURPLE         # 领地聊天颜色: 深紫
  TeleportDelay: 3                        # 传送延迟: 3 秒
  SaveInterval: 10                        # 保存间隔: 10 分钟
  TimeZone: Asia/Shanghai                 # 时区: 上海

  AntiGreef:
    RangeGaps:
      - all-8                             # 领地间距: 8 格
    BlockFall:
      Use: true                           # 下落方块防护启用

  Visualizer:
    Use: true                             # 启用可视化边界
    Range: 16                             # 可视化范围
    Selected:
      Frame: dust:125,150,150            # 选中框颜色
      Sides: dust:150,255,200            # 选中面颜色
    Overlap:
      Frame: dust:255,0,255              # 冲突框颜色
      Sides: dust:255,100,100            # 冲突面颜色

  GUI:
    Enabled: true                         # 启用 GUI 旗帜编辑器
    setTrue: GREEN_WOOL                   # 开启状态: 绿色羊毛
    setFalse: RED_WOOL                    # 关闭状态: 红色羊毛
    setRemove: LIGHT_GRAY_WOOL            # 移除状态: 灰色羊毛

  WebMap:
    Use: true                             # Web 地图支持启用
    ShowFlags: true                       # 显示旗帜信息

  # 以下功能均已禁用:
  EnableEconomy: false                    # 经济系统
  UseLeaseSystem: false                   # 租赁系统
  EnableRentSystem: false                 # 出租系统
  Sell.Subzone: false                     # 子区域出售
```

## flags.yml 权限配置文件

全局默认权限，主要定义了游戏世界和领地的默认行为规则。以下是该文件的核心配置总结：

### 🌍 全局世界规则 (Global Flags)

这部分定义了玩家**不在任何领地内**时的世界默认行为。

- **核心保护**：默认**禁止**玩家建造 (`build: false`)、使用 (`use: false`) 和破坏 (`destroy: false`) 方块。
- **PVP 与伤害**：默认**开启**玩家对战 (`pvp: true`) 和生物伤害 (`damage: false` - _注：此处原文为false，意为禁止生物伤害，但pvp为true_)。
- **爆炸与火灾**：默认**允许**TNT 爆炸 (`tnt: true`) 和火焰蔓延 (`firespread: false` - _注：此处原文为false，意为禁止火焰蔓延_)，但禁止苦力怕爆炸 (`creeper: false`)。

### 🚩 权限标志管理 (FlagPermission)

定义了哪些权限标志（Flags）默认对所有玩家组开放，除非在组权限中特别拒绝。

- **玩家管理**：允许玩家管理自己的领地，如设置传送点 (`tp: true`)、修改旗帜 (`admin: true`)、使用领地银行 (`bank: true`)。
- **功能使用**：允许使用床 (`bed: true`)、酿造台 (`brew: true`)、附魔台 (`enchant: true`) 等大部分功能性方块。
- **特殊限制**：禁止玩家飞行 (`fly: false`) 和保留物品栏 (`keepinv: false`)。

### 🏠 领地默认权限 (ResidenceDefault)

当玩家创建一个新的领地时，该领地会自动应用以下默认权限：

- **基础操作**：默认**禁止**建造 (`build: false`)、破坏 (`destroy: false`)、使用容器 (`container: false`) 和 PVP (`pvp: true` - _注：此处原文为true，意为开启PVP_)。
- **生物与动物**：禁止生物生成 (`nomobs: true`)，禁止动物繁殖 (`animalkilling: false`) 和剪羊毛 (`shear: false`)。
- **特殊保护**：禁止TNT爆炸 (`tnt: true` - _注：此处原文为true，意为允许TNT_)，禁止活塞推动方块 (`pistonprotection: true`)。

### 👤 创建者与租客权限 (CreatorDefault & RentedDefault)

- **领地创建者**：在自己创建的领地内拥有所有权限，包括建造、破坏、使用、PVP、传送等（全为 `true`）。
- **领地租客**：拥有与创建者几乎相同的完整权限，包括管理权限 (`admin: true`)。

### 🎨 界面与列表配置

- **GUI 图标**：定义了在游戏内领地管理界面（GUI）中，每个权限标志对应的显示物品（如 `build` 对应砖块 `BRICKS`，`tnt` 对应 TNT 方块）。
- **物品列表**：定义了一个名为 `DefaultList` 的物品黑名单，包含 `LAVA`（岩浆）、`WATER`（水）等，可用于限制特定物品的使用或放置。

## 常用命令

### 领地创建与管理

| 命令                      | 说明                |
| ------------------------- | ------------------- |
| `/res select <x> <y> <z>` | 选择领地顶点坐标    |
| `/res select vert`        | 纵向扩展到天空/基岩 |
| `/res create <名称>`      | 创建领地            |
| `/res auto <名称> <半径>` | 自动创建领地        |
| `/res remove <名称>`      | 移除领地            |
| `/res expand <数量>`      | 扩展领地            |
| `/res info`               | 查看当前领地信息    |
| `/res list`               | 列出自己的领地      |

### 领地权限 (Flags)

| 命令                                        | 说明             |
| ------------------------------------------- | ---------------- |
| `/res set <领地> <flag> true/false`         | 设置领地旗帜     |
| `/res pset <领地> <玩家> <flag> true/false` | 设置玩家特定旗帜 |
| `/res flags`                                | 查看可用旗帜列表 |

### 常用 Flags

| 旗帜名       | 说明                  |
| ------------ | --------------------- |
| `build`      | 建造权限              |
| `destroy`    | 破坏权限              |
| `use`        | 使用（门/按钮等）权限 |
| `container`  | 容器访问权限          |
| `move`       | 移动权限              |
| `pvp`        | PVP 权限              |
| `tp`         | 传送权限              |
| `mobkilling` | 击杀生物              |
| `mobdamage`  | 生物伤害              |

### 传送

| 命令             | 说明       |
| ---------------- | ---------- |
| `/res tp <领地>` | 传送到领地 |
| `/res rt`        | 随机传送   |

### 领地聊天

| 命令             | 说明               |
| ---------------- | ------------------ |
| `/res rc <消息>` | 在领地频道发送消息 |
| `/res rc join`   | 加入领地频道       |
| `/res rc leave`  | 离开领地频道       |
