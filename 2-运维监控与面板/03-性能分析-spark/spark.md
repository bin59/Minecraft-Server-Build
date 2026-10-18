# 13. 性能分析 — spark

spark 是 LuckPerms 作者开发的高性能服务器性能分析工具，实时监控 CPU、TPS、内存，profiler 生成报告定位卡顿。本文给出关键配置与常用命令。

**文件**: 内置于 `plugins/spark/` 目录 ｜ **官网**: https://spark.lucko.me

## 关键配置 (`plugins/spark/config.json`)

```json
{
  "backgroundProfiler": true    // 启用后台性能分析器
}
```

## 常用命令

| 命令 | 说明 |
|---|---|
| `/spark` | 查看基本信息和使用率 |
| `/spark health` | 健康报告 |
| `/spark tps` | 查看 TPS |
| `/spark profiler start` | 开始性能分析 |
| `/spark profiler stop` | 停止并获取分析报告链接 |
| `/spark profiler open` | 打开最近的分析报告 |
| `/spark gc` | 查看 GC 信息 |
| `/spark ping` | 查看玩家延迟 |

---

## 用 spark 看「网络线程 CPU」(排查 java.exe 高上行 / 基岩崩断 / 记分板风暴)

> **一句话结论**：spark 是**量 CPU 时间占比**的采样器，**不是抓包工具**——它告诉你「哪条线程在烧 CPU」（比如 Geyser/Netty/TAB 线程爆了），但**不给你带宽、不给你具体包内容**。要看带宽占用和具体流量，得配合资源监视器 / Plan / Wireshark（见文末）。

### 1. 为什么默认采样看不到网络线程

默认 `/spark profiler start` 只采样**主线程（server thread）**，也就是「游戏逻辑」线程。网络收发在别的线程里跑，默认根本不进火焰图，所以**看网络必须加 `--thread '*'`** 全线程采样。

### 2. 正确的启动 / 停止命令

```text
# 全线程采样 120 秒（含 Netty / Geyser / WebRTC / TAB 线程）
/spark profiler start --thread '*' --timeout 120

# 只看 TPS 掉到 5 以下时的样本（过滤掉空闲期，报告更聚焦）
/spark profiler start --thread '*' --only-ticks-over 5 --timeout 120

# 停止并拿到报告链接
/spark profiler stop

# 重新打开最近一次报告
/spark profiler open
```

- `--timeout 120`：采样 2 分钟，足够覆盖一个「玩家进服 / 崩断 / 重连」的完整周期。
- 采样期间**让目标玩家复现问题**（比如让那两个进不去的基岩玩家连一次），报告才有意义。

### 3. 报告里怎么切到「网络线程」视图

打开 spark.lucko.me 报告页后，默认按「By Class / Method」聚合，**看不到单线程占比**。必须：

1. 顶部把 **Group by** 从 `Class` / `Method` 切到 **`Thread`**（或页面里的 "Group by Threads" 开关）。
2. 左侧线程列表里找网络相关线程（名字见下表），点开看它的**自身 CPU %** 和**自顶向下火焰图**。
3. 火焰图里热点方法名即罪魁（例子：`me.neznamy.tab` → TAB 风暴；`org.cloudburstmc.netty` → Geyser 发包；`tel.schich.libdatachannel` → WebRTC 数据通道）。

### 4. 网络相关线程名清单

| 线程名（含关键词） | 归属 | 干什么 |
|---|---|---|
| `Netty Server IO #N` / `Netty Epoll Server IO #N` | 服务端主网络（Java 玩家 + 插件） | 处理 Java 版连接、插件网络 |
| `multiThreadIoEventLoopGroup-N-N` / `Geyser ... IO` | Geyser | 基岩版上下行、RakNet/WebRTC 媒体收发 |
| `tel.schich.libdatachannel.*` / `LibDataChannel` / `webrtc` | Geyser-WebRTC(NetherNet) | WebRTC 数据通道、SCTP、ICE |
| `TAB` / `TAB Thread` / `me.neznamy.tab` | TAB 插件 | 计分板 / 标签页刷新发包 |
| `Netty Client IO` | 客户端式连接（如 Bungee/Velocity、部分跨服插件） | 出站连接 |

### 5. CPU 占比怎么判读（经验阈值）

- **正常**：单条网络线程 CPU 长期 < 5–10%，峰值偶发 < 20%。
- **异常**：某条网络线程持续 **20–40%+**，甚至单核打满 → 这条线程在「烧 CPU 地发包/收包」。
  - `TAB` 线程高 → 记分板/占位符刷新太频繁（查 `TAB/config.yml` 的 `default-refresh-interval`，建议 ≥ 2000ms，别用 500ms）。
  - `Geyser` / `libdatachannel` 线程高且伴随 SCTP `errno=108`、ICE `errno=10051` → WebRTC(NetherNet) 数据通道对该玩家网络不稳定，连上数秒必崩（本案两个基岩玩家的根因）。
  - `Netty` 线程高 → 大量 Java 玩家或插件在高频收发。

> 注意：spark 看到的是「CPU 忙不忙」，**不代表流量大**。一个线程 CPU 5% 也可能在悄悄跑满带宽（纯转发、不怎么算）。CPU 高只是「有活干」，要结合下文资源监视器看带宽。

### 6. 配合其他工具：spark 量 CPU，别的量带宽/内容

| 现象 | 工具 | 看什么 |
|---|---|---|
| 某线程 CPU 爆 | **spark**（本文） | 哪条线程、哪个方法在烧 CPU |
| java.exe 上行/下行带宽大 | **Windows 资源监视器**（`resmon` → 网络 → 按进程排） | 确认是 `java.exe` 在吃带宽、上行还是下行 |
| 单玩家贡献了多少流量 | **Plan 插件**（`/plan` 玩家页 → 流量/会话） | 哪个玩家发包异常多 |
| 具体是什么包、哪个端口 | **Wireshark**（本地抓 `udp port 19132 or 19133`） | 抓基岩 RakNet/WebRTC 媒体包内容、重传率 |
| 玩家延迟 / 抖动 | `/spark ping` | 单玩家 RTT |

### 7. 一张图串起「网络占用大」的完整排查路径

```text
java.exe 带宽高（资源监视器确认）
        │
        ├─ 是不是某个玩家在刷？ ── Plan 单玩家流量 ── 踢/限速
        │
        ├─ 是不是 TAB 风暴？ ── spark --thread '*' → TAB 线程 CPU ── 调 default-refresh-interval
        │
        ├─ 是不是 Geyser/WebRTC 崩断重连循环？ ── 日志 SCTP errno=108 / ICE 10051
        │       └─ transport: both 给 RakNet(19132) 退路 + 放行 UDP 19133
        │
        └─ 具体包内容 / 重传？ ── Wireshark 抓 19132/19133
```

### 8. 关键记忆点（别踩坑）

- **spark 不是抓包工具**，别指望它给你带宽或包内容——它是 CPU 采样器。
- **看网络必须 `--thread '*'`**，否则网络线程根本不进报告。
- 报告里**务必切到 Group by Thread**，默认视图看不到单线程。
- WebSocket/WebRTC 线程崩断（SCTP 108、ICE 10051）是「连接建立即断」，spark 只能看到对应线程 CPU 尖刺 + 日志报错，真因要靠日志和 Wireshark。
- 基岩 `transport: both` 方案：19132 仍给正常玩家（RakNet + builtin 信令），19133 给 NetherNet(WebRTC) 媒体；正常玩家无感自动切换，仅云端安全组需放行 UDP 19133。
