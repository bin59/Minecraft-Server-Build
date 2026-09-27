# ══════════════════════════════════════════════════════════════════════
#  综合RPG框架 · 服主配置文件（怪物领域 + 传送阵 + 奖励开关）
# ══════════════════════════════════════════════════════════════════════
#  用法：用文本编辑器修改本文件的数值，保存后在游戏内执行：
#        /reload
#        /function rpg:config
#
#  为什么要手动执行？—— 本文件不会在 /reload 时自动生效，
#  这样你在游戏内用图形界面做的临时调整不会被文件里的旧值覆盖。
#  只有你主动执行 /function rpg:config 时，才以本文件为准强制覆盖全部参数。
# ══════════════════════════════════════════════════════════════════════


# ┌────────────────────────────────────────────────────────────────────┐
# │ 一、怪物领域系统 —— 全局参数                                        │
# └────────────────────────────────────────────────────────────────────┘

# 系统总开关：0=停用（怪物生成完全交给原版）  1=启用（执行区域限制）
# 【重要】首次安装默认为 0。请先设好区域再开启，否则会立刻清怪。
scoreboard players set #zone_on rpg.data 0

# 管制强度：
#   1 = 严格模式 —— 全服区域外的敌对怪物一律清除（含原版自然生成的），
#                    即"怪物只能存在于管理员设定的区域内"
#   0 = 宽松模式 —— 只管制本框架在区域内刷出的怪，原版生态不受影响
scoreboard players set #zone_strict rpg.data 1

# 越界处理方式：
#   1 = 直接清除
#   2 = 先拉回（区域外但仍在缓冲带内的怪拉回区域中心，超出缓冲带才清除）——推荐
scoreboard players set #zone_mode rpg.data 2

# 缓冲带宽度（格）：仅拉回模式使用。区域半径 + 本值 = 拉回生效范围。
# 注意：修改本值后，已存在的区域需要重新设置才会应用新的缓冲带。
scoreboard players set #zone_pad rpg.data 24

# 刷怪间隔（秒）：每隔多少秒对每个启用的区域尝试刷怪一次
scoreboard players set #zone_rate rpg.data 3

# 每次刷怪尝试次数（1~4）：数值越大填充越快
scoreboard players set #zone_batch rpg.data 2


# ┌────────────────────────────────────────────────────────────────────┐
# │ 二、传送阵系统 —— 全局参数                                          │
# └────────────────────────────────────────────────────────────────────┘

# 系统总开关：0=停用  1=启用
scoreboard players set #portal_on rpg.data 1

# 充能所需次数：传送阵每 5 刻扫描一次，8 次 = 40 刻 = 2 秒站立时间
scoreboard players set #portal_charge rpg.data 8

# 传送后冷却次数（同样以 5 刻为单位）：12 = 3 秒，防止落地后立刻被再次传送
scoreboard players set #portal_cd rpg.data 12


# ┌────────────────────────────────────────────────────────────────────┐
# │ 三、奖励系统 —— 总开关 + 分项开关                                    │
# └────────────────────────────────────────────────────────────────────┘
#  判定规则：实际发放 = 总开关 且 对应分项 都为 1。
#  总开关为 0 时，下面所有分项一律停发；总开关为 1 时，各分项可单独关掉某一类。
#
#  关闭时不会清除任何进度：任务保持在"已完成待交付"，当日签到资格保留，
#  重新开启后玩家照常领取，不会白干。

# 奖励总开关：0=全部停发  1=按分项发放
scoreboard players set #rw_master rpg.data 1

# 分项：击杀怪物掉落的经验与金币（1=发放 0=停发）
scoreboard players set #rw_kill rpg.data 1

# 分项：任务奖励 + 每日任务奖励
scoreboard players set #rw_quest rpg.data 1

# 分项：每日签到奖励（+100金 +20经验）
scoreboard players set #rw_daily rpg.data 1

# 分项：升级奖励（+50金）+ 5/10/20/30/50 级里程碑奖励
scoreboard players set #rw_level rpg.data 1

