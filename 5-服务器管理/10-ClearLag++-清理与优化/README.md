# ClearLag++ — 实体清理与服务器优化

ClearLag++ 是一款综合性能优化插件，自动清理地面掉落物、限制每区块实体数量、优化红石与物理计算，并提供实时 TPS/MSPT 监控、热点区块诊断、性能历史记录和网页仪表盘。

**官网/下载**：https://modrinth.com/plugin/clearlag++
**版本**：1.14.1
**支持版本**：MC 1.18 – 1.21.11
**平台**：Bukkit / Spigot / Paper / Purpur / Folia

> 本文档标注了本服每个功能的实际开启状态。✅ = 已开启，❌ = 未开启。

---

## 一、自动清理系统

| 功能                 | 状态    | 说明                                         |
| -------------------- | ------- | -------------------------------------------- |
| **自动清理主开关**   | ✅ 开启 | 每 180 秒（3 分钟）清理一次                  |
| 清理掉落物           | ✅ 开启 | 地面物品自动清除                             |
| 清理实体             | ✅ 开启 | 清掉落物+实体（玩家/物品展示框等除外），靠下方黑名单保护重要实体 |
| 黑名单模式           | ✅ 开启 | 列表为黑名单（列表内的不清）                 |
| 排除世界             | ✅ 配置 | `world_from_fernsehheft` 不执行自动清理      |
| 排除实体（黑名单）   | ✅ 配置 | 猪/羊/牛/鸡/铜傀儡、**全部 7 种矿车**（含 TNT/熔炉/刷怪/命令方块矿车，后 4 种为本次新增保护，原会被清）、**全部 20 种船/筏**（逐木头列出，因 1.21 起 `BOAT`/`CHEST_BOAT` 已废弃必须逐条写）、僵尸村民、村民，以及全部友好/中性生物（约 30 种）不清 |
| 排除掉落物（黑名单） | ✅ 配置 | 钻石剑、铁镐、绿宝石不清                     |
| 玩家附近掉落物保护   | ✅ 开启 | 半径 8 格内、存活 30 秒内的掉落物不被清      |
| 空服务器跳过         | ✅ 开启 | 可清理实体 < 200 时跳过整个周期，不刷倒计时  |
| 跳过倒计时广播       | ✅ 开启 | 无内容可清时不广播"即将清理"                 |
| 跳过清理本身         | ✅ 开启 | 无内容可清时不执行清理和结果广播             |
| 命名生物保护         | ✅ 开启 | 铁砧改名的生物不清                           |
| 繁殖生物保护         | ✅ 开启 | 可繁殖生物不清                               |
| 标签生物保护         | ✅ 开启 | 带 `clearlag_exclude` 标签的生物不清         |
| **死亡掉落物保护**   | ✅ 开启 | 玩家死亡掉落物保护 600 秒（10 分钟）         |
| **昼夜差异化清理**   | ❌ 关闭 | 白天/夜晚使用相同清理规则                    |
| **各世界单独配置**   | ❌ 关闭 | 全服统一配置                                 |

### 排除实体黑名单（excluded-entities）重点说明

当前黑名单除默认家畜（猪/羊/牛/鸡/铜傀儡）外，还额外保护了以下对服务器玩法重要的实体，避免被自动清理误删：

| 实体类型           | 枚举名              | 保护原因                                                                                         | 状态 |
| ------------------ | ------------------- | ------------------------------------------------------------------------------------------------ | ---- |
| 普通（可乘坐）矿车 | `MINECART`          | 铁路/物流系统，丢失影响运输                                                                       | 原有 |
| 漏斗矿车           | `HOPPER_MINECART`   | 自动分类/运输线，丢失破坏农场（`HOPPER_MINECART`/`CHEST_MINECART` 即标准 Bukkit `EntityType` 名，非 `MINECART_HOPPER`/`MINECART_CHEST`） | 原有 |
| 箱子矿车           | `CHEST_MINECART`    | 同上                                                                                             | 原有 |
| TNT 矿车           | `TNT_MINECART`      | TNT 炮 / 陷阱设施                                                                                 | ✨ 新增 |
| 熔炉矿车           | `FURNACE_MINECART`  | 熔炼运输线                                                                                       | ✨ 新增 |
| 刷怪矿车           | `SPAWNER_MINECART`  | 刷怪设施                                                                                         | ✨ 新增 |
| 命令方块矿车       | `COMMAND_BLOCK_MINECART` | 命令方块电路                                                                                 | ✨ 新增 |
| 僵尸村民           | `ZOMBIE_VILLAGER`   | 治疗需约 4–5 分钟，中途被清会浪费金苹果+虚弱药；**无论是否在治疗，类型名都是 `ZOMBIE_VILLAGER`** | 原有 |
| 村民               | `VILLAGER`          | 僵尸村民治疗完成的瞬间会变成村民，若不保护，刚治好的村民会在下一个清理周期（180s）被清掉，前功尽弃 | 原有 |
| 下落的方块         | `FALLING_BLOCK`     | 沙子/沙砾/混凝土粉末/铁砧/龙蛋等下落过程不清除，避免破坏沙墙、TNT 炮推沙、自动 farms | ✨ 新增 |
| 点燃的 TNT         | `TNT`               | 引信中（即将爆炸）的 TNT 不清除，避免 TNT 炮/陷阱/红石机关正在引爆时被误清，导致哑炮或爆炸时序错乱 | ✨ 新增 |

