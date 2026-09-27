# ══ 一键切换奖励总开关 ══
# 用法：/function rpg:reward/toggle
# 关闭时：击杀掉落、任务、签到、升级、里程碑、BOSS/精英、抽奖 全部停发
#         且不会消耗任务完成状态与当日签到资格，重新开启后仍可照常领取
execute if score #rw_master rpg.data matches 0 run return run function rpg:reward/on
function rpg:reward/off
