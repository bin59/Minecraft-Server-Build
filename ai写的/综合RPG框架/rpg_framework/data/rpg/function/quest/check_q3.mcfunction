# ══ 任务3进度检测 ══
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q3k
scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q3base
execute if score rpg.tmp_holder rpg.tmp matches 8.. run function rpg:quest/ready_q3
