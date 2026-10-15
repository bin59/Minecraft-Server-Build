# 自研插件审查 A 组 · 经济/数据类（性能与安全）

审查范围：Leaf/Paper 1.21.11 服上 6 个被标注"轻量/低占用"的自研插件。
所有结论均来自实际读到的 src 下 .java 与 resources 下 yml，行号对应当地文件。
公共根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\`

---

## 南瓜账本-MoneyLedger

### [高] 每次余额变动都在主线程同步写 SQLite，未开 WAL/未批量，每笔一次 fsync
- 位置：MoneyLedger.java → onBalanceUpdate（L62-100）；ledger/LedgerStore.java → insert（L45-62）、open（L27-43）
- 问题：EssentialsX 的 `UserBalanceUpdateEvent` 由 eco/商店/拍卖等操作在主线程触发，监听器里直接 `store.insert(...)` 同步写一行。`open()` 建连接后没有任何 `PRAGMA`，SQLite 默认 journal_mode=DELETE + synchronous=FULL，每条 INSERT 自带一次事务提交+磁盘 fsync。EconomyShop 连续买卖、拍卖成交、批量发奖时，每一笔钱变动都在主线程上堵一次磁盘 IO。
- 建议：
  1. open() 里执行 `PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=5000;`；
  2. 引入内存队列：事件线程只 `queue.offer(entry)`，单条后台线程按"累计 N 条 或 每 1 秒"批量 `addBatch()+executeBatch()` 刷盘。
  ```java
  // onEnable
  Executors.newSingleThreadExecutor().submit(() -> { while(!Thread.interrupted()){ List<LedgerEntry> batch=drainQueue(50,1000); if(!batch.isEmpty()) store.insertBatch(batch); } });
  ```
- 影响/收益：IO/卡顿。高频交易时段主线程每笔省一次 fsync，肉眼可感知的"点购买不卡"；WAL 下读写不互斥。
- 证据：
  - `LedgerStore.java` L27-43 `open()` 只 `CREATE TABLE/INDEX`，无 PRAGMA；
  - L45-62 `insert()` 每次 `conn.prepareStatement(...).executeUpdate()` 单条提交。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\src\main\java\com\nangua\moneyledger\ledger\LedgerStore.java`

### [高] 命令完全无权限校验，任意玩家可查/导出任何人的流水
- 位置：cmd/MoneyLedgerCommand.java → onCommand（L35-55）；resources/plugin.yml（L8-17）
- 问题：plugin.yml 声明了 `moneyledger.admin`（default: op），但既没把它挂到 `moneyledger` 命令的 `permission:` 上，onCommand 里也没有任何 `sender.hasPermission(...)` 判断。结果任意玩家可 `/moneyledger recent <任意玩家>`、`/moneyledger summary <任意玩家>`、`/moneyledger export`（全表导出）、`/moneyledger stats`。玩家隐私（谁给谁转了多少钱）完全暴露。
- 建议：二选一即可——
  - plugin.yml 在 commands.moneyledger 下加 `permission: moneyledger.admin`（Bukkit 自动拦截）；
  - 或 onCommand 开头统一：`if(!sender.hasPermission("moneyledger.admin")){ sender.sendMessage("§c无权限"); return true; }`
