# EssentialsX 多功能指令整合：功能说明与完整配置

EssentialsX 是 Spigot/Paper 服务器装机量最大的经典指令套件，本文先以功能说明梳理它提供的传送家园、经济、管理审核、物品工具包、聊天、世界保护等全部模块，再给出一份面向 Java 基岩互通服的完整 config.yml，并附 kits.yml、commands.yml 模板与 LuckPerms 权限分配示例，服主可直接照抄后按注释微调。

---

## 第一部分：功能说明

**EssentialsX 是 Minecraft 服务器生态中最经典、装机量最大的插件套件**，为 Spigot/Paper 服务器提供超过 150 个核心命令和完整的服务器管理功能。以下是其完整详细的功能说明：

---

### 📦 插件概览

EssentialsX 是原版 Essentials（2014 年停更）的现代化分支，由社区持续维护。

**基础信息：**

| 项目       | 说明                                                                            |
| ---------- | ------------------------------------------------------------------------------- |
| 支持版本   | MC 1.8.8 ~ 1.21.11+ |
| 服务端要求 | CraftBukkit / Spigot / **Paper（推荐）**                                        |
| Java 要求  | Java 8+                                                                         |
| 核心依赖   | Vault（经济接口）、LuckPerms（权限管理，推荐）                                  |
| 配置文件   | `plugins/Essentials/config.yml`                                                 |

---

### 🏠 传送与家园系统

这是 EssentialsX 最核心的功能之一：

| 指令              | 功能               | 说明                   |
| ----------------- | ------------------ | ---------------------- |
| `/home`           | 传送回家（禁用）   | 支持多个家             |
| `/sethome [名称]` | 设置家（禁用）     | 不指定名称则设为默认家 |
| `/delhome <名称>` | 删除家（禁用）     | —                      |
| `/tpa <玩家>`     | 发送传送请求       | 请求传送到对方位置     |
| `/tpahere <玩家>` | 请求对方传送到你   | —                      |
| `/tpaccept`       | 接受传送请求       | —                      |
| `/tpdeny`         | 拒绝传送请求       | —                      |
| `/tp <玩家>`      | 管理员直接传送     | 无需对方同意           |
| `/tphere <玩家>`  | 管理员拉人         | 将玩家传送到自己身边   |
| `/warp`           | 查看公共传送点列表 | —                      |
| `/warp <名称>`    | 传送到指定传送点   | —                      |
| `/setwarp <名称>` | 设置公共传送点     | 管理员权限             |
| `/delwarp <名称>` | 删除公共传送点     | —                      |
| `/back`           | 返回上一个位置     | 传送/死亡后可用        |
| `/spawn`          | 传送到服务器出生点 | —                      |

**可配置项：**

- 传送冷却时间（防止频繁传送）
- 传送延迟（发送请求后等待 N 秒再传送，移动则取消）
- 家园数量上限（可按权限组设置不同上限）
- 传送费用（配合经济系统扣费）
- 跨世界传送权限控制

---

### 💰 经济系统

内置完整的经济功能，并通过 Vault 接口与其他插件联动：

| 指令                       | 功能             |
| -------------------------- | ---------------- |
| `/balance` 或 `/bal`       | 查询自己的余额   |
| `/balancetop` 或 `/baltop` | 查看财富排行榜   |
| `/pay <玩家> <金额>`       | 转账给其他玩家   |
| `/eco give <玩家> <金额>`  | 管理员给玩家加钱 |
| `/eco take <玩家> <金额>`  | 管理员扣玩家钱   |
| `/eco reset <玩家>`        | 重置玩家余额     |
| `/sell`                    | 出售手中物品     |

**特色功能：**

- **告示牌商店** — 玩家可创建 `[Buy]` / `[Sell]` 告示牌，自动进行交易
- **命令收费** — 可配置特定指令消耗金币（如 `/home` 收费 10 元）
- **初始资金** — 新玩家加入时自动获得配置的初始金额
- **最大余额上限** — 可设置玩家持有金额的上限

---

### 🛡️ 管理与审核工具

覆盖服务器日常运营所需的管理命令：

