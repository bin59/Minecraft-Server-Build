# Advanced Portals（跨服传送门）

> **本文职责**：只讲 Advanced Portals 这个插件本身——版本、命令、标签、配置文件、排错、升级。
> 游戏内三种跨服入口的整体方案与菜单 / NPC 配置见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md)；
> 端口与网络基准见 [多服网络与端口架构.md](./多服网络与端口架构.md) §4.1。

> 适用范围：多服副本 `leaf-26.3-多服`（Velocity + survival / minigames / skyblock）。
> 本服玩法：在主城放门，玩家走进选区即跨服；基岩端同样生效。

## 0. 版本与官方文档

| 项 | 值 |
| --- | --- |
| 实装版本 | `advanced-portals-2.8.0-spigot.jar`，**2.8.0**（官方最新，Modrinth 标注支持 MC 26.3） |
| 安装位置 | 仅多服 `leaf-26.3-多服/plugins/`；主服未装 |
| 官方文档 | <https://advancedportals.sekwah.com/docs/commands> · <https://advancedportals.sekwah.com/docs/portal-tags> · <https://advancedportals.sekwah.com/docs/config> |
| 项目 / 下载 | <https://github.com/sekwah41/Advanced-Portals> · <https://modrinth.com/plugin/advanced-portals/versions> |
| jar 校验 | 3,221,235 B / md5 `0ed644299c5fb1c1f4a644859fbf62cc` / sha1 `fee9a8926aba6068797fdf81549ddfe9e524da7a` |
| 兼容性 | `plugin.yml` 为 `version: 2.8.0`、`api-version: 1.13`（最低 API 声明，不限制 26.3）；class major 61 = Java 17，Java 25 可加载 |
| 数据来源 | 本地 jar 反编译 + 官方站 / Modrinth 实测 |

## 1. 本服采用方案（只保留这一条）

| 项           | 取值                                                                                                                           |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| 跨服标签     | `bungee:<子服名>`，服务端向 `BungeeCord` 通道发 `Connect` 消息，Velocity 兼容通道接管换服                                      |
| 安装范围     | 仅 survival 枢纽服 `plugins/`；目的地服无需装。做「子服→主服」反向门时，minigames / skyblock 各装一份并建 `bungee:survival` 门 |
| 触发方式     | 移动事件判定玩家进入选区内的触发方块区域（`PlayerMoveEvent` 路径），不是原版下界门事件                                         |
| 推荐触发方块 | `WATER`（关门内放水，双端表现一致）；`NETHER_PORTAL` 亦可，但基岩端渲染/物理有差异                                             |
| 落点         | 目标子服保存的玩家位置 / 出生点（EssentialsX `/setspawn`）；Velocity 的 `try` 只决定首次进服落到哪个子服                       |
| 未采用       | `proxy:` 标签（需代理端装同源 jar 且 `config.yaml` 开 `enableProxySupport: true`）、`desti:` 单服内目的地                      |
| 并存入口     | QuickMenu `worldmenu`、FancyNpcs 三个 NPC（点按钮/点 NPC 跨服），见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) |

**关键区分**：`bungee:` 不发 `/server` 命令，因此**不需要** `velocity.toml` 的 `[commands]` 段、代理端 LuckPerms、`velocity.command.server` 授权——那「权限三件套」只约束点按钮 / 点 NPC 的入口。
三条入口的完整链路对比与权限说明见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) §1.1 / §3，本文不重复。

## 2. Velocity 侧前置（已核对 `velocity.toml`）

| 配置                              | 当前值                                | 作用                                           |
| --------------------------------- | ------------------------------------- | ---------------------------------------------- |
| `bungee-plugin-message-channel`   | `true`                                | **`bungee:` 标签必需**；关掉则走进门无任何反应 |
| `[servers]`                       | `survival` / `minigames` / `skyblock` | `bungee:` 后面的名字必须与这里逐字一致         |
| `[commands]` + LuckPerms-Velocity | 已配                                  | 只服务 `/server` 方案，与传送门无关，见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) §3 |

## 3. 命令与权限（2.8.0）

`/portal` 实测别名：`/portals`、`/aportals`、`/advancedportals`；`/destination` 别名 `/desti`。权限默认值见 jar 内 `plugin.yml`（管理类默认 op，使用类默认 true）。

> ⚠️ 自研 SpacePortal 1.0.0-r1 曾把 `portal` 注册成自己的别名，与本命令同名、两插件同服会互相抢命令。r2 起 SpacePortal 别名改为 `/csz`、`/spportal`，`/portal` 专归本插件；服务器上若仍是 r1 旧 jar，需替换后完整重启。详见 [SpacePortal 使用说明](../../自研/传送阵-SpacePortal/空间传送阵使用说明.md) §9。

