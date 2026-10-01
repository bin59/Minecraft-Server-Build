# ExcellentEnchants —— 80+ 原版风格自定义附魔

> 作者 NightExpress。新增 **80+ 与原版风格一致**的自定义附魔，完全融入附魔台 / 村民交易 / 战利品箱 / 钓鱼 / 铁砧 / 砂轮等原版机制，生存服体验无缝。开源免费。

## 下载与兼容

| 项       | 值                                                                                                                                                                          |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 最新版   | **5.4.3**（2026-06 更新）                                                                                                                                                   |
| 语言     | **官方支持中文**（Spigot 页：All Translatable + Español, 中文, Polski；附魔名称 / 消息随语言显示）                                                                        |
| 服务端   | **1.21.11：Paper ✔ / Spigot ✔ / Folia ❌**；Java 21                                                                                                                         |
| 依赖     | **必须安装 NightCore**（同作者前置库，无玩家命令，后台支撑）                                                                                                                |
| 下载     | [SpigotMC](https://www.spigotmc.org/resources/excellentenchants-%E2%AD%90-80-vanilla-like-enchantments.61693/) ｜ [Modrinth](https://modrinth.com/plugin/excellentenchants) |
| 前置     | [NightCore (Modrinth)](https://modrinth.com/plugin/nightcore)                                                                                                               |
| 官方站   | [nightexpressdev.com/excellentenchants](https://nightexpressdev.com/excellentenchants/)                                                                                     |
| 中文文档 | [snowcutieowo.github.io/ExcellentEnchants](https://snowcutieowo.github.io/ExcellentEnchants/)                                                                               |

## 安装

1. `NightCore.jar` + `ExcellentEnchants.jar` **一起**放进 `plugins/`
2. 重启服务器
3. `/eenchants reload` 可热重载

## ⚠️ 排错：插件不加载

**症状**：jar 已放 `plugins/` 但插件没加载（`/eenchants` 无命令）。

**日志报错**：

```
UnknownDependencyException: Unknown/missing dependency plugins: [nightcore]. Please download and install these plugins to run 'ExcellentEnchants'.
```

**原因**：只放了 `ExcellentEnchants.jar`，**缺 NightCore 前置库**。

**解决**：下载 NightCore（**Paper 版、支持 1.21.x 的最新版本**）放进 `plugins/`，重启服务器；确认 `plugins/` 同时存在 `NightCore-xxx.jar` + `ExcellentEnchants-5.4.3.jar`。

- [NightCore (Modrinth)](https://modrinth.com/plugin/nightcore)
- [NightCore (SpigotMC)](https://www.spigotmc.org/resources/nightcore.105099/)

## 核心设计（与同类插件的区别）

- **完全原版风格**：新附魔与原版并列出现在附魔台、村民交易、战利品箱、钓鱼、生物装备；铁砧合并、砂轮拆书行为与原版一致
- **不改原版**：不能修改 / 禁用原版附魔，也不能自定义新附魔（作者明确不做，只提供 80+ 预置附魔）
- **每个附魔独立配置**：`plugins/ExcellentEnchants/enchants/` 下每个附魔一个 yml，可调最大等级、获取权重、粒子、音效、特效参数
- **充能机制**：附魔消耗充能，用燃料物品（默认**青金石**）在铁砧上充电
- 附魔带粒子与音效反馈（吸血红、连锁挖矿、爆炸箭等）

## 附魔获取途径

| 途径     | 说明                                              |
| -------- | ------------------------------------------------- |
| 附魔台   | 自定义附魔与原版一起出现，青金石 / 等级消耗同原版 |
| 附魔书   | 怪物掉落、宝箱、钓鱼获得；铁砧应用                |
| 村民交易 | 部分自定义附魔书出现在村民交易                    |
| 生物装备 | 怪物可携带附魔自定义附魔的装备生成（5.4+）        |
| 铁砧合并 | 两本同附魔同等级书合并升下一级                    |
| 砂轮     | 可拆书，与原版一致                                |

## 充能机制（新版 5.x）

- 附魔带**充能值**：每次触发消耗充能（`%enchantment_charges_consume_amount%`）
- **充电**：拿燃料物品（默认青金石 `LAPIS_LAZULI`）在铁砧上为附魔物品充电（`%enchantment_charges_recharge_amount%` 每燃料回复量，燃料可在各附魔配置中改）
- 命令生成的附魔默认未充能；加 `-charged` 标志生成**完全充能**附魔
- 充能显示占位符：`%enchantment_charges_*%`（燃料物品名 / 消耗量 / 回复量）

## 命令（`/excellentenchants` 或 `/eenchants`）

| 命令                                                | 用途                                     |
| --------------------------------------------------- | ---------------------------------------- |
| `/eenchants [help]`                                 | 命令列表                                 |
| `/eenchants reload`                                 | 重载插件与附魔配置                       |
| `/eenchants list [player]`                          | 打开新附魔 GUI                           |
| `/eenchants book <附魔> [等级] [玩家] [-charged]`   | 发附魔书；`-1` = 随机等级                |
| `/eenchants randombook [玩家] [-custom] [-charged]` | 随机附魔书；`-custom` 只出非原版附魔     |
| `/eenchants enchant <附魔> <等级> [玩家] [槽位]`    | 附魔手上物品 / 指定槽位；`-1` = 随机等级 |
| `/eenchants disenchant <附魔> [玩家] [槽位]`        | 移除物品上的附魔                         |
| `/eenchants givefuel <附魔> [数量] [玩家]`          | 发充能燃料物品                           |

> 命令别名可在 `engine.yml` 修改。

## 权限

| 节点                                           | 说明                         |
| ---------------------------------------------- | ---------------------------- |
| `excellentenchants.*`                          | 全部功能                     |
| `excellentenchants.command.*`                  | 全部命令                     |
| `excellentenchants.command.book`               | `/eenchants book`            |
| `excellentenchants.command.randombook`         | `/eenchants randombook`      |
| `excellentenchants.command.enchant`            | `/eenchants enchant`         |
| `excellentenchants.command.disenchant`         | `/eenchants disenchant`      |
| `excellentenchants.command.givefuel`           | `/eenchants givefuel`        |
| `excellentenchants.command.list`               | `/eenchants list`            |
| `excellentenchants.command.list.others`        | 查看他人附魔列表             |
| `excellentenchants.command.reload`             | `/eenchants reload`          |
| `excellentenchants.enchant.decapitator.bypass` | 持有者被「斩首」击杀时不掉头 |

## 配置

### config.yml

- `Enchantments -> Disabled`：禁用指定附魔（按注释格式加入，重启生效）
- `Disabled_In_Worlds`：指定世界禁用部分 / 全部自定义附魔（5.4+）

### 附魔目录 `enchants/`

- 每个附魔一个 yml：最大等级、获取权重、触发概率、粒子 / 音效、充能消耗 / 燃料
- **彻底移除某附魔**：把文件从 `enchants/` 移到 `enchants/_disabled_/`，重载生效
- **分布权重**：附魔台 / 钓鱼 / 战利品宝箱 / 生物装备 / 村民交易可分别设权重（权重越高越容易出）；可指定战利品表包含特定附魔

### 当前启用状态（南瓜服）

- **只启用 1 个：夜视（night_vision）**
- 其余 **80 个附魔全部移入 `enchants/_disabled_/`**（已禁用，附魔台 / 战利品 / 村民 / 钓鱼不再出现）
- 生效：重启服务器或 `/eenchants reload`；已存在物品上的旧附魔不受影响
- **临时开启某附魔**：把 `_disabled_/` 下对应 yml 移回 `enchants/`，重载即可
- **全部恢复**：把 `_disabled_/` 下 80 个 yml 移回 `enchants/`

## 中文汉化（附魔名称与描述）

**机制**：插件消息（命令反馈等）走 `lang_cn.yml`，`engine.yml` 里 `Language: zh` 即可生效；但**附魔名称与描述**存在各附魔 yml 里（`DisplayName` / `Description` 字段），**默认英文硬编码、插件不自动翻译**——想显示中文必须手动改配置。

**本服已执行**：运行服 81 个附魔的 `DisplayName` + `Description` **已全部汉化**：

- 名称示例：Veinminer → 连锁挖矿、Silk Chest → 乾坤袋、Decapitator → 斩首者
- 描述示例：`Mines up to %amount% blocks of the ore vein at once.` → `一次挖掘最多 %amount% 个矿脉方块。`
- 汉化时**保留** `<c:#279CF5>` / `<gray>` 颜色标签与全部 `%占位符%`（触发概率 / 伤害 / 半径 / 药水类型等级时长等），游戏内数值正常显示

**生效**：重启服务器，或 `/eenchants reload`。

⚠️ **换新服 / 重装插件后配置会重新生成、汉化丢失**，需重新执行汉化（名称+描述两个脚本，见自研目录可复用）。

## 附魔全览（81 个）

> 分类按可附魔物品划分；效果中的 `%占位符%`（触发概率 / 伤害 / 药水类型等级时长 / 半径等）由游戏内按等级与配置动态显示；**获取权重**数值越高越容易在附魔台 / 战利品中出现（范围 1–1024）。

> ⚠️ ❌ 已禁用 80 个（详见 `enchants/_disabled_/`）：下表除「夜视 night_vision」外，其余附魔当前均不可通过附魔台 / 战利品 / 村民 / 钓鱼获取，全览表仅作配置参考。
全部附魔本身均**非宝藏**；但**当前实际仅「夜视 night_vision」可获取**，其余 80 个已移入 `enchants/_disabled_/` 禁用。
### 近战武器（剑/斧）（23 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **下界之敌** | 对下界生物造成额外 %damage%❤ 伤害。 | 5 | 10 |
| **致盲** | %enchantment_trigger_chance%% 概率在命中时施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 2 | 10 |
| **迷惑** | %enchantment_trigger_chance%% 概率在命中时施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 2 | 10 |
| **治愈** | %enchantment_trigger_chance%% 概率在命中时治愈僵尸猪灵与僵尸村民。 | 3 | 10 |
| **死亡诅咒** | 击杀玩家时，你有概率一同死亡。 | 3 | 2 |
| **切割者** | %enchantment_trigger_chance%% 概率扔掉敌人的盔甲并对其造成 %damage%% 伤害。 | 3 | 2 |
| **斩首者** | %enchantment_trigger_chance%% 概率获得玩家或生物的头颅。 | 2 | 2 |
| **双重打击** | %enchantment_trigger_chance%% 概率造成双倍伤害。 | 2 | 1 |
| **力竭** | %enchantment_trigger_chance%% 概率在命中时施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 4 | 10 |
| **寒冰附身** | 命中时冻结目标并施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 10 |
| **敏捷** | 将所有生物掉落物直接移入你的背包。 | 1 | 2 |
| **麻痹** | %enchantment_trigger_chance%% 概率在命中时施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 5 | 5 |
| **暴怒** | %enchantment_trigger_chance%% 概率在命中时获得 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 2 | 5 |
| **火箭** | %enchantment_trigger_chance%% 概率将敌人击飞上天。 | 3 | 5 |
| **顺手牵羊** | %enchantment_trigger_chance%% 概率从玩家身上偷取 %amount% 点经验。 | 3 | 2 |
| **淬炼** | 每失去 %radius%❤ 生命值，额外造成 %amount%% 伤害。 | 5 | 1 |
| **节俭** | %enchantment_trigger_chance%% 概率使生物掉落刷怪蛋。 | 3 | 2 |
| **雷霆** | %enchantment_trigger_chance%% 概率引下闪电，额外造成 %damage%❤ 伤害。 | 5 | 5 |
| **吸血鬼** | %enchantment_trigger_chance%% 概率在命中时恢复 %amount%❤ 生命值。 | 3 | 2 |
| **蛇毒** | %enchantment_trigger_chance%% 概率在命中时施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 2 | 10 |
| **村庄卫士** | 对所有掠夺者造成额外 %amount%❤ 伤害。 | 5 | 10 |
| **智慧** | 生物掉落 %modifier% 倍更多经验。 | 5 | 5 |
| **凋零** | %enchantment_trigger_chance%% 概率在命中时施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 2 | 5 |

### 弓/弩（15 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **轰炸机** | %enchantment_trigger_chance%% 概率射出引燃 %time%s. 的TNT。 | 3 | 1 |
| **迷惑之箭** | %enchantment_trigger_chance%% 概率使箭矢附带 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 10 |
| **黑暗之箭** | %enchantment_trigger_chance%% 概率使箭矢附带 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 10 |
| **龙焰之箭** | %enchantment_trigger_chance%% 概率使箭矢附带龙焰效果（半径=%radius%，%duration%s）。 | 3 | 2 |
| **电击之箭** | %enchantment_trigger_chance%% 概率使箭矢引下闪电，额外造成 %damage%❤ 伤害。 | 3 | 5 |
| **末影之弓** | 射出末影珍珠而非箭矢。 | 1 | 1 |
| **爆炸之箭** | %enchantment_trigger_chance%% 概率射出爆炸箭矢。 | 3 | 5 |
| **照明弹** | %enchantment_trigger_chance%% 概率在箭矢落点生成火把。 | 1 | 5 |
| **恶魂** | 射出火球而非箭矢。 | 1 | 1 |
| **悬浮** | %enchantment_trigger_chance%% 概率使箭矢附带 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 10 |
| **滞留** | %enchantment_trigger_chance%% 概率使药水箭生成滞留效果。 | 3 | 2 |
| **剧毒之箭** | %enchantment_trigger_chance%% 概率使箭矢附带 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 5 |
| **狙击手** | 投射物速度提升 %amount%%。 | 2 | 10 |
| **吸血之箭** | %enchantment_trigger_chance%% 概率在箭矢命中时恢复 %amount%❤ 生命值。 | 3 | 2 |
| **凋零之箭** | %enchantment_trigger_chance%% 概率使箭矢附带 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 5 |

### 挖掘工具（镐/斧/工具）（12 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **爆破挖掘** | %enchantment_trigger_chance%% 概率以爆炸方式挖掘方块。 | 5 | 2 |
| **玻璃破碎者** | 瞬间破坏玻璃。 | 1 | 10 |
| **矿工狂热** | 挖掘方块时获得 %enchantment_potion_type% %enchantment_potion_level% 效果。 | 3 | 2 |
| **幸运矿工** | %enchantment_trigger_chance%% 概率从矿石获得 %amount%% 更多经验。 | 3 | 5 |
| **自动播种** | 右键点击与收获时自动补种作物。 | 1 | 1 |
| **乾坤袋** | 掉落箱子并保存其中全部内容。 | 1 | 1 |
| **精准刷怪笼** | %enchantment_trigger_chance%% 概率挖掘刷怪笼。 | 1 | 1 |
| **冶炼** | %enchantment_trigger_chance%% 概率自动冶炼挖掘的方块。 | 5 | 5 |
| **念力** | 将所有方块掉落物直接移入你的背包。 | 1 | 1 |
| **伐木工** | 砍倒整棵树。 | 1 | 2 |
| **隧道** | 按特定形状一次挖掘多个方块。 | 3 | 1 |
| **连锁挖矿** | 一次挖掘最多 %amount% 个矿脉方块。 | 3 | 2 |

### 胸甲/护甲（9 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **寒钢** | %enchantment_trigger_chance%% 概率向攻击者施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 5 |
| **黑暗斗篷** | %enchantment_trigger_chance%% 概率向攻击者施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 5 |
| **龙心** | 永久获得 %enchantment_potion_type% %enchantment_potion_level% 效果。 | 5 | 2 |
| **元素防护** | 减少 %amount%% 的药水与元素伤害。 | 4 | 10 |
| **火焰护盾** | %enchantment_trigger_chance%% 概率点燃攻击者 %duration%s.。 | 4 | 2 |
| **硬化** | %enchantment_trigger_chance%% 概率在受伤时获得 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 2 | 5 |
| **寒冰护盾** | %enchantment_trigger_chance%% 概率冻结攻击者并施加 %enchantment_potion_type% %enchantment_potion_level%（%enchantment_potion_duration%s.）。 | 3 | 10 |
| **神风** | %enchantment_trigger_chance%% 概率在死亡时爆炸。 | 3 | 5 |
| **再生** | 每隔几秒恢复 %amount%❤ 生命值。 | 4 | 2 |

### 头盔（3 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **夜视** | 永久获得 %enchantment_potion_type% %enchantment_potion_level% 效果。 | 1 | 1 |
| **饱和** | 每隔几秒恢复 %amount% 点饥饿值。 | 2 | 2 |
| **水下呼吸** | 永久获得 %enchantment_potion_type% %enchantment_potion_level% 效果。 | 1 | 1 |

### 靴子（5 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **火焰行者** | 可在岩浆上行走，免疫岩浆块伤害。 | 2 | 1 |
| **高跳** | 永久获得 %enchantment_potion_type% %enchantment_potion_level% 效果。 | 2 | 2 |
| **轻盈** | 可安全踩踏海龟蛋、耕地与大型垂滴叶。 | 1 | 10 |
| **回弹** | 落地时获得粘液块般的弹跳效果。 | 1 | 2 |
| **疾速** | 永久获得 %enchantment_potion_type% %enchantment_potion_level% 效果。 | 2 | 2 |

### 护腿（1 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **阻滞力** | %enchantment_trigger_chance%% 概率减少 %amount%% 击退效果。 | 3 | 5 |

### 钓鱼竿（6 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **自动收线** | 咬钩时自动收线。 | 1 | 1 |
| **溺尸诅咒** | %enchantment_trigger_chance%% 概率钓上溺尸。 | 3 | 5 |
| **双倍捕获** | %enchantment_trigger_chance%% 概率使捕获物品数量翻倍。 | 3 | 2 |
| **河流之主** | 增加抛竿距离。 | 5 | 10 |
| **老练渔夫** | 钓鱼获得的经验增加 %amount%%。 | 3 | 5 |
| **生存专家** | 钓到的生鱼自动烤熟。 | 1 | 2 |

### 三叉戟（1 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **炼狱** | 投出的三叉戟命中时点燃敌人 %time%s.。 | 3 | 10 |

### 诅咒/通用（可耐久、工具武器）（6 个）

| 附魔 | 效果 | 最大等级 | 获取权重 |
| --- | --- | ---: | ---: |
| **破碎诅咒** | %enchantment_trigger_chance%% 概率额外消耗 %amount% 点耐久。 | 3 | 10 |
| **脆弱诅咒** | 阻止物品被砂轮或铁砧处理。 | 1 | 10 |
| **平庸诅咒** | %enchantment_trigger_chance%% 概率使掉落物品被解除附魔。 | 3 | 5 |
| **厄运诅咒** | %enchantment_trigger_chance%% 概率使方块或生物不掉落任何东西。 | 3 | 5 |
| **复原** | %enchantment_trigger_chance%% 概率使物品免于损坏并恢复至 %amount%% 耐久。 | 3 | 2 |
| **灵魂绑定** | 死亡时防止物品掉落。 | 1 | 2 |

## 与南瓜服集成建议

- **经济出口**：附魔书可作为 EconomyShop / 拍卖行商品（稀有附魔书定价出售，形成消耗 + 交易品）
- **平衡**：先只开 5–8 个核心附魔试水（吸血红、连锁挖矿、爆炸箭等），避免技能过多改变原版平衡；配合 `Disabled` / `_disabled_/` 关闭不想用的
- **世界限制**：若不想在主城出现破坏性附魔，用 `Disabled_In_Worlds` 单独关
- **充能消耗**：充能机制天然是经济消耗点（玩家需要青金石），可让青金石成为硬通货之一
- **注意**：与自定义附魔类插件**只能装一个**（同类冲突）；本服未装同类插件则无冲突
