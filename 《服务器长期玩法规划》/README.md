# 服务器长期玩法规划（南瓜生存服 / 南瓜国际服）

> 目标：让一个已经开服几个月、玩家中期开始流失的生存服，**长期有人玩**。
> 核心方法论一句话：**长期留存靠"多条并行的进度线"**——建造党、肝帝、社交党、PVP 党、收藏党、剧情党，各有一直玩下去的理由，而不是只有"挖矿—造房子—AFK"一条线走到头。
>
> 原则（契合现有运维习惯）：**尽量用现有插件栈实现，少上新重插件**；改动小、可回退、配置化；任何新系统都要有"每天上来"的钩子。

> ⚠️ **关于 neOmega 的前提说明**：用户环境为**网易（NetEase）版 neOmega**，**不是可自由编写任意 Lua 逻辑的开源 neOmega 框架**，本规划**不将其作为通用 RPG 脚本平台依赖**。原有的签到计分板 / 信任传送 / 群服互通等既成功能仍由它承担，但新增的 RPG 系统（每日任务脚本、Boss 定时、城镇统计、成就核验等）一律改用成熟插件实现，不假设可自由脚本。具体选型见 `插件方案对比.md`。

---

## 0. 现状资产盘点（已有、可直接复用）

| 类别 | 已有插件 / 系统 | 本次规划怎么用 |
|---|---|---|
| 权限 / 标签 | LuckPerms + TAB（`tabprefix`/`tagprefix` 读 `%luckperms_prefix%`） | 职业、阵营、称号全走组前缀；新人 `[新人]` 标签机制已验证可用 |
| 自研菜单 | QuickMenu（取代 DeluxeMenus） | 职业选择、任务领取、活动报名、商店入口的统一界面 |
| 领地 | Residence + 自研 AdvZone（非规则区域保护） | 城镇划分、副本分区、商业区、阵营领地 |
| 经济 / 货币 | 南瓜币（Essentials `money` 真源）+ `import-trade-history.mjs` 幂等回填 | 所有系统的奖励、交易、排行都围绕南瓜币转 |
| PVP / Boss | VexBot（智能 PVP 机器人，`/vexbot spawn <名> <kit>` + `advancedspawn`） | 改造为 Boss / 训练假人 / 副本小怪 |
| 宠物 / 收集 | SimplePets | 成就奖励、收藏线 |
| 展示 / 叙事 | DecentHolograms + BlockJournal「随笔」+ 腾讯文档 smartcanvas 手册 | 成就墙、遗迹石碑、服务器传说、玩家故事沉淀 |
| 统计 / 分析 | Plan + TrackPlayer | 玩家活跃度、单玩家流量、成就进度核验 |
| 清理 / 优化 | ClearLag（已配船/矿车/下落方块/点燃 TNT 保护） | 保障新增玩法实体不被误清 |

> 结论：**核心系统可基于现有栈搭建**；任务 / 技能 / 成就 / 商店等按需补**轻量免费插件**（选型见 `插件方案对比.md`），不引入重插件、不引入第二套领地系统。

---

## 1. 总架构：八条并行进度线

```
                 ┌─────────────────────────────────────────┐
                 │           长期留存总引擎                  │
                 └─────────────────────────────────────────┘
   ┌──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
   │职业  │任务  │Boss │经济  │城镇  │传说  │成就  │赛季  │
   │技能  │剧情  │副本  │商店  │阵营  │lore │收集  │活动  │
   └──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘
        ↑________________ 全部以「南瓜币 + 称号/前缀」为统一货币与荣誉 ________________↑
```

每条线相互独立又能交叉（例：副本掉职业专属材料 → 经济流通 → 城镇共建 → 成就解锁），玩家流失任何一条线仍有其他线在跑。

---

## 2. 系统 1：职业 / 技能系统（改动最小、立即可上）

