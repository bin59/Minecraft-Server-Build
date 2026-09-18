# ══ 管理员：升1级（对执行者生效） ══
scoreboard players add @s rpg.level 1
function rpg:player/calc_need
execute if score @s rpg.level matches 10.. run function rpg:player/apply_passives
tellraw @s ["",{"text":"[管理] ","color":"gold"},{"text":"+1级，当前等级 ","color":"green"},{"score":{"name":"@s","objective":"rpg.level"},"color":"aqua","bold":true}]
