# ══ 查看全部奖励开关状态 ══
# 用法：/function rpg:reward/status
tellraw @s ["",{"text":"══════ 奖励开关总览 ══════","color":"gold","bold":true}]
function rpg:reward/status_line {key:"master",label:"总开关"}
function rpg:reward/status_line {key:"kill",label:"击杀掉落(经验/金币)"}
function rpg:reward/status_line {key:"quest",label:"任务 / 每日任务"}
function rpg:reward/status_line {key:"daily",label:"每日签到"}
function rpg:reward/status_line {key:"level",label:"升级奖励 / 里程碑"}
function rpg:reward/status_line {key:"boss",label:"BOSS / 精英参战"}
function rpg:reward/status_line {key:"lottery",label:"幸运抽奖"}
tellraw @s ["",{"text":"实际发放 = 总开关 且 对应分项均为开启。","color":"dark_gray"},{"text":"[打开管理面板]","color":"aqua","bold":true,"click_event":{"action":"run_command","command":"/function rpg:ui/reward_admin_open"}}]
