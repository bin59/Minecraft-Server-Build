# ══ 传送到BOSS竞技场 ══
execute unless data storage rpg:warps w3 run return run tellraw @s ["",{"text":"[传送] ","color":"gold"},{"text":"BOSS竞技场传送点尚未设置","color":"red"}]
function rpg:warp/tp_mac with storage rpg:warps w3
title @s actionbar {"text":"已传送到 BOSS竞技场","color":"aqua"}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 1
