# 主城 BGM 循环（自动）
# 以主城中心坐标为圆心、半径 48 内的玩家听到 bgm.lobby
# ⚠️ 把下面的 0 64 0 改成主城实际中心坐标（XYZ）
execute in minecraft:overworld positioned 0 64 0 run as @a[distance=..48] at @s run playsound bgm.lobby music @s ~ ~ ~ 1.0 1.0
# 按曲目长度自动重播：2400 tick = 120 秒。按 lobby.ogg 实际长度（秒×20）改
schedule function bgm:lobby_play 2400 replace
