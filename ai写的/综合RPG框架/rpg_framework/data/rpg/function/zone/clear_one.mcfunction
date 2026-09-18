# ══ 清空单个区域内由本框架刷出的怪（不动原版怪、不动BOSS/精英） ══
$execute in $(dim) positioned $(x1) $(y1) $(z1) run kill @e[type=#rpg:zone_mobs,tag=rpg.zmob,tag=!rpg.zkeep,tag=!rpg.boss,tag=!rpg.elite,dx=$(dx),dy=$(dy),dz=$(dz)]
