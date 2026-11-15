# 3. Java-Bedrock 互通层

本页是 Java-Bedrock 互通层的目录总览，用于说明双端玩家同服游玩的技术原理与组件分工，供服主和管理员在搭建或排障时定位相关子文档。下方表格列出 Geyser 与 Floodgate 的部署位置与职责，其后附有基岩玩家从 UDP 55551（与 Java 端口一致，Geyser `clone-remote-port: true` 跟随）进入到 Leaf 服务端的完整认证与翻译流程图。

这是实现 Java 版与基岩版玩家**在同一个服务器中游玩**的核心技术栈，由两个配合使用的插件组成：

| 插件 | 文件 | 说明 |
|---|---|---|
| [Geyser-Spigot](01-Geyser-Spigot.md) | `plugins/Geyser-Spigot.jar` | 协议转换引擎，将基岩版协议翻译为 Java 版协议；**装在 Leaf 后端 `plugins/` 下**（本服无 Velocity 代理层） |
| [Floodgate](02-Floodgate.md) | 后端 `plugins/floodgate-spigot.jar` (11.03 MB) | 基岩版认证插件，允许基岩版玩家绕过 Java 正版验证 |

> **皮肤链路现状**：BedrockSkinRestorer 与自研 BedrockSkinRecorder **均已停用**（后者归档在 `自研/disabled/`）。基岩玩家皮肤由 Geyser 在线透传，Java 端皮肤由 SkinsRestorer 与外置登录 session 提供；详见 [3.3 基岩皮肤与皮肤头显示异常排查](03-基岩皮肤与皮肤头显示异常排查.md)。


## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | `plugins/Geyser-Spigot.jar`（`2.12.1-SNAPSHOT`）+ `plugins/floodgate-spigot.jar`（`2.2.5-SNAPSHOT (b141)`） |
| 官方最新 | 官方下载页最新 Build #1302（2026-11-12，Java 26.3 / Bedrock 26.40-26.63）；官网持续更新，当前按官方文档口径 |
| 官方文档站 | <https://geysermc.org/wiki/geyser/>（Geyser）· <https://geysermc.org/wiki/floodgate/>（Floodgate）；旧 `wiki.geysermc.org` 已迁到 `geysermc.org/wiki/` |
| 官方下载 | <https://geysermc.org/download> |
| 主要页面 | [Geyser 安装](https://geysermc.org/wiki/geyser/setup) · [Geyser](https://geysermc.org/wiki/geyser/) · [Floodgate](https://geysermc.org/wiki/floodgate/) · [其他项目](https://geysermc.org/wiki/other/) · [REST API](https://geysermc.org/wiki/api/) |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方文档复核。

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
