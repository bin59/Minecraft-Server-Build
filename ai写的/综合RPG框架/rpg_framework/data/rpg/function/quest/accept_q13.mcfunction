# ══ 接受每日任务3：拾取5个绿宝石 ══
execute if score @s rpg.q6s matches 1.. run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"今日寻宝已接取、待交付或已完成","color":"red"}]
scoreboard players operation @s rpg.q6base = @s rpg.q6e
scoreboard players set @s rpg.q6s 1
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"已接取每日任务「绿宝石猎人」：拾取5个绿宝石","color":"green"}]
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.7 1.2
