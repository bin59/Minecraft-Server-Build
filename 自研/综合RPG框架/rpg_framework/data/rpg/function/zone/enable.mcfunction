# ══ 管理员：启用某个区域槽位 ══
# 用法：/function rpg:zone/enable {slot:1}
$execute unless data storage rpg:zones z$(slot) run return run tellraw @s ["",{"text":"[领域] ","color":"gold"},{"text":"槽位 z$(slot) 尚未设置","color":"red"}]
$data modify storage rpg:zones z$(slot).on set value 1b
$tellraw @s ["",{"text":"[领域] ","color":"gold","bold":true},{"text":"槽位 z$(slot) 已","color":"gray"},{"text":"启用","color":"green","bold":true}]
