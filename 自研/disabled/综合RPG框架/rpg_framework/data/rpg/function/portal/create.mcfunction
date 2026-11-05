# ══════════════════════════════════════════════════════════
#  管理员：在自己脚下登记并建造一座传送阵
# ══════════════════════════════════════════════════════════
#  用法：/function rpg:portal/create {slot:1,dt:2,di:1,lv:5,cost:20,name:"主城传送阵"}
#    slot 槽位 1~8
#    dt   目标类型：1=另一座传送阵  2=怪物领域中心  3=传送点 w1/w2/w3
#    di   目标编号：dt=1 填传送阵槽位，dt=2 填区域槽位，dt=3 填 1~3
#    lv   等级门槛（0=无门槛）    cost 每次消耗金币（0=免费）
#  也可以走图形界面（推荐）：/function rpg:ui/open_admin
#  注意：建造时会清掉阵台中心 3x3x2 范围内的方块，请选空地执行。
data modify storage rpg:cfg ptmp set value {on:1b,s:1,name:"未命名传送阵",dim:"minecraft:overworld",x:0,y:0,z:0,dt:1,di:1,lv:0,cost:0}
data modify storage rpg:cfg ptmp.dim set from entity @s Dimension
$data modify storage rpg:cfg ptmp.s set value $(slot)
$data modify storage rpg:cfg ptmp.name set value "$(name)"
$data modify storage rpg:cfg ptmp.dt set value $(dt)
$data modify storage rpg:cfg ptmp.di set value $(di)
$data modify storage rpg:cfg ptmp.lv set value $(lv)
$data modify storage rpg:cfg ptmp.cost set value $(cost)
execute store result storage rpg:cfg ptmp.x int 1 run data get entity @s Pos[0]
execute store result storage rpg:cfg ptmp.y int 1 run data get entity @s Pos[1]
execute store result storage rpg:cfg ptmp.z int 1 run data get entity @s Pos[2]
$data modify storage rpg:portals p$(slot) set from storage rpg:cfg ptmp
$function rpg:portal/build with storage rpg:portals p$(slot)
$tellraw @a ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"selector":"@s","color":"aqua"},{"text":" 在此建成了 ","color":"gray"},{"text":"$(name)","color":"light_purple","bold":true},{"text":"（槽位 p$(slot)，目标类型 $(dt) 编号 $(di)，需 $(lv) 级，费用 $(cost) 金）","color":"dark_gray"}]
tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"玩家站上阵台停留 2 秒即可传送。","color":"gray"},{"text":"[查看全部传送阵]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/function rpg:portal/list"}}]
playsound minecraft:block.beacon.activate player @s ~ ~ ~ 1 1.2