> **注意 1**：`ZOMBIE_VILLAGER` 不是「正在治疗的僵尸村民」专属名字，而是**僵尸村民这一物种的统称**，治疗只是它身上的一段临时状态（转化进度），类型名不变。因此加入该类型即同时保护「治疗中」与「未治疗」的僵尸村民。
> **注意 2**：普通僵尸（绿皮、非村民）枚举名为 `ZOMBIE`，**不在**保护列表内，仍会被正常清理。
> **注意 3**：命名牌/计分板标签 `clearlag_exclude`（`tagged`）/ 繁殖（`bred`）等排除规则依然生效，可作为单实体的额外保险。

> **注意 4（友好生物全保护）**：黑名单已补全**全部友好/中性生物**，悦灵(`ALLAY`)、美西螈、蝙蝠、蜜蜂、骆驼、猫、海豚、驴、狐狸、青蛙、山羊、马、铁傀儡、羊驼、蘑菇牛、骡、豹猫、熊猫、鹦鹉、兔子、雪傀儡、炽足兽、海龟、流浪商人、狼、犰狳、嗅探兽、北极熊均不会被自动清理；敌对生物（僵尸/骷髅/苦力怕/蜘蛛/末影人/女巫/史莱姆/猪灵/劫掠兽等）仍正常清理。新增友好生物时按此规律补充枚举名即可（用 Bukkit `EntityType` 大写名，矿车类用标准 `HOPPER_MINECART`/`CHEST_MINECART`）。

> **注意 5（物理/瞬态实体）**：新增两类非生物、但玩家玩法依赖的瞬态实体——`FALLING_BLOCK`（下落的方块，涵盖沙子、沙砾、混凝土粉末、铁砧、龙蛋等所有下落态方块，Bukkit 中统一为一种实体类型，无需逐种写）与 `TNT`（引信中、即将爆炸的 TNT 实体，类名为 `TNTPrimed`，枚举常量即 `TNT`；注意它和矿车 `TNT_MINECART` 是两种不同实体）。两者排除后，沙墙/自动沙 farms、TNT 炮、红石陷阱等依赖"下落/引爆时序"的装置不会被误清导致结构崩坏或哑炮。这两类本就是短暂存在的实体，正常离场（落地成方块 / 爆炸消失）由游戏自身处理，无需 ClearLag 介入。

### ⚠️ 船/筏：1.21 起必须逐木头写（`BOAT`/`CHEST_BOAT` 已废弃）

> **踩坑结论**：1.21 起船**不再有统一的 `BOAT` / `CHEST_BOAT` 实体类型**。ClearLag 用 Bukkit `EntityType` 枚举名做匹配，只写 `BOAT` 不会报错，但**该类型根本不存在 → 匹配不到任何实体 → 船照样被清（静默失效）**。本服已改为逐木头列出全部 20 种。

**本服排除列表（20 种，1.21.11 实际枚举常量）**：

