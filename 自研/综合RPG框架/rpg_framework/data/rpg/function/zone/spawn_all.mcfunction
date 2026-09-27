# ══ 刷怪调度：每 #zone_rate 秒对每个启用的区域尝试刷怪 ══
scoreboard players set #zone_ztick rpg.data 0
execute if data storage rpg:zones z1{on:1b} run function rpg:zone/spawn with storage rpg:zones z1
execute if data storage rpg:zones z2{on:1b} run function rpg:zone/spawn with storage rpg:zones z2
execute if data storage rpg:zones z3{on:1b} run function rpg:zone/spawn with storage rpg:zones z3
execute if data storage rpg:zones z4{on:1b} run function rpg:zone/spawn with storage rpg:zones z4
execute if data storage rpg:zones z5{on:1b} run function rpg:zone/spawn with storage rpg:zones z5
execute if data storage rpg:zones z6{on:1b} run function rpg:zone/spawn with storage rpg:zones z6
execute if data storage rpg:zones z7{on:1b} run function rpg:zone/spawn with storage rpg:zones z7
execute if data storage rpg:zones z8{on:1b} run function rpg:zone/spawn with storage rpg:zones z8
