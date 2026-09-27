# ══════════════════════════════════════════════════════════
#  管理员：以自己当前位置为中心创建/覆盖一个怪物领域
# ══════════════════════════════════════════════════════════
#  用法：/function rpg:zone/create {slot:1,r:48,h:32,pool:1,cap:16,name:"哥布林营地"}
#    slot 槽位 1~8   r 水平半径   h 垂直半高   pool 怪物池 1~4   cap 数量上限
#  也可以走图形界面（推荐，带滑块不会填错）：/function rpg:ui/open_admin
# ---- 参数落到分数，方便做边界运算 ----
$scoreboard players set #zr rpg.data $(r)
$scoreboard players set #zh rpg.data $(h)
# ---- 读取执行者所在坐标作为区域中心 ----
execute store result score #zcx rpg.data run data get entity @s Pos[0]
execute store result score #zcy rpg.data run data get entity @s Pos[1]
execute store result score #zcz rpg.data run data get entity @s Pos[2]
# ---- 换算成"最小角 + 边长"，这样每刻判定只需一次体积框选择器 ----
scoreboard players operation #zx1 rpg.data = #zcx rpg.data
scoreboard players operation #zx1 rpg.data -= #zr rpg.data
scoreboard players operation #zz1 rpg.data = #zcz rpg.data
scoreboard players operation #zz1 rpg.data -= #zr rpg.data
scoreboard players operation #zy1 rpg.data = #zcy rpg.data
scoreboard players operation #zy1 rpg.data -= #zh rpg.data
scoreboard players operation #zdx rpg.data = #zr rpg.data
scoreboard players operation #zdx rpg.data *= #c2 rpg.data
scoreboard players operation #zdy rpg.data = #zh rpg.data
scoreboard players operation #zdy rpg.data *= #c2 rpg.data
# ---- 拉回生效半径 = 半径 + 缓冲带 ----
scoreboard players operation #zrp rpg.data = #zr rpg.data
scoreboard players operation #zrp rpg.data += #zone_pad rpg.data
# ---- 组装区域数据 ----
data modify storage rpg:cfg tmp set value {on:1b,s:1,name:"未命名领域",dim:"minecraft:overworld",cx:0,cy:0,cz:0,r:16,h:16,x1:0,y1:0,z1:0,dx:32,dy:32,dz:32,rp:40,pool:1,cap:12}
data modify storage rpg:cfg tmp.dim set from entity @s Dimension
$data modify storage rpg:cfg tmp.s set value $(slot)
$data modify storage rpg:cfg tmp.name set value "$(name)"
$data modify storage rpg:cfg tmp.r set value $(r)
$data modify storage rpg:cfg tmp.h set value $(h)
$data modify storage rpg:cfg tmp.pool set value $(pool)
$data modify storage rpg:cfg tmp.cap set value $(cap)
execute store result storage rpg:cfg tmp.cx int 1 run scoreboard players get #zcx rpg.data
execute store result storage rpg:cfg tmp.cy int 1 run scoreboard players get #zcy rpg.data
execute store result storage rpg:cfg tmp.cz int 1 run scoreboard players get #zcz rpg.data
execute store result storage rpg:cfg tmp.x1 int 1 run scoreboard players get #zx1 rpg.data
execute store result storage rpg:cfg tmp.y1 int 1 run scoreboard players get #zy1 rpg.data
execute store result storage rpg:cfg tmp.z1 int 1 run scoreboard players get #zz1 rpg.data
execute store result storage rpg:cfg tmp.dx int 1 run scoreboard players get #zdx rpg.data
execute store result storage rpg:cfg tmp.dy int 1 run scoreboard players get #zdy rpg.data
execute store result storage rpg:cfg tmp.dz int 1 run scoreboard players get #zdx rpg.data
execute store result storage rpg:cfg tmp.rp int 1 run scoreboard players get #zrp rpg.data
# ---- 写入槽位 ----
$data modify storage rpg:zones z$(slot) set from storage rpg:cfg tmp
# ---- 反馈 ----
$tellraw @a ["",{"text":"[领域] ","color":"gold","bold":true},{"selector":"@s","color":"aqua"},{"text":" 设置了 ","color":"gray"},{"text":"z$(slot)","color":"yellow"},{"text":" 号怪物领域 ","color":"gray"},{"text":"$(name)","color":"light_purple","bold":true},{"text":"（半径 $(r)，垂直 ±$(h)，怪物池 $(pool)，上限 $(cap)）","color":"dark_gray"}]
execute unless score #zone_on rpg.data matches 1 run tellraw @s ["",{"text":"[领域] ","color":"gold"},{"text":"提醒：区域系统当前处于","color":"gray"},{"text":"停用","color":"red","bold":true},{"text":"状态，执行 ","color":"gray"},{"text":"/function rpg:zone/on","color":"aqua","click_event":{"action":"suggest_command","command":"/function rpg:zone/on"}},{"text":" 才会开始生效","color":"gray"}]
playsound minecraft:block.beacon.activate player @s ~ ~ ~ 1 1.4
