# ══ 开启奖励系统总开关 ══
# 用法：/function rpg:reward/on
scoreboard players set #rw_master rpg.data 1
tellraw @a ["",{"text":"[奖励] ","color":"gold","bold":true},{"text":"奖励系统已","color":"gray"},{"text":"开启","color":"green","bold":true},{"text":" —— 各分项仍可在奖励管理面板单独关闭","color":"dark_gray"}]
