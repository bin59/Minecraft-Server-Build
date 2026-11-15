# 一、核心与网络层

## Geyser

### 玩家命令

基岩版玩家无需输入任何命令即可加入服务器，但 Geyser 提供几条基岩端相关的交互命令（通常由 OP 或有权限者执行，基岩玩家也可用于自身操作）：

| 命令 | 说明 |
| --- | --- |
| `/geyser offhand` | 切换主手与副手物品。基岩版默认没有与 Java 版「F」键对应的副手切换键，基岩玩家可用此命令把主手物品与副手物品互换。 |
| `/geyser help` | 查看 Geyser 全部可用子命令及简要说明。 |

示例：
- 基岩玩家想把手中的盾牌从主手换到副手：`/geyser offhand`
- 查看还能用哪些 Geyser 命令：`/geyser help`

### 管理员命令

| 命令 | 语法 | 参数 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `/geyser help` | `/geyser help [子命令]` | 可选子命令名 | `/geyser help offhand` | 查看帮助，后接子命令可看该命令详情 |
| `/geyser dump` | `/geyser dump` | 无 | `/geyser dump` | 生成一份包含服务器/插件状态的调试信息（返回链接），用于向官方反馈问题 |
| `/geyser reload` | `/geyser reload` | 无 | `/geyser reload` | 重载 Geyser 配置文件（修改 config.yml 后使用） |
| `/geyser statistics` | `/geyser statistics` | 无 | `/geyser statistics` | 查看 Geyser 运行统计信息（如当前基岩连接数、翻译计数等） |
| `/geyser offhand` | `/geyser offhand [玩家]` | 可选玩家名 | `/geyser offhand .Steve` | 切换指定玩家的副手物品；不带参数时作用于自己 |

> 基岩玩家的连接端口与 Java 版相同（由 clone-remote-port 控制），玩家在基岩客户端中须手动把服务器地址填为 `<服务器IP>:<端口>`（如 play.example.com:25565），基岩客户端不会自动发现该端口。

### 权限节点

- `geyser.command.*` —— 所有 Geyser 命令的总权限
- `geyser.command.help` —— 允许使用 `/geyser help`
- `geyser.command.offhand` —— 允许使用 `/geyser offhand`
- `geyser.command.dump` —— 允许使用 `/geyser dump`
- `geyser.command.statistics` —— 允许使用 `/geyser statistics`
- `geyser.command.reload` —— 允许使用 `/geyser reload`

### 典型工作流

**玩家视角（基岩版）：**
1. 打开 Minecraft 基岩版（手机 / Win10-11 / Xbox / Switch / PS）。
2. 在「添加服务器」中填写服务器 IP，端口填与 Java 版相同的端口（如 25565）。
3. 点击加入，用 Xbox Live 账号直接连入（无需 Java 版账号）。
4. 进入游戏后，如需切换副手物品，输入 `/geyser offhand`。

**管理员视角：**
1. 想确认当前基岩互通运行状况：控制台执行 `/geyser statistics`。
2. 排查基岩玩家连接异常时：执行 `/geyser dump` 并把生成的链接附到反馈中。
3. 修改过 config.yml 后：执行 `/geyser reload` 使配置生效。

### 版本信息

最新版本：Geyser Build #1295（Java 26.3 · Bedrock 26.40-26.62），发布于 2026-11-07 / 查证日期 2026-11-13 / 来源 https://geysermc.org/download

---

## Floodgate

### 玩家命令

无玩家可直接使用的命令。Floodgate 是纯服务端认证组件，玩家全程无感知地受益于以下功能：

- **绕过 Java 版正版验证**：基岩版玩家凭 Xbox Live 账号即可直接加入 online-mode=true 的服务器，无需购买 Java 版账号。
- **玩家名前缀识别**：基岩版玩家进入服务器后，用户名会自动带 `.` 前缀（例如基岩玩家 Steve 在服务器内显示为 `.Steve`），据此可区分 Java 版与基岩版玩家。

### 管理员命令

无独立的管理员命令（Floodgate 没有注册 Bukkit 命令）。其联动的基岩连接状态可通过 Geyser 的 `/geyser` 系列命令查看；账号关联（player-link）相关功能由 Floodgate 自身的配置与表单驱动，管理员无需输入命令维护。

### 权限节点

Floodgate 本身不提供独立命令，因此无专属权限节点。其生效完全依赖与 Geyser 共享的加密密钥（key.pem），只要密钥匹配，经 Geyser 代理的基岩连接即被放行。

### 典型工作流

**玩家视角（基岩版）：**
1. 与 Geyser 流程一致，在基岩客户端填好服务器地址与端口后加入。
2. 登录后服务器内名字显示带 `.` 前缀，正常游戏即可，无需任何额外操作。

