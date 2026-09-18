# ══ 成为牧师：缓慢自动回血 ══
scoreboard players set @s rpg.class 4
tag @s add rpg.c_priest
title @s title {"text":"牧师","color":"yellow","bold":true}
title @s subtitle {"text":"圣光庇佑，生生不息！","color":"gray"}
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"selector":"@s","color":"white","bold":true},{"text":" 选择了职业 ","color":"gray"},{"text":"牧师","color":"yellow","bold":true}]
tellraw @s ["",{"text":"[RPG] ","color":"gold"},{"text":"被动：每5秒获得生命恢复效果","color":"yellow"}]
playsound minecraft:ui.toast.challenge_complete player @s ~ ~ ~ 1 1.6
