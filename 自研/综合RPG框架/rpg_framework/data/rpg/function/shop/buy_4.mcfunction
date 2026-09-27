# ══ 购买铁剑×1 —— 100金 ══
execute if score @s rpg.gold matches ..99 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要100金）","color":"red"}]
scoreboard players remove @s rpg.gold 100
give @s minecraft:iron_sword 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：铁剑×1","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
