# ══ 任务接受分发 ══
execute if score @s rpg.qacc matches 1 run function rpg:quest/accept_q1
execute if score @s rpg.qacc matches 2 run function rpg:quest/accept_q2
execute if score @s rpg.qacc matches 3 run function rpg:quest/accept_q3
execute if score @s rpg.qacc matches 11 run function rpg:quest/accept_q11
execute if score @s rpg.qacc matches 12 run function rpg:quest/accept_q12
execute if score @s rpg.qacc matches 13 run function rpg:quest/accept_q13
scoreboard players reset @s rpg.qacc