**目的**：给不同玩法风格的玩家一个"身份"和专属增益，让"随便玩"变成"我是个战士/工匠"。

**实现**（LuckPerms 组 + QuickMenu 选职界面 + TAB 前缀）：
1. 建职业组（继承 `default`，设权重、前缀）：
   ```text
   /lp group warrior  create
   /lp group warrior  parent add default
   /lp group warrior  meta setprefix "&c[战士]&f "
   /lp group warrior  setweight 10

   /lp group farmer   create
   /lp group farmer   parent add default
   /lp group farmer   meta setprefix "&a[农夫]&f "
   /lp group farmer   setweight 10

   /lp group artisan  create
   /lp group artisan  parent add default
   /lp group artisan  meta setprefix "&e[工匠]&f "
   /lp group artisan  setweight 10

   /lp group merchant create
   /lp group merchant parent add default
   /lp group merchant meta setprefix "&b[商人]&f "
   /lp group merchant setweight 10

   /lp group mage     create
   /lp group mage     parent add default
   /lp group mage     meta setprefix "&d[法师]&f "
   /lp group mage     setweight 10
   ```
2. 职业增益（用 permission 节点 + 插件属性，避免直接给破坏平衡权限）：
   - 战士：`essentials.kit.warrior`（专属装备包）、近战伤害加成（LuckPerms 组权限 + 属性插件如 AuraSkills）
   - 农夫：作物产量加成、`<essentials.feed>` 冷却短
   - 工匠：`essentials.repair` 免费 / 附魔折扣
   - 商人：商店交易税减免（经济系统里读 `merchant` 组）
   - 法师：consumable 类增益（如抗火、夜视时长加成）
3. QuickMenu 做"职业选择"界面：新玩家点菜单 → 选职 → 执行 `/lp user <p> parent set <group>`（注意：**替换主组即去掉 `[新人]` 标签**，自动毕业）。
4. TAB 前缀复用 newbie 机制，职业前缀直接显示。

**进阶（可选）**：想要"刷怪/挖矿真涨属性、有技能树" → 上 **AuraSkills**（免费、基岩无碍），与 LuckPerms 组身份叠加。

**回退**：职业只是 LuckPerms 组，删组即恢复 `default`，零副作用。

---

## 3. 系统 2：任务 / 剧情（每日钩子 + 长线叙事）

**目的**：制造"每天上来做点什么"的强制回访理由，同时用主线剧情给世界厚度。

**每日 / 周常（基于 Quests / BetonQuest）**：
- 每日任务：挖 X 块、击杀 Y 怪、交易 Z 次 → 给南瓜币 + 连签额外加成
- 连签 7/30 天大奖：SimplePets 宠物蛋 / 限定前缀称号
- 实现：任务插件配置每日目标 + 连签奖励，奖励写入南瓜币（走现有 `money` 通道）

**长线主线（BetonQuest）**：
- 用 BlockJournal「随笔」+ 腾讯文档手册承载剧情文本
- DecentHolograms 做 NPC 对话石碑，点击触发任务
- 主线分章节，几个月逐步放出，保持内容更新节奏

> 选型说明：只做简单每日任务用 **Quests**（GUI 建、零脚本）；想做分支剧情用 **BetonQuest**（配 Citizens NPC）。均不依赖 neOmega 脚本。

---

## 4. 系统 3：Boss / 副本（中长线内容填充）

**目的**：给 PVP/战斗党和高战力玩家持续目标，产出职业材料与经济流通物。

**Boss（VexBot 改造）**：
```text
# 先开 advancedspawn（全局一次）
/vexbot settings advancedspawn on
# 存一套 Boss 装备 kit（穿上再存）
/vexbot createkit dragonboss
# 召唤带 kit 的 Boss
/vexbot spawn 末影之主 dragonboss
```
- 用计划任务插件（如 ScheduledCommands）或活动手动/定时触发在世界指定坐标刷 Boss，全服通告
- Boss 掉落：自定义物品（如 `netherite_spear` + `minecraft:lunge` 穿刺附魔，用 `/give` 输出）

