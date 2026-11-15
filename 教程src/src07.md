# 七、服务器管理工具

## WorldEdit（创世神）

> 本服 `//wand` 给的是**木斧**（选区工具），导航杖是**指南针**。完整 7.4 命令、图案/遮罩、权限与配置对照见 `5-服务器管理/创世神WorldEdit/创世神WorldEdit.md`。

### 玩家命令（建筑者可用，需权限）

**选区**：`//wand` 拿木斧（左键点方块设 pos1，右键设 pos2）；`//pos1` / `//pos2` 以当前位置设点；`//expand <数量> [方向]` 扩大；`//expand vert` 垂直铺满；`//size` 查看大小；`//desel` 取消。

**填充与替换**：`//set <图案>`（如 `//set stone`）；`//replace [原遮罩] <新图案>`（省略原遮罩即全部非空气）；`//walls <图案>` 只生成围墙；`//hollow` 内部挖空。

**复制 / 粘贴 / 移动**：`//copy`；`//paste`；`//paste -a` 忽略空气；`//move <距离>`；`//stack <次数>` 连续复制。

**旋转与镜像**：`//rotate <角度>`（90/180/270）；`//flip <方向>`。

**生成几何体**：`//sphere <方块> <半径>` 实心球；`//hsphere` 空心球；`//cyl <方块> <半径> <高度>` 实心圆柱；`//hcyl` 空心圆柱。

**撤销 / 重做**：`//undo`；`//redo`。

**保存 / 加载（schematic）**：`//schem save <名称>` 保存剪贴板到 plugins/WorldEdit/schematics/；`//schem load <名称>` 载入，随后 `//paste`。

**导航（指南针）**：`/jumpto` 传送到准星所指方块；`/thru` 穿过面前的墙；`/unstuck` 解除卡墙。

### 管理员命令

- `/worldedit version`（`/we version`）— 查看版本；旧写法 `//version` 不在 7.4 官方命令表
- `/worldedit reload` 重载配置、`/worldedit report [-p]` 生成报告
- 通过 LuckPerms 给建筑组逐项授权，**别用 `*` 通配**

### 权限节点（官方 7.4）

- `worldedit.*` — 全部
- 选区：`worldedit.wand`、`worldedit.selection.pos/.hpos/.chunk/.expand/.contract/.shift/.size`
- 编辑：`worldedit.region.set/.replace/.walls/.faces/.move/.stack/.hollow`
- 剪贴板：`worldedit.clipboard.copy/.cut/.paste/.rotate/.flip`；方案 `worldedit.schematic.load/.save` **还需** `worldedit.clipboard.load/.save`
- 历史：`worldedit.history.undo/.redo/.clear`；生成：`worldedit.generation.sphere/.cylinder/.shape`
- 导航：命令 `worldedit.navigation.jumpto.command` / `.thru.command` / `.unstuck`，指南针左右键另需 `.jumpto.tool` / `.thru.tool`
- 限制：`worldedit.limit`（配合 `//limit`）、`worldedit.limit.unrestricted`（绕过上限）、`worldedit.timeout.unrestricted`
- 特殊：`worldedit.setnbt`（告示牌/箱子内容必需）、`worldedit.anyblock`（用被禁方块）、`worldedit.global-mask`

> 旧写法 `worldedit.limit.<数字>`、`worldedit.bypass`、`worldedit.set`、`worldedit.utility.*`、`worldedit.navigation.jumpto` 在 7.4 官方权限表中均已不存在，完整对照见插件文档 §4。

### 典型工作流

**玩家（建筑者）**：`//wand` 拿木斧 → 左键点一角、右键点对角 → `//set stone` 填充 → 不满意 `//undo` → 建好 `//copy` → `//schem save mybuild` 存盘 → 换地点 `//schem load mybuild` → `//paste`。

### 版本信息

本地与官方最新正式版均为 7.4.6（2026-10-15，支持 MC 1.21.4–26.3）/ 官方文档 <https://worldedit.enginehub.org/en/latest/>（Release 7.4，2026-11-10 构建）/ 查证日期 2026-11-14

---

## OpenInv（离线背包查看）

> 该插件仅管理员使用，玩家无直接命令；玩家可感知的是"自己的背包/末影箱可能被管理员查看或编辑"（离线也能）。

### 管理员命令

