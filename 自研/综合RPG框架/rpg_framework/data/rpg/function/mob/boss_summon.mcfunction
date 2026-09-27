# ══ 召唤BOSS：深渊领主（需5级 + 深渊之眼 + 冷却结束，在BOSS竞技场生成） ══
scoreboard players reset @s rpg.boss
execute if score #boss_alive rpg.data matches 1 run return run tellraw @s ["",{"text":"[BOSS] ","color":"dark_red"},{"text":"深渊领主已在世间，无法重复召唤","color":"red"}]
execute if score #boss_cd rpg.data matches 1.. run return run function rpg:mob/boss_cd_msg
execute if score @s rpg.level matches ..4 run return run tellraw @s ["",{"text":"[BOSS] ","color":"dark_red"},{"text":"等级不足！需要 5 级才能挑战深渊领主","color":"red"}]
execute store result score #has_item rpg.tmp run clear @s minecraft:ender_eye[minecraft:custom_data~{rpg_boss_summon:1b}] 0
execute if score #has_item rpg.tmp matches ..0 run return run tellraw @s ["",{"text":"[BOSS] ","color":"dark_red"},{"text":"需要召唤道具 ","color":"red"},{"text":"深渊之眼","color":"light_purple","bold":true},{"text":"（商店第11项，300金购买）","color":"red"}]
execute unless data storage rpg:warps w3 run return run tellraw @s ["",{"text":"[BOSS] ","color":"dark_red"},{"text":"BOSS竞技场尚未设置，请联系服主（/function rpg:warp/set_3）","color":"red"}]
clear @s minecraft:ender_eye[minecraft:custom_data~{rpg_boss_summon:1b}] 1
scoreboard players set #boss_alive rpg.data 1
scoreboard players set #boss_cd rpg.data 12000
scoreboard players set #boss_last_hp rpg.data 200
tag @s add rpg.boss_fight
tag @s add rpg.summon_tp
function rpg:mob/boss_spawn with storage rpg:warps w3
function rpg:warp/go_3
tag @s remove rpg.summon_tp
bossbar set rpg:boss value 200
bossbar set rpg:boss visible true
bossbar set rpg:boss players @a
tellraw @a ["",{"text":"══════ ","color":"dark_red"},{"text":"深渊领主降临","color":"dark_red","bold":true},{"text":" ══════","color":"dark_red"}]
tellraw @a ["",{"selector":"@a[tag=rpg.boss_fight]","color":"aqua","bold":true},{"text":" 唤醒了深渊领主！战场位于BOSS竞技场","color":"gray"}]
tellraw @a ["",{"text":"前往方式：主菜单 → 传送系统 → BOSS竞技场","color":"aqua"}]
tellraw @a ["",{"text":"击杀奖励：参战玩家 +500金币 +300经验（对BOSS造成伤害即算参战）","color":"gold"}]
title @a title {"text":"深渊领主降临","color":"dark_red","bold":true}
title @a subtitle {"text":"拿起武器，保卫世界！","color":"gray"}
execute as @a run playsound minecraft:entity.ender_dragon.growl master @s ~ ~ ~ 0.6 0.8
