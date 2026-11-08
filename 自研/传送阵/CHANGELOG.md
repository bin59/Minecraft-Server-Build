# SpacePortal 变更记录

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
