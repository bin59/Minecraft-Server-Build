# ══ 每日任务1进度检测（击杀15只怪物） ══
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.kills
scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q4base
execute if score rpg.tmp_holder rpg.tmp matches 15.. run function rpg:quest/ready_q11