**副本（Residence + AdvZone）**：
- 用 Residence 划副本区，AdvZone 做非规则区域保护 / 进入权限
- 副本内怪物由 VexBot 小队 + 自然刷怪组成，通关给成就 + 南瓜币
- 副本进度用 Plan 数据核验，防作弊

> 进阶：认真做多阶段 Boss / 地牢 → 上 **MythicMobs**（可控、社区资源多）；想要开箱即用地牢 → **EliteMobs** 一体包。均不依赖 neOmega。

---

## 5. 系统 4：玩家经济 / 商店（让南瓜币真正流动）

**目的**：货币有意义 = 玩家为稀缺资源长期在线。

**玩家商店 / 商业区**：
- Residence 划一块"商业区"领地，玩家可租摊位
- QuickMenu 做统一商店入口；交易走南瓜币（`money`）
- 商人职业（`merchant` 组）享交易税减免，激励摆摊

**商店插件（轻量免费）**：
- 玩家摆摊：**QuickShop-Hikari**（免费、Residence 集成防乱放）
- 统一定价商店：**EconomyShopGUI**（免费、官方称"为基岩设计"）
- 所有交易流水自动进 `trade.log` → `import-trade-history.mjs` 幂等回填，南瓜币明细不断档

**核心**：经济系统必须与职业、Boss、任务奖励互通，形成"打 Boss → 掉材料 → 摆摊卖 → 别人买 → 买装备 → 打更强 Boss"的闭环。

---

## 6. 系统 5：城镇 / 阵营 RP（社交绑定最硬）

**目的**：把玩家互相锁在一起，比任何系统都留人——这是长期服的命脉。

**城镇**：
- 每个城镇 = 一个 Residence 大领地 + 子领地（队友 `/res subzone`）
- 城镇有公共仓库、城墙、图腾（DecentHolograms 显示城镇名/等级）
- 城镇等级随成员数 / 共建建筑数提升（用经济或领地数据估算，或手动维护）

**阵营 / 公会**：
- LuckPerms 建 `faction_a` / `faction_b` 等组，TAB 前缀显示阵营色（复用职业前缀机制）
- 阵营关系（同盟 / 敌对）用组权限 + 自定义逻辑
- 阵营战：定期领土争夺，胜方获专属前缀 / 领地特权

> ⚠️ 城镇/阵营**只用 Residence + LuckPerms**，不要引入 Towny / Lands / Factions（会和第二套领地系统冲突）。

---

## 7. 系统 6：传说 / Lore（世界厚度，低投入高感知）

**目的**：让老玩家有"我见证过历史"的归属感。

- **BlockJournal「随笔」**：玩家写服务器故事、建筑分享（已有 UUID 映射机制）
- **腾讯文档手册**：玩家向 wiki（已用 smartcanvas 维护"南瓜国际服"手册）
- **DecentHolograms 遗迹石碑**：在世界各处放石碑，点开读一段服务器传说 / 历史事件
- **节日事件沉淀**：每次赛季活动结束，把战报写进腾讯文档，成为"编年史"

---

## 8. 系统 7：成就 / 收集（收藏党的终点）

**目的**：给"什么都要集齐"的玩家无限目标。

- **AdvancedAchievements**（免费）：原生追踪 55 类成就、GUI、排行、奖励（含 Vault 经济 / PAPI），替代手动核验
- **DecentHolograms 成就墙**：在主城展示全服成就列表，已解锁亮、未解锁灰
- **SimplePets 收集**：限定宠物作为成就奖励（如全 Boss 击杀解锁"龙崽"）
- 全收集追踪：用 AdvancedAchievements 原生追踪 + Plan 数据核验（全作物、全生物图鉴、全 Boss）
- 成就奖励：专属前缀（如 `[传奇]`）、南瓜币、宠物蛋