| 木头 | 普通船 | 箱子船 | 注释（与 config 一致） |
| --- | --- | --- | --- |
| OAK 橡木 | `OAK_BOAT` | `OAK_CHEST_BOAT` | 橡木船 / 橡木箱船 |
| SPRUCE 云杉 | `SPRUCE_BOAT` | `SPRUCE_CHEST_BOAT` | 云杉船 / 云杉箱船 |
| BIRCH 白桦 | `BIRCH_BOAT` | `BIRCH_CHEST_BOAT` | 白桦船 / 白桦箱船 |
| JUNGLE 丛林木 | `JUNGLE_BOAT` | `JUNGLE_CHEST_BOAT` | 丛林木船 / 丛林木箱船 |
| ACACIA 金合欢 | `ACACIA_BOAT` | `ACACIA_CHEST_BOAT` | 金合欢船 / 金合欢箱船 |
| DARK_OAK 深色橡木 | `DARK_OAK_BOAT` | `DARK_OAK_CHEST_BOAT` | 深色橡木船 / 深色橡木箱船 |
| MANGROVE 红树 | `MANGROVE_BOAT` | `MANGROVE_CHEST_BOAT` | 红树木船 / 红树木箱船 |
| CHERRY 樱花 | `CHERRY_BOAT` | `CHERRY_CHEST_BOAT` | 樱花木船 / 樱花木箱船 |
| PALE_OAK 苍白橡木 | `PALE_OAK_BOAT` | `PALE_OAK_CHEST_BOAT` | 苍白橡木船 / 苍白橡木箱船 |
| BAMBOO 竹（筏） | `BAMBOO_RAFT` | `BAMBOO_CHEST_RAFT` | 竹筏 / 竹箱筏 |

> ⚠️ 切勿写入 `POPLAR_BOAT` / `POPLAR_CHEST_BOAT`——杨木（Poplar）是 **Java 26.3**（Wilderness Bound 更新，2026 年左右）才加入的**全新木头类型，属于 1.21.x 之后的另一条更高主版本线**，不在本服的 1.21.11 范围内。已实测 **Paper 1.21.11 的 `EntityType` 枚举里没有 `POPLAR_*` 任何常量**（只有上表 20 种），写了同样静默失效。等本服升级到 26.3+ 时再补这两行即可。
> - 出处：Minecraft Wiki「Poplar Boat」历史段标注 *Java Edition 26.3* 加入 → <https://minecraft.wiki/w/Poplar_Boat>；实测枚举见 <https://jd.redguy.ru/paper/1.21.11/org/bukkit/entity/EntityType.html>（船类常量仅 `OAK…PALE_OAK`×2 + `BAMBOO_RAFT`×2，无 POPLAR）。
> - 佐证：Minecraft 官方 ItemSprite 库已含 `poplar-boat.png`（与 `oak-boat`/`spruce-boat`/…/`bamboo-raft` 并列），证明杨木船是当前（26.3+）正式物品；但本服 API 为 1.21.11，故仍不可写。

**官方船/筏物品精灵图全集（ItemSprite 仓库，跟踪最新版 26.3+）**：

下表为 Minecraft Wiki 官方 `ItemSprite` 仓库中的船/筏物品清单（即游戏内真实存在的船物品 ID）。ClearLag `excluded-entities` 用的是 **Bukkit `EntityType` 枚举名**（全大写、带 `_BOAT`/`_CHEST_BOAT`/`_RAFT` 后缀），与下表 sprite 小写连字符命名一一对应。

| 官方 sprite（物品 ID） | 中文 | 1.21.11 实体枚举是否存在 | 对应 EntityType 常量 | 备注 |
| --- | --- | --- | --- | --- |
| `oak-boat` | 橡木船 | ✅ 是 | `OAK_BOAT` / `OAK_CHEST_BOAT` | 已在本服排除列表 |
| `spruce-boat` | 云杉木船 | ✅ 是 | `SPRUCE_BOAT` / `SPRUCE_CHEST_BOAT` | 已在本服排除列表 |
| `birch-boat` | 白桦木船 | ✅ 是 | `BIRCH_BOAT` / `BIRCH_CHEST_BOAT` | 已在本服排除列表 |
| `jungle-boat` | 丛林木船 | ✅ 是 | `JUNGLE_BOAT` / `JUNGLE_CHEST_BOAT` | 已在本服排除列表 |
| `acacia-boat` | 金合欢木船 | ✅ 是 | `ACACIA_BOAT` / `ACACIA_CHEST_BOAT` | 已在本服排除列表 |
| `dark-oak-boat` | 深色橡木船 | ✅ 是 | `DARK_OAK_BOAT` / `DARK_OAK_CHEST_BOAT` | 已在本服排除列表 |
| `mangrove-boat` | 红树木船 | ✅ 是 | `MANGROVE_BOAT` / `MANGROVE_CHEST_BOAT` | 已在本服排除列表 |
| `cherry-boat` | 樱花木船 | ✅ 是 | `CHERRY_BOAT` / `CHERRY_CHEST_BOAT` | 已在本服排除列表 |
| `pale-oak-boat` | 苍白橡木船 | ✅ 是 | `PALE_OAK_BOAT` / `PALE_OAK_CHEST_BOAT` | 已在本服排除列表 |
| `bamboo-raft` | 竹筏 | ✅ 是 | `BAMBOO_RAFT` / `BAMBOO_CHEST_RAFT` | 已在本服排除列表 |
| `poplar-boat` | 杨木船 | ❌ 否（26.3+ 才有） | 无（1.21.11 枚举缺失） | **已写入配置（标 26.3+）**：当前 1.21.11 静默无效，升级到 26.3+ 后自动生效，无需再改 |