| 命令                                                      | 作用                                 | 权限                              |
| --------------------------------------------------------- | ------------------------------------ | --------------------------------- |
| `/portal selector`（`/portal wand`）                      | 给选区斧，左键 pos1 / 右键 pos2      | `advancedportals.portal.selector` |
| `/portal create name:<名字> [标签…]`                      | 创建门，`name:` 必需                 | `advancedportals.portal.create`   |
| `/portal portalblock` / `endportalblock` / `gatewayblock` | 给可摆放的门方块                     | `advancedportals.portal.create`   |
| `/portal show` / `/portal list`                           | 粒子显示选区 / 列出门                | `.portal.show` / `.portal.list`   |
| `/portal info <名字>` / `/portal remove [名字]`           | 查详情 / 删除（留空删当前选中的门）  | `.portal.info` / `.portal.remove` |
| `/portal reload` / `/portal version`                      | 重载数据 / 查版本                    | `.portal.reload` / 无             |
| `/portal import` / `langupdate` / `disablebeacon <名字>`  | 旧格式导入 / 补语言键 / 关末地门光柱 | 对应 `.portal.*`                  |
| `/desti create` `remove` `list` `show` `tp <名字>`        | 单服内目的地的增删改查传送           | `advancedportals.desti.*`         |

> 官方 Commands 页明确：**先创建门，再摆放门方块**，否则原版物理更新可能把下界传送门方块打碎。

## 4. 建门流程（survival 主城，管理员执行）

```text
/portal selector                          # 拿选区斧；左键一角、右键对角，两点须同一世界
/portal create name:to_minigames bungee:minigames triggerblock:WATER
/portal create name:to_skyblock  bungee:skyblock  triggerblock:WATER
/portal portalblock                       # 装饰用；若用 NETHER_PORTAL 触发，需把它铺满选区
/portal show                              # 绿色粒子核对选区是否包住脚下触发方块
/portal list                              # 列出已建门
```

- 反向门：minigames / skyblock 各建一个 `bungee:survival` 的门，玩家即可走回主城。
- 删除：`/portal remove to_minigames`；改标签/改名一律删掉重建，插件没有原地改名命令。
- 选区必须包住玩家**脚下**那一格触发方块，只框住门框不会触发。

### 常用标签

| 标签                                                  | 作用                                                                                          |
| ----------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `name:<名字>`                                         | 必需，全服唯一                                                                                |
| `bungee:<子服名>`                                     | 本服跨服方案，名字取 `[servers]`                                                              |
| `triggerblock:<材质>`                                 | 触发方块，可逗号分隔；默认 `NETHER_PORTAL`，常用 `WATER` / `END_PORTAL` / `END_GATEWAY`       |
| `desti:` / `destination:`                             | 传送到本服内 `/desti create` 定义的落点                                                       |
| `message:"…"` / `permission:<节点>` / `cooldown:<秒>` | 传送提示 / 使用权限 / 每人冷却（跨登录保留）                                                  |
| `command:"…"`                                         | 触发时执行命令，支持 `@player`；前缀 `!` op、`^` 全权限、`#` 控制台、`%` 代理（需代理端插件） |
| `conditions:<比较式>`                                 | 需 PlaceholderAPI，如 `conditions:%player_food_level%>=5`                                     |
| `portalEvent:true`                                    | 改用原版 portal 事件触发（基岩端不建议）                                                      |

## 5. 配置与数据文件（首次启用后生成 `plugins/AdvancedPortals/`）

2.x 重编码版是**一文件一对象**，旧的单文件只作迁移来源：

| 路径                                              | 内容                                                      |
| ------------------------------------------------- | --------------------------------------------------------- |
| `config.yaml`                                     | 主配置（**不是** `config.yml`）                           |
| `portals/<名字>.yaml`                             | 每个门一个文件                                            |
| `desti/<名字>.yaml`                               | 每个目的地一个文件                                        |
| `lang/*.lang`                                     | 语言文件，jar 内置 en_GB / de_DE / fr_FR / hu_HU / pt-br  |
| `portals.yml` / `destinations.yml` / `config.yml` | 旧格式文件，`/portal import` 读取迁移；原始数据不会被删除 |

`config.yaml` 关键默认值（jar 内 `Config` 构造函数实测，键名以首次生成文件为准）：