# 分项：BOSS / 精英入侵的参战奖励（金币与经验；进度成就仍会授予）
scoreboard players set #rw_boss rpg.data 1

# 分项：幸运抽奖（关闭后抽奖按钮不可用，也不会扣金币）
scoreboard players set #rw_lottery rpg.data 1


# ┌────────────────────────────────────────────────────────────────────┐
# │ 四、直接用文件定义区域（可选，取消注释即可使用）                     │
# └────────────────────────────────────────────────────────────────────┘
#  推荐做法是在游戏内用图形界面创建（自动计算边界，不会算错）：
#      /function rpg:ui/open_admin  →  怪物领域管理
#
#  如果你更习惯写配置，可以照下面的格式手写。字段含义：
#    on    是否启用（1b/0b）
#    s     槽位号，必须与 zN 的 N 一致
#    name  区域名称
#    dim   维度：minecraft:overworld / minecraft:the_nether / minecraft:the_end
#    cx cy cz  区域中心坐标
#    r     水平半径（格）      h  垂直半高（格）
#    x1 y1 z1  长方体最小角 = (cx-r, cy-h, cz-r)   ← 必须自己算对
#    dx dy dz  长方体边长     = (2r, 2h, 2r)       ← 必须自己算对
#    rp    拉回生效半径       = r + 缓冲带宽度
#    pool  怪物池编号 1~4（见 zone/pool_1 ~ pool_4 文件）
#    cap   区域内怪物数量上限
#
#  示例：主城东侧的哥布林营地，中心 (200,64,-350)，半径 48，垂直 ±32
# data modify storage rpg:zones z1 set value {on:1b,s:1,name:"哥布林营地",dim:"minecraft:overworld",cx:200,cy:64,cz:-350,r:48,h:32,x1:152,y1:32,z1:-398,dx:96,dy:64,dz:96,rp:72,pool:1,cap:16}
#
#  示例：下界熔狱，中心 (30,50,80)，半径 32，垂直 ±24
# data modify storage rpg:zones z2 set value {on:1b,s:2,name:"熔狱裂谷",dim:"minecraft:the_nether",cx:30,cy:50,cz:80,r:32,h:24,x1:-2,y1:26,z1:48,dx:64,dy:48,dz:64,rp:56,pool:3,cap:12}


# ┌────────────────────────────────────────────────────────────────────┐
# │ 五、直接用文件定义传送阵（可选，取消注释即可使用）                   │
# └────────────────────────────────────────────────────────────────────┘
#  字段含义：
#    on    是否启用（1b/0b）
#    s     槽位号，必须与 pN 的 N 一致
#    name  传送阵名称
#    dim x y z  阵台中心坐标（玩家站上来的那一格）
#    dt    目标类型：1=另一座传送阵  2=怪物领域中心  3=传送点(w1/w2/w3)
#    di    目标编号：dt=1 时填传送阵槽位，dt=2 时填区域槽位，dt=3 时填 1~3
#    lv    等级门槛（0 = 无门槛）
#    cost  每次传送消耗金币（0 = 免费）
#
#  示例：主城传送阵 → 通往 1 号怪物领域，需 5 级，收费 20 金
# data modify storage rpg:portals p1 set value {on:1b,s:1,name:"主城传送阵",dim:"minecraft:overworld",x:0,y:65,z:0,dt:2,di:1,lv:5,cost:20}
#  示例：营地返程阵 → 回到 1 号传送阵，免费
# data modify storage rpg:portals p2 set value {on:1b,s:2,name:"营地返程阵",dim:"minecraft:overworld",x:200,y:64,z:-350,dt:1,di:1,lv:0,cost:0}
#
#  用文件定义的传送阵不会自动建造阵法方块，需要自己搭或执行：
#      /function rpg:portal/rebuild {slot:1}   （把 1 换成对应槽位号）


# ---- 配置应用完成提示 ----
tellraw @a ["",{"text":"[RPG] ","color":"gold","bold":true},{"text":"配置文件已应用（怪物领域 + 传送阵 + 奖励开关）","color":"green"}]
function rpg:zone/list
function rpg:portal/list
function rpg:reward/status
