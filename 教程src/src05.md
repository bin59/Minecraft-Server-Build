# 五、家园 / 地图 / 模型 / 附魔 / 死亡 / 皮肤

## Residence（领地系统）

> 默认圈地工具为 `木锄`（左键设点 A，右键设点 B）。

### 玩家命令

| 命令 | 语法 / 参数 | 说明 |
|---|---|---|
| 创建领地 | `/res create <领地名>` | 基于当前选区创建 |
| 选区纵向铺满 | `/res select vert` | 领地从基岩到天空全覆盖 |
| 查看领地信息 | `/res info [领地名]` | 站领地内可直接 `/res info` |
| 列出我的领地 | `/res list` | 列出自己全部领地 |
| 设置旗帜 | `/res set <领地名> <flag> true/false` | `/res set myhome pvp false` |
| 设置玩家旗帜 | `/res pset <领地名> <玩家> <flag> true/false` | `/res pset myhome Steve build true` |
| 一键授权玩家 | `/res pset <领地名> <玩家> trusted true` | 给某玩家领地内全部权限 |
| 设为领地管理员 | `/res pset <领地名> <玩家> admin true` | 让好友帮你管理 |
| 传送 / 设传送点 | `/res tp <领地名>` / `/res tpset` | 传送到领地 / 设当前点为传送点 |
| 卡住脱困 | `/res unstuck` | 卡领地内时回到安全点 |
| 扩建 / 缩小 | `/res expand [方向] <数量>` / `/res contract [方向] <数量>` | `/res expand north 10` |
| 平移领地 | `/res move <方向> <数量>` | `/res move north 10` |
| 子领地 | `/res subzone <父领地> <子领地>` | `/res subzone myhome shop` |
| 进入/离开提示 | `/res message <领地名> enter <消息>` / `leave <消息>` | |
| 领地聊天 | `/res rc join` / `/res rc <消息>` / `/res rc leave` | 仅成员可见的频道 |
| 删除领地 | `/res remove <领地名>` | 删除后保护消失 |
| 领地银行 | `/res bank <领地名>` / `deposit <领地名> <金额>` / `withdraw <领地名> <金额>` | 领地公共钱袋 |
| 限制查询 | `/res limits` | 查看还可圈多大 |
| 显示边界 | `/res show <领地名>` | 粒子显示领地边框 |

常用旗帜（flag）：`build`、`destroy`、`use`、`container`、`move`、`tp`、`pvp`、`damage`、`mobkilling`、`tnt`、`explode`、`creeper`、`firespread`、`flow`、`monsters`、`bed`、`brew`、`enchant`、`anvil`、`fly`、`keepinv`、`trusted`、`bank`。

> 基岩玩家名带 `.` 前缀（如 `.shangxin2179`），`/res pset` 等命令必须手打全名（含点）。

### 管理员命令

本服管理命令前缀为 `/resadmin`（非 /res admin），需 `residence.admin` 权限。

| 命令 | 说明 |
|---|---|
| `/resadmin setowner <领地名> <玩家>` | 转移领地归属 |
| `/resadmin remove <领地名>` | 删除任意领地 |
| `/resadmin removeall <玩家>` | 删除某玩家全部领地 |
| `/resadmin give <领地名> <玩家>` | 赠送领地 |
| `/resadmin tp <领地名>` | 传送到任意领地 |
| `/resadmin info <领地名>` | 查看任意领地详情 |
| `/resadmin rename <旧名> <新名>` | 重命名 |
| `/res reload` | 刷新配置 |
| `/res gui` | 打开可视化旗帜编辑器 |

### 权限节点

- `residence.admin`：全部管理命令
- 玩家默认即可创建/管理自己领地，无需额外授权
- 查看/修改他人领地需 `residence.admin`

### 典型工作流

**玩家圈地**：拿木锄左键点 A → 右键点 B → `/res select vert` → `/res create myhome` → `/res set myhome build false` → `/res pset myhome 朋友 build true` → `/res tp myhome` 回家。

**管理员处理纠纷**：`/resadmin info 领地` 查归属 → `/resadmin setowner 领地 玩家` 转交 → 或 `/resadmin remove 领地` 强制删除。

### 版本信息

最新版本：Residence 6.0.4.1（2026-11-12，支持至 26.3）/ 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/residence.11480/

---

## BlueMap（网页世界地图）

### 玩家命令

BlueMap 是网页 3D 世界地图，**玩家无游戏内命令**。查看地图只需用浏览器打开地图地址（如 http://服务器:8100）：拖拽旋转、滚轮缩放、点击标记查看领地名。无需任何游戏内权限，地图为公开网页。

### 管理员命令

