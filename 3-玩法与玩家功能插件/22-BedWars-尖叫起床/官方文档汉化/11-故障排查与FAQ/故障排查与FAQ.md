# 故障排查与常见问题
ScreamingBedWars 官方文档简体中文翻译 — 故障排查与 FAQ
> 原文：[Troubleshooting and FAQ](https://docs.screamingsandals.org/BedWars/latest/troubleshooting/)

> 翻译说明：本译文为官方文档的简体中文翻译，命令、权限节点、配置项/键名、占位符、物品/升级 ID、API 名称、版本号、URL 及代码块一律保留英文原文。

## 非 OP 玩家无法使用告示牌

将告示牌移离原版出生点保护范围（例如，如果 server.properties 中把 spawn protection 设为 16，就离出生点 34 格远），或将出生点保护改为 0。非 OP 玩家在出生点保护范围内无法进行某些操作。

## 位置 1 与位置 2

这两个位置用于划定一个被视为竞技场的区域。（类似 WorldEdit 中的木斧选区）

参见 [竞技场](arena.md#setting-the-arena-positions)。

<img alt="竞技场边界" src="../assets/arena_bounds.png" width="400"/>

## 床无法被破坏

BedWars 允许你把任意方块设为目标方块，所以即使把它设成地板之类的方块，你也不会看到任何报错。请仔细检查，并确保在设置队伍目标方块时对准的是床头。

## 添加告示牌

放置一个告示牌，第一行写上 `[BedWars]` 或 `[BWGame]`（区分大小写），第二行写上你的竞技场名称。请确保告示牌不在原版出生点保护范围内。

## 自动为商店物品上色

使用 `applycolorbyteam` 属性，例如：

```yaml
- price: 1
  price-type: bronze
  properties:
    - name: "applycolorbyteam"
  stack:
    type: white_wool
    amount: 2
```

## 升级

参见[升级](upgrades.md)一文。

## 语言文件

参见[语言](language.md)一文。

## PlaceholderAPI 占位符

参见[占位符 API](placeholderapi.md)一文。

## 修改消息前缀

前缀既可以在语言文件中全局修改，也可以用以下命令按竞技场设置：`/bw admin <arena> customprefix &6My Awesome Prefix `。

## 编辑资源

参见[配置](config.md)一文。

## 商店损坏

请用 [yamlchecker](https://yamlchecker.com/) 确认你的商店 YAML 合法，并使用正确的[材料](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)和[格式](https://github.com/ScreamingSandals/SimpleInventories/wiki)。

## 指南针会传送玩家

这通常是因为你安装了 WorldEdit 或 FastAsyncWorldEdit。指南针被用于穿墙传送。不过它只对有权限或 OP 的玩家生效。你可以在 WorldEdit 配置中禁用该工具，或把它绑定到别的物品上。你也可以在 BedWars 配置中把队伍选择物品换成别的。

## 权限

参见[命令与权限](commands.md#permissions)页面。

## 添加开始与重生物品

可以按以下方式配置：

### 游戏开始

```yaml
game-start-items: true
gived-game-start-items:
- leather_helmet
- leather_boots
- leather_leggings
- leather_chestplate
- wooden_sword
```

### 重生

```yaml
player-respawn-items: true
gived-player-respawn-items: 
- leather_helmet
- leather_boots
- leather_leggings
- leather_chestplate
- wooden_sword
```

## 村民不生成

以下是排查此问题的一些要点：

* 确保生物生成（Mob Spawning）处于**开启**状态
* 你**没有**用 WorldGuard 保护竞技场
* 确保 NPC 处于**启用**状态

## 是否有视觉/粒子效果？

有的。请查看[此页面](config.md#game-effects)。

## 修改 Fireball 的伤害或爆炸威力

打开你的 config.yml，里面有 Fireball 的相关设置。
它位于 specials 节点下，并不难找 😛
基本上其他特殊物品也是同理。🙂

## 用 BungeeCord 搭建插件

插件支持单竞技场 bungeecord 模式。请查看[此页面](bungee.md)。

## PvP 无法生效

请确认：

* 在 MultiVerse（或类似的多世界插件）中已启用 PvP；若未启用，执行 `/mvm set pvp true <world name>`。
* WorldGuard 没有在竞技场所在世界禁止战斗（若禁止，执行 `/rg flag <region name> pvp allow`）；
* 已禁用 `spawn-protection`（在 server.properties 中设为 0）。

请注意，BedWars **并不干预 PvP**。如果 PvP 对你不生效，那是由配置错误或其他插件引起的。如果以上建议仍未解决，请在我们的 Discord 服务器上联系我们。

如果你安装了 WorldGuard，还可以用一条命令检查是哪个插件阻止了 PvP。命令是 `/wg debug testdamage -t <player_name>`，你需要另一名玩家来配合测试。点击[此处](https://worldguard.enginehub.org/en/latest/commands/#event-simulation)阅读关于该命令的更多说明。

## 玩家回血过快 / 剑伤害过低

这不是 BedWars 的问题，请调高服务器的游戏难度（例如从 easy 调到 normal）。

## 修改竞技场名称

首先，如果你把这里搞砸了，我们**不会提供任何支持**——**竞技场文件本就不应该被人工编辑**。

1. 进入 BedWars 文件夹，再进入 arenas 文件夹。路径应为 `plugins/BedWars/arenas`
2. 打开你想改名的文件
3. 第一个字段是 name，输入你的新名称。名称不能写成 `test arena` 这样带空格的形式，必须是单个字符串，即写成 `test-arena`。名称还必须唯一，即不能有两个同名竞技场。
4. 保存文件
5. 重启或重载服务器

## Class 版本错误

`SomeClass has been compiled by a more recent version of the Java Runtime (class file version 55.0), this version of the Java Runtime only recognizes class file versions up to 52.0 (unable to load class SomeClass)`

这表示你正在使用 Java 8（52.0），但插件至少需要 Java 11（55.0）。如果你想知道如何升级，请访问[此页面](https://docs.papermc.io/java-install-update)（它介绍的是升级到 Java 17，Java 17 可以运行 Java 11 的软件；升级到 Java 11 也可参考类似方法）。

目前 BedWars LATEST_VERSION_HERE 并不要求 Java 11，但其最知名的附加插件 SBA 至少需要 Java 11。新版 BedWars（0.3.0+）将至少要求 Java 11。

## 使用 no（挪威语）语言

是这样的，YAML 规范规定字面量 `no` 表示 `false`。要把 `no` 当作"挪威语"使用，需要将其强制转换为字符串：`locale: "no"`。

## 玩家死亡后被移出起床战争游戏，或重连后在竞技场内重生

你服务器上有某个插件在覆盖玩家的出生点，可能包括（但不限于）EssentialsSpawn、WorldGuard 等。

请移除这些插件，或在 bedwars 世界中禁用它们。