- `/openinv <玩家>` — 打开指定玩家背包（支持离线；别名 /invsee、/inv）
- `/openender <玩家>` — 打开指定玩家末影箱（支持离线；别名 /invec）
- `/searchinv <物品>` — 搜索在线玩家背包中的某物品
- `/searchcontainer <物品> <半径>` — 搜索以自己为中心的指定半径内的容器
- `/silentcontainer` — 开关"静默容器"（打开容器不播放动画/声音）
- `/anycontainer` — 开关"任意容器"（即使容器被方块挡住也能打开）

### 权限节点

- `openinv.*` — 全部
- `openinv.open.self` / `openinv.open.ender.self` — 打开自己的背包/末影箱
- `openinv.open.*` / `openinv.open.offline` — 打开他人（含离线）背包
- `openinv.search` — 使用搜索命令
- `openinv.modify` — 在界面中实际移动/修改物品（读写权限）
- `openinv.exempt` — 自身背包受保护，低权限 staff 无法打开
- `openinv.override` — 紧急情况下越过 exempt 保护

### 典型工作流

**管理员视角**：玩家报"物品丢了" → `/openinv 玩家名` 查看/补发 → 怀疑藏物于末影箱 → `/openender 玩家名` → 批量排查某非法物品 → `/searchinv 物品名` 或 `/searchcontainer 物品名 10`。

### 版本信息

最新版本 5.3.5（Modrinth，约 2026-10）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/openinv

---

## ClearLag++（清理与优化）

> 自动清理每 600 秒（10 分钟）执行一次并全服倒计时广播；玩家主要感知"地面杂物被定时清理"及可关闭清理提示。

### 玩家命令

- `/cl togglemessage` — 切换是否接收清理倒计时广播
- `/cl togglesound` — 切换是否播放清理倒计时音效

### 管理员命令

- `/clearlag` — 主帮助菜单
- `/clearlag clear <items|entity|all> [--preview]` — 手动清理；--preview 仅报告不移除
- `/clearlag killmobs <evil|good|all>` — 手动击杀生物
- `/clearlag nuke [半径]` — 清理半径内所有非玩家实体
- `/clearlag specs` — 显示 TPS/CPU/内存/运行时间
- `/clearlag history <30m|24h|7d>` — 查看性能历史图表
- `/clearlag chunkfinder <数量>` — 查找最卡的区块
- `/clearlag gui` — 打开 GUI 仪表盘
- `/clearlag reload` — 重载配置

### 权限节点

- `clearlag.clear` / `clearlag.killmobs` / `clearlag.nuke` / `clearlag.reload` / `clearlag.admin` / `clearlag.bossbar` / `clearlag.web`

### 典型工作流

**玩家视角**：清理倒计时刷屏烦人时 `/cl togglemessage` 关掉广播；`/cl togglesound` 关掉音效。

**管理员视角**：发现区块卡顿 → `/clearlag chunkfinder 5` 找最卡区块 → `/clearlag clear items --preview` 先看会清多少 → `/clearlag clear all`；某区域生物过多 → `/clearlag killmobs evil`。

### 版本信息

最新版本 2.0.0（支持 MC 1.18–26.3）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/clearlag++

---

## BedrockPlayerSupport（基岩版 GUI 表单）

> 仅对**基岩版玩家**（经 GeyserMC + Floodgate 接入）生效，提供 GUI 表单替代敲指令。前置：GeyserMC + Floodgate 必须安装。本服当前关闭了家园、经济、点券、死亡回传等表单。

### 玩家命令（基岩版玩家）

- `/tpgui` — 打开传送请求表单，点选目标玩家发送 tpa
- `/warpgui` — 打开传送点列表表单，点选传送点传送
- `/msggui` — 打开私信表单，点选玩家发消息
- `/kitgui` — 打开工具包领取表单，点选领取
- 收到 tpa/tpahere 请求时，**自动弹出**接受/拒绝/忽略表单

> 以上表单底层调用 EssentialsX 等命令，玩家需同时拥有对应前置插件权限。已关闭（本服不可用）：/homegui、/phomegui（家园）、/paygui（经济）、/pointsgui（点券）。

### 管理员命令

- `/bps reload` — 重载插件配置

### 权限节点（LuckPerms）

- `bedrockplayersupport.tpgui` / `.warpgui` / `.msggui` / `.kitgui` — 各表单权限
- `bedrockplayersupport.*` — 全部（管理员组）
- `bedrockplayersupport.form.backdeath` — 死亡回传表单权限（默认所有玩家拥有）

### 典型工作流

