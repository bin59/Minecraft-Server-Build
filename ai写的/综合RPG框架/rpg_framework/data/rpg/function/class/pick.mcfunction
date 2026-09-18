# ══ 职业选择分发 ══
execute if score @s rpg.class matches 1.. run function rpg:class/already
execute if score @s rpg.class matches 0 if score @s rpg.pick matches 1 run function rpg:class/set_warrior
execute if score @s rpg.class matches 0 if score @s rpg.pick matches 2 run function rpg:class/set_archer
execute if score @s rpg.class matches 0 if score @s rpg.pick matches 3 run function rpg:class/set_mage
execute if score @s rpg.class matches 0 if score @s rpg.pick matches 4 run function rpg:class/set_priest
execute if score @s rpg.class matches 1.. run advancement grant @s only rpg:first_class
scoreboard players reset @s rpg.pick
