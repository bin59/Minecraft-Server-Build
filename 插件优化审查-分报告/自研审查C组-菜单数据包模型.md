# 自研内容审查 C 组 — 快捷菜单 / RPG 数据包 / 自定义模型物品

审查日期：2026-10-15
审查范围：
1. 快捷菜单系统（QuickMenu Java 插件，21 个 .java）
2. 综合 RPG 框架（rpg_framework 数据包，mcfunction）
3. 自定义模型物品（资源包 + Python 打包/纹理脚本）

---

## 一、快捷菜单系统（QuickMenu Java 插件）

### [中] 每次打开 GUI 都重建全部 ItemStack/ItemMeta，未做缓存
- 位置：`plugin-src/src/main/java/com/nangua/quickmenu/gui/ChestMenuRenderer.java` → `render(Player, Menu)` / `buildItemStack` / `buildFiller`
- 问题：类头注释声称"物品图标会做缓存复用"（第 26 行），但实际代码中 `render()` 每次被调用都会重新 `new ItemStack(...)`、`getItemMeta()`、`setDisplayName`、`setLore`、写 PDC（itemKey/menuKey）。一个 54 格菜单每次打开要构造 ~10 个菜单项 ItemStack + 1 个 filler ItemStack，每个都走一遍 ItemMeta 序列化路径。菜单项内容在配置加载期就是只读常量（material/displayName/lore/glowing/id 都不变），完全可以在 `ConfigLoader.parseItem` 阶段预构建好 `ItemStack` 原型，打开时只做 `clone()`。filler 也是同理，每次 `buildFiller` 都重新 `Material.matchMaterial` + 构造 meta。
- 建议：在 `MenuItem` 中增加一个 `volatile ItemStack prototype` 字段，首次访问时构建并缓存；`render()` 里改为 `item.getPrototype().clone()`。filler 在 `Menu` 对象上缓存一份即可。
- 影响/收益：CPU（减少每次开菜单时的 ItemMeta 构造与 PDC 写入）。开菜单是玩家主动操作，频率不高，但高峰时多人同时开菜单可减少主线程瞬时尖峰。
- 证据：
  - ChestMenuRenderer.java 第 98-144 行 `render()`：
    ```java
    ItemStack filler = buildFiller(menu);          // 每次都重建
    ...
    for (MenuItem item : menu.getItems()) {
        ...
        inventory.setItem(slot, buildItemStack(menu, item));  // 每个 item 每次都重建
    }
    ```
  - 第 388-423 行 `buildItemStack()`：每次都 `new ItemStack` + `getItemMeta` + `setDisplayName` + `setLore` + PDC.set。
  - 绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统\plugin-src\src\main\java\com\nangua\quickmenu\gui\ChestMenuRenderer.java`

### [中] ResidenceHook 每次开默认菜单都做全链反射调用
- 位置：`plugin-src/.../hook/ResidenceHook.java` → `isInsideOwnResidence(Player)`；调用方 `menu/MenuManager.java` → `openDefaultMenu`
- 问题：玩家每次右键触发物品打开默认菜单时，`openDefaultMenu()` 都会调用 `ResidenceHook.isInsideOwnResidence(player)`。该方法内部每次执行 `Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi")` + 连续 4 次 `getMethod(...).invoke(...)`（第 36-44 行）。虽然 `available` 布尔做了缓存（第 17 行），但 Method 对象和 Class 对象没有缓存，每次都走 JVM 方法查找。右键触发是高频玩家操作，反射查找在主线程上有可观开销。
- 建议：把 `ResidenceApi` Class、`getResidence` Method、`getResidenceManager` Method、`getByPlayer` Method、`isOwner`/`isMember` Method 全部缓存为 `static volatile` 字段，首次成功反射后复用。`isInsideOwnResidence` 内部只做 `invoke`。
- 影响/收益：CPU（主线程反射调用减少）。每次开菜单省 4-5 次 `getMethod` 反射查找。
- 证据：
  - ResidenceHook.java 第 36-44 行：
    ```java
    Class<?> apiCls = Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi");
    Method getApi = apiCls.getMethod("getResidence");
    Object api = getApi.invoke(null);
    Method getRm = api.getClass().getMethod("getResidenceManager");
    Object rm = getRm.invoke(api);
    Method getByPlayer = rm.getClass().getMethod("getByPlayer", Player.class);
    Object res = getByPlayer.invoke(rm, player);
    ```
  - MenuManager.java 第 104-112 行 `openDefaultMenu` 每次打开都调 `ResidenceHook.isInsideOwnResidence(player)`。
  - 绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统\plugin-src\src\main\java\com\nangua\quickmenu\hook\ResidenceHook.java`

### [低] findItemById 每次点击线性遍历菜单项列表
- 位置：`listener/MenuClickListener.java` → `findItemById(Menu, String)`
- 问题：每次玩家点击菜单项，都线性遍历 `menu.getItems()` 查找匹配 id。菜单有几十个 item 时每次点击走 O(n)。可在 `Menu` 对象上缓存一个 `Map<String, MenuItem>`。
- 建议：`Menu` 类持有 `Map<String, MenuItem> byId`，在 `ConfigLoader.parseMenu` 阶段填充；`findItemById` 改为 O(1) 查表。
- 影响/收益：CPU（点击是用户事件，单次开销极小，但属于随手可改的优化）。
- 证据：MenuClickListener.java 第 285-292 行：
  ```java
  private MenuItem findItemById(Menu menu, String itemId) {
      for (MenuItem item : menu.getItems()) {
          if (item.getId().equals(itemId)) return item;
      }
      return null;
  }
  ```
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统\plugin-src\src\main\java\com\nangua\quickmenu\listener\MenuClickListener.java`

