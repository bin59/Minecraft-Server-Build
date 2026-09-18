# ══ 等级被动属性（10级起每2级+1生命，上限+10） ══
attribute @s minecraft:max_health modifier remove rpg:level_hp
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.level
scoreboard players remove rpg.tmp_holder rpg.tmp 10
scoreboard players operation rpg.tmp_holder rpg.tmp /= #c2 rpg.data
execute if score rpg.tmp_holder rpg.tmp matches 11.. run scoreboard players set rpg.tmp_holder rpg.tmp 10
execute if score rpg.tmp_holder rpg.tmp matches 1.. run function rpg:player/set_hp_bonus
scoreboard players set rpg.tmp_holder rpg.tmp 0
