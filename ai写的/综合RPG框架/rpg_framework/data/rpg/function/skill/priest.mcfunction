# ══ 牧师技能：圣光普照（治疗周围所有玩家） ══
scoreboard players set @s rpg.cd 180
execute if entity @s[tag=rpg.c_mage] run scoreboard players set @s rpg.cd 126
effect give @a[distance=..8] minecraft:instant_health 1 1 true
effect give @a[distance=..8] minecraft:regeneration 6 0 true
particle minecraft:heart ~ ~2 ~ 1.5 0.8 1.5 0.2 20
particle minecraft:happy_villager ~ ~1 ~ 2 0.6 2 0.1 30
playsound minecraft:entity.player.levelup player @a ~ ~ ~ 1 1.5
tellraw @a[distance=..8] ["",{"text":"[技能] ","color":"yellow"},{"text":"圣光普照！","color":"yellow","bold":true},{"text":" 你被圣光治愈了","color":"gray"}]
