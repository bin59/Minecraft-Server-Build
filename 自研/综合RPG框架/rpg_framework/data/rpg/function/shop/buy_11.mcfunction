# ══ 购买深渊之眼（BOSS召唤道具） —— 300金 ══
execute if score @s rpg.gold matches ..299 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要300金）","color":"red"}]
scoreboard players remove @s rpg.gold 300
give @s minecraft:ender_eye[minecraft:custom_name='{"text":"深渊之眼","color":"light_purple","italic":false}',minecraft:lore=['{"text":"凝视深渊的禁忌之物","color":"gray","italic":false}','{"text":"使用：/trigger rpg.boss 召唤深渊领主","color":"dark_gray","italic":false}','{"text":"召唤后冷却10分钟","color":"dark_gray","italic":false}'],minecraft:custom_data={rpg_boss_summon:1b},minecraft:enchantment_glint_override=true] 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：","color":"green"},{"text":"深渊之眼","color":"light_purple","bold":true},{"text":"（BOSS召唤道具）","color":"gray"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 0.8
