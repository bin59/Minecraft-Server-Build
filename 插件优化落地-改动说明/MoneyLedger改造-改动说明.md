# MoneyLedger 性能改造 — 改动说明

- 插件根目录：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\`
- 改造日期：2026-10-15
- 仅改动 1 个源文件：`src\main\java\com\nangua\moneyledger\ledger\LedgerStore.java`
- 未改动：`MoneyLedger.java`（onBalanceUpdate 调用方零改动）、`cmd\MoneyLedgerCommand.java`、`ledger\Category.java`、`ledger\LedgerEntry.java`、`ledger\SourceDetector.java`、`plugin.yml`、`config.yml`、对外命令/API。

---

## 一、问题回顾

原实现：每笔 `UserBalanceUpdateEvent` 在主线程同步执行 `INSERT INTO ledger ...`；
`open()` 未设任何 PRAGMA，SQLite 默认 `journal_mode=DELETE` + `synchronous=FULL`，
每笔 INSERT 触发一次 fsync，高频发币（商店/拍卖/活动批量发奖）时主线程卡顿。

## 二、改动点

### 改动一：WAL 模式（`LedgerStore.open()`，L65–67）

建表/建索引之后新增三行 PRAGMA（原 open() 只有建表语句）：

```java
// 改前：无 PRAGMA
s.execute("CREATE INDEX IF NOT EXISTS idx_cat ON ledger(category)");
}

// 改后（L63–67）：
s.execute("CREATE INDEX IF NOT EXISTS idx_cat ON ledger(category)");
// 性能改造一：WAL + NORMAL 同步级别 + 忙等待超时
s.execute("PRAGMA journal_mode=WAL");
s.execute("PRAGMA synchronous=NORMAL");
s.execute("PRAGMA busy_timeout=5000");
```

效果：DELETE 日志 → WAL 预写日志；每次 fsync 仅在 checkpoint 发生，写并发下读写不互锁；
忙时等待 5s 而不是立刻抛 SQLITE_BUSY。

### 改动二：异步批量落盘（`LedgerStore.insert()` + 新增写线程）

新增字段（L35–43）：

```java
private static final int BATCH_SIZE = 50;
private static final long FLUSH_INTERVAL_MS = 1000L;

private final BlockingQueue<LedgerEntry> queue = new LinkedBlockingQueue<>();
private ExecutorService writer;
private volatile boolean closed;
```

**insert()（L85–88）——签名保持 `public void insert(LedgerEntry e)`，与调用方完全兼容：**

```java
// 改前：主线程同步 prepareStatement + executeUpdate，每笔一次 fsync
public synchronized void insert(LedgerEntry e) {
    if (conn == null) return;
    try (PreparedStatement ps = conn.prepareStatement("INSERT ...")) {
        ...绑定 9 个参数...
        ps.executeUpdate();
    }
}

// 改后：入队即返回，主线程零 JDBC 操作
public void insert(LedgerEntry e) {
    if (closed || conn == null || e == null) return;
    queue.offer(e);
}
```

**open() 末尾（L69–73）启动单条守护线程：**

```java
closed = false;
writer = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "MoneyLedger-DbWriter");
    t.setDaemon(true);
    return t;
});
writer.execute(this::writerLoop);
```

**writerLoop()（L91–120）——批量消费：**

```java
private void writerLoop() {
    while (true) {
        LedgerEntry first;
        try {
            first = queue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS); // 最多等 1 秒
        } catch (InterruptedException ie) {
            flushRemaining();   // 关闭中断：刷剩余队列后退出
            return;
        }
        if (first == null) {
            if (closed) { flushRemaining(); return; }
            continue;
        }
        List<LedgerEntry> batch = new ArrayList<>(BATCH_SIZE);
        batch.add(first);
        queue.drainTo(batch, BATCH_SIZE - 1); // 最多攒到 50 条
        flushBatch(batch);
    }
}
```

**flushBatch()（L124–146）——唯一执行写 SQL 的地方：**

```java
private void flushBatch(List<LedgerEntry> batch) {
    synchronized (this) {                       // 与所有读查询共用 this 锁
        if (conn == null || batch.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ledger(...) VALUES(?,?,?,?,?,?,?,?,?)")) {
            for (LedgerEntry e : batch) {
                ...绑定 9 个参数...
                ps.addBatch();
            }
            ps.executeBatch();                  // 一批一次提交
        }
    }
}
```

**close()（L244–262）——onDisable 关闭顺序：**

```java
public synchronized void close() {
    closed = true;                              // 1) 停止接受新记录
    if (writer != null) {
        writer.shutdownNow();                  // 2) 中断 poll，写线程 flushRemaining
        writer.awaitTermination(10, TimeUnit.SECONDS);
    }
    if (conn != null) conn.close();             // 3) 刷完后关连接
    conn = null;
}
```

### 线程安全说明

- 主线程 `insert()` 只做 `queue.offer`，不触碰连接；
- SQLite 连接只在 `synchronized (this)` 块内被使用（写线程 `flushBatch` / 主线程命令读查询 `recent/summary/all/count` 等），同一时刻最多一个线程操作连接；
- 低峰期最多 1 秒、高峰期最多 50 条触发一次批量提交；关服时中断写线程会先把队列剩余条目全部落盘再关连接。

## 三、构建命令

```powershell
$root='F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger'
$javac='C:\Program Files\Java\jdk-21\bin\javac.exe'
$jar='C:\Program Files\Java\jdk-21\bin\jar.exe'

