# ══ 里程碑：50级 ══
scoreboard players add @s rpg.gold 5000
give @s minecraft:netherite_sword[minecraft:enchantments={"minecraft:sharpness":5,"minecraft:fire_aspect":2}] 1
tellraw @a ["",{"text":"[里程碑] ","color":"gold","bold":true},{"selector":"@s","color":"aqua"},{"text":" 达成 50 级里程碑，成为传说！","color":"gold"},{"text":" 奖励5000金+附魔下界合金剑","color":"yellow"}]
advancement grant @s only rpg:level_50
