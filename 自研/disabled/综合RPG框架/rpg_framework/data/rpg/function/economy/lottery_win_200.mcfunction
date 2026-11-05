# ══ 中奖：200金 ══
scoreboard players add @s rpg.gold 200
tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"🎉 手气不错！","color":"gold","bold":true},{"text":" 获得 ","color":"gray"},{"text":"+200金币","color":"gold","bold":true}]
playsound minecraft:block.note_block.pling player @s ~ ~ ~ 1 2
