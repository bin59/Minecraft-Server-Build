# ══ 购买精英召集令（精英挑战道具） —— 100金 ══
execute if score @s rpg.gold matches ..99 run return run tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"金币不足（需要100金）","color":"red"}]
scoreboard players remove @s rpg.gold 100
give @s minecraft:blaze_rod[minecraft:custom_name='{"text":"精英召集令","color":"gold","italic":false}',minecraft:lore=['{"text":"燃烧着怒火的召集令","color":"gray","italic":false}','{"text":"使用：/trigger rpg.elite 召唤3只精英","color":"dark_gray","italic":false}','{"text":"召唤后冷却5分钟","color":"dark_gray","italic":false}'],minecraft:custom_data={rpg_elite_summon:1b},minecraft:enchantment_glint_override=true] 1
tellraw @s ["",{"text":"[商店] ","color":"gold"},{"text":"购买成功：","color":"green"},{"text":"精英召集令","color":"gold","bold":true},{"text":"（精英挑战道具）","color":"gray"}]
playsound minecraft:entity.villager.yes player @s ~ ~ ~ 1 0.8
