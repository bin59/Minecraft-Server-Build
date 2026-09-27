# ══ 传送阵踩踏检测宏 ══
# s 字段是槽位号，直接写进玩家的 rpg.pnow，省掉 8 个分发函数。
# 检测半径 3 格刚好覆盖 5x5 阵台，站在阵台任意一格都算。
$execute in $(dim) positioned $(x) $(y) $(z) as @a[distance=..3,gamemode=!spectator] run scoreboard players set @s rpg.pnow $(s)
# 待机特效：让玩家远远就能看见这是一座活跃的传送阵
$execute in $(dim) positioned $(x) $(y) $(z) run particle minecraft:end_rod ~ ~0.6 ~ 1.3 0.3 1.3 0.01 6
