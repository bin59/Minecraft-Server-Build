# ══ 执行转账（目标 = 16格内最近的其他玩家） ══
tag @s add rpg.payer
execute as @a[distance=0.1..16,limit=1,sort=nearest,tag=!rpg.payer] at @s run function rpg:economy/receive_pay
tag @s remove rpg.payer
