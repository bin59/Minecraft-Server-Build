# ══ 新玩家初始化 ══
scoreboard players set @s rpg.inited 1
scoreboard players set @s rpg.level 1
scoreboard players set @s rpg.exp 0
scoreboard players set @s rpg.gold 100
scoreboard players set @s rpg.class 0
scoreboard players set @s rpg.k_done 0
scoreboard players set @s rpg.cd 0
function rpg:player/calc_need
give @s minecraft:bread 16
give @s minecraft:carrot_on_a_stick[minecraft:custom_name='{"text":"技能魔杖","color":"aqua","italic":false}',minecraft:lore=['{"text":"右键释放职业技能","color":"gray","italic":false}'],minecraft:custom_data={rpg_wand:1b}]
give @s minecraft:warped_fungus_on_a_stick[minecraft:custom_name='{"text":"冒险指南","color":"gold","italic":false}',minecraft:lore=['{"text":"右键打开RPG主菜单","color":"gray","italic":false}','{"text":"Java与基岩版通用","color":"dark_gray","italic":false}']]
title @s title {"text":"欢迎来到RPG世界","color":"gold","bold":true}
title @s subtitle {"text":"右键冒险指南，或按ESC打开菜单","color":"yellow"}
tellraw @s ["",{"text":"━━━━━━ ","color":"gold"},{"text":"角色创建","color":"yellow","bold":true},{"text":" ━━━━━━","color":"gold"}]
tellraw @s ["",{"text":"获得新手礼包：面包×16、技能魔杖、冒险指南、金币×100","color":"green"}]
tellraw @s ["",{"text":"第一步：Java版按 ","color":"gray"},{"text":"ESC","color":"yellow","bold":true},{"text":" 打开暂停菜单；基岩版 ","color":"gray"},{"text":"右键冒险指南","color":"yellow","bold":true},{"text":" 或输入 ","color":"gray"},{"text":"/trigger rpg.menu","color":"aqua"}]
advancement grant @s only rpg:root
