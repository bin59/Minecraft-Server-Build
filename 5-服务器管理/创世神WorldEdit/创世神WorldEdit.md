# 创世神 WorldEdit（官方 7.4 文档口径）

![WorldEdit](worldedit.webp)

本文按 **EngineHub 官方 7.4 新版文档站** 与本地 26.3 主服实测配置整理，覆盖安装、配置、权限、图案/遮罩、全类命令、剪贴板、笔刷、快照与本服注意点。

## 1. 版本与官方文档

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | `plugins/worldedit-bukkit-7.4.6.jar`，`version: 7.4.6+7648-00664b0`，`api-version: 1.21.4`，`folia-supported: true`，`load: STARTUP`，`softdepend: [Vault]` |
| 官方最新正式版 | **7.4.6**（2026-10-15 发布，支持 MC 1.21.4 ~ 26.3）；本地已是最新，无需升级 |
| 官方新版文档站 | <https://worldedit.enginehub.org/en/latest/> — Release **7.4**，PDF 版构建于 2026-11-10，已取代旧 `wiki.worldedit.enginehub.org` |
| 官方下载 | [Modrinth](https://modrinth.com/plugin/worldedit)（推荐）、[Hangar](https://hangar.papermc.io/EngineHub/WorldEdit/versions)、[官方首页](https://enginehub.org/worldedit) |

新版文档站常用页面（本文命令与权限节点均出自这些页）：

| 页面 | 地址 |
| --- | --- |
| 安装 / 快速上手 | `/en/latest/install/` · `/en/latest/quickstart/` |
| 配置 / 权限 | `/en/latest/config/` · `/en/latest/permissions/` |
| 完整命令表 | `/en/latest/commands/` |
| 图案 / 遮罩 | `/en/latest/usage/general/patterns/` · `/en/latest/usage/general/masks/` |
| 历史 / 会话 | `/en/latest/usage/general/history/` · `/en/latest/usage/general/sessions/` |
| 选区 / 区域操作 / 剪贴板 / 生成 | `/en/latest/usage/regions/selections/` · `/en/latest/usage/regions/regionops/` · `/en/latest/usage/clipboard/` · `/en/latest/usage/generation/` |
| 工具 / 笔刷 / 实用 / 快照 | `/en/latest/usage/tools/tools/` · `/en/latest/usage/tools/brushes/` · `/en/latest/usage/utilities/` · `/en/latest/usage/snapshots/` |
| 表达式 / CraftScript / FAQ | `/en/latest/usage/other/expressions/` · `/en/latest/usage/other/craftscripts/` · `/en/latest/faq/` |

## 2. 安装与验证

- **只支持 Java 版**：官方明确 Realms / Windows 10 版 / 基岩版 / 携带版都装不了；服务端官方推荐 **Paper**（优于 Spigot，WorldEdit 能用到其改进接口）。
- 下载与 MC 版本匹配的 **Bukkit/Paper 插件版** jar（别下 Fabric/NeoForge 模组版），放入 `plugins/`，**完整重启**生效。
- 配置目录：插件服 `plugins/WorldEdit/`；NeoForge/Fabric `config/worldedit/worldedit.properties`；Sponge `config/worldedit/worldedit.conf`。

| 验证 | 命令 |
| --- | --- |
| 插件已加载 | `/plugins` |
| 版本信息 | `/worldedit version`（可简写 `/we version`） |
| 命令帮助 | `//help [-s] [-p <页>] [命令]` |
| 重载配置 | `/worldedit reload` |

> 旧教程常写的 `//version` **未列入 7.4 官方命令表**，以 `/worldedit version` 为准。

本地 `plugins/WorldEdit/` 实际内容：`config.yml`、`schematics/`、`sessions/`、`assets/`。另外**服务器根目录**仍有一份 `wepif.yml`（旧 WEPIF 权限接口模板，内容是默认示例 `sk89q` / `admins` 等，resolvers 含 VaultResolver）；jar 内也保留 WEPIF 兼容类。**7.4 的权限统一在 LuckPerms 配 `worldedit.*` 节点**，不要改这份模板里的示例权限，也不要把它当成现行授权入口。

## 3. 本服实际配置（对照 `plugins/WorldEdit/config.yml`，2026-11-14 实测）

> **键名差异**：官方 `config/` 页的设置表用 camelCase **逻辑字段**（如 `defaultChangeLimit`），Bukkit 版 `config.yml` 用 kebab-case **分层键**（如 `limits.max-blocks-changed.default`）。改配置以本地 YAML 键为准，不要照官方表里的名字直接写。

| 本地 YAML 键 | 实际值 | 官方 7.4 对应字段 / 默认值 | 说明 |
| --- | --- | --- | --- |
| `limits.max-blocks-changed.default` / `.maximum` | `-1` / `-1` | `defaultChangeLimit` / `maxChangeLimit`（`-1`） | 单次改动**无上限**，`//limit` 可调 |
| `limits.vertical-height.default` | `256` | `defaultVerticalHeight`（256） | 默认垂直高度 |
| `limits.max-polygonal-points` | `default: -1` / `maximum: 20` | `defaultMaxPolygonalPoints` / `maxPolygonalPoints` | `//sel poly` |
| `limits.max-radius` | `-1` | `maxRadius` | 半径类命令无限制 |
| `limits.max-super-pickaxe-size` | `5` | `maxSuperPickaxeSize`（5） | 超级镐最大范围 |
| `limits.max-brush-radius` | **`5`** | `maxBrushRadius`（默认 6） | 本服比官方默认小 1 |
| `limits.butcher-radius` | `-1` / `-1` | `butcherDefaultRadius` / `butcherMaxRadius` | `/butcher` 无半径限制 |
| `limits.disallowed-blocks` | 60+ 项（树苗、床、铁轨、花草、作物、红石元件、TNT、火、基岩等） | `disallowedBlocks` | 禁止用于图案的「会掉落/引发物理」方块；绕过需 `worldedit.anyblock` |
| `history.size` / `.expiration` | `15` / `10` | — | 保留最近 15 次操作；会话登出后保留 10 分钟 |
| `snapshots.directory` | **空** | `snapshotRepo`（默认空） | **快照未配置，`//restore` / `/snapshot` 不可用**；路径相对服务器根目录，可填绝对路径 |
| `wand-item` | `minecraft:wooden_axe` | `wandItem` | `//wand` 给的选区工具 |
| `navigation-wand.item` / `.max-distance` | `minecraft:compass` / **`100`** | `navigationWand` / `navigationWandMaxDistance`（默认 50） | 本服导航距离比官方默认大一倍 |
| `saving.dir` | `schematics` | `saveDir` | 方案文件目录 |
| `scripting.dir` / `.timeout` | `craftscripts` / `3000` | `scriptsDir` / `scriptTimeout` | CraftScript 目录与超时(ms) |
| `calculation.timeout` | `100` | `calculationTimeout` / `maxCalculationTimeout`(300) | 表达式超时，`//timeout` 可调 |
| `use-inventory.enable` / `.allow-override` | `false` / **`true`** | `useInventory` / `useInventoryOverride`（默认 false） | 本服允许 `worldedit.inventory.unrestricted` 覆盖 |
| `super-pickaxe.drop-items` / `.many-drop-items` | `true` / **`false`** | `superPickaxeDrop` / `superPickaxeManyDrop`（默认 true/true） | 多方块模式不掉物品 |
| `logging.log-commands` | `false` | `logCommands` | 命令日志关闭 |
| `server-side-cui` / `command-block-support` | `true` / `false` | `serverSideCUI` / — | `//drawsel` 可用 |
| `no-op-permissions` / `debug` / `show-help-on-first-use` | `false` / `false` / `true` | — | OP 仍可作为权限回退 |

> **刻意错开（本服专属）**：WorldEdit 导航杖是**指南针** `minecraft:compass`，QuickMenu 触发物品是**时钟 CLOCK**，两者刻意不同避免冲突；`//wand` 给的是**木斧**（选区），别把导航杖当成 `//wand`。另一处已知冲突：BedWars 文档提到指南针会被 WorldEdit 占用做穿墙传送，配 BedWars 时要改绑定物品。

## 4. 权限（LuckPerms，官方 7.4 节点）

**默认无人能用 WorldEdit**，必须给节点或 OP（`no-op-permissions: false` 时 OP 可回退）。管理员直接给 `worldedit.*`；建筑组按需要逐项授予。

| 组 | 官方节点 |
| --- | --- |
| 全局 | `worldedit.*`、`worldedit.help`、`worldedit.reload`、`worldedit.report` |
| 选区 | `worldedit.wand`、`worldedit.wand.toggle`、`worldedit.setwand`（`/selwand`、`/navwand`）、`worldedit.selection.pos/.hpos/.chunk/.expand/.contract/.shift/.outset/.inset/.trim/.size` |
| 分析 | `worldedit.analysis.count`、`worldedit.analysis.distr` |
| 编辑 | `worldedit.region.set/.replace/.overlay/.center/.naturalize/.walls/.faces/.smooth/.snowsmooth/.line/.curve/.move/.stack/.deform/.hollow/.forest/.flora` |
| 生成 | `worldedit.generation.cylinder/.sphere/.cone/.pyramid/.shape/.shape.biome/.forest/.pumpkins/.feature/.structure` |
| 剪贴板 / 方案 | `worldedit.clipboard.copy/.cut/.paste/.rotate/.flip/.clear/.load/.save/.share`、`worldedit.schematic.list/.formats/.load/.save/.delete/.share` |
| 历史 | `worldedit.history.undo`（含 `.self`）、`.redo`（含 `.self`）、`.clear` |
| 限制 | `worldedit.limit`、`worldedit.timeout`、`worldedit.global-mask`、`worldedit.placement`、`worldedit.perf`、`worldedit.fast` |
| 导航 | `worldedit.navigation.unstuck/.ascend/.descend/.ceiling/.up/.thru.command/.jumpto.command`；导航杖左右键另需 `.thru.tool` / `.jumpto.tool` |
| 工具 / 超级镐 | `worldedit.tool.*`（`info/tree/replacer/data-cycler/flood-fill/deltree/farwand/lrbuild/stack`）、`worldedit.superpickaxe`（+ `.area` / `.recursive`） |
| 笔刷 | `worldedit.brush.<类型>`（sphere/cylinder/set/smooth/…）、`worldedit.brush.options.mask/.material/.range/.size/.tracemask` |
| 生物群系 / 区块 | `worldedit.biome.list/.info/.set`、`worldedit.chunkinfo`、`worldedit.listchunks`、`worldedit.delchunks` |
| 快照 / 脚本 | `worldedit.snapshots.restore/.list`、`worldedit.scripting.execute`、`worldedit.scripting.execute.<文件名>` |
| 实用 | `worldedit.fill`、`worldedit.fill.recursive`、`worldedit.drain`、`worldedit.fixlava/.fixwater`、`worldedit.removeabove/.removebelow/.removenear/.replacenear`、`worldedit.snow/.thaw/.green/.extinguish`、`worldedit.butcher`、`worldedit.remove`、`worldedit.calc` |

**特殊权限**（官方 "Other Permissions"）：`worldedit.anyblock`（用被禁方块）、`worldedit.limit.unrestricted`（突破 `//limit` 上限）、`worldedit.timeout.unrestricted`、`worldedit.inventory.unrestricted`、`worldedit.override.bedrock`、`worldedit.override.data-cycler`、`worldedit.setnbt`（**告示牌文字/箱子内容/方块实体必需**）、`worldedit.report.pastebin`。

**旧写法纠正**（仓库旧文档里出现过，7.4 官方权限表已无这些节点）：

| 错误节点 | 正确写法 |
| --- | --- |
| `worldedit.limit.<数字>` / `worldedit.bypass` | `worldedit.limit` + `//limit`；绕过上限用 `worldedit.limit.unrestricted` |
| `worldedit.set` / `.replace` / `.copy` / `.paste` / `.undo` / `.redo` | `worldedit.region.set` / `worldedit.clipboard.copy` / `worldedit.history.undo` 等完整前缀 |
| `worldedit.utility.*` | 不存在该组，实用命令各自独立（`worldedit.fill`、`worldedit.butcher` …） |
| `worldedit.scripting.*` / `.cui` | `worldedit.scripting.execute`（+ `.<文件名>` 精确控制） |
| `worldedit.navigation.jumpto` / `.thru` | 命令版 `.jumpto.command` / `.thru.command`；导航杖另需 `.jumpto.tool` / `.thru.tool` |
| `worldedit.schematic.load` / `.save` 单独给 | `/schematic load` 还需 `worldedit.clipboard.load`，`save` 还需 `worldedit.clipboard.save` |

> 本服 LuckPerms 用 `*` 通配会命中全部节点（此前 `offlinereward.exempt` 被 admin 组 `*` 误命中的坑），给建筑组别偷懒用通配。

## 5. 快速上手（5 步）

```
//wand                  # 拿木斧
# 左键点角 / 右键点对角 → 设 pos1 / pos2
//set stone             # 填充
//undo                  # 撤销
//copy → //schem save mybuild   # 存盘
```

## 6. 选区与导航

| 命令 | 说明 |
| --- | --- |
| `//wand [-n]` | 给选区木斧；`-n` 给导航杖 |
| `//pos1 [坐标]` / `//pos2 [坐标]` / `//hpos1` / `//hpos2` | 用当前位置或准星指向设点 |
| `//pos [pos1] [pos2...] [-s <选择器>]` | 一次性设点并切换选区类型 |
| `//sel [-d] [选择器]`，别名 `//desel` | 切换/清除选区（cuboid / poly / convex / ellipsoid / sphere / cylinder）；不带参数即取消 |
| `//expand <数量 或 vert> [反向量] [方向]` / `//contract` / `//outset [-hv] <数量>` / `//inset` / `//shift` | 扩缩与平移；`//expand vert` 垂直铺满 |
| `//chunk [-cs] [坐标]` | 选区块（256 格高），`-c` 用区块坐标、`-s` 扩展包含所含区块 |
| `//size [-c]` / `//count <遮罩>` / `//distr [-cd] [-p <页>]` | 选区大小 / 计数 / 方块分布 |
| `/unstuck` `/ascend` `/descend` `/ceil [-fg]` `/thru` `/jumpto` `/up [-fg] <距离>` | 导航；指南针左键 `/jumpto`、右键 `/thru` |

## 7. 图案（Pattern）与遮罩（Mask）—— 7.4 重点

**图案**决定「放什么」，**遮罩**决定「哪些方块受影响」。全局遮罩 `//gmask [mask]`（不带参数清除）、笔刷遮罩 `/mask [mask]`，多层遮罩会**叠加求交**。

| 图案写法 | 示例 |
| --- | --- |
| 单方块 / 方块状态 / NBT | `//set stone`、`//set oak_stairs[facing=east,half=top]`、`//set oak_sign{'is_waxed':1}` |
| 手持 / pos1 方块 | `//set hand`、`//set offhand`、`//set pos1` |
| 随机加权（权重是相对值，无需凑 100） | `//set 50%red_wool,10%red_stained_glass` → 5:1 |
| 随机状态 / 标签 | `//set *oak_log`、`//set ##wool`、`//set ##*slabs` |
| 类型/状态套用（保留朝向等） | `//replace oak_stairs ^acacia_stairs`、`//set ^[waterlogged=false]` |
| **剪贴板图案（7.4）** | `//replace #existing #clipboard`、`//set #clipboard@[2,0,1]` |
| 特殊数据 | `//set oak_sign\|Line1\|Line2`、`//set player_head\|dinnerbone`、`//set spawner\|squid`（有空格需整体加引号） |

| 遮罩写法 | 示例 |
| --- | --- |
| 方块 / 否定 / 交集 | `//replace !dirt,stone,grass_block stone`、`//replace "stone <air" dirt` |
| 非空气 / 固体 / **完整方块（7.4）** | `#existing`、`#solid`、`#fullcube` |
| 上方 / 下方 / 相邻 | `>##planks`、`<mask`、`~spawner` |
| 选区 / 剪贴板 / 标签 / 表面 | `#sel` `#dsel`、`#clipboard`、`##carpets`、`#surface` |
| 随机噪声 / 状态 / 表达式 / 群系 | `%50`、`^=[open=false]`、`=y<64`、`$minecraft:plains` |

> 7.4 新增 `#clipboard` 图案与遮罩、`#fullcube` 遮罩、`//regen -c`（把自然生成结果输出到剪贴板），配合 `//replace #existing #clipboard` 可把地形按剪贴板样式批量重铺。

## 8. 区域编辑与生成

| 命令 | 说明 |
| --- | --- |
| `//set <图案>` | 填充选区 |
| `//replace [原遮罩] <新图案>` | 替换；省略原遮罩 = 全部非空气 |
| `//overlay` `//center` `//walls` `//faces <图案>` | 表层覆盖 / 中心 / 围墙 / 六面外壳 |
| `//naturalize` `//smooth [次数] [遮罩]` `//snowsmooth` | 自然化地形 / 平滑 / 雪地平滑 |
| `//hollow [厚度] [图案]` `//line [-h] <图案> [厚度]` `//curve` | 挖空 / 连线 / 曲线 |
| `//move [-abes] [倍数] [偏移] [替换图案]` / `//stack [-abers] [次数] [偏移]` | 移动 / 连续复制；`-a` 忽略空气、`-e` 含实体、`-b` 含群系、`-s` 移动选区 |
| `//regen [-bc] [种子]` | 按种子重生地形，`-b` 含群系、`-c` 输出到剪贴板 |
| `//deform` `//generate <图案> <表达式>` `//generatebiome` | 表达式造型（语法见官方 Expression Syntax） |
| `//forest [类型] [密度]` `//flora [密度]` `//update` | 植树 / 植被 / 方块更新 |
| `//sphere [-hr] <图案> <半径>` `//hsphere` `//cyl [-h] <图案> <半径> [高]` `//hcyl` `//cone [-h]` `//pyramid [-h]` `//hpyramid` | 几何体；半径可用 `x,y,z` 三轴不等 |
| `/forestgen` `/pumpkins` `//feature` `//structure` | 森林 / 南瓜田 / 地物 / 结构 |

## 9. 剪贴板与 schematic

| 命令 | 说明 |
| --- | --- |
| `//copy [-be] [-m <遮罩>]` / `//cut [-be] [遗留图案]` | 复制/剪切；**会记住你与选区的相对位置** |
| `//paste [-abenosv] [-m <源遮罩>]` | `-a` 不贴空气、`-o` 贴回原始位置、`-n` 只预演选区不贴、`-s` 把选区移到粘贴处、`-e` 实体、`-b` 群系 |
| `//rotate <Y> [X] [Z]` / `//flip [方向]` / `//revolve` | 旋转/镜像/环绕；仅 X 轴 `//rotate 0 90` |
| `/clearclipboard` | 清空剪贴板 |
| `/schematic <list\|formats\|load\|delete\|save\|share>`（别名 `//schem`） | 方案管理 |
| `/schematic save [-f] <文件名> [格式]` / `load <名称> [格式]` / `list [-dn] [-p <页>]` / `formats` / `delete <名称>` / `share` | 存/取/列/格式/删/分享；文件在 `plugins/WorldEdit/schematics/` |

> 剪贴板存在**会话**里，登出后保留 10 分钟；`//paste -o` 可忽略相对位置贴回原处。跨世界复制要先把内容存成 schematic。

## 10. 历史、会话与性能

```
/undo [次数] [玩家]     /redo [次数] [玩家]     /clearhistory
//limit [上限]          //timeout [毫秒]        //drawsel [开关]
//perf [-h] [副作用] [状态]     //reorder [模式]     //world [世界]
/gmask [遮罩]           /toggleplace            /placement <类型> [倍数] [偏移]
/searchitem [-bi] [-p <页>] <关键词>       //registry <注册表> [查询]
/worldedit <help|version|trace|reload|cui|tz|report>
```

- 历史默认存 **15 次**（本服 `history.size: 15`），`//undo 5` 一次撤 5 步；有权限可撤他人操作（登出后 10 分钟内仍可）。
- **只记录直接改动**：液体流动、门/牌子/草因失去依附而掉落等间接变化不在历史里。
- 会话含选区、历史、剪贴板、工具绑定、改动上限等；登出 10 分钟后丢弃。选区杖/导航杖绑定、CUI 开关、默认选择器等为持久数据。
- `//fast` **已弃用**，官方要求改用 `//perf`（WorldEdit 8 会移除 `//fast`）；`//perf` 格式未来仍会调整，别写死依赖。
- 大编辑内存经验值（官方 FAQ）：基础 1 GB + 每 1000 万方块约 2 GB；Paper 可用 `spigot.yml` 的 `timeout-time` 缓解 watchdog。本服 `limits.max-blocks-changed` 为 `-1`（无上限），大面积操作仍要自行控制。

## 11. 工具、超级镐与笔刷

工具/笔刷**绑定到手持物品**，右键触发（部分支持左键），用 `/tool none` 或 `/brush none` 解绑（两者等价）。

```
/tool <cycler|repl|farwand|floodfill|deltree|stacker|selwand|tree|none|info|lrbuild|navwand>
/tool repl <图案>       /tool floodfill <图案> <范围>     /tool tree [类型]
/tool lrbuild <主手> <副手>      /tool stacker [范围] [遮罩]
/mask [遮罩]  /material <图案>  /range <范围>  /size <尺寸>  /tracemask [遮罩]
/superpickaxe <single|area|recursive>      /superpickaxe area <范围>
/brush sphere [-h] <图案> [半径]      /br cyl [-h] <图案> [半径] [高]
/brush set <形状> [尺寸] <图案>       /brush smooth [半径] [次数] [遮罩]
/brush clipboard [-abeov]            /brush extinguish [半径]
/brush forest <形状> [半径] [密度] <树类型>          /brush biome [-c] <形状> [半径] <群系>
/brush gravity [半径] [-h <高度>]     /brush heightmap [-efr] <图片名> [半径] [强度]
/brush raise/lower <形状> [半径]      /brush butcher [-abfgnprtw] [半径]
/brush splatter <图案> [半径] [衰减]  /brush deform/erode/dilate/morph/snow/snowsmooth
/brush none
```

笔刷最大半径受 `limits.max-brush-radius` 限制（本服 **5**），超级镐范围上限 5。

## 12. 生物群系 / 区块 / 快照 / 脚本 / 实用

```
/biomelist [-p <页>]   /biomeinfo [-pt]   //setbiome [-p] <群系>   //replacebiome [-p] <原> <新>
/chunkinfo             /listchunks [-p <页>]      /delchunks [-o <时间点>]
/snapshot <before|after|use|sel|list>       /restore [快照]
/cs <文件名> [参数...]                       /.s [参数...]
//fill <图案> <半径> [深度]   //fillr <图案> <半径> [深度]   //drain [-w] <半径>
/fixlava <半径>  /fixwater <半径>  /extinguish [半径]  /butcher [-abfgnprtw] [半径]
/removeabove [尺寸] [高度]   /removebelow   /removenear <遮罩> [半径]
/replacenear <半径> [原] <新>   /snow [-s]   /thaw   /green [-f]   /remove <类型> <半径>
//calculate <表达式...>
```

- **快照本服不可用**：`snapshots.directory` 为空。要用需先填备份目录（相对**服务器根目录**，不是 WorldEdit 目录），放入带时间戳的世界备份 zip/文件夹，再 `//restore` 或 `/snapshot use|before|after|sel`。
- CraftScript 脚本放 `plugins/WorldEdit/craftscripts/`，需 `worldedit.scripting.execute`（可按 `.<文件名>` 精确授权），超时 3000 ms。
- `//setbiome` 自 1.15 起群系是 3D、最小 4×4×4，改完需重进世界才看得到。

## 13. 完整命令索引

官方 `/en/latest/commands/` 共 **14 类约 200 条** 命令，本文第 6~12 节已覆盖常用项；全量语法用 `//help` 或官方页面查：

| 分类 | 官方页内小节 |
| --- | --- |
| General / Navigation / Selection / Region | 通用、导航、选区、区域编辑 |
| Generation / Schematic and Clipboard | 几何生成、剪贴板与方案 |
| Tool / Super Pickaxe / Brush | 工具、超级镐、笔刷 |
| Biome / Chunk / Snapshot / Scripting / Utility | 群系、区块、快照、脚本、实用 |

参数约定：`[ ]` 可选、`< >` 必填；`//` 前缀与 `/` 前缀在多数命令上互为别名（如 `/undo` = `//undo`、`/schematic` = `//schem`）。

## 14. 本服注意点

1. **跨端**：WorldEdit 是 Java 版插件，基岩客户端经 Geyser 连入后命令由服务端执行、可用，但需手动输入，选区木斧左右键等交互体验与 Java 端可能不同（未在服内实测），建筑作业建议用 Java 端。
2. **CoreProtect**：本服 `worldedit: true` 会记录 WorldEdit 改动，`r:#worldedit` 可在 WE 选区内回滚（见 CoreProtect 文档）；`//undo` 与 `/co rollback` 是两套独立机制。
3. **权限**：默认无人可用；给建筑者权限时别用 `*` 通配，按 §4 逐项给；需要告示牌/箱子内容必须加 `worldedit.setnbt`。
4. **全局遮罩残留**：命令返回「0 blocks changed」先跑一次 `//gmask`（不带参数）清全局遮罩。
5. **大范围操作**：本服无改动上限，配合 CoreProtect 记录与 `//limit` 自行设临时上限；操作前确认 Residence 领地与 `//undo` 可用。
6. **Java 版限定**：不要把基岩版客户端的 `.mcpack` / 资源包与 WorldEdit 混为一谈，WE 无需客户端装任何东西。

## 15. 7.4 相对旧版的主要变化

| 变化 | 来源 |
| --- | --- |
| 文档站迁到 `worldedit.enginehub.org/en/latest/`（Release 7.4），旧 wiki 退役 | 官方站 + PDF 2026-11-10 |
| `#clipboard` 图案/遮罩、`#fullcube` 遮罩、`//regen -c` | 官方 Patterns / Masks / Commands 页 |
| `//fast` 弃用，改用 `//perf`；`//perf` 格式后续仍会变 | 官方 Commands 页警告 |
| 7.4.5 起支持 Fabric Permissions API v1（Bukkit/LuckPerms 不受影响，仍用 `worldedit.*` 点分节点） | 官方 Permissions 页 |
| 权限统一走 LuckPerms 的 `worldedit.*` 树；根目录 `wepif.yml` 为旧 WEPIF 兼容模板，不再是授权入口 | 官方 Permissions 页 + 本地 26.3 主服实测 |
| 方块状态 / NBT / 标签图案语法（`block[state]{nbt}`、`##tag`、`^` 套用）成为标准写法 | 官方 Patterns 页 |
