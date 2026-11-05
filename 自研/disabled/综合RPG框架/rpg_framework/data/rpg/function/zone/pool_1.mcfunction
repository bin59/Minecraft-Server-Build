# ══════════════════════════════════════════════════════════
#  怪物池 1 · 常规地表（可直接编辑本文件调整种类与属性）
# ══════════════════════════════════════════════════════════
#  改这个文件时只要守住两条规则：
#   1) 必须带 Tags:["rpg.zmob","rpg.znew"]
#      rpg.zmob = 标记"本框架刷出的怪"（宽松模式只管制这一类）
#      rpg.znew = 供出生点自检使用，一轮结束后自动移除
#   2) 怪物类型必须在 rpg:zone_mobs 实体标签里，否则不受区域约束
#  防火效果是为了让亡灵白天也能战斗，不加会在地表被晒死。
execute store result score #zr rpg.data run random value 1..4
execute if score #zr rpg.data matches 1 run summon minecraft:zombie ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,active_effects:[{id:"minecraft:fire_resistance",amplifier:0b,duration:1000000,show_particles:false}]}
execute if score #zr rpg.data matches 2 run summon minecraft:skeleton ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false,active_effects:[{id:"minecraft:fire_resistance",amplifier:0b,duration:1000000,show_particles:false}]}
execute if score #zr rpg.data matches 3 run summon minecraft:spider ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false}
execute if score #zr rpg.data matches 4 run summon minecraft:husk ~ ~ ~ {Tags:["rpg.zmob","rpg.znew"],PersistenceRequired:false}
