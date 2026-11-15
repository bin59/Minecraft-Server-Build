# 三、基础指令与经济系统

## EssentialsX

> 本服通用基础插件套件。注意：家园系统（/home /sethome /delhome）已禁用；/back 已取消。

### 玩家命令

| 命令 | 语法 / 参数 | 说明 |
|---|---|---|
| 传送请求 | `/tpa <玩家>` | 请求传送到对方位置 |
| 反向传送请求 | `/tpahere <玩家>` | 请求对方传送到自己 |
| 接受请求 | `/tpaccept` | 接受收到的传送请求 |
| 拒绝请求 | `/tpdeny` | 拒绝收到的传送请求 |
| 公共传送点 | `/warp [名称]` | 不带参数查看列表；带名称传送到该传送点 |
| 出生点 | `/spawn` | 传送到服务器出生点 |
| 首次出生点 | `/firstspawn` | 传送到首次加入出生点 |
| 查余额 | `/balance` 或 `/bal` | 查询自己的南瓜币余额 |
| 财富榜 | `/balancetop` 或 `/baltop` | 查看财富排行榜 |
| 转账 | `/pay <玩家> <金额>` | 转账给其他玩家（受 EzTax 交易税影响） |
| 工具包 | `/kit [名称]` | 不带参数查看可用工具包；带名称领取 |
| 私信 | `/msg <玩家> <消息>` | 给玩家发私信（别名 /whisper / /w） |
| 回复私信 | `/r <消息>` 或 `/reply <消息>` | 快速回复上一条私信 |
| 昵称 | `/nick <昵称>` | 设置自己的显示名 |
| 忽略玩家 | `/ignore <玩家>` | 屏蔽某玩家聊天 |
| 戴帽子 | `/hat` / `/hat remove` | 把手持物品戴到头盔槽；remove 摘下 |
| 挂机 | `/afk` | 标记自己为挂机状态 |

基岩版玩家（Geyser/Floodgate）命令用法与 Java 完全一致；若输入不便可用快捷菜单按钮替代。

### 管理员命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 直接传送 | `/tp <玩家>` / `/tphere <玩家>` | 无需对方同意的传送/拉人 |
| 经济调整 | `/eco give <玩家> <金额>` / `/eco take <玩家> <金额>` / `/eco reset <玩家>` | 加钱 / 扣钱 / 重置余额 |
| 踢出 | `/kick <玩家> [原因]` | 踢出玩家 |
| 封禁 | `/ban <玩家> [原因]` / `/tempban <玩家> <时长> [原因]` / `/unban <玩家>` | 永久/临时封禁 / 解封 |
| 禁言 | `/mute <玩家> [时长]` / `/unmute <玩家>` | 禁言 / 解除禁言 |
| 模式与状态 | `/god` / `/fly` / `/heal [玩家]` / `/feed [玩家]` / `/gamemode <模式> [玩家]` | 无敌 / 飞行 / 治疗 / 回饱食 / 切游戏模式 |
| 查询 | `/whois <玩家>` / `/seen <玩家>` / `/invsee <玩家>` / `/vanish` | 玩家信息 / 最后在线 / 查看背包 / 隐身 |
| 传送点管理 | `/setwarp <名称>` / `/delwarp <名称>` | 设置 / 删除公共传送点 |
| 出生点管理 | `/setspawn` / `/setfirstspawn` | 设置当前世界 / 首次出生点 |
| 物品 | `/item <物品> [数量]`（`/i`）/ `/give <玩家> <物品> [数量]` / `/enchant <附魔> [等级]` / `/repair` | 给物品 / 给玩家物品 / 附魔 / 修复 |
| 重载 | `/ess reload` | 热重载配置，无需重启 |

> 注意：服务器根目录 commands.yml 已把 /give /enchant /gamemode /tp 等指向 minecraft: 原版命名空间，因此这些命令走原版逻辑，不会触发 Essentials 版本。

### 权限节点

