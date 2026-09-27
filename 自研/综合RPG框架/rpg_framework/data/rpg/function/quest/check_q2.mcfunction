# ══ 任务2进度检测 ══
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q2i
scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q2base
execute if score rpg.tmp_holder rpg.tmp matches 16.. run function rpg:quest/ready_q2