| 指令                            | 功能                           |
| ------------------------------- | ------------------------------ |
| `/kick <玩家> [原因]`           | 踢出玩家                       |
| `/ban <玩家> [原因]`            | 永久封禁玩家                   |
| `/tempban <玩家> <时长> [原因]` | 临时封禁（如 `7d` 表示 7 天）  |
| `/unban <玩家>`                 | 解封玩家                       |
| `/mute <玩家> [时长]`           | 禁言玩家                       |
| `/unmute <玩家>`                | 解除禁言                       |
| `/jail <玩家>`                  | 关禁闭（限制在指定区域）       |
| `/unjail <玩家>`                | 解除禁闭                       |
| `/god`                          | 管理员无敌模式                 |
| `/fly`                          | 飞行模式                       |
| `/heal [玩家]`                  | 治疗玩家（恢复生命值和饥饿值） |
| `/feed [玩家]`                  | 恢复饥饿值                     |
| `/gamemode <模式> [玩家]`       | 切换游戏模式                   |
| `/whois <玩家>`                 | 查看玩家详细信息               |
| `/seen <玩家>`                  | 查看玩家最后在线时间           |
| `/invsee <玩家>`                | 查看/编辑其他玩家背包          |
| `/vanish`                       | 隐身模式                       |

---

### 📦 物品与工具包系统

| 指令                          | 功能               |
| ----------------------------- | ------------------ |
| `/item <物品> [数量]` 或 `/i` | 给予物品           |
| `/give <玩家> <物品> [数量]`  | 给指定玩家物品     |
| `/enchant <附魔> [等级]`      | 附魔手中物品       |
| `/repair`                     | 修复手中物品耐久   |
| `/repair all`                 | 修复背包内所有物品 |
| `/kit`                        | 查看可用工具包列表 |
| `/kit <名称>`                 | 领取指定工具包     |

**工具包（Kit）系统详解：**

- 支持按权限组配置不同工具包（如新手包 / 每日奖励包）
- 支持设置领取冷却时间
- 支持在工具包中包含附魔物品、药水、刷怪蛋等
- 支持 Banner 元数据（旗帜图案）
- 可通过配置文件精确定义每个工具包的内容

---

### 💬 聊天系统（EssentialsChat 模块）

| 功能           | 说明                                     |
| -------------- | ---------------------------------------- |
| 聊天格式自定义 | 可配置前缀、后缀、世界名、权限组标签等   |
| 本地聊天       | 玩家只能看到附近玩家的聊天（可配置范围） |
| 全局聊天       | 使用 `!` 前缀发送全服可见消息            |
| 私信系统       | `/msg <玩家> <消息>` 或 `/whisper`       |
| 回复私信       | `/reply` 或 `/r` 快速回复                |
| 聊天过滤       | 自动过滤不当内容                         |
| 昵称系统       | `/nick <昵称>` 自定义显示名称            |
| 颜色权限       | 可按权限组控制玩家可使用的聊天颜色       |
| 忽略玩家       | `/ignore <玩家>` 屏蔽指定玩家的聊天      |

---

### 🔔 玩家加入 / 退出消息自定义（custom-join-message / custom-quit-message）

玩家加入 / 退出时在聊天栏广播的「X 加入了游戏 / X 离开了游戏」可用 EssentialsX 覆盖原版广播，做出与服务器主题一致的进出服提示。

**配置文件**：`plugins/Essentials/config.yml`

```yaml
custom-join-message: 'none' # 默认 "none" = 沿用原版；改为自定义文案即覆盖
custom-quit-message: 'none' # 退出广播
custom-new-username-message: 'none' # 改名后重进专用，一般留 "none" 复用上面两条
hide-join-quit-messages-above: -1 # -1 = 在线人数再多也显示；设正整数则在人多时隐藏
```

**可用占位符**：

| 占位符                  | 含义                        |
| ----------------------- | --------------------------- |
| `{PLAYER}`              | 玩家显示名（含前缀 / 后缀） |
| `{USERNAME}`            | 玩家原始用户名              |
| `{PREFIX}` / `{SUFFIX}` | 前缀 / 后缀                 |
| `{ONLINE}`              | 当前在线人数                |
| `{UNIQUE}`              | 累计加入过的独立玩家数      |
| `{UPTIME}`              | 服务端已运行时长            |

**颜色代码用 `&`（不是 `§`）**：EssentialsX 配置里颜色用 `&`（`&a` 绿、`&c` 红、`&e` 金、`&f` 白、`&5` 紫、`&d` 粉、`&l` 粗体……），与 `server.properties` 的 `§` 不同，勿混。

**贴合本服 MOTD 风格的示例**：

