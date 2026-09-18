# ══ 打开图形主菜单（指令/聊天按钮入口） ══
scoreboard players reset @s rpg.gui
# 先输出聊天菜单（基岩版兜底），再尝试弹出图形界面（Java版）
function rpg:ui/menu
dialog show @s rpg:main
