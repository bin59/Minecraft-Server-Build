# Free Minecraft Models（FMM）—— 自定义模型加载插件

## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | `plugins/FreeMinecraftModels.jar`，`version: 2.15.0`；`plugins/ResourcePackManager.jar`，`version: 2.4.12` |
| 官方最新 | 官网持续更新，当前按官方文档口径（官方 GitHub README 与 wiki 持续更新；本服 2.15.0） |
| 官方文档站 | <https://wiki.nightbreak.io/FreeMinecraftModels/>；源码 <https://github.com/MagmaGuy/FreeMinecraftModels>；官网 magmaguy.com |
| 官方下载 | <https://modrinth.com/plugin/free-minecraft-models>；<https://www.spigotmc.org/resources/free-minecraft-models.111660/>；ResourcePackManager <https://nightbreak.io/plugin/resourcepackmanager/> |
| 主要页面 | `wiki.nightbreak.io/FreeMinecraftModels/faq`、`/pets`、`/player_disguises`、`/magic_weapons`、`/api_and_developer_guide`、`nightbreak.io/plugin/resourcepackmanager/` |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方文档复核。

> **状态**：✅ 已安装启用 | **版本**：**2.15.0**（2026-11-14 jar 实测，已支持 26.3）| **基岩链路**：FMM + **ResourcePackManager 2.4.12** + Geyser/Floodgate 链路已通（Java 端实测显示 ✅）；基岩端 2026-11-10 排查结论：**缺一次重启激活 Geyser 桥**，见 §4.4
> **作者**：MagmaGuy（EliteMobs / BetterStructures / ResourcePackManager 同作者）| **协议**：GPL-3.0 | **定位**：服主提供模型库（家具 / 装饰 / 武器 / 宠物 / 伪装 / 活动道具），**Java 与基岩通吃**
> 配套文档：[宠物系统 SimplePets](../宠物系统/宠物系统simplepets.md)（FMM 宠物是第三方模型宠物，与 SimplePets 并存不冲突）、[经济系统](../经济系统/Vault.md)（南瓜币出口）

---

## 1. 这是什么

把 **Blockbench 建模**的家具、物品、交互道具直接放进 Minecraft 世界，**Java 与基岩（经 Geyser）玩家都能看到**，也支持把玩家伪装成模型。免费、开源、纯服务端（玩家**不需要装任何客户端 mod**）。

- 绕过原版模型大小 / 旋转限制，Blockbench 动画（含 MoLang 关键帧）在 Java 与基岩**同步播放**
- 道具 / 物品支持 **Lua 脚本**交互（坐、睡、储物、点击触发、投射物命中）
- 基岩可见性依赖 **ResourcePackManager** 把生成的资源包转换并下发给基岩客户端

## 2. 下载与兼容