```yaml
custom-join-message: '&d[&5+&d] &f{PLAYER} &e加入了南瓜国际服'
custom-quit-message: '&d[&5-&d] &f{PLAYER} &e离开了南瓜国际服'
```

效果：`[+] Steve 加入了南瓜国际服` / `[-] Steve 离开了南瓜国际服`（紫金配色）。

**生效**：游戏内 `/ess reload` 或重启后端即生效；云端服务器改同一份配置即可。

---

### 🌍 世界与重生点管理（EssentialsSpawn 模块）

| 指令             | 功能                 |
| ---------------- | -------------------- |
| `/setspawn`      | 设置当前世界出生点   |
| `/spawn`         | 传送到出生点         |
| `/firstspawn`    | 传送到首次加入出生点 |
| `/setfirstspawn` | 设置首次加入出生点   |

**可配置项：**

- 每个世界独立的出生点
- 首次加入服务器的专属出生点
- 死亡后重生位置控制
- `/time` 设置游戏时间
- `/weather` 控制天气（晴/雨/雷暴）
- `/difficulty` 调整游戏难度

---

### 🛡️ 服务器保护（EssentialsProtect / AntiBuild 模块）

**EssentialsProtect 模块：**

- 防止 TNT / 苦力怕 / 床爆炸破坏地形
- 控制火焰蔓延
- 防止熔岩/水流破坏
- 控制怪物行为（如末影人搬运方块）
- 防止作物被踩踏
- 控制实体交互行为

**EssentialsAntiBuild 模块：**

- 按权限组控制建筑权限
- 防止未授权玩家放置/破坏方块
- 防止未授权玩家使用物品（如火焰弹、水桶）
- 区域级别的建筑权限管理

---

### ⏱️ 命令冷却系统（Command Cooldowns）

可对任意命令设置冷却时间，防止滥用：

```yaml
command-cooldowns:
  feed: 10 # /feed 冷却 10 秒
  home: 70 # /home 冷却 1 分 10 秒
  '*potato*': 30 # 通配符匹配，含 potato 的命令冷却 30 秒
```

**高级特性：**

- 支持通配符匹配（`*`）
- 支持正则表达式匹配（以 `^` 开头）
- 冷却持久化（重启服务器后冷却不丢失）
- 可通过权限 `essentials.commandcooldowns.bypass` 绕过冷却

---

### 🎮 Discord 集成（EssentialsDiscord 模块）

| 功能     | 说明                                        |
| -------- | ------------------------------------------- |
| 聊天同步 | 游戏内聊天 ↔ Discord 频道双向同步           |
| 事件通知 | 玩家加入/离开/死亡/成就等事件推送到 Discord |
| 远程执行 | 从 Discord 执行服务器控制台命令             |
| 私信转发 | 从 Discord 给游戏内玩家发私信               |
| 在线列表 | 在 Discord 查看服务器在线玩家               |

---

### 🌐 GeoIP 地理位置查询（EssentialsGeoIP 模块）

- 查看玩家登录时的地理位置（国家/城市）
- 管理员可在玩家加入时自动显示 IP 归属地
- 用于辅助审核和反作弊

---

### 🔧 其他实用功能

| 功能         | 说明                                           |
| ------------ | ---------------------------------------------- |
| 多语言支持   | 内置中文语言文件 `messages_zh.properties`      |
| 热重载       | `/essentials reload` 重载配置无需重启          |
| 数据存储     | 支持 YAML / MySQL / SQLite                     |
| 异步处理     | 数据库操作异步执行，不阻塞主线程               |
| 物品别名     | 支持所有版本的物品别名（如 `/i stone` `/i 1`） |
| 刷怪蛋支持   | 全版本支持刷怪蛋和刷怪笼指令                   |
| 现代物品支持 | 支持鞘翅、三叉戟、自定义附魔等新机制           |
| UUID 支持    | 基于 UUID 的玩家数据管理                       |

---

### 📐 权限系统

EssentialsX 为每条命令都提供独立的权限节点，可配合 LuckPerms 精细控制：

```
essentials.home          # 使用 /home
essentials.sethome       # 使用 /sethome
essentials.tpa           # 使用 /tpa
essentials.kit           # 使用 /kit
essentials.fly           # 使用 /fly
essentials.god           # 使用 /god
essentials.ban           # 使用 /ban
essentials.commandcooldowns.bypass  # 绕过命令冷却
```

