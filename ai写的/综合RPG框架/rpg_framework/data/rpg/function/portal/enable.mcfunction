# ══ 管理员：启用某座传送阵 ══
# 用法：/function rpg:portal/enable {slot:1}
$execute unless data storage rpg:portals p$(slot) run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"槽位 p$(slot) 尚未设置","color":"red"}]
$data modify storage rpg:portals p$(slot).on set value 1b
$tellraw @s ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"text":"槽位 p$(slot) 已","color":"gray"},{"text":"启用","color":"green","bold":true}]
