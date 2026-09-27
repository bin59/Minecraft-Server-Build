# ══ 充能完成：按玩家所处槽位号取出对应传送阵数据 ══
scoreboard players set @s rpg.pcharge 0
data modify storage rpg:cfg pick set value {i:1}
execute store result storage rpg:cfg pick.i int 1 run scoreboard players get @s rpg.pnow
function rpg:portal/fire_mac with storage rpg:cfg pick
