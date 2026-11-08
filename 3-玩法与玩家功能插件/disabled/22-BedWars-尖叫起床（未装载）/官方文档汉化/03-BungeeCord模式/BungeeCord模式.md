# BungeeCord 模式
ScreamingBedWars 官方文档简体中文翻译 — BungeeCord 模式
> 原文：[BungeeCord mode](https://docs.screamingsandals.org/BedWars/latest/bungee/)

> 翻译说明：本译文为官方文档的简体中文翻译，命令、权限节点、配置项/键名、占位符、物品/升级 ID、API 名称、版本号、URL 及代码块一律保留英文原文。

ScreamingBedWars 支持 BungeeCord 模式，可用于单竞技场服务器并实现竞技场自动加入。本文档介绍如何配置此模式。

> 注意：Velocity 支持
>
> 此模式同样兼容 Velocity。请确保代理端的 `velocity.toml` 文件中启用了 `bungee-plugin-message-channel`。

## 配置游戏服务器

要启用 BungeeCord 模式，请在 `config.yml` 中找到 `bungee` 段落，并将 `enabled` 设为 `true`。然后根据需要配置每个选项：

* `serverRestart` —— 决定游戏结束后服务器是否重启。这对插件来说不是必需的，请按需调整。要启用服务器重启，必须在 `spigot.yml` 中定义一个现有的启动脚本（`.sh` 或 `.bat`）。
* `serverStop` —— 与 `serverRestart` 类似，但仅关闭服务器。仅当你有独立的软件管理服务器自动启动时才使用此项。
* `server` —— 指定游戏结束后将玩家转移到的中心（hub）服务器。
* `auto-game-connect` —— 决定玩家加入服务器时是否自动加入 BedWars 游戏。通常应启用此项，除非由某个附属插件（addon）接管管理。
* `kick-when-proxy-too-slow` —— 如果代理未能将玩家转移到中心服务器，或转移过慢，服务器会将等待过久的玩家踢出。
* `random-game-selection` —— 当同一服务器上存在多个竞技场时，启用竞技场随机选择。将 `enabled` 设为 `true` 以使用此功能。`preselect-games` 选项可用于在任何玩家加入之前（服务器启动时或上一局游戏结束后）预知下一个被选中的游戏。
* `motd` —— 对使用 MOTD 文本的服务器选择插件很有用，例如 [BungeeSigns](https://www.spigotmc.org/resources/bungeesigns.6563/)。启用后，插件会根据当前状态和玩家数量更新 MOTD。共有五种状态：`waiting`、`waiting_full`、`running`、`rebuilding` 和 `disabled`。使用占位符 `%name%` 表示竞技场名称，`%current%` 表示当前玩家数，`%max%` 表示最大玩家数。消息可使用旧式颜色代码（`§<color code>`）着色。

配置段落大致如下：

```yaml
bungee:
  enabled: false
  serverRestart: true
  serverStop: false
  server: hub
  auto-game-connect: false
  kick-when-proxy-too-slow: true
  random-game-selection:
    enabled: true
    preselect-games: false
  motd:
    enabled: false
    waiting: '%name%: Waiting for players [%current%/%max%]'
    waiting_full: '%name%: Game is full [%current%/%max%]'
    running: '%name%: Game is running [%current%/%max%]'
    rebuilding: '%name%: Rebuilding...'
    disabled: '%name%: Game is disabled'
```

## 配置中心服务器

虽然 ScreamingBedWars 没有自带大厅插件，但可以使用任何服务器选择插件，例如 [BungeeSigns](https://www.spigotmc.org/resources/bungeesigns.6563/)。推荐使用能够读取 MOTD 的插件，因为它们可以将游戏状态中继到你的中心服务器。

如果你更喜欢使用基于物品栏的 GUI 而非告示牌，并希望显示服务器 MOTD 中的信息，可以使用任何兼容 [PlaceholderAPI](https://placeholderapi.com/) 的物品栏插件，例如 [DeluxeMenus](https://www.spigotmc.org/resources/deluxemenus.11734/)。推荐使用 [Pinger expansion](https://wiki.placeholderapi.com/users/placeholder-list/#pinger)，因为它提供了此目的所需的占位符。要使用 DeluxeMenus 创建服务器选择器，可参考[本指南](https://wiki.helpch.at/helpchat-plugins/deluxemenus/example-gui-menus#server-selector)并查看[此示例](https://github.com/HelpChat/DeluxeMenus-Wiki/blob/master/gui_menus/serverselector.yml)。

<!-- TODO: list possible options with little tutorials -->

> 提示：同步统计数据
>
> 对于 BungeeCord 网络，通常会将统计数据同步到数据库。按照[此处](config.md#database-connection)所述配置 `database` 段落。确保所有服务器连接到同一个数据库。
>
> 要在大厅中访问统计数据，最简单的方法是在中心服务器上也安装 ScreamingBedWars。请确保该实例不处于 BungeeCord 模式。或者，你也可以创建一个附属插件（addon）来获取统计数据。数据库结构详见[数据库配置段落](config.md#database-connection)末尾。
