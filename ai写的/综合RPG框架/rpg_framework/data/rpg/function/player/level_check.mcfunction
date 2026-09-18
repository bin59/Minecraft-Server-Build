# ══ 升级检测（递归调用，直到经验不够） ══
execute if score @s rpg.level matches ..99 if score @s rpg.exp >= @s rpg.exp_need run function rpg:player/do_levelup
