# 3. Java-Bedrock 互通层

本页是 Java-Bedrock 互通层的目录总览，用于说明双端玩家同服游玩的技术原理与组件分工，供服主和管理员在搭建或排障时定位相关子文档。下方表格列出 Geyser 与 Floodgate 的部署位置与职责，其后附有基岩玩家从 UDP 55551（与 Java 端口一致，Geyser `clone-remote-port: true` 跟随）进入到 Leaf 服务端的完整认证与翻译流程图。

这是实现 Java 版与基岩版玩家**在同一个服务器中游玩**的核心技术栈，由两个配合使用的插件组成：

| 插件 | 文件 | 说明 |
|---|---|---|
| [Geyser-Spigot](01-Geyser-Spigot.md) | `plugins/Geyser-Spigot.jar` | 协议转换引擎，将基岩版协议翻译为 Java 版协议；**装在 Leaf 后端 `plugins/` 下**（本服无 Velocity 代理层） |
| [Floodgate](02-Floodgate.md) | 后端 `plugins/floodgate-spigot.jar` (11.03 MB) | 基岩版认证插件，允许基岩版玩家绕过 Java 正版验证 |

### 工作流程

```
基岩版客户端 (手机/Win10/Xbox/Switch/PS)
  │  UDP 55551（与 Java 端口一致）
  ▼
Geyser 协议转换引擎
  │  将 Bedrock 协议 → Java 协议
  │  基岩版玩家被标记 "." 前缀（如 .Steve）
  ▼
Floodgate 认证
  │  使用 key.pem 密钥对验证合法性
  │  允许基岩版玩家绕过 Mojang 正版验证
  ▼
Leaf 服务端 (online-mode=true)
  │  Java 玩家走正常 Mojang/Yggdrasil 认证
  │  基岩版玩家由 Floodgate 认证通过
  ▼
Java + Bedrock 玩家同服游玩
```
