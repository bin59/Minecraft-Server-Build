# VexTrial — 试炼（the Trial）

> 原文：[VexTrial — the Trial](https://mods.icondice.org/vexbot/wiki/vextrial)

> 波次生存：一圈圈机器人逼近，一波接一波，装备随进程逐级升级。

独立附加组件：`/vexbot extensions pull vextrial`，然后重启服务器。它以前是 VexSim 的一部分，后来拆分出来成为独立附加组件，所以如果你在 Bounty Sim 里找 `/vexbot trial`，是找不到的。需要 VexBot 2.1.13 或更新版本。

## 玩法

`/vexbot trial start` 会把你站着的任意位置变成竞技场——不用建造、不用布置。一波波机器人以环形在你周围生成并逼近；清完一波，下一波大约五秒后到达。全部清完，试炼即完成。

- **装备逐级升级** —— 装备包沿 皮革 → 锁链 → 铁 → 钻石 → 下界合金 的阶梯爬升，横跨你选定的波次数；所以第一波是一群皮革杂鱼，最后一波是下界合金。每一阶都是手工配好的成套负载，不是随机生成的装备。
- **波次递增** —— 从你配置的数量开始，每两波增加一台机器人，同时在场上限 12 台。
- **登场方式** —— 早期波次以雷击形式在每个生成点降临；后期波次则带着缓降效果从天而降，以一声雷鸣宣告登场。

## 设置

```
/vexbot trial start [player]
/vexbot trial stop
/vexbot trial status
/vexbot trial reset                     # back to defaults
/vexbot trial config waves <1-15>       # default 5
/vexbot trial config difficulty <easy|medium|hard|expert|perfect>
/vexbot trial config count <n>          # bots in wave 1, default 3
/vexbot trial config radius <blocks>    # spawn ring, default 12
```

> 每一台试炼机器人都加入同一个承载难度的队伍，所以修改该队伍的设置会改变整波机器人。如果试炼看起来卡住了，`stop` 会清除该队伍的目标并移除残留机器人；而 `start` 会先清掉游离的名字记录，确保崩溃的试炼永远不会挡住下一场。
