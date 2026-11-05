# ══ 技能释放入口（右键技能魔杖触发，每刻执行） ══
scoreboard players reset @s rpg.stick
execute if score @s rpg.class matches 0 run tellraw @s ["",{"text":"[RPG] ","color":"gold"},{"text":"你还没有职业，先输入 ","color":"gray"},{"text":"/trigger rpg.cls","color":"aqua"},{"text":" 选择","color":"gray"}]
execute if score @s rpg.class matches 1.. run function rpg:skill/cast
