# ══ 管理员：删除区域槽位 ══
# 用法：/function rpg:zone/remove {slot:1}
# 删除前会先清掉该区域里本框架刷出的怪，避免留下无主怪物在原地游荡
$execute if data storage rpg:zones z$(slot) run function rpg:zone/clear_one with storage rpg:zones z$(slot)
$data remove storage rpg:zones z$(slot)
$tellraw @s ["",{"text":"[领域] ","color":"gold","bold":true},{"text":"已删除区域槽位 ","color":"gray"},{"text":"z$(slot)","color":"yellow"}]
