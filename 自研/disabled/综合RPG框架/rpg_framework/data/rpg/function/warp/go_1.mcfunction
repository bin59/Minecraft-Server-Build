# ══ 传送到主城 ══
execute unless data storage rpg:warps w1 run return run tellraw @s ["",{"text":"[传送] ","color":"gold"},{"text":"主城传送点尚未设置","color":"red"}]
function rpg:warp/tp_mac with storage rpg:warps w1
title @s actionbar {"text":"已传送到 主城","color":"aqua"}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 1
