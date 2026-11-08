# SpacePortal 变更记录

## 2026-11-08 · 修复同一阵法堆积出多个标记物品（仅 26.3 版）

### 问题与根因

26.3 互通服切到 `ITEM`（悬浮掉落物）模式后，同一个传送阵位置出现了很多个标记物品。

旧实现 `ensure()` 只依赖内存 `Map<阵法名, 实体UUID>` 判重：UUID 对应实体一旦
`Bukkit.getEntity()` 取不到（掉落物被原版物品合并、被清理插件移除、区块卸载、
`Item` 自然消失、玩家挤动），就立即补建一个；而 `removeAll()` 只清内存表里的 UUID
和阵法中心 3×4×3 范围内带标记的实体，**清不掉已经"失联"的旧实体**。
多次 `/csz reload` / 每 5 秒一次补齐后，旧实体越积越多。

### 修改（仅 `自研/升级26.3版本/SpacePortal/`，旧版 api 1.20 未动）

- `display/PortalDisplayManager.java`
  - 幂等策略改为**以 PDC 标记为唯一真相**：每 5 秒扫描全服已加载实体，
    按 `portal_display`（阵法名，小写归一）分组；不属于任何现存阵法的孤儿实体直接删除；
    每个阵法只保留一个有效且模式匹配的实体，其余全部 `remove()`，缺失才补建。
  - 新增 PDC `portal_display_mode`：记录创建时的展示模式，模式切换后旧模式实体
    会被自动清掉重建，避免 `ITEM_DISPLAY` 与 `ITEM` 混用造成"两个物品"。
  - `restart()` 不再无条件 `removeAll()`（避免 reload 闪烁），交给 `ensure()` 去重收敛；
    `stop()` / `removeAll()` 改为全服 PDC 扫描清理，覆盖被推离中心的历史遗留。
  - 新增 `ItemMergeEvent` / `ItemDespawnEvent` / `EntityPickupItemEvent` 三个拦截，
    加上原有 `InventoryPickupItemEvent`，从源头堵住掉落物失效；
    拦截 `ItemMergeEvent` 尤其关键，否则合并会改写数量并让记录的 UUID 失效。
  - 掉落物被挤离中心超过 0.75 格时才 `teleport` 纠正一次，不产生周期发包。
  - 新增 `countMarkers()` 供指令查询实际存在的标记实体数。
- `command/PortalCommand.java`：`/csz item`（无参数）增加
  「标记实体 N 个 / 阵法 M 个」，相等显示正常，不等提示 `/csz item refresh` 去重。

### 验证

- `build_all.py` 单模块编译通过：12 源文件 / 14 class，class major 65，
  `api-version: '26.3'`；jar 内 `effects-enabled: false` / `mode: AUTO` /
  `geyser-fallback: true` 保持不变；
- 字节码确认含 `portal_display_mode` / `ItemMergeEvent` / `ItemDespawnEvent` /
  `EntityPickupItemEvent` / `countMarkers` / `pickKeeper`；
- 回归：4 处 `playSound` 仍全部包在 `if (EffectController.isEnabled())` 内，
  关特效逻辑未被破坏；`_out` 仍是 10 个 jar，旧版 jar 未改动。
- 产物同步 `自研/升级26.3版本/_out/SpacePortal-1.0.0.jar`（56541 B，
  SHA256 `10778b5225345e2d…`）。

### 部署

**停服替换 jar → 完整重启**（标记实体 `setPersistent(false)` 不写存档，重启本身也会
清掉旧实体）；或先 `/csz item refresh` 立即去重。用 `/csz item` 确认
「标记实体数 = 阵法数」。若重启后仍持续增多，是清理插件在删掉落物，
需加排除列表或把 `display-item.mode` 改成 `ARMOR_STAND`。

## 2026-11-08 · 修复基岩玩家看不到传送阵物品标记（仅 26.3 版）

### 问题与根因

