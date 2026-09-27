# ══ 越界清除 ══
# 严格模式(#zone_strict=1)：全服区域外的受管制怪物一律清除，含原版自然生成的
#                           —— 这就是"怪物只能存在于管理员设定的区域内"
# 宽松模式(#zone_strict=0)：只清除本框架刷出的怪(rpg.zmob)，原版生态照常运转
# 三类实体永久豁免：BOSS(rpg.boss)、精英(rpg.elite)、服主手动保护(rpg.zkeep)
execute if score #zone_strict rpg.data matches 1 run kill @e[type=#rpg:zone_mobs,tag=!rpg.zin,tag=!rpg.zkeep,tag=!rpg.boss,tag=!rpg.elite]
execute if score #zone_strict rpg.data matches 0 run kill @e[type=#rpg:zone_mobs,tag=rpg.zmob,tag=!rpg.zin,tag=!rpg.zkeep,tag=!rpg.boss,tag=!rpg.elite]
