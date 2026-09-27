# ══ 里程碑：10级 ══
scoreboard players add @s rpg.gold 500
give @s minecraft:diamond_sword 1
tellraw @a ["",{"text":"[里程碑] ","color":"gold","bold":true},{"selector":"@s","color":"aqua"},{"text":" 达成 10 级里程碑！","color":"gold"},{"text":" 奖励500金+钻石剑","color":"yellow"}]
advancement grant @s only rpg:level_10
