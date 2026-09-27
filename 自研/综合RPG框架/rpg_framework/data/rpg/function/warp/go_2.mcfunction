# ══ 传送到矿区 ══
execute unless data storage rpg:warps w2 run return run tellraw @s ["",{"text":"[传送] ","color":"gold"},{"text":"矿区传送点尚未设置","color":"red"}]
function rpg:warp/tp_mac with storage rpg:warps w2
title @s actionbar {"text":"已传送到 矿区","color":"aqua"}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 1