- 影响/收益：安全/隐私。修一行即可堵住任意玩家财务流水窥探。
- 证据：
  - `MoneyLedgerCommand.java` L35-55 整个 onCommand 无 hasPermission；
  - `plugin.yml` L8-13 commands 段只有 description/usage/aliases，无 permission 字段。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\src\main\java\com\nangua\moneyledger\cmd\MoneyLedgerCommand.java`

### [高] /moneyledger export 无参时 SELECT * 全表一次性载入内存并主线程写 CSV
- 位置：cmd/MoneyLedgerCommand.java → export（L96-132）、allRows（L134-137）；ledger/LedgerStore.java → allByNameNull（L112-114）
- 问题：`/moneyledger export`（不带玩家名）走 `allByNameNull()`，SQL 是 `SELECT * FROM ledger ORDER BY id ASC`，把全表所有历史行逐行 `new LedgerEntry` 装进 ArrayList，再在主线程 `BufferedWriter` 写 CSV。账本长期运行后表可能几十万行，一次性全 load 进内存 + 主线程写大文件 = 卡顿甚至 OOM。
- 建议：
  1. export 整体丢 `Bukkit.getScheduler().runTaskAsynchronously(...)`；
  2. 流式分页读（`ORDER BY id ASC LIMIT 5000 OFFSET ?` 循环），边读边写 CSV，不一次性持有全表；
  3. 或直接禁用无参全量导出，仅允许 `/moneyledger export <玩家>`。
- 影响/收益：内存/主线程卡顿。避免管理员随手一 export 把服务器打 TPS 掉。
- 证据：
  - `LedgerStore.java` L112-114 `query("SELECT * FROM ledger ORDER BY id ASC", ps->{})`；
  - `MoneyLedgerCommand.java` L100-104 无参分支 `rows = allRows();`。
  - 文件：同上。

### [中] 每笔钱变动都抓两次完整调用栈做来源判定
- 位置：ledger/SourceDetector.java → detectPackage（L70-86）、describe（L92-115）；MoneyLedger.java → onBalanceUpdate（L71-76）
- 问题：`categorize(cause)` 在 Cause=API 时内部调一次 `detectPackage()` 抓栈；紧接着 L76 `describe(reason)` 又抓一次完整栈并双层循环（遍历栈帧 × 遍历 IGNORE 前缀数组）。每笔余额变动都跑两次，发生在主线程。
- 建议：在 onBalanceUpdate 里抓一次 `StackTraceElement[] st = Thread.currentThread().getStackTrace()`，复用给 categorize 和 describe；非 API cause 时直接跳过 detectPackage。
- 影响/收益：CPU。交易密集时段省掉每次双倍栈遍历。
- 证据：
  - `SourceDetector.java` L71 `Thread.currentThread().getStackTrace()`、L93 再次调用；
  - `MoneyLedger.java` L72、L76 两次分别触发。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\src\main\java\com\nangua\moneyledger\ledger\SourceDetector.java`

### [中] 事件里 Bukkit.getOfflinePlayer(uuid) 反查名字，主线程潜在磁盘阻塞
- 位置：MoneyLedger.java → onBalanceUpdate（L81-85）
- 问题：`e.getPlayer().getName()` 为 null 时调 `Bukkit.getOfflinePlayer(uuid)`，首次未缓存会读 usercache/离线玩家数据文件。
- 建议：name 拿不到时直接用 uuid 前 8 位兜底（已经是 L85 的兜底），把 `getOfflinePlayer` 这步去掉或丢异步。
- 影响/收益：主线程卡顿。
- 证据：`MoneyLedger.java` L82-84。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\src\main\java\com\nangua\moneyledger\MoneyLedger.java`

### [低] ledger 表只增不删，/stats 每次全表 COUNT(*)
- 位置：ledger/LedgerStore.java → count（L116-123）；全文件无任何 DELETE/归档
- 问题：流水只 INSERT 不清理，表无限膨胀；`/moneyledger stats` 的 `SELECT COUNT(*)` 在 SQLite 上对大表是全表扫。
- 建议：加定时任务删除 N 天前记录；count 结果缓存 5~10 分钟。
- 影响/收益：磁盘/IO。
- 证据：`LedgerStore.java` L116-123。

---

## 南瓜邮箱-PumpkinMail

### [高] HintTask 每 30 秒遍历全体在线玩家，每人一次主线程 SQLite 查询
- 位置：HintTask.java → tick（L39-44）、onJoin（L31-35）；storage/Storage.java → countPending（L146-148）、getPending（L117-133）
- 问题：`runTaskTimer` 跑在主线程，默认每 30 秒（config hint.interval-seconds=30）对每个在线玩家调一次 `countPending`。而 `countPending` 的实现是 `return getPending(...).size();`——它把该玩家所有待领 Reward 行（含 data/desc 长文本）全部 `new Reward(...)` 物化进 List，只为取个 size。100 人在线 = 每 30 秒 100 次主线程 SELECT + 行构造。
- 建议：
  1. countPending 直接改成 `SELECT COUNT(*) FROM rewards WHERE claimed=0 AND (player_name=? COLLATE NOCASE OR player_uuid=?)`；
  2. 进一步在内存维护一个 `Set<UUID> hasPending`：发奖 addReward 时加、领取 markClaimed 后若该玩家无剩余则移除，tick 只对集合里的人发 ActionBar，彻底去掉周期性 DB 查。
- 影响/收益：IO/主线程卡顿。这是 PumpkinMail 上最违背"低占用"的一点。
- 证据：
  - `Storage.java` L146-148 `public synchronized int countPending(...) { return getPending(playerName, uuid).size(); }`；
  - `HintTask.java` L40-43 `for (Player p : ...getOnlinePlayers()) { int n = ...countPending(...); ... }`。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜邮箱-PumpkinMail\src\main\java\com\nangua\pumpkinmail\storage\Storage.java`

