# ══ 传送点菜单 ══
scoreboard players reset @s rpg.warp
tellraw @s ["",{"text":"╔═══ ","color":"gold"},{"text":"传送系统","color":"yellow","bold":true},{"text":" ═══╗","color":"gold"}]
execute if data storage rpg:warps w1 run tellraw @s ["",{"text":"1. 主城 ","color":"white"},{"text":"[传送]","color":"aqua","bold":true,"click_event":{"action":"suggest_command","command":"/trigger rpg.go set 1"}}]
execute unless data storage rpg:warps w1 run tellraw @s ["",{"text":"1. 主城 ","color":"dark_gray"},{"text":"（管理员尚未设置）","color":"dark_gray","italic":true}]
execute if data storage rpg:warps w2 run tellraw @s ["",{"text":"2. 矿区 ","color":"white"},{"text":"[传送]","color":"aqua","bold":true,"click_event":{"action":"suggest_command","command":"/trigger rpg.go set 2"}}]
execute unless data storage rpg:warps w2 run tellraw @s ["",{"text":"2. 矿区 ","color":"dark_gray"},{"text":"（管理员尚未设置）","color":"dark_gray","italic":true}]
execute if data storage rpg:warps w3 run tellraw @s ["",{"text":"3. BOSS竞技场 ","color":"white"},{"text":"[传送]","color":"aqua","bold":true,"click_event":{"action":"suggest_command","command":"/trigger rpg.go set 3"}}]
execute unless data storage rpg:warps w3 run tellraw @s ["",{"text":"3. BOSS竞技场 ","color":"dark_gray"},{"text":"（管理员尚未设置）","color":"dark_gray","italic":true}]
tellraw @s ["",{"text":"╚══════════════════════╝","color":"gold"}]
tellraw @s ["",{"text":"管理员设置传送点：/function rpg:warp/set_1（站在目标位置执行）","color":"dark_gray","italic":true}]
