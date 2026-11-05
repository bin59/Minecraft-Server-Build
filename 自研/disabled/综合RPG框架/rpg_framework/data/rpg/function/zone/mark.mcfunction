# ══ 区域标记宏 ══
# 参数来自 storage rpg:zones zN，用 x1/y1/z1 最小角 + dx/dy/dz 边长框出长方体。
# dx/dy/dz 选择器是"体积框"判定，比 distance 的球形更符合服主对"区域"的直觉。
$execute in $(dim) positioned $(x1) $(y1) $(z1) run tag @e[type=#rpg:zone_mobs,dx=$(dx),dy=$(dy),dz=$(dz)] add rpg.zin
$execute in $(dim) positioned $(x1) $(y1) $(z1) run tag @a[dx=$(dx),dy=$(dy),dz=$(dz),gamemode=!spectator] add rpg.zin_p
