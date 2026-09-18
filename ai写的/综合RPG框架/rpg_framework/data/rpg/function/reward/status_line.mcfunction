# ══ 打印一行开关状态（宏，仅供 rpg:reward/status 调用） ══
# 用法：/function rpg:reward/status_line {key:"kill",label:"击杀掉落"}
$execute if score #rw_$(key) rpg.data matches 1 run tellraw @s ["",{"text":"  ▸ ","color":"dark_gray"},{"text":"$(label)","color":"gray"},{"text":"  ","color":"gray"},{"text":"开启","color":"green","bold":true}]
$execute unless score #rw_$(key) rpg.data matches 1 run tellraw @s ["",{"text":"  ▸ ","color":"dark_gray"},{"text":"$(label)","color":"gray"},{"text":"  ","color":"gray"},{"text":"关闭","color":"red","bold":true}]
