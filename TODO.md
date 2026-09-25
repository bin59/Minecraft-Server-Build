# 服务器待办 TODO

> 维护中事项清单，做完一条勾一条。日期：2026-09-21

## 🔊 语音相关

- [ ] **基岩版语音**：✅ 已下载 `Svg-Spigot-0.1.4.jar` 放进 `plugins/`（依赖 voicechat/Geyser/floodgate 均就绪）→ **重启服务器**后验证基岩玩家收到网页链接、授权麦克风语音
  - 下载：Modrinth `modrinth.com/plugin/simplevoice-geyser`（Bukkit 版，作者 TheodoreMeyer）
- [ ] **SVC 网络**：确认路由器/防火墙放行 **UDP 24454**（语音直连，不过 Velocity），否则 Java 玩家语音无声

## 📢 侧边公告栏（TAB sidebar）

- [x] 已启用 `plugins/TAB/config.yml` 的 scoreboard（`enabled: true`）
- [x] 设计：金橙渐变标题「南瓜生存服」+ 玩家/在线/世界 + 余额/时长/延迟 + 「欢迎回家」+ 底部 `/sb` 提示
- [x] 玩家个人开关：`/sb`，`remember-toggle-choice: true`（重进游戏记住隐藏/显示选择）
- [ ] **进服执行 `/tab reload` 生效并截图验证效果**
- [ ] 验证 `%plan_player_playtime%` 在线时长占位符能解析（若显示成 `%...%` 原文，改 PAPI playtime 扩展或换 `%playtime%`）
- [ ] 确认基岩端（Geyser）能正常显示 sidebar 计分板
- [ ] 把「`/sb` 可隐藏公告栏」写进玩家手册

## 💰 经济 / 排行榜

- [ ] **财富榜上线验证**：进服执行 `/dh reload` → `/papi parse me %vault_eco_balance_top_1_player%` 验证占位符 → `/ess reload`
  - 全息文件已建：`plugins/DecentHolograms/holograms/baltop.yml`（Top10，每秒刷新）
  - baltop 缓存已改 60 秒
  - 调整全息位置：站到目标点 `/dh move baltop`
- [ ] **死亡提示**：定 CustomDeathMessages 关哪几项（公屏播报 / 死者消息 / 音效粒子 / 重生消息），并确认"死亡税扣 5 块"是否保留

## 🗄️ 数据备份

- [ ] **本地归档备份脚本**：写 `.ps1`，只增不减（`latest/` 覆盖 + `archive/日期/` 新增，永不删旧）
  - Plan：MySQL（localhost:3306，root/minecraft，库 Plan）→ `mysqldump`
  - CoreProtect：当前 **SQLite**（`plugins/CoreProtect/database.db`）→ 直接拷文件；**待定是否切 MySQL**
  - LuckPerms：H2（`luckperms-h2-v2.mv.db`）→ 停服拷文件
  - 用 Windows 计划任务定时跑
  - 需确认：Plan 的 MySQL 是云服务器本机还是独立云数据库，本地备份机怎么连

## 💬 聊天触发

- [ ] **聊天触发 tpa**：装 Skript，写 `.sk` 脚本
  - 聊天发 `传送` / `tpa`（单独）→ 打开 QuickMenu 传送菜单
  - 聊天发 `tpa 玩家名` / `传送 玩家名` → 直接发起请求，支持英文 ID 和 `/nick` 昵称反查
  - 依赖：PAPI 的 `%essentials_nickname%`

## 🎮 玩法插件

- [ ] **RideOnHead**：重启服务器后完全生效；首次生成 `config.yml` 后把实际键名补进文档
- [ ] **末影龙管理**：`DragonManager.jar`（`/dragon respawn` 重生不刷柱子 + 禁传送门四角水晶）+ `dragon-block-plugin`（龙不破坏任何方块）已放 `plugins/` → 重启后验证（文档+源码：`ai写的/末影龙重生与破坏保护/`）
- [ ] **BGM 资源包**：补 OGG 音乐文件 + HTTPS 托管 URL，配置 `server.properties` 后上线（骨架已建在 `1-服务端核心与网络层/08-服务器资源包-BGM/`）

## 📊 在线时间显示

- [x] **TAB 显示在线时间**：已并入侧边公告栏（sidebar 里「时长」行，`%plan_player_playtime%`），待 `/tab reload` 后验证
- [ ] **主城显示在线时间**：主城用 DecentHolograms + PAPI 做全息，显示在线时间（确认是个人在线时间还是当前在线人数/服总在线时长）

## 💰 每日随机收购（DailySell）

- [x] 需求已定：每天随机 4 种物品换**南瓜币**，每种额度随机 10-100、单价当日浮动、总价 ≤ 800、防刷
- [x] 插件已做并部署：`plugins/DailySell.jar` **v1.2.1（27格GUI多页 + 命令 + QuickMenu入口）**（源码+文档：`ai写的/每日随机收购/`）
- [ ] **重启后验证**：`/ds` GUI（左键卖1/Shift卖满/翻页）、`/qm reload` 后快捷菜单入口、`/ds today` 清单、`/ds sell` 到账、额度/预算上限、跨天重抽
