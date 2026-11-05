# ══ 计算升级所需经验：20 + (等级-1) * 12 ══
# rpg.tmp = 等级-1
scoreboard players operation rpg.tmp_holder rpg.tmp = @s rpg.level
scoreboard players remove rpg.tmp_holder rpg.tmp 1
scoreboard players operation rpg.tmp_holder rpg.tmp *= #c12 rpg.data
scoreboard players add rpg.tmp_holder rpg.tmp 20
scoreboard players operation @s rpg.exp_need = rpg.tmp_holder rpg.tmp
