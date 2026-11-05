# ══ BOSS监控（每刻执行，仅在BOSS存活时） ══
execute unless entity @e[type=minecraft:zombie,tag=rpg.boss] run function rpg:mob/boss_death
execute if entity @e[type=minecraft:zombie,tag=rpg.boss] as @e[type=minecraft:zombie,tag=rpg.boss,limit=1] run function rpg:mob/boss_bar
execute if entity @e[type=minecraft:zombie,tag=rpg.boss] as @e[type=minecraft:zombie,tag=rpg.boss,limit=1] at @s run function rpg:mob/boss_mark
# 区域束缚：BOSS跑出竞技场范围会被拉回中心
execute if entity @e[type=minecraft:zombie,tag=rpg.boss] run function rpg:mob/boss_leash with storage rpg:warps w3
# 防止BOSS溺水卡住：若在水下给予漂浮
execute as @e[type=minecraft:zombie,tag=rpg.boss] at @s if block ~ ~ ~ minecraft:water run effect give @s minecraft:levitation 1 4 true
