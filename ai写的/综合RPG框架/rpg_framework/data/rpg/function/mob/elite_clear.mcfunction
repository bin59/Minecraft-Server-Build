# ══ 精英挑战：全部击败，结算 ══
scoreboard players set #elite_alive rpg.data 0
bossbar set rpg:elite visible false
bossbar set rpg:elite players
# 参战玩家奖励（对精英造成伤害即算参战）——受 总开关 与「BOSS/精英」分项 控制
execute if score #rw_master rpg.data matches 1 if score #rw_boss rpg.data matches 1 run function rpg:mob/elite_reward
advancement grant @a[tag=rpg.elite_fight] only rpg:elite_hero
tellraw @a ["",{"text":"══════ ","color":"gold"},{"text":"精英入侵已平息","color":"gold","bold":true},{"text":" ══════","color":"gold"}]
title @a title {"text":"胜利！","color":"gold","bold":true}
title @a subtitle {"text":"精英入侵已平息","color":"yellow"}
execute as @a[tag=rpg.elite_fight] run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1.3
execute as @a[tag=rpg.elite_fight] at @s run particle minecraft:totem_of_undying ~ ~1 ~ 1 0.8 1 0.2 40
tag @a remove rpg.elite_fight
