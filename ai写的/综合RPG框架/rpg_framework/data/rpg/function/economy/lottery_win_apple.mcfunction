# ══ 中奖：附魔金苹果 ══
give @s minecraft:enchanted_golden_apple 1
tellraw @s ["",{"text":"[抽奖] ","color":"light_purple"},{"text":"🍎 稀有大奖！","color":"gold","bold":true},{"text":" 获得 ","color":"gray"},{"text":"附魔金苹果×1","color":"gold","bold":true}]
tellraw @a ["",{"text":"[抽奖] ","color":"light_purple"},{"selector":"@s","color":"aqua"},{"text":" 抽中了稀有大奖：附魔金苹果！","color":"gold"}]
playsound minecraft:ui.toast.challenge_complete player @a ~ ~ ~ 0.7 1.8
