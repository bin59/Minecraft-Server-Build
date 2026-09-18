# ══ 任务交付分发 ══
execute if score @s rpg.qdone matches 1 run function rpg:quest/done_q1
execute if score @s rpg.qdone matches 2 run function rpg:quest/done_q2
execute if score @s rpg.qdone matches 3 run function rpg:quest/done_q3
execute if score @s rpg.qdone matches 11 run function rpg:quest/done_q11
execute if score @s rpg.qdone matches 12 run function rpg:quest/done_q12
execute if score @s rpg.qdone matches 13 run function rpg:quest/done_q13
scoreboard players reset @s rpg.qdone
