# ══ 怪物领域列表（管理员与玩家通用） ══
scoreboard players set #zn rpg.data 0
tellraw @s ["",{"text":"╔═══ ","color":"gold"},{"text":"怪物领域","color":"yellow","bold":true},{"text":" ═══╗","color":"gold"}]
execute if score #zone_on rpg.data matches 1 run tellraw @s ["",{"text":"系统状态：","color":"gray"},{"text":"运行中","color":"green","bold":true}]
execute unless score #zone_on rpg.data matches 1 run tellraw @s ["",{"text":"系统状态：","color":"gray"},{"text":"已停用","color":"red","bold":true},{"text":"（怪物生成完全交给原版）","color":"dark_gray"}]
execute if score #zone_on rpg.data matches 1 if score #zone_strict rpg.data matches 1 run tellraw @s ["",{"text":"管制强度：","color":"gray"},{"text":"严格","color":"red"},{"text":" —— 区域外不会有任何敌对怪物","color":"dark_gray"}]
execute if score #zone_on rpg.data matches 1 if score #zone_strict rpg.data matches 0 run tellraw @s ["",{"text":"管制强度：","color":"gray"},{"text":"宽松","color":"yellow"},{"text":" —— 仅管制领域刷出的怪，原版生态照常","color":"dark_gray"}]
execute if score #zone_on rpg.data matches 1 if score #zone_mode rpg.data matches 2 run tellraw @s ["",{"text":"越界处理：","color":"gray"},{"text":"先拉回，超出缓冲带才清除","color":"aqua"}]
execute if score #zone_on rpg.data matches 1 if score #zone_mode rpg.data matches 1 run tellraw @s ["",{"text":"越界处理：","color":"gray"},{"text":"直接清除","color":"aqua"}]
execute if data storage rpg:zones z1 run function rpg:zone/info with storage rpg:zones z1
execute if data storage rpg:zones z2 run function rpg:zone/info with storage rpg:zones z2
execute if data storage rpg:zones z3 run function rpg:zone/info with storage rpg:zones z3
execute if data storage rpg:zones z4 run function rpg:zone/info with storage rpg:zones z4
execute if data storage rpg:zones z5 run function rpg:zone/info with storage rpg:zones z5
execute if data storage rpg:zones z6 run function rpg:zone/info with storage rpg:zones z6
execute if data storage rpg:zones z7 run function rpg:zone/info with storage rpg:zones z7
execute if data storage rpg:zones z8 run function rpg:zone/info with storage rpg:zones z8
execute if score #zn rpg.data matches 0 run tellraw @s ["",{"text":"  尚未设置任何怪物领域","color":"dark_gray","italic":true}]
tellraw @s ["",{"text":"前往方式：站上传送阵充能 2 秒即可跨图传送","color":"aqua"},{"text":"  [传送阵列表]","color":"light_purple","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.portal"}}]
tellraw @s ["",{"text":"╚══════════════════════╝","color":"gold"}]
