# ══ 单个区域信息行（宏，由 zone/list 逐槽位调用） ══
scoreboard players add #zn rpg.data 1
$execute if data storage rpg:zones z$(s){on:1b} run tellraw @s ["",{"text":"[z$(s)] ","color":"gold","bold":true},{"text":"$(name)","color":"light_purple","bold":true},{"text":" 启用","color":"green"},{"text":"  $(dim) ($(cx), $(cy), $(cz))","color":"white"},{"text":"  半径$(r) 垂直±$(h) 怪物池$(pool) 上限$(cap)","color":"dark_gray"}]
$execute if data storage rpg:zones z$(s){on:0b} run tellraw @s ["",{"text":"[z$(s)] ","color":"dark_gray","bold":true},{"text":"$(name)","color":"gray"},{"text":" 停用","color":"red"},{"text":"  $(dim) ($(cx), $(cy), $(cz))","color":"dark_gray"},{"text":"  半径$(r) 垂直±$(h) 怪物池$(pool) 上限$(cap)","color":"dark_gray"}]
