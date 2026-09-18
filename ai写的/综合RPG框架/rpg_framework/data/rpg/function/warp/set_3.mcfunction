# ══ 管理员：设置BOSS竞技场传送点（站在目标位置执行） ══
data merge storage rpg:warps {w3:{x:0,y:0,z:0,dim:"minecraft:overworld"}}
execute if entity @s[nbt={Dimension:-1}] run data merge storage rpg:warps {w3:{dim:"minecraft:the_nether"}}
execute if entity @s[nbt={Dimension:1}] run data merge storage rpg:warps {w3:{dim:"minecraft:the_end"}}
execute store result storage rpg:warps w3.x int 1 run data get entity @s Pos[0]
execute store result storage rpg:warps w3.y int 1 run data get entity @s Pos[1]
execute store result storage rpg:warps w3.z int 1 run data get entity @s Pos[2]
tellraw @a ["",{"text":"[传送] ","color":"gold"},{"selector":"@s","color":"aqua"},{"text":" 设置了BOSS竞技场传送点","color":"gray"}]