### [低] Material.matchMaterial 在渲染热路径重复调用
- 位置：`gui/ChestMenuRenderer.java` → `buildFiller`（第 371 行）、`buildItemStack`（第 389 行）、`isTriggerItem`（第 493 行）
- 问题：`Material.matchMaterial(String)` 每次打开菜单都被调用。该方法内部做字符串到枚举的查找表匹配。配置加载阶段已经在 `ConfigLoader.isValidMaterial` 校验过材质名，但没有把解析后的 `Material` 对象存下来。
- 建议：在 `MenuItem` / `Menu` 中缓存 `Material resolvedMaterial`，配置加载时解析一次。`buildTriggerItem` 同理。
- 影响/收益：CPU（微小）。
- 证据：ChestMenuRenderer.java 第 371 行 `Material material = Material.matchMaterial(materialName);`、第 389 行 `Material material = Material.matchMaterial(item.getMaterial());`
  绝对路径：同上。

### [低] config.yml 消息字符串在点击/交互热路径重复 getString
- 位置：`listener/MenuClickListener.java` 第 136-137 行、`listener/TriggerItemListener.java` 第 78-79 行、第 125-126 行
- 问题：每次点击无权限项、每次无权限右键、每次阻止丢弃，都调 `plugin.getConfig().getString("messages.xxx", ...)`。`getConfig()` 本身是内存对象，但 `getString` 会做路径解析和默认值处理。
- 建议：在 `PluginSettings` 中缓存所有 messages 字符串（启动/reload 时加载）。
- 影响/收益：CPU（极微）。
- 证据：MenuClickListener.java 第 136-137 行：
  ```java
  player.sendMessage(ActionExecutor.color(plugin.getConfig().getString(
          "messages.no-permission", "&c你没有权限使用快捷菜单")));
  ```
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统\plugin-src\src\main\java\com\nangua\quickmenu\listener\MenuClickListener.java`

### [中] AH_SELL 动作的背包交换逻辑在异步拍卖插件下可能丢物品/复制物品
- 位置：`menu/ActionExecutor.java` → `sellItemFromInventory(Player, String, Material)` 第 160-227 行
- 问题：方法把目标物品换到主手后立即 `player.performCommand("ah sell " + price)`，紧接着假设"上架成功后主手被扣走，失败则物品还在主手"并立即还原。但绝大多数拍卖行插件（EconomyShopGUI、AuctionHouse 等）的 `/ah sell` 是异步处理经济校验、背包扫描的，命令返回时物品可能尚未被扣除。此方法在 `performCommand` 返回后立即把主手换回原物品，如果拍卖插件还没扣走目标物品，就会把它塞回背包——导致玩家既上架了物品又保留了背包里的一份（复制物品）。反之若拍卖插件同步扣了但还原逻辑判断失误，会丢物品。
- 建议：不要在 `performCommand` 后立即还原。改用 `Bukkit.getScheduler().runTaskLater(plugin, () -> 还原逻辑, 2L)` 延迟 1-2 tick 再检查主手状态并还原。或更稳妥：让拍卖插件自己读主手，本插件只负责把物品换到主手，不做"还原"（让玩家上架后自己收回触发物品）。
- 影响/收益：安全/经济（物品复制漏洞风险）。这是最需要关注的正确性问题。
- 证据：ActionExecutor.java 第 211-226 行：
  ```java
  player.performCommand("ah sell " + price);
  ItemStack after = inv.getItemInMainHand();
  if (after != null && after.getType() == material) {
      // 立即还原，不等拍卖插件处理完
      if (foundStorage >= 0) inv.setItem(foundStorage, after);
      ...
  }
  inv.setItemInMainHand(current);
  ```
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统\plugin-src\src\main\java\com\nangua\quickmenu\menu\ActionExecutor.java`

