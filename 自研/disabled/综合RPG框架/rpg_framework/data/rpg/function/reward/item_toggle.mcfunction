# ══ 一键切换某个奖励分项（宏） ══
# 用法：/function rpg:reward/item_toggle {key:"kill"}
# key 取值：kill 击杀掉落 | quest 任务/每日 | daily 签到 | level 升级/里程碑 | boss BOSS/精英 | lottery 抽奖
$execute if score #rw_$(key) rpg.data matches 0 run return run function rpg:reward/item_on {key:"$(key)"}
$function rpg:reward/item_off {key:"$(key)"}
