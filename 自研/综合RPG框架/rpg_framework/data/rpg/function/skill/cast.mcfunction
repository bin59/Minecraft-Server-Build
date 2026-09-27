# ══ 技能释放分发（含冷却判断） ══
execute if score @s rpg.cd matches 1.. run title @s actionbar ["",{"text":"技能冷却中：","color":"red"},{"score":{"name":"@s","objective":"rpg.cd"},"color":"red"},{"text":" tick","color":"red"}]
execute if score @s rpg.cd matches ..0 if score @s rpg.class matches 1 run function rpg:skill/warrior
execute if score @s rpg.cd matches ..0 if score @s rpg.class matches 2 run function rpg:skill/archer
execute if score @s rpg.cd matches ..0 if score @s rpg.class matches 3 run function rpg:skill/mage
execute if score @s rpg.cd matches ..0 if score @s rpg.class matches 4 run function rpg:skill/priest
execute if score @s rpg.cd matches 1.. run advancement grant @s only rpg:first_skill
scoreboard players reset @s rpg.skill