### [低] 防连点 Map 在玩家快速切换菜单时可能误拦
- 位置：`listener/MenuClickListener.java` → `isSpamming(UUID, int)` 第 297-305 行
- 问题：`lastClick` 记录的是 `(slot, timestamp)`，但 slot 是基于当前打开的容器。玩家从菜单 A 点"返回"按钮（slot=53）切到菜单 B，如果菜单 B 里 slot=53 恰好是另一个功能项，且间隔 <300ms，会被误判为 spam 而吞掉点击。实际影响很小（300ms 内连点两屏概率低），但属于逻辑瑕疵。
- 建议：把菜单 id 也纳入 key（`lastClick` 改为存 `(menuId, slot, timestamp)`），或在打开新菜单时清空该玩家的记录。
- 影响/收益：UX（偶发点击无响应）。
- 证据：MenuClickListener.java 第 297-305 行，`record[0] == slot` 只比较槽位不比较菜单。
  绝对路径：同上。

### 正面确认（无问题项）
- **无线程安全问题**：`lastClick` HashMap 只在主线程事件处理器中访问（InventoryClickEvent / PlayerQuitEvent 都是主线程），无需 ConcurrentHashMap。
- **监听器无泄漏**：`onQuit` 第 318-320 行及时 `lastClick.remove(UUID)`；无玩家 Map 缓存。
- **无同步 IO/DB**：配置加载只在 onEnable 和 /qm reload 时发生（ConfigLoader.loadAll），运行时零文件读取。
- **无高频事件订阅**：只监听 PlayerInteractEvent / InventoryClickEvent / InventoryDragEvent / PlayerJoinEvent / PlayerDropItemEvent / PlayerQuitEvent，没有 PlayerMoveEvent 等每 tick 事件。
- **权限检查**：菜单项打开前（ChestMenuRenderer.render 第 114 行）和点击时（MenuClickListener 第 135 行）双重权限校验。
- **无 BukkitRunnable 泄漏**：插件没有注册任何定时任务。

---

## 二、综合 RPG 框架（rpg_framework 数据包）

