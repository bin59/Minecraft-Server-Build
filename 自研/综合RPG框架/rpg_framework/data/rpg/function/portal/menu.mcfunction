# ══ 玩家传送阵面板（基岩版主入口，Java 版也可用） ══
function rpg:portal/list
execute if score @s rpg.pnow matches 1.. run tellraw @s ["",{"text":"你正站在传送阵上 ","color":"green"},{"text":"[立即激活]","color":"light_purple","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.portal set 2"},"hover_event":{"action":"show_text","value":"跳过 2 秒充能，立刻传送"}}]
execute if score @s rpg.pnow matches 0 run tellraw @s ["",{"text":"提示：走到传送阵上原地停留 2 秒即会自动传送，无需任何指令","color":"gray","italic":true}]
tellraw @s ["",{"text":"[怪物领域列表]","color":"red","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.zone"},"hover_event":{"action":"show_text","value":"查看怪物集中刷新的区域"}},{"text":"   ","color":"gray"},{"text":"[返回主菜单]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.menu"}}]
