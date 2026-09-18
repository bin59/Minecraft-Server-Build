# ══ 传送阵充能（@s = 站在阵上的玩家） ══
# 刚落地的玩家处于冷却中，不累积充能，避免在目标阵上被立刻弹走
execute if score @s rpg.pcd matches 1.. run return 0
scoreboard players add @s rpg.pcharge 1
particle minecraft:portal ~ ~0.3 ~ 0.5 0.7 0.5 0.4 30
playsound minecraft:block.beacon.ambient player @s ~ ~ ~ 0.4 1.8
title @s actionbar ["",{"text":"传送阵充能中 ","color":"aqua"},{"score":{"name":"@s","objective":"rpg.pcharge"},"color":"yellow","bold":true},{"text":" / ","color":"gray"},{"score":{"name":"#portal_charge","objective":"rpg.data"},"color":"yellow","bold":true},{"text":"   离开阵台可取消","color":"dark_gray"}]
execute if score @s rpg.pcharge >= #portal_charge rpg.data run function rpg:portal/fire
