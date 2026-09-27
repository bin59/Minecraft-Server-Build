# ══ 精英受击标记（@s = 精英）：血量下降时标记周围参战玩家 ══
execute store result score #elite_cur rpg.data run data get entity @s Health
execute if score #elite_cur rpg.data < #elite_last_hp rpg.data run tag @a[distance=..48] add rpg.elite_fight
execute store result score #elite_last_hp rpg.data run data get entity @s Health
