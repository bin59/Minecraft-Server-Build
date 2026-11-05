# ══ 中奖：500金 ══
scoreboard players add @s rpg.gold 500
tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"💰 大奖！","color":"gold","bold":true},{"text":" 获得 ","color":"gray"},{"text":"+500金币","color":"gold","bold":true}]
tellraw @a ["",{"text":"[抽奖] ","color":"light_purple"},{"selector":"@s","color":"aqua"},{"text":" 抽中了500金大奖！","color":"gold"}]
playsound minecraft:ui.toast.challenge_complete player @a ~ ~ ~ 0.7 1.5
