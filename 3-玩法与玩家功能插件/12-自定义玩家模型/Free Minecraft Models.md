# Free Minecraft Models（FMM）—— 自定义模型加载插件

> 作者 MagmaGuy（EliteMobs / BetterStructures / ResourcePackManager 同作者）。把 **Blockbench 建模**的家具、物品、交互道具直接放进 Minecraft 世界，Java 与**基岩玩家都能看到**；也支持玩家伪装成模型。免费开源。

## 下载与兼容

| 项 | 值 |
|---|---|
| 最新版 | **2.12.2**（2026-09-29 发布） |
| 最低服务端 | **1.21.4**（`api-version: 1.21.4`）；实测 1.21 / 26.1 / 26.2，**1.21.11 可用** |
| 依赖 | **无**（MagmaCore 已内置）；软依赖：WorldGuard / WorldEdit / GriefPrevention / Vault / Floodgate / Geyser-Spigot |
| 下载 | [SpigotMC](https://www.spigotmc.org/resources/free-minecraft-models.111660/) |
| 源码 | [GitHub](https://github.com/MagmaGuy/FreeMinecraftModels) |
| Wiki | [wiki.nightbreak.io/FreeMinecraftModels](https://wiki.nightbreak.io/FreeMinecraftModels/) |

## 安装步骤

1. `FreeMinecraftModels.jar` 放进 `plugins/`，**重启服务器**
2. 下载官方内容包（**不要解压**）放进 `plugins/EternalTD/imports/`
3. `/fmm reload`（或重启，推荐重启）
4. 完成

**资源包生成**：插件自动在配置文件的 `outputs` 文件夹生成资源包（含 `models/` 全部模型）；基岩端必须用 **ResourcePackManager** 强制下发资源包才能看到模型。

## 核心功能

- **自定义家具 / 物品 / 交互道具**：可坐、可睡、可储物，配 **Lua 脚本**自由扩展
- **Blockbench 模型自动转资源包**，绕过原版模型大小 / 旋转限制
- **玩家伪装**：`/fmm disguise` 把玩家外观换成任意已加载模型，同步移动 / 转头 / 挥臂动画
- **免依赖 API**：第三方插件可通过 `DisguiseAPI`、`ModeledEntity` 驱动（伪装、视图距离、交互）
- **内容包体系**：官方免费包（BASIC FURNITURE PACK、BETTERSTRUCTURES PROP PACK、ELITEMOBS PROP PACK、Craftenmine's Weapons/Tools Item Pack），或自建模型分发

## 命令速查

### 玩家可用

| 命令 | 用途 |
|---|---|
| `/fmm` | 打开可合成物品菜单（显示有配方的模型，`freeminecraftmodels.menu` 默认全玩家） |
| `/fmm shop` | 打开家具商店（Vault 经济，需在 shop_config.yml 开启） |
| `/fmm disguise <模型ID>` | 伪装成模型（自己） |
| `/fmm undisguise` | 解除伪装 |

### 管理（需 `freeminecraftmodels.*` 或对应节点）

| 命令 | 用途 |
|---|---|
| `/fmm admin` | 管理内容浏览器：浏览已装包 / 文件夹 / 模型 / 自定义物品（`.admin`） |
| `/fmm setup` | 内容管理 / 首次配置菜单 |
| `/fmm reload` | 重载并验证全部配置 / 模型 / 配方 / 资源包输出 |
| `/fmm stats` | 已加载模型统计 |
| `/fmm spawn static\|dynamic\|prop <id>` | 放置模型（静态 / 动态 / 持久道具，300 格射线） |
| `/fmm itemify <id> <材质>` | 生成放置用物品（如 `PAPER`） |
| `/fmm mount <id>` | 生成可骑乘模型（实验性，伪装马实现） |
| `/fmm giveitem <物品>` | 发放已注册 FMM 物品（`.admin`） |
| `/fmm craftify <id>` | 配方构建器：3×3 摆原料 → 点输出保存原版合成配方 |
| `/fmm disguise <模型ID> <玩家>` | 伪装他人（需 `.disguise.self` + `.disguise.others`，控制台可用） |
| `/fmm disguiselist` | 列出当前所有伪装玩家（`.disguise.others`） |
| `/fmm deleteall [半径]` | 移除全部已加载模型实体（危险，谨慎） |
| `/fmm packetdebug <tick数>` | 采样模型数据包流量（性能排查，1–200） |
| `/fmm debug bedrock\|props on\|off` | 基岩调试日志 / 道具堆叠扫描 |
| `/fmm downloadall` | 下载插件更新 + 全部官方内容包 |
| `/fmm version` | 版本号（无权限限制） |

### Nightbreak 共享（MagmaCore 注册，顶层命令）

| 命令 | 用途 |
|---|---|
| `/nightbreaklogin <token>` | 登录 Nightbreak 账号，解锁官方内容下载 / 插件更新（`nightbreak.login`，默认 op） |
| `/nightbreaklogout` | 清除登录 token |
| `/freeminecraftmodelschangelog [dismiss\|disable]` | 查看 / 忽略更新日志 |

## 权限

| 节点 | 默认 | 说明 |
|---|---|---|
| `freeminecraftmodels.*` | op | 通配（admin / deleteall / disguise.self / disguise.others / bypassregionprotection）及大部分管理命令要求 |
| `freeminecraftmodels.admin` | op | `/fmm admin`、`/fmm giveitem` |
| `freeminecraftmodels.deleteall` | op | `/fmm deleteall` |
| `freeminecraftmodels.disguise.self` | op | 自己伪装 / 解除 |
| `freeminecraftmodels.disguise.others` | op | 伪装他人 + `/fmm disguiselist` |
| `freeminecraftmodels.bypassregionprotection` | op | 绕过 FMM 放置 / 交互保护检查 |
| `freeminecraftmodels.menu` | **true（全体）** | `/fmm` 合成菜单 |
| `freeminecraftmodels.shop` | **true（全体）** | `/fmm shop` |
| `nightbreak.login` | op | Nightbreak 登录命令 |

## 配置（`plugins/FreeMinecraftModels/config.yml`）

| 键 | 默认 | 说明 |
|---|---|---|
| `setupDone` | false | 首次引导标记，`/fmm setup` 后置 true |
| `nightbreak.autoDownloadPluginUpdates` | false | 自动下载插件 / 内容更新（需 Nightbreak token + Patreon） |
| `useDisplayEntitiesWhenPossible` | true | 优先 Display 实体渲染（性能好）；false 退回盔甲架 |
| `maxModelViewDistance` | 60 | 模型可视距离，密集区调小降负载 |
| `maxInteractionAndAttackDistance` | 3 | 静态 / 动态模型交互距离 |
| `maxInteractionAndAttackDistanceForProps` | 6 | 道具（prop）交互距离 |
| `sendCustomModelsToBedrockClientsV2` | **true** | **基岩端发送自定义模型**（旧键已废弃；需配合 Geyser/Floodgate + 资源包） |
| `skipUnchangedBoneUpdates` | true | 未变化骨骼跳过更新（省带宽） |
| `useDeltaMetadataPackets` | true | 逐 tick 只发变更骨骼元数据（省带宽） |
| `maxModelsForProximityOverride` | 25 | 每世界模型 ≥ 25 个时关闭近距无视遮挡优化（防密集区爆包） |
| `preventPropPlacementInProtectedRegions` | true | 受保护区域禁止放置道具（内置提供方：**WorldGuard / GriefPrevention**） |
| `registerCraftingRecipes` | true | 注册原版合成配方；false 则纯商店购买经济 |

### 商店（`shop_config.yml`，Vault 经济，默认关闭）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | false | 主开关；开启需 Vault + 经济提供方（本服南瓜币） |
| `defaultPrice` | 100.0 | 未单独定价配方的默认价 |
| `menuTitle` | `&8FMM - Furniture Shop` | 商店标题（支持 `&` 颜色） |
| `messages.*` | — | 购买成功 / 余额不足等提示文案（`{item}` `{price}` `{balance}` 占位符） |

配方文件 `recipes/<model_id>.yml` 可加 `shopEnabled` / `shopPrice` 单独调价。

## 模型与内容

- 建模用 **Blockbench**（.bbmodel），模型文件与同名 `.yml` 放 `plugins/FreeMinecraftModels/models/`（YAML 文件名全局大小写唯一）
- 道具 Lua 脚本：`plugins/FreeMinecraftModels/scripts/`
- 合成配方：`plugins/FreeMinecraftModels/recipes/<model_id>.yml`
- 官方内容包：通过 `/fmm setup` / `/fmm downloadall` / Nightbreak 获取，导入包放 `plugins/EternalTD/imports/`（不解压）

## 基岩版支持（关键）

- `sendCustomModelsToBedrockClientsV2` **默认 true**，基岩玩家默认可见模型；**必须**配好 Geyser/Floodgate 的资源包下发路径（ResourcePackManager 强制资源包）
- 调试：`/fmm debug bedrock on`，查完记得 `off`
- 基岩端看不到伪装时，先确认资源包路径就绪

## 与南瓜服集成建议

- **定位**：与 CPM 互补——CPM 是玩家自己捏模型（需客户端 mod）；FMM 是服主提供模型库（家具 / 装饰 / 伪装 / 活动道具），Java 基岩通吃
- **领地保护**：FMM 内置保护提供方只覆盖 WorldGuard / GriefPrevention；本服用 **Residence**，需实测领地内是否可放置 FMM 道具，如可放则用 Residence 对应 flag 或事件拦截（放置物是实体而非方块，别只查方块 flag）
- **经济出口**：`/fmm shop` 可接 Vault 南瓜币，家具 / 装扮做成商店商品，形成消费出口
- **性能**：模型密集区调低 `maxModelViewDistance`，用 `/fmm packetdebug` 实测
- **活动**：伪装系统可做万圣节变装、活动 NPC、变身玩法
