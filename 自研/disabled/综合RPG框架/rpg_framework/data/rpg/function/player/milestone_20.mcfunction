# ══ 里程碑：20级 ══
scoreboard players add @s rpg.gold 1000
give @s minecraft:enchanted_golden_apple 1
give @s minecraft:diamond 8
tellraw @a ["",{"text":"[里程碑] ","color":"gold","bold":true},{"selector":"@s","color":"aqua"},{"text":" 达成 20 级里程碑！","color":"gold"},{"text":" 奖励1000金+附魔金苹果+钻石×8","color":"yellow"}]
advancement grant @s only rpg:level_20
