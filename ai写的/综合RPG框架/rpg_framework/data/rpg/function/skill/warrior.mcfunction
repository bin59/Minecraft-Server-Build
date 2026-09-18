# ══ 战士技能：冲锋怒吼（力量+抗性+震飞周围敌人） ══
scoreboard players set @s rpg.cd 160
effect give @s minecraft:strength 8 0 true
effect give @s minecraft:resistance 8 0 true
effect give @e[type=!minecraft:player,distance=..5] minecraft:levitation 1 3 false
execute at @s run particle minecraft:angry_villager ~ ~1 ~ 1.2 0.8 1.2 0.1 25
execute at @s run playsound minecraft:entity.ravager.roar player @a ~ ~ ~ 1 1
tellraw @s ["",{"text":"[技能] ","color":"red"},{"text":"冲锋怒吼！","color":"red","bold":true},{"text":" 8秒内力量+1、抗性+1，并震飞周围敌人","color":"gray"}]
