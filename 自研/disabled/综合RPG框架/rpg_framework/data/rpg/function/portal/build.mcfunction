# ══ 建造 5x5 传送阵结构（宏，坐标来自传送阵数据） ══
# 布局：黑石砖底盘 → 中心磁石 → 四正向海晶灯 → 四斜角哭泣黑曜石 → 四角末地烛
$execute in $(dim) positioned $(x) $(y) $(z) run fill ~-1 ~ ~-1 ~1 ~1 ~1 minecraft:air
$execute in $(dim) positioned $(x) $(y) $(z) run fill ~-2 ~-1 ~-2 ~2 ~-1 ~2 minecraft:polished_blackstone_bricks
$execute in $(dim) positioned $(x) $(y) $(z) run fill ~-1 ~-1 ~-1 ~1 ~-1 ~1 minecraft:chiseled_polished_blackstone
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~ ~-1 ~ minecraft:lodestone
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~2 ~-1 ~ minecraft:sea_lantern
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~-2 ~-1 ~ minecraft:sea_lantern
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~ ~-1 ~2 minecraft:sea_lantern
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~ ~-1 ~-2 minecraft:sea_lantern
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~2 ~-1 ~2 minecraft:crying_obsidian
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~-2 ~-1 ~2 minecraft:crying_obsidian
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~2 ~-1 ~-2 minecraft:crying_obsidian
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~-2 ~-1 ~-2 minecraft:crying_obsidian
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~2 ~ ~2 minecraft:end_rod
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~-2 ~ ~2 minecraft:end_rod
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~2 ~ ~-2 minecraft:end_rod
$execute in $(dim) positioned $(x) $(y) $(z) run setblock ~-2 ~ ~-2 minecraft:end_rod
$execute in $(dim) positioned $(x) $(y) $(z) run particle minecraft:flash ~ ~1 ~ 0 0 0 0 1
