# ══ 传送执行宏：目标数据含 dim/x/y/z（传送阵、传送点通用） ══
particle minecraft:portal ~ ~1 ~ 0.6 1 0.6 0.6 80
$execute in $(dim) run tp @s $(x) $(y) $(z)
function rpg:portal/arrive with storage rpg:cfg pmsg