| 命令 | 权限节点 | 说明 |
|---|---|---|
| `/bluemap` | `bluemap.status` | 显示渲染状态/进度 |
| `/bluemap reload [light]` | `bluemap.reload` | 重载资源/配置/Web 服务 |
| `/bluemap maps` | `bluemap.maps` | 列出已加载地图 |
| `/bluemap stop` / `start` | `bluemap.stop` / `bluemap.start` | 暂停/恢复全部渲染 |
| `/bluemap purge <map-id>` | `bluemap.purge` | 清空某地图渲染数据 |
| `/bluemap update [map-id] [x z] [半径]` | `bluemap.update` | 手动更新（只渲变化区块） |
| `/bluemap tasks` / `tasks cancel` | `bluemap.tasks` | 查看/取消渲染队列 |

### 权限节点

- 玩家查看网页地图：无需任何权限（匿名访问）
- 管理命令：`bluemap.*`（给 admin/owner）；或逐项 `bluemap.status` / `bluemap.reload` / `bluemap.update` 等

### 典型工作流

**玩家看地图**：浏览器开 http://服务器:8100 → 切换主世界/下界/末地标签 → 旋转缩放找自己建筑 → 点标记看领地名。

**管理员重渲某区**：`/bluemap update world` 全图增量；或 `/bluemap update world 100 200 50` 只渲以 (100,200) 为中心半径 50 的区域。

### 版本信息

最新版本：BlueMap 5.29（2026-10-10，支持 1.16.5–26.3，需 Java 25）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/bluemap

---

## FreeMinecraftModels（自定义模型）

### 玩家命令

自定义模型（家具/装饰/武器/宠物/伪装），Java 与基岩玩家均可看到，玩家无需装客户端 mod。

| 命令 | 权限 | 说明 |
|---|---|---|
| `/fmm` | `freeminecraftmodels.menu`（默认全员） | 打开有配方的模型合成菜单 |
| `/fmm shop` | `freeminecraftmodels.shop`（默认全员） | 家具商店（需服务端开启 Vault 经济） |
| `/fmm disguise <模型ID>` | `freeminecraftmodels.disguise.self` | 把自己伪装成模型 |
| `/fmm undisguise` | `freeminecraftmodels.disguise.self` | 解除伪装 |
| `/fmm pets` | — | 宠物收集菜单 |

### 管理员命令

| 命令 | 权限 | 说明 |
|---|---|---|
| `/fmm admin` | `freeminecraftmodels.admin` | 内容浏览器 |
| `/fmm reload` | `freeminecraftmodels.*` | 重载配置/模型/配方并重新生成资源包 |
| `/fmm spawn static|dynamic|prop <id>` | `freeminecraftmodels.*` | 放置模型 |
| `/fmm mount <id> [scale]` | `freeminecraftmodels.*` | 生成可骑乘模型 |
| `/fmm giveitem <物品>` | `freeminecraftmodels.admin` | 发放已注册 FMM 物品 |
| `/fmm disguise <模型ID> <玩家>` | `freeminecraftmodels.disguise.others` | 伪装他人 |
| `/fmm deleteall [半径]` | `freeminecraftmodels.deleteall` | 移除全部已加载模型实体（危险） |

### 权限节点

- `freeminecraftmodels.*` — 通配（op）
- `freeminecraftmodels.menu` / `.shop` — 默认全员
- `freeminecraftmodels.admin` — `/fmm admin`、`/fmm giveitem`
- `freeminecraftmodels.disguise.self` / `.others` — 伪装自己/他人
- `freeminecraftmodels.deleteall` — `/fmm deleteall`

### 典型工作流

**玩家换装/装饰**：`/fmm` 打开合成菜单 → 选模型合成 → 手持物品右键放置成家具；`/fmm shop` 用南瓜币买装饰品；`/fmm disguise pumpkin` 变装，`/fmm undisguise` 还原。

**管理员导入内容**：把模型包 zip（不解压）丢进 plugins/FreeMinecraftModels/imports/ → `/fmm reload` 生效 → `/fmm admin` 浏览确认。

### 版本信息

本地实测 2.14.2（2026-11，支持 26.3）/ 来源 https://modrinth.com/plugin/free-minecraft-models

---

## ExcellentEnchants（自定义附魔）

### 玩家命令

80+ 原版风格自定义附魔，完全融入附魔台/村民/战利品箱/钓鱼/铁砧/砂轮等原版机制。Java 玩家用物理铁砧即可；基岩玩家用自研 `/eshop` 商店 GUI 附魔。

| 途径 | 说明 |
|---|---|
| 附魔台 / 铁砧 / 砂轮 | 像原版一样获取与合并自定义附魔书 |
| 村民交易 / 钓鱼 / 战利品箱 | 获取自定义附魔书 |
| `/eshop` | 基岩玩家专用附魔商店：主手拿装备 → 点菜单选附魔 → 扣经验等级施加 |

### 管理员命令

命令别名 `/excellentenchants` 或 `/eenchants`（依赖前置 NightCore）。

