# PumpkinMail 性能改造说明

- 日期：2026-10-15
- 插件根：`F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜邮箱-PumpkinMail\`
- 新产物：`PumpkinMail.jar`（12,016,633 字节，旧 12,015,376 字节）

---

## 一、改动点（文件 + 方法 + 改前/改后）

### 改动一：countPending 改为 COUNT 查询（不再物化 List）

**文件**：`src/main/java/com/nangua/pumpkinmail/storage/Storage.java`

**改前**：
```java
public synchronized int countPending(String playerName, java.util.UUID uuid) {
    return getPending(playerName, uuid).size();
}
```
（会把该玩家全部 Reward 行构造为 List，只为取 size。100 人在线 × 每 30 秒 = 每轮 100 次全行物化查询。）

**改后**：
```java
public synchronized int countPending(String playerName, java.util.UUID uuid) {
    String key = cacheKey(playerName, uuid == null ? null : uuid.toString());
    Integer cached = pendingCache.get(key);
    if (cached != null) return cached;
    int n = countPendingFromDb(playerName, uuid);
    pendingCache.put(key, n);
    return n;
}

private int countPendingFromDb(String playerName, java.util.UUID uuid) {
    String sql = "SELECT COUNT(*) FROM rewards WHERE claimed=0 AND (player_name=? COLLATE NOCASE";
    if (uuid != null) sql += " OR player_uuid=?";
    sql += ")";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, playerName);
        if (uuid != null) ps.setString(2, uuid.toString());
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
}
```

### 改动二：待领缓存（Map<key,Integer> + synchronized）

**字段（新增）**：
```java
private final Map<String, Integer> pendingCache = new HashMap<>();

private String cacheKey(String playerName, String playerUuid) {
    if (playerUuid != null && !playerUuid.isEmpty()) return playerUuid;
    return playerName == null ? "" : playerName.toLowerCase(Locale.ROOT);
}
```
key 优先用 player_uuid，否则用小写玩家名——与查询语义 `player_name=? COLLATE NOCASE OR player_uuid=?` 对齐。所有 Storage 方法本就是 `synchronized`，缓存与 conn 共用一把锁，天然线程安全。

**addReward（成功插入时推进缓存）**：
```java
int affected = ps.executeUpdate();
if (affected > 0) {
    String key = cacheKey(r.playerName(), r.playerUuid());
    if (pendingCache.containsKey(key)) {
        pendingCache.merge(key, affected, Integer::sum);
    }
}
```
注意：INSERT OR IGNORE 可能因主键冲突静默忽略——只在 `affected > 0` 时才推进。且只在缓存已有该玩家条目时自增；若该玩家从未被 countPending 过，不主动写缓存（下次 countPending 会直查数据库回填，避免基于未知旧值自增）。

**markClaimed（精准失效该玩家缓存条目）**：
```java
// 先查出 reward 归属玩家
String ownerName = null, ownerUuid = null;
try (PreparedStatement sel = conn.prepareStatement(
        "SELECT player_name, player_uuid FROM rewards WHERE id=?")) { ... }
// 执行 UPDATE ...
// 不做"缓存 -1"（无法确切知道剩余条数），直接移除该玩家条目，下次查库回填
if (ownerName != null || ownerUuid != null) {
    pendingCache.remove(cacheKey(ownerName, ownerUuid));
} else {
    pendingCache.clear();
}
```

**新增公开方法（防膨胀/重启清理）**：
```java
public synchronized void clearPendingCache() { pendingCache.clear(); }
```

**PumpkinMail.onDisable 调用一次**：
```java
if (storage != null) {
    try { storage.clearPendingCache(); } catch (Exception ignored) { }
    try { storage.close(); } catch (Exception ignored) { }
}
```

### 改动三：WAL / synchronous / busy_timeout

**Storage 构造器（连接建立后立即执行）**：
```java
try (Statement st = conn.createStatement()) {
    st.executeUpdate("PRAGMA journal_mode=WAL");
    st.executeUpdate("PRAGMA synchronous=NORMAL");
    st.executeUpdate("PRAGMA busy_timeout=5000");
    // 原 CREATE TABLE / CREATE INDEX 语句保持不变
}
```

### 未改动项（严格保持对外 API）
- `addReward(Reward)` 签名不变（OfflineReward 复用不受影响）
- `getPending / listForPlayer / stats / savePreset / getPreset / listPresets / removePreset / close` 签名与行为不变
- 命令 `/mailbox`、plugin.yml、config.yml 一字未动
- HintTask.java 本身未改（仍调 `storage.countPending(name, uuid)`，但现在优先命中缓存，30 秒轮询几乎不再查库）

---

## 二、缓存一致性与防膨胀设计

**一致性路径**：
1. **写入**：`addReward` 成功插入 → 若该玩家已在缓存中，原子 +1（merge）；不在缓存则不写，下次查库自然带上新行。
2. **领取**：`markClaimed` 不做"减 1"（无法确切知道剩余条数，可能并发/同 ID 多发），改为按 reward 归属玩家 `pendingCache.remove(key)`，下次 `countPending` 回退查库回填真实值。
3. **异常兜底**：`countPendingFromDb` 内 SQLException 被捕获并 printStackTrace、返回 0，与全项目风格一致；任何异常都不会让缓存返回脏值（异常路径下连 put 都不执行，下次仍会重试查库）。
4. **重启回退**：Storage 实例随 onDisable 销毁，缓存是堆内 Map，进程结束即清空；onDisable 里还显式调了一次 `clearPendingCache()`。

**防膨胀**：
- 缓存条目只在"被 countPending 查过的玩家"上产生——HintTask 只遍历在线玩家，所以条目数 ≈ 在线玩家数，绝不会无限累积。
- 玩家领取后该玩家条目被移除。
- 服务器重启/插件重载后缓存为空，第一次轮询冷启动后再次回填。
- 极端情况（如管理员批量发放给数千离线玩家但他们从未上线）也不会污染缓存，因为缓存只记录在线被轮询过的玩家。

**性能效果**：HintTask 每 30 秒一轮，100 人在线时——
- 改前：100 次 `getPending(...).size()`，每次物化全部 Reward 行（含字符串解析、Reward 对象构造）。
- 改后：99 次直接命中堆内 HashMap（O(1)），0~1 次冷启动 COUNT(*) 查询（走 `idx_rewards_player` 索引，仅扫索引不取行）。

---

## 三、构建命令（实际执行）

```powershell
$javac = "C:\Program Files\Java\jdk-21\bin\javac.exe"
$jar   = "C:\Program Files\Java\jdk-21\bin\jar.exe"
$root  = "F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜邮箱-PumpkinMail"
Set-Location $root

