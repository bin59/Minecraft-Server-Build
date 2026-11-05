# ══ 接收转账（@s = 收款人，rpg.tmp_holder = 金额） ══
execute if score @s rpg.gold matches ..-1 run return run function rpg:economy/pay_fail
execute as @a[tag=rpg.payer] at @s run scoreboard players operation @s rpg.gold -= rpg.tmp_holder rpg.tmp
scoreboard players operation @s rpg.gold += rpg.tmp_holder rpg.tmp
tellraw @s ["",{"text":"[银行] ","color":"gold"},{"selector":"@a[tag=rpg.payer]","color":"aqua","bold":true},{"text":" 向你转账 ","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"gold","bold":true},{"text":" 金币","color":"gray"}]
tellraw @a[tag=rpg.payer] ["",{"text":"[银行] ","color":"gold"},{"text":"转账成功，已向 ","color":"gray"},{"selector":"@s","color":"aqua","bold":true},{"text":" 转出 ","color":"gray"},{"score":{"name":"rpg.tmp_holder","objective":"rpg.tmp"},"color":"gold","bold":true},{"text":" 金币","color":"gray"}]
playsound minecraft:block.anvil.use player @s ~ ~ ~ 0.5 1.5
