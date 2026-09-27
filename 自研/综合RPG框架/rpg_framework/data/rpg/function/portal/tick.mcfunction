# ══════════════════════════════════════════════════════════
#  传送阵系统 · 扫描循环（每 5 刻一次，每秒 4 次）
# ══════════════════════════════════════════════════════════
#  本系统与怪物领域系统完全独立：不读写任何 zone 判定逻辑，
#  只在"目标类型=怪物领域"时只读取一次区域中心坐标。
#  总开关关闭时下面所有逻辑一行都不执行。
# ---- 总开关 ----
execute unless score #portal_on rpg.data matches 1 run return 0
# ---- 传送冷却递减 ----
execute as @a[scores={rpg.pcd=1..}] run scoreboard players remove @s rpg.pcd 1
# ---- 重置"当前站在哪座阵上" ----
scoreboard players set @a rpg.pnow 0
# ---- 逐槽位扫描：站上阵台的玩家记录槽位号，同时播放待机特效 ----
execute if data storage rpg:portals p1{on:1b} run function rpg:portal/scan with storage rpg:portals p1
execute if data storage rpg:portals p2{on:1b} run function rpg:portal/scan with storage rpg:portals p2
execute if data storage rpg:portals p3{on:1b} run function rpg:portal/scan with storage rpg:portals p3
execute if data storage rpg:portals p4{on:1b} run function rpg:portal/scan with storage rpg:portals p4
execute if data storage rpg:portals p5{on:1b} run function rpg:portal/scan with storage rpg:portals p5
execute if data storage rpg:portals p6{on:1b} run function rpg:portal/scan with storage rpg:portals p6
execute if data storage rpg:portals p7{on:1b} run function rpg:portal/scan with storage rpg:portals p7
execute if data storage rpg:portals p8{on:1b} run function rpg:portal/scan with storage rpg:portals p8
# ---- 站在阵上的玩家累积充能；离开阵台立刻清零 ----
execute as @a[scores={rpg.pnow=1..}] at @s run function rpg:portal/charge
execute as @a[scores={rpg.pnow=0}] run scoreboard players set @s rpg.pcharge 0
