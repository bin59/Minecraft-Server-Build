# ══ 区域束缚宏 ══
# 把"已在区域外、但仍在缓冲带 rp 内"的怪拽回区域中心，避免玩家把怪引出领域。
# BOSS / 精英 / 服主保护标记(rpg.zkeep) 一律跳过 —— 它们由 BOSS 系统自己管，互不干扰。
$execute in $(dim) positioned $(cx) $(cy) $(cz) run tag @e[type=#rpg:zone_mobs,tag=!rpg.zin,tag=!rpg.zkeep,tag=!rpg.boss,tag=!rpg.elite,distance=..$(rp)] add rpg.zpull
$execute in $(dim) run tp @e[tag=rpg.zpull] $(cx) $(cy) $(cz)
$execute in $(dim) positioned $(cx) $(cy) $(cz) run particle minecraft:reverse_portal ~ ~1 ~ 1.5 1 1.5 0.05 30
tag @e[tag=rpg.zpull] add rpg.zin
tag @e[tag=rpg.zpull] remove rpg.zpull
