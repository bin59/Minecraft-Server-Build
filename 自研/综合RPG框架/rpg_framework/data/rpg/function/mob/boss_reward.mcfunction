# ══ BOSS 参战奖励（受「BOSS/精英」分项开关控制） ══
tellraw @a ["",{"text":"参战奖励发放（仅参战玩家）：","color":"gray"},{"text":"+500金币 +300经验","color":"yellow","bold":true}]
scoreboard players add @a[tag=rpg.boss_fight] rpg.gold 500
scoreboard players add @a[tag=rpg.boss_fight] rpg.exp 300
