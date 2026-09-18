# ══ 传送阵使用判定（@s = 玩家） ══
$scoreboard players set #plv rpg.data $(lv)
$scoreboard players set #pcost rpg.data $(cost)
$scoreboard players set #pdt rpg.data $(dt)
$scoreboard players set #pdi rpg.data $(di)
$data modify storage rpg:cfg pmsg.name set value "$(name)"
# ---- 门槛检查：等级 → 金币 ----
execute unless score @s rpg.level >= #plv rpg.data run return run function rpg:portal/deny_lv
execute unless score @s rpg.gold >= #pcost rpg.data run return run function rpg:portal/deny_gold
# ---- 扣费并进入冷却 ----
scoreboard players operation @s rpg.gold -= #pcost rpg.data
scoreboard players operation @s rpg.pcd = #portal_cd rpg.data
execute if score #pcost rpg.data matches 1.. run tellraw @s ["",{"text":"[传送阵] ","color":"light_purple"},{"text":"消耗 ","color":"gray"},{"score":{"name":"#pcost","objective":"rpg.data"},"color":"gold","bold":true},{"text":" 金币","color":"gray"}]
function rpg:portal/route
