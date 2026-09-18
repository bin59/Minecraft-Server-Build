# ══ 等级里程碑奖励 ══
# ---- 奖励开关：总开关 与「升级/里程碑」分项 任一关闭则不发放 ----
execute unless score #rw_master rpg.data matches 1 run return 0
execute unless score #rw_level rpg.data matches 1 run return 0
execute if score @s rpg.level matches 5 run function rpg:player/milestone_5
execute if score @s rpg.level matches 10 run function rpg:player/milestone_10
execute if score @s rpg.level matches 20 run function rpg:player/milestone_20
execute if score @s rpg.level matches 30 run function rpg:player/milestone_30
execute if score @s rpg.level matches 50 run function rpg:player/milestone_50
