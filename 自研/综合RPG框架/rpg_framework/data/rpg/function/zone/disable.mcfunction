# ══ 管理员：停用某个区域槽位（保留配置，只是不再刷怪/不再判定） ══
# 用法：/function rpg:zone/disable {slot:1}
$execute unless data storage rpg:zones z$(slot) run return run tellraw @s ["",{"text":"[领域] ","color":"gold"},{"text":"槽位 z$(slot) 尚未设置","color":"red"}]
$execute if data storage rpg:zones z$(slot) run function rpg:zone/clear_one with storage rpg:zones z$(slot)
$data modify storage rpg:zones z$(slot).on set value 0b
$tellraw @s ["",{"text":"[领域] ","color":"gold","bold":true},{"text":"槽位 z$(slot) 已","color":"gray"},{"text":"停用","color":"red","bold":true},{"text":"（配置保留，可随时重新启用）","color":"dark_gray"}]
