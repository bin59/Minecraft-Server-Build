# ══ 综合RPG框架 · 卸载 ══
# 用法：/function rpg:uninstall
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"text":"正在卸载综合RPG框架…","color":"red"}]
bossbar remove rpg:boss
bossbar remove rpg:elite
tag @a remove rpg.c_warrior
tag @a remove rpg.c_archer
tag @a remove rpg.c_mage
tag @a remove rpg.c_priest
tag @a remove rpg.payer
tag @a remove rpg.boss_fight
tag @a remove rpg.elite_fight
execute as @a run attribute @s minecraft:max_health modifier remove rpg:level_hp
execute as @a run attribute @s minecraft:max_health modifier remove rpg:class_hp
execute as @a run attribute @s minecraft:attack_damage modifier remove rpg:class_atk
execute as @a run attribute @s minecraft:movement_speed modifier remove rpg:class_spd
scoreboard objectives remove rpg.level
scoreboard objectives remove rpg.exp
scoreboard objectives remove rpg.exp_need
scoreboard objectives remove rpg.gold
scoreboard objectives remove rpg.class
scoreboard objectives remove rpg.kills
scoreboard objectives remove rpg.k_done
scoreboard objectives remove rpg.q1z
scoreboard objectives remove rpg.q2i
scoreboard objectives remove rpg.q3k
scoreboard objectives remove rpg.q1base
scoreboard objectives remove rpg.q2base
scoreboard objectives remove rpg.q3base
scoreboard objectives remove rpg.q1s
scoreboard objectives remove rpg.q2s
scoreboard objectives remove rpg.q3s
scoreboard objectives remove rpg.q4base
scoreboard objectives remove rpg.q4s
scoreboard objectives remove rpg.q5m
scoreboard objectives remove rpg.q5base
scoreboard objectives remove rpg.q5s
scoreboard objectives remove rpg.q6e
scoreboard objectives remove rpg.q6base
scoreboard objectives remove rpg.q6s
scoreboard objectives remove rpg.qdone_count
scoreboard objectives remove rpg.cd
scoreboard objectives remove rpg.inited
scoreboard objectives remove rpg.day_claim
scoreboard objectives remove rpg.stick
scoreboard objectives remove rpg.guide
scoreboard objectives remove rpg.data
scoreboard objectives remove rpg.tmp
scoreboard objectives remove rpg.menu
scoreboard objectives remove rpg.cls
scoreboard objectives remove rpg.pick
scoreboard objectives remove rpg.stats
scoreboard objectives remove rpg.skill
scoreboard objectives remove rpg.shop
scoreboard objectives remove rpg.buy
scoreboard objectives remove rpg.quest
scoreboard objectives remove rpg.qacc
scoreboard objectives remove rpg.qdone
scoreboard objectives remove rpg.warp
scoreboard objectives remove rpg.go
scoreboard objectives remove rpg.pay
scoreboard objectives remove rpg.daily
scoreboard objectives remove rpg.top
scoreboard objectives remove rpg.boss
scoreboard objectives remove rpg.lottery
scoreboard objectives remove rpg.elite
scoreboard objectives remove rpg.gui
# ---- 怪物领域 / 传送阵 ----
scoreboard objectives remove rpg.zone
scoreboard objectives remove rpg.portal
scoreboard objectives remove rpg.admin
scoreboard objectives remove rpg.zone_admin
scoreboard objectives remove rpg.portal_admin
scoreboard objectives remove rpg.pnow
scoreboard objectives remove rpg.pcharge
scoreboard objectives remove rpg.pcd
tag @a remove rpg.zin_p
tag @a remove rpg.zwas
data remove storage rpg:zones z1
data remove storage rpg:zones z2
data remove storage rpg:zones z3
data remove storage rpg:zones z4
data remove storage rpg:zones z5
data remove storage rpg:zones z6
data remove storage rpg:zones z7
data remove storage rpg:zones z8
data remove storage rpg:portals p1
data remove storage rpg:portals p2
data remove storage rpg:portals p3
data remove storage rpg:portals p4
data remove storage rpg:portals p5
data remove storage rpg:portals p6
data remove storage rpg:portals p7
data remove storage rpg:portals p8
data remove storage rpg:cfg tmp
data remove storage rpg:cfg ptmp
data remove storage rpg:cfg pick
data remove storage rpg:cfg pmsg
data remove storage rpg:cfg off
data remove storage rpg:warps w1
data remove storage rpg:warps w2
data remove storage rpg:warps w3
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"text":"卸载完成，请执行 /datapack disable \"file/rpg_framework\" 或移除文件夹","color":"gray"}]
