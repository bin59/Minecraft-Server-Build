# ══ 幸运抽奖（50金一次） ══
scoreboard players reset @s rpg.lottery
execute if score @s rpg.gold matches ..49 run return run tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"金币不足！需要50金币","color":"red"}]
# ---- 奖励开关：总开关 与 抽奖分项 任一关闭则抽奖暂停（不扣金币）----
execute unless score #rw_master rpg.data matches 1 run return run tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"奖励系统当前关闭，抽奖暂停。","color":"red"}]
execute unless score #rw_lottery rpg.data matches 1 run return run tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"抽奖功能已被管理员关闭。","color":"red"}]
scoreboard players remove @s rpg.gold 50
# 用 0-99 随机数决定奖项：0-49=100金 50-74=200金 75-84=500金 85-89=附魔金苹果 90-99=谢谢惠顾
execute store result score #roll rpg.data run random value 0..99
execute if score #roll rpg.data matches ..49 run function rpg:economy/lottery_win_100
execute if score #roll rpg.data matches 50..74 run function rpg:economy/lottery_win_200
execute if score #roll rpg.data matches 75..84 run function rpg:economy/lottery_win_500
execute if score #roll rpg.data matches 85..89 run function rpg:economy/lottery_win_apple
execute if score #roll rpg.data matches 90.. run function rpg:economy/lottery_lose
execute at @s run particle minecraft:firework ~ ~2 ~ 0.5 0.5 0.5 0.1 30