---

### 🧩 模块清单总览

| 模块                      | 功能                                 | 是否必须 |
| ------------------------- | ------------------------------------ | -------- |
| **EssentialsX**           | 核心模块（传送、经济、物品、管理等） | ✅ 必须  |
| **EssentialsX Chat**      | 聊天格式与控制                       | 可选     |
| **EssentialsX Spawn**     | 出生点管理                           | 可选     |
| **EssentialsX Protect**   | 世界保护                             | 可选     |
| **EssentialsX AntiBuild** | 建筑权限控制                         | 可选     |
| **EssentialsX Discord**   | Discord 集成                         | 可选     |
| **EssentialsX GeoIP**     | 地理位置查询                         | 可选     |

---

### 💡 与 Java 基岩互通服的配合

对于 GeyserMC 互通服场景：

- EssentialsX 的所有指令对 Java 和基岩版玩家**同样生效**
- 基岩版玩家通过 GeyserMC + Floodgate 可以直接使用 `/tpa`、`/home`、`/kit` 等指令
- 经济系统、工具包、权限控制对两端玩家统一生效
- 如果基岩版玩家输入指令不便，可搭配 GUI 插件（如 BedrockPlayerSupport）提供表单界面辅助操作

---

---

## 第二部分：完整配置方案

以下是一份面向 **Java 基岩互通服** 的 EssentialsX 完整配置方案。由于 EssentialsX 的 `config.yml` 包含数百个配置项，这里提供的是**覆盖所有核心功能的完整配置框架**，每个配置项都附带中文注释，你可以直接复制到服务器中使用，按注释修改数值即可。

---

### 📁 前置依赖

在配置 EssentialsX 之前，确保已安装以下插件：

| 插件                     | 用途                               | 是否必须      |
| ------------------------ | ---------------------------------- | ------------- |
| **Vault**                | 经济接口，EssentialsX 经济功能依赖 | ✅ 必须       |
| **LuckPerms**            | 权限管理，控制玩家可用指令         | ✅ 推荐       |
| **GeyserMC + Floodgate** | Java 基岩互通                      | ✅ 互通服必须 |

---

### 📄 config.yml 完整配置

文件路径：`plugins/Essentials/config.yml`

