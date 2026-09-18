# ══ 击杀怪物结算（本刻总击杀 - 已结算击杀 = 新增击杀） ══
scoreboard players operation #diff rpg.tmp = @s rpg.kills
scoreboard players operation #diff rpg.tmp -= @s rpg.k_done
scoreboard players operation @s rpg.k_done += #diff rpg.tmp
# ---- 奖励开关：总开关 与 击杀掉落分项 任一关闭则不发放 ----
execute unless score #rw_master rpg.data matches 1 run return 0
execute unless score #rw_kill rpg.data matches 1 run return 0
execute unless score #diff rpg.tmp matches 1.. run return 0
scoreboard players operation @s rpg.exp += #diff rpg.tmp
scoreboard players operation @s rpg.gold += #diff rpg.tmp
title @s actionbar {"text":"","extra":[{"text":"+","color":"aqua"},{"score":{"name":"#diff","objective":"rpg.tmp"},"color":"aqua"},{"text":" 经验  ","color":"aqua"},{"text":"+","color":"gold"},{"score":{"name":"#diff","objective":"rpg.tmp"},"color":"gold"},{"text":" 金币","color":"gold"}]}
playsound minecraft:entity.experience_orb.pickup player @s
