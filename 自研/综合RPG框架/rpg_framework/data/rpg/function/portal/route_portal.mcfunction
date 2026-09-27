# ══ 目标 = 另一座传送阵 ══
$execute unless data storage rpg:portals p$(i) run function rpg:portal/deny_dest
$execute if data storage rpg:portals p$(i) run function rpg:portal/tp_xyz with storage rpg:portals p$(i)
