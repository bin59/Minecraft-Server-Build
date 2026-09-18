# ══ 管理员：给已登记的传送阵重新铺一遍阵法方块 ══
# 用法：/function rpg:portal/rebuild {slot:1}
# 适用于：用配置文件定义了传送阵、或阵法被玩家挖坏需要修复
$execute unless data storage rpg:portals p$(slot) run return run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"槽位 p$(slot) 尚未设置","color":"red"}]
$function rpg:portal/build with storage rpg:portals p$(slot)
$tellraw @s ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"text":"已重建槽位 p$(slot) 的阵法结构","color":"gray"}]
