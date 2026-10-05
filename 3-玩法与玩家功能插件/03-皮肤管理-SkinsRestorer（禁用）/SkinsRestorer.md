# 9. 皮肤管理 — SkinsRestorer

SkinsRestorer 让玩家自由更换 Minecraft 皮肤，支持从正版账号获取、URL 自定义和内置推荐皮肤库，对离线模式与基岩版玩家尤为重要。本文档列出了 config.yml 的关键配置（中文界面、换肤冷却、皮肤缓存等）以及玩家换肤命令和管理员维护命令。供管理员确认换肤功能配置时参考。

**文件**: `plugins/SkinsRestorer.jar` (7.64 MB)

**官方网站**: https://skinsrestorer.net | **文档**: https://skinsrestorer.net/docs

## 功能说明

SkinsRestorer 允许玩家自由更换 Minecraft 皮肤，支持从 Mojang 正版账号获取皮肤、使用 URL 自定义皮肤、以及使用内置的推荐皮肤库。对基岩版玩家和离线模式 Java 玩家尤为重要。

## 关键点

服务器上没有任何一个玩家的皮肤来自皮肤站——LittleSkin 在这里只负责「让玩家能登录」，皮肤是 SkinsRestorer 在游戏里用 /skin 设定的，数据只存在于 MC 服那台机器的磁盘上。网站在另一台机器，看不见它，所以要打通。

## 数据存储位置（含基岩版玩家）

本服 `database.type: FILE`（文件存储，未用 MySQL），全部皮肤数据在 `plugins/SkinsRestorer/` 下：

| 数据 | 位置 | 说明 |
| --- | --- | --- |
| **玩家当前皮肤 + 历史** | `plugins/SkinsRestorer/players/<UUID>.player` | JSON 文件，记录当前 `skinIdentifier` 与全部换肤历史（含收藏、撤销记录） |
| **Mojang 皮肤纹理缓存** | `plugins/SkinsRestorer/cache/*.mojangcache` | 从 Mojang 拉取的皮肤纹理数据 |
| **URL 皮肤缓存** | `plugins/SkinsRestorer/skins/*.urlskin` + `.urlindex` | 用 `/skin url` 设置的皮肤 |
| **皮肤库推荐皮肤** | `plugins/SkinsRestorer/skins/sr-recommendation-*.customskin` + `recommendations.json` | 皮肤库浏览 / `/skin random` 的数据源（本服已本地化，无需联网） |

**基岩版玩家**：基岩玩家经 Floodgate 进服，UUID 是 `00000000-0000-0000-0009-xxxx` 固定格式，换过皮肤（皮肤库 / URL / 自定义）后同样落在 `players/<基岩UUID>.player`，与 Java 玩家完全同构，例如：

```json
{"uniqueId":"00000000-0000-0000-0009-01f068214d30",
 "skinIdentifier":{"identifier":"sr-recommendation-tv","type":"CUSTOM"},
 "history":[{"timestamp":1789292670,"skinIdentifier":{"identifier":"sr-recommendation-tv","type":"CUSTOM"}}, ...],
 "dataVersion":2}
```

- **没自定义过皮肤的基岩玩家**不产生 `.player` 文件，皮肤由 Geyser 从基岩客户端直接透传（`uploaded_skins/` 目录存在但为空，即未走本地缓存）。
- **备份 / 迁移**：整目录拷贝 `plugins/SkinsRestorer/`（players + cache + skins）即可，无需额外导出数据库。

## 皮肤来源（本服实测，250 名玩家）

全服玩家皮肤的**来源只有 4 类**，其中仅前两类会进入 SkinsRestorer 记录（产生 `.player` / `.customskin` / `.urlskin` 文件）；后两类**不经过 SkinsRestorer**，服务器本地原本无数据：

