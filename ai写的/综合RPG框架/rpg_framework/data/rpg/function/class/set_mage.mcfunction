# ══ 成为法师：技能冷却-30% ══
scoreboard players set @s rpg.class 3
tag @s add rpg.c_mage
title @s title {"text":"法师","color":"light_purple","bold":true}
title @s subtitle {"text":"掌控元素，焚尽一切！","color":"gray"}
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"selector":"@s","color":"white","bold":true},{"text":" 选择了职业 ","color":"gray"},{"text":"法师","color":"light_purple","bold":true}]
tellraw @s ["",{"text":"[RPG] ","color":"gold"},{"text":"被动：你的技能冷却减少30%","color":"light_purple"}]
playsound minecraft:ui.toast.challenge_complete player @s ~ ~ ~ 1 1.4