```yaml
############################################################
# EssentialsX 完整配置文件
# 适用于 Java 基岩互通服
############################################################

# ==================== 通用设置 ====================

# 是否使用 Bukkit 权限系统（推荐开启，配合 LuckPerms）
use-bukkit-permissions: true

# 是否启用命令监听器（用于命令冷却等功能）
turn-off-command-blocks: false

# 是否允许玩家使用 /list 查看在线玩家
hide-join-quit-messages: false

# 服务器名称（用于部分消息显示）
server-name: '&6南瓜国际服'

# 是否启用调试模式（排查问题时开启）
debug: false

# ==================== 传送系统 ====================

# 传送延迟（秒）- 发送传送请求后等待的时间，移动则取消
teleport-delay: 0

# 传送冷却时间（秒）- 两次传送之间的最短间隔
teleport-cooldown: 0

# 传送无敌时间（秒）- 传送后获得无敌的时间，防止传送中被攻击
teleport-invulnerability: 4

# 传送时是否治愈玩家
teleport-heal: false

# 是否允许传送到其他世界
allow-world-teleport: true

# 是否在传送时显示目标位置
teleport-to-player-location: true

# 是否允许使用 /back 返回上一个位置
register-back-in-listener: false

# /back 命令的冷却时间（秒）
back-cooldown: 30

# ---- 死亡后"自动弹出返回死亡点"提示的控制 ----
# 该提示有两个来源，按玩家客户端区分：
#
# 1. Java 版玩家：EssentialsX 的 essentials.back.ondeath 权限
#    - 有该权限：死亡后聊天栏自动出现可点击的"返回死亡点"消息
#    - 关闭方法（保留 essentials.back，/back 指令仍可手动使用）：
#      lp group default permission set essentials.back.ondeath false
#      或彻底取消：lp group default permission unset essentials.back.ondeath
#
# 2. 基岩版玩家：BedrockPlayerSupport 的"死亡回传"表单
#    - 配置在 plugins/BedrockPlayerSupport/config.yml：
#      form:
#        back:
#          enable: false   # 设为 false：重生后不再自动弹出"返回死亡地点"表单

# ==================== 家园系统 ====================

# 每个玩家可设置的家数量上限（默认值，可通过权限覆盖）
# 权限格式: essentials.sethome.multiple.<数量>
sethome-multiple:
  default: 3 # 默认玩家可设 3 个家
  admin: 9999 # 管理员/服主可设无数个家（用大数近似无限）

# 设置家时的冷却时间（秒）
sethome-cooldown: 0

# 传送回家时的冷却时间（秒）
home-cooldown: 30

# 传送回家的费用（配合经济系统）
home-teleport-cost: 0

# ==================== 经济系统 ====================

# 是否启用经济系统
enable-economy: true

# 货币符号
currency-symbol: '¥'

# 新玩家初始余额
starting-balance: 0

# 玩家最大余额上限（-1 表示无上限）
max-money: 10000000000000

# 是否允许负余额（欠钱）
allow-negative-balance: false

# 是否启用告示牌商店
enable-sign-shops: true

# 是否启用命令收费（可在下方配置具体命令费用）
command-costs:
  # 示例：使用 /home 收费 10 元
  # home: 10
  # 示例：使用 /tpa 收费 5 元
  # tpa: 5

# 余额显示格式
balance-format: '&a余额: {0}{1}'

# ==================== 命令冷却系统 ====================

# 为特定命令设置冷却时间（单位：秒）
command-cooldowns:
  # 运行服未启用命令冷却（以下示例均注释）
  # feed: 30 # /feed 冷却 30 秒
  # heal: 60 # /heal 冷却 60 秒
  # home: 70 # /home 冷却 1 分 10 秒
  # tpa: 15 # /tpa 冷却 15 秒
  # spawn: 20 # /spawn 冷却 20 秒
  # back: 30 # /back 冷却 30 秒
  # 支持通配符匹配
  # '*potato*': 30  # 含 potato 的命令冷却 30 秒

# 命令冷却是否在服务器重启后保留
command-cooldown-persistence: true

# ==================== AFK 挂机系统 ====================

afk:
  # 是否启用 AFK 系统
  enabled: true
  # 多少秒无操作后自动进入 AFK 状态
  auto-afk: 300
  # 是否自动踢出 AFK 玩家
  auto-afk-kick: true
  # AFK 多久后被踢出（秒）
  kick-time: 600
  # AFK 时是否冻结玩家位置
  freeze-afk-players: false
  # AFK 时是否禁用物品交互
  disable-item-pickup: true
  # 玩家移动时是否自动取消 AFK
  cancel-on-interact: true
  cancel-on-chat: true
  cancel-on-move: true

# ==================== 聊天系统 ====================

chat:
  # 是否启用本地聊天（只显示附近玩家的聊天）
  radius: -1
  # -1 表示关闭本地聊天，所有聊天全服可见
  # 设置为正数（如 100）则只有 100 格内的玩家能看到聊天

  # 聊天格式（需要 EssentialsChat 模块）
  # 可用变量: {DISPLAYNAME}, {USERNAME}, {WORLDNAME}, {PREFIX}, {SUFFIX}, {MESSAGE}
  format: '&7[{WORLDNAME}] &f<{DISPLAYNAME}&f> {MESSAGE}'

  # 全局聊天前缀（在消息前加 ! 发送全服消息）
  global-prefix: '!'

  # 是否允许聊天颜色代码
  allow-color-codes: true

  # 是否过滤不当内容
  filter-enabled: false
  # 过滤词列表
  filter-list:
    - '不当词汇1'
    - '不当词汇2'

# ==================== 昵称系统 ====================

# 是否允许玩家修改昵称
allow-nicknames: true

# 昵称最大长度
max-nick-length: 15

# 昵称前缀（显示在昵称前的符号）
nickname-prefix: '~'

# 是否允许昵称使用颜色代码
allow-color-nicknames: true

# 昵称格式
nickname-format: '{DISPLAYNAME}'

# ==================== 工具包系统 ====================

# 工具包领取冷却时间（秒）- 默认值
kit-cooldown: 86400

# 是否允许一次性领取多个工具包
kit-once: true

# 是否在领取工具包时清空背包
kit-clear-on-receive: false

# ==================== 物品管理 ====================

# 是否允许使用 /repair 修复物品
repair-enabled: true

# 是否允许修复附魔物品
repair-enchanted: true

# /give 命令的最大数量限制
max-give-amount: 640

# ==================== 管理员工具 ====================

# /kick 默认原因
kick-default-reason: '&c你已被踢出服务器'

# /ban 默认原因
ban-default-reason: '&c你已被永久封禁'

# 临时封禁默认原因
tempban-default-reason: '&c你已被临时封禁'

# /mute 默认时长（秒），0 表示永久禁言
mute-default-duration: 0

# /jail 默认原因
jail-default-reason: '&c你已被关禁闭'

# ==================== 世界与重生点 ====================

# 是否启用多世界重生点支持（需要 EssentialsSpawn 模块）
spawn-on-join: false

# 首次加入时是否传送到 firstspawn
first-spawn-on-join: true

# 死亡后是否重生在 spawn 点
respawn-at-spawn: true

# ==================== 保护模块 ====================

# 以下设置需要 EssentialsProtect 模块

protect:
  # 防止 TNT 爆炸破坏
  prevent-tnt-explosion: true
  # 防止苦力怕爆炸破坏
  prevent-creeper-explosion: true
  # 防止床爆炸破坏（下界/末地）
  prevent-bed-explosion: true
  # 防止末影水晶爆炸破坏
  prevent-ender-crystal-explosion: true
  # 防止火焰蔓延
  prevent-fire-spread: true
  # 防止熔岩点燃方块
  prevent-lava-fire: true
  # 防止树叶腐烂
  prevent-leaf-decay: false
  # 防止冰融化
  prevent-ice-melt: false
  # 防止雪融化
  prevent-snow-melt: false
  # 防止末影人搬运方块
  prevent-enderman-pickup: true
  # 防止作物被踩踏
  prevent-crop-trample: true
  # 防止活塞推动方块到保护区
  prevent-piston-push: false

# ==================== 建筑权限（AntiBuild）====================

# 以下设置需要 EssentialsAntiBuild 模块

antibuild:
  # 是否启用建筑权限检查
  enabled: false
  # 未授权时是否显示提示消息
  notify-on-violation: true
  # 是否阻止未授权玩家放置方块
  prevent-place: true
  # 是否阻止未授权玩家破坏方块
  prevent-break: true
  # 是否阻止未授权玩家使用物品
  prevent-use: true

# ==================== GeoIP 地理位置 ====================

# 以下设置需要 EssentialsGeoIP 模块

geoip:
  # 是否在玩家加入时显示地理位置
  show-on-join: false
  # 是否仅对管理员显示
  admin-only: true

# ==================== Discord 集成 ====================

# 以下设置需要 EssentialsDiscord 模块
# 详细配置请参考 EssentialsDiscord 模块文档

discord:
  # Discord 机器人 Token（需自行创建机器人获取）
  token: ''
  # 主频道 ID
  primary-channel: ''
  # 是否启用聊天同步
  chat-sync: true
  # 是否启用加入/离开通知
  join-leave-notifications: true
  # 是否启用死亡通知
  death-notifications: true

# ==================== 数据存储 ====================

# 数据存储方式: yaml / mysql / sqlite
database:
  type: yaml

  # MySQL 配置（仅在 type 为 mysql 时生效）
  mysql:
    host: 'localhost'
    port: 3306
    database: 'essentials'
    username: 'root'
    password: ''
    # 连接池大小
    pool-size: 10

# ==================== 性能优化 ====================

# 是否异步处理数据库操作（推荐开启）
async-database: true

# 玩家数据自动保存间隔（秒）
auto-save-interval: 300

# 是否清理不活跃玩家数据
cleanup-inactive-players: false
# 不活跃天数阈值
inactive-days: 90

# ==================== 消息与语言 ====================

# 语言文件（中文）
locale: zh

# 是否在控制台显示玩家命令
log-command-executions: false

# 是否显示玩家加入/离开消息
show-join-quit: true

# 自定义加入 / 退出广播（覆盖原版 "X 加入了游戏 / X 离开了游戏"）
# 设为 "none" 沿用原版；可用占位符 {PLAYER} {USERNAME} {ONLINE} {UPTIME} 等
custom-join-message: 'none'
custom-quit-message: 'none'
# 改名后重进专用（一般保持 none 复用上方两条）
custom-new-username-message: 'none'
# 在线人数超过该值时隐藏加入/退出消息；-1 = 始终显示
hide-join-quit-messages-above: -1

# ==================== 互通服专用设置 ====================

# 以下配置针对 GeyserMC + Floodgate 互通服优化

# 是否允许基岩版玩家使用所有指令
# Floodgate 玩家默认拥有与 Java 玩家相同的权限
# 可通过 LuckPerms 为 Floodgate 玩家单独设置权限

# 是否将基岩版玩家的指令输入记录到日志
log-bedrock-commands: false

# 基岩版玩家默认家园数量（可通过权限覆盖）
bedrock-home-limit: 3
```

