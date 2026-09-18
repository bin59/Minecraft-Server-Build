# ══ 管理员：停用某座传送阵（阵法还在，但踩上去不会触发） ══
# 用法：/function rpg:portal/disable {slot:1}
$execute unless data storage rpg:portals p$(slot) run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"槽位 p$(slot) 尚未设置","color":"red"}]
$data modify storage rpg:portals p$(slot).on set value 0b
$tellraw @s ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"text":"槽位 p$(slot) 已","color":"gray"},{"text":"停用","color":"red","bold":true}]
