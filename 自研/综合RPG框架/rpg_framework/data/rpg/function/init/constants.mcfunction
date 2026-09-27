# ══ 常量初始化（幂等：仅首次写入） ══
execute unless score #c2 rpg.data = #c2 rpg.data run scoreboard players set #c2 rpg.data 2
execute unless score #c7 rpg.data = #c7 rpg.data run scoreboard players set #c7 rpg.data 7
execute unless score #c12 rpg.data = #c12 rpg.data run scoreboard players set #c12 rpg.data 12
execute unless score #c20 rpg.data = #c20 rpg.data run scoreboard players set #c20 rpg.data 20
execute unless score #day rpg.data = #day rpg.data run scoreboard players set #day rpg.data 0
execute unless score #last_daytime rpg.data = #last_daytime rpg.data run scoreboard players set #last_daytime rpg.data 0
execute unless score #boss_alive rpg.data = #boss_alive rpg.data run scoreboard players set #boss_alive rpg.data 0
execute unless score #elite_alive rpg.data = #elite_alive rpg.data run scoreboard players set #elite_alive rpg.data 0
execute unless score #boss_last_hp rpg.data = #boss_last_hp rpg.data run scoreboard players set #boss_last_hp rpg.data 200
execute unless score #elite_last_hp rpg.data = #elite_last_hp rpg.data run scoreboard players set #elite_last_hp rpg.data 180
execute unless score #boss_cd rpg.data = #boss_cd rpg.data run scoreboard players set #boss_cd rpg.data 0
execute unless score #elite_cd rpg.data = #elite_cd rpg.data run scoreboard players set #elite_cd rpg.data 0
execute unless score #tick rpg.data = #tick rpg.data run scoreboard players set #tick rpg.data 0
# ---- 传送阵扫描节拍计数器 ----
execute unless score #ptick rpg.data = #ptick rpg.data run scoreboard players set #ptick rpg.data 0
# ---- 怪物领域刷怪节拍计数器 ----
execute unless score #zone_ztick rpg.data = #zone_ztick rpg.data run scoreboard players set #zone_ztick rpg.data 0
# ---- 内部常量：随机正负号（固定值，无条件写入，避免被 0 误判为已初始化） ----
scoreboard players set #cneg rpg.data -1
# ---- 怪物领域：可覆盖的默认参数（仅首次写入，避免覆盖游戏内临时调整） ----
execute unless score #zone_on rpg.data = #zone_on rpg.data run scoreboard players set #zone_on rpg.data 0
execute unless score #zone_strict rpg.data = #zone_strict rpg.data run scoreboard players set #zone_strict rpg.data 1
execute unless score #zone_mode rpg.data = #zone_mode rpg.data run scoreboard players set #zone_mode rpg.data 2
execute unless score #zone_pad rpg.data = #zone_pad rpg.data run scoreboard players set #zone_pad rpg.data 24
execute unless score #zone_rate rpg.data = #zone_rate rpg.data run scoreboard players set #zone_rate rpg.data 3
execute unless score #zone_batch rpg.data = #zone_batch rpg.data run scoreboard players set #zone_batch rpg.data 2
# ---- 传送阵：可覆盖的默认参数 ----
execute unless score #portal_on rpg.data = #portal_on rpg.data run scoreboard players set #portal_on rpg.data 1
execute unless score #portal_charge rpg.data = #portal_charge rpg.data run scoreboard players set #portal_charge rpg.data 8
execute unless score #portal_cd rpg.data = #portal_cd rpg.data run scoreboard players set #portal_cd rpg.data 12
# ---- 奖励开关：总开关 + 6 个分项（1=发放 0=停发；实际发放 = 总开关 且 对应分项）----
execute unless score #rw_master rpg.data = #rw_master rpg.data run scoreboard players set #rw_master rpg.data 1
execute unless score #rw_kill rpg.data = #rw_kill rpg.data run scoreboard players set #rw_kill rpg.data 1
execute unless score #rw_quest rpg.data = #rw_quest rpg.data run scoreboard players set #rw_quest rpg.data 1
execute unless score #rw_daily rpg.data = #rw_daily rpg.data run scoreboard players set #rw_daily rpg.data 1
execute unless score #rw_level rpg.data = #rw_level rpg.data run scoreboard players set #rw_level rpg.data 1
execute unless score #rw_boss rpg.data = #rw_boss rpg.data run scoreboard players set #rw_boss rpg.data 1
execute unless score #rw_lottery rpg.data = #rw_lottery rpg.data run scoreboard players set #rw_lottery rpg.data 1
