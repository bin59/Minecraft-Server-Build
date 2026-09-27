# ══ 角色面板 ══
scoreboard players reset @s rpg.stats
tellraw @s ["",{"text":"╔═══ ","color":"gold"},{"text":"角色面板","color":"yellow","bold":true},{"text":" ═══╗","color":"gold"}]
execute if score @s rpg.class matches 0 run tellraw @s ["",{"text":"职业：","color":"gray"},{"text":"未选择","color":"dark_gray","italic":true}]
execute if score @s rpg.class matches 1 run tellraw @s ["",{"text":"职业：","color":"gray"},{"text":"战士","color":"red","bold":true},{"text":"（攻击+3 生命+4）","color":"dark_gray"}]
execute if score @s rpg.class matches 2 run tellraw @s ["",{"text":"职业：","color":"gray"},{"text":"弓箭手","color":"green","bold":true},{"text":"（移速+15%）","color":"dark_gray"}]
execute if score @s rpg.class matches 3 run tellraw @s ["",{"text":"职业：","color":"gray"},{"text":"法师","color":"light_purple","bold":true},{"text":"（技能冷却-30%）","color":"dark_gray"}]
execute if score @s rpg.class matches 4 run tellraw @s ["",{"text":"职业：","color":"gray"},{"text":"牧师","color":"yellow","bold":true},{"text":"（持续回血）","color":"dark_gray"}]
tellraw @s ["",{"text":"等级：","color":"gray"},{"score":{"name":"@s","objective":"rpg.level"},"color":"aqua","bold":true},{"text":"  ","color":"gray"},{"text":"经验：","color":"gray"},{"score":{"name":"@s","objective":"rpg.exp"},"color":"green"},{"text":"/","color":"gray"},{"score":{"name":"@s","objective":"rpg.exp_need"},"color":"green"}]
tellraw @s ["",{"text":"金币：","color":"gray"},{"score":{"name":"@s","objective":"rpg.gold"},"color":"gold","bold":true},{"text":"  ","color":"gray"},{"text":"总击杀：","color":"gray"},{"score":{"name":"@s","objective":"rpg.kills"},"color":"red"}]
execute if score @s rpg.cd matches 1.. run tellraw @s ["",{"text":"技能冷却：","color":"gray"},{"score":{"name":"@s","objective":"rpg.cd"},"color":"red"},{"text":" tick","color":"dark_gray"}]
execute if score @s rpg.cd matches ..0 run tellraw @s ["",{"text":"技能冷却：","color":"gray"},{"text":"就绪","color":"green","bold":true}]
tellraw @s ["",{"text":"╚════════════════╝","color":"gold"}]
