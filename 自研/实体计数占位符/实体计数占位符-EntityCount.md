# 实体计数占位符 — EntityCount

自研 PAPI 占位符插件，解决官方 Server 扩展在异步刷新下触发主线程检查报错的问题。

| 项目 | 内容 |
|---|---|
| 插件名 | EntityCount |
| 版本 | 1.1.0 |
| 类型 | PlaceholderAPI 扩展（随插件注册） |
| 依赖 | PlaceholderAPI（硬依赖）、Paper 系服务端（Leaf） |
| 适用版本 | MC 1.20+（`api-version: 1.20`） |
| Java | 17 |
| 产物 | `target/EntityCount-1.1.0.jar` |
| 源码 | `src/main/java/com/mc/entitycount/EntityCountPlugin.java` |

---

## 一、解决的问题

PAPI 官方 Server 扩展的 `%server_total_entities%` / `%server_total_living_entities%` 在**每次请求时同步遍历 chunk 取实体**。而 TAB 等插件在**异步线程**（`TAB Placeholder Refreshing Thread`）刷新占位符，Leaf/Paper 的 `AsyncCatcher` 会直接拦下 `Chunk getEntities` 调用，导致：

- 生物数取不到、公告栏/TAB 显示为空；
- 控制台周期性刷屏：

```
ERROR]: Thread TAB Placeholder Refreshing Thread failed main thread check: Chunk getEntities call
```

本插件把「统计」和「读取」解耦：**主线程低频统计 + 内存缓存，占位符请求只读缓存**，彻底绕开异步主线程检查。

---

## 二、占位符一览

| 占位符 | 含义 | 数据来源 |
|---|---|---|
| `%entitycount_count%` | 全部已加载实体数（含物品掉落物、箭等） | `World.getEntityCount()`，O(1) |
| `%entitycount_living%` | 生物数（玩家 + 怪物 + 动物等） | 主线程一次遍历（`LivingEntity` 实例） |
| `%entitycount_items%` | 掉落物数（地面物品实体 `minecraft:item`） | 主线程一次遍历（`Item` 实例） |

- 统计范围为**全服所有已加载世界**（主世界/下界/末地等，`Bukkit.getWorlds()` 遍历求和）。
- 数值为**缓存值**，最长滞后 5 秒刷新一次，非实时。

### 口径关系（重要：生物 ⊂ 实体）

三个占位符**不是并列**的，`living`（生物）和 `items`（掉落物）都是 `count`（实体）的**子集**：

- **实体 `count`** = 所有已加载实体 = 生物 `living` + 掉落物 `items` + 其他（箭 / 经验球 / 船 / 矿车 / 盔甲架 / 展示框 / 画等）。
- **生物 `living` ⊂ 实体 `count`**，**掉落物 `items` ⊂ 实体 `count`**；且 `living` 与 `items` 之间**互斥**（一只实体要么是生物、要么不是，不会被同时计入两个数）。

因此使用时要注意：

| 想得到 | 正确算法 | 反例 |
|---|---|---|
| 实体总数 | 直接用 `%entitycount_count%` | — |
| 非生物实体 | `count − living`（含掉落物等） | — |
| 精确拆分 | `count = living + items + 其他非生物非掉落物` | — |
| 别这么做 | — | ❌ 把 `living` 和 `count` 相加——`count` 已包含 `living`，相加必然重复计 |

**举例**：`count=800、living=350、items=420` → 生物 350 + 掉落物 420 = 770，其余 30 是箭/经验球/盔甲架等；生物和掉落物都在这 800 之内，三者加起来并不等于 1700。

---

## 三、工作原理

```
Bukkit.getScheduler().runTaskTimer(this, this::refresh, 20L, 100L)
```

- 启动 1 秒（20 tick）后开始，之后**每 5 秒**（100 tick）在主线程执行一次 `refresh()`；
- `refresh()` 遍历所有世界：`getEntityCount()` O(1) 取实体总数，**一次 `getEntities()` 遍历同时统计生物（`LivingEntity`）与掉落物（`Item`）**，写入三个 `volatile int` 缓存字段；
- 占位符请求（可能来自异步线程）**只读取缓存值**，不触碰任何 Bukkit API，异步安全零开销；
- `volatile` 保证主线程写入、异步线程读取的可见性。

**设计取舍**：

| 方案 | 问题 |
|---|---|
| 请求时实时统计（官方 Server 扩展做法） | 异步线程调 Bukkit API 被 AsyncCatcher 拦截 → 报错刷屏 |
| 本插件：定时统计 + 缓存 | 读取零开销、异步安全；代价是数值最长滞后 5 秒（显示用途完全可接受） |

---

## 四、安装与部署

1. 构建 jar（见「七、构建」）或直接使用 `target/EntityCount-1.1.0.jar`；
2. 将 jar 放入服务端 `plugins/` 目录；
3. 确认已安装 PlaceholderAPI（硬依赖，缺失则插件不加载）；
4. 重启服务器或 `/reload confirm`（建议重启）。

无需任何配置文件。

---

## 五、使用示例

### 公告栏（TAB Scoreboard）

```
- '&7▸ 生物: &a%entitycount_living%'
- '&7▸ 实体: &a%entitycount_count%'
- '&7▸ 掉落物: &a%entitycount_items%'
```

### TAB 配置

```yaml
customtabname: "%entitycount_living% 只生物在线"
```

### 其他插件（任意支持 PAPI 的位置）

```
当前服务器已加载实体：%entitycount_count%
```

---

## 六、常见问题

| 问题 | 说明 |
|---|---|
| 数值不变/更新慢 | 正常。缓存 5 秒刷新一次，极端大服遍历实体可能略耗时，属预期行为 |
| 占位符显示原样字符串 | 检查 PlaceholderAPI 是否加载、`/papi list` 中是否存在 `entitycount` 扩展（注册时插件名 `EntityCount`） |
| 生物数比实体数少很多 | 正常。`count` 含掉落物/投射物等非生物实体，`living` 仅统计生物（口径关系见「二、占位符一览 → 口径关系」） |
| 掉落物数是 0 | 检查统计逻辑是否生效；掉落物为地面 `minecraft:item` 实体，箱子内物品/物品展示框不计入 |
| 为什么不用 `%server_total_entities%` | 该占位符在异步刷新下触发 `Chunk getEntities` 主线程检查报错，即本插件存在的意义 |

---

## 七、构建

```bash
mvn clean package
```

- 依赖：`paper-api 1.20.1-R0.1-SNAPSHOT`、`placeholderapi 2.11.6`（均 provided，不打包进 jar）；
- 产物：`target/EntityCount-1.1.0.jar`。

---

## 八、源码结构

| 文件 | 职责 |
|---|---|
| `EntityCountPlugin.java` | 插件主类：定时统计任务 + PAPI 扩展注册 |
| `EntityCountExpansion`（内部类） | PlaceholderExpansion 实现：`entitycount` 标识，`onRequest` 分发 `count` / `living` / `items` |

**关键代码片段**：

```java
// 主线程每 5 秒统计一次并缓存
Bukkit.getScheduler().runTaskTimer(this, this::refresh, 20L, 100L);

private void refresh() {
    int entities = 0, living = 0, items = 0;
    for (World world : Bukkit.getWorlds()) {
        entities += world.getEntityCount();              // O(1)
        for (Entity e : world.getEntities()) {           // 一次遍历统计生物 + 掉落物
            if (e instanceof LivingEntity) living++;
            else if (e instanceof Item) items++;
        }
    }
    entityCount = entities;
    livingCount = living;
    itemCount = items;
}
```
