# ══ 按加成点数写入生命修饰符（1~10） ══
# 注：1.21.5+ 起修饰符语法为 modifier add <id> <值> <运算>
execute if score rpg.tmp_holder rpg.tmp matches 1 run attribute @s minecraft:max_health modifier add rpg:level_hp 1 add_value
execute if score rpg.tmp_holder rpg.tmp matches 2 run attribute @s minecraft:max_health modifier add rpg:level_hp 2 add_value
execute if score rpg.tmp_holder rpg.tmp matches 3 run attribute @s minecraft:max_health modifier add rpg:level_hp 3 add_value
execute if score rpg.tmp_holder rpg.tmp matches 4 run attribute @s minecraft:max_health modifier add rpg:level_hp 4 add_value
execute if score rpg.tmp_holder rpg.tmp matches 5 run attribute @s minecraft:max_health modifier add rpg:level_hp 5 add_value
execute if score rpg.tmp_holder rpg.tmp matches 6 run attribute @s minecraft:max_health modifier add rpg:level_hp 6 add_value
execute if score rpg.tmp_holder rpg.tmp matches 7 run attribute @s minecraft:max_health modifier add rpg:level_hp 7 add_value
execute if score rpg.tmp_holder rpg.tmp matches 8 run attribute @s minecraft:max_health modifier add rpg:level_hp 8 add_value
execute if score rpg.tmp_holder rpg.tmp matches 9 run attribute @s minecraft:max_health modifier add rpg:level_hp 9 add_value
execute if score rpg.tmp_holder rpg.tmp matches 10 run attribute @s minecraft:max_health modifier add rpg:level_hp 10 add_value
effect give @s minecraft:instant_health 1 0 true
