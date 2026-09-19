# 启动全部区域 BGM 循环（控制台/OP 执行一次：/function bgm:start）
# 重复执行安全：schedule 用 replace，不会叠加
function bgm:lobby_play
function bgm:spawn_play
function bgm:mines_play
tellraw @a {"text":"[BGM] 区域背景音乐已启动","color":"yellow"}
