# ══ 管理员：清空BOSS与精英挑战召唤冷却 ══
scoreboard players set #boss_cd rpg.data 0
scoreboard players set #elite_cd rpg.data 0
tellraw @s ["",{"text":"[管理] ","color":"gold"},{"text":"BOSS与精英挑战的召唤冷却已清空","color":"green"}]
