# ══ 管理员：注销传送阵槽位（只删登记数据，阵法方块保留，需要自己拆） ══
# 用法：/function rpg:portal/remove {slot:1}
$data remove storage rpg:portals p$(slot)
$tellraw @s ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"text":"已注销槽位 p$(slot)（阵法方块未拆除，如需清理请手动挖掉）","color":"gray"}]
