# ══ 购买面包×8 —— 30金 ══
execute if score @s rpg.gold matches ..29 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要30金）","color":"red"}]
scoreboard players remove @s rpg.gold 30
give @s minecraft:bread 8
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：面包×8","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
