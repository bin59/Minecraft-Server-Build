# DailySell 性能改造 — 改动说明

- 插件：每日随机收购 DailySell（单文件 `DailySell.java`）
- 改造日期：2026-10-15
- 目标：消除每次卖出同步全量写 `data.yml` 的主线程开销；控制 `data.yml` 体积随日期无限膨胀。
- 玩法/金额/物品消耗/config/plugin.yml/命令逻辑：**未改动**。

---

## 一、改动点（方法 + 行号 + 改前/改后）

行号为改动后文件 `DailySell源码/src/com/hh/dailysell/DailySell.java` 的实际行号。

### 1. 新增字段（L51-58 附近）

新增 import：`org.bukkit.scheduler.BukkitTask;`（L21，紧随原有 `BukkitRunnable`）。

新增字段：

```java
// L53-58
private boolean dirty = false;
private BukkitTask saveTask = null;
private static final long SAVE_DELAY_TICKS = 100L;   // 5 秒合并窗口
private static final int KEEP_DAYS = 7;              // 旧日期保留天数（含当天）
```

### 2. 防抖落盘：saveData() / markDirty()（L315-335）

改前（原 L278-280）：

```java
private void saveData() {
    try { data.save(dataFile); } catch (IOException ex) { getLogger().severe("data.yml 保存失败"); }
}
```

改后：

```java
private void saveData() {
    try {
        data.save(dataFile);
        dirty = false;
    } catch (Throwable t) {
        // 保存失败只记日志，绝不让主线程抛异常
        getLogger().severe("data.yml 保存失败: " + t.getMessage());
    }
}

private void markDirty() {
    dirty = true;
    if (saveTask == null) {
        saveTask = getServer().getScheduler().runTaskLater(this, (Runnable) () -> {
            saveTask = null;
            if (dirty) saveData();
        }, SAVE_DELAY_TICKS);
    }
}
```

要点：
- 用 scheduler + lambda（invokedynamic）实现延迟任务，**不产生新的 class 文件**，瘦 jar 类数仍为 3。
- 100 ticks（5 秒）窗口内多次卖出只排一次任务，合并为一次写盘。
- `catch (Throwable)`：序列化/IO 任何异常都只记日志，不冒泡到主线程。

### 3. 三处卖出点：saveData() → markDirty()

| 位置 | 方法 | 改后行号 | 改前 | 改后 |
|---|---|---|---|---|
| GUI 卖出 | `sellFromGui` | L517-518 | `data.set(...); saveData();` | `data.set(...); markDirty();` |
| `/ds sell` | `sellHand` | L620-621 | 同上 | 同上 |
| `/ds sellall` | `sellAll` | L653-654 | 同上 | 同上 |

内存中的 `data` 对象仍然立即 `data.set(...)`，因此同一进程内 `soldCount/earnedToday` 读取不受影响；只是把“立即写盘”降级为“5 秒内合并写盘”。

### 4. onDisable 强制落盘（L141-149）

改前（原 L132-135）：

```java
public void onDisable() {
    saveData();
}
```

改后：

```java
public void onDisable() {
    if (saveTask != null) {
        saveTask.cancel();
        saveTask = null;
    }
    if (dirty) saveData();
}
```

要点：关服时先取消还没到点的防抖任务，再把内存里脏数据强制写一次，防止 5 秒窗口内的卖出在关服瞬间丢失。

### 5. 旧键清理：cleanOldDates() + 跨天触发（L189、L194-216）

在 `rollIfNeeded` 真正跨天重抽清单的分支里，daily.yml 保存之后插入一行调用（L189）：

```java
try { daily.save(dailyFile); } catch (IOException ex) { getLogger().severe("daily.yml 保存失败"); }
// 跨天：清理 data.yml 中 7 天前的旧日期键，并立即落盘一次
cleanOldDates(now);
if (broadcast) broadcastToday();
```

新增方法（L194-216）：

```java
private void cleanOldDates(String todayStr) {
    try {
        LocalDate today = LocalDate.parse(todayStr);
        Set<String> keep = new HashSet<>();
        for (int i = 0; i < KEEP_DAYS; i++) keep.add(today.minusDays(i).toString());
        if (!data.isConfigurationSection("players")) return;
        for (String uuid : data.getConfigurationSection("players").getKeys(false)) {
            String uRoot = "players." + uuid;
            if (!data.isConfigurationSection(uRoot)) continue;
            for (String dateStr : new ArrayList<>(data.getConfigurationSection(uRoot).getKeys(false))) {
                if (!keep.contains(dateStr)) {
                    data.set(uRoot + "." + dateStr, null);   // 删除整段旧日期子树
                }
            }
        }
        saveData();   // 清理后立即落盘一次
    } catch (Throwable t) {
        getLogger().warning("清理旧日期数据失败: " + t.getMessage());
    }
}
```

