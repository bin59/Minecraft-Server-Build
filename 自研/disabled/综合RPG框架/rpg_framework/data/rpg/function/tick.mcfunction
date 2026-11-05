# ══ 综合RPG框架 · 每刻主循环 ══
# ---- 新玩家初始化 ----
execute as @a unless score @s rpg.inited matches 1 run function rpg:player/join
# ---- 玩家触发器处理 ----
execute as @a[scores={rpg.gui=1..}] at @s run function rpg:ui/open_gui
execute as @a[scores={rpg.menu=1..}] at @s run function rpg:ui/menu
execute as @a[scores={rpg.cls=1..}] at @s run function rpg:class/menu
execute as @a[scores={rpg.pick=1..}] at @s run function rpg:class/pick
execute as @a[scores={rpg.stats=1..}] at @s run function rpg:ui/stats
execute as @a[scores={rpg.skill=1..}] at @s run function rpg:skill/cast
execute as @a[scores={rpg.shop=1..}] at @s run function rpg:shop/menu
execute as @a[scores={rpg.buy=1..}] at @s run function rpg:shop/buy_pick
execute as @a[scores={rpg.quest=1..}] at @s run function rpg:quest/menu
execute as @a[scores={rpg.qacc=1..}] at @s run function rpg:quest/accept
execute as @a[scores={rpg.qdone=1..}] at @s run function rpg:quest/done
execute as @a[scores={rpg.warp=1..}] at @s run function rpg:warp/menu
execute as @a[scores={rpg.go=1..}] at @s run function rpg:warp/go
execute as @a[scores={rpg.pay=1..}] at @s run function rpg:economy/pay
execute as @a[scores={rpg.daily=1..}] at @s run function rpg:economy/daily
execute as @a[scores={rpg.top=1..}] at @s run function rpg:ui/top
execute as @a[scores={rpg.boss=1..}] at @s run function rpg:mob/boss_summon
execute as @a[scores={rpg.lottery=1..}] at @s run function rpg:economy/lottery
execute as @a[scores={rpg.elite=1..}] at @s run function rpg:mob/elite_start
# ---- 怪物领域 / 传送阵 玩家入口 ----
execute as @a[scores={rpg.zone=1..}] at @s run function rpg:zone/entry
execute as @a[scores={rpg.portal=1..}] at @s run function rpg:portal/entry
execute as @a[scores={rpg.admin=1..}] at @s run function rpg:ui/open_admin
execute as @a[scores={rpg.zone_admin=1..}] at @s run function rpg:ui/zone_admin_open
execute as @a[scores={rpg.portal_admin=1..}] at @s run function rpg:ui/portal_admin_open
# ---- 战斗结算与升级 ----
execute as @a[scores={rpg.kills=1..}] run function rpg:player/kill_diff
execute as @a[scores={rpg.inited=1}] run function rpg:player/level_check
execute as @a[scores={rpg.gold=10000..}] run advancement grant @s only rpg:rich
execute as @a[scores={rpg.cd=1..}] run scoreboard players remove @s rpg.cd 1
execute if score #boss_cd rpg.data matches 1.. run scoreboard players remove #boss_cd rpg.data 1
execute if score #elite_cd rpg.data matches 1.. run scoreboard players remove #elite_cd rpg.data 1
# ---- 右键处理：技能魔杖(胡萝卜钓竿) / 冒险指南(诡异菌钓竿)，双端通用 ----
execute as @a[scores={rpg.stick=1..}] at @s run function rpg:skill/use
execute as @a[scores={rpg.guide=1..}] at @s run function rpg:ui/guide_open
# ---- 牧师被动：持续缓慢回血 ----
execute if score #tick rpg.data matches 1 as @a[tag=rpg.c_priest] run effect give @s minecraft:regeneration 2 0 true
# ---- 任务进度检测 ----
function rpg:quest/check
# ---- 精英挑战监控 ----
execute if score #elite_alive rpg.data matches 1 run function rpg:mob/elite_tick
# ---- 跨天检测（每日签到/每日任务刷新） ----
execute store result score #daytime rpg.data run time query daytime
execute if score #daytime rpg.data < #last_daytime rpg.data run function rpg:economy/new_day
scoreboard players operation #last_daytime rpg.data = #daytime rpg.data
# ---- BOSS监控 ----
execute if score #boss_alive rpg.data matches 1 run function rpg:mob/boss_tick
# ---- 每秒重新启用触发器 ----
scoreboard players add #tick rpg.data 1
execute if score #tick rpg.data matches 20.. run scoreboard players set #tick rpg.data 0
execute if score #tick rpg.data matches 1 as @a run function rpg:player/enable_triggers
# ---- 怪物领域系统（每秒一次，由 #tick 触发） ----
execute if score #tick rpg.data matches 1 run function rpg:zone/tick
# ---- 传送阵系统（每 5 刻扫描一次，独立于怪物领域） ----
scoreboard players add #ptick rpg.data 1
execute if score #ptick rpg.data matches 5 run scoreboard players set #ptick rpg.data 0
execute if score #ptick rpg.data matches 1 run function rpg:portal/tick