26.3 互通服上 Java 端能看到传送阵的特殊物品，基岩端完全看不到。反查本地
`Geyser-Spigot.jar` 实证：Geyser 只有 `DisplayBaseEntity` / `TextDisplayEntity`，
**没有 `ItemDisplayEntity`**，`VanillaEntities` 注册表里也没有 `ITEM_DISPLAY` 映射
（该字符串只出现在 MCProtocolLib 的协议枚举 `EntityType` 中）。因此 Java 端生成的
Item Display 实体不会被转发给基岩客户端 —— 与视距、粒子开关、客户端设置无关，
改 `ARMOR_STAND` 之外的任何刷新操作都无效。

### 修改（仅 `自研/升级26.3版本/SpacePortal/`，旧版 api 1.20 未动）

- `SpacePortalPlugin.java`
  - 新增 `detectGeyser()`：遍历已加载插件名，包含 `geyser` / `floodgate` 即判定互通服；
  - 新增 `geyserPresent` / `geyserFallback` 字段与配置 `display-item.geyser-fallback`（默认 true）；
  - `getDisplayMode()` 运行时降级：`AUTO` → 互通服 `ITEM` / 纯 Java 服 `ITEM_DISPLAY`；
    显式配 `ITEM_DISPLAY` 且检测到 Geyser 时也降级为 `ITEM`；
  - 新增 `getRawDisplayMode()` / `isGeyserPresent()` / `isGeyserFallback()`；
  - 启动日志打印实际模式与降级提示。
  - **关键点**：`saveDefaultConfig()` 不覆盖服务器已有 `config.yml`，只改 jar 内默认配置
    对旧配置无效，所以降级必须放在运行时。
- `display/PortalDisplayManager.java`
  - 新增 `ITEM` 模式：`world.dropItem` 悬浮掉落物（Geyser 有原生 `ItemEntity` 映射，
    Java / 基岩双端可见）；不可拾取、不消失、无重力、无速度、无敌、静音、
    `setPersistent(false)` 不写存档、PDC 标记；
  - 实现 `Listener`，拦截 `InventoryPickupItemEvent`，防止漏斗 / 漏斗矿车吸走标记物品；
  - 原 `ARMOR_STAND` / `ITEM_DISPLAY` 分支保留。
- `command/PortalCommand.java`：`/csz item`（无参数）增加显示「配置模式 → 实际模式」、
  自动降级标记与 Geyser 检测状态。
- `config.yml`：`mode` 默认 `ITEM_DISPLAY` → `AUTO`，新增 `geyser-fallback: true`
  及三种模式的注释说明。

### 验证

- `build_all.py` 单模块编译 `SpacePortal` 通过：12 源文件 / 14 class，class major 65，
  `api-version: '26.3'`，jar 内 `mode: AUTO` 与 `geyser-fallback: true` 已写入；
- 字节码确认含 `dropItem` / `setCanPlayerPickup` / `setUnlimitedLifetime` /
  `InventoryPickupItemEvent` / `detectGeyser` / `geyser` / `floodgate`；
- 产物同步至 `自研/升级26.3版本/_out/SpacePortal-1.0.0.jar`（SHA256 一致，55038 字节），
  `_out` 仍是 10 个 jar；旧版 `自研/传送阵/SpacePortal-1.0.0.jar` 未改动。
- 文档同步：`空间传送阵使用说明.md`（安装 / 参数表 / 6.2 / 第八章 FAQ）。

### 部署

停服替换 26.3 的 jar 后**完整重启**，不要 `/reload`。旧 `config.yml` 无需修改
（运行时自动降级）。启动日志应出现「检测到 Geyser/Floodgate：物品标记自动使用 ITEM
模式」，游戏内 `/csz item` 显示 `ITEM` 且「已检测到 Geyser/Floodgate」即生效。
若清理插件会删掉落物，需把标记实体加入排除列表（插件每 5 秒幂等补齐）。

---

## 2026-11-07 · 关闭全部特效，改用特殊物品标记

### 需求

关闭传送阵的全部粒子 / 音效 / 时光隧道特效；特效关闭后，用可自定义的「特殊物品」作为阵法的可见标记。