---

### 📄 kits.yml 工具包配置示例

文件路径：`plugins/Essentials/kits.yml`

```yaml
kits:
  # 新手工具包
  starter:
    delay: 86400 # 冷却 24 小时（秒）
    items:
      - IRON_SWORD 1
      - IRON_PICKAXE 1
      - IRON_AXE 1
      - IRON_SHOVEL 1
      - COOKED_BEEF 32
      - OAK_PLANKS 64
      - TORCH 32

  # 每日奖励工具包
  daily:
    delay: 86400 # 冷却 24 小时
    items:
      - GOLDEN_APPLE 2
      - DIAMOND 3
      - EXPERIENCE_BOTTLE 10
```

---

### 📄 commands.yml 自定义命令别名

文件路径：`plugins/Essentials/commands.yml`

```yaml
aliases:
  # 快捷传送命令
  家:
    - 'essentials:home'
  回家:
    - 'essentials:home'
  设置家:
    - 'essentials:sethome'
  传送:
    - 'essentials:tpa $1-'
  接受:
    - 'essentials:tpaccept'
  拒绝:
    - 'essentials:tpdeny'
  余额:
    - 'essentials:balance'
  转账:
    - 'essentials:pay $1-'
  工具包:
    - 'essentials:kit'
  出生点:
    - 'essentials:spawn'
```

