# ══ 接受任务3：骷髅猎手 ══
execute if score @s rpg.q3s matches 1.. run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「骷髅猎手」已接取或待交付","color":"red"}]
scoreboard players operation @s rpg.q3base = @s rpg.q3k
scoreboard players set @s rpg.q3s 1
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"已接取「骷髅猎手」：击杀8只骷髅","color":"green"}]
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.7 1.2
