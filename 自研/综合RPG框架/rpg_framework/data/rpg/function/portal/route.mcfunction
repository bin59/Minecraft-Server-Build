# ══ 目标解析（@s = 玩家） ══
#  dt=1 另一座传送阵     di = 传送阵槽位 1~8
#  dt=2 怪物领域中心     di = 区域槽位 1~8   ← 唯一与领域系统的交集，且只读坐标
#  dt=3 传送点 w1/w2/w3  di = 1~3           ← 复用初版传送系统已设好的点
data modify storage rpg:cfg pick set value {i:1}
execute store result storage rpg:cfg pick.i int 1 run scoreboard players get #pdi rpg.data
execute if score #pdt rpg.data matches 1 run function rpg:portal/route_portal with storage rpg:cfg pick
execute if score #pdt rpg.data matches 2 run function rpg:portal/route_zone with storage rpg:cfg pick
execute if score #pdt rpg.data matches 3 run function rpg:portal/route_warp with storage rpg:cfg pick
execute unless score #pdt rpg.data matches 1..3 run function rpg:portal/deny_dest
