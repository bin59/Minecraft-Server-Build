# 新增玩法：蛋战争 / 蛋糕战争（复用现有 BedWars 插件）

> 结论先行：**不装新插件**。30003 小游戏服已在跑 `ScreamingBedWars-0.2.44.jar`，它官方原生支持把「队伍目标方块」从床换成龙蛋 / 蛋糕 / 重生锚，直接对应 EggWars / CakeWars / AnchorWars 三种新玩法。新建一张地图 + 一组竞技场即可上线，**零新增 jar、零额外常驻内存、Java/基岩通用**。

## 0. 版本与官方文档（2026-11-19 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 现用插件 | `plugins/BedWars-0.2.44.jar`（`version: 0.2.44`），30003 小游戏服实跑，支持 Paper 1.8.8–26.3，本服 Java 25 可加载 |
| 变体依据 | 官方 Hangar 页明确写 `BedWars/EggWars/AnchorWars/CakeWars minigame`；本地汉化「竞技场创建」§设置队伍床注明：床只是目标方块之一，可用 **Dragon Egg / Cake / Respawn Anchor** 创建 EggWars / CakeWars / AnchorWars |
| 官方文档站 | <https://docs.screamingsandals.org/BedWars/latest/> · 竞技场创建：<https://docs.screamingsandals.org/BedWars/latest/arena/> |
| 官方下载 | <https://hangar.papermc.io/ScreamingSandals/ScreamingBedWars> · <https://modrinth.com/plugin/screamingbedwars> · <https://www.spigotmc.org/resources/screaming-bedwars.63714/> |
| 源码 / 许可 | <https://github.com/ScreamingSandals/BedWars> · LGPL-3.0 |
| 本地汉化 | 同目录 `官方文档汉化/`（12 篇，含「竞技场创建 / 配置 / 商店 / 升级 / 命令与权限」）；本文命令以汉化版为准 |
| 跨端 | Geyser/Floodgate 基岩玩家已在现有 BedWars 竞技场实跑；变体只换目标方块，交互链路完全一致 |

> 本方案只复用已运行的插件与官方支持的功能，不引入第三方来源不明的新插件，因此不产生新的兼容性赌注。

## 1. 选型对比（为什么是它，而不是装新插件）

| 候选玩法 | 载体 | 26.3 / Java 25 | 基岩可玩 | 额外内存 | 落地成本 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| **蛋战争 / 蛋糕战争** | 现有 BedWars 0.2.44 换目标方块 | ✅ 官方支持到 26.3 | ✅ 已在 BedWars 实跑验证 | ≈ 0（复用同插件、同世界管理机制） | 建 1 张图 + 一组 `/bw admin` 命令 | **✅ 采用** |
| 空岛战争 SkyWars | 新装独立插件 | ⚠️ ScreamingSandals 官方仅有「计划中」，第三方多止步 1.21.11/26.2 | ❌ 未验证（商店/选队 GUI 多为点击式，基岩易踩坑） | 中（多套世界/刷怪/商店配置） | 高 | ❌ 暂不采用 |
| 坠落挑战 Dropper | 新装独立插件 | ⚠️ 主流版本止步 1.21.4 / 1.21.11 / 26.1.2；部分需 ProtocolLib + WorldGuard/Edit | ❌ 地图投票/计分板 GUI 未验证 | 中 | 高（装插件 + 建竖直地图 + 调权限） | ❌ 暂不采用 |
| 起床战争（第二张图） | 现有 BedWars 再开一图 | ✅ | ✅ | ≈ 0 | 低 | 🔸 备选（同质化，不新增玩法类型） |

**取舍说明**：本服玩家以基岩为主、minigames 堆仅 3G/1G。新插件的兼容性风险和基岩 GUI 交互风险都远高于「换目标方块」。蛋战争对玩家是**肉眼可辨的新玩法**（保护对象从「床」变成会弹跳、不能自然再生的龙蛋，节奏和资源争夺点不同），同时把风险压到最低。

## 2. 玩法规则（三种变体差异）

| 变体 | 目标方块 | 破坏方式 | 玩法气质 |
| --- | --- | --- | --- |
| **EggWars 蛋战争** | 末地龙蛋 Dragon Egg | 只能被特定工具/攻击破坏（受配置 `target-block` 控制） | 龙蛋无法用活塞推动、会瞬移，防守更吃走位；主打快节奏 |
| **CakeWars 蛋糕战争** | 蛋糕 Cake | 默认可被「吃掉」破坏（`target-block.cake.destroy-by-eating: true`） | 反直觉、欢乐向，适合休闲房 |
| **AnchorWars 重生锚战争** | 重生锚 Respawn Anchor | 需充能，开局状态受 `target-block.respawn-anchor.fill-on-start` 控制 | 偏战术，节奏较慢 |

> 具体保护规则由全局 `plugins/BedWars/config.yml` 的 `target-block` 段统一控制（见 `官方文档汉化/配置/`），三种变体共享这套开关——**改一处，所有竞技场生效**。若要不同竞技场不同规则，需用 Per-Arena 覆盖（官方 API 文档有述，本文不展开）。

## 3. 落地步骤（服主进服执行；AI 不启服、不 reload）

前置：用 Multiverse 在 30003 新建并加载一张地图世界（minigames 服已有 Multiverse-Core），把自己传送进去。世界名建议 `map-eggwars`，与现有 `map-jkpof`（幸运之柱）并列。

