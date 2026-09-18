# ══════════════════════════════════════════
#  综合RPG框架 · 加载初始化（服务器启动/重载时执行）
# ══════════════════════════════════════════
# ---- 核心属性计分板 ----
scoreboard objectives add rpg.level dummy "§6RPG等级"
scoreboard objectives add rpg.exp dummy "经验"
scoreboard objectives add rpg.exp_need dummy "升级需求"
scoreboard objectives add rpg.gold dummy "§e金币"
scoreboard objectives add rpg.class dummy "职业"
scoreboard objectives add rpg.kills totalKillCount "击杀"
scoreboard objectives add rpg.k_done dummy
# ---- 任务统计计分板 ----
scoreboard objectives add rpg.q1z minecraft.killed:minecraft.zombie
scoreboard objectives add rpg.q2i minecraft.picked_up:minecraft.iron_ingot
scoreboard objectives add rpg.q3k minecraft.killed:minecraft.skeleton
scoreboard objectives add rpg.q1base dummy
scoreboard objectives add rpg.q2base dummy
scoreboard objectives add rpg.q3base dummy
scoreboard objectives add rpg.q1s dummy "任务1状态"
scoreboard objectives add rpg.q2s dummy "任务2状态"
scoreboard objectives add rpg.q3s dummy "任务3状态"
# ---- 每日任务 ----
scoreboard objectives add rpg.q4base dummy
scoreboard objectives add rpg.q4s dummy "每日任务1状态"
scoreboard objectives add rpg.q5m minecraft.mined:minecraft.stone
scoreboard objectives add rpg.q5base dummy
scoreboard objectives add rpg.q5s dummy "每日任务2状态"
scoreboard objectives add rpg.q6e minecraft.picked_up:minecraft.emerald
scoreboard objectives add rpg.q6base dummy
scoreboard objectives add rpg.q6s dummy "每日任务3状态"
# ---- 任务完成计数（成就用） ----
scoreboard objectives add rpg.qdone_count dummy
# ---- 冷却/状态/排行 ----
scoreboard objectives add rpg.cd dummy "技能冷却"
scoreboard objectives add rpg.inited dummy
scoreboard objectives add rpg.day_claim dummy
scoreboard objectives add rpg.stick minecraft.used:minecraft.carrot_on_a_stick
scoreboard objectives add rpg.guide minecraft.used:minecraft.warped_fungus_on_a_stick
scoreboard objectives add rpg.data dummy
scoreboard objectives add rpg.tmp dummy
# ---- 传送阵玩家状态（所在阵台 / 充能进度 / 传送冷却） ----
scoreboard objectives add rpg.pnow dummy
scoreboard objectives add rpg.pcharge dummy
scoreboard objectives add rpg.pcd dummy
# ---- 玩家指令触发器（Java/基岩通用） ----
scoreboard objectives add rpg.menu trigger
scoreboard objectives add rpg.cls trigger
scoreboard objectives add rpg.pick trigger
scoreboard objectives add rpg.stats trigger
scoreboard objectives add rpg.skill trigger
scoreboard objectives add rpg.shop trigger
scoreboard objectives add rpg.buy trigger
scoreboard objectives add rpg.quest trigger
scoreboard objectives add rpg.qacc trigger
scoreboard objectives add rpg.qdone trigger
scoreboard objectives add rpg.warp trigger
scoreboard objectives add rpg.go trigger
scoreboard objectives add rpg.pay trigger
scoreboard objectives add rpg.daily trigger
scoreboard objectives add rpg.top trigger
scoreboard objectives add rpg.boss trigger
scoreboard objectives add rpg.lottery trigger
scoreboard objectives add rpg.elite trigger
scoreboard objectives add rpg.gui trigger
# ---- 怪物领域 / 传送阵 触发器 ----
scoreboard objectives add rpg.zone trigger
scoreboard objectives add rpg.portal trigger
scoreboard objectives add rpg.admin trigger
scoreboard objectives add rpg.zone_admin trigger
scoreboard objectives add rpg.portal_admin trigger
# ---- 展示：侧边栏=等级榜，Tab列表=金币 ----
scoreboard objectives setdisplay sidebar rpg.level
scoreboard objectives setdisplay list rpg.gold
# ---- BOSS血条 ----
bossbar add rpg:boss {text:"深渊领主",color:"dark_red",bold:true}
bossbar set rpg:boss max 200
bossbar set rpg:boss color red
bossbar set rpg:boss visible false
# ---- 精英挑战血条 ----
bossbar add rpg:elite {text:"精英挑战",color:"dark_purple",bold:true}
bossbar set rpg:elite max 3
bossbar set rpg:elite color purple
bossbar set rpg:elite visible false
# ---- 常量与存储 ----
function rpg:init/constants
tellraw @a ["",{"text":"[RPG] ","color":"gold","bold":true},{"text":"综合RPG框架已加载（界面版）—— 按 ","color":"gray"},{"text":"ESC","color":"yellow","bold":true},{"text":" 打开暂停菜单，或输入 ","color":"gray"},{"text":"/trigger rpg.gui","color":"aqua","click_event":{"action":"run_command","command":"/trigger rpg.gui"}},{"text":" 打开主菜单","color":"gray"}]
