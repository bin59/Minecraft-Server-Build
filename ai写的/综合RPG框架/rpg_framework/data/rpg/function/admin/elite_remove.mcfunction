# ══ 管理员：强制移除精英入侵（不发放奖励） ══
kill @e[tag=rpg.elite]
scoreboard players set #elite_alive rpg.data 0
tag @a remove rpg.elite_fight
bossbar set rpg:elite visible false
bossbar set rpg:elite players
tellraw @a ["",{"text":"[精英] ","color":"dark_purple"},{"text":"精英入侵已被管理员强制移除","color":"gray"}]
