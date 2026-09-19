# 停止全部 BGM 循环并静音（控制台/OP 执行：/function bgm:stop）
schedule clear bgm:lobby_play
schedule clear bgm:spawn_play
schedule clear bgm:mines_play
stopsound @a music
tellraw @a {"text":"[BGM] 区域背景音乐已停止","color":"yellow"}
