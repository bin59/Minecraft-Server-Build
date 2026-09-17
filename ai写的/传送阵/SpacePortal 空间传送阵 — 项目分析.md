# SpacePortal 空间传送阵 — 项目分析

本文档是对 SpacePortal 空间传送阵插件源码项目的结构分析，面向打算二次开发或维护该插件的开发者。内容涵盖项目分层架构与模块划分、充能传送核心流程、设计亮点、潜在改进点与 Maven 构建方式。

### 项目概览

这是一个 **Minecraft Spigot/Paper 服务器插件**（v1.0.0），为 Java/基岩互通服（Geyser+Floodgate）提供带大型粒子特效的两点传送功能。编译目标 Java 17，API 版本 1.20。

### 架构与模块划分

项目采用清晰的分层结构，共 8 个 Java 文件，分为 4 个包：

| 包                  | 类                                           | 职责                                                           |
| ------------------- | -------------------------------------------- | -------------------------------------------------------------- |
| `SpacePortalPlugin` | 主类                                         | 生命周期管理、配置加载、组件初始化                             |
| `portal`            | `Portal` / `PortalSession` / `PortalManager` | 数据模型、充能会话状态、YAML 持久化与连接关系                  |
| `teleport`          | `TeleportService`                            | 核心业务：充能计时、钻石扣费、传送执行、冷却、时光穿梭特效调度 |
| `listener`          | `PortalListener`                             | 移动事件监听、进入/离开阵法检测、边界触发去重                  |
| `command`           | `PortalCommand`                              | `/spaceportal`（别名 `/csz`）指令解析、Tab 补全、权限校验      |
| `effect`            | `EffectController` / `PortalEffectTask`      | 纯数学粒子渲染（地面法阵、充能螺旋、传送爆发、时光隧道）       |

### 核心流程

1. **玩家跨格移动** → `PortalListener.onMove` 检测是否进入某个阵法的激活半径（水平 2.2 格、垂直 2.5 格）
2. **进入阵法** → `TeleportService.tryStartCharge` 校验权限/冷却/钻石/目的地连接 → 创建 `PortalSession`，启动每 tick 定时器
3. **充能阶段**（默认 60 tick = 3 秒）→ 动作栏显示进度、音调渐升音效；玩家移出范围或死亡/离线则取消
4. **充能完成** → 二次确认钻石 → 扣除 → 延迟 6 tick 后执行 `player.teleport` → 起点收缩漩涡 + 终点爆发粒子 + 末影音效
5. **落地时光穿梭** → 双螺旋彩虹粒子流环绕玩家身体，持续 50 tick，水晶音效收尾
6. **冷却** → 5 秒内不再触发

### 亮点与设计优点

- **零 NMS 依赖**：所有粒子和音效走 Bukkit 标准 API，Java/基岩双端兼容
- **性能意识**：`PortalEffectTask` 只在 40 格内有玩家时才渲染；移动事件仅跨格时处理；充能期间每 10 tick 才刷新 ActionBar
- **防刷机制**：进入状态用 `inside` Map 去重，避免移动事件重复触发；传送后清理状态防止落地即触发
- **费用安全**：钻石在传送前一刻才扣除，余额不足直接提示不扣费；充能完成时再次校验
- **配置热重载**：`/csz reload` 即时生效，参数有合理下限保护
- **数据完整性**：删除阵法时自动清理指向它的连接

### 潜在改进点

1. **并发安全**：`sessions`/`cooldownUntil`/`inside` 均为普通 HashMap，虽然 Bukkit 主线程单线程调度通常没问题，但如果未来接入异步操作需注意
2. **粒子开销**：每帧地面法阵约生成 100+ 粒子包，多个阵法同时可见时可能影响低配客户端；可考虑按距离 LOD 降级
3. **缺少 Metrics/bStats**：没有匿名统计，不利于了解使用情况
4. **无国际化**：消息硬编码中文，如需多语言支持可抽取到 lang 文件
5. **测试缺失**：没有单元测试，核心逻辑（如钻石扣除、范围判定）适合补充测试
6. **`removeDiamonds` 未处理副手**：只扫描 `StorageContents`，如果玩家把钻石放在副手槽不会被计入（不过 `countDiamonds` 也同样不计，行为一致）

### 构建方式

项目自带 Maven 3.9.9（`tools/apache-maven-3.9.9/`），构建命令：

```
tools\apache-maven-3.9.9\bin\mvn.cmd -f SpacePortal\pom.xml package
```

产物为 `SpacePortal/target/SpacePortal-1.0.0.jar`。

整体来看这是一个结构清晰、完成度较高的 MC 插件项目，代码注释充分，特效实现精致，适合直接使用或在此基础上二次开发。
