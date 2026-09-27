# ══ 管理员：重置执行者全部RPG数据 ══
scoreboard players set @s rpg.level 1
scoreboard players set @s rpg.exp 0
scoreboard players set @s rpg.gold 100
scoreboard players set @s rpg.k_done 0
scoreboard players set @s rpg.q1s 0
scoreboard players set @s rpg.q2s 0
scoreboard players set @s rpg.q3s 0
scoreboard players set @s rpg.q4s 0
scoreboard players set @s rpg.q5s 0
scoreboard players set @s rpg.q6s 0
scoreboard players set @s rpg.qdone_count 0
scoreboard players set @s rpg.day_claim 0
function rpg:class/reset_self
function rpg:player/calc_need
attribute @s minecraft:max_health modifier remove rpg:level_hp
tellraw @s ["",{"text":"[管理] ","color":"gold"},{"text":"你的全部RPG数据已重置","color":"yellow"}]
