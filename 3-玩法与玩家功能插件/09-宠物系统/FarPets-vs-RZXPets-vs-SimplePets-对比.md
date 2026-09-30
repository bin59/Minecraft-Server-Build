# FarPets vs RZXPets vs SimplePets 对比

> 适用场景：南瓜生存服（Leaf 1.21.11，Paper 分支，Java + Geyser/Floodgate 基岩互通，已接 Vault 经济，当前运行 SimplePets + Vault Addon）

***

## 一、一句话速览

| 插件             | 作者 / 出处                                                    | 定位                      | 一句话画像                                                                                                                                         |
| -------------- | ---------------------------------------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **FarPets**    | MavenPL（波兰），SpigotMC #138745                               | "装饰 + 轻度养成" 宠物插件 | v1.0.2，仅 14 次下载、0 评分；50+ 原版宠物、22 种被动加成、等级经验、70+ 粒子，但**无 Vault、无自定义模型、无开源仓库**。                                            |
| **RZXPets**    | Kurz（GitHub: KurzIsRio），SpigotMC #137426                   | "肩膀漂浮宠物 + 玩家被动 buff" 插件 | 2026-07-27 发布，v1.0.1，下载～24、GitHub 0 star / 0 issue；原版生物 1→100 级，10 种随等级缩放的 buff 机制，接 Vault；**无骑乘、无自定义模型、强依赖未公开的私有 RZXCore 框架**。               |
| **SimplePets** | brainsynder / BSDevelopment（仓库 brainsynder-Dev/SimplePets） | 老牌 "原版生物外观宠物" 插件        | 十年历史、R5 重写版 \$12 付费、GPL-3.0 开源；90 种原版生物可骑 / 可飞 / 可顶头 / 多只同放，官方**明确支持 1.21.11**，Vault Addon 免费；但**无内置等级 / 技能、无自定义模型 / MythicMobs、issue 响应偏慢**。 |

***

## 二、功能对比矩阵

