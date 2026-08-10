# 3.1 Geyser-Spigot

**文件**: `plugins/Geyser-Spigot.jar` (18.26 MB)

**官方网站**: https://geysermc.org | **Wiki**: https://wiki.geysermc.org

## 功能说明

Geyser 是一个协议转换代理/插件，它能将 Minecraft 基岩版的网络协议实时翻译为 Java 版的协议，使得基岩版玩家（手机、Win10/11、Xbox、Switch、PS4/PS5 等）可以直接加入 Java 版服务器。

## 关键配置 (`plugins/Geyser-Spigot/config.yml`)

```yaml
bedrock:
  address: 0.0.0.0        # 监听所有网络接口
  port: 19132             # Bedrock UDP 端口
  clone-remote-port: true # 自动跟随 Java 端口

java:
  auth-type: floodgate    # 使用 Floodgate 认证基岩版玩家

motd:
  passthrough-motd: true              # 透传 Java 服务器的 MOTD
  passthrough-player-counts: true     # 透传在线人数
  integrated-ping-passthrough: true   # 集成 Ping 透传

gameplay:
  cooldown-type: crosshair            # 战斗冷却显示样式
  show-coordinates: true              # 显示坐标
  disable-bedrock-scaffolding: false  # 允许基岩版脚手架搭建
  emotes-enabled: true                # 启用表情
  xbox-achievements-enabled: false    # 禁用 Xbox 成就
  max-visible-custom-skulls: 128      # 最大可见自定义头颅
  custom-skull-render-distance: 32    # 头颅渲染距离
  enable-custom-content: true         # 启用自定义物品/方块映射
  enable-integrated-pack: true        # 启用集成资源包
  force-resource-packs: true          # 强制资源包
  block-legacy-codes: true            # 阻止旧版格式化代码

advanced:
  floodgate-key-file: key.pem         # Floodgate 密钥文件
  java:
    use-direct-connection: true       # 直连模式（性能最佳）
    disable-compression: true         # 禁用压缩（优化性能）
  bedrock:
    mtu: 1400                         # 网络 MTU 值
    compression-level: 6              # 压缩级别
```

## 基岩版玩家连接方式

- **地址**: 服务器 IP（与 Java 版相同）
- **端口**: `19132`（基岩版默认端口）
- 基岩版客户端自动发现 UDP 19132 端口，无需手动设置

## 常用命令

| 命令 | 说明 |
|---|---|
| `/geyser help` | 查看 Geyser 帮助 |
| `/geyser dump` | 生成调试信息 |
| `/geyser reload` | 重载配置 |
| `/geyser statistics` | 查看统计信息 |
| `/geyser offhand` | 切换副手物品 |
