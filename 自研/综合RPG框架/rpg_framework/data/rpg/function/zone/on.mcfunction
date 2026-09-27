# ══ 开启怪物领域系统总开关 ══
scoreboard players set #zone_on rpg.data 1
tellraw @a ["",{"text":"[领域] ","color":"gold","bold":true},{"text":"怪物领域系统已","color":"gray"},{"text":"启用","color":"green","bold":true},{"text":" —— 怪物将被限制在管理员设定的区域内","color":"gray"}]
execute unless data storage rpg:zones z1 unless data storage rpg:zones z2 unless data storage rpg:zones z3 unless data storage rpg:zones z4 unless data storage rpg:zones z5 unless data storage rpg:zones z6 unless data storage rpg:zones z7 unless data storage rpg:zones z8 run tellraw @s ["",{"text":"[领域] ","color":"gold"},{"text":"注意：当前没有任何区域。严格模式下这会清掉全服敌对怪物！先创建区域：","color":"red"},{"text":"[打开管理面板]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/function rpg:ui/open_admin"}}]
