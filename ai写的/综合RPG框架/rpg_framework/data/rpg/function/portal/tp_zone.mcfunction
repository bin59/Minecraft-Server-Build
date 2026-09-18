# ══ 传送执行宏：目标数据含 dim/cx/cy/cz（怪物领域中心） ══
particle minecraft:portal ~ ~1 ~ 0.6 1 0.6 0.6 80
$execute in $(dim) run tp @s $(cx) $(cy) $(cz)
function rpg:portal/arrive with storage rpg:cfg pmsg