**管理员视角：**
1. 确认 Geyser 配置中 java.auth-type: floodgate 已启用。
2. 确保 plugins/floodgate/key.pem 与 Geyser 指向的密钥文件一致。
3. 在游戏内或后台通过 `.` 前缀区分基岩玩家。
4. 如需将基岩账号与 Java 账号关联，由玩家侧在基岩客户端内通过 Floodgate 弹出的表单完成。

### 版本信息

最新版本：Floodgate Build #141（floodgate-spigot.jar，对应版本线 2.2.x），发布于 2026-09-17 / 查证日期 2026-11-13 / 来源 https://geysermc.org/download

---

## ViaVersion

### 玩家命令

玩家无需任何命令。ViaVersion 让**比服务端版本更高的 Java 版客户端**直接连入服务器，玩家用新版本客户端打开服务器即可正常进入，无需改动自身游戏。

### 管理员命令

| 命令 | 语法 | 参数 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| `/viaversion` | `/viaversion`（别名 `/viaver`、`/vvbukkit`） | 无 | `/viaversion` | 查看 ViaVersion 插件信息及已加载的 Via 系列插件 |
| `/viaversion list` | `/viaversion list` | 无 | `/viaversion list` | 列出当前在线玩家各自使用的客户端版本 |
| `/viaversion reload` | `/viaversion reload` | 无 | `/viaversion reload` | 重载 ViaVersion 配置（注意：装有 ProtocolLib 时可能踢掉在线玩家） |
| `/viaversion dump` | `/viaversion dump` | 无 | `/viaversion dump` | 生成诊断信息，报障时附上 |
| `/viaversion pps` | `/viaversion pps` | 无 | `/viaversion pps` | 查看被协议转换的玩家的数据包速率 |
| `/viaversion autoteam` | `/viaversion autoteam` | 无 | `/viaversion autoteam` | 开关碰撞自动组队 |
| `/viaversion dontbugme` | `/viaversion dontbugme` | 无 | `/viaversion dontbugme` | 开关更新提醒通知 |
| `/viaversion debug` | `/viaversion debug` | 无 | `/viaversion debug` | 调试模式，在控制台打印所有数据包 |

### 权限节点

- `viaversion.command` —— 基础命令权限
- `viaversion.command.list` —— 允许 `/viaversion list`
- `viaversion.command.reload` —— 允许 `/viaversion reload`
- `viaversion.command.dump` —— 允许 `/viaversion dump`
- `viaversion.command.pps` —— 允许 `/viaversion pps`
- `viaversion.command.autoteam` —— 允许 `/viaversion autoteam`
- `viaversion.command.dontbugme` —— 允许 `/viaversion dontbugme`
- `viaversion.command.debug` —— 允许 `/viaversion debug`
- `viaversion.admin` —— 管理员总权限（默认赋予 OP）

> 版本封禁（block-versions / block-protocols）与数据包限速（packet-limiter）为全局策略，在 ViaVersion 的 config.yml 中配置，不通过命令设置。

### 典型工作流

**玩家视角（高版本客户端）：**
1. 玩家客户端版本高于服务器（例如服务器 1.21.x，玩家用 1.21.11）。
2. 直接用该客户端点击加入服务器，ViaVersion 在后台自动完成协议翻译，玩家进服后正常游戏。

**管理员视角：**
1. 想确认当前有哪些版本客户端在线：控制台执行 `/viaversion list`。
2. 怀疑某玩家发包异常：执行 `/viaversion pps` 查看速率。
3. 修改过 config.yml 后：执行 `/viaversion reload` 生效（装 ProtocolLib 时建议完整重启而非 reload）。

### 版本信息

最新版本：ViaVersion 5.12.2（稳定版，支持至 26.3），发布于 2026-10-27 / 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/viaversion.19254/

---

## ViaBackwards

### 玩家命令

无玩家可直接使用的命令。ViaBackwards 让**比服务端版本更旧的 Java 版客户端**连入服务器（本服可低至 1.9），玩家用旧版客户端直接加入即可。

> 能力边界（玩家可感知的限制）：旧客户端看不到 y<0 与 y>255 的方块、部分背包交互可能不同步；1.7.x/1.8.x 客户端本服未装 ViaRewind 故连不上。

### 管理员命令

ViaBackwards **没有独立命令**（`plugin.yml` 未注册命令），所有管理操作均通过 ViaVersion 的命令执行：

| 命令 | 作用 | 权限 |
| --- | --- | --- |
| `/viaversion` | 查看版本与已加载的 Via 系列插件（含 ViaBackwards） | `viaversion.command` |
| `/viaversion list` | 列出在线玩家客户端版本 | `viaversion.command.list` |
| `/viaversion reload` | 重载 ViaBackwards 配置（优先完整重启） | `viaversion.command.reload` |
| `/viaversion dump` | 生成诊断信息 | `viaversion.command.dump` |
| `/viaversion pps` | 查看数据包速率 | `viaversion.command.pps` |

