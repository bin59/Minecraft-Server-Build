# ══ 接受每日任务1：击杀15只怪物 ══
execute if score @s rpg.q4s matches 1.. run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"今日讨伐已接取、待交付或已完成","color":"red"}]
scoreboard players operation @s rpg.q4base = @s rpg.kills
scoreboard players set @s rpg.q4s 1
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"已接取每日任务「讨伐怪物」：击杀15只怪物","color":"green"}]
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.7 1.2
