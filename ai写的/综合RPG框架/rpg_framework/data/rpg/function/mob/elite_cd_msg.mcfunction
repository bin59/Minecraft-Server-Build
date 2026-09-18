# ══ 精英挑战冷却提示（剩余时间换算为秒显示） ══
scoreboard players operation #show_sec rpg.tmp = #elite_cd rpg.data
scoreboard players operation #show_sec rpg.tmp /= #c20 rpg.data
tellraw @s ["",{"text":"[精英] ","color":"dark_purple"},{"text":"精英挑战冷却中：","color":"dark_purple"},{"score":{"name":"#show_sec","objective":"rpg.tmp"},"color":"yellow","bold":true},{"text":" 秒","color":"dark_purple"}]
