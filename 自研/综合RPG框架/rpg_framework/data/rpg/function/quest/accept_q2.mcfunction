# ══ 接受任务2：收集铁锭 ══
execute if score @s rpg.q2s matches 1.. run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「收集铁锭」已接取或待交付","color":"red"}]
scoreboard players operation @s rpg.q2base = @s rpg.q2i
scoreboard players set @s rpg.q2s 1
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"已接取「收集铁锭」：拾取16个铁锭","color":"green"}]
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.7 1.2
