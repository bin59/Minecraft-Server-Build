# 矿区 BGM 循环（自动）
# ⚠️ 把 0 64 0 改成矿区实际中心坐标；若矿区在另一个世界，改 in minecraft:<世界名>
execute in minecraft:overworld positioned 0 64 0 run as @a[distance=..64] at @s run playsound bgm.mines music @s ~ ~ ~ 0.7 1.0
# 按曲目长度自动重播（秒×20）
schedule function bgm:mines_play 2400 replace
