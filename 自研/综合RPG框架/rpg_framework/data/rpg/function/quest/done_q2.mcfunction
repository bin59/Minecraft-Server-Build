# ══ 交付任务2：收集铁锭 ══
execute if score @s rpg.q2s matches 0 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"你还没有接取「收集铁锭」","color":"red"}]
execute if score @s rpg.q2s matches 1 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「收集铁锭」尚未完成，继续加油！","color":"red"}]
# ---- 奖励开关：总开关 与 任务分项 任一关闭则暂不可交付（进度保留）----
execute unless score #rw_master rpg.data matches 1 run return run function rpg:quest/reward_off
execute unless score #rw_quest rpg.data matches 1 run return run function rpg:quest/reward_off
scoreboard players set @s rpg.q2s 0
scoreboard players add @s rpg.gold 150
scoreboard players add @s rpg.exp 80
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「收集铁锭」完成！奖励：","color":"green"},{"text":"+150金币 +80经验","color":"yellow","bold":true}]
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.8 1.5