# 编译
Remove-Item -Recurse -Force "$root\_build" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$root\_build\classes"
$srcs = Get-ChildItem -Recurse "$root\src\main\java" -Filter *.java | % { $_.FullName }
& $javac -encoding UTF-8 -cp "$root\lib\*" -d "$root\_build\classes" $srcs

# 组装 staging：插件 classes + resources + sqlite-jdbc 的 org/ 与 properties
$staging="$root\_build\staging"
New-Item -ItemType Directory -Force $staging
Copy-Item -Recurse "$root\_build\classes\com" "$staging\com"
Copy-Item "$root\src\main\resources\plugin.yml" $staging
Copy-Item "$root\src\main\resources\config.yml" $staging
Push-Location $staging
& $jar xf "$root\lib\sqlite-jdbc-3.53.4.0.jar" org sqlite-jdbc.properties
Pop-Location

# 打包 fat jar
& $jar cf "$root\MoneyLedger.jar" -C "$root\_build\staging" .
```

## 四、验证结果

| 验证项 | 结果 |
|---|---|
| javac 编译 | exit code 0，无 warning/error，6 个源文件全部通过 |
| 类清单 | 旧 jar 文件条目 158 条，新 jar 158 条，`Compare-Object` 差异 = 0（8 个 com/nangua class 全在，org/sqlite/** 全在，sqlite-jdbc.properties 在） |
| plugin.yml / config.yml | 与旧 jar 内文件 SHA-256 完全一致（`plugin.yml CE757C…`，`config.yml 6ED545…`），未改动 |
| javap -verbose LedgerStore | `minor version: 0 / major version: 65`（Java 21），类可正常解析 |
| 新 jar 大小 | 12,004,221 字节（旧 11,999,835 字节，+4,386 字节为新增队列/线程类字节码） |

旧 jar 条目摘要（fat jar 结构）：`META-INF/MANIFEST.MF` + `com/nangua/moneyledger/**`（8 class）+ `plugin.yml` + `config.yml` + `sqlite-jdbc.properties` + `org/sqlite/**`（sqlite-jdbc 3.53.4.0 全部类与各平台 native 库），新 jar 与之逐文件一致。

## 五、备份文件清单

| 文件 | 大小 | 说明 |
|---|---|---|
| `自研\南瓜账本-MoneyLedger\MoneyLedger.jar.bak_20261015` | 11,999,835 B | 开发目录旧 jar 备份 |
| `服务器数据备份\leaf-1.21.11\plugins\MoneyLedger.jar.bak_20261015` | 11,999,835 B（2026/10/7 原文件） | 生产环境旧 jar 备份 |

## 六、部署状态

- 新 `MoneyLedger.jar`（12,004,221 B）已复制到 `F:\game\pc\MC\开服\服务器数据备份\leaf-1.21.11\plugins\MoneyLedger.jar`，目录确认存在。
- 注意：WAL 模式首次开启后，数据库文件旁会出现 `ledger.db-wal` / `ledger.db-shm` 两个辅助文件，属正常现象，勿删；服务器下次重启（或 `/reload`）后新代码生效。
- 回滚方式：把两个 `.bak_20261015` 复制回对应 `MoneyLedger.jar` 即可。
