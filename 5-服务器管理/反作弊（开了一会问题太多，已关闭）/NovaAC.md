````
[12:44:52 ERROR]: Could not pass event PlayerMoveEvent to NovaAC v1.24.10java.lang.IllegalArgumentException: Cannot measure distance between world and world_the_end        at org.bukkit.Location.distanceSquared(Location.java:577) ~[leaf-api-1.21.11-R0.1-SNAPSHOT.jar:?]        at org.bukkit.Location.distance(Location.java:560) ~[leaf-api-1.21.11-R0.1-SNAPSHOT.jar:?]        at
```

搜索确认：这是 NovaAC 1.24.10 的**插件代码 bug**（跨世界移动时直接调用 `Location.distance()`，没检查世界切换），目前**没有搜到作者发布修复版本**。这类错误在 Bukkit 生态很常见，属于作者疏忽。

## 处理建议（按推荐顺序）

**1. 暂时停用 NovaAC（推荐）**

NovaAC 在你们服务器定位是"**副反作弊 / 载具坐骑补位**"（文档里本来就是"待手动下载"刚装上的），而：
- **GrimAC** 已覆盖 Java 玩家移动/飞行/速度
- **Themis** 已覆盖基岩玩家移动/船/鞘翅

停用方法：`plugins/NovaAC.jar` 改名为 `NovaAC.jar.disabled` → 重启服务器（或看有没有 NovaAC 的 disable 命令）。日志刷屏立即消失，检测主体不受影响。

**2. 留意 NovaAC 更新**

作者在 SpigotMC/CurseForge 发布（作者 Discord 在资源页），这类"跨世界 distance"bug 是作者大概率会修的常见问题——发现新版本再升级。

**3. 反馈作者**

在 NovaAC 的 SpigotMC Discussion 贴这个堆栈（`Location.distanceSquared` + 两个世界名），作者修起来很快。

## 影响范围说明

- 只在玩家**跨世界移动瞬间**（进/出末地传送门 `world ↔ world_the_end`）触发
- 同一世界内移动不受影响
- 危害 = 控制台刷屏 + 异常后 NovaAC 对"上一个位置"状态没更新（可能连锁误判/漏判），**不会直接导致"三叉戟不能起飞"**（那是 Themis 拦截）

---

小结一下你们服务器当前三个反作弊的问题：**Themis 误拦截基岩玩家正常飞行**（Score 10 卡阈值）+ **NovaAC 跨世界报错**（代码 bug）。我的建议是：NovaAC 先停用，Themis 按上一条回复改 `block: false` 观察，GrimAC 等 `/grim verbose on` 复现挖方块问题。

需要我把这三个处置（Themis 误判案例 + NovaAC bug + 待办）一起写进反作弊文档吗？
````
