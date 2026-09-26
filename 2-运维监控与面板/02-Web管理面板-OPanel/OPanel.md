# 11. Web 管理面板 — OPanel

OPanel 是自带完整 Web 前后端的 Minecraft 服务器图形化管理面板，管理员浏览器登录即可管理服务器。本文给出插件 config.yml、访问方式、根目录 `opanel/` 辅助文件与安全加固建议。

**文件**: `plugins/opanel-bukkit-1.21.9-build-2.0.1.jar`

## 功能

OPanel 全功能 Web 管理面板，内置完整 Web 前端与后端服务（单插件约 78.62 MB）。

## 关键配置 (`plugins/OPanel/config.yml`)

```yaml
accessKey: <AccessKey>                         # API 访问密钥（真实值在服务器 config.yml，勿写入文档）
salt: <Salt>                                      # 加密盐值（真实值在服务器 config.yml，勿写入文档）
webServerPort: 25555                              # Web 面板端口
mcdrSocketPort: 25576                             # MCDR Socket 端口（用于 MCDReforged 通信）
cookieSecure: false                               # Cookie 不使用 HTTPS
proxyHeaders: false                               # 不使用反向代理头
```

## 访问方式

- **Web 面板地址**: `http://服务器IP:25555`
- 使用 `accessKey` 鉴权
- 支持通过浏览器直接管理服务器

## 外部文件

OPanel 在根目录 `opanel/` 下还存放了辅助运行文件：

| 文件 | 说明 |
|---|---|
| `mcp-config.json` | MCDReforged 通信配置（本服已禁用） |
| `launch-command.txt` | 面板启动命令记录 |
| `open-api.json` | OpenAPI 接口定义 |
| `tasks.json` | 定时任务配置 |
| `login-banner.png` | 登录页面横幅图片 |

## 安全提醒

> ⚠️ `accessKey` 和 `salt` 为敏感信息，请勿公开。如需暴露到公网，建议使用 Nginx 反向代理并配置 HTTPS。

## 推荐配置

为保证安全，建议：
1. 修改默认 `accessKey` 为复杂随机字符串
2. 如非必要，将 `webServerPort` 绑定到 `127.0.0.1`
3. 通过 Nginx 反向代理 + HTTPS 对外提供服务
4. 定期更新 OPanel 到最新版本
