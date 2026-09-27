# ══ 关闭传送阵系统（阵法与登记数据全部保留） ══
scoreboard players set #portal_on rpg.data 0
scoreboard players set @a rpg.pnow 0
scoreboard players set @a rpg.pcharge 0
tellraw @a ["",{"text":"[传送阵] ","color":"light_purple","bold":true},{"text":"传送阵网络已","color":"gray"},{"text":"停用","color":"red","bold":true}]
