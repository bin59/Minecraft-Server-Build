# VexSim — 悬赏模拟 V3（Bounty Sim V3）

> 原文：[VexSim — Bounty Sim V3](https://mods.icondice.org/vexbot/wiki/vexsim)

> 契约、操作员 NPC、绿宝石悬赏与 AI 猎人。

独立附加组件：`/vexbot extensions pull bounty-sim`，然后重启服务器。它添加的一切都在 `/vexbot bounty` 之下，所以如果这些命令不存在，就说明它没有安装。V3 构建（3.0.0）每个 Minecraft 版本对应一个 jar。

## 契约

操作员（operator）是一台被改造为扎根原地、无敌、不攻击的任务发放者——它永远不走动、永远不战斗、永远不受伤害。用 `/vexbot bounty operator spawn <name>` 生成一台，或者用 `operator add <name>` 把你已有的一台机器人提拔为操作员。右键它打开契约板：一份契约会指定一个 120–260 格外的目标，并递给你一张写着目标信息的纸。赶到那里，杀掉它指定的目标，再走回来——你一站到它旁边它就自动发放奖金，不需要再右键。

## 奖金发放

一个战利品潜影盒，装着一整套完整护甲等级的装备——皮革、铜、铁、钻石或下界合金——包含对应护甲、一把配套的剑、金苹果、熟牛肉和绿宝石。等级越高，两样东西给得越多。用 `/vexbot bounty config reward <random|colour>` 设置颜色。

## 绿宝石悬赏与 WANTED

`/vexbot bounty place <target> <amount>` 把绿宝石挂到某人头上——玩家要从自己的背包里掏钱（每次挂赏 1–1024，赏金池上限 4096），控制台和命令方块挂赏免费。谁杀掉目标，谁就拿走赏金池。

## V3 中的 WANTED

一名真实玩家如果连续杀掉三名真实玩家，就会变成 WANTED（通缉）：服务器会公告此事，AI 猎人会找上门来。杀机器人不再会让任何人成为 WANTED（可用 `/vexbot bounty config wanted off|players|all` 改回）。一次悬赏会派出三轮猎人，被杀掉的猎人不会复活，WANTED 状态 10 分钟后自动解除。`/vexbot bounty config enabled off` 会彻底关闭悬赏；契约仍然可用。

在战斗中途执行 `/vexbot bounty reset` 不会再立刻把猎人刷回来；你死亡或下线后，契约通缉犯也会停止追击你。

## 悬赏装备包

悬赏机器人穿的是按护甲等级挑选的、真实保存下来的装备包，而不是随机生成的装备；发放奖金的潜影盒里给的，也是那台悬赏机器人原本穿着的同一套装备包。放进你自己的 `config/vexsim-bounty-kits.txt` 即可替换全部装备包，无需重新构建。

## 如果它卡住了

`/vexbot bounty reset` 就是解卡按钮：以操作员身份执行时，它会清除所有契约、移除所有通缉犯和猎人（包括因崩溃而滞留的那些）、清空契约板并忘掉连续击杀记录。普通玩家执行它只会清除自己的契约。操作员、军衔和设置会保留——`reset operators` 和 `reset ranks` 分别单独清除它们。

```
/vexbot bounty operator spawn <name>
/vexbot bounty place <target> <amount>
/vexbot bounty list
/vexbot bounty hunters <0-5>
/vexbot bounty config targets <bots|players|both>
/vexbot bounty config reward <random|colour>
/vexbot bounty reset
/vexbot bounty config wanted <off|players|all>
/vexbot bounty config enabled <on|off>
```
