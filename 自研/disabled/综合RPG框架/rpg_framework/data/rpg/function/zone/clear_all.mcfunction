# ══ 清除全服由本框架刷出的怪物（不影响原版自然生成的怪、不影响BOSS与精英） ══
kill @e[type=#rpg:zone_mobs,tag=rpg.zmob,tag=!rpg.zkeep,tag=!rpg.boss,tag=!rpg.elite]
tellraw @s ["",{"text":"[领域] ","color":"gold","bold":true},{"text":"已清除全服由领域系统刷出的怪物","color":"gray"}]
