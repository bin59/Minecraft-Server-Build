# ══ 里程碑：30级 ══
scoreboard players add @s rpg.gold 2000
give @s minecraft:netherite_ingot 2
tellraw @a ["",{"text":"[里程碑] ","color":"gold","bold":true},{"selector":"@s","color":"aqua"},{"text":" 达成 30 级里程碑！","color":"gold"},{"text":" 奖励2000金+下界合金锭×2","color":"yellow"}]
advancement grant @s only rpg:level_30
