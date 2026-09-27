# ══ 接受每日任务2：开采20个石头 ══
execute if score @s rpg.q5s matches 1.. run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"今日采石已接取、待交付或已完成","color":"red"}]
scoreboard players operation @s rpg.q5base = @s rpg.q5m
scoreboard players set @s rpg.q5s 1
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"已接取每日任务「采石工人」：开采20个石头","color":"green"}]
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.7 1.2
