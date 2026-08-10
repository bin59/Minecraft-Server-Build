# 3.2 Floodgate

**文件**: `plugins/floodgate-spigot.jar` (11.03 MB)

## 功能说明

Floodgate 是 Geyser 的配套认证插件。它允许基岩版玩家**绕过 Java 版的正版验证**，使用基岩版的 Xbox Live 账号直接加入开启了 `online-mode=true` 的服务器。Floodgate 使用加密密钥对来确保只有经过 Geyser 代理的合法基岩版连接才能绕过认证。

## 关键配置 (`plugins/floodgate/config.yml`)

```yaml
key-file-name: key.pem # 加密密钥文件（与 Geyser 共享）
username-prefix: '.' # 基岩版玩家用户名前缀（点号）
replace-spaces: true # 替换用户名中的空格为下划线

player-link:
  enabled: true # 启用账号关联
  require-link: false # 不强制要求关联
  enable-global-linking: true # 启用全局关联

disconnect:
  invalid-key: 'Please connect through the official Geyser'
  invalid-arguments-length: 'Expected {} arguments, got {}. Is Geyser up-to-date?'
```

## 基岩版玩家识别

- 基岩版玩家登录后，用户名会自动添加 `.` 前缀
- 例如：基岩版玩家 `Steve` → 服务器内显示为 `.Steve`
- 通过此前缀可轻松区分 Java 和基岩版玩家

## Floodgate 密钥管理

- 密钥文件: `plugins/floodgate/key.pem`
- Geyser 的 `config.yml` 中 `floodgate-key-file: key.pem` 指向此文件
- **重要**: 此密钥对需要保密，更换需同时更新 Geyser 配置
