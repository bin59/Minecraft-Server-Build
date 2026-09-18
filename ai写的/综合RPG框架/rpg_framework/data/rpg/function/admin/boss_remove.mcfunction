# ══ 管理员：强制移除BOSS（不发放奖励） ══
kill @e[type=minecraft:zombie,tag=rpg.boss]
scoreboard players set #boss_alive rpg.data 0
tag @a remove rpg.boss_fight
bossbar set rpg:boss visible false
bossbar set rpg:boss players
tellraw @a ["",{"text":"[BOSS] ","color":"dark_red"},{"text":"深渊领主已被管理员强制移除","color":"gray"}]