| 维度                       | FarPets                                                         | RZXPets                                                                                         | SimplePets（R5）                                                                               |
| ------------------------ | --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| 宠物数量 / 类型                | 50+ 原版生物（含 "无害化" 原版敌对生物）                                        | 原版实体白名单（Allay/Parrot/Bat/Blaze/Dragon 等）                                                        | 90 种原版生物，仍在增加                                                                                |
| 自定义模型（ME/IA/Oraxen/Nexo） | 未查到支持                                                           | 未查到支持                                                                                           | **明确不支持**（官方 addon 清单无模型类）                                                                   |
| MythicMobs 怪物当宠物         | 未查到支持                                                           | 未查到支持                                                                                           | 无官方 addon                                                                                    |
| 宠物穿装备 / 盔甲               | 未查到                                                             | 无                                                                                               | 无护甲槽（仅原版 NBT 外观数据）                                                                           |
| 粒子特效                     | **70+ 条粒子轨迹可选**                                                 | 仅攻击触发粒子（默认 CRIT，可配）                                                                             | R5 未把粒子拖尾列为核心特性                                                                              |
| 跟随模式                     | 坐 / 跟随切换、召唤 / 解散、tricks 动作                                      | 悬浮跟随 / 停肩，仅 summon/dismiss，无距离 / 坐停                                                             | 跟随速度 / 距离在每宠 JSON 调整；GUI + 召唤 / 解散                                                           |
| 骑乘（坐骑）                   | 支持（1.0 飞行卡骑 bug 已在 1.0.2 修）                                     | **不支持**                                                                                         | **支持**（GUI Ride 按钮，`pet.type.<mob>.mount`）                                                   |
| 飞行                       | 鹦鹉等作为可骑乘飞行坐骑；宠物自主飞行跟随未明确                                        | 满级后**给玩家**解锁生存飞行（非骑宠飞）                                                                          | 按宠物独立开关 `pet.type.<mob>.fly`                                                                 |
| 宠物帽子（顶头上）                | 无                                                               | 无（停肩）                                                                                           | **有** `pet.type.<mob>.hat`                                                                   |
| 多宠物同放                    | 未明确                                                             | 单只（Storage 仓库切换）                                                                                | **支持多只同放 + Pet Saves 存盘再召**                                                                  |
| 繁殖 / 幼年成长                | 未查到繁殖；幼年形态未提                                                    | 无                                                                                               | 无繁殖；幼年仅是 NBT 开关 `{baby:true}`，不成长                                                            |
| 等级 / 经验 / 技能             | **独立等级 + XP**，22 种物种绑定被动加成（抗火、钓鱼辅助、近战增伤等）                       | **1→100 级 XP**，10 种 level-scaling 机制（药水效果 / 飞行 / 挖矿打怪 zGems / 吸血 / 双倍收获 / 范围治疗 / 冲锋等），可 YAML 扩展 | **无内置等级 / 技能**（官方把 leveling 列为社区可写 addon 示例）                                                 |
| GUI                      | 玩家 `/pet` 菜单 + 管理员测试面板                                          | 玩家三菜单：商店 / Storage / Upgrade；无管理员 GUI                                                           | 玩家 `/pet gui` 选择 + `/pet data` 外观调整；管理靠 JSON 文件                                              |
| 主要命令                     | `/pet`、`/pet dismiss`、`/pet info`、`/pet reload`、`/pet admin`    | `/pets`、`/pets summon <id>`、`/pets dismiss`、`/pets admin give/addxp`                            | `/pet gui/summon/remove/rename/data/modify/addon/debug`、`/pet purchased`、`/pet regenerate`   |
| 权限体系                     | `farpets.use`、`farpets.admin`（仅公开两个；按组限物种靠 LuckPerms）           | `rzxpets.use/summon/dismiss/admin`                                                              | **按物种 / 细分到 fly/mount/hat/data/name** 全套节点，如 `pet.type.<mob>.*`                              |
| Vault 经济                 | **未查到 Vault 集成**                                                | **支持 Vault /zGems/ 物品货币**，商店定价                                                                  | **Vault Addon（spets-vault）**：每宠定价、一次性购买 / 每次召唤付费两种模式                                         |
| PlaceholderAPI           | 官方声明支持，未公布占位符清单                                                 | **必需依赖**                                                                                        | 支持，消息占位符 `{ownerName}/{petName}/{petType}...`，完整 `%simplepets_%` 表未公开                        |
| 多语言 / 中文                 | 配置键出现波兰语（`ogolne`、`pety-niesmiertelne`），`messages.yml` 可改；无中文文档 | 英文文档（2 个 md）                                                                                    | 英文 Wiki（[wiki.bsdevelopment.org](https://wiki.bsdevelopment.org)），无官方中文文档；`messages.yml` 可汉化 |

***

## 三、商务、授权与兼容性

| 维度                 | FarPets                               | RZXPets                                                        | SimplePets（R5）                                                              |
| ------------------ | ------------------------------------- | -------------------------------------------------------------- | --------------------------------------------------------------------------- |
| 价格                 | **完全免费**，无内购                          | 页面自称 premium，但 JAR 在 GitHub Releases **免费下**，无付费墙              | **\$12 USD**（voxel.shop）；旧 v4.4 免费版已 Outdated                               |
| 附加组件收费             | 无 addon 体系                            | 无                                                              | Vault/PlayerPoints/GemsEconomy/Treasury 等经济 addon **全免费**，`/pet addon` 游戏内装 |
| 开源 / License       | **未查到开源仓库**，SpigotMC 未给源码链接           | 源码公开于 GitHub，但 License 为**作者自写 All-Rights-Reserved**（禁商用、禁再分发） | **GPL-3.0-only**，仓库 brainsynder-Dev/SimplePets                              |
| 分发渠道               | 仅 SpigotMC #138745                    | SpigotMC #137426 + GitHub Releases                             | voxel.shop（\$12）+ Modrinth + Hangar + GitHub                                |
| 最低 MC 版本           | Paper/Spigot 1.21+                    | **1.20.4+**                                                    | 官方表：**最低 1.21.8**，列 1.21.11 / 1.21.10 / 1.21.8 / 26.1 / 26.2                |
| **1.21.11 官方点名**   | 未点名（"1.21+" 推断可跑，需自测）                 | 未点名（"1.20.4+" 推断可跑，需自测）                                        | **明确在支持列表内**                                                     |
| 服务端类型              | 仅声明 Paper/Spigot；Leaf/Purpur/Folia 未提 | Spigot/Paper/Bukkit；Folia 未声明（代码为标准 Bukkit 任务，未见 region 线程适配）  | 官方写 "Spigot 及其分支（Paper、Purpur）均可"；**Folia 不在列表**；Leaf 属 Paper 分支，按官方措辞可跑    |
| Java 版本            | 未明确（跑 1.21+ 实际需 Java 21）              | **Java 21**                                                    | **Java 21**                                                                 |
| ProtocolLib        | 不需要                                   | 不需要                                                            | **可选**（非必需）                                                                 |
| 硬依赖                | 无；可选 PAPI、LuckPerms                   | **RZXCore（作者私有框架，未公开发布）** + PAPI（必需）                           | 核心无硬依赖；Vault Addon 需 Vault ≥ 1.7.3                                          |
| Geyser / Floodgate | 未表态；宠物是原版实体，理论上基岩端可见                  | 未表态；原版实体理论可见，**均为推断**                                          | 未表态；原版实体 + 容器 GUI，Java 逻辑在 Geyser 后一般能跑，**GUI 基岩端交互无官方承诺**                  |
| 中文界面 / 文档          | 无独立 Wiki，Documentation 标签为死链          | 仅 2 个英文 md                                                     | 英文 Wiki 完整，无官方中文                                                            |

***

## 四、维护状态

| 维度      | FarPets                 | RZXPets                            | SimplePets（R5）                                              |
| ------- | ----------------------- | ---------------------------------- | ----------------------------------------------------------- |
| 首发时间    | 2026-09-19                | 2026-07-26 / 27                    | 2015 年起（v4 时代），R5 重写版持续在更                                   |
| 最新版本    | 1.0.2（紧急修复飞行骑乘 bug） | 1.0.1（2026-07-27）                  | **R5-B315**                                                     |
| 累计下载    | **14**                  | SpigotMC 24 + GitHub 27            | Modrinth \~24.9K；Spigot 旧 v4.4 历史 107.6K；Vault Addon \~3.6K |
| 评分 / 评论 | 0 分 / 0 评论              | 0 分 / 0 评论 / 0 论坛回复                | Spigot 旧 v4.4 历史 **4.1/5（约 217 条评价）**                       |
| GitHub  | 未检索到仓库                  | **0 star / 0 fork / 0 open issue** | 仓库持续活跃；历史 148 issue / 82 PR，**平均 issue 关闭时长约 6 个月**（响应偏慢）   |
| 社区反馈样本  | 无（新发布）                | 无（零讨论）                          | 旧 v4 遗留抱怨：跨世界卡住、与 Citizens 冲突、某次更新宠物全丢（均已停更版本）              |

***

## 五、各插件独立小节

### 5.1 FarPets（MavenPL）

**亮点**

* 完全免费、无内购（[SpigotMC #138745](https://www.spigotmc.org/resources/farpets.138745/)）。
* 50+ 物种，含被 "无害化" 的原版敌对生物；每宠独立等级 + XP，22 种主题被动加成（炽足兽抗火、海豚辅助钓鱼、狼近战增伤等）；70+ 粒子轨迹；自带管理员测试面板。
* 仅可选 PAPI + LuckPerms，无重依赖。

**风险**

* **1.0 阶段、仅 14 次下载、0 评分**，作者主语言波兰语，配置键 `ogolne` / `pety-niesmiertelne` 为波兰语；无 Wiki、Documentation 标签死链、PAPI 占位符清单未公开。
* **无 Vault**，与现有经济体系无法直接打通；无自定义模型 / MythicMobs；Leaf、Geyser、Folia 均未表态。
* 首发即热修两个 bug（飞行骑乘卡空、宠物无敌化默认开启），新版本稳定性未经社区验证。

### 5.2 RZXPets（Kurz / KurzIsRio）

**亮点**

* 功能定位清晰：**肩膀漂浮宠物 + 10 种随 1→100 级缩放的被动 buff**（药水效果、挖矿 / 打怪 zGems、吸血、双倍收获、范围治疗、冲锋攻击等），机制文件可 YAML 扩展。
* 经济三选一：Vault /zGems/ 物品货币；玩家三菜单（商店、仓库、升级）开箱即用；Java 21、1.20.4+，对 1.21.11 理论兼容。

**风险**

* **强依赖未公开发布的 RZXCore 私有框架**（`com.rzx:core:1.0.0-SNAPSHOT`），框架停更则插件不可用 —— 单点故障。
* 无骑乘、无宠物帽子、无跟随距离 / 坐停细分、无管理员 GUI；无自定义模型 / MythicMobs / 繁殖。
* 下载～24、GitHub 0 star / 0 issue / 0 fork，社区零反馈；License 是 All-Rights-Reserved（禁商用、禁再分发），商用服需自行评估。
* 作者自曝发布初期曾出现以下 bug（已修）：Bat 实体 `setAI(false)` 冻结、和平难度刷掉 Blaze 类宠物、区块卸载后宠物不重生、升级 GUI 被主菜单覆盖。

### 5.3 SimplePets（brainsynder / BSDevelopment，当前在运行的版本）

**亮点**

* 十年积累，**R5 官方版本表明确含 1.21.11**（最新构建 R5-B315，支持 1.21.8 \~ 26.2），Java 21，Paper/Purpur 分支直接跑。
* 90 种原版生物，**骑乘 / 飞行 / 顶头 / 多只同放 / Pet Saves 存盘 / 五种改名方式 / 每宠 JSON 微调外观**（幼年、羊毛色、项圈色、史莱姆尺寸、荧光、手持方块等）。
* 权限粒度细到 `pet.type.<mob>.{fly,mount,hat,data.*}`；Vault Addon 免费、支持一次性购买或按次召唤付费，对接已有的 Vault 主货币体系。
* GPL-3.0 开源，Modrinth \~24.9K 下载，历史版本累计 10 万 + 下载、4.1/5 口碑。

**短板**

* **\$12 付费**（v4 长期免费，R5 转 premium，社区有落差情绪）。
* **无内置等级 / 技能 / 成长 / 繁殖**，想做 RPG 养成得自己写 addon。
* **不支持** ModelEngine / ItemsAdder / Oraxen / Nexo / MythicMobs 自定义模型宠物。
* Folia 未官方支持；Geyser/Floodgate 无官方适配承诺（基岩端玩家 GUI 交互需实测）。
* 版本政策：1.21.6 / 1.21.7 等旧补丁需回退旧构建，1.21.x 不享受版本链接；GitHub issue 平均关闭时长约 6 个月。

***

## 六、针对南瓜生存服的结论与推荐

**结论：维持现状，继续使用 SimplePets（R5）+ Vault Addon，不建议更换 FarPets 或 RZXPets。**

理由：

1. **兼容性这一票只有 SimplePets 稳。** 目标版本是 Leaf 1.21.11（Paper 分支），三者中**只有 SimplePets 在官方版本表把 1.21.11 列了出来**；FarPets 只写 "1.21+"，RZXPets 写 "1.20.4+"，二者对 1.21.11 都是推断，需要自测成本。
2. **经济已经打通，换了要重做。** 现网已经跑着 SimplePets + Vault Addon，QuickMenu 里 `/pet gui` 直达也接好了；FarPets **完全没有 Vault 集成**，RZXPets 虽然接 Vault 但强依赖未公开的 RZXCore—— 切换都要重新接经济、重做菜单入口、重新配宠物定价与权限组。
3. **成熟度差距悬殊。** SimplePets 十年历史、Modrinth 24.9K 下载、历史版本 217 条 4.1 分评价；FarPets 是新发布的 v1.0.2（14 次下载、0 评论），RZXPets 发布 2 个月、24 次下载、GitHub 0 star / 0 issue。这两个插件目前没有经过任何第三方生产环境验证，直接上生存服等于当测试员。
4. **功能并不缺要的。** 诉求是 "外观奖励型装饰宠物 + 经济购买"，SimplePets 的骑乘 / 飞行 / 顶头 / 多宠同放 / 每宠定价已覆盖；FarPets 多出来的 "等级 + 22 种被动加成" 和 RZXPets 的 "100 级 buff" 属于 RPG 养成方向，而 QuickMenu 定位是主菜单里的休闲直达项，不是 RPG 养成线 —— 为这个未确认的方向换掉一个已经跑稳的插件不划算。
5. **三者共同的天花板，换谁都解决不了。** 它们**都不支持** ModelEngine / ItemsAdder / Oraxen / MythicMobs 自定义模型宠物，**都没有官方 Geyser/Floodgate 适配声明**（基岩端 GUI 交互都要实测），**都不官方支持 Folia**。也就是说，换 FarPets / RZXPets 不仅没拿到新东西，还丢掉了成熟度和经济链路。

**建议动作**

* 保持 SimplePets R5 + Vault Addon 现状，等官方 R5 构建随 1.21.x 小版本同步升级即可。
* 想观察新方向时，把 FarPets / RZXPets 挂在观察列表（SpigotMC 收藏即可），**3–6 个月后**再看下载量、issue 数、是否出 Geyser 兼容报告 —— 届时若 FarPets 的 "等级 + 被动加成" 玩法真的成熟且补了 Vault，再评估也不迟。
* 若未来真的想要 "养成型宠物"（等级 / 技能树 / 自定义模型），本次三选一里没有合适答案，应另行评估 **PetBlocks**（社区公认支持 Geyser/Folia/1.21.x）或 **AdvancedPets / MyPet** 这类成熟 RPG 向宠物插件。
* 基岩端玩家在 `/pet gui` 容器里的点击、改名弹窗（铁砧 / 对话 / 告示牌）在 Floodgate 下的表现，建议在测试服用基岩版账号实测一遍 —— 三家都没官方承诺，这条是要自己承担的验证项。

***

## 七、主要信息来源

* FarPets 资源页与更新日志：[https://www.spigotmc.org/resources/farpets.138745/](https://www.spigotmc.org/resources/farpets.138745/) ；[https://www.spigotmc.org/resources/farpets.138745/history/](https://www.spigotmc.org/resources/farpets.138745/history/) ；[https://www.spigotmc.org/resources/farpets.138745/update?update=655880](https://www.spigotmc.org/resources/farpets.138745/update?update=655880)

* RZXPets 资源页：[https://www.spigotmc.org/resources/rzxpets.137426/](https://www.spigotmc.org/resources/rzxpets.137426/) ；GitHub 仓库 [https://github.com/KurzIsRio/RZXPets](https://github.com/KurzIsRio/RZXPets) （README / DOCUMENTATION.md / walkthrough.md / LICENSE）

* SimplePets：Modrinth [https://modrinth.com/plugin/simplepets](https://modrinth.com/plugin/simplepets) ；Vault Addon [https://modrinth.com/plugin/spets-vault](https://modrinth.com/plugin/spets-vault) ；付费页 [https://voxel.shop/product/1952/simplepets](https://voxel.shop/product/1952/simplepets) ；Wiki [https://wiki.bsdevelopment.org/](https://wiki.bsdevelopment.org/) ；Hangar 版本 [https://hangar.papermc.io/BSDevelopment/SimplePets/versions](https://hangar.papermc.io/BSDevelopment/SimplePets/versions) ；GitHub [https://github.com/brainsynder-Dev/SimplePets](https://github.com/brainsynder-Dev/SimplePets)

* 社区口碑参考：Spigot v4.4 评论区 [https://www.spigotmc.org/resources/simplepets-outdated-read-description.14124/reviews](https://www.spigotmc.org/resources/simplepets-outdated-read-description.14124/reviews) ；issue 统计 [https://issues.ecosyste.ms/hosts/GitHub/repositories/brainsynder-Dev/SimplePets](https://issues.ecosyste.ms/hosts/GitHub/repositories/brainsynder-Dev/SimplePets)

> 注：下载量、star 等社区数据会随时间变化；商业价格与授权以 voxel.shop、SpigotMC、GitHub LICENSE 原文为准。