`viaversion.admin`（默认 OP）包含上述全部权限。

### 权限节点

ViaBackwards 不注册独立权限节点，复用 ViaVersion 的权限体系。`viaversion.admin` 默认赋予 OP，覆盖全部相关子命令。

### 典型工作流

**玩家视角（旧版本客户端）：**
1. 玩家客户端版本低于服务器（例如服务器 1.21.x，玩家用 1.20.4）。
2. 直接用旧版客户端加入服务器，ViaBackwards 后台自动把新版数据包翻译为旧客户端可理解的格式。

**管理员视角：**
1. 确认 ViaBackwards-*.jar 与 ViaVersion-*.jar 版本一致且都在 plugins/ 下。
2. 想确认旧客户端是否成功连入：执行 `/viaversion list` 查看在线玩家的客户端版本。
3. 修改过 ViaBackwards/config.yml 后：优先**完整停服重启**（reload 在装 ProtocolLib 时可能踢人）。

### 版本信息

最新版本：ViaBackwards 5.12.2（稳定版，支持至 26.3，依赖 ViaVersion 5.12.2），发布于 2026-10-27 / 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/viabackwards/versions

---

## Leaf

### 玩家命令

无。Leaf 是服务端核心（Paper/Gale 链路的高性能分支），普通玩家在游戏内没有任何专属命令，玩家感知不到它的存在，只会在整体服务器流畅度上间接受益。

### 管理员命令

- `/leaf reload` — 热重载 Leaf 的全局配置（Leaf 特有生成的配置文件，如 leaf-global.yml 等），无需完整重启即可生效配置改动。

### 权限节点

- `/leaf reload` 通常由 OP 或控制台执行；Leaf 未对外公开细粒度的玩家级权限节点。

### 典型工作流

- **玩家视角**：正常进入游戏即可，无需任何操作。
- **管理员视角**：将 Leaf 作为 Paper 的即插即用替代品部署后，日常运维时若调整了 Leaf 全局配置，可在控制台执行 `/leaf reload` 热重载。

### 版本信息

最新版本：Leaf 26.3（构建 #22，2026-11-08，Java 25+）/ 查证日期 2026-11-13 / 来源 https://www.leafmc.one/zh/download

---

## Velocity + Multiverse

### Velocity（代理端）

Velocity 是独立的代理服务器（独立进程），玩家从 Velocity 一个地址进出，可在多个后端子服间无缝切换。

**玩家命令**
- `/server [子服名]` — 切换到指定子服，或省略参数列出可前往的子服。
- `/glist` — 查看整个网络中在线的玩家总数与各子服分布。

**管理员命令**
- `/velocity list` — 列出网络内所有子服及在线玩家。
- `/velocity send <玩家|all|current> <子服名>` — 将玩家（或全体/all、当前服/current）传送到指定子服。
- `/velocity reload` — 重载代理配置。
- `/velocity plugins` — 查看代理端已加载的插件列表。

**权限节点**
- `velocity.command.server` —— 允许使用 `/server`
- `velocity.command.list` —— 允许查看网络玩家列表
- `velocity.command.send` —— 允许使用 `/velocity send`
- `velocity.command.reload` —— 允许重载代理配置

**典型工作流**
- 玩家：用正式地址进入服务器 → 默认落到首个子服 → 想换服时 `/server minigames` 无缝切换；用 `/glist` 看大家在哪个服。
- 管理员：日常用 `/velocity list` 监控各服在线；活动结束用 `/velocity send all lobby` 把玩家集中回大厅。

### Multiverse-Core（后端多世界）

Multiverse-Core 安装在后端子服上，让单个服务端内存在多个独立世界。

**玩家命令**
- `/mv tp <世界名>` — 传送到指定世界。
- `/mv tp <玩家> <世界名>` — 把其他玩家传送到指定世界。
- `/mv list` — 列出你有权进入的所有世界。
- `/mv spawn` — 回到当前所在世界的出生点。

**管理员命令**
- `/mv create <世界名> <环境> -t <类型>` — 创建一个新世界。
- `/mv set spawn <世界名>` — 设置某世界的出生点。
- `/mv remove <世界名>` / `/mv delete <世界名>` — 移除 / 彻底删除世界。
- `/mv unload <世界名>` / `/mv load <世界名>` — 卸载 / 加载世界。
- `/mv import <名称> <环境>` — 导入已存在于磁盘上的世界。