# 1) 编译
$srcFiles = Get-ChildItem -Path "src\main\java" -Recurse -Filter *.java | % { $_.FullName }
& $javac -encoding UTF-8 -cp "lib\*" -d "_build\classes" $srcFiles

# 2) 组装 staging
Copy-Item "_build\classes\com" "_build\staging\com" -Recurse
Copy-Item "src\main\resources\plugin.yml" "_build\staging\plugin.yml"
Copy-Item "src\main\resources\config.yml" "_build\staging\config.yml"
# 从 sqlite-jdbc jar 解压 org/ + sqlite-jdbc.properties
& $jar xf "$root\lib\sqlite-jdbc-3.53.4.0.jar"   # 在 _build\sqlite_src 下执行
Copy-Item "_build\sqlite_src\org" "_build\staging\org" -Recurse
Copy-Item "_build\sqlite_src\sqlite-jdbc.properties" "_build\staging\sqlite-jdbc.properties"

# 3) 打包
& $jar --create --file "PumpkinMail.jar" -C "_build\staging" .
```

---

## 四、验证结果（逐项）

| # | 验证项 | 结果 |
|---|---|---|
| 1 | `javac -encoding UTF-8 -cp "lib\*"` 零错误 | ✅ exit code 0，仅 deprecation 提示（无关） |
| 2 | `jar tf` 新旧 com/ 类清单对比 | ✅ 旧 18 条 = 新 18 条，`Compare-Object` 无差异 |
| 3 | plugin.yml / config.yml 在 jar 内且与源一致 | ✅ MD5：plugin.yml `6CBE77...1F991`、config.yml `EF827C...F807`，jar 内与源文件逐字节相同；`sqlite-jdbc.properties`、`META-INF/MANIFEST.MF` 均在 |
| 4 | `javap -verbose` Storage / HintTask | ✅ `major version: 65`（Java 21）、`minor 0`；Storage 常量池可见 `countPendingFromDb`、`countPending`、`clearPendingCache`，方法表含所有原有 public synchronized 方法 |
| 5 | 新 jar 复制到生产 plugins\ | ✅ `F:\...\leaf-1.21.11\plugins\PumpkinMail.jar` 存在，MD5 `A74D88...91EDA` 与插件根新 jar 完全一致 |

---

## 五、备份清单

| 文件 | 路径 | 大小 |
|---|---|---|
| 插件根旧 jar | `F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜邮箱-PumpkinMail\PumpkinMail.jar.bak_20261015` | 12,015,376 字节 |
| 生产旧 jar | `F:\game\pc\MC\开服\服务器数据备份\leaf-1.21.11\plugins\PumpkinMail.jar.bak_20261015` | 12,015,376 字节 |

回滚方法：把对应 `.bak_20261015` 改回 `PumpkinMail.jar` 覆盖即可（数据库 schema 未变，WAL 是连接级 pragma，向下兼容）。

---

## 六、部署状态

- 插件根 `PumpkinMail.jar`：已替换为新版本（12,016,633 字节）。
- 生产 `plugins\PumpkinMail.jar`：已替换为新版本，MD5 与插件根一致。
- 数据库文件 `plugins\PumpkinMail\mailbox.db` 未触碰；首次启动时连接会自动切到 WAL 模式（产生 `-wal` / `-shm` 旁文件，属正常）。
- 未执行 reload/重启服务器——按运维习惯由管理员择机 `/reload` 或重启服务端生效。
