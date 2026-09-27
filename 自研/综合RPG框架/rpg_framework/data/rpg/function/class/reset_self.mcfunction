# ══ 重置自身职业（管理员操作用） ══
scoreboard players set @s rpg.class 0
tag @s remove rpg.c_warrior
tag @s remove rpg.c_archer
tag @s remove rpg.c_mage
tag @s remove rpg.c_priest
attribute @s minecraft:attack_damage modifier remove rpg:class_atk
attribute @s minecraft:max_health modifier remove rpg:class_hp
attribute @s minecraft:movement_speed modifier remove rpg:class_spd
tellraw @s ["",{"text":"[RPG] ","color":"gold"},{"text":"你的职业已被重置，可重新选择","color":"yellow"}]