> 说明：上表为**普通船/筏**（`*_boat` / `*_raft`）。各木头（除 bamboo 为筏）另有**箱子船**变体，命名规律为 `*-chest-boat` 物品 ↔ `EntityType` 的 `*_CHEST_BOAT` 常量；`poplar-chest-boat` 同样属 26.3+。本服 1.21.11 的 9 种木箱船（`OAK_CHEST_BOAT`…`PALE_OAK_CHEST_BOAT`）+ 竹箱筏（`BAMBOO_CHEST_RAFT`）均已列入排除列表，共 20 种（见上表「本服排除列表」）。

**出处与说明（为什么必须逐条写）**：

1. **Spigot 官方 `Boat.Type` 枚举已标记 `@Deprecated`**，javadoc 原文：
   > *"different boats types are now different entity types"*
   > （不同木头类型的船现在是不同实体类型）
   - 出处：<https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/Boat.Type.html>
2. **Paper 1.21 `EntityType` 枚举中已无 `BOAT` / `CHEST_BOAT` 常量**，只有逐木头的 `OAK_BOAT`、…、`BAMBOO_RAFT` 等独立常量。
   - 出处：<https://jd.papermc.io/paper/1.21/org/bukkit/entity/EntityType.html>
3. 同理，**矿车也是逐类型拆分**（见上表 7 种），不是单一 `MINECART`。

### ✨ 本次变更摘要（矿车）

| 变更项 | 原状态 | 现状态 | 影响 |
| --- | --- | --- | --- |
| `TNT_MINECART` | 在排除列表外，**会被清** | 已加入排除列表 | TNT 矿车不再被自动清理 |
| `FURNACE_MINECART` | 在排除列表外，**会被清** | 已加入排除列表 | 熔炉矿车不再被清 |
| `SPAWNER_MINECART` | 在排除列表外，**会被清** | 已加入排除列表 | 刷怪矿车不再被清 |
| `COMMAND_BLOCK_MINECART` | 在排除列表外，**会被清** | 已加入排除列表 | 命令方块矿车不再被清（⚠️ 留着有安全隐患，若不需请删此行恢复默认清理） |
| `BOAT` / `CHEST_BOAT` | 写在排除列表（但已废弃、静默失效） | 已删除 | 由上方 20 种逐木头船/筏类型替代，保护真正生效 |

> 💡 **行为确认**：旧配置里 `BOAT`/`CHEST_BOAT` 尽管写着，其实从未保护过任何船（类型不存在）；旧配置也从未保护 TNT/熔炉/刷怪/命令方块矿车。本次改动让船和这 4 种矿车的"不被清理"**真正生效**。若只想保留 TNT 矿车、其余矿车维持默认清理，删掉 `FURNACE_MINECART`/`SPAWNER_MINECART`/`COMMAND_BLOCK_MINECART` 三行即可。

### 手动清理命令

| 命令                                             | 说明                                       |
| ------------------------------------------------ | ------------------------------------------ |
| `/clearlag clear <items/entity/all> [--preview]` | 手动清理；`--预览` 仅报告不移除            |
| `/clearlag killmobs <evil/good/all>`             | 手动击杀生物（命名/繁殖/标签生物同样保护） |
| `/clearlag nuke [半径]`                          | radical 清理半径内所有非玩家实体           |

---

## 二、性能优化项

### 红石与物理

