# ══ 落脚点校验：脚位与头位可通行 + 脚下是实心方块 ══
# 用"不属于 #rpg:zone_clear"来判定实心，避免穷举全部固体方块
execute unless block ~ ~ ~ #rpg:zone_clear run return 0
execute unless block ~ ~1 ~ #rpg:zone_clear run return 0
execute if block ~ ~-1 ~ #rpg:zone_clear run return 0
scoreboard players set #zok rpg.data 1
function rpg:zone/summon