### [中] 每次领取都用反射 Class.forName + getMethod + invoke 接 MoneyLedger
- 位置：RewardApplier.java → setLedgerReason（L51-56）、clearLedgerReason（L58-63）
- 问题：玩家每点一次领取，就做两次 `Class.forName("com.nangua.moneyledger.MoneyLedger")` + `getMethod(...).invoke(null, reason)`。
- 建议：类加载时静态缓存 `Method setReason`（找不到就留 null），后续直接 `if(m!=null) m.invoke(null, reason)`。
- 影响/收益：CPU。领取是低频操作，收益小但改法简单。
- 证据：`RewardApplier.java` L53-54、L60-61。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜邮箱-PumpkinMail\src\main\java\com\nangua\pumpkinmail\RewardApplier.java`

### [中] ChatInput.pending 静态 Map 无超时/退服清理
- 位置：ChatInput.java → pending（L19）、await（L26-28）、onChat（L30-44）
- 问题：`static final Map<UUID, Consumer<String>> pending` 注册后，玩家若不输入/cancel 就永远残留 Consumer（闭包持有 plugin）。管理员流程低频，泄漏慢但存在。
- 建议：注册时一起记 `long expireAt`，定时清理超过 5 分钟的；或在 PlayerQuitEvent 里 remove。
- 影响/收益：内存。
- 证据：`ChatInput.java` L19、L26-28。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜邮箱-PumpkinMail\src\main\java\com\nangua\pumpkinmail\ChatInput.java`

### [低] 管理 GUI 点击/Tab 补全反复 listPresets() 查库
- 位置：gui/GuiManager.java → onClick pick 分支（L151）；cmd/MailboxCommand.java → onTabComplete（L252、L261）
- 问题：管理员在奖品库 GUI 每点一个格子都 `SELECT ... FROM presets`；玩家每按一次 Tab 也全量查 presets。
- 建议：openAdmin 时把 `List<Preset>` 放进 AdminSession 缓存；Tab 补全缓存 id 列表。
- 影响/收益：IO。管理员低频操作，收益小。
- 证据：`GuiManager.java` L151；`MailboxCommand.java` L252、L261。

### [低] SQLite 连接未开 WAL
- 位置：storage/Storage.java → 构造器（L16-44）
- 问题：getConnection 后只建表，无 PRAGMA。与 MoneyLedger/OfflineReward 一致。
- 建议：`PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;`。
- 证据：`Storage.java` L18-19。

---

## 离线奖励-OfflineReward

### [低] 上线/退出事件里同步 SQLite 读写在主线程
- 位置：listener/PlayerListener.java → onJoin（L24-50）、onQuit（L52-58）
- 问题：每次 join 2 次 SELECT（lastLogout、lastReward）+ 可能 1 次 UPDATE；每次 quit 1 次 upsert，全在主线程且走 synchronized 连接。单玩家无所谓，但开服/重启后批量登录时会在锁上串行。
- 建议：join/quit 的 DB 操作丢 `runTaskAsynchronously`，发奖提示用 `runTask` 回主线程。
- 影响/收益：IO/主线程。低频但易在高峰放大。
- 证据：`PlayerListener.java` L31、L35、L44、L56。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\离线奖励-OfflineReward\src\main\java\com\nangua\offlinereward\listener\PlayerListener.java`

### [低] 回退直发命令字符串拼接
- 位置：reward/Delivery.java → deliver（L40）
- 问题：`Bukkit.dispatchCommand(..., "eco give " + player.getName() + " " + amount)`。amount 是 long（安全），player.getName() 来自当前在线玩家。风险低，记录在案。
- 建议：保持现状即可；若担心基岩名带空格可改用 offlinerward 的 Vault API 直接 deposit。
- 证据：`Delivery.java` L40。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\离线奖励-OfflineReward\src\main\java\com\nangua\offlinereward\reward\Delivery.java`

