# ══ 任务完成公共逻辑：累计完成数 + 成就检测 ══
scoreboard players add @s rpg.qdone_count 1
execute if score @s rpg.qdone_count matches 5.. run advancement grant @s only rpg:quest_master
