# ══ 每日任务3进度检测（拾取5绿宝石） ══
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q6e
scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q6base
execute if score rpg.tmp_holder rpg.tmp matches 5.. run function rpg:quest/ready_q13
