# VexBot — 智能 PvP 机器人

> 在服务器里生成会真打架的假人，用于练 PvP、填场、FFA 陪练。机器人是完整的 Java 实体，基岩版玩家通过 Geyser 也能正常看到、攻击、被它攻击。

## 基本信息

| 项目     | 内容                                                         |
| -------- | ------------------------------------------------------------ |
| Jar      | `vexbot-paper-3.0.14paper-mc1.21.11.jar`                     |
| 版本     | 3.0.14（Paper，MC 1.21.11）                                   |
| 主类     | `vexbot.paper.VexBotPaper`                                   |
| 软依赖   | LuckPerms（可选，用于权限/分组）                             |
| 数据目录 | `plugins/VexBot/`（首次启动自动生成，含 settings、kit 存档） |
| 配置方式 | 游戏内 GUI 为主，`/vexbot settings <键> <值>` 为辅           |

https://modrinth.com/plugin/vexbot?loader=paper&version=1.21.11

> 当前服务器部署版本：`vexbot-paper-3.0.14paper-mc1.21.11.jar`（2026-10 由 3.0.3 升级，适配 Leaf 1.21.11）。

## 它会什么

机器人不是站桩靶子，而是带完整战斗 AI 的假人，主要技术包括：

- **近战**：剑 PvP（走位/strafe）、斧 PvP（破盾、暴击、击退时机）
- **远程**：弓（拉弓时机预判）、弩（上弦/发射节奏）
- **防御**：举盾格挡、图腾（Totem）触发与拉扯、药水（回血/迅捷等）
- **高端招式**：搭高（Scaffold）、弹跳（BunnyHop）、蛛网陷阱与逃脱、末影水晶（Crystal PvP）、鞘翅+锤、 chorus fruit 逃生
- **机动**：船 / 矿车移动、寻路追击、追击分散（ChaseSpread）防止围殴
- **补给**：自动吃食物/回血、从潜影盒或末影箱换装补装备、盔甲优劣评估
- **队伍**：支持 FFA（各自为战）和分队对抗，队伍间目标自动平衡分配

## 难度系统

机器人分多档难度预设，从 `EASY`（慢、间隔出招、反应迟钝）到 `PERFECT`（接近人类极限）。

- FFA 开局时可为每台机器人随机发一档难度（`EASY..PERFECT`）；
- 也可通过设置/GUI 固定难度，模拟不同水平的对手。

### 设置难度（操作）

| 做法 | 命令 / 操作 |
| --- | --- |
| 固定某档难度 | `/vexbot` GUI 里给机器人固定难度（EASY..PERFECT） |
| FFA 随机发难度 | `/vexbot ffa`（按当前难度预设给每台机器人随机发一档） |
| 随机战斗风格 | `/vexbot randomplaystyle`（剑/斧/弓/搭高/水晶等打法随机） |
| 全局开关 | `/vexbot settings <键> <值>`，例：`/vexbot settings enderchestregear false`（关末影箱换装，让机器人站桩硬刚） |

> 精确菜单项与子参数以游戏内 `/vexbot` GUI 和命令补全提示为准。

## 常用命令

主命令前缀为 `/vexbot`（需 OP 或对应权限）。

| 命令                         | 作用                                                                           |
| ---------------------------- | ------------------------------------------------------------------------------ |
| `/vexbot`                    | 打开主 GUI（召唤/管理机器人）                                                  |
| `/vexbot settings <键> <值>` | 调整全局开关，例如 `/vexbot settings enderchestregear false`（关闭末影箱换装） |
| `/vexbot ffa`                | 开启/重开 FFA 模式，按当前难度预设给在场机器人发难度                           |
| `/vexbot randomplaystyle`    | 让机器人随机一种战斗风格                                                       |
| 按名删除/管理                | 机器人有名字，可在 GUI 或命令里按名字移除（提示 `No bot named 'xxx'`）         |
| 清空                         | 一键清除所有机器人（clearbots）、清空队伍（clearteams）                        |

