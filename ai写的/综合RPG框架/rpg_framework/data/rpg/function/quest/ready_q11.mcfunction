# ══ 每日任务1达标 ══
scoreboard players set @s rpg.q4s 2
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.8
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"每日任务「讨伐怪物」目标达成！","color":"green","bold":true},{"text":" ","color":"gray"},{"text":"[点击交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 11"},"hover_event":{"action":"show_text","value":"点击交付任务"}}]
