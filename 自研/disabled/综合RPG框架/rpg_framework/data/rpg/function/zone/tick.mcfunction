# ══════════════════════════════════════════════════════════
#  怪物领域系统 · 主循环（每秒一次，由 rpg:tick 调度）
# ══════════════════════════════════════════════════════════
#  本系统与传送阵系统完全独立：不读写任何 portal 数据，
#  总开关关闭时下面所有逻辑一行都不执行。
# ---- 总开关 ----
execute unless score #zone_on rpg.data matches 1 run return 0
# ---- 1. 清除上一轮的"在区域内"标记 ----
tag @e[type=#rpg:zone_mobs] remove rpg.zin
tag @a remove rpg.zin_p
# ---- 2. 逐槽位标记：落在长方体范围内的怪物与玩家 ----
execute if data storage rpg:zones z1{on:1b} run function rpg:zone/mark with storage rpg:zones z1
execute if data storage rpg:zones z2{on:1b} run function rpg:zone/mark with storage rpg:zones z2
execute if data storage rpg:zones z3{on:1b} run function rpg:zone/mark with storage rpg:zones z3
execute if data storage rpg:zones z4{on:1b} run function rpg:zone/mark with storage rpg:zones z4
execute if data storage rpg:zones z5{on:1b} run function rpg:zone/mark with storage rpg:zones z5
execute if data storage rpg:zones z6{on:1b} run function rpg:zone/mark with storage rpg:zones z6
execute if data storage rpg:zones z7{on:1b} run function rpg:zone/mark with storage rpg:zones z7
execute if data storage rpg:zones z8{on:1b} run function rpg:zone/mark with storage rpg:zones z8
# ---- 3. 拉回模式：缓冲带内的越界怪先拽回中心（拽回后视为在区域内） ----
execute if score #zone_mode rpg.data matches 2 run function rpg:zone/leash_all
# ---- 4. 清除仍在区域外的受管制怪物 ----
function rpg:zone/purge
# ---- 5. 玩家进出领域提示 ----
execute as @a[tag=rpg.zin_p,tag=!rpg.zwas] run function rpg:zone/enter_msg
execute as @a[tag=!rpg.zin_p,tag=rpg.zwas] run function rpg:zone/leave_msg
tag @a[tag=rpg.zin_p] add rpg.zwas
tag @a[tag=!rpg.zin_p] remove rpg.zwas
# ---- 6. 刷怪节拍：每 #zone_rate 秒触发一轮 ----
scoreboard players add #zone_ztick rpg.data 1
execute if score #zone_ztick rpg.data >= #zone_rate rpg.data run function rpg:zone/spawn_all