| 命令 | 语法 / 参数 | 说明 |
|---|---|---|
| 重载 | `/eenchants reload` | 重载插件与附魔配置 |
| 附魔书 | `/eenchants book <附魔> [等级] [玩家] [-charged]` | `/eenchants book veinminer 3` |
| 随机书 | `/eenchants randombook [玩家] [-custom] [-charged]` | `-custom` 只出非原版附魔 |
| 直接附魔 | `/eenchants enchant <附魔> <等级> [玩家] [槽位]` | 附魔手上物品 |
| 移除附魔 | `/eenchants disenchant <附魔> [玩家] [槽位]` | 移除物品上的附魔 |
| 充能燃料 | `/eenchants givefuel <附魔> [数量] [玩家]` | 发青金石类充能燃料 |
| 附魔列表 | `/eenchants list [player]` | 打开新附魔 GUI |

**充能机制**：附魔带充能值，每次触发消耗充能；拿燃料物品（默认青金石）在铁砧上充电；命令生成默认未充能，加 -charged 生成满充能。

### 权限节点

- `excellentenchants.*` — 全部功能
- `excellentenchants.command.book` / `.randombook` / `.enchant` / `.disenchant` / `.givefuel` / `.list` / `.reload` — 各命令
- `excellentenchants.enchant.decapitator.bypass` — 持有者被「斩首」击杀时不掉头

### 版本信息

最新版本：ExcellentEnchants 5.4.3（2026-06-02，需 NightCore 前置，支持至 26.1.x）/ 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/excellentenchants.80-vanilla-like-enchantments.61693/

---

## CustomDeathMessages（自定义死亡信息）

### 玩家命令

自定义死亡整活播报，玩家无需权限即可正常看到死亡消息。

| 命令 | 权限 | 说明 |
|---|---|---|
| `/cdm` | `cdm.use`（默认全员） | 主菜单/命令概览 |
| `/cdm help [命令]` | `cdm.use` | 多页帮助 |
| `/cdm test [killer] [source]` | `cdm.test` | 预览死亡消息 |

> 别名：/customdeathmessages、/customdeathmessage、/deathmessage、/deathmessages。

### 管理员命令

| 命令 | 权限 | 说明 |
|---|---|---|
| `/cdm reload` | `cdm.reload` | 原子重载配置（出错自动回退旧配置） |
| `/cdm config validate` | `cdm.reload` | 校验配置文件是否有错 |
| `/cdm editor` | `cdm.editor` | 游戏内可视化配置编辑器 |
| `/cdm broadcast message [模式] [文本]` | `cdm.broadcast` | 手动广播消息 |

### 权限节点

- `cdm.use` — 所有玩家（基础命令）
- `cdm.reload` / `cdm.test` / `cdm.editor` / `cdm.broadcast` — OP
- `cdm.bypass` — 持有者**不触发**自定义消息（适合 staff 隐身测试）

> 注意：OP 因通配 `*` 会自动获得 `cdm.bypass`，导致 OP 死亡时走原版消息；测试请用非 OP 账号。

### 版本信息

本地部署 1.7.0-modern（支持 1.18–26.3）/ 来源 https://mcreference.com/plugins/customdeathmessages

---

## SkinsRestorer（皮肤管理）

### 玩家命令

自由更换皮肤，Java/基岩玩家均可用，离线服也能显示皮肤。

| 命令 | 说明 |
|---|---|
| `/skin set <皮肤名>` | 使用正版玩家名对应的皮肤 |
| `/skin url <URL>` | 通过 PNG 图片链接设置皮肤（走 MineSkin 转换） |
| `/skin clear` | 清除自定义皮肤，恢复默认/账号皮肤 |
| `/skin update` | 更新当前皮肤（重新从源拉取） |
| `/skin random` | 随机套用服务器推荐皮肤库中的一款 |
| `/skin undo` | 撤销上一次换肤 |
| `/skin favourite add <名称>` | 收藏皮肤 |
| `/skins` | 打开皮肤 GUI 浏览器，点点换肤 |
| `/skull <玩家>` | 获取带指定玩家皮肤的头颅 |

> 默认换肤冷却 30 秒（skinChangeCooldown）。基岩版玩家经 Floodgate 进服，换肤数据与其他玩家同构。

### 管理员命令

| 命令 | 说明 |
|---|---|
| `/sr reload` | 重载配置 |
| `/sr props <玩家>` | 查看玩家皮肤属性 |
| `/sr applyskin <目标> <皮肤>` | 强制为某玩家设置皮肤 |

### 权限节点

SkinsRestorer 默认通过 `forceDefaultPermissions: true` 给全体玩家开放换肤基础权限，无需手动授权；管理员维护命令一般由 OP 使用。

### 典型工作流

**玩家换皮肤**：`/skins` 打开 GUI 挑一款推荐皮肤一键应用；或 `/skin set Steve`；想用自制图就 `/skin url <图片链接>`；不满意 `/skin undo` 撤销，`/skin clear` 还原默认。

### 版本信息

最新版本：SkinsRestorer 15.12.6（2026-09-21，支持至 26.3，Java 17+）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/skinsrestorer