| 项 | 值 |
|---|---|
| 最新版（本服） | **2.15.0**（2026-11-14 jar 实测，支持 26.3） |
| 最低服务端 | **1.21.4**（`api-version: 1.21.4`）；实测 1.21.11 / 26.1 / 26.2 / **26.3** 均可 |
| 依赖 | **无**（MagmaCore 已内置）；软依赖：WorldGuard / GriefPrevention / Vault / Floodgate / Geyser-Spigot |
| **基岩硬性前置** | **ResourcePackManager（必装）** —— 没有它基岩玩家看不到任何自定义模型 |
| 下载 | [SpigotMC](https://www.spigotmc.org/resources/free-minecraft-models.111660/) / [Modrinth](https://modrinth.com/plugin/free-minecraft-models) |
| 源码 | [GitHub](https://github.com/MagmaGuy/FreeMinecraftModels) |
| Wiki | [wiki.nightbreak.io/FreeMinecraftModels](https://wiki.nightbreak.io/FreeMinecraftModels/) |

## 3. 安装步骤（本服已部署，留档）

1. `FreeMinecraftModels.jar` 与 `ResourcePackManager.jar` 放进 `plugins/`，**重启服务器**
2. （可选）用 `/fmm setup` + `/nightbreaklogin` 装官方内容包；手动导入时只有**带 `pack.meta` 的规范内容包 zip**（不解压）或**单个 `.bbmodel`** 适合放 `plugins/FreeMinecraftModels/imports/`。带配套 `.yml`/`.json`/纹理的模型要直接放 `models/<包名>/`，再 `/fmm reload`。详见 [免费模型包清单](免费模型包清单.md)
3. 首次 `/fmm setup` 走引导（会把 `setupDone` 置 true）
4. 资源包由 FMM 自动生成于 `plugins/FreeMinecraftModels/output/FreeMinecraftModels.zip`，**ResourcePackManager 接管合并 + 基岩转换 + 经 Geyser 下发**

> ⚠️ **路径纠正**：内容包放 `plugins/FreeMinecraftModels/imports/`，**不是** `plugins/EternalTD/imports/`（那是 EliteMobs/EternalTD 的路径，勿混）。

## 4. 资源包与基岩显示链路（重点）

### 4.1 原理

```
FMM 启动时生成资源包
  plugins/FreeMinecraftModels/output/FreeMinecraftModels.zip
        │
        ▼
ResourcePackManager（已装）
  合并本服其他插件的资源包 + 转成基岩格式
        │
        ▼
Geyser / Floodgate 把包发给基岩客户端
        │
        ▼
Java 与基岩玩家都看到模型（含动画 / 粒子）
```

官方 changelog 在 2.6.0 / 2.7.0 / 2.7.1 连续三次强调 *"ResourcePackManager is mandatory to make this work"*。**缺这一环，基岩侧直接断流**——无论模型做得多好。本服 RPM 已装，链路现在完整。

### 4.2 ResourcePackManager

- 放 `plugins/` 即可，**无需额外配置**，启动后自动接管 FMM 资源包
- 与 Geyser 协同：基岩玩家进服时 Geyser 推送合并后的包
- 若用 OPanel / MCSM 托管，资源包路径由 RPM 与 Geyser 协商，**不要在 Geyser 里硬编码另一个冲突的资源包**

### 4.3 基岩调试

- `/fmm debug bedrock on` 开基岩显示诊断日志，`off` 关
- 基岩看不到模型时排查顺序：① RPM 是否装且在跑 → ② `sendCustomModelsToBedrockClientsV2` 是否 true → ③ Geyser 是否把 RPM 的包推出去 → ④ `/fmm reload` 后基岩重进服刷新包

### 4.4 基岩排错实测案例（2026-11-10，Java 显示 / 基岩不显示）

**现象**：Java 端提示下载资源包、下载后模型正常显示；基岩端（Floodgate `.xxx` 玩家）看不到自定义模型。

**诊断结论**（查 `logs/latest.log`）：链路本身没坏，是**缺一次重启 + 玩家进得太早**：

| 证据（日志行） | 含义 |
|---|---|
| `[Geyser-Spigot] 已加载 0 个扩展` | 本次启动时 Geyser 未加载 RPM 扩展 |
| `[ResourcePackManager] Geyser support updated. Restart your server to finish the update.` | RPM 在**运行中**才安装/更新 Geyser 桥（`extensions/ResourcePackManager.jar` + `custom_mappings/rspm_geyser_mappings.json`），**必须重启才生效** |
| `[ResourcePackManager] Bedrock conversion complete: 127 mappings (15 unique models...)` | 基岩转换本身成功（产物：`plugins/ResourcePackManager/output/ResourcePackManager_Bedrock.zip`） |
| `RSPM: 'xxx' joined before the resource pack is ready` | 玩家在包就绪前进服；Java 可中途补发，**基岩走 Geyser 通道，通道未激活就什么都没有** |

**修复步骤**：
1. **重启服务器一次**（RPM 官方指示；Geyser 的 custom_mappings 也只在启动时读取）。
2. 重启后确认日志顺序：Geyser 扩展/桥加载 → `Bedrock conversion complete` 出现**早于**基岩玩家加入。
3. 基岩玩家**退出重进**（包在加入时下发；`force-resource-packs: true` 会强制下载，不会弹拒绝）。
4. 仍不显示再查：基岩客户端旧包缓存（客户端设置里清理）、Geyser `extensions/` 里的 jar 是否有效（RPM 会自修复，勿手动删）。

**注意**：`/fmm reload` 只重新生成 Java 包和转换产物，**不会**让 Geyser 重读 mappings——改完模型想让基岩生效，稳妥做法是 `reload + 重启`。

### 4.5 玩家自行安装（手动分发）

不依赖进服弹窗、由玩家自己装包时，分发的是 **RPM 合并产物**，不是 FMM 原始包：

| 端 | 源文件 | 客户端可否识别 | 分发文件 |
|---|---|---|---|
| Java | `plugins/ResourcePackManager/output/ResourcePackManager_RSP.zip`（合并包，117 项，`pack_format: 97` + `min_format: 69` / `max_format: 999`） | ✅ 含 `pack.mcmeta`，适配 MC 26.3 | `南瓜服模型资源包-Java-20261113.zip` |
| 基岩 | `plugins/ResourcePackManager/output/ResourcePackManager_Bedrock.zip`（含 `manifest.json` + `pack_icon.png`） | ✅ 改后缀 `.mcpack` 可双击导入 | `南瓜服模型资源包-基岩-20261113.mcpack` |
| ~~Java（不推荐）~~ | `plugins/FreeMinecraftModels/output/FreeMinecraftModels.zip`（99 项，仅 FMM 自身） | ✅ | 缺 RPM 合并内容，单独发会少模型 |

- 分发物与可直接转发给玩家的 [`资源包分发-ResourcePackManager/玩家安装说明.md`](资源包分发-ResourcePackManager/玩家安装说明.md) 已备在该目录；两个 zip 均从本地 26.3 主服产物复制，md5 一致（`a2c3fe7a` / `6fa0df71`）。
- **`pack.mcmeta` 源头**：`plugins/ResourcePackManager/blueprint/pack.mcmeta`。原为 `pack_format: 34`（1.21~1.21.1），而 MC 26.3 资源包格式是 `97.1`（客户端 jar 内 `version.json` 的 `pack_version.resource_major/minor`），客户端因此显示红色「不兼容」。2026-11-14 改为 `pack_format: 97` + `supported_formats: 34–99` + `min_format: 69` / `max_format: 999`，并同步写回 mixer 缓存、output 合并包与分发包；1.21.9+ 只认 `min_format`/`max_format`，光改 `pack_format` 无效。RPM 重新合并时读取 blueprint，不会再退回 34。
- 分发渠道任选：QQ 群文件 / 网盘直发；或丢到静态站点目录（本地 `D:\web\nginx-1.28.3\html\mchappyhut`、云端 Caddy 站点目录）取 HTTPS 直链给玩家下载。**不要**改 `server.properties` 的 `resource-pack=`——那是自动下发通道，当前为空。
- **版本必须同步**：新增模型或 `/fmm reload` 后 FMM 与 RPM 会重新生成 zip，玩家要重新下载安装；文件名带日期便于识别旧包。客户端缓存旧包会表现为「看不到新家具」。
- 现状：RPM `autoHost: true` / `bedrockAutoDeployToGeyser: true`、Geyser `force-resource-packs: true`，进服仍会自动推送**同一个 uuid 的包**，手动安装是备用/离线方案，两者不冲突。若要完全交给玩家自己装，需关掉 `bedrockAutoDeployToGeyser` 与 Geyser `force-resource-packs`，但**基岩玩家不装包就看不到模型**，且 Geyser 扩展 / `rspm_geyser_mappings.json` 链路必须保持正常（装包只解决客户端资源，模型实体仍由服务端下发）。
- 基岩 FMM 最新包与 RPM mixer 缓存 md5 一致（`8c2f4861`），说明 2026-11-12 那次 `/fmm reload` 内容未变，2026-11-10 的 RPM 输出仍是当前有效版本。

## 5. 核心功能

- **自定义家具 / 物品 / 交互道具（props）**：可坐、可睡、可储物，配 Lua 脚本自由扩展
- **Blockbench 模型自动转资源包**，绕过原版模型大小 / 旋转限制
- **宠物系统（2.13+）**：`.pet.yml` 把模型变成可驯服 / 繁殖的宠物物种，有行为（聚营火、夜间发光、迁徙），详见 §12
- **魔法武器 / 自定义物品**：手持模型 + 自定义附魔 + Lua 技能（法杖施法），见 `enchantments/` 与 `models/fmm_default_magic/`
- **玩家伪装（disguise）**：`/fmm disguise` 把玩家外观换成任意已加载模型，同步移动 / 转头 / 挥臂
- **骑乘模型（rideable）**：带 `m_` 座位骨骼的模型可右键骑乘、可操控
- **玩家伪装 API / ModeledEntity API**：第三方插件可驱动（伪装、视图距离、交互）

## 6. 模型类型目录（跨平台能力）

下表所有类型 **Java + 基岩均可见**（基岩含动画 / 粒子）：

| 类型 | 基岩可见 | 玩法 | 南瓜服可用场景 |
|---|---|---|---|
| **宠物 / 生物（pets）** | ✅ 含动画 | 可驯服 / 繁殖 / 野生，有行为（聚营火、夜间发光、迁徙） | 服务器吉祥物、南瓜精随行宠物、主题坐骑 |
| **武器 / 魔法（custom items / wands / staves）** | ✅ 含附魔 + 施法 | 手持模型 + Lua 技能 | 南瓜法杖（替代默认奥术魔杖）、RPG 武器 |
| **家具 / 装饰（props / furniture）** | ✅ 含交互 | 可坐 / 睡 / 储物，Lua 脚本 | 南瓜灯家具、主题街区装饰 |
| **场景道具 / 装饰** | ✅ | 静态摆放 | 灯笼、招牌、蔬菜主题摆件 |
| **玩家伪装（disguise）** | ✅ | 把自己变模型 | 活动 NPC、整蛊、万圣变装 |
| **骑乘模型（rideable）** | ✅ | 右键骑乘、可操控 | 主题坐骑 |

## 7. 目录结构与文件说明（基于本服实际安装）

```
plugins/FreeMinecraftModels/
├─ config.yml              # 主配置（含基岩开关、性能调优）
├─ pets.yml                # 宠物模式（COSMETIC / CREATURES）
├─ shop_config.yml         # /fmm shop 商店（Vault 南瓜币，默认关）
├─ nightbreak-changelogs.yml
├─ models/                 # 模型文件（.bbmodel 同名 .yml 配对）
│   └─ fmm_default_magic/  #   默认：奥术魔杖、奇迹奥术法杖
├─ enchantments/           # 16 个自定义附魔（爆破半径、点火、 multicast 等）
├─ content_packages/       # 内容包清单/元信息（仅 .yml，非模型本体）
├─ pet_behaviors/          # 宠物行为 Lua 模块（跟随、聚营火、夜间发光等 15 个）
├─ scripts/                # 道具 Lua 脚本
├─ recipes/                # 合成配方 <model_id>.yml（可单独 shopEnabled / shopPrice）
├─ imports/                # 单个 .bbmodel，或带 pack.meta 的内容包 zip（不解压）
├─ output/                 # ⭐ FMM 生成的资源包（FreeMinecraftModels.zip）
└─ resource_pack/          # 资源包中间产物
```

> 资源包目录是 **`output/`**（单数），不是 `outputs`。

### 7.1 当前实际内容（2026-11-13 本地 26.3 主服实测）

**服务端只加载了 2 个模型**，就是 FMM 自带的默认示例，日志为证：`Loaded 2 custom item definition(s).`

| 模型 ID | 显示名 | 材质 | 类型 |
|---|---|---|---|
| `fmm_default_arcane_wand` | 奥术魔杖 | `BLAZE_ROD` | WAND（左键自动瞄准魔法飞弹） |
| `fmm_default_arcane_staff` | 奇迹奥术法杖 | `WOODEN_SPEAR` | STAFF（左键近战 / 右键范围火球） |

| 目录 | 实际数量 | 说明 |
|---|---|---|
| `models/` | **2 个模型**（`fmm_default_magic/` 下 8 个文件） | 仅默认魔法示例 |
| `content_packages/` | 6 个 `.yml` 清单 | `version: 0`、`contentFilePrefixes: []`，**只是元信息，模型本体未下载**；对应 `folderName`（Basic Furniture Pack、Craftenmine 武器/工具、Driftjelly 宠物、BetterStructures / EliteMobs 道具）在 `models/` 里都不存在 |
| `imports/` | 0 | 从未手动导入内容包 |
| `recipes/` | 0 | 无配方，`/fmm` 合成菜单为空 |
| `enchantments/` | 16 个（32 文件） | 附魔 Lua 脚本，**不是模型**；要绑定物品才生效 |
| `scripts/` / `pet_behaviors/` | 4 / 17 | 交互与宠物行为脚本，**不是模型**；无任何 `.pet.yml` 物种定义 |

状态：`config.yml` 的 `setupDone: false`、日志 `No account token found. Run /nightbreaklogin...`，说明首次引导没走完、Nightbreak 未登录，所以官方内容包一直没下载；`shop_config.yml` 的 `enabled: false`，`pets.yml` 为 `COSMETIC` 但无物种。Java 合并包里 items 只有 `fmm_default_arcane_wand` / `fmm_default_arcane_staff` 两个 ID；基岩包的 7 个 `.geo.json` 与 30 个 attachables 是**这两个武器的骨骼拆分**，不代表有 30 个模型。

可装的免费/付费包、下载源与导入方式见 [免费模型包清单](免费模型包清单.md)。装更多内容后资源包会重新生成，§4.5 的分发包随之过期、玩家需重新下载；基岩端还要按 §4.4 重启一次。

## 8. 命令速查

### 玩家可用

| 命令 | 用途 |
|---|---|
| `/fmm` | 合成物品菜单（显示有配方的模型，`freeminecraftmodels.menu` 默认全玩家） |
| `/fmm shop` | 家具商店（Vault 经济，需在 shop_config.yml 开启） |
| `/fmm disguise <模型ID>` | 伪装成模型（自己） |
| `/fmm undisguise` | 解除伪装 |
| `/fmm pets` | 宠物收集菜单（2.13+，需宠物模式开启） |

### 管理（需 `freeminecraftmodels.*` 或对应节点）

| 命令 | 用途 |
|---|---|
| `/fmm admin` | 内容浏览器：浏览已装包 / 模型 / 自定义物品（`.admin`） |
| `/fmm setup` | 内容管理 / 首次配置菜单 |
| `/fmm reload` | 重载并验证全部配置 / 模型 / 配方 / 重新生成资源包 |
| `/fmm stats` | 已加载模型统计 |
| `/fmm spawn static\|dynamic\|prop <id>` | 放置模型（静态 / 动态 / 持久道具） |
| `/fmm itemify <id> <材质>` | 生成放置用物品（如 `PAPER`） |
| `/fmm mount <id> [scale]` | 生成可骑乘模型 |
| `/fmm giveitem <物品>` | 发放已注册 FMM 物品（`.admin`） |
| `/fmm craftify <id>` | 配方构建器：3×3 摆原料 → 保存原版合成配方 |
| `/fmm disguise <模型ID> <玩家>` | 伪装他人（需 `.disguise.self` + `.disguise.others`） |
| `/fmm disguiselist` | 列出当前所有伪装玩家 |
| `/fmm petadmin` | 切换宠物模式 / 列出、生成、给予宠物物种（2.13+） |
| `/fmm petmigrate <物种>` | 触发该物种的迁徙群飞（2.13+） |
| `/fmm deleteall [半径]` | 移除全部已加载模型实体（危险，谨慎） |
| `/fmm packetdebug <tick数>` | 采样模型数据包流量（性能排查，1–200） |
| `/fmm debug bedrock\|props on\|off` | 基岩调试日志 / 道具堆叠扫描 |
| `/fmm downloadall` | 下载插件更新 + 全部官方内容包 |
| `/fmm version` | 版本号（无权限限制） |

### Nightbreak 共享（MagmaCore 注册）

| 命令 | 用途 |
|---|---|
| `/nightbreaklogin <token>` | 登录 Nightbreak 账号，解锁官方内容下载 / 插件更新（`nightbreak.login`，默认 op） |
| `/nightbreaklogout` | 清除登录 token |
| `/freeminecraftmodelschangelog [dismiss\|disable]` | 查看 / 忽略更新日志 |

## 9. 权限

| 节点 | 默认 | 说明 |
|---|---|---|
| `freeminecraftmodels.*` | op | 通配（admin / deleteall / disguise.self / disguise.others / bypassregionprotection） |
| `freeminecraftmodels.admin` | op | `/fmm admin`、`/fmm giveitem` |
| `freeminecraftmodels.deleteall` | op | `/fmm deleteall` |
| `freeminecraftmodels.disguise.self` | op | 自己伪装 / 解除 |
| `freeminecraftmodels.disguise.others` | op | 伪装他人 + `/fmm disguiselist` |
| `freeminecraftmodels.bypassregionprotection` | op | 绕过 FMM 放置 / 交互保护检查 |
| `freeminecraftmodels.menu` | **true（全体）** | `/fmm` 合成菜单 |
| `freeminecraftmodels.shop` | **true（全体）** | `/fmm shop` |
| `nightbreak.login` | op | Nightbreak 登录命令 |

## 10. 配置（`config.yml`）

### 基岩相关（关键）

| 键 | 默认 | 说明 |
|---|---|---|
| `sendCustomModelsToBedrockClientsV2` | **true** | **基岩端发送自定义模型**（旧键已废弃；需配合 Geyser/Floodgate + ResourcePackManager）。false 时基岩玩家看不到模型，但动态模型（Boss 等）会退化显示成它所基于的原版生物 |
| `useDisplayEntitiesWhenPossible` | true | 优先 Display 实体渲染（性能好）；**基岩不存在 Display 实体，FMM 自动退回盔甲架显示**，无需手动干预 |
| `modelViewersTrackHiddenBodies` | true | 隐藏实体的观者以隐形裸身追踪，使模型骨骼像坐骑一样随实体平滑移动；**基岩玩家保持原隐藏逻辑** |
| `particleEffectsEnabled` | 默认 true / **本服 false** | Blockbench 粒子是否在模型、道具与**手持物品**上播放；本服 2026-11-14 已关闭，见下方「本服配置变更记录」。基岩随资源包重生同步 |

### 手持粒子（奥术魔杖，本服已关闭）

`fmm_default_arcane_wand` 自带 Blockbench 粒子 `wand_particles`，绑定在 `.bbmodel` 的 `Effects` 关键帧（`bind_to_actor: true`），拿在手上会持续冒。FMM 2.12.6 起随默认魔杖一起加入（"Dali's arcane wand … with sparkles"）。每颗粒子都是服务端发给 Java 玩家的 packet entity，**客户端「视频设置 → 粒子」关不掉**。

#### 本服配置（2026-11-14）

| 项 | 内容 |
|---|---|
| 文件 | `服务器数据备份/leaf-26.3/leaf-26.3/plugins/FreeMinecraftModels/config.yml` |
| 键 | `particleEffectsEnabled`：`true`（默认）→ **`false`** |
| 配置片段 | `particleEffectsEnabled: false`（上一行注释：`Set to false to switch every particle effect off without editing models.`） |
| 改前原件 | `.workbuddy/backup_配置_20261114/FreeMinecraftModels-config.yml.orig`（md5 `db0d9581`） |
| 生效范围 | 关闭**全部** Blockbench 粒子：`models / props / held items`；模型显示、动画、Lua 技能不受影响 |
| 实际影响 | 本服仅加载 2 个默认模型，只有奥术魔杖带 `.particle.json`（`fmm_default_arcane_staff` 没有），关总开关实际只影响魔杖，无误伤 |
| 生效方式 | Java 端 `/fmm reload`；基岩端需资源包重新生成（`/fmm reload` 或重启）后跟随，玩家可能要重进服刷新包 |
| 校验 | `grep -n particleEffectsEnabled <config.yml>` → `false` |
| 回滚 | 改回 `true` 后 `/fmm reload`；或用改前原件覆盖（原件为改前快照，覆盖前先对比插件后来补的新键） |

> 插件重载 / 升级会往 `config.yml` 自动补新配置键（2026-11-14 已补 `longIdleMinSeconds` / `longIdleMaxSeconds`），文件 md5 会变；**是否关闭粒子只看该键的值，不要用 md5 判断**。`/fmm reload` 会重新生成 Java / 基岩资源包，重发分发包前先核对分发物 md5 是否变化。

其它可选做法：

| 想怎么做 | 改法 |
| --- | --- |
| 全服关（本服采用） | `particleEffectsEnabled: false` |
| 只调少、不关 | `maxParticlesPerEmitter`（默认 100，取粒子文件自身 `max_particles: 100` 的较小值）调到 20~30，或 `maxParticlesPerViewer`（默认 400）调小 |
| 只关某一个模型 | 模型 `.yml` 里**没有** per-model 粒子开关；只能删 `.bbmodel` 的 Effects 关键帧或 `.particle.json`，会被 `/fmm reload` / 插件更新覆盖，不推荐 |

### 性能 / 动画调优

| 键 | 默认 | 说明 |
|---|---|---|
| `maxModelViewDistance` | 60 | 模型可视距离，密集区调小降负载 |
| `maxInteractionAndAttackDistance` | 3 | 静态 / 动态模型交互距离 |
| `maxInteractionAndAttackDistanceForProps` | 6 | 道具（prop）交互距离 |
| `skipUnchangedBoneUpdates` | true | 未变化骨骼跳过更新（省带宽） |
| `useDeltaMetadataPackets` | true | 逐 tick 只发变更骨骼元数据（省带宽） |
| `maxModelsForProximityOverride` | 25 | 每世界模型 ≥ 25 个时关闭近距无视遮挡优化（防密集区爆包） |
| `cullOffscreenBones` | true | 屏幕外的骨骼不绘制（省客户端性能） |
| `idleWalkBlendTicks` | 5 | idle↔walk 动画融合 tick（基岩同值） |
| `deathBlendTicks` | 4 | 死亡动画融合 tick（基岩同值） |
| `weightedTurning` | true | 模型随生物转向带惯性（越大转越慢） |
| `filmicMode` | false | ⚠️ **录制用**，常开会大幅增包，平时务必关 |

### 其他

| 键 | 默认 | 说明 |
|---|---|---|
| `setupDone` | false | 首次引导标记，`/fmm setup` 后置 true |
| `preventPropPlacementInProtectedRegions` | true | 受保护区域禁止放置道具（内置提供方：**WorldGuard / GriefPrevention**；**不含 Residence**，见 §15） |
| `registerCraftingRecipes` | true | 注册原版合成配方；false 则纯商店购买经济 |
| `nightbreak.autoDownloadPluginUpdates` | false | 自动下载插件 / 内容更新（需 Nightbreak token + Patreon） |

## 11. 商店（`shop_config.yml`，Vault 经济，默认关闭）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | false | 主开关；开启需 Vault + 经济提供方（**本服南瓜币 / EssentialsX**） |
| `defaultPrice` | 100.0 | 未单独定价配方的默认价 |
| `menuTitle` | `&8FMM - Furniture Shop` | 商店标题（支持 `&` 颜色） |
| `messages.*` | — | 购买成功 / 余额不足等提示（`{item}` `{price}` `{balance}` 占位符） |

配方文件 `recipes/<model_id>.yml` 可加 `shopEnabled` / `shopPrice` 单独调价。开启后家具 / 装扮即成为南瓜币消费出口。

## 12. 宠物系统（2.13+，本服可用）

FMM 把任意模型变成可驯服 / 繁殖的宠物物种：

- **启用方式**：`pets.yml` 设 `petMode`（默认 `COSMETIC` = 仅有主宠物、不可被伤害；`CREATURES` = 新区块刷出野生宠物、喂食驯服，像家畜一样生活）
- **定义物种**：在模型文件旁放同名 `物种名.pet.yml`，声明幼体、变种、行为
- **行为模块**：`pet_behaviors/` 下 Lua 脚本，自带 15 个（跟随、停留、聚营火、白天栖息、夜间发光、蜇刺、浇灌作物等），在 `.pet.yml` 的 `behaviors:` 引用
- **飞行物种**：`migration:` 段落会让它们成群掠过夜空
- **玩家指令**：`/fmm pets` 收集菜单、`/fmm petadmin` 管理、`/fmm petmigrate <物种>` 触发迁徙

> 南瓜服可做「会跟玩家跑的南瓜精」当服务器 mascot，或蔬菜主题宠物（胡萝卜兔、南瓜猫等）。

## 13. 模型与内容来源（去哪搞）

按省事程度排序：

1. **Nightbreak 内置商店（最推荐）**：游戏内 `/fmm setup` + `/nightbreaklogin`，官方维护、全基岩兼容、一键装，不用自己找包。⚠️ **本服尚未走这条路**：`setupDone: false`、无账号 token，故目前只有默认 2 个模型（见 §7.1）；装完用 `/fmm downloadall` 拉取全部官方内容包，`/fmm reload` 生效
2. **免费 `.bbmodel` 源**：Blockbench Workshop（898 个免费）、Spaleforce（1781+）、BuiltByBit 免费套装等。拿到 `.bbmodel` 后，**带配套 `.yml`/`.json`/纹理时直接放 `models/<包名>/`**；`imports/` 现在只可靠处理单个 `.bbmodel` 和带 `pack.meta` 的规范 zip。完整来源、许可证与导入方式见 [免费模型包清单](免费模型包清单.md)
3. **别下 ItemsAdder / Nexo / Oraxen / MythicCrucible 专属包**（MCModels 免费区多数属此类）：FMM 只认 `.bbmodel` / `.fmmodel`，成品资源包 + 配置的格式用不了
4. **自己用 BlockBench 做**：`.bbmodel` 建模 → FMM 免费转资源包，完全开源可控（最贴合南瓜主题）

## 14. 与南瓜服集成建议

- **定位**：与 CPM 互补——CPM 是玩家自己捏模型（需客户端 mod）；FMM 是**服主提供模型库**（家具 / 装饰 / 伪装 / 活动道具），Java 基岩通吃
- **经济出口**：`/fmm shop` 接 Vault 南瓜币，家具 / 装扮做成商店商品，形成消费出口（见 §11）
- **活动玩法**：伪装系统做万圣节变装、活动 NPC、变身玩法；宠物系统做吉祥物
- **主题模型建议**（贴合「狂笑南瓜切蔬菜」）：
  - 南瓜灯家具套装（椅、桌、路灯）
  - 南瓜头宠物 / 坐骑、`物种名.pet.yml` 定义
  - 南瓜法杖（替代默认奥术魔杖，`models/fmm_default_magic/` 下自建模）
  - 蔬菜主题装饰（胡萝卜路灯、南瓜田围栏、辣椒灯笼）
- **性能**：模型密集区调低 `maxModelViewDistance`，用 `/fmm packetdebug` 实测

## 15. 已知限制 / 注意事项

- **基岩缺 Display 实体**：FMM 自动用盔甲架回退渲染，行为一致，但 Display 实体专属优化在基岩侧不生效——属正常，不用手改
- **Residence 保护未覆盖**：FMM 内置保护提供方只覆盖 **WorldGuard / GriefPrevention**，**不含 Residence**。本服用 Residence 领地，需实测领地内是否可放置 FMM 道具；如可放，用 Residence 对应 flag 或事件拦截（放置物是**实体**而非方块，别只查方块 flag）。`preventPropPlacementInProtectedRegions: true` 仅对上述两插件生效
- **filmicMode 务必关**：录制完立刻关回，否则全服增包卡顿
- **RPM 与 Geyser 冲突**：不要在 Geyser 里再硬编码另一个资源包和 RPM 抢下发，否则基岩可能拿到残缺包
- **版本同步**：升级 FMM 时一并确认 ResourcePackManager 兼容版本，避免基岩包转换失败