### [高] 每 tick 执行约 25 个 `execute as @a[scores={...}]` 玩家遍历
- 位置：`data/rpg/function/tick.mcfunction` 第 3-39 行
- 问题：主 tick 函数每刻（20 次/秒）依次执行约 25 条 `execute as @a[scores={rpg.xxx=1..}] run function ...`。每条 `@a[scores=...]` 选择器都会遍历所有在线玩家检查计分板分数。在线玩家 N 人时，每 tick 做 25*N 次玩家分数判断。虽然这是基岩端通用的 trigger 菜单模式，但 25 个触发器偏多。
- 建议：这些触发器菜单（gui/menu/cls/pick/stats/skill/shop/buy/quest/qacc/qdone/warp/go/pay/daily/top/boss/lottery/elite/zone/portal/admin/zone_admin/portal_admin）可以合并为一个 `rpg.menu_action` 计分板，用不同数值代表不同菜单，一条 `execute as @a[scores={rpg.menu_action=1..}]` 进入一个分发函数。这样从 25 条 @a 扫描降为 1 条。
- 影响/收益：CPU（每 tick 减少 24 次全服玩家遍历）。50 人在线时每 tick 省 ~1200 次玩家分数查询。
- 证据：tick.mcfunction 第 5-29 行连续 25 条 `execute as @a[scores={rpg.xxx=1..}] at @s run function rpg:xxx/...`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\tick.mcfunction`

### [高] 升级检测对所有在线玩家每 tick 跑一次
- 位置：`tick.mcfunction` 第 32 行 → `player/level_check.mcfunction`
- 问题：`execute as @a[scores={rpg.inited=1}] run function rpg:player/level_check` 每 tick 对所有已初始化玩家执行一次，内部检查 `exp >= exp_need`。这是轮询式升级检测，20 次/秒/玩家。升级只在玩家获得经验时才需要判断，完全可以改为事件驱动：在击杀、任务奖励等加经验的函数末尾调用 `level_check`。
- 建议：去掉 tick.mcfunction 第 32 行的常驻调用；在所有 `scoreboard players add @s rpg.exp N` 的位置（kill_diff、reward、milestone 等）末尾加 `function rpg:player/level_check`。
- 影响/收益：CPU（每 tick 减少 N 次玩家 exp 比较）。50 人在线时每 tick 省 50 次分数比较。
- 证据：tick.mcfunction 第 32 行 `execute as @a[scores={rpg.inited=1}] run function rpg:player/level_check`；level_check.mcfunction 第 2 行 `execute if score @s rpg.level matches ..99 if score @s rpg.exp >= @s rpg.exp_need run function rpg:player/do_levelup`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\tick.mcfunction` 及 `...\player\level_check.mcfunction`

### [高] zone/tick 每秒做全服实体标签清除 + 8 区域盒扫描
- 位置：`data/rpg/function/zone/tick.mcfunction` 第 9 行、第 12-19 行、`zone/purge.mcfunction` 第 6-7 行
- 问题：第 9 行 `tag @e[type=#rpg:zone_mobs] remove rpg.zin` 每秒全服扫描 31 种怪物实体（zone_mobs tag 含 31 个实体类型），无条件清除标记。这是一个无坐标/无维度限制的全局实体选择器，在多世界/多区块加载服务器上开销大。随后第 12-19 行对 8 个启用区域各做一次 `@e[type=#rpg:zone_mobs,dx=...,dy=...,dz=...]` 盒扫描。purge.mcfunction 第 6-7 行又做一次全局 `kill @e[type=#rpg:zone_mobs,...]` 扫描。
- 建议：
  1. 第 9 行的全局 `tag @e[type=#rpg:zone_mobs] remove rpg.zin` 可以改为只在各区域 mark 函数内部做区域内清除（因为 mark 是从区域左上角 positioned 后用 dx/dy/dz 盒选择器的，天然只影响盒内实体）。全局清除这一步可以去掉，因为 mark 完后区域外的怪本来就没有 zin 标记。
  2. 如果保留全局清除，至少加 `[nbt=...]` 或限制在主世界维度。
