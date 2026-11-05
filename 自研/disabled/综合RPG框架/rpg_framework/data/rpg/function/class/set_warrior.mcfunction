# ══ 成为战士：攻击+3 生命+4 ══
scoreboard players set @s rpg.class 1
tag @s add rpg.c_warrior
attribute @s minecraft:attack_damage modifier add rpg:class_atk 3 add_value
attribute @s minecraft:max_health modifier add rpg:class_hp 4 add_value
effect give @s minecraft:instant_health 1 1 true
title @s title {"text":"战士","color":"red","bold":true}
title @s subtitle {"text":"近战之王，冲锋陷阵！","color":"gray"}
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"selector":"@s","color":"white","bold":true},{"text":" 选择了职业 ","color":"gray"},{"text":"战士","color":"red","bold":true}]
playsound minecraft:ui.toast.challenge_complete player @s ~ ~ ~ 1 1
