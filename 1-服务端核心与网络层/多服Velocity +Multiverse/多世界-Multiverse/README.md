# 多世界 — Multiverse-Core（插件分册）

Multiverse-Core 是**单服多世界**管理插件：在一个 Leaf 进程里开多个独立世界（主世界 / 资源界 / 小游戏 / 建筑区），玩家用 `/mv tp` 或传送门穿梭。

> **两层关系（别混）**：Velocity 管"**服与服**"（跨 JVM，命令 `/server`）；Multiverse 管"**世界与世界**"（同一 JVM 内，命令 `/mv tp`）。两者互补、不替代。**Multiverse 只装后端子服，绝不装 Velocity 代理进程**——代理不加载游戏世界。

**适用副本**：`leaf-26.3-多服`（survival / minigames 装了 Multiverse-Core 5.8.2；skyblock 用 BentoBox 管岛，**不装 Multiverse**）。

## 0. 版本与官方链接（2026-11-22 核对）

| 项 | 信息 |
| --- | --- |
| 当前版本 | **Multiverse-Core 5.8.2**（2026-10-26） |
| MC 支持 | 官方 Modrinth 明确列出 **1.13.x → 26.3**（含 1.21.x / 26.1.x / 26.2 / **26.3**） |
| 平台 | Bukkit / Paper / Purpur / Spigot；本服 Leaf 26.3-18 + Java 25 可直接装，无版本顾虑 |
| 许可 | BSD-3-Clause |
| 玩家端 | 不用装，纯服务端插件 |
| 依赖 | 无强依赖；可选 Multiverse-Portals / -Inventories / -NetherPortals / -SignPortals |

**官方链接**

- Modrinth：https://modrinth.com/plugin/multiverse-core （5.8.2 具体版：https://modrinth.com/plugin/multiverse-core/version/5.8.2 ）
- GitHub：https://github.com/Multiverse/Multiverse-Core
- 官方 Wiki：https://wiki.multiverse.org/
- Discord：https://discord.gg/NZtfKky

## 1. 安装

