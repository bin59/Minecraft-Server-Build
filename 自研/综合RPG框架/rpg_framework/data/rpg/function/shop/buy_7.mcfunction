# ══ 购买箭×32 —— 60金 ══
execute if score @s rpg.gold matches ..59 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要60金）","color":"red"}]
scoreboard players remove @s rpg.gold 60
give @s minecraft:arrow 32
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：箭×32","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
