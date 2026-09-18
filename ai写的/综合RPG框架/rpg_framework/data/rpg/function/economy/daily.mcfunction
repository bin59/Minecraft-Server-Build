# ══ 每日签到 ══
scoreboard players reset @s rpg.daily
execute if score @s rpg.day_claim = #day rpg.data run return run tellraw @s ["",{"text":"[签到] ","color":"gold"},{"text":"今天已经签到过了，明天再来吧","color":"red"}]
# ---- 奖励开关：总开关 与 签到分项 任一关闭则暂不可签到（当日资格保留）----
execute unless score #rw_master rpg.data matches 1 run return run tellraw @s ["",{"text":"[签到] ","color":"gold"},{"text":"奖励系统当前关闭，暂时无法签到。","color":"red"},{"text":"（今日签到资格保留，重新开启后仍可领取）","color":"dark_gray"}]
execute unless score #rw_daily rpg.data matches 1 run return run tellraw @s ["",{"text":"[签到] ","color":"gold"},{"text":"签到奖励已被管理员单独关闭。","color":"red"}]
scoreboard players operation @s rpg.day_claim = #day rpg.data
scoreboard players add @s rpg.gold 100
scoreboard players add @s rpg.exp 20
tellraw @s ["",{"text":"[签到] ","color":"gold"},{"text":"签到成功！奖励 ","color":"green"},{"text":"+100金币 +20经验","color":"yellow","bold":true}]
playsound minecraft:block.amethyst_block.chime player @s ~ ~ ~ 1 1.3