> 具体子参数以游戏内 `/vexbot` GUI 和命令提示为准；大部分日常操作（召唤、选 kit、调装备、切难度）都在 GUI 里点，不必记指令。

### 召唤机器人（官方命令）

> 来源：VexBot 官方（Modrinth 描述 + 更新日志，Paper 版 3.0.14）。

| 方式 | 命令 | 说明 |
| --- | --- | --- |
| GUI 召唤（主推） | `/vexbot` | 打开主 GUI，点选机器人类型 / kit，在脚下生成 |
| 命令召唤 | `/vexbot spawn` | 官方 spawn 命令，支持坐标：`/vexbot spawn x y z` |
| 批量召唤 | `/vexbot mass_spawn` | 一次拉多个机器人 |
| FFA 混战 | `/vexbot ffa` | 自动生成一批陪练机器人开混战 |

> Paper 版 3.0.14 的精确子参数以游戏内 `/vexbot` 命令补全提示为准。

## Kit 系统

- 机器人可以套用预设 Kit（武器、盔甲、物品配置）；
- 支持 Kit 列表 GUI、Kit 图标菜单、保存/加载自定义 kit；
- 报错 `Failed to load/save kit` 说明某个 kit 存档损坏，去 `plugins/VexBot/` 下检查对应文件。

### 穿装备（套 Kit，操作）

1. `/vexbot` 打开主 GUI；
2. 进 **Kit 列表 / Kit 图标菜单**；
3. **套用现有 Kit**：召唤机器人时给它选一个 Kit，机器人带着那套武器+盔甲+物品生成；
4. **自定义 Kit**：GUI 里**保存 / 加载自定义 kit**——把想要的装备配置存成一个 kit，之后直接套用。

## 注意事项

1. **会破坏地形**：若 kit 里带「轨道打击炮（Orbital Strike Cannon）」类物品，机器人会发射 TNT 雨轰炸地形，造成大量破坏。只在允许炸图的场地开启。
2. **性能**：机器人数量多时代价明显（寻路 + 战斗 AI）。练刀场建议 1–3 台，填场按需；机器人太多会卡服。
3. **刷怪/农场**：机器人在场会影响刷怪机制，密集机器人可能拖慢 mob farm 或增加刷怪开销，可在设置里控制。
4. **末影箱换装**：默认开启，机器人残血会钻末影箱补给；想让它站着硬刚可 `enderchestregear false` 关掉。
5. **基岩玩家**：机器人是 Java 端实体，Geyser 透传正常，基岩玩家可直接与之对战；无需额外配置。
6. **权限**：`softdepend: LuckPerms`，装了 LuckPerms 后可按组分配谁能召唤机器人。

## 命令报错排查：`/vexbot` 提示 Unknown / 未知命令

**现象**：`/vexbot` 报 `Unknown command` / `Unknown or incomplete command. See below for error`。

**含义**：命令未注册 → 插件未实际加载（不是权限、也不是参数问题）。

按序排查：

1. **jar 是否装进运行服 plugins**：注意项目文档仓库（`3-玩法与玩家功能插件/19-pvp机器人/`）只是整理文档，**不是运行服本体**。jar 必须放进**运行服服务端**的 `plugins\` 目录（云服需先上传文件）。
2. **是否完整重启**：放入 jar 后必须 `stop` → `start` **完整重启服务端进程**；`/reload` 经常不会加载新插件。
3. **`/plugins` 看颜色**：VexBot 为**黄色** = 加载失败，去控制台启动日志看它报的错；为**绿色**但命令仍 Unknown = Leaf 命令未注册。
4. **Leaf 命令未注册兜底**：同 Quests 先例（`DeluxeMenus open_command 在 Leaf 未注册，已用 commands.yml 别名兜底`），用 `commands.yml` 给 `/vexbot` 加命令别名映射。

> 常见根因排序：jar 没进运行服 plugins（占比最高）→ 只 reload 没完整重启 → 插件加载失败（依赖/版本）→ Leaf 命令注册问题。
