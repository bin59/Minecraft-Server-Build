# 服务器待办 TODO

> 维护中事项清单，做完一条勾一条。日期：2026-09-21

## 💰 经济 / 排行榜

- [ ] **财富榜上线验证**：进服执行 `/dh reload` → `/papi parse me %vault_eco_balance_top_1_player%` 验证占位符 → `/ess reload`
  - 全息文件已建：`plugins/DecentHolograms/holograms/baltop.yml`（Top10，每秒刷新）
  - baltop 缓存已改 60 秒
  - 调整全息位置：站到目标点 `/dh move baltop`
- [ ] **死亡提示**：定 CustomDeathMessages 关哪几项（公屏播报 / 死者消息 / 音效粒子 / 重生消息），并确认"死亡税扣 5 块"是否保留

## 💬 聊天触发

- [ ] **聊天触发 tpa**：装 Skript，写 `.sk` 脚本
  - 聊天发 `传送` / `tpa`（单独）→ 打开 QuickMenu 传送菜单
  - 聊天发 `tpa 玩家名` / `传送 玩家名` → 直接发起请求，支持英文 ID 和 `/nick` 昵称反查
  - 依赖：PAPI 的 `%essentials_nickname%`

## 🎮 玩法插件

- [ ] **BGM 资源包**：补 OGG 音乐文件 + HTTPS 托管 URL，配置 `server.properties` 后上线（骨架已建在 `1-服务端核心与网络层/08-服务器资源包-BGM/`）

## 死亡信息

从高处摔下来
僵尸杀死

## 后续

后面可以有限时飞行

积分、活动、在线时长等等，可以累积长不过期

# 20260929异常卡死

位置
30093.581104843528, 128.0, 29764.520788865135

## 按离线时长下次上线自动发南瓜币

## 插件

1.自定义玩家模型

4.ImageFrame 图片展示会不会卡？