**基岩版玩家视角**：想传送到朋友 → `/tpgui` 点玩家头像发请求；别人请求 → 屏幕自动弹表单点接受；领每日工具包 → `/kitgui` 点选礼包；发私信 → `/msggui`。

**管理员视角**：给 default 组授予 `bedrockplayersupport.tpgui` `warpgui` `msggui` `kitgui`；改了 config.yml 后 `/bps reload`。

### 版本信息

最新版本 2.1.1（2025-09-17，支持 MC 1.18–1.21.8）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/bedrockplayersupport

---

## UuidMigrate（玩家数据迁移）

> 用于在服务器 Online（正版）↔ Offline（离线）模式切换时迁移玩家背包、统计、成就数据而不丢失进度。玩家加入时**自动检测并迁移**，通常无需手动操作。玩家无直接命令。

### 管理员命令

- `/uuidmigrate status <玩家>` — 查看该玩家是否有待迁移数据
- `/uuidmigrate migrate <玩家>` — 手动强制迁移该玩家数据
- `/uuidmigrate dryrun <玩家>` — 预演：只显示会做什么，不实际改动
- `/uuidmigrate reload` — 重载配置

### 权限节点

- `uuidmigrate.admin` — 全部命令权限（默认仅 OP）

### 典型工作流

**管理员视角（切换正版/离线模式前）**：先在测试服对目标玩家执行 `/uuidmigrate dryrun <玩家>` 预演 → 切换服务端 online-mode 并重启 → 玩家加入时插件自动迁移（默认 COPY 安全模式并自动备份）→ 个别未自动迁移 `/uuidmigrate migrate <玩家>`。注意：LuckPerms 等权限插件数据**不会**自动迁移，需另行处理。

### 版本信息

最新版本 1.0.0（2026-01-04，支持 1.19–26.3，Java 17+）/ 查证日期 2026-11-13 / 来源 https://www.curseforge.com/minecraft/bukkit-plugins/useroveruuid

---

## PlanTop（在线时间排行榜）

> 自研插件：从 Plan 数据库读取玩家历史总在线时间，注册 PlaceholderAPI 占位符，供 TAB / 计分板 / QuickMenu 展示 Top 30 排行榜。纯服务端组件，玩家/管理员无独立命令，玩家可感知的是"排行榜上能看到自己的在线时长排名"。

### 管理员命令

无 PlanTop 专属命令；验证占位符是否生效可用 PlaceholderAPI 命令：
- `/papi parse me %ptop_1_name%` — 应返回第 1 名玩家名
- `/papi parse me %ptop_updated%` — 应返回最后刷新时间

### 可用占位符

- `%ptop_<N>_name%` / `%ptop_<N>_displayname%` — 第 N 名玩家名/显示名（N=1..30）
- `%ptop_<N>_time%` — 第 N 名总时长（仅显示分钟）
- `%ptop_updated%` — 最后成功刷新时间
- `%ptop_error%` — 最近错误信息（ok 为正常）

### 版本信息

自研 1.0.0，本服自研 / 查证日期 2026-11-13

---

## AntiLitematica（防打印机作弊）

> 服务端层面检测并阻断 Litematica / Schematica 模组的"打印机(Printer)""快速放置(EasyPlace)"等作弊功能。玩家无需、也不应安装任何客户端模组；玩家可感知的是"使用打印机/快速放置时被拦截并收到提示"。前置：ProtocolLib。

### 玩家命令

- 无玩家可直接使用的命令。使用 Litematica 打印机或快速放置时会被服务端拦截；本服当前 detection.action: LOG（仅记录日志，不自动踢/封）。

### 管理员命令

- `/antilitematica reload` — 重新加载配置文件
- `/antilitematica test <玩家名>` — 手动扫描指定玩家是否使用违规模组

### 权限节点

- `antilitematica.admin` — 允许使用所有管理命令
- `antilitematica.bypass` — 拥有此权限的玩家**免受检测**（适合管理员/建筑比赛裁判）

### 典型工作流

**管理员视角（排查疑似作弊）**：收到违规日志或怀疑某玩家 → `/antilitematica test <玩家名>` 手动扫描 → 确认裁判/管理员需要豁免检测 → 授予 `antilitematica.bypass` → 改了 config.yml 后 `/antilitematica reload`。

### 版本信息

最新版本 7.0.1 / 7.0.1-fix（2026-08，支持 1.12.2–26.2，需 ProtocolLib）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/antilitematica
