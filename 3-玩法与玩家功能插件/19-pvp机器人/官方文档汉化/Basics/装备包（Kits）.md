# 装备包（Kits）

> 原文：[Kits](https://mods.icondice.org/vexbot/wiki/kits)

> 保存装备配置、装备包 ID、装备包药水和随机装备。

装备包是一份背包的存档快照：快捷栏、护甲、副手、附魔，一应俱全。从你自己的背包存一个，然后想发给多少台机器人就发多少台。

```
/vexbot createkit <kit>
/vexbot updatekit <kit>
/vexbot renamekit <kit> <new name>
/vexbot deletekit <kit>
/vexbot kiticon <kit>
/vexbot givekit <kit> <target>
/vexbot givekitall <kit>
```

## 你的末影箱也会被存入装备包

保存装备包时，也会一并保存你当时末影箱里的所有东西——哪怕装备包本身并没有末影箱。发放这个装备包时，同样的内容会放进目标的末影箱，而且会通过装备包的每一种途径传递：`givekit`、装备包 ID、装备包药水、带着装备包生成、队伍批量生成以及 API。在此功能之前保存的装备包不带末影数据，这意味着目标自己的末影箱会保持原样，而不是被清空。

## 装备包 ID

装备包 ID 是站点上已发布装备包的可分享代码，这样你就能直接使用别人的装备配置，而不必自己重新搭配。

> `getkitid` 只是读取一个装备包——它把代码交给你，不做任何改动。要把你自己的背包存进装备包，请用 `/vexbot updatekit <kit>`（新建则用 `createkit`）。在 2.1.54 之前，`getkitid` 会用你随身带着的东西覆盖装备包，如果你两手空空，就会把它清空。

```
/vexbot getkitid <kit>
/vexbot givekitid <target> <kitId>
/vexbot teamgivekitid <team> <kitId>
/vexbot givekitallid <kitId>
```

## 装备包药水

装备包药水在使用时会发放一份装备包。当 kitpotiondosplash 为 on 时，它会被扔出成喷溅药水，范围内的每个人都获得该装备包；为 off 时，它只是右键使用，只补充你自己。

## 随机装备

randomgear 是逐件拼凑出一套装备，而不是使用已保存的装备包——装备档次协调、带附魔，每次都不一样。randomgearconfig 决定它可以掷取哪些东西。

每一个分类默认都是开启的，除非你手动关闭：`allowleather` `allowcopper` `allowiron` `allowdiamond` `allownetherite` `allowcrystals` `allowanchors` `allowcarts` `allowmace` `allowtrident` `allowelytra` `allowbow` `allowcrossbow` `allowarrows` `allowwebs` `allowpearls` `allowtotems` `allowgapples` `allowegaps` `allowshields` `allowtools` `allowblocks` `allowwindcharges` `allowsnowballs` `allowbuckets` `allowenchants`。食物则用 `allowfood <food> <value>` 一项一项地开关。

```
/vexbot randomchestlootconfig list
/vexbot randomchestlootconfig allowarrows false
/vexbot randomchestlootconfig allowfood cooked_beef false
```

> `allowarrows` 是 2.2 新增的；而且在宝箱战利品中，熟牛肉现在会服从 `allowfood` 的设置——在此之前，不管怎样都会再加一份牛肉。此外，箭矢现在只有在真正掷出弓或弩时才会出现，所以宝箱里再也不会出现「为一把它根本没有的武器准备弹药」的情况。
