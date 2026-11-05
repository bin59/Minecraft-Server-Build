# ══ 游戏内新的一天（daytime 回绕时触发） ══
scoreboard players add #day rpg.data 1
# 重置所有每日任务状态（0=可接取）
scoreboard players set @a rpg.q4s 0
scoreboard players set @a rpg.q5s 0
scoreboard players set @a rpg.q6s 0
tellraw @a ["",{"text":"[RPG] ","color":"gold"},{"text":"新的一天开始了！记得签到（暂停菜单→每日签到），每日任务也已刷新","color":"gray"}]
