# ══ 关闭奖励系统总开关（任务进度与签到记录一律保留） ══
# 用法：/function rpg:reward/off
scoreboard players set #rw_master rpg.data 0
tellraw @a ["",{"text":"[奖励] ","color":"gold","bold":true},{"text":"奖励系统已","color":"gray"},{"text":"关闭","color":"red","bold":true},{"text":" —— 击杀掉落/任务/签到/升级/BOSS/抽奖 全部停发（进度保留）","color":"dark_gray"}]
