# ══ 任务1进度检测 ══
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q1z
scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q1base
execute if score rpg.tmp_holder rpg.tmp matches 10.. run function rpg:quest/ready_q1
