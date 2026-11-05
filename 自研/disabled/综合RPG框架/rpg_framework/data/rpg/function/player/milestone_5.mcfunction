# ══ 里程碑：5级 ══
scoreboard players add @s rpg.gold 200
give @s minecraft:golden_apple 2
tellraw @a ["",{"text":"[里程碑] ","color":"gold","bold":true},{"selector":"@s","color":"aqua"},{"text":" 达成 5 级里程碑！","color":"gold"},{"text":" 奖励200金+金苹果×2","color":"yellow"}]
advancement grant @s only rpg:level_5