---

### 🔧 LuckPerms 权限配置示例

以下是配合 LuckPerms 的权限分配示例：

```
# 默认玩家组权限
lp group default permission set essentials.home true
lp group default permission set essentials.sethome true
lp group default permission set essentials.sethome.multiple.3 true
lp group default permission set essentials.tpa true
lp group default permission set essentials.tpaccept true
lp group default permission set essentials.tpdeny true
lp group default permission set essentials.spawn true
lp group default permission set essentials.balance true
lp group default permission set essentials.pay true
lp group default permission set essentials.kit true
lp group default permission set essentials.back true
# 死亡后是否自动弹出"点击返回死亡点"提示，由 essentials.back.ondeath 控制：
#   关闭（不自动弹出，/back 仍可用）：lp group default permission set essentials.back.ondeath false
#   彻底取消：lp group default permission unset essentials.back.ondeath
lp group default permission set essentials.msg true
lp group default permission set essentials.nick true
lp group default permission set essentials.help true
lp group default permission set essentials.list true
lp group default permission set essentials.motd true
lp group default permission set essentials.rules true

# 管理员组权限
lp group admin inheritance add default
lp group admin permission set essentials.* true
lp group admin permission set essentials.god true
lp group admin permission set essentials.fly true
lp group admin permission set essentials.heal true
lp group admin permission set essentials.feed true
lp group admin permission set essentials.kick true
lp group admin permission set essentials.ban true
lp group admin permission set essentials.tempban true
lp group admin permission set essentials.mute true
lp group admin permission set essentials.jail true
lp group admin permission set essentials.invsee true
lp group admin permission set essentials.vanish true
lp group admin permission set essentials.give true
lp group admin permission set essentials.enchant true
lp group admin permission set essentials.repair true
lp group admin permission set essentials.eco true
lp group admin permission set essentials.setwarp true
lp group admin permission set essentials.setspawn true
```

---

### ⚠️ 注意事项

- **首次启动**服务器后，EssentialsX 会自动生成默认配置文件，建议用上述配置**覆盖**默认文件
- 配置修改后使用 `/essentials reload` 热重载，无需重启服务器
- 经济功能**必须安装 Vault** 插件才能正常工作
- 互通服中基岩版玩家通过 Floodgate 登录后，权限系统与 Java 玩家统一，可通过 LuckPerms 的 `floodgate` 前缀为基岩版玩家单独设置权限
- 建议定期备份 `plugins/Essentials/` 文件夹，防止数据丢失
- 如需使用中文消息提示，确保 `plugins/Essentials/messages/messages_zh.properties` 文件存在