- 影响/收益：CPU（每秒一次的全服实体扫描是数据包最贵的操作之一）。31 种怪物在多加载区块服务器上可能有数百到数千实体，每秒遍历一次。
- 证据：
  - zone/tick.mcfunction 第 9 行 `tag @e[type=#rpg:zone_mobs] remove rpg.zin`（无坐标限制，全局）
  - zone/purge.mcfunction 第 6 行 `kill @e[type=#rpg:zone_mobs,tag=!rpg.zin,tag=!rpg.zkeep,tag=!rpg.boss,tag=!rpg.elite]`（全局）
  - zone_mobs.json tag 含 31 个实体类型
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\zone\tick.mcfunction`

### [中] quest/check 每 tick 无条件执行 6 条玩家遍历
- 位置：`tick.mcfunction` 第 43 行 → `quest/check.mcfunction`
- 问题：`function rpg:quest/check` 每 tick（20 次/秒）无条件调用，内部 6 条 `execute as @a[scores={rpg.qXs=1}]`。即使没有任何玩家接任务（q1s-q6s 全为 0），每条选择器仍会遍历所有玩家检查分数。
- 建议：加一个全局 `#any_quest rpg.data` 计数器，玩家接任务时 +1、完成时 -1；tick.mcfunction 改为 `execute if score #any_quest rpg.data matches 1.. run function rpg:quest/check`。无任务时一行不执行。
- 影响/收益：CPU（无任务期间每 tick 省 6*N 次玩家遍历）。
- 证据：tick.mcfunction 第 43 行 `function rpg:quest/check`（无条件）；quest/check.mcfunction 第 2-7 行 6 条 `execute as @a[scores={rpg.qXs=1}]`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\tick.mcfunction`

### [中] boss_tick / elite_tick 内部重复做相同的实体存在性检查
- 位置：`mob/boss_tick.mcfunction` 第 2-4 行、`mob/elite_tick.mcfunction` 第 2-4 行
- 问题：boss_tick 每 tick 连续 3 次独立执行 `execute if entity @e[type=zombie,tag=rpg.boss]` 存在性检查（第 2、3、4 行），每次都要扫描实体列表。可以合并为一次 `execute as @e[type=zombie,tag=rpg.boss,limit=1] at @s run ...` 块。elite_tick 同理（第 2、3、4 行三次 `@e[tag=rpg.elite]`）。
- 建议：合并为：
  ```
  execute as @e[type=zombie,tag=rpg.boss,limit=1] at @s run function rpg:mob/boss_core
  ```
  把 boss_bar、boss_mark、boss_leash、水下检测全部放进一个 `boss_core` 函数里。
- 影响/收益：CPU（BOSS 存活期间每 tick 减少 2 次实体存在性扫描）。
- 证据：boss_tick.mcfunction 第 2-4 行：
  ```
  execute unless entity @e[type=minecraft:zombie,tag=rpg.boss] run function rpg:mob/boss_death
  execute if entity @e[type=minecraft:zombie,tag=rpg.boss] as @e[type=minecraft:zombie,tag=rpg.boss,limit=1] run function rpg:mob/boss_bar
  execute if entity @e[type=minecraft:zombie,tag=rpg.boss] as @e[type=minecraft:zombie,tag=rpg.boss,limit=1] at @s run function rpg:mob/boss_mark
  ```
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\mob\boss_tick.mcfunction`

### [中] portal/tick 每 5 tick 重置全服玩家 pnow + 8 座阵台粒子
- 位置：`portal/tick.mcfunction` 第 12 行、`portal/scan.mcfunction` 第 6 行
- 问题：第 12 行 `scoreboard players set @a rpg.pnow 0` 每 5 刻（每秒 4 次）重置所有在线玩家的 pnow 分数。第 6 行每座启用的传送台每 5 tick 发射 6 个 end_rod 粒子（`particle minecraft:end_rod ~ ~0.6 ~ 1.3 0.3 1.3 0.01 6`）。8 座阵台 = 每 5 tick 发 48 个粒子，每秒 192 个粒子。
- 建议：
  1. pnow 重置可以和 charge 合并：在 charge 函数里对 `pnow=0` 的玩家清零即可，不需要对 @a 全局 set。
  2. 待机粒子可以降频到每秒一次（每 20 tick 一次），或用 `particle ... force` 限制可视半径。
- 影响/收益：CPU/网络包（粒子包每 5 tick 广播一次给附近玩家）。
- 证据：portal/tick.mcfunction 第 12 行 `scoreboard players set @a rpg.pnow 0`；scan.mcfunction 第 6 行 `run particle minecraft:end_rod ~ ~0.6 ~ 1.3 0.3 1.3 0.01 6`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\portal\tick.mcfunction`

### [低] enable_triggers 每秒对每个玩家 enable 24 个触发器
- 位置：`tick.mcfunction` 第 55 行 → `player/enable_triggers.mcfunction`
- 问题：每秒对每个在线玩家执行 24 条 `scoreboard players enable @s rpg.xxx`。这是 trigger 计分板机制的固有开销（trigger 用一次就自动 disable），但 24 个触发器全量 enable 偏多。
- 建议：可以接受（这是数据包 trigger 模式的标准写法），但可考虑把不常用的触发器（admin/zone_admin/portal_admin 等管理员用的）只在玩家有权限时 enable，通过 `execute as @a[permission=...]` 过滤。
- 影响/收益：CPU（微小）。
- 证据：enable_triggers.mcfunction 第 2-25 行连续 24 条 `scoreboard players enable @s rpg.xxx`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\player\enable_triggers.mcfunction`

