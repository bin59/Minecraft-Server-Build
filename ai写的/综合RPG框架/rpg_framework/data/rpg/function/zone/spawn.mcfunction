# ══ 单区域刷怪宏 ══
# 三道闸门：区域内必须有非旁观玩家（无人区不堆怪）→ 未达数量上限 → 找到合法落脚点
# 锚点取区域内随机一名玩家，在其 7~15 格外择地生成，既保证玩家能遇到怪，又不贴脸刷。
$scoreboard players set #zcap rpg.data $(cap)
$scoreboard players set #zpool rpg.data $(pool)
$execute in $(dim) positioned $(x1) $(y1) $(z1) store result score #zcount rpg.data if entity @e[type=#rpg:zone_mobs,dx=$(dx),dy=$(dy),dz=$(dz)]
$execute in $(dim) positioned $(x1) $(y1) $(z1) store result score #zpl rpg.data if entity @a[dx=$(dx),dy=$(dy),dz=$(dz),gamemode=!spectator]
execute if score #zpl rpg.data matches 0 run return 0
execute if score #zcount rpg.data >= #zcap rpg.data run return 0
$execute in $(dim) positioned $(x1) $(y1) $(z1) as @a[dx=$(dx),dy=$(dy),dz=$(dz),gamemode=!spectator,sort=random,limit=1] at @s run function rpg:zone/try_batch
# 出生点自检：随机偏移有可能把怪甩到区域外，这里立刻把越界的新怪清掉。
# 通过校验的新怪直接补上 rpg.zin，避免本轮 purge 误杀刚出生的怪。
$execute in $(dim) positioned $(x1) $(y1) $(z1) run tag @e[tag=rpg.znew,dx=$(dx),dy=$(dy),dz=$(dz)] add rpg.zin
kill @e[tag=rpg.znew,tag=!rpg.zin]
tag @e[tag=rpg.znew] remove rpg.znew
