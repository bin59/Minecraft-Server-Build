# ══ BOSS冷却提示（剩余时间换算为秒显示） ══
scoreboard players operation #show_sec rpg.tmp = #boss_cd rpg.data
scoreboard players operation #show_sec rpg.tmp /= #c20 rpg.data
tellraw @s ["",{"text":"[BOSS] ","color":"dark_red"},{"text":"深渊领主正在沉睡，冷却中：","color":"red"},{"score":{"name":"#show_sec","objective":"rpg.tmp"},"color":"yellow","bold":true},{"text":" 秒","color":"red"}]
