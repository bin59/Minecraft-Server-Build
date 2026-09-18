# ══ 玩家入口分发：/trigger rpg.portal ══
#   set 1（默认）→ 打开传送阵面板
#   set 2         → 立即激活脚下的传送阵（跳过充能）
execute if score @s rpg.portal matches 2 run function rpg:portal/instant
execute unless score @s rpg.portal matches 2 run function rpg:portal/menu
scoreboard players reset @s rpg.portal
