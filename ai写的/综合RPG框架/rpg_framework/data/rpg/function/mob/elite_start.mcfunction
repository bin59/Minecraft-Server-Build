# ══ 精英入侵：开启挑战（需3级 + 精英召集令 + 冷却结束，在BOSS竞技场生成） ══
scoreboard players reset @s rpg.elite
execute if score #elite_alive rpg.data matches 1 run return run tellraw @s ["",{"text":"[精英] ","color":"dark_purple"},{"text":"已有精英入侵进行中","color":"red"}]
execute if score #elite_cd rpg.data matches 1.. run return run function rpg:mob/elite_cd_msg
execute if score @s rpg.level matches ..2 run return run tellraw @s ["",{"text":"[精英] ","color":"dark_purple"},{"text":"等级不足！需要3级","color":"red"}]
execute store result score #has_item rpg.tmp run clear @s minecraft:blaze_rod[minecraft:custom_data~{rpg_elite_summon:1b}] 0
execute if score #has_item rpg.tmp matches ..0 run return run tellraw @s ["",{"text":"[精英] ","color":"dark_purple"},{"text":"需要召唤道具 ","color":"dark_purple"},{"text":"精英召集令","color":"gold","bold":true},{"text":"（商店第12项，100金购买）","color":"dark_purple"}]
execute unless data storage rpg:warps w3 run return run tellraw @s ["",{"text":"[精英] ","color":"dark_purple"},{"text":"BOSS竞技场尚未设置，请联系服主（/function rpg:warp/set_3）","color":"dark_purple"}]
clear @s minecraft:blaze_rod[minecraft:custom_data~{rpg_elite_summon:1b}] 1
scoreboard players set #elite_alive rpg.data 1
scoreboard players set #elite_cd rpg.data 6000
scoreboard players set #elite_last_hp rpg.data 180
tag @s add rpg.elite_fight
tag @s add rpg.summon_tp
function rpg:mob/elite_spawn with storage rpg:warps w3
function rpg:warp/go_3
tag @s remove rpg.summon_tp
bossbar set rpg:elite value 3
bossbar set rpg:elite visible true
bossbar set rpg:elite players @a
tellraw @a ["",{"text":"══════ ","color":"dark_purple"},{"text":"精英入侵","color":"dark_purple","bold":true},{"text":" ══════","color":"dark_purple"}]
tellraw @a ["",{"selector":"@a[tag=rpg.elite_fight]","color":"aqua"},{"text":" 触发了精英入侵！战场位于BOSS竞技场","color":"gray"}]
tellraw @a ["",{"text":"前往方式：主菜单 → 传送系统 → BOSS竞技场","color":"aqua"}]
tellraw @a ["",{"text":"击败全部3只精英，参战玩家获得重赏！","color":"gold"}]
title @a title {"text":"精英入侵！","color":"dark_purple","bold":true}
title @a subtitle {"text":"击败3只精英怪物","color":"gray"}
execute as @a run playsound minecraft:entity.ender_dragon.growl master @s ~ ~ ~ 0.5 1.2
