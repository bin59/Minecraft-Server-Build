# ══ BOSS受击标记（@s = BOSS）：血量下降时标记周围参战玩家 ══
execute store result score #boss_cur rpg.data run data get entity @s Health
execute if score #boss_cur rpg.data < #boss_last_hp rpg.data run tag @a[distance=..64] add rpg.boss_fight
execute store result score #boss_last_hp rpg.data run data get entity @s Health