**权限节点**
- `multiverse.access.*` / `multiverse.access.<世界名>` — 进入世界
- `multiverse.teleport.self.*` / `multiverse.teleport.other` — 传送
- `multiverse.create.*` — 创建世界
- `multiverse.core.manage` — 管理世界（删除/重载/卸载等）

### 版本信息

Velocity 4.3.0（build #44，2026-10-18）/ 来源 https://papermc.io/downloads/velocity/
Multiverse-Core 5.8.2（2026-10-26）/ 来源 https://modrinth.com/plugin/multiverse-core/version/5.8.2

---

## ProtocolLib

### 玩家命令

无。ProtocolLib 是数据包拦截与修改的底层库（Library），普通玩家在游戏内无任何命令或权限，也感知不到它的存在；它仅为依赖它的功能插件提供底层支持。

### 管理员命令

- `/protocol` — 打开 ProtocolLib 主菜单，列出当前已挂钩的插件。
- `/protocol list` — 列出已挂钩插件及其注册的数据包监听器。
- `/protocol lib` — 显示 ProtocolLib 自身版本与加载状态。
- `/protocol dump` — 生成诊断 dump 文件。
- `/protocol verify` — 校验数据包字段映射是否正确。
- `/protocol reload` — 重载 ProtocolLib 的 config.yml。

### 权限节点

- `protocol.admin` —— 上述所有 `/protocol` 子命令的管理权限（默认 OP 拥有）
- `protocol.info` —— 接收 ProtocolLib 发来的数据包警告聊天通知（默认 OP）

### 版本信息

最新稳定版本：ProtocolLib 5.4.0（2025-08，支持 MC 1.8–1.21.8，Java 17+）；在 MC 26.3 上需使用 5.5.0+ 的开发构建才能较好适配 26.x / 查证日期 2026-11-13 / 来源 https://www.spigotmc.org/resources/protocollib.1997/

---

## YggdrasilOfficialProxy

无玩家/管理员可直接使用的命令（纯服务端组件）。

> YggdrasilOfficialProxy（YOP，v2.3.0）是一个 Java Agent，通过字节码注入封装 authlib-injector，让服务器在保持 online-mode=true 的同时改用第三方 Yggdrasil 认证服务器验证玩家身份。它以 -javaagent 方式随服务端启动，在 JVM 层面生效，不提供任何游戏内或控制台命令。
>
> 使用者能感知的功能：使用第三方启动器的玩家可正常通过外置登录加入服务器；皮肤系统、基岩版 Floodgate 认证链路也随同一套外置认证生效。

### 版本信息

最新版本：YggdrasilOfficialProxy 2.3.0 / 查证日期 2026-11-13 / 来源 https://github.com/Neubulae/YggdrasilOfficialProxy

---

## BGM 资源包

> 本服 BGM 方案由「服务器资源包 + 自定义音效 + 循环播放数据包」组成，并非独立插件。玩家进服时若同意下载资源包，进入主城、矿区、出生点等区域会**被动自动听到**对应 BGM；基岩版玩家经 Geyser 转译同样能听到。

### 玩家命令

无专属命令。玩家进服弹窗选择下载音乐资源包（选"是"即可收听），走进对应区域自动响起背景音乐，无需任何权限或操作。

若想手动测试某段 BGM（通常由管理员执行）：
- `/playsound <音效事件> <类别> <目标> <x> <y> <z> <音量> <音高>` — 播放自定义音效事件。
  - 示例：`/playsound bgm.lobby music @a ~ ~ ~ 1 1`（向全体玩家播放主城 BGM）
  - 可用事件：`bgm.lobby`（主城）、`bgm.mines`（矿区）、`bgm.spawn`（出生点）

### 管理员命令

- `/reload` — 重载数据包（修改或首次放入循环 BGM 数据包后）。
- `/function bgm:start` — 启动常驻 BGM 循环。
- `/function bgm:stop` — 停止 BGM 循环。

### 权限节点

- 玩家侧：无需任何权限，`/playsound` 由服务端对玩家播放，玩家被动收听。
- 管理员侧：`/function bgm:start|stop` 与 `/reload` 需 OP 或控制台权限。

### 典型工作流

**玩家视角**：进服弹窗选择下载音乐资源包 → 走进主城/矿区/出生点区域自动响起对应背景音乐 → 无需任何输入。

**管理员视角**：准备好 OGG 音乐并打包为资源包托管到 HTTPS → 把循环 BGM 数据包放入 world/datapacks/ → 控制台执行 `/reload` → 执行 `/function bgm:start` 开启常驻循环 → 日后维护用 `/function bgm:stop` 暂停。

### 版本信息

自研方案，无独立插件版本号（基于 Minecraft 原版资源包 + 数据包机制，适用 MC 26.3）/ 查证日期 2026-11-13
