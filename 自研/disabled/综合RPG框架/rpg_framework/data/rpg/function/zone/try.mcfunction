# ══ 单次刷怪尝试（执行者=锚点玩家，执行位置=其所在方块） ══
execute if score #zcount rpg.data >= #zcap rpg.data run return 0
# 随机水平偏移：距离取 7~15 格，正负号各自随机（#cneg 常量 = -1）
execute store result score #zox rpg.data run random value 7..15
execute store result score #zoz rpg.data run random value 7..15
execute store result score #ztmp rpg.data run random value 0..1
execute if score #ztmp rpg.data matches 0 run scoreboard players operation #zox rpg.data *= #cneg rpg.data
execute store result score #ztmp rpg.data run random value 0..1
execute if score #ztmp rpg.data matches 0 run scoreboard players operation #zoz rpg.data *= #cneg rpg.data
# 偏移量要参与坐标运算，只能走宏；先把分数落到 storage
data modify storage rpg:cfg off set value {ox:0,oz:0}
execute store result storage rpg:cfg off.ox int 1 run scoreboard players get #zox rpg.data
execute store result storage rpg:cfg off.oz int 1 run scoreboard players get #zoz rpg.data
function rpg:zone/place with storage rpg:cfg off
