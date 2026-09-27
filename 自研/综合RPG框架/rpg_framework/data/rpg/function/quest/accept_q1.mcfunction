# ══ 接受任务1：讨伐僵尸 ══
execute if score @s rpg.q1s matches 1.. run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「讨伐僵尸」已接取或待交付","color":"red"}]
scoreboard players operation @s rpg.q1base = @s rpg.q1z
scoreboard players set @s rpg.q1s 1
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"已接取「讨伐僵尸」：击杀10只僵尸","color":"green"}]
playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 0.7 1.2
