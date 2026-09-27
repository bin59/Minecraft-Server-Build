# ══ 购买弓×1 —— 120金 ══
execute if score @s rpg.gold matches ..119 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要120金）","color":"red"}]
scoreboard players remove @s rpg.gold 120
give @s minecraft:bow 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：弓×1","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
