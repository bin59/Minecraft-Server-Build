# 9. 协议库 — ProtocolLib

> **一句话**：ProtocolLib 是服务端的**数据包拦截与修改中间层**，让插件能监听、改写、取消玩家与服务器之间的网络包。它本身**不面向玩家提供任何功能**，而是作为其他插件的底层依赖存在。
>
> **本服版本**：5.4.0（jar：`ProtocolLib.jar`）
>
> **归档位置**：`1-服务端核心与网络层/09-协议库-ProtocolLib/`（2026-10-05 由 `2-运维监控与面板/07-协议库-ProtocolLib/` 移入；归类是按"它工作在 Netty 网络层"而非"它是运维面板"）
>
> ⚠️ **版本兼容性提醒（2026-10-05 校准）**：5.4.0 是官方"最后支持 Java 8"的版本，发布时主要覆盖 1.20.5/6 与 1.21，**未标记支持 1.21.11**。在 1.21.11 上启动会输出
> `[ProtocolLib] Version (MC: 1.21.11) has not yet been tested! Proceed with caution.`
> 1.21.11 的支持是在 **5.5.0-SNAPSHOT** 中加入的（`add support for 1.21.11` #3578 / `Mark 1.21.11 as supported` #3589），建议升级到 **5.5.0 或更高**。详见 §六。

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
- **启动日志 `Version (MC: 1.21.11) has not yet been tested! Proceed with caution.`**：ProtocolLib 的版本自检提示，说明**该 jar 未标记支持当前 MC 版本**——本服 5.4.0 就属此列。它只是警告，**不代表已经坏了**（ProtocolLib 靠反射 + 结构修改器工作，字段没大变时多数包照常跑）。判断要不要管，看后面有没有 `NoSuchMethodError` / `NoSuchFieldError` 之类真实异常；没有则可暂时不动，但**依赖它的 AntiLitematica 检测可能漏判**，建议升级。两种升级方式：
  1. **自动更新**：`plugins/ProtocolLib/config.yml` 把 `global.auto updater.download` 改 `true`（本服默认 `false`），重启后自行拉取最新版。
  2. **手动替换**：从 [GitHub Releases](https://github.com/dmulloy2/ProtocolLib/releases) 或 [SpigotMC](https://www.spigotmc.org/resources/protocollib.1997/) 下载 **≥ 5.5.0**，替换 jar 后**完整重启**（不能 `/reload`）。dev build 已从 Jenkins 迁到 GitHub。
  升级后验证：`/protocol lib` 看版本号、`/protocol verify` 校验字段映射、`/protocol list` 确认 AntiLitematica 等仍在挂钩。
  > 5.5.0+ 要求 **Java 17+**（本服 JDK 21，满足）。
- **新版本 Minecraft 出来后插件报 NoSuchFieldError**：等 ProtocolLib 更新结构修改器；临时可开 `background compiler: true`（本服已开）让它自动适配。

---

## 七、与文档体系链接

- 上级目录：[1-服务端核心与网络层](../)
- 兄弟章节：[04-跨版本协议兼容-ViaVersion](../04-%E8%B7%A8%E7%89%88%E6%9C%AC%E5%8D%8F%E8%AE%AE%E5%85%BC%E5%AE%B9-ViaVersion/ViaVersion.md)、[05-端口与网络架构总览](../05-%E7%AB%AF%E5%8F%A3%E4%B8%8E%E7%BD%91%E7%BB%9C%E6%9E%B6%E6%9E%84%E6%80%BB%E8%A7%88/README.md)、[08-服务器资源包-BGM](../08-%E6%9C%8D%E5%8A%A1%E5%99%A8%E8%B5%84%E6%BA%90%E5%8C%85-BGM/%E9%83%A8%E7%BD%B2%E4%B8%8E%E9%85%8D%E7%BD%AE%E8%AF%B4%E6%98%8E.md)
- 相关：[04-核心依赖库-CMILib](../../2-%E8%BF%90%E7%BB%B4%E7%9B%91%E6%8E%A7%E4%B8%8E%E9%9D%A2%E6%9D%BF/04-%E6%A0%B8%E5%BF%83%E4%BE%9D%E8%B5%96%E5%BA%93-CMILib/CMILib.md)（同为无玩家交互的底层库，留在 2-运维监控与面板）
- 被依赖方：AntiLitematica 文档（`6-工具与常见问题约束/03-禁用影响平衡的插件功能/AntiLitematica-.../`）
- 官方：https://github.com/dmulloy2/ProtocolLib
