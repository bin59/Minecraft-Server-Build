════════════════════════════════════════════
  综合RPG框架（界面版） · 使用说明
  适配：Minecraft Java 1.21.11（数据包格式 94）
  服务端：Leaf / Paper 等（Java/基岩互通服可用）
════════════════════════════════════════════

【安装】
1. 将本文件夹放入 world/datapacks/rpg_framework
2. 重启服务器，或执行 /reload
3. 看到 "[RPG] 综合RPG框架已加载（界面版）" 即成功

【打开主菜单（三种方式）】
  方式1：按 ESC → 暂停菜单 → "综合RPG框架"（Java版）
  方式2：右键"冒险指南"道具（Java/基岩通用）
  方式3：聊天输入 /trigger rpg.gui 或 /trigger rpg.menu
        （基岩版玩家推荐此方式，按钮全在聊天里）

【玩法系统一览】
  ◆ 职业：战士/弓箭手/法师/牧师，专属技能与被动
  ◆ 等级：击杀怪物升级，10级起加生命，5/10/20/30/50级里程碑大奖
  ◆ 技能：右键技能魔杖释放（或 /trigger rpg.skill）
  ◆ 任务：3条可重复任务 + 3条每日任务（每日刷新）
  ◆ 经济：商店/转账/每日签到/幸运抽奖（50金一次）
  ◆ 传送：主城/矿区/BOSS竞技场（管理员站位设置）
  ◆ BOSS战：深渊领主（需5级+深渊之眼道具，冷却10分钟，仅参战玩家获奖励）
  ◆ 精英入侵：3精英挑战（需精英召集令道具，冷却5分钟，仅参战玩家获奖励）
  ◆ 召唤道具：商店购买 深渊之眼(300金)/精英召集令(100金)
  ◆ 成就：原版成就界面 → "综合RPG" 标签页（11个成就）
  ◆ 排行：侧边栏等级榜，Tab键金币榜

【界面系统说明】
  Java版：全部图形弹窗，零指令操作
  基岩版：图形弹窗不显示（客户端不支持），
        通过"右键冒险指南"或聊天按钮使用全部功能，
        聊天里的按钮基岩版可以直接点击

【玩家指令（全部可用按钮替代，仅作备用）】
  /trigger rpg.gui       打开图形主菜单
  /trigger rpg.menu      打开聊天菜单
  /trigger rpg.stats     角色面板
  /trigger rpg.cls       选择职业
  /trigger rpg.skill     释放技能
  /trigger rpg.quest     任务面板（含每日任务进度）
  /trigger rpg.shop      商店
  /trigger rpg.buy set <编号>   购买商品
  /trigger rpg.warp      传送菜单
  /trigger rpg.pay set <金额>   转账给最近玩家
  /trigger rpg.daily     每日签到
  /trigger rpg.top       排行榜
  /trigger rpg.boss      召唤BOSS
  /trigger rpg.lottery   幸运抽奖
  /trigger rpg.elite     精英入侵

【管理员命令】
  /function rpg:admin/help         管理员命令帮助
  /function rpg:admin/gold_1000    +1000金币
  /function rpg:admin/exp_500      +500经验
  /function rpg:admin/level_up     升1级
  /function rpg:admin/reset_class  重置职业
  /function rpg:admin/boss_remove  强制移除BOSS
  /function rpg:admin/reset_player 重置全部数据
  /function rpg:admin/elite_remove 强制移除精英（新增）
  /function rpg:admin/clear_cd     清空BOSS与精英召唤冷却
  对指定玩家：/execute as <玩家> run function rpg:admin/gold_1000

【奖励开关（一键启停全部奖励）】
  图形入口：/function rpg:ui/open_admin → 奖励开关管理
  命令：
    /function rpg:reward/toggle                      一键开/关总开关
    /function rpg:reward/on | off                    直接开 / 关
    /function rpg:reward/status                      查看总开关与 6 个分项
    /function rpg:reward/item_toggle {key:"kill"}    单独切换某一类
  分项 key：kill 击杀掉落 / quest 任务每日 / daily 签到
            level 升级里程碑 / boss BOSS精英 / lottery 抽奖
  判定规则：实际发放 = 总开关 且 对应分项 都为 1
  关闭后：击杀不掉金币经验；任务/签到暂不可交付（进度与当日资格保留，
          重开后照常领取）；升级不发金币不给里程碑；BOSS/精英不发金币
          经验但进度成就仍授予；抽奖暂停且不扣金币
  默认值：全部为 1（发放）。可在 config.mcfunction 第三节改默认，
          改完执行 /reload 后 /function rpg:config 生效

【传送点设置（站在目标位置执行）】
  /function rpg:warp/set_1   主城
  /function rpg:warp/set_2   矿区
  /function rpg:warp/set_3   BOSS竞技场

【卸载】
  /function rpg:uninstall
  然后 /datapack disable "file/rpg_framework" 或删除本文件夹

════════════════════════════════════════════
