# ══ 弓箭手技能：箭雨（向天空射出箭矢，落下覆盖敌人） ══
scoreboard players set @s rpg.cd 140
summon minecraft:arrow ~ ~2.5 ~ {Motion:[0.7d,0.9d,0.3d],pickup:0b}
summon minecraft:arrow ~ ~2.5 ~ {Motion:[-0.5d,1.1d,0.6d],pickup:0b}
summon minecraft:arrow ~ ~2.5 ~ {Motion:[0.2d,1.2d,-0.7d],pickup:0b}
summon minecraft:arrow ~ ~2.5 ~ {Motion:[-0.6d,0.8d,-0.4d],pickup:0b}
summon minecraft:arrow ~ ~2.5 ~ {Motion:[0.1d,1.0d,0.9d],pickup:0b}
summon minecraft:arrow ~ ~2.5 ~ {Motion:[-0.8d,1.0d,0.1d],pickup:0b}
particle minecraft:crit ~ ~2 ~ 0.5 0.5 0.5 0.2 30
playsound minecraft:entity.arrow.shoot player @a ~ ~ ~ 1 0.7
tellraw @s ["",{"text":"[技能] ","color":"green"},{"text":"箭雨！","color":"green","bold":true},{"text":" 六支箭矢从天而降","color":"gray"}]
