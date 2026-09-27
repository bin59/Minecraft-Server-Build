# ══ 购买力量药水 —— 200金 ══
execute if score @s rpg.gold matches ..199 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要200金）","color":"red"}]
scoreboard players remove @s rpg.gold 200
give @s minecraft:potion[minecraft:potion_contents={potion:"minecraft:strength"}] 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：力量药水","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
