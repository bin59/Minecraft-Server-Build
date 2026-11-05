# ══ 关闭怪物领域系统总开关（区域配置全部保留） ══
scoreboard players set #zone_on rpg.data 0
tag @e[type=#rpg:zone_mobs] remove rpg.zin
tag @a remove rpg.zin_p
tag @a remove rpg.zwas
tellraw @a ["",{"text":"[领域] ","color":"gold","bold":true},{"text":"怪物领域系统已","color":"gray"},{"text":"停用","color":"red","bold":true},{"text":" —— 怪物生成恢复原版规则（区域配置已保留）","color":"gray"}]
