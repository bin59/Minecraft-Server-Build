# ══ BOSS被击败 ══
scoreboard players set #boss_alive rpg.data 0
bossbar set rpg:boss visible false
bossbar set rpg:boss players
tellraw @a ["",{"text":"══════ ","color":"gold"},{"text":"深渊领主已被击败","color":"gold","bold":true},{"text":" ══════","color":"gold"}]
# ---- 参战金币/经验受 总开关 与「BOSS/精英」分项 控制；进度成就照常授予 ----
execute if score #rw_master rpg.data matches 1 if score #rw_boss rpg.data matches 1 run function rpg:mob/boss_reward
advancement grant @a[tag=rpg.boss_fight] only rpg:boss_slayer
title @a title {"text":"胜利！","color":"gold","bold":true}
title @a subtitle {"text":"深渊领主已被消灭","color":"yellow"}
execute as @a[tag=rpg.boss_fight] run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1.3
execute as @a[tag=rpg.boss_fight] at @s run particle minecraft:totem_of_undying ~ ~1 ~ 1.5 1 1.5 0.3 60
tag @a remove rpg.boss_fight
