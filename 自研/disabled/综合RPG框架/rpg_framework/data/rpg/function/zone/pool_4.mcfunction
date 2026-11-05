# ══════════════════════════════════════════════════════════
#  怪物池 4 · 虚空深渊（末地风格，机动性强）
# ══════════════════════════════════════════════════════════
execute store result score #zr rpg.data run random value 1..4
execute if score #zr rpg.data matches 1 run summon minecraft:enderman ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,CustomName:'{"text":"虚空行者","color":"dark_purple"}',Health:45.0f,attributes:[{id:"minecraft:max_health",base:45.0d},{id:"minecraft:attack_damage",base:9.0d}]}
execute if score #zr rpg.data matches 2 run summon minecraft:endermite ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false}
execute if score #zr rpg.data matches 3 run summon minecraft:silverfish ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false}
execute if score #zr rpg.data matches 4 run summon minecraft:creeper ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,CustomName:'{"text":"虚空爆体","color":"dark_purple"}',active_effects:[{id:"minecraft:speed",amplifier:1b,duration:1000000,show_particles:false}]}
