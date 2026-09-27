# ══ 单座传送阵信息行（宏，由 portal/list 逐槽位调用） ══
scoreboard players add #pn rpg.data 1
$execute if data storage rpg:portals p$(s){on:1b} run tellraw @s ["",{"text":"[p$(s)] ","color":"light_purple","bold":true},{"text":"$(name)","color":"aqua","bold":true},{"text":" 启用","color":"green"},{"text":"  $(dim) ($(x), $(y), $(z))","color":"white"},{"text":"  需$(lv)级 费$(cost)金","color":"gold"}]
$execute if data storage rpg:portals p$(s){on:0b} run tellraw @s ["",{"text":"[p$(s)] ","color":"dark_gray","bold":true},{"text":"$(name)","color":"gray"},{"text":" 停用","color":"red"},{"text":"  $(dim) ($(x), $(y), $(z))","color":"dark_gray"}]
$execute if data storage rpg:portals p$(s){dt:1} run tellraw @s ["",{"text":"        目标 → ","color":"dark_gray"},{"text":"传送阵 p$(di)","color":"aqua"}]
$execute if data storage rpg:portals p$(s){dt:2} run tellraw @s ["",{"text":"        目标 → ","color":"dark_gray"},{"text":"怪物领域 z$(di)","color":"red"}]
$execute if data storage rpg:portals p$(s){dt:3} run tellraw @s ["",{"text":"        目标 → ","color":"dark_gray"},{"text":"传送点 w$(di)（主城/矿区/竞技场）","color":"yellow"}]
