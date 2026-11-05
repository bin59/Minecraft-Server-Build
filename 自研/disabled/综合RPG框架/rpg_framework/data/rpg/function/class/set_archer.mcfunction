# ══ 成为弓箭手：移速+15% ══
scoreboard players set @s rpg.class 2
tag @s add rpg.c_archer
attribute @s minecraft:movement_speed modifier add rpg:class_spd 0.15 add_multiplied_total
effect give @s minecraft:speed 10 0 true
title @s title {"text":"弓箭手","color":"green","bold":true}
title @s subtitle {"text":"百步穿杨，箭雨封喉！","color":"gray"}
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"selector":"@s","color":"white","bold":true},{"text":" 选择了职业 ","color":"gray"},{"text":"弓箭手","color":"green","bold":true}]
playsound minecraft:ui.toast.challenge_complete player @s ~ ~ ~ 1 1.2