| 节点 | 说明 |
|---|---|
| `essentials.tpa` `essentials.tpaccept` `essentials.tpdeny` | 传送请求相关 |
| `essentials.spawn` `essentials.warp` | 出生点 / 公共传送 |
| `essentials.balance` `essentials.pay` `essentials.balancetop` | 经济查询与转账 |
| `essentials.kit` `essentials.kits.<包名>` | 工具包 |
| `essentials.msg` `essentials.nick` `essentials.ignore` `essentials.hat` | 聊天 / 昵称 / 忽略 / 帽子 |
| `essentials.afk` `essentials.help` `essentials.list` `essentials.motd` `essentials.rules` | 通用 |
| `essentials.eco` `essentials.god` `essentials.fly` `essentials.heal` `essentials.feed` | 管理状态 |
| `essentials.kick` `essentials.ban` `essentials.tempban` `essentials.mute` `essentials.invsee` `essentials.vanish` `essentials.give` `essentials.enchant` `essentials.repair` `essentials.setwarp` `essentials.setspawn` | 管理操作 |
| `essentials.home` `essentials.sethome` `essentials.delhome` | 本服禁用，不授予 |

> 本服 default 组已授予上述玩家节点；admin 组持有 essentials.* 及全部管理节点。

### 典型工作流

**玩家视角（传送 + 经济 + 工具包）**
1. 想找人一起玩：`/tpa Steve` → Steve 输入 `/tpaccept` 即可互传。
2. 查钱与转账：`/bal` 看余额；`/pay Alex 50` 给好友转 50 南瓜币（转账会被 EzTax 按转出方抽 1% 税）。
3. 领每日补给：每 6 小时 `/kit daily` 一次，内含面包×2 + 南瓜币×5，一天最多 4 次。
4. 设昵称：`/nick 南瓜王` 自定义显示名。

**管理员视角**
1. 处理违规：`/kick Steve 广告` 或 `/tempban Steve 1d 违规`。
2. 经济干预：`/eco give Steve 1000` 补发奖励；`/eco take Steve 500` 处罚。
3. 建公共服务点：站到目的地输入 `/setwarp shop`，玩家即可 `/warp shop` 到达。

### 版本信息

最新稳定版 EssentialsX 2.22.0（2026-05-31）；本服当前运行 2.22.1-dev+28 开发构建 / 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/essentialsx/version/2.22.0

---

## Vault

> Vault 是纯服务端**桥接 API**（经济 / 权限 / 聊天接口），本身不存钱、不管权限，只把各玩法插件统一接到 EssentialsX（经济核心）与 LuckPerms（权限核心）上。玩家与管理员**无 Vault 直接命令可用**，能感知的是：装好 Vault 后，各玩法插件才能"找得到钱"——余额、转账、商店买卖、税收等全部走同一本 EssentialsX 账（货币名"南瓜币"）。

### 版本信息

本服使用经典 Vault 桥（最后发布 1.7.3，已停滞）。当前维护的等价 drop-in 替换是 VaultUnlocked 2.20.3（2026-09-16，向后兼容原 Vault，支持 Folia / 26.x）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/vaultunlocked

---

## AuctionHouse（拍卖行）

> 拍卖行（iliüs 版），本服**主交易渠道**。玩家上架物品设定价格，他人可竞价或一口价购买。货币为南瓜币。

### 玩家命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 打开拍卖行 | `/ah` | 打开拍卖行界面（浏览 / 上架 / 竞拍 / 购买） |
| 管理拍卖行 | `/ah admin` | 管理员管理拍卖行（下架违规物品等） |

界面内操作：点"上架"放入手中物品 → 设一口价（或起拍价+竞价） → 确认。其他玩家在 /ah 里右键购买或出价。

### 权限节点

- `auction.*` —— 全部权限，**默认对玩家开放**。

### 典型工作流

**玩家视角（上架 + 竞拍）**
1. 出售多余物品：手持物品打开 /ah → 选择上架 → 设价 → 确认。上架即扣 30 南瓜币上架费（不退）。
2. 被买走后：成交价自动入账，但卖家到手需扣 5% 成交税。
3. 淘货：在 /ah 浏览分类，右键一口价直接买，或对其他拍卖出价竞拍。

**管理员视角**：用 /ah admin 下架违规/欺诈上架。

### 版本信息

最新版 AuctionHouse by ilius 1.1.4（2026-07-23）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/auctionhouse-by-ilius/version/1.1.4

---

## EconomyShop（玩家商店）

> 玩家商店（iliüs 版），与拍卖行互补的玩家间交易渠道。防印钞设计：关闭"卖物品给系统商店"与 /sell，只能玩家间买卖。货币为南瓜币。

### 玩家命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 打开商店 | `/shop` | 打开个人商店 GUI |
| 木牌商店 | 无命令（箱子 + `[Shop]` 牌） | 创建木牌商店 |
| 管理员重载 | `/shop admin reload` | 重载配置 |
| 管理员面板 | `/shop admin` | 进入系统商店管理 |

