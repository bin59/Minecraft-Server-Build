# 9. 皮肤管理 — SkinsRestorer

SkinsRestorer 让玩家自由更换 Minecraft 皮肤，支持从正版账号获取、URL 自定义和内置推荐皮肤库，对离线模式与基岩版玩家尤为重要。本文档列出了 config.yml 的关键配置（中文界面、换肤冷却、皮肤缓存等）以及玩家换肤命令和管理员维护命令。供管理员确认换肤功能配置时参考。

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

## 网页上传皮肤 → 应用服务器（方案A：RCON + SkinsRestorer）

> 供"另一台云服务器网页上传皮肤，应用到本服游戏"使用。核心思路：**网页后端通过 RCON 向服务器发 `/skin url`（SkinsRestorer 命令），把玩家皮肤应用到游戏内**。

### 前置条件（已核实通过）

| 项 | 状态 | 值 |
|---|---|---|
| RCON | ✅ 已开启 | `enable-rcon=true`，端口 `25595`，密码已换为强随机（见 `server.properties`） |
| SkinsRestorer `/skin` 命令 | ✅ 已启用 | `disableSkinCommand: false` |
| URL 皮肤限制 | ✅ 未限制 | `restrictSkinUrls.enabled: false`（任意 URL 可用） |

> ✅ RCON 密码已由弱口令 `test_55551` 加固为强随机密码（写入 `server.properties`）。对外提供前请确认使用新密码。

### 使用步骤

1. **运行后端**（Python 标准库，零第三方依赖，低资源占用）：
   ```
   python skin_upload_rcon_backend.py
   ```
   默认监听 `127.0.0.1:8090`。可用环境变量覆盖：
   - `RCON_HOST` / `RCON_PORT` / `RCON_PASSWORD`：服务器 RCON 连接
   - `SKIN_PUBLIC_BASE_URL`：皮肤图片对外可访问 URL
   - `SKIN_API_TOKEN`：可选鉴权 Token（推荐设置）

2. **网页端调用**（两种方式二选一）：
   - **上传图片**：`POST /api/apply-skin`，`multipart` 表单，字段 `player`（玩家名）+ 皮肤图片文件
   - **直接给 URL**：`POST /api/apply-skin`，表单字段 `player` + `url`（皮肤图片地址）

3. 后端会保存图片 → 拼 URL → 经 RCON 发送：
   ```
   skinsrestorer:applyskin <玩家名> <皮肤URL>
   ```
   返回服务器执行输出。

### 联调自测
```
python skin_upload_rcon_backend.py --selftest
```
> 可先手动发 `/skin url <玩家> <皮肤URL>` 验证 SkinsRestorer 抓取正常，再接入网页。
