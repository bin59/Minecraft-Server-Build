# ══ 购买铁甲套装 —— 350金 ══
execute if score @s rpg.gold matches ..349 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要350金）","color":"red"}]
scoreboard players remove @s rpg.gold 350
give @s minecraft:iron_helmet 1
give @s minecraft:iron_chestplate 1
give @s minecraft:iron_leggings 1
give @s minecraft:iron_boots 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：铁甲套装（头盔/胸甲/护腿/靴子）","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
