# ══ 购买附魔金苹果 —— 500金 ══
execute if score @s rpg.gold matches ..499 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要500金）","color":"red"}]
scoreboard players remove @s rpg.gold 500
give @s minecraft:enchanted_golden_apple 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：附魔金苹果","color":"green"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 1.2
