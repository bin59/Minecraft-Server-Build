# ══ 精英挑战监控（每刻） ══
execute unless entity @e[tag=rpg.elite] run function rpg:mob/elite_clear
execute if entity @e[tag=rpg.elite] store result bossbar rpg:elite value if entity @e[tag=rpg.elite]
execute if entity @e[tag=rpg.elite] as @e[tag=rpg.elite] at @s run function rpg:mob/elite_mark
# 区域束缚：精英跑出竞技场范围会被拉回中心
execute if entity @e[tag=rpg.elite] run function rpg:mob/elite_leash with storage rpg:warps w3
