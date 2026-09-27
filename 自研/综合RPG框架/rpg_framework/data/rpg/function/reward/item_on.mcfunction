# ══ 开启某个奖励分项（宏） ══
# 用法：/function rpg:reward/item_on {key:"kill"}
# key 取值：kill 击杀掉落 | quest 任务/每日 | daily 签到 | level 升级/里程碑 | boss BOSS/精英 | lottery 抽奖
$scoreboard players set #rw_$(key) rpg.data 1
$tellraw @s ["",{"text":"[奖励] ","color":"gold","bold":true},{"text":"分项 ","color":"gray"},{"text":"$(key)","color":"yellow"},{"text":" 已","color":"gray"},{"text":"开启","color":"green","bold":true},{"text":"（总开关仍需为开才会实际发放）","color":"dark_gray"}]
