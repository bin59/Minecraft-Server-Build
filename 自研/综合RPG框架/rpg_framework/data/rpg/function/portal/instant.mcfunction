# ══ 立即激活脚下的传送阵 ══
execute unless score #portal_on rpg.data matches 1 run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"传送阵系统当前已停用","color":"red"}]
execute if score @s rpg.pnow matches 0 run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"你当前不在任何传送阵上","color":"red"}]
execute if score @s rpg.pcd matches 1.. run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"传送冷却中，请稍等","color":"red"}]
function rpg:portal/fire