| 键                                             | 默认值              | 说明                                     |
| ---------------------------------------------- | ------------------- | ---------------------------------------- |
| `useOnlySpecialAxe` / `selectorMaterial`       | `true` / `IRON_AXE` | 只有插件发的斧子能选区，避免生存铁斧误触 |
| `defaultTriggerBlock`                          | `NETHER_PORTAL`     | 建门时不写 `triggerblock:` 的兜底值      |
| `stopWaterFlow`                                | `true`              | 防止门内水流出来，`WATER` 触发方案依赖它 |
| `portalProtection` / `portalProtectionRadius`  | `true` / `5`        | 门周围区域保护                           |
| `joinCooldown`                                 | `5` 秒              | 进服冷却，防止刚上线被门反复弹           |
| `enableProxySupport`                           | `false`             | `proxy:` 标签总开关，本服方案不用        |
| `commandPortals.enabled` 等                    | 各等级开关          | 控制 `command:` 标签能否提权执行         |
| `warpMessageOnActionBar` / `warpMessageInChat` | `true` / `false`    | 传送提示位置                             |

改完执行 `/portal reload`；语言文件与部分配置项按官方说明需完整重启才生效。

## 6. 基岩端与排错

基岩玩家经 Geyser-Velocity 进后端后同样是 Bukkit `Player`，移动判定在服务端，走进门即触发，玩家无需装任何客户端内容。触发方块建议用 `WATER`，避免基岩客户端对下界/末地传送门方块渲染与物理事件差异；不要用 `portalEvent:true`。

| 现象                    | 原因                                                                         | 处理                                                            |
| ----------------------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------- |
| 走进门毫无反应          | `bungee-plugin-message-channel` 未开；或 `bungee:` 名字与 `[servers]` 不一致 | 核对 `velocity.toml`，用 `/server <子服>` 先验证子服名可用      |
| 走进门毫无反应          | 选区没包住脚下触发方块，或方块材质与 `triggerblock:` 不符                    | `/portal show` 看粒子，重新选区；`WATER` 方案要真的在选区里放水 |
| 提示无权限              | 玩家缺 `advancedportals.portal.use`，或门上配了 `permission:`                | 该权限默认 `true`，检查 LuckPerms 是否显式设为 false            |
| 门方块自己碎掉          | 先放方块后建门                                                               | 按官方顺序先 `/portal create`，再 `/portal portalblock` 摆放    |
| 基岩看不到门 / 卡在门里 | 传送门类方块在 Geyser 侧表现不同                                             | 改用 `WATER` 或空气区域触发，方块只做装饰                       |
| 换服后落点不对          | 目标服 spawn 未设置                                                          | 各子服 `/setspawn`，并核对 Velocity `try`                       |
| `proxy:` 标签报错       | 默认 `enableProxySupport: false`，代理端也没装插件                           | 本服不用该标签，改回 `bungee:`                                  |

> **别把两套权限混用**：传送门不生效时去查 `[commands]` / `velocity.command.server` 是走错方向，那是菜单 / NPC 方案才需要的。两种入口的排查分界见 [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) §1.1。

## 7. 与自研插件的分工

| 需求                   | 用哪个                                                                     |
| ---------------------- | -------------------------------------------------------------------------- |
| 走进门跨子服           | Advanced Portals `bungee:`（本分册）                                       |
| 点菜单 / 点 NPC 跨子服 | [游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md)（QuickMenu `worldmenu`、FancyNpcs） |
| 单服内坐标传送、传送阵 | 自研 SpacePortal，**不能跨服**，命令 `/csz` / `/spportal` / `/spaceportal` |

命令冲突与**区域隔离**（门选区不要落在阵法激活半径内，否则两个插件的移动事件会同时触发）的完整说明见
[游戏内传送、大厅、菜单等配置.md](./游戏内传送、大厅、菜单等配置.md) §3 与
[SpacePortal 使用说明](../../自研/传送阵-SpacePortal/空间传送阵使用说明.md) §9。

## 8. 升级与回滚

- 官方最新 2.8.0 即本地实装版本，当前无升级动作。
- 升级流程：停服 → 旧 jar 改名 `.bak-YYYYMMDD`（不删除）→ 换新 jar → **完整重启**（不用 `/reload`）→ 读新 jar 的 `plugin.yml` 更新 §0 → 点验每个门。
- 从 1.x 升到 2.x：先备份 `plugins/AdvancedPortals/`，用 `/portal import` 迁移旧 `portals.yml` / `destinations.yml` / `config.yml`；配置文件需按新格式重做。
- 回滚：换回旧 jar，并恢复备份的 `plugins/AdvancedPortals/` 目录。

---

_本分册为仓库文档；命令、标签与默认值由本地 2.8.0 jar 反编译与官方站核实，实机行为以服主启用后点验为准。
本目录文档导航见 [README.md](./README.md)。_