| 功能               | 状态    | 说明                          |
| ------------------ | ------- | ----------------------------- |
| 红石更新节流       | ❌ 关闭 | 已关闭，红石机器不受限制      |
| 快速红石时钟检测   | ❌ 关闭 | 已关闭，不会封禁高频红石区块  |
| TNT 爆炸不掉落方块 | ❌ 关闭 | TNT 爆炸仍正常掉落圆石/泥土等 |
| 方块物理滥用防护   | ❌ 关闭 | 大规模沙子/沙砾下落不处理     |
| 快速树叶衰减       | ✅ 开启 | 破坏原木时立即移除树叶        |

### 漏斗与农场

| 功能             | 状态    | 说明                            |
| ---------------- | ------- | ------------------------------- |
| 漏斗传输节流     | ❌ 关闭 | 漏斗正常速度                    |
| 农场作物高度限制 | ❌ 关闭 | 甘蔗/竹子/仙人掌/海带不限制高度 |

### 掉落物与经验球

| 功能       | 状态    | 说明                                      |
| ---------- | ------- | ----------------------------------------- |
| 掉落物合并 | ✅ 开启 | 半径 30 格内掉落物自动合并，延迟 15 秒    |
| 智能堆叠   | ✅ 开启 | 物品可超过 64 堆叠上限，相同药水也可合并  |
| 最大堆叠数 | ✅ 开启 | 每堆最多 1024 个                          |
| 经验球合并 | ✅ 开启 | 半径 4 格内经验球合并，每 15 秒全服扫一次 |

#### 掉落物合并配置

```yaml
item-merge-optimization:
  enabled: true
  check-delay-ticks: 300 # 15 秒后开始合并
  merge-radius: 30.0
  smart-stacking: true
  max-stack-limit:
    enabled: true
    max-size: 1024
  name-visible: true
```

> **注意**：掉落物合并没有单独的物品排除列表。`excluded-items` 是自动清理的黑名单，不是合并的。

### 生物优化

| 功能                | 状态    | 说明                     |
| ------------------- | ------- | ------------------------ |
| 生物生成限制        | ❌ 关闭 | 不限制自然刷怪速度       |
| 按类型全局生物上限  | ❌ 关闭 | 不按生物类型限制总数     |
| 生物堆叠            | ❌ 关闭 | 不自动堆叠相似生物       |
| 距离消怪（AI 优化） | ❌ 关闭 | 不清离玩家远的生物       |
| 村民 AI 节流        | ❌ 关闭 | 村民 AI 正常运行         |
| 弹射物限制          | ❌ 关闭 | 箭/雪球/三叉戟不自动清理 |

#### 生物堆叠白名单（配置参考，功能当前关闭）

```yaml
excluded-mobs:
  - VILLAGER # 村民
  - WOLF # 狼
  - IRON_GOLEM # 铁傀儡
  - WANDERING_TRADER # 流浪商人
```

| 排除规则 | 状态 | 说明                                   |
| -------- | ---- | -------------------------------------- |
| named    | ✅   | 有自定义名称的生物不堆叠               |
| bred     | ✅   | 可繁殖生物不堆叠                       |
| tagged   | ✅   | 带 `clearlag_exclude` 标签的生物不堆叠 |
| leashed  | ✅   | 被拴绳牵引的生物不堆叠                 |
| tamed    | ✅   | 已驯服生物不堆叠                       |

---

## 三、实体与方块限制

| 功能                             | 状态    | 说明                     |
| -------------------------------- | ------- | ------------------------ |
| 盔甲架数量限制                   | ❌ 关闭 | 不限制                   |
| 物品展示框限制                   | ❌ 关闭 | 不限制                   |
| 矿车数量限制                     | ❌ 关闭 | 不限制                   |
| 船数量限制                       | ❌ 关闭 | 不限制                   |
| 方块实体限制（箱子/熔炉/漏斗等） | ❌ 关闭 | 不限制                   |
| 刷怪笼限制                       | ❌ 关闭 | 不限制                   |
| 活塞机器限制                     | ❌ 关闭 | 不限制                   |
| 繁殖数量限制                     | ❌ 关闭 | 不限制                   |
| 液体流动限制                     | ❌ 关闭 | 水/岩浆流动不限制        |
| 火焰/岩浆蔓延限制                | ❌ 关闭 | 不限制                   |
| 传送门冷却                       | ❌ 关闭 | 不限制传送频率           |
| 投掷器刷屏限制                   | ❌ 关闭 | 不限制                   |
| 数据包速率限制                   | ❌ 关闭 | 需要 ProtocolLib，未启用 |

