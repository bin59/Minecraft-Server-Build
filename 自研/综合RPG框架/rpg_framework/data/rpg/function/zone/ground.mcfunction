# ══ 落脚点搜索：在当前坐标柱上下 3 格内逐个高度试探 ══
# 找到一个就置 #zok=1 并停止，找不到就本轮放弃（下个节拍会重新随机点位）
scoreboard players set #zok rpg.data 0
execute if score #zok rpg.data matches 0 run function rpg:zone/ground_try
execute if score #zok rpg.data matches 0 positioned ~ ~1 ~ run function rpg:zone/ground_try
execute if score #zok rpg.data matches 0 positioned ~ ~-1 ~ run function rpg:zone/ground_try
execute if score #zok rpg.data matches 0 positioned ~ ~2 ~ run function rpg:zone/ground_try
execute if score #zok rpg.data matches 0 positioned ~ ~-2 ~ run function rpg:zone/ground_try
execute if score #zok rpg.data matches 0 positioned ~ ~3 ~ run function rpg:zone/ground_try
execute if score #zok rpg.data matches 0 positioned ~ ~-3 ~ run function rpg:zone/ground_try
