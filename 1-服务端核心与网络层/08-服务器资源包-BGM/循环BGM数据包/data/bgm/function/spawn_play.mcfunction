# 出生点 BGM 循环（自动）
# ⚠️ 把 0 64 0 改成出生点实际中心坐标
execute in minecraft:overworld positioned 0 64 0 run as @a[distance=..32] at @s run playsound bgm.spawn music @s ~ ~ ~ 0.8 1.0
# 按曲目长度自动重播（秒×20）
schedule function bgm:spawn_play 2400 replace
