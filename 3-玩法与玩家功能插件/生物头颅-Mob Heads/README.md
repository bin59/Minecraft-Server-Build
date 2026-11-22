# 生物头颅（Mob Heads）

全生物头颅掉落玩法。**本服已安装**真·Mob Heads（`mob-heads-v4.5.0.1-paper.jar`，2026-11-22 05:55 放入 `plugins/`，支持 26.3）——500+ 种头颅、全生物/变种/幼年、收集成就。本文记录版本实况、掉率机制与**如何调低概率**，以及与 `PlayerHeadDrops` 内置生物头的区别。

> ⚠️ **口径纠错（2026-11-22）**：本文档初版误判「本服未装 Mob Heads」，把生物头概率当成 `PlayerHeadDrops` 的 `mob-heads` 去改了——那是玩家头颅插件附带的 6 种原版头小功能，且处于关闭状态，改了不生效。该误改**已全部撤销**（配置从备份还原、文档数值复原）。真正要调的是下面这个已装的 Mob Heads。

## 0. 版本与官方资料（2026-11-22 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本服现状 | ✅ **已安装** `plugins/mob-heads-v4.5.0.1-paper.jar`（3,403,267 B，2026-11-22 05:55）；`paper-plugin.yml` 内 `version: 1-v4.5.0.1`、`api-version: 1.21.4`、主类 `dev.jodek.dpshim.PackPlugin`（数据包封装成 Paper 插件的壳） |
| 支持 26.3 的版本 | **v4.5.0.1**（更新日志写 `1.21.9 - 26.3 compatibility`）——本服装的正是它，版本选对了 |
| 版本坑 | 最新 **v4.5.2.0**（11-20）文件名只标 `1.21.9-26.2`，**不支持 26.3**，别去更新；v5.x 是并存的旧版本号线，选包看**文件名标注的版本区间**，不看版本号大小 |
| 载体形态 | 同一版本出 3 种：`.zip`（数据包）、`-paper.jar`（本服用的这种）、`.jar`（Fabric/Forge 等 mod）。paper jar 就是把数据包包进插件、由 Paper 自动加载 |
| 运行环境 | Server-side（客户端原版直连不用装）；支持 Paper / Purpur / Fabric / Forge / NeoForge / Quilt |
| 必需依赖 | 官方标注需 `5555ff-lib` 数据包；paper jar 形态是否已内置该依赖**未启服无法确认**，以游戏内实际加载为准 |
| 官方主页 | <https://modrinth.com/datapack/mob-heads> |
| 官方站 | <https://jodek.dev/datapacks/mob-heads/> |
| 官方版本页（本服所用） | <https://modrinth.com/datapack/mob-heads/version/v4.5.0.1> |
| 官方更新日志 | <https://modrinth.com/datapack/mob-heads/changelog> |
| 官方头颅图鉴 | <https://codeberg.org/jodeks-datapacks/Mob-Heads/wiki/Mob-Heads> |
| 官方问题反馈 | <https://codeberg.org/jodeks-datapacks/mob-heads/issues> |
| 必需依赖下载 | <https://modrinth.com/datapack/lib-5555ff> |
| 官方中文翻译资源包 | <https://modrinth.com/resourcepack/jodeks-translations> |

## 它是什么

- **500+ 种头颅**：全生物、幼年个体、各种变种（不同颜色羊、不同纹理村民、冷/温/热变体等）。
- **每种生物独立掉率**：默认 2%（鸡/牛/猪/僵尸）到 20%（末影龙/凋灵）、15%（远古守卫者/监守者）。
- **收集成就**：内置 advancement 进度追踪。
- **音符盒音效**：头颅放置后自带 note block 声音。
- **纯服务端**：客户端原版直连（含基岩通过 Geyser），不用装 mod；中文头颅名/提示需配官方翻译资源包。

## 掉率配置——怎么「缩小概率」（重点）

**关键机制**：这个插件的掉率**不在任何明文 yml 里**，存在运行时的两块数据中：

| 存储位置 | 内容 |
| --- | --- |
| `storage mob_heads:root data.*` | 全局 `drop_rate_multiplier`（掉率乘数，默认 1）与每种生物的 `drop_rate_<生物>` |
| `scoreboard ... mob_heads.config` / `mob_heads.drop_rates` | 开关位（玩家击杀 / 蓄力苦力怕）与配合 GUI 的计分 |

### 方法一（推荐）：游戏内配置菜单