### 新增

- **`effects-enabled: false`**（默认关闭）：`applyConfig()` 写入 `EffectController.setEnabled()`；`startEffectTask()` 在关闭时直接返回，不启动任何粒子定时任务。
- **`EffectController` 静态开关**：`drawPortalCircle` / `chargeComplete` / `teleportBurst` / `drawTimeTunnel` / `cancelPuff` / `drawPortalFrame` 全部在入口判断；`inRange` 是范围工具方法，不受影响。
- **`TeleportService` 全音效屏蔽**：充能开始、充能进度、钻石不足（2 处）、充能完成、传送爆发、时光隧道、6 tick 蓄力停顿，全部按 `EffectController.isEnabled()` 跳过。动作栏文字进度、聊天提示、扣费、随行实体、GUI 逻辑保留。
- **`display/PortalDisplayManager.java`（新类，14 class）**：在每个阵法中心上方生成展示实体，显示一个特殊物品作为静态标记。
  - `ITEM_DISPLAY`（默认）：无碰撞、不可拾取、`setPersistent(false)` 不写存档，重启无残留。
  - `ARMOR_STAND`（可选）：隐形 marker 盔甲架头顶戴物品，兼容基岩端 / Geyser。
  - PDC 标记 + 每 100 tick 幂等补齐；建阵 / 删阵 / reload / 关服精确清理。
- **`/csz item` 子命令**（管理员）：`set`（把主手任意带数据组件的物品整体序列化进 `override.yml`）、`give`、`reset`、`refresh`、无参数查看状态；含 Tab 补全。
- **`display-item.use-as-trigger`**（默认 false）：可选「物品凭证模式」，玩家必须主手持有该特殊物品才能启动阵法（精确 `isSimilar` + 同材质/同显示名兜底）。

### 配置新增（`config.yml`）

```yaml
effects-enabled: false
display-item:
  mode: ITEM_DISPLAY      # 或 ARMOR_STAND
  enabled: true
  height: 1.2
  billboard: CENTER
  material: ECHO_SHARD
  name: "&d✦ 空间传送阵"
  use-as-trigger: false
```

`saveDefaultConfig()` 不覆盖旧配置，但所有新键的缺省值即「关特效 + 物品标记」，替换 jar 后不改配置也直接生效。

### 同步范围

- `自研/传送阵/SpacePortal/`（旧版，`api-version: 1.20`，Maven 离线编译，major 61）。
- `自研/升级26.3版本/SpacePortal/`（26.3，`api-version: 26.3`，`build_all.py` 编译，major 65）。
  - 保留 26.3 三处差异：Adventure `sendActionBar`、`Particle.DUST`、`show-coordinates` 走 override。
- 产物：`自研/传送阵/SpacePortal-1.0.0.jar`、`自研/升级26.3版本/_out/SpacePortal-1.0.0.jar`（各 14 class）。

### 顺带修复

- `自研/升级26.3版本/build_all.py`：资源复制时补齐 Maven filtering 的 `${project.version}`，此前 26.3 jar 内 `plugin.yml` 的 `version` 是字面量 `${project.version}`。
- `plugin.yml` 的 `description` 从「大型粒子阵法 + 时光穿梭特效」改为「特殊物品标记 + 可选粒子特效」。

### 验证

```
JDK25 javac --release 21 + leaf-api 26.3 + _deps  → 26.3 exit 0
JDK25 + Maven 3.9.9 -o clean package（spigot-api 1.20.1）→ 旧版 exit 0
两版 jar：14 class、含 PortalDisplayManager、config 含 effects-enabled:false / mode
源码扫描：EffectController 之外的 playSound / spawnParticle 调用点未受开关保护数 = 0
```

### 待实跑确认

- 基岩端对 `ITEM_DISPLAY` 的可见性取决于 Geyser 版本；若基岩玩家看不到标记，把 `display-item.mode` 改成 `ARMOR_STAND`。
- 若服内清理插件会清 Display / ArmorStand 实体，5 秒补齐机制会重建，必要时加入排除列表。
