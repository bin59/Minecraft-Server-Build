# VexBot 3 新内容（What's new in VexBot 3）

> 原文：[What's new in VexBot 3](https://mods.icondice.org/vexbot/wiki/whats-new-3)

> 3.0 的亮点：Paper、实时统计、队伍战斗指令、专家级船战，以及更聪明的剑术。

VexBot 3 是迄今为止最大的一次更新：把从 2.2.25 到 2.2.101 的全部内容合并到一个版本里。完整列表见 changelog；下面是你最先会注意到的部分。

## 服务端与统计

- 除 Fabric 外，也能运行在 **Paper** 服务端（1.21.11、26.1.2、26.2）上 —— 参见 Paper。
- 一个公开的 **Live stats** 页面：当前在线的机器人和服务端、股票走势风格的图表、击杀、死亡、伤害、热门命令和机器人名 —— 参见 Live stats。
- 每个扩展都升级到 V3，每个游戏版本一个 jar，由 `/vexbot extensions pull` 为你挑选。
- 机器人数量多时，服务端 tick 开销降低 17-20%（`botadvancements`、`botspawnmobs`）。

## 队伍

- 一条 `/vexbot team` 命令搞定一切：`/vexbot team <name>` 打开一个可搜索的设置界面，里面包含该队伍的每一项设置、难度参数和技巧。
- 战斗指令：一名首领、阵型、保护首领的护卫、殿后的弓箭手、血量低时撤退 —— 参见 Teams。
- 山羊角召唤：吹一声绑定好的角，整队就会跑到你身边列队。
- `/vexbot team <name> massspawn <amount> <kit>` 直接往队伍里生成一个小队。

## 战斗

- 格挡反击与惩罚暴击：你打它们时它们会立刻打回来，并在一次轻击之后暴击你。
- 专家级船战 PvP：`/vexbot settings cartdoctrine guide|relentless`、按距离缩放的瞬发开船、两连弩连招，以及在最后一个图腾之后副手开船。
- 更聪明的治疗：机器人会用珍珠、蜘蛛网或击退一击来争取时间。
- 当有人准备用重锤落到机器人身上时，主手会切换出一个图腾（`macetotem`）。
- `/vexbot prefer totem|shield|auto` 选择机器人副手手持的东西。
- 轨道打击炮和 Orbital+ 鱼竿（`orbitalstrike`）。

## 移动

- 机器人会正经游泳、坐船过河、绕开墙壁走路，并且绝不会走进蜘蛛网或岩浆里。
- 移动时也能放方块（`placeblocksmoving`），而且 `goto` 会穿过湖泊和河流。

## 变更的命令

| 3.0 之前 | 现在 |
| --- | --- |
| /vexbot team create <team> | /vexbot team create <team> |
| /vexbot team join <team> <targets> | /vexbot team join <team> <targets> |
| /vexbot team <team> settings <setting> <value> | /vexbot team <team> settings <setting> <value> |
| /vexbot teamtargets / teamdifficulty / teamfollow / teamgoto <team> … | /vexbot team <team> targets / difficulty / follow / goto … |
| /vexbot teammass_spawn <team> <amount> <kit> | /vexbot team <team> massspawn <amount> <kit> |
| /vexbot teamalliance <team> add <other> | /vexbot team <team> allies add <other> |
| /vexbot settings <technique> on | /vexbot settings <technique> on |
| /vexbot view inventory <name> | /vexbot view inventory <name> |

## 自 3.0.0 起

- 3.0.1 与 3.0.2：Live stats 页面只统计由玩家或控制台输入的命令，并且只统计真正存在于世界中的机器人。
- 3.0.3：修复了 26.2 的报错 "Class net.minecraft.world.item.Items does not have member field ... STAINED_GLASS_PANE"。
- 3.0.4：机器人开船时的准度更好，山羊角召唤回来了（之前被误删了）。
