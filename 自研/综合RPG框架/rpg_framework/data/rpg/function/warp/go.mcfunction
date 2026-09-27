# ══ 传送分发 ══
execute if score @s rpg.go matches 1 run function rpg:warp/go_1
execute if score @s rpg.go matches 2 run function rpg:warp/go_2
execute if score @s rpg.go matches 3 run function rpg:warp/go_3
scoreboard players reset @s rpg.go
