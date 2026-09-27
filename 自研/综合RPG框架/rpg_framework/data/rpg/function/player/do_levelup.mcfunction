# ══ 执行升级 ══
scoreboard players operation @s rpg.exp -= @s rpg.exp_need
scoreboard players add @s rpg.level 1
function rpg:player/calc_need
# ---- 升级奖励（受 总开关 与「升级/里程碑」分项 控制）----
execute if score #rw_master rpg.data matches 1 if score #rw_level rpg.data matches 1 run function rpg:player/level_reward
effect give @s minecraft:regeneration 8 1 false
title @s title {"text":"升级！","color":"gold","bold":true}
execute if score #rw_master rpg.data matches 1 if score #rw_level rpg.data matches 1 run title @s subtitle ["",{"text":"当前等级 ","color":"yellow"},{"score":{"name":"@s","objective":"rpg.level"},"color":"aqua"},{"text":" · 奖励 50 金币","color":"yellow"}]
execute unless score #rw_master rpg.data matches 1 run title @s subtitle ["",{"text":"当前等级 ","color":"yellow"},{"score":{"name":"@s","objective":"rpg.level"},"color":"aqua"},{"text":" · 奖励已关闭","color":"dark_gray"}]
execute if score #rw_master rpg.data matches 1 unless score #rw_level rpg.data matches 1 run title @s subtitle ["",{"text":"当前等级 ","color":"yellow"},{"score":{"name":"@s","objective":"rpg.level"},"color":"aqua"},{"text":" · 奖励已关闭","color":"dark_gray"}]
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"selector":"@s","color":"white","bold":true},{"text":" 升到了 ","color":"gray"},{"score":{"name":"@s","objective":"rpg.level"},"color":"aqua","bold":true},{"text":" 级！","color":"gray"}]
playsound minecraft:ui.toast.challenge_complete player @a ~ ~ ~ 1 1.2
execute if score @s rpg.level matches 10.. run function rpg:player/apply_passives
function rpg:player/milestone
