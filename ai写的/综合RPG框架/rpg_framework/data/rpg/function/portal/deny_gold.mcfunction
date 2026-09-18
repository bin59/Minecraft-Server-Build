# ══ 拒绝：金币不足 ══
scoreboard players set @s rpg.pcharge 0
scoreboard players set @s rpg.pcd 8
title @s actionbar ["",{"text":"[传送阵] ","color":"red","bold":true},{"text":"金币不足，本次传送需要 ","color":"red"},{"score":{"name":"#pcost","objective":"rpg.data"},"color":"gold","bold":true},{"text":" 金币","color":"red"}]
playsound minecraft:block.note_block.bass player @s ~ ~ ~ 1 0.6
