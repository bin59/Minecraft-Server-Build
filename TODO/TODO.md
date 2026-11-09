# 服务器待办 TODO

> 维护中事项清单，做完一条勾一条。日期：2026-09-21（2026-11-09 按 26.3 实跑插件复核）

## 💰 经济 / 排行榜

- [x] ~~财富榜上线验证~~：**方案作废**——DecentHolograms 已从 26.3 实跑服移除，`/dh` 命令与 `plugins/DecentHolograms/holograms/baltop.yml` 均不再适用；改用 TAB / 计分板 + `%vault_eco_balance_top_*%` 占位符重新落地，另立待办。
- [ ] **死亡提示**：定 CustomDeathMessages 关哪几项（公屏播报 / 死者消息 / 音效粒子 / 重生消息），并确认"死亡税扣 5 块"是否保留

## 💬 聊天触发

- [ ] **聊天触发 tpa**：装 Skript，写 `.sk` 脚本
  - 聊天发 `传送` / `tpa`（单独）→ 打开 QuickMenu 传送菜单
  - 聊天发 `tpa 玩家名` / `传送 玩家名` → 直接发起请求，支持英文 ID 和 `/nick` 昵称反查
  - 依赖：PAPI 的 `%essentials_nickname%`

## 🎮 玩法插件

- [ ] **BGM 资源包**：补 OGG 音乐文件 + HTTPS 托管 URL，配置 `server.properties` 后上线（骨架已建在 `1-服务端核心与网络层/服务器资源包-BGM/`）

## 死亡信息

从高处摔下来
僵尸杀死

## 🖥️ 运维排查（2026-09-30 / 10-01）

- [x] **外置登录方案切换（2026-10-02）**：弃用 YggdrasilOfficialProxy（Java 21 下 profile 路由 404，官方已弃用），改用 authlib-injector-1.2.7 直连 `https://littleskin.cn/api/yggdrasil`，本地已验证，UUID 不变（详见 `1-服务端核心与网络层/02-外置登录-authlib-injector/`）
- [x] **JPZS9527 皮肤档案缺失（已诊断）**：LittleSkin 无该 UUID 档案 → authlib-injector 报 `Couldn't look up profile properties`；判定无害、不影响登录
- [x] **RCON 密码加固**：已把 `server.properties` 里 `rcon.password` 由弱口令 `test_55551` 换为强随机密码（下次启动生效，密码仅存于 server.properties，勿写入文档）
- [x] **网页上传皮肤 → 应用游戏**：已改由云服务器自行实现连接方案（`skin_upload_rcon_backend.py` 脚本方案作废并已删除）；SkinsRestorer `/skin` 命令与 RCON 均已可用
- [ ] **Geyser 基岩断连**：`PacketErrorEvent` NoClassDefFoundError 为 2.11.3-SNAPSHOT 开发版自身 bug，建议换官方稳定版 release
- [ ] **nightcore 皮肤 URL 校验**：Leaf 26.3 只允许 `textures.minecraft.net`，第三方皮肤站触发报错；方案：Leaf `texture-url-whitelist` 加 `littleskin.cn`（见外置登录文档常见问题）
- [ ] **RSLB 正版/外置共存（26.3 环境已具备）**：正版玩家**免绑定 LittleSkin** 的方案——MultiLogin 的 Paper 单服二次开发，Netty 层无感拦截，内置 LittleSkin 服务配置，玩家无需装任何东西
  - 下载：https://github.com/Rain-Serenity/RSLB/releases ｜ 介绍：https://klpbbs.com/thread-173808-1-1.html
  - **硬性要求：Minecraft 26.2/26.3 + Java 25+**（本服 Leaf 26.3 + Java 25 已满足）
  - 必配项：`online-mode=true`、`enforce-secure-profile=false`
  - 备选路线：上 Velocity 代理 + MultiLogin（当前架构可用但改动大）

## 后续

后面可以有限时飞行 → 已实现：自研 PumpkinFly（10 万买永久 / 20 币 1 分钟，仅飞行中扣秒，限自己领地与主城，见 `自研/南瓜飞-PumpkinFly/使用说明.md`）

积分、活动、在线时长等等，可以累积长不过期

## 按离线时长下次上线自动发南瓜币

## 插件

1.自定义玩家模型

##

禁忌之物 互斥附魔甲 ，比如保护4加火焰 1500一个

有空可以收集你们机器位置，下次清理先选你们的机器然后反选就行了

##

南瓜以后会不会会出一个那个躲猫猫的活动呀？就是可以把人玩家变成方块的那个

啥时候加一个手动自刎归天功能，并且不掉头
