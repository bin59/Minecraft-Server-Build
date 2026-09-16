# 7. 协议库 — ProtocolLib

> **一句话**：ProtocolLib 是服务端的**数据包拦截与修改中间层**，让插件能监听、改写、取消玩家与服务器之间的网络包。它本身**不面向玩家提供任何功能**，而是作为其他插件的底层依赖存在。
>
> **本服版本**：5.4.0（jar：`ProtocolLib.jar`）

---

## 一、简介

ProtocolLib 在 Netty 网络层与 Bukkit 事件系统之间架了一座桥：

- **监听入站包**（玩家→服务器）：如 AntiLitematica 通过它检测 Litematica 的 Servux 同步包、异常命中向量、NBT 查询。
- **拦截出站包**（服务器→玩家）：如皮肤/HUD 修改、假方块放置、反作弊的数据包级校验。
- **结构修改器（Structure Modifier）**：自动为各 Minecraft 版本编译字段映射，使插件代码无需关心版本差异。

> ⚠️ ProtocolLib 是**库**，不是功能插件。装它是为了让别的插件能跑；普通玩家在游戏里感知不到它的存在。

---

## 二、本服哪些插件依赖它

| 插件 | 用 ProtocolLib 做什么 |
|---|---|
| **AntiLitematica 7.0.1** | 拦截 `servux:litematics` 通道包、检测 EasyPlace 异常向量、NBT 查询信号（config.yml `detection.signals`） |
| **EssentialsX 2.22.1** | 部分数据包级功能（如假方块、背包查看底层） |
| **CoreProtect 24.0** | 方块改动的数据包级记录 |
| **DecentHolograms 2.10.1** | 全息实体的数据包级渲染（ ArmorStand 替换为数据包实体） |
| **Residence 6.0.0.1** | 领地 flag 的数据包级交互检测 |

> 装 ProtocolLib 后，这些插件启动时会自动加载其 hook，无需手动配置。

---

## 三、安装

1. 下载与服务端版本（Paper 1.21.x）兼容的 `ProtocolLib.jar` 放入 `plugins/`。
2. 重启服务器，自动生成 `plugins/ProtocolLib/config.yml`。
3. 启动日志出现 `[ProtocolLib] Loading ProtocolLib v5.4.0` 即成功。

---

## 四、配置要点（plugins/ProtocolLib/config.yml 实际值）

```yaml
global:
  auto updater:
    notify: true        # 启动时在控制台提示有新版本
    download: false     # 不自动下载（避免重启后版本漂移）
    delay: 43200        # 检查间隔 12 小时
  metrics: true         # bStats 匿名统计
  chat warnings: true   # 把 warning 发到带 protocol.info 权限的玩家聊天框
  background compiler: true   # 后台异步编译结构修改器
  debug: false          # 数据包过滤调试
  detailed error: false # 打印完整堆栈（排障时可开）
  script engine: JavaScript  # /protocol filter 的脚本引擎
```

> 本服**未做任何自定义修改**，全部为默认值。日常无需动 config.yml。

---

## 五、命令与权限

| 命令 | 作用 | 权限 |
|---|---|---|
| `/protocol` | 打开主菜单（列出已挂钩的插件） | `protocol.admin`（默认 OP） |
| `/protocol list` | 列出已挂钩插件 | `protocol.admin` |
| `/protocol lib` | 显示 ProtocolLib 自身版本与加载状态 | `protocol.admin` |
| `/protocol dump` | 生成诊断 dump（报障时用） | `protocol.admin` |
| `/protocol verify` | 校验数据包字段映射是否正确 | `protocol.admin` |
| `/protocol reload` | 重载 config.yml | `protocol.admin` |
| `/protocol filter <脚本>` | 用 JS 脚本过滤数据包（高级排障） | `protocol.admin` |
| `protocol.info` | 接收数据包警告聊天通知 | 默认 OP |

> 普通玩家**无任何命令与权限**。

---

## 六、常见问题

- **启动报错 `Cannot access net.minecraft.server`**：ProtocolLib 版本与服务端不兼容。换与 Paper 1.21.x 匹配的构建。
- **AntiLitematica 没检测到 Litematica**：确认 ProtocolLib 已加载（`/protocol lib`），且 AntiLitematica 的 `detection.signals` 三项均为 `enabled: true`。
- **性能下降**：ProtocolLib 本身开销极低；若某个挂钩它的插件在高频包上做了重活，瓶颈在那个插件而非 ProtocolLib 本体。用 `/protocol list` 看哪个插件注册了监听器。
- **新版本 Minecraft 出来后插件报 NoSuchFieldError**：等 ProtocolLib 更新结构修改器；临时可开 `background compiler: true`（本服已开）让它自动适配。

---

## 七、与文档体系链接

- 上级目录：[../](../)（2-运维监控与面板）
- 兄弟章节：[04-核心依赖库-CMILib](../04-%E6%A0%B8%E5%BF%83%E4%BE%9D%E8%B5%96%E5%BA%93-CMILib/CMILib.md)、[03-性能分析-spark](../03-%E6%80%A7%E8%83%BD%E5%88%86%E6%9E%90-spark/spark.md)
- 被依赖方：AntiLitematica 文档（`7-工具与常见问题约束/03-禁用影响平衡的插件功能/AntiLitematica-.../`）
- 官方：https://github.com/dmulloy2/ProtocolLib
