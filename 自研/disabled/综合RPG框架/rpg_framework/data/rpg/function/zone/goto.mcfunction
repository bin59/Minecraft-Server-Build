# ══ 管理员：直接传送到某个区域中心（用于巡查，不走传送阵） ══
# 用法：/function rpg:zone/goto {slot:1}
$execute unless data storage rpg:zones z$(slot) run return run tellraw @s ["",{"text":"[领域] ","color":"gold"},{"text":"槽位 z$(slot) 尚未设置","color":"red"}]
$function rpg:zone/goto_exec with storage rpg:zones z$(slot)
