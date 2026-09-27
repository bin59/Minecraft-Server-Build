# ══ 传送阵列表 ══
scoreboard players set #pn rpg.data 0
tellraw @s ["",{"text":"╔═══ ","color":"light_purple"},{"text":"传送阵网络","color":"light_purple","bold":true},{"text":" ═══╗","color":"light_purple"}]
execute if score #portal_on rpg.data matches 1 run tellraw @s ["",{"text":"系统状态：","color":"gray"},{"text":"运行中","color":"green","bold":true},{"text":"  站上阵台停留 2 秒自动传送","color":"dark_gray"}]
execute unless score #portal_on rpg.data matches 1 run tellraw @s ["",{"text":"系统状态：","color":"gray"},{"text":"已停用","color":"red","bold":true}]
execute if data storage rpg:portals p1 run function rpg:portal/info with storage rpg:portals p1
execute if data storage rpg:portals p2 run function rpg:portal/info with storage rpg:portals p2
execute if data storage rpg:portals p3 run function rpg:portal/info with storage rpg:portals p3
execute if data storage rpg:portals p4 run function rpg:portal/info with storage rpg:portals p4
execute if data storage rpg:portals p5 run function rpg:portal/info with storage rpg:portals p5
execute if data storage rpg:portals p6 run function rpg:portal/info with storage rpg:portals p6
execute if data storage rpg:portals p7 run function rpg:portal/info with storage rpg:portals p7
execute if data storage rpg:portals p8 run function rpg:portal/info with storage rpg:portals p8
execute if score #pn rpg.data matches 0 run tellraw @s ["",{"text":"  尚未建造任何传送阵","color":"dark_gray","italic":true}]
tellraw @s ["",{"text":"╚══════════════════════╝","color":"light_purple"}]
