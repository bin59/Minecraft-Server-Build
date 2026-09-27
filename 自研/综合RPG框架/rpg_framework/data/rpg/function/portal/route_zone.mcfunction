# ══ 目标 = 怪物领域中心（只读区域坐标，不触碰任何区域判定逻辑） ══
$execute unless data storage rpg:zones z$(i) run function rpg:portal/deny_dest
$execute if data storage rpg:zones z$(i) run function rpg:portal/tp_zone with storage rpg:zones z$(i)
