# ══ 法师技能：圣火流星（在视线前方召唤坠落的圣火，不破坏地形） ══
scoreboard players set @s rpg.cd 100
execute if entity @s[tag=rpg.c_mage] run scoreboard players set @s rpg.cd 70
execute at @s anchored eyes run summon minecraft:small_fireball ^2 ^8 ^8 {Motion:[0.0d,-0.9d,0.0d]}
execute at @s anchored eyes run summon minecraft:small_fireball ^-3 ^10 ^12 {Motion:[0.0d,-0.9d,0.0d]}
execute at @s anchored eyes run summon minecraft:small_fireball ^1 ^9 ^16 {Motion:[0.0d,-0.9d,0.0d]}
particle minecraft:flame ~ ~2 ~ 0.6 0.6 0.6 0.03 30
playsound minecraft:entity.blaze.shoot player @a ~ ~ ~ 1 0.8
playsound minecraft:entity.blaze.shoot player @a ~ ~ ~ 1 1.2
tellraw @s ["",{"text":"[技能] ","color":"light_purple"},{"text":"圣火流星！","color":"light_purple","bold":true},{"text":" 三团圣火砸向前方区域","color":"gray"}]