> 备注：plugin.yml L17 已正确给 `offlinereward` 命令挂了 `permission: offlinereward.admin`（default op），所以 reload/check 不存在越权问题——初看像漏了权限，核实后排除。

---

## 附魔商店-EnchantShop

### [低] onEnable 主线程遍历 Registry.ENCHANTMENT 并逐个读 yaml 配置
- 位置：EnchantShop.java → loadEnchants（L67-90）
- 问题：启动时对每个 excellentenchants 附魔做 `File.exists()` + `YamlConfiguration.loadConfiguration(f)` 磁盘读。仅启动一次，影响的是开服时间，不是运行期 TPS。
- 建议：可接受；如要进一步，把 loadEnchants 丢异步线程，GUI 打开前确保完成。
- 影响/收益：启动时间。
- 证据：`EnchantShop.java` L78-86。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\附魔商店-EnchantShop\src\main\java\com\nangua\enchshop\EnchantShop.java`

### [低] 硬编码相对路径 plugins/ExcellentEnchants/enchants
- 位置：EnchantShop.java → loadEnchants（L68）
- 问题：写死 `"plugins/ExcellentEnchants/enchants"`，依赖进程工作目录；EE 改名/换目录即失效。
- 建议：`new File(Bukkit.getPlugin("ExcellentEnchants").getDataFolder(), "enchants")`。
- 证据：`EnchantShop.java` L68。

> 备注：本插件无 DB、无定时任务、无持久化，运行期只有 GUI 点击，本身非常轻量，符合目标。命令 `/eshop` 无权限是设计上公开（玩家用经验等级购买），不视为问题。

---

## 每日随机收购-DailySell

### [高] 每次卖出都同步 saveData() 全量重写 data.yml（主线程磁盘 IO）
- 位置：DailySell.java → sellFromGui（L463-464）、sellHand（L566-567）、sellAll（L599-600）、saveData（L278-280）
- 问题：玩家每点一次卖（包括 GUI 连点、Shift 连卖满额度），都 `data.save(dataFile)` 把整个 YamlConfiguration 序列化成文本写盘。多人同时在 /ds 摆摊时，主线程被 YAML 序列化 + 磁盘写反复卡住。这是 6 个插件里最违背"轻量"的一点。
- 建议：
  1. `data.set(key, v)` 后只置 `dirty=true`；
  2. 用一个 2~5 秒防抖的 `runTaskLater`：dirty 才真正 `data.save(dataFile)`；
  3. onDisable 时强制 saveData()。
  ```java
  private void saveDataAsync() {
      if (dirtySaver != null) dirtySaver.cancel();
      dirtySaver = runTaskLater(this, () -> { dirty=false; try{data.save(dataFile);}catch(Exception e){} }, 100L);
  }
  ```
- 影响/收益：IO/主线程卡顿。把每次卖一次 fsync 降为每秒最多一次合并写。
- 证据：
  - `DailySell.java` L463-464 `data.set(...); saveData();`；L566-567、L599-600 同样模式；
  - L278-280 `private void saveData(){ data.save(dataFile); }`。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\每日随机收购\DailySell源码\src\com\hh\dailysell\DailySell.java`

### [高] data.yml 键按"玩家+日期+物品"无限累积，从不清理
- 位置：DailySell.java → soldKey（L256-258）；rollIfNeeded（L154-176）
- 问题：键格式 `players.<uuid>.<yyyy-MM-dd>.<MATERIAL>`。每天每个卖过的玩家每种物品留一条，跨天只增不删。跑几个月 data.yml 越来越大，上一条 saveData() 每次要序列化的内容也越来越多（叠加放大）。
- 建议：`rollIfNeeded` 检测到日期变化时，prune 掉 `players.*.<昨天及更早>.*` 节点（或只保留最近 N 天）；示例：
  ```java
  if (!now.equals(stored)) {
      for (String uuid : data.getConfigurationSection("players").getKeys(false))
          for (String date : data.getConfigurationSection("players."+uuid).getKeys(false))
              if (date.compareTo(now) < 0) data.set("players."+uuid+"."+date, null);
  }
  ```
- 影响/收益：内存/磁盘。长期运行避免 data.yml 膨胀到几 MB 还每次全量重写。
- 证据：`DailySell.java` L256-258；全文件无任何 `data.set("players...旧日期...", null)` 清理。

