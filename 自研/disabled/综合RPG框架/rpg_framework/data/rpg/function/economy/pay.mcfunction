# ══ 转账入口 ══
# 用法：/trigger rpg.pay set 100  （转给离自己最近的玩家）
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.pay
scoreboard players reset @s rpg.pay
execute if score rpg.tmp_holder rpg.tmp matches ..0 run return run tellraw @s ["",{"text":"[银行] ","color":"gold"},{"text":"转账金额必须大于0","color":"red"}]
execute if score @s rpg.gold < rpg.tmp_holder rpg.tmp run return run tellraw @s ["",{"text":"[银行] ","color":"gold"},{"text":"金币不足，无法转账","color":"red"}]
execute positioned ~ ~ ~ if entity @a[distance=0.1..16,limit=1,sort=nearest] run function rpg:economy/do_pay
execute positioned ~ ~ ~ unless entity @a[distance=0.1..16,limit=1,sort=nearest] run tellraw @s ["",{"text":"[银行] ","color":"gold"},{"text":"16格内没有可转账的玩家","color":"red"}]