```bash
# 0) 建图并加载（Multiverse，示例）
/mv create map-eggwars normal
/mv tp map-eggwars

# 1) 创建竞技场
/bw admin eggwars1 add

# 2) 框定竞技场范围（走到地图两角各执行一次）
/bw admin eggwars1 pos1
/bw admin eggwars1 pos2

# 3) 添加至少 2 支队伍（颜色见「竞技场创建」汉化文档配色表）
/bw admin eggwars1 team add Red RED 2
/bw admin eggwars1 team add Blue BLUE 2

# 4) 逐队设置出生点（站在该队出生位、朝出生朝向执行）
/bw admin eggwars1 team spawn Red
/bw admin eggwars1 team spawn Blue

# 5) 关键差异步：看向该队的【龙蛋】执行（不再是床）
/bw admin eggwars1 team bed Red
/bw admin eggwars1 team bed Blue
# 蛋糕战争：把龙蛋摆成蛋糕，命令相同；AnchorWars 同理摆重生锚

# 6) 资源生成器（站在刷矿点执行；bronze/iron/gold 为默认资源）
/bw admin eggwars1 spawner add bronze true
/bw admin eggwars1 spawner add iron true
/bw admin eggwars1 spawner add gold true

# 7) 商店 villagers（站在商店位、朝店主朝向执行）
/bw admin eggwars1 store add &a商店

# 8) 大厅与旁观者点
/bw admin eggwars1 lobby
/bw admin eggwars1 spec

# 9) 保存并启用
/bw admin eggwars1 save
```

**加入告示牌**：在大厅放告示牌，第 1 行写 `[BedWars]`（或 `[BWGame]`），第 2 行写 `eggwars1`，插件会自动更新为可点击进入的牌子。离开牌第 2 行写 `leave`。

> 命令与权限节点完整清单见 `官方文档汉化/命令与权限/`。改竞技场用 `/bw admin eggwars1 edit` → 调整 → 再次 `save`。

## 4. 与现有玩法 / 菜单共存

| 项 | 现状 | 新增后 |
| --- | --- | --- |
| 世界 | `mg_hub`（大厅）、`map-jkpof`（幸运之柱） | + `map-eggwars`（蛋战争）；均经 Multiverse 加载 |
| 小游戏入口 | QuickMenu（右键时钟）「幸运之柱」按钮 | 加一个「蛋战争」按钮，动作走服内世界切换：`[player] mvtp map-eggwars`（进入后点加入牌） |
| 跨服入口 | survival 菜单 `to-minigames` 跨服到 30003 | 不变；进服后右键时钟开本服菜单选玩法 |
| 端口 / YOP | 30003 / 32219（见 [多服网络与端口架构.md](../../1-服务端核心与网络层/多服Velocity +Multiverse/多服网络与端口架构.md)） | 不变（同一 JVM 内的新世界，不开新端口） |
| 命令冲突 | 无独立命令前缀冲突 | 无（复用 `/bw`，竞技场名区分） |

> QuickMenu 配置改法与命令格式见 [游戏内传送、大厅、菜单等配置.md](../../1-服务端核心与网络层/多服Velocity +Multiverse/游戏内传送、大厅、菜单等配置.md) §5（minigames 精简版菜单）；改完 `/qm reload`。

## 5. 基岩兼容性（本服重点）

- ✅ **能玩**：变体只改目标方块， joining、选队、商店、计分板、复活链路与现有 BedWars 完全一致，基岩玩家已在实跑中验证过这条链路。
- ⚠️ **商店交互**：BedWars 商店是村民/实体点击式 GUI。Geyser 对「背包 GUI 内左右键」区分不了（本服已知限制），但对**村民/实体右键**交互正常，玩家进店后按表单选物品即可。若个别基岩客户端反馈选队/买东西点不动，按 BedWars 官方「命令与权限」页给一条兜底命令（如直接 `/bw join <arena>`、`/bw leave`）绑定到 QuickMenu 按钮。
- ⚠️ **龙蛋瞬移**：龙蛋受攻击会瞬移（原版特性），基岩端表现与 Java 一致；若玩家找不到蛋，引导看计分板/全息（`holo-above-bed`）。

## 6. 奖励与经济联动（防通胀）

竞技场奖励走插件的 Vault 集成（`config.yml` 的 rewards / reward-commands）。**注意**：本服刚对南瓜币做过去通胀治理（卖鱼每日封顶 300、鱼价 ×0.3）。新增玩法若发放南瓜币，建议：

- 胜场奖励走**非货币**（经验、钻石、称号、抽奖券），或
- 货币奖励纳入每日封顶口径，避免形成新的无上限 faucet。

详见 [南瓜币获取方式.md](../经济系统/南瓜币获取方式.md) 与 [服务器扣费点清单.md](../经济系统/服务器扣费点清单.md)。

## 7. 边界

- 本方案为**落地文档 + 命令清单**：AI 只改本地文档，未建世界、未执行 `/bw admin`、未启服、未 reload、未改运行中的 `BedWars/` 配置与数据库、未碰 30003 实跑服、未碰云端。
- 实际建图、摆方块、执行命令、改 QuickMenu 由服主进服完成；坐标 / 地图细节由服主后续设计（沿用幸运之柱的做法）。
- 需回滚时：删除该竞技场目录或用 `/bw admin eggwars1 delete`，不涉及已运行的起床战争竞技场。
