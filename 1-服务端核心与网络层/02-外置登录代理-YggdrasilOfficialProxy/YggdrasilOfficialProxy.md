# 2. 外置登录代理 — YggdrasilOfficialProxy

**主文件**: `YggdrasilOfficialProxy-2.3.0-paperclip.jar` (17.09 MB)  
**核心引擎**: `authlib-injector-1.2.7.jar` (336 KB)

## 功能说明

YggdrasilOfficialProxy 是一个 Java Agent，通过修改服务端字节码，为服务器提供**外置登录（Yggdrasil 认证）**支持。它允许服务器使用第三方的 Yggdrasil 认证服务器来验证玩家身份，而不是仅依赖 Mojang 官方认证服务器。

## 架构组成

YggdrasilOfficialProxy 本质上是 **authlib-injector** 的封装增强版，两者协同工作：

| 文件 | 大小 | 角色 |
|---|---|---|
| `YggdrasilOfficialProxy-2.3.0-paperclip.jar` | 17.09 MB | 外层 Java Agent 封装，通过 `-javaagent` 加载 |
| `authlib-injector-1.2.7.jar` | 336 KB | **核心字节码注入引擎**，负责实际拦截和修改认证相关类 |

### authlib-injector 注入日志（`authlib-injector.log`）

```
[authlib-injector] [INFO] Version: 1.2.7
[authlib-injector] [INFO] Authentication server: http://localhost:32217
[authlib-injector] [INFO] Httpd is running on port 14502
```

> authlib-injector 在本地启动了一个 HTTP 代理服务（端口 `14502`），将 Mojang 认证请求转发到第三方 Yggdrasil 认证服务器（`localhost:32217`）。

### 被注入修改的类

从日志可见，authlib-injector 在运行时注入了以下类：

| 目标类 | 注入方式 | 目的 |
|---|---|---|
| `com.mojang.authlib.properties.Property` | Yggdrasil Public Key Transformer | 替换 Mojang 公钥 |
| `com.mojang.authlib.HttpAuthenticationService` | ConcatenateURL Workaround | 修正认证 URL 拼接 |
| `com.mojang.authlib.yggdrasil.YggdrasilEnvironment` | Constant URL Transformer | 替换认证服务器地址 |
| `net.minecraft.server.network.ServerLoginPacketListenerImpl` | Username Check Transformer | 兼容中文等特殊字符用户名 |
| `net.skinsrestorer.shared.connections.MojangAPIImpl` | Constant URL Transformer | 皮肤 API 走外置认证 |
| `org.geysermc.floodgate.util.MojangUtils` | Constant URL Transformer | Floodgate 基岩版认证兼容 |
| `org.geysermc.geyser.skin.SkinProvider` | Constant URL Transformer | Geyser 皮肤加载走外置认证 |

> 这意味着 **SkinsRestorer、Geyser（皮肤）、Floodgate** 的认证和皮肤加载链路全部经过 authlib-injector 拦截，确保在 online-mode=true 下所有组件正常工作。

## 使用方式

在启动参数中通过 `-javaagent` 加载：
```batch
java -Xmx4G -Xms1G -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar -jar leaf-1.21.11-174.jar nogui
```

- **不需要额外配置文件**，功能直接在 JVM 层面通过字节码注入生效
- 需要在 `server.properties` 中保持 `online-mode=true`
- `authlib-injector-1.2.7.jar` 由 YggdrasilOfficialProxy 内部调用，**无需单独配置或启动**

## 适用场景

- 允许使用第三方启动器（如 LittleSkin、BakaXL、HMCL 等）的玩家通过外置认证服务器加入
- 配合 `online-mode=true`，基岩版玩家通过 Floodgate 绕过认证，Java 版玩家通过 Yggdrasil 代理认证
- 皮肤系统（SkinsRestorer + Geyser）的皮肤请求也通过相同的外置认证链路
