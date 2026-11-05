# ══ 按 #zone_batch 配置重复尝试刷怪（1~4 次） ══
function rpg:zone/try
execute if score #zone_batch rpg.data matches 2.. run function rpg:zone/try
execute if score #zone_batch rpg.data matches 3.. run function rpg:zone/try
execute if score #zone_batch rpg.data matches 4.. run function rpg:zone/try
