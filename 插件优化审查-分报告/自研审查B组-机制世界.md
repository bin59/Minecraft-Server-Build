# 自研插件性能与安全审查报告（B 组 · 机制世界）

审查对象：Leaf/Paper 1.21.11 上 5 个自研插件。所有结论均来自实际读取的 `src/` 下 Java 源码与 `resources/config.yml`。
审查日期：2026-10-15。文件路径均为绝对路径。

---

## 传送阵 SpacePortal
（源码根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\传送阵\SpacePortal\src\main\java\com\mc\spaceportal\`）

### [中] 地面粒子任务每帧对"每个阵法"都全量遍历充能会话
- 位置：`effect/PortalEffectTask.java` → `run()`（L32-45）+ `teleport/TeleportService.java` → `getChargeProgress()`（L359-367）
- 问题：特效任务默认每 4 tick（每秒 5 次）在主线程跑一帧。对**每一个**阵法都调用 `getTeleportService().getChargeProgress(portalName)`，而该方法内部又 `for (PortalSession s : sessions.values())` 线性扫描全部充能会话。复杂度是 O(阵法数 × 充能人数)，每帧、每玩家可见世界都重复算。阵法上百个时，每帧白白扫几千次。
- 建议：把 `Map<String,List<PortalSession>> byPortal` 在 `sessions.put/remove` 时同步维护，`getChargeProgress` 直接按阵法名取列表；或维护 `Map<String,Double> maxProgress`，在 `tickSession` 里顺手刷新，特效任务 O(1) 读取。
- 影响/收益：CPU/主线程耗时。阵法越多收益越明显；消除每帧重复扫描。
- 证据：`PortalEffectTask.java` L43 `double charge = plugin.getTeleportService().getChargeProgress(portal.getName());`；`TeleportService.java` L361 `for (PortalSession session : sessions.values()) { if (session.portalName.equals(portalName)) ... }`。

### [中] 跨格移动事件对全阵法做线性扫描，且含字符串世界名比较
- 位置：`listener/PortalListener.java` → `onMove()` / `findPortalNear()`（L34-84）
- 问题：玩家每次跨一个方块（跑步时约每秒数次）就在主线程 `for (Portal portal : plugin.getPortals().all())` 全量遍历，做 `portal.getWorldName().equals(loc.getWorld().getName())` 字符串比较 + 距离平方运算。阵法多时这是高频主线程 CPU 开销。注释里已做"只跨格才判"的优化（L36-40），但内层扫描仍是 O(总阵法数)。
- 建议：在 PortalManager 里按世界名建 `Map<String, List<Portal>> byWorld`，`findPortalNear` 只取当前世界的子列表遍历；阵法名建阵/删阵时同步维护。
- 影响/收益：CPU/主线程。人多+阵法多时收益明显。
- 证据：`PortalListener.java` L70 `for (Portal portal : plugin.getPortals().all()) {` L71 `if (!portal.getWorldName().equals(loc.getWorld().getName())) { continue; }`。

### [中] 命名/连接等待集合在异步聊天线程与主线程间无同步共享
- 位置：`gui/PortalGuiListener.java` → 字段 `naming`(HashSet) / `linking`(HashMap)（L33-35），`onChat()`（L245-262）
- 问题：`onChat` 是 `AsyncPlayerChatEvent`，在异步线程执行 `naming.contains(uuid)` / `linking.containsKey(uuid)`；而 `handleCreate`/`onClose`/`finishNaming` 在主线程对同一个 HashSet/HashMap 做 add/remove。非并发容器跨线程读写，存在可见性丢失与 HashMap 结构破坏（resize 时死循环/丢数据）风险。
- 建议：改用 `ConcurrentHashMap.newKeySet()` 与 `ConcurrentHashMap`；或在 onChat 里只把 uuid 投递到主线程后再判断集合（当前已 reschedule 执行 finish，但判断动作本身放在了异步侧）。
- 影响/收益：线程安全/偶发诡异 bug。
- 证据：`PortalGuiListener.java` L33 `private final Set<UUID> naming = new HashSet<>();` L35 `private final Map<UUID, String> linking = new HashMap<>();` L249-250 在 onChat 内直接 `naming.contains(uuid)`。

### [低] cooldownUntil 冷却 Map 只增不清，离线玩家条目常驻
- 位置：`teleport/TeleportService.java` → `applyCooldown()`（L324-327）
- 问题：`cooldownUntil.put(...)` 在每次传送后写入，但 `onQuit`/`handleQuit` 里只 `cancelCharge`，从不 remove 这个玩家的冷却条目。长期运行 + 玩家频繁上下线会累积小对象（每个 16 字节左右），泄漏缓慢但确实存在。
- 建议：在 `handleQuit`（L370）里加 `cooldownUntil.remove(player.getUniqueId());`。
- 影响/收益：内存。收益小，但一行修复。
- 证据：`TeleportService.java` L370-372 `public void handleQuit(Player player) { cancelCharge(player, false); }`（无 cooldownUntil 清理）。

### [低] 每次 emit 都 new 一个 Particle.DustOptions
- 位置：`effect/EffectController.java` → `emit()`（L202-208）
- 问题：`COLOR_CACHE` 缓存了 Color，但 `new Particle.DustOptions(hueToColor(hue), size)` 每一颗粒子调用都新建。单帧几十次 emit × 每秒 5 帧，产生短命对象 GC 压力。
- 建议：仿照 COLOR_CACHE，按 `(int)hue,size` 缓存 DustOptions（size 只有几档）。
- 影响/收益：GC/对象分配。收益小。
- 证据：`EffectController.java` L206-207 `p.getWorld().spawnParticle(Particle.DUST, p, count, spread, spread, spread, 0, new Particle.DustOptions(hueToColor(hue), size));`。

> 正面记录：特效任务在 `applyConfig` 里先 `cancel()` 旧任务再新建（SpacePortalPlugin.java L74-81），`onDisable` 调 `cancelTasks`，无任务泄漏；`getCenter()` 用 `Bukkit.getWorld(name)` 取已加载世界，不触发 `getChunkAt` 加载；费用在传送前用 `chargeFee` 双重校验，无免费 TP 漏洞；阵法名正则 `[A-Za-z0-9_\u4e00-\u9fa5]{1,16}` 校验到位。

---

## AdvZone
（源码根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\AdvZone\src\main\java\com\hh\advzone\`）

### [高] /az 全部管理子命令零权限校验，任意玩家可建/删区域、强制全服冒险模式
- 位置：`AdvZone.java` → `onCommand()`（L291-431）
- 问题：`onCommand` 里没有任何 `sender.hasPermission(...)` 或 `isOp()` 判断。任何普通玩家都能 `/az create <名>`（在脚下任意拉一个多边形区域，L329-348），区域会让走进来的人被强制改成冒险模式（`check()` L261-273）；也能 `/az delete <名>`（L369-380）删掉管理员建的任何区域，`/az reload`、`/az edit` 同理。这是越权/破坏型安全漏洞。
- 建议：在 `onCommand` 开头对 `wand/add/undo/clear/points/create/edit/delete/reload` 等管理子命令加 `if (!p.hasPermission("advzone.admin")) { p.sendMessage("无权限"); return true; }`；`check`/`list`/`info` 可放行。plugin.yml 里注册 `advzone.admin` 默认 op。
- 影响/收益：安全。不修则服内任何人可搞破坏。
- 证据：`AdvZone.java` L301 `switch (args[0].toLowerCase()) {` 直接进入各子命令分支，全程无权限判断；对照 `check()` L262 玩家判定里反而有 `p.hasPermission("advzone.bypass")`，说明权限体系设计了却没在命令入口用。

### [中] 选点粒子预览任务每 10 tick 常驻跑，即使无人选点也不停止
- 位置：`AdvZone.java` → `startParticlePreview()`（L119-133）
- 问题：`runTaskTimer(this, ..., 20L, 10L)` 从 onEnable 启动后永不 cancel。每 0.5 秒遍历全服在线玩家，只为找"有没有人手里有选点缓冲"。绝大多数时候 `selection` 是空的，纯空转；有人选点时才真正发粒子。
- 建议：把任务句柄存下来，仅当 `selection` 从空变为非空时启动、从非空变为空时 cancel（在 addPoint/undoPoint/clear/quit 处判断）；或直接降频到 20L（1 秒）。
- 影响/收益：CPU/无谓调度。收益小但属"轻量"承诺的逆命题。
- 证据：`AdvZone.java` L132 `}, 20L, 10L);`（无返回值保存，无法后续 cancel）；L121 `for (Player p : getServer().getOnlinePlayers()) {`。

### [低] 跨格移动对所有区域做射线法判点，区域多时 O(区域数×顶点数)
- 位置：`AdvZone.java` → `onMove()`/`zoneAt()`/`Zone.contains()`（L247-281, L72-87）
- 问题：已做"只跨方块才判"的优化（L252-254，好），但 `zoneAt` 对每个区域都跑一遍包围盒+射线法。区域顶点多/数量多时，玩家跑步是高频主线程开销。
- 建议：现状可接受；若区域 >20 个，可按世界 + 粗粒度分桶（区块网格）缓存玩家所在区域，跨格时只查相邻桶。
- 影响/收益：CPU。低优先级。
- 证据：`AdvZone.java` L276-281 `for (Zone z : zones) { if (z.contains(loc)) return z.name; }`。

> 正面记录：`onDisable` 会把被强制冒险的玩家还原（L109-116）；`onQuit` 清理 forced/selection（L242-245）；选点魔棒左键 `setCancelled` 防误破坏（L222）；saveConfig 仅在 create/delete 时触发，非高频。

---

## 建筑评分 BuildRate
（源码根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\建筑评分-BuildRate\src\main\java\com\nangua\buildrate\`）

### [中] 开打分面板时为每座建筑单独查一次分数（N+1 查询），每次都新开 SQLite 连接
- 位置：`gui/GuiManager.java` → `buildItem()`（L218-230），配合 `storage/Db.java` → `open()`（L24-31）
- 问题：`openList` 循环对每座建筑调 `buildItem`，里面 `db().getScores(b.id())` 单独开一个连接、跑一次 `SELECT * FROM scores WHERE build_id=?`。而 `openResult` 明明有 `getScoresByBuild(eventId)` 一次取全活动分数的能力，列表面板却没用。N 座建筑 = N 次连接 + N 次 PRAGMA。
- 建议：`openList` 先 `Map<Integer,List<Score>> all = db().getScoresByBuild(ev.id());` 一次取好，`buildItem` 改为接收 `List<Score>` 参数，复用 `avgText`。
- 影响/收益：主线程 DB IO。一次开面板从 N 次连接降到 2 次。
- 证据：`GuiManager.java` L224 `List<Models.Score> scores = db().getScores(b.id());`（在 for 循环内）；`Db.java` L24-31 每次 `open()` 都 `DriverManager.getConnection` + 2 条 PRAGMA。

### [中] /br history 与 GUI 历史面板对每个活动各查一次建筑数（N+1）
- 位置：`cmd/BuildRateCommand.java` → `history()`（L272-286）；`gui/GuiManager.java` → `historyItem()`（L350-361）
- 问题：`history` 循环 `for (Event ev : all)` 里每次 `db().getBuilds(ev.id()).size()`，历史活动越多，命令/GUI 打开时主线程同步查询次数越多。
- 建议：一条 SQL `SELECT event_id, COUNT(*) FROM builds GROUP BY event_id` 一次取回，内存里取 size。
- 影响/收益：主线程 DB IO。
- 证据：`BuildRateCommand.java` L278 `int n = db().getBuilds(ev.id()).size();`；`GuiManager.java` L356 `db().getBuilds(ev.id()).size()`（构造 lore 时）。

### [低] 每次查询都重连 + 重复执行 PRAGMA busy_timeout/journal_mode
- 位置：`storage/Db.java` → `open()`（L24-31）
- 问题：没有连接复用，每个 DAO 方法 `try (Connection c = open())` 都新建连接，且每次都 `PRAGMA busy_timeout=5000` + `PRAGMA journal_mode=WAL`。WAL 模式其实只需设置一次（持久在 db 文件头）。
- 建议：持有一个长连接（插件单例，定时心跳），或至少在 open 里去掉重复的 journal_mode PRAGMA；WAL 可在 init() 里跑一次。
- 影响/收益：DB IO。本插件低频，收益有限，但改法简单。
- 证据：`Db.java` L26-29 `s.execute("PRAGMA busy_timeout=5000"); s.execute("PRAGMA journal_mode=WAL");`（位于每次 open 路径）。

### [低] pendingCreate 聊天等待集合无超时、关界面也不清理
- 位置：`gui/GuiManager.java` → `pendingCreate`（L48, L210-214, L429-443）
- 问题：注释写"10 秒内"，但代码里既没有定时任务移除，也没有在 `InventoryCloseEvent` 里清理。管理员点了"创建活动"又关窗不说话，UUID 永久留在 map 里（下次他随便发一句话还会被当成活动名创建——轻微）。
- 建议：在 `onClick`/打开管理界面处注册一个 10 秒后 `pendingCreate.remove(uuid)` 的 `runTaskLater`；或在监听 InventoryCloseEvent 时清理。
- 影响/收益：内存 + 偶发误触发。
- 证据：`GuiManager.java` L48 `private final Map<UUID, Boolean> pendingCreate = new HashMap<>();` L212 `pendingCreate.put(p.getUniqueId(), true);`（无对应 close 清理）。

> 正面记录：所有 SQL 参数化或 int 拼接（id/eventId 均为 int，无注入）；打分有 min/max/自评/活动归属校验；admin 子命令统一走 `requireAdmin`（L290-294）；结算发奖走南瓜邮箱队列，不在主线程发经济。

---

## 基岩皮肤记录 BedrockSkinRecorder
（源码根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\基岩皮肤记录-BedrockSkinRecorder\plugin-src\src\main\java\com\nangua\bedrockskinrecorder\`）

### [中] 每个玩家上线都在主线程反射 FloodgateApi 并重复 getMethod 查找
- 位置：`SkinCaptureListener.java` → `onJoin()`（L24-32）；`SkinFetcher.java` → `isBedrock()`/`callFloodgateApi()`/`clientBrand()`（L54-112）
- 问题：`PlayerJoinEvent` 在主线程触发，对**每个**玩家都执行：`Class.forName` + `getMethod("getInstance")` + invoke，随后 `getMethod("isFloodgatePlayer")`、`getMethod("getPlayer")`、`player.getClass().getMethod("getClientBrandName")`。`Class.getMethod` 会遍历方法表，人多的服每次上线都付这份反射查找开销。
- 建议：把 `FloodgateApi` 实例、`isFloodgatePlayer`/`getPlayer` 两个 Method、以及 `getClientBrandName` Method 缓存到 `static volatile` 字段（onEnable 时探测一次），isBedrock 里只 invoke 不再 lookup。
- 影响/收益：CPU/上线峰值。
- 证据：`SkinFetcher.java` L95-97 `Class<?> clazz = Class.forName("org.geysermc.floodgate.api.FloodgateApi"); Method getInstance = clazz.getMethod("getInstance");`（每次 isBedrock 都走一遍）；L65/L71/L106 反复 `getMethod`。

### [中] 兜底下载 PNG 用 openStream 无超时，网络挂起会永久占用异步任务
- 位置：`SkinFetcher.java` → `download()`（L254-264）
- 问题：`new URL(urlStr).openStream()` 没有 connect/read timeout。textures.minecraft.net 慢响应或被墙时，这条异步任务线程会无限期阻塞；虽然重试链上限 5 次（MAX_ATTEMPTS），但每次 download 本身可能永远不返回，线程泄漏。
- 建议：用 `HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(); conn.setConnectTimeout(5000); conn.setReadTimeout(8000);` 再取流。
- 影响/收益：异步线程安全/资源泄漏。
- 证据：`SkinFetcher.java` L255 `try (InputStream in = new URL(urlStr).openStream();`（无任何 timeout 设置）。

### [低] /bsr list、onEnable 在主线程 listFiles 扫描皮肤目录
- 位置：`SkinStore.java` → `countRecorded()`/`listRecorded()`（L63-88）；调用点 `BedrockSkinRecorder.java` L41, L63
- 问题：`listFiles` + `Arrays.sort` by lastModified 在主线程执行。仅 /bsr 命令触发，低频；但皮肤文件累积上千个时，单次 list 仍可能轻微卡顿。
- 建议：可接受；如需彻底，listRecorded 走异步任务查完后主线程发消息。当前量级不必改。
- 影响/收益：主线程 IO。低。
- 证据：`SkinStore.java` L67 `File[] files = skinsDir.listFiles((d, n) -> n.endsWith(".png"));`。

> 正面记录：抓皮肤主链路走 `runTaskLaterAsynchronously`（SkinCaptureListener L30），文件写入 `store.save` 在异步线程，不堵主线程；皮肤文件名经 `SAFE` 正则过滤（SkinStore L20, L39），无路径穿越；套皮动作 `applySkinOnMainThread` 正确 reschedule 回主线程（SkinFetcher L150）；重试链有界（MAX_ATTEMPTS=5），无任务泄漏。

---

## 末影龙重生与破坏保护 DragonManager
（源码根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\末影龙重生与破坏保护\DragonManager源码\src\com\hh\dragon\`）

### [中] /dragon respawn 无权限校验，任意玩家可召唤末影龙并全服广播
- 位置：`DragonManager.java` → `onCommand()`（L35-62）
- 问题：命令入口没有任何 `hasPermission` 判断。任何在线玩家都能 `/dragon respawn [世界]` 生成一只末影龙，并且 `Bukkit.broadcastMessage` 全服公告。虽有"该世界已有龙则不重复生成"的保护（L51-56），但玩家可在任意末地子世界反复刷龙、扰乱服务器。
- 建议：在 onCommand 开头加 `if (!sender.hasPermission("dragon.respawn")) { sender.sendMessage("无权限"); return true; }`，plugin.yml 注册该权限默认 op。
- 影响/收益：安全/服内秩序。
- 证据：`DragonManager.java` L35 `public boolean onCommand(...) {` L36 直接判断 args，无权限检查；L59-60 `Bukkit.broadcastMessage(...)`。

### [低] 查"是否已有龙"用 getEntities().stream() 拉全世界实体
- 位置：`DragonManager.java` → `onCommand()`（L51-52）
- 问题：`w.getEntities()` 取末地全部实体再 stream 匹配。末地实体数量有限、且仅命令时触发，影响小；但更省的写法是 `w.getEntitiesByClass(EnderDragon.class).isEmpty()`（同样全量，但语义清晰）或用 `Entities` 标签。
- 建议：可保留；如在意可改 `w.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).isEmpty()`。
- 影响/收益：CPU。低。
- 证据：`DragonManager.java` L51-52 `boolean hasDragon = w.getEntities().stream().anyMatch(e -> e.getType() == EntityType.ENDER_DRAGON);`。

> 正面记录：`onEntityPlace` 只比 4 个槽位坐标（L72-88），极廉价；只在 THE_END 维度生效；无定时器、无 DB、无内存累积，整体非常轻量。

---

## 汇总：最高优先级三件事
1. **AdvZone `/az` 命令零权限**（高）——任何玩家可建/删区域强制他人冒险，必须补 `advzone.admin`。
2. **DragonManager `/dragon respawn` 零权限**（中）——任何玩家可刷龙+全服广播，补 `dragon.respawn`。
3. **BuildRate GUI/history 的 N+1 SQLite 查询**（中）——开打分面板时为每座建筑单独连库查分，改为一次 `getScoresByBuild`。
