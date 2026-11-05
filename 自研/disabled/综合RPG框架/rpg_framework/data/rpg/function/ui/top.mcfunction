# ══ 排行榜 ══
scoreboard players reset @s rpg.top
tellraw @s ["",{"text":"═══ ","color":"gold"},{"text":"等级排行榜","color":"yellow","bold":true},{"text":" ═══","color":"gold"}]
tellraw @s ["",{"text":"（右侧计分板实时显示排名）","color":"dark_gray","italic":true}]
scoreboard players list rpg.level
