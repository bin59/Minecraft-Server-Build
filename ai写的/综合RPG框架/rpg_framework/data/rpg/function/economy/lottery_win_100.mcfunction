# ══ 中奖：100金 ══
scoreboard players add @s rpg.gold 100
tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"🎉 恭喜中奖！","color":"gold","bold":true},{"text":" 获得 ","color":"gray"},{"text":"+100金币","color":"gold","bold":true}]
playsound minecraft:block.note_block.pling player @s ~ ~ ~ 1 2
