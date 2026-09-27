# ══ 购买速度药水 —— 180金 ══
execute if score @s rpg.gold matches ..179 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要180金）","color":"red"}]
scoreboard players remove @s rpg.gold 180
give @s minecraft:potion[minecraft:potion_contents={potion:"minecraft:swiftness"}] 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：速度药水","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1