### [低] boss_mark / elite_mark 每 tick 读 NBT Health
- 位置：`mob/boss_mark.mcfunction` 第 2、4 行；`mob/elite_mark.mcfunction` 第 2、4 行
- 问题：BOSS/精英存活期间每 tick 两次 `data get entity @s Health`。这个开销很小（单个实体 NBT 读取），但可以合并为一次 store。
- 建议：把两次 `data get` 合并为一次：
  ```
  execute store result score #boss_cur rpg.data run data get entity @s Health
  execute if score #boss_cur rpg.data < #boss_last_hp rpg.data run tag @a[distance=..64] add rpg.boss_fight
  scoreboard players operation #boss_last_hp rpg.data = #boss_cur rpg.data
  ```
  （去掉第二次重复 store）。
- 影响/收益：CPU（极微）。
- 证据：boss_mark.mcfunction 第 2 行和第 4 行各 `execute store result score #boss_cur rpg.data run data get entity @s Health`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\综合RPG框架\rpg_framework\data\rpg\function\mob\boss_mark.mcfunction`

### 正面确认
- 总开关设计合理：zone/tick 和 portal/tick 开头都有 `execute unless score #xxx_on rpg.data matches 1 run return 0`，关闭时零开销。
- BOSS/精英 tick 只在 `#boss_alive`/`#elite_alive=1` 时才执行（tick.mcfunction 第 45、51 行），平时不跑。
- zone 刷怪有"区域内无玩家则不刷"保护（zone/spawn.mcfunction 第 8 行 `if score #zpl matches 0 run return 0`）。
- 跨天检测用 daytime 比较（tick.mcfunction 第 47-49 行），不是每秒轮询日历，合理。

---

## 三、自定义模型物品（资源包 + Python 脚本）

### [中] diamond_sword.json 使用已废弃的 overrides/custom_model_data predicate 机制
- 位置：`模型物品资源包/assets/minecraft/models/item/diamond_sword.json` 第 7-10 行
- 问题：文件在 `models/item/diamond_sword.json` 中使用 `"overrides": [{"predicate": {"custom_model_data": 101}, ...}]` 语法。在 MC 1.21.4（pack_format 64）之后，物品模型系统改为 `assets/minecraft/items/<item>.json` 定义文件，旧的 `models/item/*.json` 中的 `overrides` + `custom_model_data` predicate 已被废弃/移除。当前 pack.mcmeta 声明 `pack_format: 64`（对应 1.21.4），此 overrides 块在该版本可能不会生效，导致自定义模型不显示。
- 建议：迁移到新系统：新建 `assets/minecraft/items/diamond_sword.json`，内容形如：
  ```json
  {
    "model": {
      "type": "minecraft:select",
      "property": "minecraft:custom_model_data",
      "cases": [
        {"when": 101, "model": {"type": "minecraft:model", "model": "minecraft:item/pumpkin_blade"}},
        {"when": 102, "model": {"type": "minecraft:model", "model": "minecraft:item/nexus_core"}}
      ],
      "fallback": {"type": "minecraft:model", "model": "minecraft:item/diamond_sword"}
    }
  }
  ```
