# ══ 任务面板 ══
scoreboard players reset @s rpg.quest
tellraw @s ["",{"text":"╔═══ ","color":"gold"},{"text":"任务面板","color":"yellow","bold":true},{"text":" ═══╗","color":"gold"}]
# ── 任务1：讨伐僵尸 ──
execute if score @s rpg.q1s matches 0 run tellraw @s ["",{"text":"① ","color":"white"},{"text":"讨伐僵尸","color":"yellow"},{"text":" 击杀10只僵尸 | 奖励200金+150经验 ","color":"gray"},{"text":"[接受]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qacc set 1"},"hover_event":{"action":"show_text","value":"点击接受任务"}}]
execute if score @s rpg.q1s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q1z
execute if score @s rpg.q1s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q1base
execute if score @s rpg.q1s matches 1 if score rpg.tmp_holder rpg.tmp matches 11.. run scoreboard players set rpg.tmp_holder rpg.tmp 10
execute if score @s rpg.q1s matches 1 run tellraw @s ["",{"text":"① ","color":"white"},{"text":"讨伐僵尸","color":"yellow"},{"text":" 进行中：","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"aqua","bold":true},{"text":"/10","color":"gray"}]
execute if score @s rpg.q1s matches 2 run tellraw @s ["",{"text":"① ","color":"white"},{"text":"讨伐僵尸","color":"green"},{"text":" 已达标 ","color":"green"},{"text":"[交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 1"},"hover_event":{"action":"show_text","value":"点击交付任务"}}]
# ── 任务2：收集铁锭 ──
execute if score @s rpg.q2s matches 0 run tellraw @s ["",{"text":"② ","color":"white"},{"text":"收集铁锭","color":"yellow"},{"text":" 拾取16个铁锭 | 奖励150金+80经验 ","color":"gray"},{"text":"[接受]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qacc set 2"},"hover_event":{"action":"show_text","value":"点击接受任务"}}]
execute if score @s rpg.q2s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q2i
execute if score @s rpg.q2s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q2base
execute if score @s rpg.q2s matches 1 if score rpg.tmp_holder rpg.tmp matches 17.. run scoreboard players set rpg.tmp_holder rpg.tmp 16
execute if score @s rpg.q2s matches 1 run tellraw @s ["",{"text":"② ","color":"white"},{"text":"收集铁锭","color":"yellow"},{"text":" 进行中：","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"aqua","bold":true},{"text":"/16","color":"gray"}]
execute if score @s rpg.q2s matches 2 run tellraw @s ["",{"text":"② ","color":"white"},{"text":"收集铁锭","color":"green"},{"text":" 已达标 ","color":"green"},{"text":"[交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 2"},"hover_event":{"action":"show_text","value":"点击交付任务"}}]
# ── 任务3：骷髅猎手 ──
execute if score @s rpg.q3s matches 0 run tellraw @s ["",{"text":"③ ","color":"white"},{"text":"骷髅猎手","color":"yellow"},{"text":" 击杀8只骷髅 | 奖励250金+200经验 ","color":"gray"},{"text":"[接受]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qacc set 3"},"hover_event":{"action":"show_text","value":"点击接受任务"}}]
execute if score @s rpg.q3s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q3k
execute if score @s rpg.q3s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q3base
execute if score @s rpg.q3s matches 1 if score rpg.tmp_holder rpg.tmp matches 9.. run scoreboard players set rpg.tmp_holder rpg.tmp 8
execute if score @s rpg.q3s matches 1 run tellraw @s ["",{"text":"③ ","color":"white"},{"text":"骷髅猎手","color":"yellow"},{"text":" 进行中：","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"aqua","bold":true},{"text":"/8","color":"gray"}]
execute if score @s rpg.q3s matches 2 run tellraw @s ["",{"text":"③ ","color":"white"},{"text":"骷髅猎手","color":"green"},{"text":" 已达标 ","color":"green"},{"text":"[交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 3"},"hover_event":{"action":"show_text","value":"点击交付任务"}}]
# ── 每日任务 ──
tellraw @s ["",{"text":"──── ","color":"gold"},{"text":"每日任务（每日刷新）","color":"yellow","bold":true},{"text":" ────","color":"gold"}]
execute if score @s rpg.q4s matches 0 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"讨伐怪物","color":"yellow"},{"text":" 击杀15只怪物 | 奖励300金+100经验 ","color":"gray"},{"text":"[接受]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qacc set 11"}}]
execute if score @s rpg.q4s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.kills
execute if score @s rpg.q4s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q4base
execute if score @s rpg.q4s matches 1 if score rpg.tmp_holder rpg.tmp matches 16.. run scoreboard players set rpg.tmp_holder rpg.tmp 15
execute if score @s rpg.q4s matches 1 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"讨伐怪物","color":"yellow"},{"text":" 进行中：","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"aqua","bold":true},{"text":"/15","color":"gray"}]
execute if score @s rpg.q4s matches 2 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"讨伐怪物","color":"green"},{"text":" 已达标 ","color":"green"},{"text":"[交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 11"}}]
execute if score @s rpg.q4s matches 3 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"讨伐怪物","color":"dark_gray"},{"text":" 已完成（明日刷新）","color":"dark_gray"}]
execute if score @s rpg.q5s matches 0 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"采石工人","color":"yellow"},{"text":" 开采20个石头 | 奖励300金+100经验 ","color":"gray"},{"text":"[接受]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qacc set 12"}}]
execute if score @s rpg.q5s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q5m
execute if score @s rpg.q5s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q5base
execute if score @s rpg.q5s matches 1 if score rpg.tmp_holder rpg.tmp matches 21.. run scoreboard players set rpg.tmp_holder rpg.tmp 20
execute if score @s rpg.q5s matches 1 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"采石工人","color":"yellow"},{"text":" 进行中：","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"aqua","bold":true},{"text":"/20","color":"gray"}]
execute if score @s rpg.q5s matches 2 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"采石工人","color":"green"},{"text":" 已达标 ","color":"green"},{"text":"[交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 12"}}]
execute if score @s rpg.q5s matches 3 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"采石工人","color":"dark_gray"},{"text":" 已完成（明日刷新）","color":"dark_gray"}]
execute if score @s rpg.q6s matches 0 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"绿宝石猎人","color":"yellow"},{"text":" 拾取5个绿宝石 | 奖励300金+100经验 ","color":"gray"},{"text":"[接受]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qacc set 13"}}]
execute if score @s rpg.q6s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.q6e
execute if score @s rpg.q6s matches 1 run scoreboard players operation rpg.tmp_holder rpg.tmp -= @s rpg.q6base
execute if score @s rpg.q6s matches 1 if score rpg.tmp_holder rpg.tmp matches 6.. run scoreboard players set rpg.tmp_holder rpg.tmp 5
execute if score @s rpg.q6s matches 1 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"绿宝石猎人","color":"yellow"},{"text":" 进行中：","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"aqua","bold":true},{"text":"/5","color":"gray"}]
execute if score @s rpg.q6s matches 2 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"绿宝石猎人","color":"green"},{"text":" 已达标 ","color":"green"},{"text":"[交付]","color":"gold","bold":true,"click_event":{"action":"run_command","command":"/trigger rpg.qdone set 13"}}]
execute if score @s rpg.q6s matches 3 run tellraw @s ["",{"text":"★ ","color":"gold"},{"text":"绿宝石猎人","color":"dark_gray"},{"text":" 已完成（明日刷新）","color":"dark_gray"}]
tellraw @s ["",{"text":"╚══════════════════════╝","color":"gold"}]
