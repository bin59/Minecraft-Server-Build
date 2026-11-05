# ══ 落地演出（宏，name = 出发阵名称） ══
# 注意要 at @s 重新锚定，否则特效会放在传送前的旧坐标上
execute at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.6 1 0.6 0.4 80
execute at @s run playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 1.2
$title @s actionbar ["",{"text":"传送完成 ","color":"light_purple","bold":true},{"text":"（来自 $(name)）","color":"aqua"}]
