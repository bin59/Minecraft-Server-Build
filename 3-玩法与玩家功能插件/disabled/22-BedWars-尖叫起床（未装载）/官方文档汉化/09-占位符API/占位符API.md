# 占位符 API
ScreamingBedWars 官方文档简体中文翻译 — 占位符 API
> 原文：[Placeholder API](https://docs.screamingsandals.org/BedWars/latest/placeholderapi/)

> 翻译说明：本译文为官方文档的简体中文翻译，命令、权限节点、配置项/键名、占位符、物品/升级 ID、API 名称、版本号、URL 及代码块一律保留英文原文。

本插件向 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 注册占位符。

这些占位符供其他插件使用，若在 BedWars 配置中直接使用可能无法生效。但如果安装了 PlaceholderAPI，它们可以在 `shop.yml` 及其他商店相关文件中使用。请使用稍作修改的语法：`%papi.<placeholder_name>%`（例如 `%papi.bedwars_all_games_players%`）。

## 全局占位符

* `%bedwars_all_games_players%` - 返回所有游戏中的玩家总数。
* `%bedwars_all_games_maxplayers%` - 返回所有游戏的最大玩家总数。
* `%bedwars_all_games_anyrunning%` - 若任意游戏当前处于 `running` 或 `game_end_celebrating` 状态，返回 `true`；否则返回 `false`。
* `%bedwars_all_games_anywaiting%` - 若任意游戏当前处于 `waiting` 状态，返回 `true`；否则返回 `false`。

## 玩家当前信息

本节中的占位符与具体玩家相关，取决于其使用的上下文，通常是消息、全息投影等的查看者。

### 当前游戏信息

其中部分占位符包含参数 `<team_name>`。请将其替换为队伍名称。例如，若队伍名为 `red`，则使用 `%bedwars_current_game_team_red_bed%`。

* `%bedwars_current_game%` - 返回当前游戏的名称。
* `%bedwars_current_game_players%` - 返回游戏中的玩家数量。
* `%bedwars_current_game_minplayers%` - 返回游戏开始所需的最少玩家数。
* `%bedwars_current_game_maxplayers%` - 返回可加入游戏的最大玩家数。
* `%bedwars_current_game_world%` - 返回竞技场所在世界的名称。
* `%bedwars_current_game_state%` - 返回游戏的当前状态。可能的值为：`waiting`、`running`、`game_end_celebrating`、`rebuilding` 和 `disabled`。
* `%bedwars_current_game_time%` - 返回剩余时间（秒）。
* `%bedwars_current_game_timeformat%` - 返回格式化为 `MM:SS` 的剩余时间。
* `%bedwars_current_game_elapsedtime%` - 返回已用时间（秒）。
* `%bedwars_current_game_elapsedtimeformat%` - 返回格式化为 `MM:SS` 的已用时间。
* `%bedwars_current_game_team_<team_name>_colored%` - 返回带颜色的队伍名称。
* `%bedwars_current_game_team_<team_name>_color%` - 返回队伍颜色代码，格式为 `&<legacy color code>`。
* `%bedwars_current_game_team_<team_name>_ingame%` - 返回该队伍当前是否正在游戏中，以字符串形式表示：`yes` 或 `no`。
* `%bedwars_current_game_team_<team_name>_players%` - 返回该队伍的玩家数量。
* `%bedwars_current_game_team_<team_name>_maxplayers%` - 返回该队伍的最大玩家数。
* `%bedwars_current_game_team_<team_name>_bed%` - 返回该队伍当前是否拥有有效的目标方块，以字符串形式表示：`yes` 或 `no`。
* `%bedwars_current_game_team_<team_name>_bedsymbol%` - 返回 SBW 游戏内计分板使用的带颜色目标方块符号。
* `%bedwars_current_game_team_<team_name>_teamchests%` - 返回队伍宝箱的数量。
* `%bedwars_current_game_running%` - 若游戏当前处于 `running` 或 `game_end_celebrating` 状态，返回 `true`；否则返回 `false`。
* `%bedwars_current_game_waiting%` - 若游戏当前处于 `waiting` 状态，返回 `true`；否则返回 `false`。
* `%bedwars_current_available_teams%` - 返回已存在的队伍数量。
* `%bedwars_current_connected_teams%` - 返回当前正在游戏中的队伍数量。
* `%bedwars_current_teamchests%` - 返回所有队伍的队伍宝箱总数。

### 玩家所在队伍信息

* `%bedwars_current_team%` - 返回玩家所在队伍的名称。
* `%bedwars_current_team_color%` - 返回玩家所在队伍的颜色，格式为 `&<legacy color code>`。
* `%bedwars_current_team_colored%` - 返回带颜色的队伍名称。
* `%bedwars_current_team_players%` - 返回队伍中的玩家数量。
* `%bedwars_current_team_maxplayers%` - 返回队伍的最大玩家数。
* `%bedwars_current_team_bed%` - 返回该队伍当前是否拥有有效的目标方块，以字符串形式表示：`yes` 或 `no`。
* `%bedwars_current_team_teamchests%` - 返回队伍宝箱的数量。
* `%bedwars_current_team_bedsymbol%` - 返回 SBW 游戏内计分板使用的带颜色目标方块符号。

## 游戏信息占位符

使用这些占位符时，请将 `<game>` 替换为具体的游戏标识符，将 `<team_name>` 替换为队伍名称。例如，若游戏标识符为 `game1`、队伍名为 `red`，则使用 `%bedwars_game_game1_team_red_colored%`。

* `%bedwars_game_<game>_name%` - 返回游戏的名称。
* `%bedwars_game_<game>_players%` - 返回游戏中的玩家数量。
* `%bedwars_game_<game>_minplayers%` - 返回游戏开始所需的最少玩家数。
* `%bedwars_game_<game>_maxplayers%` - 返回可加入游戏的最大玩家数。
* `%bedwars_game_<game>_world%` - 返回竞技场所在世界的名称。
* `%bedwars_game_<game>_state%` - 返回游戏的当前状态。可能的值为：`waiting`、`running`、`game_end_celebrating`、`rebuilding` 和 `disabled`。
* `%bedwars_game_<game>_available_teams%` - 返回已存在的队伍数量。
* `%bedwars_game_<game>_connected_teams%` - 返回当前正在游戏中的队伍数量。
* `%bedwars_game_<game>_teamchests%` - 返回游戏中队伍宝箱的数量。
* `%bedwars_game_<game>_time%` - 返回剩余时间（秒）。
* `%bedwars_game_<game>_timeformat%` - 返回格式化为 `MM:SS` 的剩余时间。
* `%bedwars_game_<game>_elapsedtime%` - 返回已用时间（秒）。
* `%bedwars_game_<game>_elapsedtimeformat%` - 返回格式化为 `MM:SS` 的已用时间。
* `%bedwars_game_<game>_team_<team_name>_colored%` - 返回带颜色的队伍名称。
* `%bedwars_game_<game>_team_<team_name>_color%` - 返回队伍颜色代码，格式为 `&<legacy color code>`。
* `%bedwars_game_<game>_team_<team_name>_ingame%` - 返回该队伍当前是否正在游戏中，以字符串形式表示：`yes` 或 `no`。
* `%bedwars_game_<game>_team_<team_name>_players%` - 返回该队伍的玩家数量。
* `%bedwars_game_<game>_team_<team_name>_maxplayers%` - 返回该队伍的最大玩家数。
* `%bedwars_game_<game>_team_<team_name>_bed%` - 返回该队伍当前是否拥有有效的目标方块，以字符串形式表示：`yes` 或 `no`。
* `%bedwars_game_<game>_team_<team_name>_bedsymbol%` - 返回 SBW 游戏内计分板使用的带颜色目标方块符号。
* `%bedwars_game_<game>_team_<team_name>_teamchests%` - 返回队伍宝箱的数量。
* `%bedwars_game_<game>_running%` - 若游戏当前处于 `running` 或 `game_end_celebrating` 状态，返回 `true`；否则返回 `false`。
* `%bedwars_game_<game>_waiting%` - 若游戏当前处于 `waiting` 状态，返回 `true`；否则返回 `false`。

## 统计占位符

### 玩家统计占位符

> 提示
>
> 你可以使用占位符创建自定义排行榜，以多种方式展示玩家统计数据。为此，建议结合下方列出的占位符使用 [ajLeaderboards](https://www.spigotmc.org/resources/ajleaderboards.85548/) 插件。这能带来比 BedWars 插件本身所提供的更大灵活性。
>
> 需要设置帮助？请参阅 [ajLeaderboards 设置指南](https://wiki.ajg0702.us/ajLeaderboards/setup/) 获取详细说明。
>
> 例如，在使用以下命令添加一个基于击杀数的新榜单后：

```
/ajlb add %bedwars_stats_kills%
```

> 你就可以使用以下内容显示榜首玩家的名称和数值：

```
%ajlb_lb_bedwars_stats_kills_1_alltime_name%
%ajlb_lb_bedwars_stats_kills_1_alltime_value%
```

> 如果你想使用按总分排序的排行榜，可以改用[内置占位符](#score-leaderboard-placeholders)，而无需 ajLeaderboards。

本节中的占位符与具体玩家相关，取决于其使用的上下文，通常是消息、全息投影等的查看者。

* `%bedwars_stats_deaths%` - 返回死亡数。
* `%bedwars_stats_destroyed_beds%` - 返回被摧毁的床数。
* `%bedwars_stats_kills%` - 返回击杀数。
* `%bedwars_stats_loses%` - 返回失败数。
* `%bedwars_stats_score%` - 返回总分。
* `%bedwars_stats_wins%` - 返回胜利数。
* `%bedwars_stats_games%` - 返回总游戏场次。
* `%bedwars_stats_kd%` - 返回击杀/死亡比。

### 任意玩家统计占位符

使用这些占位符时，请将 `<player>` 替换为具体玩家的名称。例如，若玩家名为 `Misat11`，则使用 `%bedwars_otherstats_Misat11_deaths%`。

* `%bedwars_otherstats_<player>_deaths%` - 返回死亡数。
* `%bedwars_otherstats_<player>_destroyed_beds%` - 返回被摧毁的床数。
* `%bedwars_otherstats_<player>_kills%` - 返回击杀数。
* `%bedwars_otherstats_<player>_loses%` - 返回失败数。
* `%bedwars_otherstats_<player>_score%` - 返回总分。
* `%bedwars_otherstats_<player>_wins%` - 返回胜利数。
* `%bedwars_otherstats_<player>_games%` - 返回总游戏场次。
* `%bedwars_otherstats_<player>_kd%` - 返回击杀/死亡比。

### 分数排行榜占位符

这些占位符用于访问内置的基于分数的排行榜。将 `<position>` 替换为你想访问的排名位置数字。

例如，要获取第 3 名玩家的名称：
`%bedwars_leaderboard_score_3_name%`

* `%bedwars_leaderboard_score_<position>_name%` - 返回玩家的名称。
* `%bedwars_leaderboard_score_<position>_uuid%` - 返回玩家的 UUID。
* `%bedwars_leaderboard_score_<position>_deaths%` - 返回死亡数。
* `%bedwars_leaderboard_score_<position>_destroyed_beds%` - 返回被摧毁的床数。
* `%bedwars_leaderboard_score_<position>_kills%` - 返回击杀数。
* `%bedwars_leaderboard_score_<position>_loses%` - 返回失败数。
* `%bedwars_leaderboard_score_<position>_score%` - 返回总分。
* `%bedwars_leaderboard_score_<position>_wins%` - 返回胜利数。
* `%bedwars_leaderboard_score_<position>_games%` - 返回总游戏场次。
* `%bedwars_leaderboard_score_<position>_kd%` - 返回击杀/死亡比。
