# ══ 交付任务3：骷髅猎手 ══
execute if score @s rpg.q3s matches 0 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"你还没有接取「骷髅猎手」","color":"red"}]
execute if score @s rpg.q3s matches 1 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「骷髅猎手」尚未完成，继续加油！","color":"red"}]
# ---- 奖励开关：总开关 与 任务分项 任一关闭则暂不可交付（进度保留）----
execute unless score #rw_master rpg.data matches 1 run return run function rpg:quest/reward_off
execute unless score #rw_quest rpg.data matches 1 run return run function rpg:quest/reward_off
scoreboard players set @s rpg.q3s 0
scoreboard players add @s rpg.gold 250
scoreboard players add @s rpg.exp 200
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「骷髅猎手」完成！奖励：","color":"green"},{"text":"+250金币 +200经验","color":"yellow","bold":true}]
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.8 1.5
