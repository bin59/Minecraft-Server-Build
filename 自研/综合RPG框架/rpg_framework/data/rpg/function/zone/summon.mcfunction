# ══ 按区域配置的怪物池生成一只怪 ══
execute if score #zpool rpg.data matches 1 run function rpg:zone/pool_1
execute if score #zpool rpg.data matches 2 run function rpg:zone/pool_2
execute if score #zpool rpg.data matches 3 run function rpg:zone/pool_3
execute if score #zpool rpg.data matches 4 run function rpg:zone/pool_4
scoreboard players add #zcount rpg.data 1
particle minecraft:large_smoke ~ ~0.5 ~ 0.3 0.4 0.3 0.02 12
particle minecraft:soul ~ ~0.8 ~ 0.2 0.3 0.2 0.01 6
