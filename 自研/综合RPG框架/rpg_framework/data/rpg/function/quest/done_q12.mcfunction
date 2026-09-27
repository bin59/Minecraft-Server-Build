# ══ 交付每日任务2 ══
execute if score @s rpg.q5s matches 0 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"你还没有接取「采石工人」","color":"red"}]
execute if score @s rpg.q5s matches 1 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"「采石工人」尚未完成，继续加油！","color":"red"}]
execute if score @s rpg.q5s matches 3 run return run tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"今日已领取过该奖励","color":"red"}]
# ---- 奖励开关：总开关 与 任务分项 任一关闭则暂不可交付（当日资格保留）----
execute unless score #rw_master rpg.data matches 1 run return run function rpg:quest/reward_off
execute unless score #rw_quest rpg.data matches 1 run return run function rpg:quest/reward_off
scoreboard players set @s rpg.q5s 3
scoreboard players add @s rpg.gold 300
scoreboard players add @s rpg.exp 100
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"每日任务「采石工人」完成！奖励：","color":"green"},{"text":"+300金币 +100经验","color":"yellow","bold":true}]
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.8 1.5
function rpg:quest/complete_common