---

## 四、监控与诊断

| 功能                       | 状态    | 说明                                                                  |
| -------------------------- | ------- | --------------------------------------------------------------------- |
| **后台性能监控**           | ✅ 开启 | 持续监控 TPS/MSPT/CPU/内存                                            |
| **BossBar 性能条**         | ✅ 开启 | 管理员可见，每 2 秒刷新，颜色随 TPS 自动变化（绿≥18 / 黄≥15 / 红<15） |
| **TPS 过低警告**           | ✅ 开启 | TPS 低于 18 时每 60 秒通知管理员                                      |
| **卡顿快照**               | ✅ 开启 | TPS 低于 15 时自动保存诊断报告，冷却 5 分钟                           |
| **动态视距**               | ✅ 开启 | TPS < 15 时视距降到 3，< 10 降到 2，< 5 降到 1                        |
| **性能历史记录（SQLite）** | ✅ 开启 | 每 30 秒采样一次，保留 14 天，用 `/clearlag history` 查看             |
| **Spark 性能分析器**       | ✅ 开启 | 用 `/clearlag profile` 生成报告链接（需安装 Spark 插件）              |
| **自动卸载热点区块**       | ❌ 关闭 | 不自动卸载卡顿区块                                                    |
| **玩家活动模拟（AFK）**    | ❌ 关闭 | 不检测 AFK 玩家                                                       |

### 诊断命令

| 命令                                   | 说明                           |
| -------------------------------------- | ------------------------------ |
| `/clearlag specs`                      | 显示 TPS/CPU/内存/运行时间概况 |
| `/clearlag cpu`                        | 显示近期 CPU 占用              |
| `/clearlag ram`                        | 显示近期内存占用               |
| `/clearlag history <30m/24h/7d>`       | 查看性能历史图表               |
| `/clearlag chunkfinder <数量>`         | 查找最卡的区块                 |
| `/clearlag unloadchunk <世界> <x> <z>` | 手动卸载区块                   |
| `/clearlag snapshot`                   | 手动触发性能快照               |
| `/clearlag validate`                   | 验证配置结构                   |

---

## 五、仪表盘与通知

| 功能                 | 状态    | 说明                                                                   |
| -------------------- | ------- | ---------------------------------------------------------------------- |
| **GUI 仪表盘**       | ✅ 可用 | `/clearlag gui` 打开，含实时统计、一键清理、各世界、最卡区块、模块开关 |
| **网页仪表盘**       | ✅ 开启 | 绑定 `0.0.0.0:8765`，只读，令牌保护，每 5 秒刷新                       |
| Discord Webhook 通知 | ❌ 关闭 | 不发送 Discord 报告                                                    |
| Discord 定期性能报告 | ❌ 关闭 | 不发送                                                                 |
| **自动更新检查**     | ❌ 关闭 | 每 24 小时检查一次插件更新                                             |

---

## 六、消息与音效

| 功能           | 状态    | 说明                                                 |
| -------------- | ------- | ---------------------------------------------------- |
| 清理倒计时广播 | ✅ 开启 | 全服广播，倒计时节点：600/300/180/60/10/5/4/3/2/1 秒 |
| 消息显示位置   | 聊天栏  | CHAT 模式                                            |
| 倒计时音效     | ✅ 开启 | 音符盒音效                                           |
| 清理完成音效   | ✅ 开启 | 升级音效                                             |
| 清理粒子特效   | ✅ 开启 | 烟雾粒子                                             |

---

## 七、插件集成与钩子

| 集成插件                           | 状态      | 说明                       |
| ---------------------------------- | --------- | -------------------------- |
| Nexo / Oraxen                      | ✅ 免疫   | 自定义内容不被清理         |
| ItemsAdder                         | ✅ 免疫   | 自定义内容不被清理         |
| MythicMobs                         | ✅ 免疫   | Mythic 怪物不被清理和击杀  |
| EliteMobs                          | ✅ 免疫   | 不被清理、击杀和 AI 优化   |
| Citizens                           | ✅ 免疫   | NPC 不受影响               |
| Shopkeepers                        | ✅ 免疫   | 商店村民不受影响           |
| 全息图（Decent/Holographic/Fancy） | ✅ 免疫   | 全息展示不受影响           |
| FancyNpcs                          | ✅ 免疫   | NPC 不受影响               |
| LevelledMobs                       | ❌ 不免疫 | 升级生物正常清理           |
| WildStacker / RoseStacker          | ❌ 不免疫 | 堆叠实体正常清理           |
| **WorldGuard 区域规则**            | ❌ 关闭   | 不使用 WorldGuard 区域免疫 |

