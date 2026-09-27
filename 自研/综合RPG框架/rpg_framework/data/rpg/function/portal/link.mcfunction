# ══ 管理员：修改已有传送阵的目标与门槛（不动阵法位置） ══
# 用法：/function rpg:portal/link {slot:1,dt:2,di:3,lv:10,cost:50}
#   dt 1=另一座传送阵  2=怪物领域中心  3=传送点 w1/w2/w3
$execute unless data storage rpg:portals p$(slot) run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"槽位 p$(slot) 尚未设置","color":"red"}]
$data modify storage rpg:portals p$(slot).dt set value $(dt)
$data modify storage rpg:portals p$(slot).di set value $(di)
$data modify storage rpg:portals p$(slot).lv set value $(lv)
$data modify storage rpg:portals p$(slot).cost set value $(cost)
$tellraw @s ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"text":"槽位 p$(slot) 目标已更新：类型 $(dt) 编号 $(di)，需 $(lv) 级，费用 $(cost) 金","color":"gray"}]