要点：
- 数据结构 `players.<uuid>.<yyyy-MM-dd>.<MATERIAL>` 不变。
- 只在“跨天重抽”分支触发（同一天重启走 `loadTodayFromFile` 恢复分支，不清理）。
- 保留最近 7 天（含当天），更早的日期子树整体 `data.set(path, null)` 删除；用 `new ArrayList<>(keys)` 避免遍历中删除导致的并发修改问题。
- 清理后立即 `saveData()` 一次（每天最多一次，不在卖出热路径上）。

---

## 二、构建命令与 classpath

JDK（系统 PATH 无 jar，全路径调用）：

- `C:\Program Files\Java\jdk-21\bin\javac.exe`
- `C:\Program Files\Java\jdk-21\bin\jar.exe`
- `C:\Program Files\Java\jdk-21\bin\javap.exe`

编译 classpath（分号分隔）：

```
F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\lib\paper-api-1.21.11-R0.1-SNAPSHOT.jar
F:\game\pc\MC\开服\服务器数据备份\leaf-1.21.11\plugins\Vault.jar
F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\lib\adventure-api-4.26.1.jar
F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\lib\adventure-key-4.26.1.jar
F:\game\pc\MC\开服\Minecraft-Server-Build\自研\南瓜账本-MoneyLedger\lib\bungee-stub.jar
```

> 说明：仅 `paper-api + Vault` 不够。paper-api 传递依赖的 `net.kyori.adventure.*`（Namespaced/Keyed/Audience/Component…）与 `net.md_5.bungee.api.chat.BaseComponent` 编译期缺失，已在南瓜账本 lib 目录找到同名 jar（adventure-api 4.26.1、adventure-key 4.26.1、bungee-stub）补齐；leaf 服务端 `libraries\net\kyori` 下版本一致，运行时由服务端提供，不打入瘦 jar。

编译命令：

```powershell
& "C:\Program Files\Java\jdk-21\bin\javac.exe" -encoding UTF-8 `
  -cp "$paper;$vault;$lib\adventure-api-4.26.1.jar;$lib\adventure-key-4.26.1.jar;$lib\bungee-stub.jar" `
  -d "_build\classes" `
  "DailySell源码\src\com\hh\dailysell\DailySell.java"
```

打包（瘦 jar，复用原 MANIFEST.MF，资源从 `DailySell源码\src\` 拷入 classes 根）：

```powershell
& "C:\Program Files\Java\jdk-21\bin\jar.exe" cfm "DailySell.jar" `
  "_build\mf\META-INF\MANIFEST.MF" -C "_build\classes" .
```

---

## 三、验证结果（逐项）

1. **javac 零错误**：`exit=0`，仅一条“使用或覆盖了已过时的 API”的 deprecation 提示（历史既有，非本次引入，不影响编译/运行）。
2. **jar tf 新 jar 条目**：与原 jar 完全一致（同集合）：
   ```
   META-INF/MANIFEST.MF
   com/hh/dailysell/DailySell.class
   com/hh/dailysell/DailySell$1.class
   com/hh/dailysell/DailySell$Entry.class
   config.yml
   plugin.yml
   ```
   瘦 jar，无外部依赖打入；大小 17,443 字节（原 16,559 字节，增加约 0.9KB 为新增方法字节码）。
3. **javap 校验**：`DailySell.class` 可解析；`major version: 65`（Java 21）；`-p` 可见新增成员 `dirty`、`saveTask`、`markDirty()`、`cleanOldDates(String)`、`lambda$markDirty$1()`。
4. **生产部署**：新 jar 已复制到 `F:\game\pc\MC\开服\服务器数据备份\leaf-1.21.11\plugins\DailySell.jar`（17,443 字节，2026-10-15 22:15）。

---

## 四、备份清单

| 文件 | 备份路径 |
|---|---|
| 插件根旧 jar | `自研\每日随机收购\DailySell.jar.bak_20261015` |
| 生产旧 jar | `服务器数据备份\leaf-1.21.11\plugins\DailySell.jar.bak_20261015` |

（另有 9-30 的历史备份 `DailySell.jar.bak_20260930`，未动。）

---

## 五、部署状态

- 新 `DailySell.jar` 已同时落到插件根目录（替换旧 jar）与生产 `plugins\` 目录（替换旧 jar，旧版已备份）。
- 运行时需在服务端 `reload` 或重启后生效；首次跨天时会自动清理 7 天前的历史 `players.*.<旧日期>` 键。