---

## 9. 系统 8：赛季活动（制造节奏）

**目的**：周期性事件制造话题与回流高峰。

- **建筑赛**：季度主题建造，QuickMenu 报名 + 玩家投票
- **PVP 联赛**：用 VexBot 训练 + 玩家对战，计分板排行
- **寻宝 / 解谜**：藏宝图 + DecentHolograms 线索
- **赛季结算**：排行榜发奖（南瓜币 + 限定前缀），战报进腾讯文档编年史

---

## 10. 落地优先级（投入小、留存高优先）

| 阶段 | 时间 | 内容 | 依赖 | 见效 |
|---|---|---|---|---|
| **阶段 1** | 第 1–2 周 | ① 每日/连签任务（Quests/BetonQuest）② 职业系统（5 组 + QuickMenu 选职） | LuckPerms、QuickMenu、任务插件 | 立刻有"每天上来"钩子 |
| **阶段 2** | 第 1 月 | ③ 玩家商店 / 商业区 ④ 城镇 RP + 阵营前缀 | Residence、TAB、经济 | 社交绑定成型 |
| **阶段 3** | 第 2–3 月 | ⑤ Boss/副本 ⑥ 成就墙 + 收集 ⑦ 传说石碑 | VexBot、DecentHolograms、SimplePets、AdvancedAchievements | 中长线内容填充 |
| **阶段 4** | 持续 | ⑧ 赛季活动 + 剧情章节滚动放出 | 以上全部 | 长期节奏 |

> 阶段 1 全部基于现有插件 + 轻量免费任务插件，建议**立刻启动**。

---

## 11. 风险与注意

1. **平衡性**：职业增益用 permission + 插件属性增量，不要直接给 `*` 或破坏生存体验的权限；每次加增益先在测试服（南瓜生存服本地）验证。
2. **轻量优先**：DeluxeMenus 等重型方案已弃用，坚持 QuickMenu 自研路线；新插件只在阶段 2+ 确需时评估（见 `插件方案对比.md`）。
3. **配置化**：所有过滤 / 增益 / 奖励规则写入配置文件（贴合你"过滤逻辑写配置"的习惯），便于回退。
4. **ClearLag 兼容**：新增实体（Boss、宠物、展示物）确认已在 `excluded-entities` 保护列表（船/矿车/下落方块/点燃 TNT 已配，Boss/VexBot 实体需补）。
5. **数据真源**：南瓜币真源 = Essentials `money`；任何奖励写入走现有通道 + `import-trade-history.mjs` 幂等回填，保持明细不断档。
6. **同步纪律**：云端 / 本地配置一致；部署类改动两端同步（你既定规范）。
7. **不依赖 neOmega 脚本**：网易 neOmega 不可当通用 RPG 脚本平台，新增系统用成熟插件实现。

---

## 12. 快速启动清单（阶段 1 即可执行）

- [ ] LuckPerms 建 5 个职业组 + 前缀 + 权重（命令见 §2）
- [ ] QuickMenu 做"职业选择"界面，执行 `parent set`
- [ ] 任务插件（Quests/BetonQuest）配置每日任务 + 连签奖励写入南瓜币
- [ ] TAB 确认职业前缀显示正常（复用 newbie 机制）
- [ ] VexBot `advancedspawn on`，备一套 Boss kit 待用
- [ ] ClearLag `excluded-entities` 补 VexBot/Boss 实体类型，防误清
- [ ] 腾讯文档手册新增"玩法总览"页，向玩家公布职业/每日系统

---

*文档生成于 2026-10-18，基于现有插件栈与已验证机制（newbie 标签、both 基岩方案、ClearLag 船/矿车/下落方块/TNT 保护、VexBot kit 召唤等）。已剔除对 neOmega（网易版）的脚本能力假设，全部方案基于现成插件。配套 `插件方案对比.md`。*