| # | 来源 | 数据形态 | 玩家规模 | 例子 |
| --- | --- | --- | --- | --- |
| ① | **皮肤库（服务器内置）** | `skins/sr-recommendation-*.customskin`（本地化，无需联网） | 29 人在用（23 款推荐皮肤） | `sr-recommendation-herobrine`（`.FilthyBulb2570`、`.SoapyMage6600`、`.wzhatk` 3 人在用） |
| ② | **皮肤网站（MineSkin）** | `skins/*.urlskin`（`/skin url` 生成的 `minesk.in` 链接，历史累计 45 次，无其他域名） | 7 人在用（5 个唯一 URL） | `https://minesk.in/265ad77148fa490eae1f1e5362189557`（`.BaiMao7975`、`BaiMao_er` 共用同一张） |
| ③ | **基岩原生（客户端自带）** | 不入库；皮肤在基岩账号（Xbox），Geyser 在线时透传 | 126 人（未换肤基岩玩家） | `.PinchHydra4690`（从未 `/skin`，皮肤来自基岩设备/皮肤商店） |
| ④ | **正版 Mojang（账号自带）** | 不入库；皮肤由 Mojang 按账号下发 | 77 名 Java 未换肤玩家中仅 18 人为真 Mojang 正版 | `KAPIAN_`（正版账号，皮肤官方下发） |

要点：

- SkinsRestorer 记录里**只有 ①②**两类来源（CUSTOM 全部为 `sr-recommendation-*`，URL 全部为 `minesk.in`，无 PLAYER 正版记录——`cache/*.mojangcache` 均为 40B 空占位，说明 `/skin set <正版名>` 从未成功用过）。
- ③基岩原生皮肤**离线无法拉取**（服务器无数据）：已由自研插件 **BedrockSkinRecorder** 在玩家上线时从 Geyser 皮肤缓存固化 PNG 到 `plugins/BedrockSkinRecorder/skins/`，跑一段时间即可补全。
- ④真正版玩家皮肤可从 Mojang session API 联网拉取（如 `Fen_Shui`、`Xeraph627` 已拉取）；其余 Java 玩家为外置登录（LittleSkin），账号未设皮肤则显示默认 Steve/Alex。

## 关键配置 (`plugins/SkinsRestorer/config.yml`)

```yaml
messages:
  locale: zh-cn # 中文界面
  consoleLocale: zh-cn # 控制台中文

database:
  type: FILE # 文件存储

commands:
  forceDefaultPermissions: true # 强制默认权限
  skinChangeCooldown: 30 # 换肤冷却: 30 秒
  skullGetCooldown: 30 # 头颅获取冷却: 30 秒
  skinErrorCooldown: 5 # 错误冷却: 5 秒
  maxHistoryLength: 36 # 历史记录条数
  maxFavouriteLength: 180 # 收藏上限

storage:
  defaultSkins:
    enabled: false # 未启用默认皮肤
  skinExpiresAfter: 15 # 皮肤缓存: 15 分钟
  uuidExpiresAfter: 60 # UUID 缓存: 60 分钟

server:
  proxyMode:
    detection: AUTO # 自动检测代理模式
    api: true # 启用代理 API
  enablePaperJoinListener: true # Paper 加入事件优化

api:
  mineskinAPIKey: key # MineSkin API Key（占位符）
  mineskinSecretSkins: false
  fetchRecommendedSkins: true # 获取推荐皮肤
  mojangBatchWindowSeconds: 1 # Mojang API 批量窗口
  elyByEnabled: false # Ely.by 已禁用
```

## 常用命令

### 玩家命令

| 命令                         | 说明                       |
| ---------------------------- | -------------------------- |
| `/skin set <皮肤名>`         | 设置皮肤（使用正版玩家名） |
| `/skin url <URL>`            | 通过图片 URL 设置皮肤      |
| `/skin clear`                | 清除自定义皮肤             |
| `/skin update`               | 更新皮肤                   |
| `/skin random`               | 随机皮肤                   |
| `/skin undo`                 | 撤销上次换肤               |
| `/skin favourite add <名称>` | 收藏皮肤                   |
| `/skins`                     | 打开皮肤 GUI 浏览器        |
| `/skull <玩家>`              | 获取玩家头颅               |

### 管理命令

| 命令                          | 说明               |
| ----------------------------- | ------------------ |
| `/sr reload`                  | 重载配置           |
| `/sr props <玩家>`            | 查看玩家皮肤属性   |
| `/sr applyskin <目标> <皮肤>` | 强制为目标设置皮肤 |
