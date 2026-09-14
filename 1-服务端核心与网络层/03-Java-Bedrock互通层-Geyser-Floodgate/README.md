# 3. Java-Bedrock 互通层

这是实现 Java 版与基岩版玩家**在同一个服务器中游玩**的核心技术栈，由两个配合使用的插件组成：

| 插件 | 文件 | 说明 |
|---|---|---|
| [Geyser-Spigot](01-Geyser-Spigot.md) | `plugins/Geyser-Spigot.jar` (18.26 MB) | 协议转换引擎，将基岩版协议翻译为 Java 版协议 |
| [Floodgate](02-Floodgate.md) | `plugins/floodgate-spigot.jar` (11.03 MB) | 基岩版认证插件，允许基岩版玩家绕过 Java 正版验证 |

### 工作流程

```
基岩版客户端 (手机/Win10/Xbox/Switch/PS)
  │  UDP 19132
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
