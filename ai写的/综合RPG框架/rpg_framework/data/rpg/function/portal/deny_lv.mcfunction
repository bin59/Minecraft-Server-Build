# ══ 拒绝：等级不足（设一段冷却防止提示刷屏） ══
scoreboard players set @s rpg.pcharge 0
scoreboard players set @s rpg.pcd 8
title @s actionbar ["",{"text":"[传送阵] ","color":"red","bold":true},{"text":"等级不足，需要 ","color":"red"},{"score":{"name":"#plv","objective":"rpg.data"},"color":"yellow","bold":true},{"text":" 级","color":"red"}]
playsound minecraft:block.note_block.bass player @s ~ ~ ~ 1 0.6
