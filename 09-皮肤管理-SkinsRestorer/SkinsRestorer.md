# 9. 皮肤管理 — SkinsRestorer

**文件**: `plugins/SkinsRestorer.jar` (7.64 MB)

**官方网站**: https://skinsrestorer.net | **文档**: https://skinsrestorer.net/docs

## 功能说明

SkinsRestorer 允许玩家自由更换 Minecraft 皮肤，支持从 Mojang 正版账号获取皮肤、使用 URL 自定义皮肤、以及使用内置的推荐皮肤库。对基岩版玩家和离线模式 Java 玩家尤为重要。

## 关键配置 (`plugins/SkinsRestorer/config.yml`)

```yaml
messages:
  locale: zh-cn                  # 中文界面
  consoleLocale: zh-cn           # 控制台中文

database:
  type: FILE                     # 文件存储

commands:
  forceDefaultPermissions: true  # 强制默认权限
  skinChangeCooldown: 30         # 换肤冷却: 30 秒
  skullGetCooldown: 30           # 头颅获取冷却: 30 秒
  skinErrorCooldown: 5           # 错误冷却: 5 秒
  maxHistoryLength: 36           # 历史记录条数
  maxFavouriteLength: 180        # 收藏上限

storage:
  defaultSkins:
    enabled: false               # 未启用默认皮肤
  skinExpiresAfter: 15           # 皮肤缓存: 15 分钟
  uuidExpiresAfter: 60           # UUID 缓存: 60 分钟

server:
  proxyMode:
    detection: AUTO              # 自动检测代理模式
    api: true                    # 启用代理 API
  enablePaperJoinListener: true  # Paper 加入事件优化

api:
  mineskinAPIKey: key            # MineSkin API Key（占位符）
  mineskinSecretSkins: false
  fetchRecommendedSkins: true    # 获取推荐皮肤
  mojangBatchWindowSeconds: 1    # Mojang API 批量窗口
  elyByEnabled: false            # Ely.by 已禁用
```

## 常用命令

### 玩家命令

| 命令 | 说明 |
|---|---|
| `/skin set <皮肤名>` | 设置皮肤（使用正版玩家名） |
| `/skin url <URL>` | 通过图片 URL 设置皮肤 |
| `/skin clear` | 清除自定义皮肤 |
| `/skin update` | 更新皮肤 |
| `/skin random` | 随机皮肤 |
| `/skin undo` | 撤销上次换肤 |
| `/skin favourite add <名称>` | 收藏皮肤 |
| `/skins` | 打开皮肤 GUI 浏览器 |
| `/skull <玩家>` | 获取玩家头颅 |

### 管理命令

| 命令 | 说明 |
|---|---|
| `/sr reload` | 重载配置 |
| `/sr props <玩家>` | 查看玩家皮肤属性 |
| `/sr applyskin <目标> <皮肤>` | 强制为目标设置皮肤 |
