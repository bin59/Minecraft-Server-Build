# ══ 每日任务2进度检测（开采20石头） ══
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q5m
scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q5base
execute if score rpg.tmp_holder rpg.tmp matches 20.. run function rpg:quest/ready_q12
