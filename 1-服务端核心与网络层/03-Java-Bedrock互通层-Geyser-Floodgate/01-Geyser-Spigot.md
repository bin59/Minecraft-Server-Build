# 3.1 Geyser（Leaf 后端）

本文档记录本服 Geyser 的实际部署——装在 **Leaf 后端** `plugins/` 下的 Geyser-Spigot。本服是 **Leaf 单后端架构，不部署 Velocity 代理**，因此 Geyser 与 Floodgate 都装在同一台 Leaf 服务端的 `plugins/` 目录里。Geyser 将基岩版网络协议实时翻译为 Java 版协议，使手机、Win10、Xbox 等基岩版玩家能直接加入本服。文档列出后端 config.yml 的关键取值、基岩版玩家连接方式与常用命令，供管理员维护互通层时参考。

**文件**: `plugins/Geyser-Spigot.jar`

> ✅ **实际部署位置（校准 2026-09-24）**: 本服为 Leaf 单后端、`online-mode=true` 经外置登录代理，**未部署 Velocity**。Geyser 装在 Leaf 后端 `plugins/` 下，Floodgate 同为后端版（见 02-Floodgate.md）。下方配置取自后端实际 `plugins/Geyser/config.yml`。
>
> ℹ️ **Geyser-Spigot 与 Geyser-Velocity 的区别**：前者跑在后端（Bukkit/Spigot/Paper/Leaf 服务端），后者跑在 Velocity 代理层。本服只有 Leaf 后端，用前者；代理层版的配置项与此处基本一致，只是文件路径在 `velocity/plugins/Geyser-Velocity/`。

**官方网站**: https://geysermc.org | **Wiki**: https://wiki.geysermc.org

## 功能说明

Geyser 是一个协议转换代理/插件，它能将 Minecraft 基岩版的网络协议实时翻译为 Java 版的协议，使得基岩版玩家（手机、Win10/11、Xbox、Switch、PS4/PS5 等）可以直接加入 Java 版服务器。

## 关键配置 (`plugins/Geyser/config.yml`)

```yaml
bedrock:
  address: 0.0.0.0 # 监听所有网络接口
  port: 55551 # Bedrock UDP 端口（本服与 Java 端口保持一致）
  clone-remote-port: true # true：基岩端口跟随 Java 端口（本服取值）；false：固定 19132，改 Java 端口不再联动

java:
  auth-type: floodgate # 使用 Floodgate 认证基岩版玩家

motd:
  passthrough-motd: true # 透传 Java 服务器的 MOTD
  passthrough-player-counts: true # 透传在线人数
  integrated-ping-passthrough: true # 集成 Ping 透传

gameplay:
  cooldown-type: crosshair # 战斗冷却显示样式
  show-coordinates: true # 显示坐标
  disable-bedrock-scaffolding: false # 允许基岩版脚手架搭建
  emotes-enabled: true # 启用表情
  xbox-achievements-enabled: false # 禁用 Xbox 成就
  max-visible-custom-skulls: 128 # 最大可见自定义头颅
  custom-skull-render-distance: 32 # 头颅渲染距离
  enable-custom-content: true # 启用自定义物品/方块映射
  enable-integrated-pack: true # 启用集成资源包
  force-resource-packs: true # 强制资源包
  block-legacy-codes: true # 阻止旧版格式化代码

advanced:
  floodgate-key-file: key.pem # Floodgate 密钥文件
  java:
    use-direct-connection: true # 直连模式（性能最佳）
    disable-compression: true # 禁用压缩（优化性能）
  bedrock:
    mtu: 1400 # 网络 MTU 值
    compression-level: 6 # 压缩级别
```

## 基岩版玩家连接方式

- **地址**: 服务器 IP（与 Java 版相同）
- **端口**: `55551`（UDP）—— 与 Java 版端口**完全相同**，因为 `clone-remote-port: true`
- ⚠️ 基岩版客户端**不会自动发现**此端口，必须手动把服务器地址填成 `<IP>:55551`
- 若哪天 Java 端口改动，Geyser 端无需再动，`clone-remote-port: true` 会自动跟随

## 常用命令

| 命令                 | 说明             |
| -------------------- | ---------------- |
| `/geyser help`       | 查看 Geyser 帮助 |
| `/geyser dump`       | 生成调试信息     |
| `/geyser reload`     | 重载配置         |
| `/geyser statistics` | 查看统计信息     |
| `/geyser offhand`    | 切换副手物品     |
