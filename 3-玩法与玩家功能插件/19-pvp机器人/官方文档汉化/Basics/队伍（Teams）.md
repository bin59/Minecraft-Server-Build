# 队伍（Teams）

> 原文：[Teams](https://mods.icondice.org/vexbot/wiki/teams)

> 给机器人分组、队伍设置界面、战斗指令、山羊角、同盟与阵型。

队伍是一组有名称的机器人，拥有每一项设置、难度参数和技巧的独立副本。队伍中的机器人会完全忽略全局设置。

```
/vexbot team create <team>
/vexbot team join <team> <targets>
/vexbot team leave <targets>
/vexbot team <team>                         # 队伍设置界面
/vexbot team <team> members
/vexbot team <team> settings <setting> <value>
/vexbot team <team> targets
/vexbot team <team> allies add <other team>
/vexbot team <team> massspawn <amount> <kit>
/vexbot team list
```

## 队伍设置界面

`/vexbot team <team>` 会打开一个界面，集中展示这支队伍的全部信息：分类、搜索框、「已为本队伍修改」标记以及 A-Z 列表。每一项设置、难度参数和技巧都在里面，并附有完整说明。`/vexbot team <team> settings search <word>` 会直接跳到匹配的设置项。

## 战斗指令

队伍可以作为一个整体作战。给它指定一个头目和指令，机器人就会照此行动：

```
/vexbot team <team> settings boss <bot>
/vexbot team <team> settings formation <shape> [<size>]
/vexbot team <team> settings protectboss on
/vexbot team <team> settings focusbosstarget on
/vexbot team <team> settings followboss on
/vexbot team <team> settings maintainformation on
/vexbot team <team> settings retreatwhenlow on
/vexbot team <team> settings orders show
```

- `boss` —— 队伍以其为领头的机器人；护卫守护它，弓箭手守在后排。
- `protectboss` —— 护卫会扑向任何攻击头目的人。
- `focusbosstarget` —— 所有人都攻击头目正在攻击的目标。
- `followboss` / `maintainformation` —— 跟随头目移动，并在移动过程中保持阵型。
- `retreatwhenlow` —— 受伤的机器人会后撤，而不是死在前线。

## 山羊角召唤

`/vexbot team <team> settings horn <player>` 会把一只山羊角绑定给某位玩家。吹响它，队伍就会跑到你身边集结；再吹一次，他们就解除戒备。`horn call` 可以用命令达到同样效果，`horn clear` 解除绑定。

## 生成一支小队

`/vexbot team <team> massspawn <amount> <kit> [x y z]` 会直接把穿着该装备包的机器人生成到队伍中。数量为 1 时只生成一台机器人。

## 同盟

两支结盟的队伍不会互相攻击，无论哪个方向。这在需要各自独立设置的 2v2 对战，以及任何有中立第三方参与的场合都很有用。用 `/vexbot team <team> allies add <other team>` 来设置。

## 阵型

跟随某人的队伍可以保持某种阵型——纵队、横队、多排、三角或方阵网格——每排人数可配置；也可以是方形、圆形、六边形或墙形，这些需要指定尺寸。装备鞘翅的机器人会脱离地面阵型。

```
/vexbot team <team> follow <player> square 6
/vexbot team <team> goto <x y z> circle 10
/vexbot team <team> goto <player> hexagon
```

- `square <n>` —— 一个 n×n 的方形，从外向内填充，所以即使机器人数量少于 n²，看起来仍是一个方形轮廓。
- `circle <n>` —— 一个直径为 n 的环，外环排满后溢出到内环。
- `hexagon` —— 围绕一个中心空位（留给队伍集结环绕的对象）的六边环。
- `wall <n>` —— 沿这条线排成 n 格宽的单列，超过 n 后折成第二列。

> 方形、圆形和六边形会对齐世界方向，而不是队伍走来时的朝向，并且到达后保持阵型。在 2.2.22 之前，`wall` 只是 `line` 的另一个名字。

## 把队伍派往某处

`/vexbot team <team> goto` 会让整支队伍以大致的阵型走向一组坐标或某位玩家。遇到实心方块挡路时，他们会掏出镐子挖掘：挖两格宽、两格高的隧道；目标偏在一侧时斜着挖；上下都能挖，直到真正能够到达一个活着的目标。

```
/vexbot team <team> goto <x y z|player> [formation] [perRow]
/vexbot team <team> goto <x y z> then attack <target>
/vexbot team <team> goto <x y z> then hold
/vexbot team <team> goto status
/vexbot team <team> goto stop
```

`then attack <target>` 会在他们到达的那一刻给他们一项任务；`then hold` 让他们原地待命。单台机器人的版本是 `/vexbot goto`——参见 Movement and navigation。

## 统一皮肤

`/vexbot team <team> skin <skin>` 会让队伍里每一台机器人都换上同一款皮肤。皮肤是 `vexbot_teamskins/` 文件夹中的 `<name>.png` 文件，该文件夹会在世界加载时自动创建。统一皮肤在死亡、重生和重启后仍然保留；`/vexbot team <team> skin stop` 会取消皮肤，`/vexbot team skin list` 列出已有的皮肤。

## 命名

`/vexbot team <team> names <pool>` 为一支队伍指定独立的名字池——参见 Modes and extras 中的 Name pools。队伍的名字池优先于全局名字池，所以一场对战的双方可以各自使用自己的命名。