---

## 八、其他设置

| 功能                      | 状态    | 说明                             |
| ------------------------- | ------- | -------------------------------- |
| 调试模式                  | ❌ 关闭 | 不打印详细日志                   |
| 隐藏所有命令              | ❌ 关闭 | togglemessages 等命令所有人可用  |
| 屏蔽 /version 和 /plugins | ❌ 关闭 | 不屏蔽                           |
| 防崩溃物品限制            | ❌ 关闭 | 不拦截大 NBT/长名称/嵌套容器物品 |
| 村民 AI 节流              | ❌ 关闭 | 见上文生物优化                   |

---

## 九、主要命令速查

| 命令                                             | 说明           | 权限              |
| ------------------------------------------------ | -------------- | ----------------- |
| `/clearlag`                                      | 主帮助菜单     | -                 |
| `/clearlag clear <items/entity/all> [--preview]` | 手动清理       | clearlag.clear    |
| `/clearlag info`                                 | 插件信息       | -                 |
| `/clearlag killmobs <evil/good/all>`             | 击杀生物       | clearlag.killmobs |
| `/clearlag reload`                               | 重载配置       | clearlag.reload   |
| `/clearlag nextclear`                            | 距下次清理时间 | -                 |
| `/clearlag topworld`                             | 世界实体排名   | -                 |
| `/clearlag specs`                                | 性能概况       | -                 |
| `/clearlag chunkfinder <数量>`                   | 查找最卡区块   | -                 |
| `/clearlag unloadchunk <世界> <x> <z>`           | 卸载区块       | clearlag.admin    |
| `/clearlag nuke [半径]`                          | 核爆清理       | clearlag.nuke     |
| `/clearlag bossbar`                              | 开关 Boss 血条 | clearlag.bossbar  |
| `/clearlag snapshot`                             | 手动快照       | clearlag.admin    |
| `/clearlag profile`                              | Spark 分析     | clearlag.admin    |
| `/clearlag validate`                             | 验证配置       | clearlag.admin    |
| `/clearlag history <30m/24h/7d>`                 | 性能历史       | -                 |
| `/clearlag gui`                                  | 仪表盘菜单     | -                 |
| `/clearlag web [newtoken]`                       | 网页仪表盘链接 | clearlag.web      |
| `/clearlag timerules <on/off/eventon/eventoff>`  | 昼夜规则开关   | clearlag.admin    |
| `/clearlag worldprofiles <on/off>`               | 各世界配置开关 | clearlag.admin    |

---

## 十、PlaceholderAPI 占位符

> 需要 PlaceholderAPI 作为前置。安装扩展：游戏内执行 `/papi ecloud download ClearLag` → `/papi reload`。
>
> **异步安全**：ClearLag++ 的占位符读取的是插件内部缓存值，不会在异步线程触发主线程检查报错，可直接用于 TAB、计分板等异步刷新场景（替代旧的自研 EntityCount 插件）。

| 占位符                           | 说明               |
| -------------------------------- | ------------------ |
| `%clearlag_nextclear%`           | 距下次自动清理时间 |
| `%clearlag_nextclear_seconds%`   | 距下次清理秒数     |
| `%clearlag_autoclear_enabled%`   | 自动清理开关状态   |
| `%clearlag_autoclear_interval%`  | 清理间隔           |
| `%clearlag_lastclear_items%`     | 上次清理物品数     |
| `%clearlag_lastclear_entities%`  | 上次清理生物数     |
| `%clearlag_lastclear_total%`     | 上次清理总数       |
| `%clearlag_entity_total%`        | 全服实体总数       |
| `%clearlag_dropped_items_total%` | 全服掉落物总数     |
| `%clearlag_mobs_total%`          | 全服生物总数       |
| `%clearlag_ram%`                 | 当前内存使用       |
| `%clearlag_loaded_chunks%`       | 已加载区块数       |

---