- 影响/收益：正确性（模型在新版本是否生效）。【待确认】需在目标服务器版本上实测是否生效——如果服务器跑的是 1.21.1 或更早（pack_format 应该是 34 左右），旧 overrides 仍可用；当前 pack_format 64 表明目标版本是 1.21.4+，此风险较高。
- 证据：diamond_sword.json 第 7-10 行：
  ```json
  "overrides": [
    { "predicate": { "custom_model_data": 101 }, "model": "minecraft:item/pumpkin_blade" },
    { "predicate": { "custom_model_data": 102 }, "model": "minecraft:item/nexus_core" }
  ]
  ```
  pack.mcmeta 第 3 行 `"pack_format": 64`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\自定义模型物品\模型物品资源包\assets\minecraft\models\item\diamond_sword.json`

### [低] 打包脚本 SHA1 计算把整个 zip 读入内存
- 位置：`打包资源包.py` 第 31 行
- 问题：`hashlib.sha1(open(OUT, "rb").read()).hexdigest()` 一次性把整个 zip 读进内存。资源包通常几十 MB，虽然打包时机器内存够用，但写法不规范，且文件句柄未关闭（没有 with 语句）。
- 建议：改为分块读取：
  ```python
  h = hashlib.sha1()
  with open(OUT, "rb") as f:
      for chunk in iter(lambda: f.read(8192), b""):
          h.update(chunk)
  print(h.hexdigest())
  ```
- 影响/收益：内存（打包脚本是构建时运行，不影响服务器）。
- 证据：打包资源包.py 第 30-31 行：
  ```python
  import hashlib
  print(hashlib.sha1(open(OUT, "rb").read()).hexdigest())
  ```
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\自定义模型物品\打包资源包.py`

### [低] 生成纹理.py 硬编码物品列表，不可扩展
- 位置：`生成纹理.py` 第 83-92 行
- 问题：脚本底部硬编码了 `pumpkin_blade.png` 和 `nexus_core.png` 两个纹理的生成调用。新增第三个自定义物品时必须手动编辑 Python 代码添加一行。没有从 models/item/ 目录自动扫描需要哪些纹理。
- 建议：遍历 `assets/minecraft/models/item/*.json`，解析其中的 `textures.layer0` 字段，自动生成对应的占位纹理。
- 影响/收益：可维护性（开发效率）。
- 证据：生成纹理.py 第 85-89 行硬编码两个 write_png 调用。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\自定义模型物品\生成纹理.py`

### [低] pumpkin_blade.json 引用了 layer3 纹理槽
- 位置：`模型物品资源包/assets/minecraft/models/item/pumpkin_blade.json` 第 5 行
- 问题：`"layer3": "minecraft:item/pumpkin_blade"` 引用了 layer3 层。`item/generated` 父模型标准使用 layer0（和可选 layer1/layer2），layer3 不是标准层，可能被忽略或导致模型加载警告。
- 建议：确认是否需要 layer3；如果只是想让模型两面都有贴图，layer0 已经足够（item/generated 会自动双面渲染）。
- 影响/收益：正确性（微小视觉差异）。
- 证据：pumpkin_blade.json 第 4-5 行 `"layer0": "..."` 和 `"layer3": "..."`。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\自定义模型物品\模型物品资源包\assets\minecraft\models\item\pumpkin_blade.json`

### [低] 打包脚本未排除 pack.mcmeta 之外的冗余文件
- 位置：`打包资源包.py` 第 21-26 行
- 问题：脚本遍历 SRC 目录下所有文件打包。如果资源包目录里临时产生了 `.DS_Store`、Thumbs.db、备份文件等，会被一起打进 zip，增大包体积。
- 建议：在 `z.write` 前加过滤：`if name.startswith('.') or name.startswith('Thumbs'): continue`。
- 影响/收益：包体积（微小）。
- 证据：打包资源包.py 第 22-25 行 `for name in sorted(files): ... z.write(full, arc)` 无过滤。
  绝对路径：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\自定义模型物品\打包资源包.py`

### 正面确认
- `.bat` 脚本路径处理安全：`cd /d "%~dp0"` 切换到脚本目录，`where python` / `where py` 两级回退，无路径注入风险。
- zip 打包方式正确：arcname 用 `os.path.relpath` + 反斜杠转正斜杠，保证 zip 根目录直接是 pack.mcmeta/assets/，不多套一层文件夹。
- 打包脚本幂等：`zipfile.ZipFile(OUT, "w", ...)` 每次覆盖输出，不会累积旧文件。
- pack.mcmeta 的 `supported_formats` 字段设置了 min/max 兼容范围，合理。