### [中] 先扣物品再发钱，中间异常会吞物品
- 位置：DailySell.java → sellFromGui（L461-462）、sellHand（L564-565）、sellAll（L597-598）
- 问题：`removeItems(p, mat, amt)` 之后才 `econ.depositPlayer(...)`。若 deposit 抛异常（经济插件抽风），物品已扣、钱没加。
- 建议：先 `econ.depositPlayer` 成功再 `removeItems`；或 try/catch 失败时回补物品。
- 影响/收益：数据一致性。
- 证据：`DailySell.java` L461-462。

### [低] 60 秒一次的 rollIfNeeded 定时任务
- 位置：DailySell.java → onEnable（L120-125）
- 问题：`runTaskTimer(this, 1200L, 1200L)` = 每 60 秒一次 `LocalDate.now()` 字符串比较 + 内存里 isSet，开销极小。可接受。
- 建议：保持现状。
- 证据：`DailySell.java` L120-125。

---

## 在线时间排行-PlanTop

### [中] 每 5 分钟新建一次 MySQL 连接，无长连接/连接池
- 位置：PlaytimeCache.java → refresh（L78-101，重点 L80）
- 问题：`try (Connection con = DriverManager.getConnection(url, user, password))` 每个刷新周期（默认 300s）都新建 TCP + 鉴权 + 关闭。Plan 数据库在公网（config.yml host=123.207.3.73），每次 connect+auth 开销叠加网络 RTT。
- 建议：持有一个长 `Connection`，refresh 前 `con.isValid(2)` 探活，断了重连；或引入 HikariCP。
- 影响/收益：网络/CPU。省掉每 5 分钟一次的 TCP 握手+认证。
- 证据：`PlaytimeCache.java` L80。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\在线时间排行-PlanTop\src\com\plantop\PlaytimeCache.java`

### [中] config.yml 把公网 MySQL 主机/账号写在仓库模板里
- 位置：resources/config.yml（L4-8）
- 问题：host `123.207.3.73`、user `host21k5c4` 是真实生产连接信息（密码写的是 xxx 占位）。随插件分发会暴露数据库拓扑。
- 建议：模板里写 `your-host` / `your-user`，真实值放私有本地配置。
- 影响/收益：安全。
- 证据：`config.yml` L4-8。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\在线时间排行-PlanTop\src\config.yml`

### [低] PAPI 每次请求 updated 占位符都 new SimpleDateFormat
- 位置：PlanTopExpansion.java → onPlaceholderRequest（L46-49）
- 问题：全息板/Tab 每刷新一次 `%ptop_updated%` 就 `new SimpleDateFormat("yyyy-MM-dd HH:mm")`。SimpleDateFormat 构造有开销且非线程安全。
- 建议：改成 `private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");` 线程安全且无重复分配。
- 影响/收益：内存/GC。
- 证据：`PlanTopExpansion.java` L48。
  - 文件：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\在线时间排行-PlanTop\src\com\plantop\PlanTopExpansion.java`

### [低] 聚合 SQL 对 plan_sessions 全历史 SUM，无日期下界
- 位置：PlaytimeCache.java → refresh（L81-83）
- 问题：`SUM(s.session_end - s.session_start)` 对该 server_id 下所有历史 session 聚合，Plan 数据日积月累后每 5 分钟一次的聚合会越来越慢。
- 建议：若业务可接受，加 `AND s.session_start > ?`（近 N 天）；若必须全历史则忽略。
- 影响/收益：DB 负载。
- 证据：`PlaytimeCache.java` L81-83。

---

## 汇总：最该先修的 5 件事
1. **MoneyLedger 命令加权限**（一行 plugin.yml），堵任意玩家查/导出他人财务流水。
2. **DailySell saveData() 改防抖异步落盘**——当前每卖一次就主线程写全量 yml，是最明显的 TPS 隐患。
3. **PumpkinMail countPending 改 COUNT(\*) + 在线待领缓存**——30 秒一次全员 DB 轮询。
4. **MoneyLedger 写流水改异步批量 + WAL**——每笔钱一次 fsync 太奢侈。
5. **DailySell data.yml 跨天清理旧日期键**——否则 saveData 的成本随时间线性膨胀。
