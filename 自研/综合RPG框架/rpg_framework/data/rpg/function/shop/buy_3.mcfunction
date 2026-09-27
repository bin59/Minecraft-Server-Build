# ══ 购买铁锭×8 —— 80金 ══
execute if score @s rpg.gold matches ..79 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要80金）","color":"red"}]
scoreboard players remove @s rpg.gold 80
give @s minecraft:iron_ingot 8
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：铁锭×8","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