**玩家开店步骤（方式 A：木牌商店，推荐）**
1. 在地上放一个**箱子**。
2. 在箱子旁 / 正上方放**告示牌**，第一行写 `[Shop]`。
3. 自动弹出设置界面 → 放入要卖的物品 → 设**买价** → 确认。
4. 牌子自动显示物品名与价格；其他玩家**右键点牌子**即可购买。
5. 补货：打开箱子放入更多物品即可。开店瞬间扣 100 南瓜币一次性开店费；每笔成交抽 5% 税。

### 权限节点（LuckPerms default 组，已授予）

- `economyshop.use` / `economyshop.sell` / `economyshop.chestshop.create` / `economyshop.chestshop.use`

### 典型工作流

**玩家视角（开店卖货）**：备货后放箱子 + 写 [Shop] 牌子，设好买价 → 他人右键购买，钱直接进卖家余额；不能把物品卖给系统（`sell-to-shop: false`）。

**管理员视角（上架系统特殊物品）**：用 1.21+ 数据组件语法造特殊物品 → 手持该物品 `/shop admin` → 进"自定义物品" → 抓取手中 ItemStack → 设买价确认 → `/shop admin reload`。

### 版本信息

最新版 EconomyShop by ilius 1.1.6（2026-07-14）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/economyshop/version/1.1.6

---

## EzTax（税务）

> 税务插件（南瓜生存服经济体系强制货币流动工具）。对高余额账户征持有税、交易税、死亡费、闲置费，并支持按权限组分档税率。货币为南瓜币。

### 玩家命令

| 命令 | 语法 | 说明 |
|---|---|---|
| 主界面 / 帮助 | `/tax`（别名 /eztax） | 打开税务主界面 |
| 预览税额 | `/tax check` | 预览自己下次应纳税额 |
| 查看统计 | `/tax stats` | 查看累计税务统计 |
| 补缴 | `/tax pay` / `/tax payfine` | 补缴税款 / 罚金 |

### 管理员命令

| 命令 | 说明 |
|---|---|
| `/tax config` | 打开游戏内配置 GUI |
| `/tax reload` | 重载配置，无需重启 |
| `/tax runwealthtax` | 手动触发持有税（测试用） |
| `/tax settaxrate [组]` | 按分组设置税率 |
| `/exempt <玩家>` | 免税（独立命令） |
| `/transactiontax <view|setpercent|setmin|enable|disable>` | 交易税配置 |

### 权限节点（LuckPerms）

- `eztax.command.use` / `check` / `paytax` / `showtax` / `history` / `stats` —— 玩家默认可用
- `eztax.command.reload` / `runwealthtax` / `settaxrate` / `transactiontax.manage` / `exempt` —— 管理员（op）

> 本服现行税率：交易税转出方 1%（最低 1 币）、持有税每日对 >25 万部分 2%、死亡费 0.1%（上限 500）、闲置费离线 30 天扣 500。税款转入指定账户 PumpkinVegetable。

### 版本信息

本服版本与官方最新一致；以本地配置为准 / 查证日期 2026-11-13

---

## Quests（每日任务）

> 每日任务插件（PikaMug / Quests Classic），本服经济体系**合规水龙头**：玩家完成每日任务获得南瓜币，是新手获取初始资金主渠道。奖励经 Vault 发放。

### 玩家命令

| 命令 | 别名 | 语法 | 说明 |
|---|---|---|---|
| 任务界面 | — | `/quests` | 插件帮助 / 任务界面 |
| 列出可接任务 | — | `/quests list` | 列出当前可接任务 |
| 接取任务 | — | `/quests take <ID>` | 接取指定任务（任务名需连写无空格） |
| 放弃任务 | — | `/quests quit` | 放弃当前任务 |
| 当前任务目标 | `/q` | `/quest` | 查看当前任务目标进度 |
| 统计 / 排行 | — | `/quest stats` / `/quest top` | 统计 / 财富排行 |

### 管理员命令

| 命令 | 别名 | 说明 |
|---|---|---|
| 管理命令 | `/qa` / `/questsadmin` | 重载、编辑、发放等（`quests.admin`） |

### 权限节点（LuckPerms）

- 玩家：`quests.list` / `take` / `quit` / `stats` / `top` / `info` —— 默认授权
- 管理员：`quests.admin.*`

### 典型工作流

