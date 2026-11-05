# ══ 目标 = 初版传送系统的传送点 w1/w2/w3 ══
$execute unless data storage rpg:warps w$(i) run function rpg:portal/deny_dest
$execute if data storage rpg:warps w$(i) run function rpg:portal/tp_xyz with storage rpg:warps w$(i)
