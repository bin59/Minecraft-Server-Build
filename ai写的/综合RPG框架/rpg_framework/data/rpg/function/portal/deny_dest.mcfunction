# ══ 拒绝：目标尚未设置或配置有误（退还已扣的金币，避免玩家白花钱） ══
scoreboard players operation @s rpg.gold += #pcost rpg.data
scoreboard players set @s rpg.pcharge 0
scoreboard players set @s rpg.pcd 8
tellraw @s ["",{"text":"[传送阵] ","color":"red","bold":true},{"text":"这座传送阵的目标地点尚未设置，已退还金币，请联系管理员","color":"red"}]
playsound minecraft:block.note_block.bass player @s ~ ~ ~ 1 0.5
