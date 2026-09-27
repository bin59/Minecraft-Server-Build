# ══════════════════════════════════════════════════════════
#  怪物池 3 · 下界熔狱（高难度，适合下界维度的区域）
# ══════════════════════════════════════════════════════════
execute store result score #zr rpg.data run random value 1..4
execute if score #zr rpg.data matches 1 run summon minecraft:blaze ~ ~1 ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,CustomName:'{"text":"熔核烈焰人","color":"gold"}',Health:30.0f,attributes:[{id:"minecraft:max_health",base:30.0d}]}
execute if score #zr rpg.data matches 2 run summon minecraft:magma_cube ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,Size:2}
execute if score #zr rpg.data matches 3 run summon minecraft:wither_skeleton ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,CustomName:'{"text":"熔狱骨将","color":"dark_red"}',Health:40.0f,attributes:[{id:"minecraft:max_health",base:40.0d},{id:"minecraft:attack_damage",base:10.0d}]}
execute if score #zr rpg.data matches 4 run summon minecraft:hoglin ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,CustomName:'{"text":"狂暴疣猪兽","color":"red"}',Health:45.0f,IsImmuneToZombification:1b,attributes:[{id:"minecraft:max_health",base:45.0d},{id:"minecraft:attack_damage",base:9.0d}]}
