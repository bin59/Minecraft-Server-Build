# ══ 精英参战奖励（受「BOSS/精英」分项开关控制） ══
scoreboard players add @a[tag=rpg.elite_fight] rpg.gold 300
scoreboard players add @a[tag=rpg.elite_fight] rpg.exp 200
tellraw @a ["",{"text":"参战奖励（仅参战玩家）：","color":"gray"},{"text":"+300金币 +200经验","color":"yellow","bold":true}]