1. 下载 `multiverse-core-5.8.2.jar`（[Modrinth](https://modrinth.com/plugin/multiverse-core/version/5.8.2)），丢进**目标子服**的 `plugins/`，完整重启。
2. 首次启动自动生成 `plugins/Multiverse-Core/`（`config.yml`、`worlds.yml`、`anchors.yml`）。
3. 确认日志有 `Enabling Multiverse-Core v5.8.2` / `Version 5.8.2 (API v5.8) Enabled`。

## 2. 基础命令

```bash
/mv create resource normal -t NORMAL   # 新建"资源世界"（normal 维度）
/mv create games normal -t NORMAL       # 新建"小游戏世界"
/mv list                                # 列出所有世界（worlds.yml 为空=没有注册世界）
/mv tp <世界名>                         # 传送到某世界
/mv who                                 # 看谁在哪个世界
/mv remove <世界名>                     # 从 Multiverse 移除（谨慎）
/mv delete <世界名>                     # 删除世界目录（资源界重置用）
```

> 资源世界建议单独生成干净维度，挖完即弃；小游戏世界可加载独立地形生成器。

## 3. 导入现成地图（26.3 重点坑 ⚠️）

本服实际踩过：`map-jkpof`（幸运之柱）导入后**只有地形、没有玩法**。这一节是排查结论，务必按此操作。

### 3.1 导入步骤（26.3 正确姿势）

```bash
# 1. 地图文件夹（如 map-jkpof）放到子服根目录，先删客户端残留：
#    session.lock（必删，否则拒载）、level.dat_old、voxy/、serverconfig/、xaeromap.txt 等
# 2. 注册进 Multiverse
/mv import map-jkpof normal
# 3. 日志确认：Loading ... persistent chunks for level 'minecraft:map-jkpof'... = 地形加载成功
/mv list
# 4. 设安全出生点（见 §4），否则 mvtp 被拦
/mvtp map-jkpof
```

### 3.2 坑 A：26.3 迁移只搬地形，不搬数据包

**现象**：地形加载成功（日志有 `Loading ... chunks`），`mvtp` 进去看得到建筑/柱子，但小游戏机制全部失效（不淘汰、不计分、不重开），等于"只有地图"。

**根因**：Leaf 26.3 走 Paper 新世界格式。`/mv import` 触发 `LegacyCraftBukkitWorldMigration`，日志形如：

```
Migrating world directory from .\map-jkpof\dimensions\minecraft\overworld\region
                            to .\mg_hub\dimensions\minecraft\map-jkpof\region
```

它**只搬 `region/ entities/ poi/`，不搬 `datapacks/`**。地图玩法若靠纯原版数据包（本图靠 `datapacks/jkpof`），数据包没跟过来 → 只剩静态地形。核对：`mg_hub/datapacks/` 为空、全盘无 `jkpof/pack.mcmeta` 即可确认。

**修复**：

1. 重新下载原始地图包，取出 `datapacks/jkpof` 整个文件夹。
2. 放进 `minigames/mg_hub/datapacks/jkpof`（26.3 下该图是 `mg_hub` 的自定义维度 `minecraft:map-jkpof`，数据包挂在主 level 的 datapacks 目录）。
3. **完整重启**——数据包只在启动时加载，`/reload` 不生效。
4. `/datapack list` 确认 `file/jkpof` 在 enabled 列表。
5. 若数据包函数写死 `minecraft:overworld`，在自定义维度下会跑错地方——改走插件托管方案（如 FortunePillars 建竞技场），或把该图设为整个子服的 `level-name`。

> ⚠️ **不要删原始地图文件夹**：26.3 迁移是"复制地形进 mg_hub"，原始数据包的唯一副本还在原 `map-jkpof/datapacks/`。先备份整个原文件夹再操作。

**数据包加载机制（怎么"给自己的世界加载自己的数据包"）**：数据包是**存档（level）级**的，不是世界/维度级。一个存档只有一个 `datapacks/` 目录，里面所有启用的包对该存档**所有维度**生效。

| 场景 | 数据包目录 |
| --- | --- |
| 客户端单人 | `saves/<存档名>/datapacks/<包名>/pack.mcmeta` |
| 服务器 | `<level-name>/datapacks/<包名>/pack.mcmeta`（`level-name` 见 `server.properties`） |
| 本服 minigames | `mg_hub/datapacks/`（26.3 下所有导入世界都并进 `mg_hub` 的维度，没有各自的 datapacks） |

- 目录结构必须是 `<datapacks>/<包名>/pack.mcmeta`，别多套一层文件夹。
- **完整重启**才加载新包（`/reload` 对未登记的新包不稳定）。日志出现 `Found new data pack file/<包名>, loading it automatically` 即成功。
- `/datapack list` 验证：enabled 列表出现 `file/<包名>`。
- `/datapack enable|disable file/<包名>` 开关，改后重启。ID 按日志实际显示（如 `file/1.jkpof`）。
- 禁用三法：① `/datapack disable <id>` + 重启；② 移出 `datapacks/` 目录；③ 点号前缀改名（`.jkpof.zip`，MC 忽略）。**`1.` 前缀和 `.zip` 后缀都不能禁用**——MC 支持 zip 包，`1.` 不是禁用标志。

> ⚠️ 因此"每个世界只加载自己的数据包"**在原版机制下做不到**——同存档的包互相可见。要真正隔离只能分服（见 §3.2.3 方案 A），或在函数逻辑层用维度 NBT 门控（方案 C）。

#### 3.2.1 同类实例：`100_levels_2`（CubeRunner 跑酷图，2026-11-22 实测）

`100_levels_2`（100 Levels 2 v1.4.2，CubeRunner Parkour Maps）踩的是同一个坑，但多一层"出生点"迷惑：

| 项 | 实测值 |
| --- | --- |
| 原始副本位置 | `minigames/100_levels_2 - 副本/`（**地形 + `datapacks/main` 都在这**，357 个函数） |
| 地形 | 16 个 `.mca`（约 30MB），X -664~776 / Z -680~648，Axiom 导出图离原点很远 |
| level.dat `spawn` | **`0,0,0`**（地图作者故意留空，靠数据包函数在进服时传送） |
| DataVersion | 4556 = 1.21.10（低于服务端 5023，启动会升级，不致命） |
| 真正出生点 | `datapacks/main/data/common/function/join.mcfunction` 第一行：`tp @s 3 71 -168 0.0 30.0` |
| 数据包格式 | `min_format/max_format: 121`，与已能加载的 jkpof 相同 → **26.3 可直接加载** |

**为什么传进去是虚空**：`spawn=0,0,0` 被 Multiverse 判为不安全 → 自动调到 `0,256,0`（虚空顶）。普通 `/mvtp` 被安全检查拦下，`--unsafe` 能进但落虚空往下掉。根因**不是 spawn 配错**——这张图设计上就不靠 level.dat 的 spawn，而是靠 `common:join` 在进服时把玩家送到 `3 71 -168`，并 `spawnpoint` 到脚下。数据包没进服 → 没人执行 join → 玩家停在虚空。

**修复**（不要再手设 spawn）：

1. 把 `100_levels_2 - 副本/datapacks/main` 整个复制到 `minigames/mg_hub/datapacks/main`（与 jkpof 并列）。原副本别删，是唯一完整来源。
2. **完整重启**（`/reload` 不加载数据包）。日志应出现 `Found new data pack file/main`。
3. `/datapack list` 确认 `file/main` enabled。
4. 玩家进该维度后，`common:join` 会自动 `tp` 到 `3 71 -168` 并设为个人 spawnpoint —— 不用 `/mvsetspawn`。
5. 进图后验证：记分板 `pb / ID / const` 等被创建、`/trigger tools` 等菜单可用 → 说明 `load→init` 跑通。

> 注意：Axiom 自定义游戏规则 `axiomPlayerInvulnerability` / `axiomDoBlockGravity` 等含大写字母，不符合 MC resource location 命名（只允许 `a-z0-9/._-`），日志会报 `Failed to parse saved data for game_rules`。这是地图作者写入的非法键，不阻止进图，但若跑酷机制异常可从此查起。

#### 3.2.2 坑 C：多个小游戏数据包同服冲突——jkpof 把玩家拉回 map-jkpof（2026-11-22 实测）

**现象**：把玩家 tp 到 `100_levels_2` 的起点 `3 71 -168`，命令执行成功（`Teleported to 3.5,71.0,-167.5`），但约 1 秒后被强制拉回 map-jkpof 大厅。反复 tp 无效。

**根因**：jkpof 数据包（`mg_hub/datapacks/jkpof`）在 state 0（等待大厅）下每秒运行 `jkpof:state/0/sec1`：

```
execute positioned 0 64 -100 as @a unless entity @s[distance=..40] run function jkpof:state/0/player/back
```

`positioned 0 64 -100` 把检测原点写死在 **map-jkpof 大厅坐标**，`as @a` 遍历**全服所有玩家**——只要某玩家距该点 > 40 格（**不管在哪个世界/维度**），`player/back` 就 `tp @s ~ ~ ~` + 漂浮效果把他拽回。这是数据包为防玩家跑出大厅设计的，但它不判断维度，跨世界也误伤。

**关键结论**：只要 jkpof 处于 state 0 且启用，**同一子服里的其他世界（100_levels_2 等）都待不住**——这是"一个子服只放一个小游戏数据包"的边界问题，不是传送命令或 spawn 的问题。

**处置三选一**（按推荐度）：

| 方案 | 做法 | 代价 |
| --- | --- | --- |
| **A. 分服（推荐）** | 把 map-jkpof 和 100_levels_2 拆到不同子服（Velocity 后端各一个 Leaf 进程），jkpof 只装在前者 | 要新开子服、配 Velocity 路由；彻底根治 |
| **B. 关掉 jkpof 的 sec1 循环** | 停用 jkpof 数据包的 tick 调度（改 `load.mcfunction` 里的 `schedule ... tick1/tick8/sec1` 三行，或 `/datapack disable file/jkpof` 后重启） | jkpof 玩法也没了；只在你暂时只用跑酷图时可用 |
| **C. 两包各加维度门控、同服共存（选定方案，见 §3.2.3）** | 用玩家 NBT 的 `Dimension` 字段给 jkpof 的全局 `@a` 操作加维度限定，让两个包各管各的世界 | 改第三方数据包（升级会被覆盖）；gamerule 仍是存档级、需接受；目前 `.main.zip`/`.jkpof.zip` 已归位，重启后验证 |

> ⚠️ **命令语法提醒**：原版 `/tp` **不接受世界名参数**。` /tp @s 100_levels_2 -520 98 -8` 会报 `Incorrect argument for command`，根本没执行。跨世界传送要么用 `/mvtp <MV世界名>`，要么先进目标世界再 `/tp @s x y z`。

**⚠️ 禁用 jkpof 的正确姿势（2026-11-22 实测踩坑）**：把 `jkpof/` 改名成 `1.jkpof.zip` **不会禁用**——MC 支持 `.zip` 格式数据包，且 `1.` 前缀不是禁用标志，日志照样 `Found new data pack file/1.jkpof, loading it automatically`，冲突仍在。可行做法三选一：

1. **游戏内禁用**：`/datapack disable file/1.jkpof` → **完整重启**（写入 level.dat 的 disabled 列表）。注意名字要按日志里实际的 ID（`file/1.jkpof`，不是 `file/jkpof`）。
2. **移出目录**：把 `1.jkpof.zip` 移到 `datapacks/` 外面（如 `minigames/` 根目录备份）——MC 只扫描 `datapacks/` 内的包，移走即彻底不加载。
3. **点号前缀**：改名为 `.jkpof.zip`（`.` 开头文件被 MC 忽略），重启后 `datapack list` 确认不在列表。

> 禁用 jkpof 后 map-jkpof 的柱子/淘汰/计分玩法也没了。若两个玩法都要保留，走方案 A 分服，或下面 §3.2.3 的同服隔离。日志里"issued command"只代表命令被发出，不代表执行成功。

#### 3.2.3 选定方案：两个包同服共存、各管各的世界（2026-11-22）

用户明确"两个玩法都要，各自加载自己的"。**MC 数据包是存档级（level-wide）的**——放进 `mg_hub/datapacks/` 的包对整个存档所有维度生效，无法让 A 世界只加载 A 包。所以"各自加载"只能在**函数逻辑层**做维度隔离：给全局 `@a` 操作加维度条件，让每个包只作用于自己维度的玩家。

**技术前提（已实测）**：26.3 玩家实体 NBT 有 `Dimension` 字段（读 `mg_hub/players/data/<uuid>.dat` 确认），值形如 `minecraft:overworld` / `minecraft:map-jkpof` / `minecraft:100_levels_2`。目标选择器可直接用：

```
@a[nbt={Dimension:"minecraft:map-jkpof"}]      # 只选在 map-jkpof 维度的玩家
@a[nbt=!{Dimension:"minecraft:map-jkpof"}]     # 排除在 map-jkpof 的玩家
```

**当前磁盘状态**（2026-11-22 15:59）：`mg_hub/datapacks/` 下 `jkpof/`(文件夹)、`main/`(文件夹)、`.jkpof.zip`、`.main.zip` 并存。点号开头的两个 zip 被 MC 忽略，实际启用的是 `jkpof/` + `main/` 两个文件夹包 → **冲突仍在**。

**jkpof 需要改的 3 处**（都在 `jkpof/data/jkpof/function/`）：

1. `state/0/sec1.mcfunction` 第 2 行——拉回玩家（**最主要冲突**）：
   ```
   # 原
   execute positioned 0 64 -100 as @a unless entity @s[distance=..40] run function jkpof:state/0/player/back
   # 改为：只拉在 map-jkpof 维度的玩家
   execute positioned 0 64 -100 as @a[nbt={Dimension:"minecraft:map-jkpof"}] unless entity @s[distance=..40] run function jkpof:state/0/player/back
   ```
2. `state/0/sec1.mcfunction` 第 3 行——全场改冒险模式：
   ```
   # 原
   gamemode adventure @a[gamemode=!creative]
   # 改为：只改 map-jkpof 维度玩家
   gamemode adventure @a[gamemode=!creative,nbt={Dimension:"minecraft:map-jkpof"}]
   ```
3. `state/0/enter.mcfunction` 第 17–19 行——游戏重置时全场传送（触发频率低但会误伤）：
   ```
   # 原
   spawnpoint @a 0 64 -100
   tp @a 0 64 -100 0 0
   gamemode adventure @a
   # 改为：三行都加 nbt 限定
   spawnpoint @a[nbt={Dimension:"minecraft:map-jkpof"}] 0 64 -100
   tp @a[nbt={Dimension:"minecraft:map-jkpof"}] 0 64 -100 0 0
   gamemode adventure @a[nbt={Dimension:"minecraft:map-jkpof"}]
   ```

**main 不用改**：它的 join 触发是 `execute as @a[tag=!joined] at @s run function common:join`，`joined` tag 一次性，玩家只被传送一次；且它逐个 `@s` 处理、没有跨维度抓人。跑酷图内部逻辑靠 `team=ingame` / 自定义 tag，不与 jkpof 的 `jkpof_*` tag 冲突（已核对两包 tag 命名空间无重叠）。

**两个残留限制（需接受）**：

- **gamerule 是存档级**：两个包的 `init`/`gamerule` 都写全局游戏规则（如 `gamerule pvp false`），**谁后加载谁生效**，无法按维度隔离。好在两包设置方向类似（都关刷怪/计时、开立即重生等），实际差异主要是 pvp、fall_damage 等几项——以最后启动时生效的为准，跑酷图若需要 pvp 要额外处理。
- **改动第三方包**：升级/重新解压 jkpof 会覆盖这些改动，需重打补丁。

**部署步骤**（由服主在 `leaf-26.3-多服/minigames` 执行，AI 不碰运行服）：

1. 停服 → 按上面 3 处改 `jkpof/` 里的函数文件（改前复制 `.bak`）。
2. 启动 → 日志确认 `Found new data pack file/jkpof` 和 `file/main` 都在。
3. `/datapack list` 确认两包 enabled，且无 `file/1.jkpof`（那个 zip 已改名点号，应被忽略）。
4. 玩家 `/mvtp map-jkpof` 玩柱子（在 map-jkpof 维度会被 jkpof 正常拉回/计分）；`/mvtp 100_levels_2` 玩跑酷（join 送到 `3 71 -168`，**不再被 jkpof 拉走**）。
5. 验证点：站在跑酷图 1 分钟不被传回；jkpof 大厅内玩家离开 40 格仍会被拉回（玩法未坏）。

### 3.3 坑 B：DataVersion 与维度目录

- 地图 `DataVersion` 必须与服务端一致（26.3 = `5023`）；低版本地图需升级后再导。
- 26.3 世界布局是 `dimensions/minecraft/overworld/region/`，不用手动挪文件；老式 `world/region/` 也能识别。
- 数据包 ID / 文件夹名不要改（本图 ID 是 `jkpof`）。

## 4. 设置出生点（spawn）⚠️ 无参数

**`/mvsetspawn` 不带任何参数**——它设置的是**你当前所在世界**的出生点。必须人先站在目标世界里再敲。带世界名的 `/mvsetspawn map-jkpof` 是错误写法，本服实测不生效。

```bash
# 位置不安全时：先强制传送进目标世界
/mvtp map-jkpof --unsafe
# 找到安全落点（脚下实心、头顶有空间、周围无岩浆/水/虚空），站定后：
/mvsetspawn          # ← 无参数！作用于当前所在世界
```

> 报错 `Spawn location from world.dat file was unsafe. Adjusting...` / `Safe spawn NOT found!!!` 就是缺安全 spawn。`--unsafe` 只跳过检查、不会把你挪到安全处，坐标要自己确认。
> Multiverse 的 `/mvsetspawn` 与 EssentialsX 的 `/setspawn` 是两回事，本服实测 **Essentials 的 `/setspawn` 在此不生效**，以 Multiverse 为准。

## 5. 资源世界自动重置（重点）

Multiverse-Core **本身不含定时自动重置**，需外部触发。三种方案按可靠度排序：

**方案 A：系统计划任务 + RCON（最稳，版本无关）**

维护窗口由系统 cron（Linux）/ 任务计划程序（Windows）调用脚本，经 RCON 对 Leaf 执行：

```bash
# reset_resource.sh（经 mcrcon 或类似工具）
mv delete resource
mv create resource normal -t NORMAL
```

前提：Leaf 开 `enable-rcon=true` 并配 `rcon.password`。这是不受插件更新影响的兜底方案。

**方案 B：定时命令插件**——如支持 26.3 的 `TaskScheduler` / `ServerUtils`，周期性执行上面两条控制台命令。装前必须确认声明支持 26.3（本服插件清单未列此类，需单独核查）。

**方案 C：专用重置插件**——`WorldReset` / `AutoReset` 类需逐一确认 26.3 兼容与维护状态，**不推荐首选**。

> 推荐 **方案 A**：简单、可控、不受插件版本限制；频率（每日/每周）由维护窗口定。

## 6. 配套插件

| 插件 | 作用 | 多服注意 |
| --- | --- | --- |
| **Multiverse-Portals** | 用方块搭传送门在世界间穿梭 | 装子服，不装代理 |
| **Multiverse-Inventories** | 让不同世界有**独立背包/末影箱** | ⚠️ 这是"子服内分世界背包"，与跨服共享背包是两码事 |
| **Multiverse-NetherPortals** | 地狱门自动分流到对应世界的下界 | 装子服 |
| **Multiverse-SignPortals** | 用牌子点击传送 | 装子服 |

## 7. 权限节点（LuckPerms）

| 节点 | 作用 |
| --- | --- |
| `multiverse.*` | 全部权限（管理员） |
| `multiverse.access.*` | 进入所有世界 |
| `multiverse.teleport.self.*` | 自己传送到各世界 |
| `multiverse.create.*` | 创建世界 |
| `multiverse.core.manage` | 管理世界（删除/重载） |

> 高级权限模式下权限需显式授予，非 op 玩家默认拿不到；op 默认拥有。

## 8. 排错速查

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| `mvtp <世界>` 报未知世界 | 地图未注册进 Multiverse | 子服执行 `/mv import <世界> normal` |
| **`mvtp mg_hub` 传不过去** | **`mg_hub` 是磁盘目录名（`level-name`），不是 Multiverse 世界名**；`worlds.yml` 里主世界注册为 `minecraft:overworld`、`legacy-world-name: mg_hub`、`alias: ''` | 用 **`/mvtp overworld`**；Essentials 的 `/spawn` 也可回主世界出生点。菜单/NPC 里写死的 `mvtp mg_hub` 同样要改成 `mvtp overworld` |
| `World already loaded` | 世界已加载，无需再 load | 忽略，直接 `mvtp` |
| `位置被认为是不安全的` | 目标世界没有安全 spawn | 见 §4：`--unsafe` 进图 → 站安全点 → `/mvsetspawn`（无参数） |
| 有地形但玩法失效 | 26.3 迁移只搬 region，没搬 `datapacks/` | 见 §3.2：放回 datapacks 到 `mg_hub/datapacks/` + **完整重启** |
| 导入后世界出现在 `mg_hub/dimensions/minecraft/` | 26.3 Paper 新世界格式，属正常迁移 | 无需处理；数据包的唯一副本仍在原地图文件夹，别删 |
| `/mv create <名> normal` 无回显、世界没建出来 | 26.3 下部分环境 create 不落地；日志无成功行即为失败 | 导入现成地图**只用 `/mv import <名> normal`**，不要先 `create`；import 自带迁移与升级 |

> **26.3 世界名对照（minigames 实测）**：目录 `mg_hub` → MV 名 `overworld`；`map-jkpof` → `minecraft:map-jkpof`（`mvtp map-jkpof` 可用）；`100_levels_2` → `minecraft:100_levels_2`。判断能不能用于 `/mvtp`，看 `worlds.yml` 的顶层 key（`minecraft:<名>`）或 `legacy-world-name`，**不要拿 `level-name` 目录名当世界名**。

## 9. 边界与关联文档

- 端口 / 转发 / 防火墙基准：[多服网络与端口架构.md](../多服网络与端口架构.md)
- 游戏内跨服入口、三服菜单、NPC、小游戏地图落地配置：[游戏内传送、大厅、菜单等配置.md](../游戏内传送、大厅、菜单等配置.md)
- Velocity 代理原理与迁移总览：[多服Velocity +Multiverse.md](<../多服Velocity +Multiverse.md>)
- 本册只写 Multiverse 插件本身；不重复端口配置与游戏内菜单实现。