**玩家视角（接每日任务赚币）**：`/quests list` 看当天 6 个可选任务 → `/quests take 每日挖矿人` 接取 → 去做任务目标 → 完成自动发奖（经 Vault 入账南瓜币）→ 每个任务 24 小时冷却。

**每日任务清单（14 个，按星期轮换，每天 6 个，全天约 250~260 币）**

| 任务 ID | 名称 | 条件 | 奖励 | 出现日 |
|---|---|---|---|---|
| dailyminer | 每日挖矿人 | 铁矿石8+深板岩铁矿石8 | 50 | 一/三/五 |
| dailyhunter | 每日猎手 | 击杀僵尸10 | 50 | 二/四/六 |
| dailyfisher | 每日渔夫 | 钓鱼5条 | 40 | 三/五/日 |
| dailywoodcutter | 每日伐木工 | 砍橡木原木16 | 40 | 四/六/一 |
| dailycook | 每日厨师 | 熔炉取出铁锭16 | 40 | 五/日/二 |
| dailytamer | 每日驯养师 | 驯服狼2只 | 50 | 六/一/三 |
| dailyfarmer | 每日农夫 | 收获小麦16 | 40 | 日/二/四 |
| dailywalker | 每日远行者 | 放置火把16 | 40 | 一/三/五 |
| dailyquarrier | 每日采石匠 | 挖石头16+深板岩16 | 40 | 二/四/六 |
| dailywarrior | 每日讨伐者 | 击杀骷髅10 | 40 | 三/五/日 |
| dailyshepherd | 每日牧羊人 | 剪白色绵羊5只 | 40 | 四/六/一 |
| dailymilker | 每日挤奶工 | 挤奶3头 | 40 | 五/日/二 |
| dailycraftsman | 每日工匠 | 制作箱子4个 | 40 | 六/一/三 |
| dailyenchanter | 每日附魔师 | 附魔铁剑2把 | 50 | 日/二/四 |

### 版本信息

最新版 Quests Classic 5.3.2（2026-08-04）/ 查证日期 2026-11-13 / 来源 https://modrinth.com/plugin/quests.classic/version/fD1cWOQu

---

## 南瓜币获取方式（收入总览）

> 本服货币为"南瓜币"，统一存于 EssentialsX 单账本（经 Vault 桥接）。

| 途径 | 入口命令 | 获取方式 | 每天数量 | 频率 / 上限 |
|---|---|---|---|---|
| 每日任务 | `/quests` | 完成当日轮换的 6 个任务 | 约 250~260 币 | 每任务 24h 冷却 |
| 每日随机收购 | `/ds choose` → `/ds sell` | 从 8 种候选自选 5 种，出售自采物品 | 单人每日上限 900 币 | 每天 0 点刷新 |
| 每日工具包 | `/kit daily` | 领取每日包（面包×2 + 南瓜币×5） | 5 币/次 × 最多 4 次 | 每 6 小时可领 1 次 |
| 离线奖励 | 上线自动发放（进南瓜邮箱） | 按离线时长结算 | 15 币/小时（单次最多计 24h） | 上线自动到账 |
| 在线奖励 | 在线阶段达成自动发（进南瓜邮箱） | 按今日累计在线分 8 档 | 300 币/天封顶 | 3 小时毕业 |
| 南瓜邮箱活动奖励 | `/mailbox` | 管理发放活动 / 补偿 | 不定期 | 上线有提示 |

---

## 服务器扣费点清单（支出总览）

| 插件 | 扣费点 | 触发条件 | 扣费规则 |
|---|---|---|---|
| EzTax | 交易税 | 玩家转账/取款 | 金额 1%，最低 1 币 |
| EzTax | 财富税 | 每日定时，余额 > 250000 | 超出部分 2% |
| EzTax | 离线费 | 离线满 30 天 | 固定 500 币 |
| EzTax | 死亡费 | 每次死亡 | 余额 0.1%（上限 500 币） |
| AuctionHouse | 上架费 | 每次上架一件 | 固定 30 币（不退） |
| AuctionHouse | 成交税 | 拍卖成交 | 成交价 5% |
| EconomyShop | 商店创建费 | 开一个箱子商店 | 固定 100 币（一次性） |
| EconomyShop | 交易税 | 玩家商店买卖成交 | 5% |
| SpacePortal | 传送钻石 | 每次传送阵传送 | 消耗 5 颗钻石（物品，不走 Vault 余额） |
