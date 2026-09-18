# ══ 职业选择菜单 ══
scoreboard players reset @s rpg.cls
tellraw @s ["",{"text":"╔═══ ","color":"gold"},{"text":"职业选择","color":"yellow","bold":true},{"text":" ═══╗","color":"gold"}]
tellraw @s ["",{"text":"▶ ","color":"gray"},{"text":"战士","color":"red","bold":true},{"text":" · 攻击+3 生命+4 | 技能：冲锋怒吼","color":"gray"},{"text":" [选择]","color":"aqua","click_event":{"action":"suggest_command","command":"/trigger rpg.pick set 1"},"hover_event":{"action":"show_text","value":"点击选择战士"}}]
tellraw @s ["",{"text":"▶ ","color":"gray"},{"text":"弓箭手","color":"green","bold":true},{"text":" · 移速+15% | 技能：箭雨","color":"gray"},{"text":" [选择]","color":"aqua","click_event":{"action":"suggest_command","command":"/trigger rpg.pick set 2"},"hover_event":{"action":"show_text","value":"点击选择弓箭手"}}]
tellraw @s ["",{"text":"▶ ","color":"gray"},{"text":"法师","color":"light_purple","bold":true},{"text":" · 技能冷却-30% | 技能：圣火流星","color":"gray"},{"text":" [选择]","color":"aqua","click_event":{"action":"suggest_command","command":"/trigger rpg.pick set 3"},"hover_event":{"action":"show_text","value":"点击选择法师"}}]
tellraw @s ["",{"text":"▶ ","color":"gray"},{"text":"牧师","color":"yellow","bold":true},{"text":" · 持续回血 | 技能：圣光普照","color":"gray"},{"text":" [选择]","color":"aqua","click_event":{"action":"suggest_command","command":"/trigger rpg.pick set 4"},"hover_event":{"action":"show_text","value":"点击选择牧师"}}]
tellraw @s ["",{"text":"╚════════════════╝","color":"gold"}]
tellraw @s ["",{"text":"注意：职业一经选择无法更改（管理员可重置）","color":"dark_gray","italic":true}]
