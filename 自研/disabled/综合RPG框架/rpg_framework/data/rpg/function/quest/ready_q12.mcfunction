# ══ 每日任务2达标 ══
scoreboard players set @s rpg.q5s 2
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.8
tellraw @s ["",{"text":"[任务] ","color":"gold"},{"text":"每日任务「采石工人」目标达成！","color":"green","bold":true},{"text":" ","color":"gray"},{"text":"[点击交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 12"},"hover_event":{"action":"show_text","value":"点击交付任务"}}]