```mcfunction
/trigger mob_heads.config
```

菜单里可以：
- 切换掉落来源（玩家击杀 / 蓄力苦力怕）
- **单独改每种生物掉率**，或一键全部设成 100%
- 有一个**全局「掉率乘数」（drop rate multiplier）**——这才是「统一缩小概率」的正解：不用逐种改，把它调成 `0.2` 就等于全生物掉率 ×0.2（2%→0.4%、20%→4%）
- 开关掉落消息、改消息文案

给自己发头颅测试：`/trigger get_mob_head`

### 方法二：命令行直接改乘数（供服主参考，需进服实测）

```mcfunction
data modify storage mob_heads:root data.drop_rate_multiplier set value 0.2
```

- 官方内部就是这么存的（见 jar 内 `function/config/update/update_drop_rates.mcfunction`）。
- 该值随世界存档持久化（初始化用的是 `execute unless data ... set value 1`，已有值不会被覆盖）。
- ⚠️ 直接改 storage 后，**游戏内菜单的显示值可能不同步**；稳妥起见改完进 `/trigger mob_heads.config` 确认一次。本鱼**未启服实测**，命令以游戏内实际行为为准。

> 补充：**抢夺（Looting）附魔不影响本插件掉率**（官方明示）。这与 `PlayerHeadDrops` 的 `looting-boost` 不同。

## 默认掉率（节选，完整表见官方主页）

| 生物 | 掉率 | 生物 | 掉率 |
| --- | --- | --- | --- |
| 鸡 / 牛 / 猪 / 羊 / 僵尸 / 骷髅 / 苦力怕 / 蜘蛛 | 2% | 末影人 / 女巫 / 恶魂 | 2–3% |
| 岩浆怪 / 史莱姆 / 蝙蝠 / 蜜蜂 | 2–5% | 猪灵蛮兵 / 僵尸村民 | 10% / 7% |
| 远古守卫者 / 监守者 | 15% | 末影龙 / 凋灵 | 20% |
| 蓄力苦力怕 | 10% | 幼年/变种生物 | 多数 5–10% |

## 基岩（Geyser）兼容

官方 changelog 记载 **v5.3.0 曾修复「基岩玩家进服被断开」**——属另一版本线，本服的 v4.5.0.1 是否含该修复未知。本服以基岩玩家为主，**建议让基岩号先进服实测**再开放给全体。配置走 `/trigger` 命令（非 GUI 点击），理论上不受 Geyser 背包 GUI 左右键限制影响，但仍以实测为准。

## 与 PlayerHeadDrops 内置生物头的区别

本服两个插件都在，但要分清：

| 维度 | Mob Heads v4.5.0.1（已装，主力） | PlayerHeadDrops 2.0.0 的 `mob-heads`（已装但关闭） |
| --- | --- | --- |
| 生物头种类 | **500+ 种**（全生物/变种/幼年） | **6 种**（僵尸/骷髅/苦力怕/凋零骷髅/猪灵/末影龙） |
| 状态 | 已安装，掉率生效 | `enabled: false`，**完全不生效** |
| 掉率配置 | 游戏内 `/trigger mob_heads.config`，存 storage/scoreboard | 改 `config.yml` 的 `mob-heads.chances`（当前 0.05/0.10/1.0） |
| 收集成就 / 音符盒音效 | 有 | 无 |
| 玩家头掉落 | 不支持 | **支持**（本服玩家头主力：30% 概率 + 10 分钟冷却 + 中文公告） |

**结论**：调「生物头概率」= 调 Mob Heads 的全局乘数（上面的方法一/二）；**不要**去动 `PlayerHeadDrops` 的 `mob-heads`（它是关的，改了没用）。两个都开着会让那 6 种生物双重掉头，非本意。

参考：[PlayerHeadDrops 完整文档](../玩家头颅掉落-PlayerHeadDrops/PlayerHeadDrops.md)

## 边界

本文是资料与操作说明。**本鱼没有启服、没有执行任何 `/trigger` 或 `data modify` 命令、没有改运行中的 storage/scoreboard、没有碰数据库与云端**。此前误改的 `PlayerHeadDrops/config.yml` 已用备份完整还原，两份文档里的错误数值也已复原。真正的概率调整由服主进服按上面方法完成。

## 相关文档

- [玩家头颅掉落](../玩家头颅掉落-PlayerHeadDrops/PlayerHeadDrops.md)
- [根目录索引](../../README.md)
