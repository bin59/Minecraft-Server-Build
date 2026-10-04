# 26. 快捷菜单系统（QuickMenu 自研插件）

> **一句话**：玩家右键一个触发物品，Java 端弹出箱子 GUI，基岩端弹出原生 Form 表单，
> 一套配置同时驱动两端，菜单项点击后执行的动作完全一致。
>
> **菜单已整合本服全部插件**：27 套菜单 / 215 个菜单项，分**玩家线**（15 套）
> 与**管理线**（12 套），管理入口靠权限门控，普通玩家看不到。

插件 jar：`dist/QuickMenu-1.0.0.jar`，复制到服务器 `plugins/` 即可部署。

> 相关文档：`快捷菜单系统-QuickMenu-与通用菜单对比.md`（为什么不用 DeluxeMenus 等通用插件、不可替代点与边界）

**插件与菜单的对应关系**（本菜单已覆盖的后端）：

| 插件                 | 玩家菜单                                  | 管理菜单                   |
| -------------------- | ----------------------------------------- | -------------------------- |
| EssentialsX          | 传送 / 经济 / 工具包 / 社交 / 信息 | 玩家管理 / 处罚 / 传送管理 |
| Residence            | 我的领地                                  | 领地管理                   |
| SkinsRestorer        | 皮肤管理                                  | —                          |
| Simple Voice Chat    | 语音聊天                                  | 服务器监控（语音管理）     |
| EasyBot              | 社交（绑定 QQ）                           | 服务器监控（重载）         |
| CoreProtect          | —                                         | 审计与回滚                 |
| OpenInv              | —                                         | 玩家管理（查背包）         |
| Chunky               | —                                         | 世界管理                   |
| WorldEdit            | —                                         | 世界管理                   |
| spark                | —                                         | 服务器监控                 |
| Geyser / ViaVersion  | —                                         | 服务器监控                 |
| LuckPerms            | —                                         | 权限管理                   |
| BedrockPlayerSupport | 传送 / 皮肤（表单）                | —                          |
| **PumpkinMail（自研）** | 南瓜邮箱（/mailbox，领活动奖励）    | 南瓜邮箱管理（/mailbox admin 发放面板，权限 `pumpkinmail.admin`） |

---

## 1. 两端界面策略

**Java 玩家用箱子 GUI，基岩玩家用原生 SimpleForm 表单**，不强制基岩端也看箱子。
原因：Geyser 把服务端箱子界面翻译成基岩界面存在物品可被拖出、移动端高亮异常、
特定环境打不开、只能左键交互等缺陷；原生 Form 由基岩客户端自己渲染，点击/滚动/
按钮反馈全部正常。

```
            手持触发物品 → 右键
                    ↓
        判断是否 Floodgate 玩家
        ┌───────────┴───────────┐
    Java 玩家               基岩玩家
        ↓                       ↓
   箱子 GUI                原生 SimpleForm
  （物品图标排版）          （按钮列表）
        └───────────┬───────────┘
                    ↓
         共用同一套动作执行器
```

> 若确实要让基岩玩家也看箱子，把 `config.yml` 的 `platform.bedrock-native-form`
> 改为 `false` 即可回退，但会继承上述 Geyser 缺陷。插件在箱子界面里已做**无条件取消
> 点击 + 拦截拖拽**的防护，可缓解物品被拖出的问题。

实现原理：Floodgate 自带 Cumulus 表单 API，普通 Bukkit 插件即可调用，无需写 Geyser 扩展：

```java
FloodgateApi api = FloodgateApi.getInstance();
if (api.isFloodgatePlayer(uuid)) {
    api.getPlayer(uuid).sendForm(form);   // 发送原生表单
}
```

基岩端两个内置处理：

1. **表单回调在 Netty 网络线程触发**，直接在回调里操作背包会崩溃 → `ActionExecutor.runAsyncSafe()` 统一切回主线程。
2. **原生 Form 按钮不支持 `&` / `§` 颜色码**（会显示乱码）→ 按钮文本统一经 `stripColor()` 去色；表单标题保留颜色。配置项 `button-label` 可单独指定基岩端按钮文本。

---

## 2. 目录结构

```
自研/快捷菜单系统/
├── 快捷菜单系统-QuickMenu.md        ← 本文档
├── build.ps1                        ← 一键构建脚本（无需 Maven）
├── dist/
│   └── QuickMenu-1.0.0.jar          ← ★ 部署用成品，复制这个
├── plugin-src/
│   ├── pom.xml                      ← 可选：Maven / IDE 用
│   └── src/main/
│       ├── java/com/nangua/quickmenu/
│       │   ├── QuickMenuPlugin.java     主类，装配与 Floodgate 探测
│       │   ├── command/QuickMenuCommand.java  /qm 命令与 Tab 补全
│       │   ├── config/                  YAML → 内存对象（启动时一次）
│       │   ├── gui/                    Java 箱子渲染 / 基岩表单渲染 / 界面持有者
│       │   ├── listener/               物品右键 / 进服发放 / 防丢弃 / 点击处理
│       │   └── menu/                   两端分发决策中枢 / 动作执行 / 菜单与动作模型
│       └── resources/
│           ├── plugin.yml              插件描述（命令与权限）
│           └── config.yml              ★ 菜单配置（27 套：玩家 15 + 管理 12）
└── _build/                            构建中间目录
```

---

## 3. 安装部署

### 3.1 环境要求

| 项目       | 要求               | 本服                         |
| ---------- | ------------------ | ---------------------------- |
| 服务端核心 | Paper 或其分支     | ✅ Leaf 1.21.11（Paper 分支） |
| Java       | 21+                | ✅ 已装 JDK 21                |
| Minecraft  | 1.21.x             | ✅ 1.21.11                    |
| Floodgate  | 基岩端原生表单必需 | 已装（见互通层章节）          |

### 3.2 部署步骤

```
1. 复制 dist\QuickMenu-1.0.0.jar  →  服务器 plugins\ 目录
2. 重启服务器
3. 首次启动自动生成 plugins\QuickMenu\config.yml
4. 按需修改配置，然后 /qm reload 热重载（无需重启）
```

> **未装 Floodgate 也能正常启动**：只在确认 Floodgate 可用后才触碰 Cumulus 相关类，
> 纯 Java 服务器自动跳过基岩端渲染，全部玩家走箱子界面。

### 3.3 权限授予（必须做，否则打不开菜单）

```bash
# 菜单本身的使用权限（default 已是 true，通常无需设置）
lp group default permission set quickmenu.use true

# 管理面板入口权限：给了才能在主菜单看到「管理面板」
lp group admin permission set quickmenu.admin true
lp group owner permission set quickmenu.admin true
```

> **菜单能打开但点了没反应 / 提示无权限**，几乎都是只给了 `quickmenu.use`
> 而漏了底层插件权限。菜单只是前端，真正干活的是 EssentialsX / Residence /
> SkinsRestorer 等后端插件。完整授权命令见 [第 8 章 LuckPerms 权限授予清单](#8-luckperms-权限授予清单)。

---

## 4. 命令与配置

### 4.1 命令一览

| 命令                | 作用                             | 权限                          |
| ------------------- | -------------------------------- | ----------------------------- |
| `/qm`               | 打开默认菜单                     | `quickmenu.use`（默认所有人） |
| `/qm open <菜单id>` | 打开指定菜单                     | `quickmenu.use`               |
| `/qm give [玩家]`   | 发放触发物品                     | `quickmenu.admin`             |
| `/qm reload`        | 热重载配置                       | `quickmenu.admin`             |
| `/qm list`          | 列出已加载菜单                   | `quickmenu.admin`             |
| `/qm info`          | 显示客户端类型与设备信息（调试） | `quickmenu.use`               |

别名：`/quickmenu`。全部命令带 Tab 补全（子命令、菜单 id、在线玩家名）。

### 4.2 配置结构

`plugins/QuickMenu/config.yml` 分四块：

| 节点                 | 作用                                 |
| -------------------- | ------------------------------------ |
| `platform`           | 两端策略开关                         |
| `trigger-item`       | 触发物品的材质、名称、描述、发放方式 |
| `sound` / `messages` | 音效与全部提示文本（可本地化）       |
| `menus`              | **菜单定义**（核心）                 |

### 4.3 菜单项字段说明

```yaml
menus:
  main: # ← 菜单 id
    title: '&6快捷菜单' # 界面标题（两端通用，支持 & 颜色码）
    bedrock-content: '&7说明文字' # 基岩表单标题下的说明（留空则用 title）
    size: 27 # 箱子容量，必须是 9 的倍数，范围 9~54
    back-menu: '' # 上级菜单 id，配置后显示返回按钮
    filler: # 箱子空白处的填充物品
      material: BLACK_STAINED_GLASS_PANE
      name: '&8'

    items:
      kit: # ← 菜单项 id（唯一）
        material: STONE_PICKAXE # 物品材质（仅 Java 箱子用）
        display-name: '&a新手工具包' # 显示名（两端通用）
        lore: # 描述行（仅 Java 箱子用）
          - '&7石头工具 + 皮装备 + 基础物资'
        slot: 11 # 箱子格子序号，从 0 开始（仅 Java 箱子用）
        glowing: true # 是否发光（无附魔副作用）
        permission: '' # 需要的权限，留空=所有人可见
        button-label: '' # 基岩按钮文本，留空=自动用 display-name 去色
        actions: # 点击后执行的动作
          - '[menu] home'
```

### 4.4 动作类型

**无条件动作**（任何客户端都执行）：

| 前缀        | 作用                                   | 示例                       |
| ----------- | -------------------------------------- | -------------------------- |
| `[player]`  | 以玩家身份执行命令（**受权限约束**）   | `[player] home`            |
| `[console]` | 以控制台身份执行（**无视权限**，慎用） | `[console] eco give X 100` |
| `[menu]`    | 打开另一个菜单                         | `[menu] teleport`          |
| `[message]` | 发送消息，支持 `&` 颜色码              | `[message] &a已传送`       |
| `[close]`   | 关闭界面/表单                          | `[close]`                  |
| 无前缀      | 等同 `[player]`                        | `home`                     |

**平台条件动作**（仅对应客户端执行）：

| 前缀                | 作用                         |
| ------------------- | ---------------------------- |
| `[bedrock-player]`  | 仅基岩玩家，以玩家身份执行   |
| `[java-player]`     | 仅 Java 玩家，以玩家身份执行 |
| `[bedrock-console]`  | 仅基岩玩家，控制台身份       |
| `[java-console]`     | 仅 Java 玩家，控制台身份      |

**动态目标动作**：

| 前缀                         | 作用                                                                                    |
| ---------------------------- | --------------------------------------------------------------------------------------- |
| `[player-selector] 命令模板` | 点击后列出在线玩家（排除自己），`{target}` 替换为所选玩家名                            |
| `[item-selector] 命令模板`    | 点击后列出背包物品（自动排除触发物品、同材质合并），`{item}` 替换为材质名（小写）      |
| `[ah-sell] 价格 材质名`       | 拍卖行上架：自动把背包中该材质物品换到主手执行 `/ah sell 价格`，再还原主手（解决触发物品占主手无法直接上架），如 `[ah-sell] 5000 elytra` |

动作可叠加，按列表顺序执行。常见组合：先 `[close]` 关界面，再执行指令，避免指令输出被界面遮挡。

### 4.5 两端分发：同一功能两端指令不同

**不要**把基岩专属指令写成无条件动作：

```yaml
# ❌ 错误：Java 玩家点击后毫无反应（/warpgui 在 Java 端不存在）
actions:
  - '[close]'
  - '[player] warpgui'

# ✅ 正确：两端各自执行自己的指令
actions:
  - '[close]'
  - '[bedrock-player] warpgui'   # 基岩 → BPS 原生传送点表单
  - '[java-player] warp'         # Java → EssentialsX 传送点列表
```

也可给缺功能的一端用 `[message]` 做文字指引降级。玩家选择器示例：

```yaml
# 两端统一走 QuickMenu 玩家选择器：动态列出在线玩家，点谁就发给谁
actions:
  - '[player-selector] tpa {target}'
  # tpahere 同理：- '[player-selector] tpahere {target}'
```

> ⚠️ 构建脚本会自动检查覆盖缺口：某菜单项只配了 `[bedrock-*]` 而 Java 端连
> `[message]` 都没有，会告警「该动作对 Java 玩家无任何有效反馈」。`[close]` 不算有效反馈。

### 4.6 权限过滤行为

菜单项配了 `permission` 且玩家无权时，**该项直接被跳过**，而不是显示为灰色不可点：

- Java 端：该槽位显示填充物品
- 基岩端：该按钮不出现，**后续按钮序号自动前移**（代码用 `visibleItems` 按序重映射，避免点 A 执行 B）

---

## 5. 菜单结构总览（27 套，玩家 / 管理双线）

`config.yml` 已配好 **27 套菜单**，把本服所有插件的常用功能全部收进菜单。

### 5.1 玩家菜单（15 套）

| 菜单 id     | 名称       | 对接插件                  | 主要内容                                                                                                                                                                                                                          |
| ----------- | ---------- | ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `main`      | 主菜单     | —                         | 一级入口，功能项 + 管理面板入口。含**宠物系统**（`/pet gui`）与**传送阵**（`/csz gui`）直达项；含拍卖行 / 玩家商店 / 每日任务 / **每日收购**（`/ds`）直达；含**南瓜邮箱**（`/mailbox`，slot 30，第 4 行）领活动奖励                                                                                                |
| `teleport`  | 传送功能   | EssentialsX + BPS         | 公共传送点（warps 子菜单）、申请传送、拉人、回主城、附近玩家                                                                                                                                                                |
| `warps`     | 传送点     | EssentialsX               | 公共传送点子菜单（当前：主城）。经 /setwarp 新增传送点后需在 config.yml 同步添加                                                                                                                                                 |
| `residence` | 我的领地   | **Residence**             | 建/删/传送领地、设置传送点、flag 开关、玩家授权、子领地、进出提示、帮助                                                                                                                                                           |
| `economy`   | 经济中心   | EssentialsX + Vault       | 余额（/balance）、财富榜（/baltop）、转账（/pay）、周持有税查询（/eztax stats）、**每日收购（/ds）**。拍卖行/玩家商店入口在主菜单首屏，不在此菜单                                                                                          |
| `kit`       | 工具包     | EssentialsX               | 新手包 `starter`（一次性）+ 每日包 `daily`（面包×2 + 南瓜币×5，6 小时一次，一天 4 次） |
| `skin`      | 皮肤管理   | **SkinsRestorer**         | 皮肤库浏览（/skins 选择菜单，含皮肤/历史/收藏三入口）、历史皮肤（/skin history）、收藏皮肤（/skin favourites）、清除、刷新、随机、撤销                                                                                              |
| `social`    | 社交设置   | EssentialsX + **EasyBot** | 私信、快速回复、屏蔽、改昵称、查信息、在线列表、绑定 QQ                                                                                                                                                                           |
| `info`      | 服务器信息 | EssentialsX               | 在线列表、公告、规则、互通说明、指令帮助                                                                                                                                                                                          |
| `voice`     | 语音聊天   | **Simple Voice Chat**     | 说话方式、群组语音、音量设置、故障排查                                                                                                                                                                                            |
| `guide`     | 新人指南   | —（说明书）               | 游戏内说明书首页：新人必做 3 件事、赚钱 / 领地 / 出行 / 玩法分册入口、常用指令速查、服务器规则                                                                                                                                  |
| `guide-economy` | 怎么赚钱   | EssentialsX + DailySell + Quests + EconomyShop + AuctionHouse + EzTax | 每日任务、每日收购、玩家商店、拍卖行、转账、余额与财富榜、周持有税                                                                              |
| `guide-build`   | 领地建房   | **Residence**             | 创建领地、传送到领地、设置传送点、邀请玩家、权限开关、子领地、打开领地菜单、卡住脱身                                                                             |
| `guide-move`    | 传送出行   | EssentialsX + SpacePortal | 回主城、公共传送点、申请传送、接受/拒绝传送、传送阵                                                                                             |
| `guide-play`    | 玩法大全   | SimplePets + RideOnHead + SkinsRestorer + SVC + PatPat + EssentialsX | 宠物、骑玩家、皮肤、语音、摸头、工具包、服务器信息                                                                                                               |

> **维护提醒**：EssentialsX 新增传送点（/setwarp）后，需同步在 config.yml 的
> `warps:` 子菜单里手动添加对应菜单项。拍卖行（/ah，slot 9）、玩家商店（/shop，slot 11）
> 在主菜单首屏直达，不放在 `economy` 经济中心子菜单。

**每日收购入口**（配合 DailySell 插件，详见 `自研/每日随机收购/每日随机收购-DailySell.md`）：

- 主菜单 `main` 与 `economy` 经济中心各有一个「每日收购」按钮（均占 **slot 19**，动作 `[player] ds`），配置见 config.yml 模板中 `daily-sell:` 条目
- **部署注意**：服务器上 `plugins/QuickMenu/config.yml` 已存在时，替换 jar 不会更新它——需手动把 `daily-sell:` 两段插入到对应菜单（主菜单 slot 19、经济中心 slot 19），再 `/qm reload` 生效

层级：玩家线所有二级菜单的 `back-menu` 均为 `main`（`warps` 为三级菜单，`back-menu: teleport`），返回按钮固定在右下角（`size-1` 槽位）。`guide` 系列为三级结构：`main` → `guide` → 四个分册，分册的 `back-menu` 均为 `guide`。

### 5.1b 新人指南（游戏内说明书）

**入口**：主菜单 `main` 的「新人指南」按钮（**slot 17**，WRITABLE_BOOK，所有玩家可见），或 `/qm open guide`。

**设计原则**：这是给新玩家的游戏内说明书——不是命令手册，而是"该做什么、怎么做"。分 5 套菜单：

| 菜单 | 内容 | 呈现方式 |
|---|---|---|
| `guide` 首页 | 新人必做 3 件事（领新手包→圈领地→熟悉菜单）、4 个分册入口、常用指令速查、服务器规则 | 说明类按钮 = 关闭界面 + 逐行 `[message]` 图文提示（两端通用） |
| `guide-economy` 怎么赚钱 | 每日任务 / 每日收购 / 玩家商店 / 拍卖行 / 转账 / 余额财富榜 / 周税 | 能直接执行的（`/quests` `/ds` `/balance`）点击即执行；需参数的（`/pay`）给用法提示 |
| `guide-build` 领地建房 | 建领地步骤、传送点、邀请玩家、flag 开关、子领地、卡住脱身 | 建领地给出完整 3 步圈地流程；其余给命令示例 |
| `guide-move` 传送出行 | 回主城、传送点、tpa、传送阵 | 回主城 / 传送阵直接执行，其余给命令示例 |
| `guide-play` 玩法大全 | 宠物、骑玩家、皮肤、语音、摸头、工具包、服务器信息 | 宠物 / 皮肤 / 工具包直接执行，骑玩家 / 摸头给说明 |

**基岩端兼容**：全部说明类按钮走 `[message]`（原生表单按钮不支持 lore），基岩玩家点击同样看到完整说明文字；按钮文本自动去色。

**给新人的完整链路**（建议）：
1. 进服自动发放触发时钟（`give-on-join: true`）+ TAB footer / 公告栏提示"右键时钟打开快捷菜单"；
2. 新人打开主菜单 → 点「新人指南」→ 先看「新人必做 4 件事」；
3. 按需进入 赚钱 / 领地 / 出行 / 玩法 分册学习。

**维护提醒**：新增玩法插件后，在主菜单加对应按钮时，顺手在 `guide-play` 或对应分册补一条说明项，说明书与功能同步更新。

**部署到线上服务器**（config.yml 已存在的服务器不会随 jar 更新）：
1. 用仓库模板 `自研/快捷菜单系统/config.yml` **整体替换**服务器 `plugins/QuickMenu/config.yml`（模板已含全部线上菜单 + guide 系列；若线上有本地化自定义，先备份再替换后手工合并）；
2. 执行 `/qm reload` 即时生效，无需重启；
3. 验证：主菜单 slot 17 出现「新人指南」，`/qm open guide` 可进入。

### 5.2 管理菜单（12 套）

| 菜单 id           | 名称         | 对接插件                                              | 主要内容                                                                                     |
| ----------------- | ------------ | ----------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| `admin`           | 管理面板     | —                                                     | 一级入口，10 个分类 + 返回玩家菜单；含**南瓜邮箱管理**（slot 18，`/mailbox admin` 发放面板，权限 `pumpkinmail.admin`）                                                           |
| `admin-player`    | 玩家管理     | **OpenInv** + EssentialsX                             | 看背包、传送/拉人、切模式、治疗、飞行、无敌、喂食、隐身、清背包、查信息、修复、发物品        |
| `gamemode`        | 切换游戏模式 | EssentialsX                                           | `admin-player` 的子菜单：生存/创造/冒险/旁观四种模式，选模式后再从在线列表选玩家执行        |
| `admin-punish`    | 处罚管理     | EssentialsX                                           | 踢出、封禁、临时封禁、解封、封 IP、禁言、解禁、关押、释放、广播、私信监视                    |
| `admin-teleport`  | 传送管理     | EssentialsX                                           | 强制传送/拉人、全员传送、头顶、设出生点、建/删传送点、静默传送                               |
| `admin-residence` | 领地管理     | **Residence**                                         | 全服领地列表、查看/删除/转移归属、传送、选取他人领地                                         |
| `admin-inspect`   | 审计与回滚   | **CoreProtect**                                       | 查询模式、附近变更、条件查询、回滚、撤销回滚、清理数据库、重载、帮助                         |
| `admin-world`     | 世界管理     | **Chunky** + **WorldEdit** + 原版                     | 时间/天气切换、区块预生成全套、创世神木斧/撤销/重做/复制/粘贴                                |
| `admin-server`    | 服务器监控   | **spark** + **Geyser** + **ViaVersion** + **EasyBot** | TPS、健康报告、延迟、性能采样、堆内存、Geyser 重载/诊断/统计、版本分布、机器人重载、插件列表 |
| `admin-perm`      | 权限管理     | **LuckPerms**                                         | 网页编辑器、同步、重载、权限树、信息、查玩家权限、实时追踪                                   |
| `admin-qm`        | 菜单管理     | QuickMenu 自身                                        | 重载配置、菜单列表、客户端诊断、发放触发物品                                                 |
| `admin-economy` | 经济管理     | EssentialsX + AuctionHouse + EconomyShop + EzTax + DailySell | 财富榜、查余额（选人）、发钱/扣钱/设余额（指引）、拍卖行管理、系统商店管理、税务统计、每日收购重生成/重载 |
| `postracker`       | 玩家位置记录 | **PosTracker**                                        | 位于 `admin` 菜单内（非独立菜单），查询玩家历史位置轨迹（`/pos radius:10 time:1h`）          |

层级：`admin` 的二级菜单共 10 个（`back-menu` 均为 `admin`），`admin-player` 另有三级子菜单 `gamemode`（选模式）；`admin` 的 `back-menu` 为 `main`，
管理员可在两条线之间自由往返。

### 5.2b 经济管理菜单（admin-economy）

管理面板「经济管理」入口（**slot 17**，金锭，仅管理员可见）→ `admin-economy` 菜单（size 27，back 到 admin）。功能与权限：

| 菜单项 | 功能 | 命令 | 权限 |
|---|---|---|---|
| `baltop` | 财富排行榜 | `/baltop` | `essentials.balancetop` |
| `balance` | 查询玩家余额（在线列表选人） | `/balance <玩家>` | `essentials.balance.others` |
| `eco-give` | 给玩家发钱（文字指引） | `/eco give 玩家 金额` | `essentials.eco` |
| `eco-take` | 扣回玩家余额（文字指引） | `/eco take 玩家 金额` | `essentials.eco` |
| `eco-set` | 设置玩家余额（文字指引） | `/eco set 玩家 金额` | `essentials.eco` |
| `ahadmin` | 拍卖行管理（删单/查记录） | `/ahadmin` | `auction.admin` |
| `shopadmin` | 系统商店管理（加物品/定价） | `/shop admin` | `economyshop.admin` |
| `eztax` | 税务统计 | `/eztax stats` | `eztax.stats` |
| `ds-reroll` | 每日收购·重新生成今日清单 | `/ds reroll` | `dailysell.admin` |
| `ds-reload` | 每日收购·重载配置 | `/ds reload` | `dailysell.admin` |

> **部署注意**：服务器上 `plugins/QuickMenu/config.yml` 已存在时替换 jar 不会更新它——需手动插入以下两段（段一进 `admin` 菜单，段二追加到 `menus:` 末尾），再 `/qm reload` 生效。完整 YAML 亦可解压 jar 内嵌 config.yml 复制 `admin-economy` 相关段。

**段一：admin 面板入口（加在 `admin` 菜单 items 里 `admin-qm` 按钮项之后，即管理面板「菜单管理」下方；源码中紧随其后的是 postracker、backmain）**

```yaml
      admin-economy:
        material: GOLD_INGOT
        display-name: '&6&l经济管理'
        lore:
          - '&7查余额、发钱/扣钱、财富榜'
          - '&7拍卖行 / 系统商店 / 税务 / 每日收购'
        slot: 17
        permission: quickmenu.admin
        actions:
          - '[menu] admin-economy'
```

**段二：admin-economy 菜单定义（追加到 `menus:` 末尾）**

```yaml
  admin-economy:
    title: '&6&l经济管理'
    bedrock-content: '&7经济系统管理'
    size: 27
    back-menu: admin
    filler:
      material: YELLOW_STAINED_GLASS_PANE
      name: '&8'

    items:
      baltop:
        material: GOLD_BLOCK
        display-name: '&e财富排行榜'
        lore:
          - '&7查看全服最富有玩家'
        slot: 10
        permission: essentials.balancetop
        actions:
          - '[close]'
          - '[player] baltop'

      balance:
        material: PLAYER_HEAD
        display-name: '&6查询玩家余额'
        lore:
          - '&7点击后从在线列表选择玩家'
        slot: 11
        permission: essentials.balance.others
        actions:
          - '[player-selector] balance {target}'

      eco-give:
        material: EMERALD
        display-name: '&a给玩家发钱'
        lore:
          - '&7需手动补充玩家名与金额'
          - '&e用法：/eco give 玩家名 金额'
        slot: 12
        permission: essentials.eco
        actions:
          - '[close]'
          - '[message] &7发钱：&e/eco give 玩家名 金额'
          - '[message] &7例：&e/eco give Steve 500'

      eco-take:
        material: BARRIER
        display-name: '&c扣回玩家余额'
        lore:
          - '&7需手动补充玩家名与金额'
          - '&e用法：/eco take 玩家名 金额'
        slot: 13
        permission: essentials.eco
        actions:
          - '[close]'
          - '[message] &7扣钱：&e/eco take 玩家名 金额'

      eco-set:
        material: DIAMOND
        display-name: '&b设置玩家余额'
        lore:
          - '&7需手动补充玩家名与金额'
          - '&e用法：/eco set 玩家名 金额'
        slot: 14
        permission: essentials.eco
        actions:
          - '[close]'
          - '[message] &7设余额：&e/eco set 玩家名 金额'

      ahadmin:
        material: GOLD_INGOT
        display-name: '&6拍卖行管理'
        lore:
          - '&7删除违规上架、查看拍卖记录'
        slot: 15
        permission: auction.admin
        actions:
          - '[close]'
          - '[player] ahadmin'

      shopadmin:
        material: CHEST
        display-name: '&e系统商店管理'
        lore:
          - '&7添加/调整商店物品与价格'
        slot: 16
        permission: economyshop.admin
        actions:
          - '[close]'
          - '[player] shop admin'

      eztax:
        material: WRITABLE_BOOK
        display-name: '&c税务统计'
        lore:
          - '&7查看周持有税与交易税征收'
        slot: 17
        permission: eztax.stats
        actions:
          - '[close]'
          - '[player] eztax stats'

      ds-reroll:
        material: EMERALD_BLOCK
        display-name: '&a每日收购·重新生成'
        lore:
          - '&7重新随机今日 4 种收购物品'
        slot: 19
        permission: dailysell.admin
        actions:
          - '[close]'
          - '[player] ds reroll'

      ds-reload:
        material: PAPER
        display-name: '&a每日收购·重载配置'
        lore:
          - '&7改完配置后应用（今日清单不变）'
        slot: 20
        permission: dailysell.admin
        actions:
          - '[close]'
          - '[player] ds reload'
```

### 5.3 管理面板如何进入

插件只有**一个触发物品**（时钟 CLOCK），右键打开的是玩家主菜单 `main`。

> 触发物品固定使用时钟材质，与 WorldEdit 导航魔杖（默认指南针 COMPASS）互不冲突。
> 材质切换后，玩家背包里的旧材质触发物品会在进服/`/qm give` 时**自动升级**为新材质。
> **右键空气/天空也能打开菜单**：监听器用 `EventPriority.LOWEST` 且不过滤已取消事件；
> 若对空气无反应，先确认触发物品仍在快捷栏第 9 格（give-slot: 8）。

管理菜单靠**权限门控**进入，两种途径：

| 途径                      | 说明                                                                                                  |
| ------------------------- | ----------------------------------------------------------------------------------------------------- |
| 主菜单点「管理面板」      | `main.admin` 配了 `permission: quickmenu.admin`，**只有管理员能看到这一项**，普通玩家界面里根本不出现 |
| 直接执行 `/qm open admin` | 适合管理员快速直达，也可绑到其他命令/菜单                                                              |

> **纵深防御**：管理菜单里**每一个**菜单项都单独配了 `permission`
> （如 `essentials.ban`、`coreprotect.rollback`、`chunky.trim`）。即便有人知道
> `/qm open admin` 强行打开，没有对应权限也**看不到任何按钮**——权限过滤在渲染阶段就跳过。

### 5.4 已做的两端分发

以下菜单项对两端调用不同指令：

| 菜单项                                                      | 基岩端                                                               | Java 端                                                                                |
| ----------------------------------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `teleport.warps`                                            | 统一 QuickMenu 传送点 GUI（`[menu] warps`，双端通用）                | 同左                                                                                   |
| `teleport.tpa` / `teleport.tpahere`                         | 统一 QuickMenu 玩家选择器（`[player-selector]`，两端从在线列表选人） | 同左                                                                                   |
| `social.msg`                                                | `/msggui`（BPS 私信表单）                                            | 文字提示 `/msg` 用法                                                                   |
| `skin.skin*`                                                | `/skin`（指令方式）                                                  | `/skins`（GUI 选择菜单）+ `/skin history` + `/skin favourites`                          |

> **未安装 BedrockPlayerSupport 时**：基岩玩家点击这些项，`[bedrock-player] xxxgui`
> 会执行一个不存在的指令，客户端提示「未知指令」，不崩溃但无效果。若不打算装 BPS，
> 把这些项改回无条件动作即可（如 `[player] warp`、`[player] homes`）。

### 5.5 为什么很多项是「文字指引」而不是直接执行

菜单只有「按钮列表」，**没有输入框**（Bukkit 箱子界面与基岩 SimpleForm 纯按钮都无法
在点击后弹出输入框填参数）。因此：

| 指令类型       | 例子                                                   | 菜单里的做法               |
| -------------- | ------------------------------------------------------ | -------------------------- |
| **不需要参数** | `/fly` `/heal` `/spark tps` `/res list`                | ✅ 直接执行 `[player] fly` |
| **需要参数**   | `/kick 玩家名` `/ban 玩家名 原因` `/res create 领地名` | ⚠️ 关闭界面 + 发送用法提示 |

后者动作形如：

```yaml
actions:
  - '[close]'
  - '[message] &7踢出玩家：&e/kick 玩家名 原因'
```

需要从在线列表选目标的项（踢人/封禁/传送/查背包等）已改用 `[player-selector]`；
需要开放文本输入的项（领地名/金额/物品/消息/广播）与离线玩家操作（unban/seen）仍保留文字指引。

### 5.6 权限过滤示范

- `main.admin` 配 `permission: quickmenu.admin` —— 管理员才看到管理面板
- `teleport.tpahere` 配 `permission: essentials.tpahere` —— 需额外授权
- `admin-inspect.purge` 配 `permission: coreprotect.purge` —— 危险操作仅供服主
- `admin-world.ctrim` 配 `permission: chunky.trim` —— 会删区块，单独隔离

---

## 6. 自行修改与重新构建

### 6.1 只改配置（最常见）

改 `plugins/QuickMenu/config.yml` 后执行 `/qm reload`，即时生效，不用重启。

### 6.2 改代码后重新构建（无需 Maven）

`build.ps1` 直接用 JDK 的 javac / jar 完成全流程：

```powershell
cd F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统
powershell -ExecutionPolicy Bypass -File build.ps1
```

脚本流程：自动定位 JDK 21+ → 下载编译依赖到 `_build\libs`（已存在则跳过）→ 编译源码
（UTF-8，开启 `-Xlint`）→ 静态校验 config.yml（材质名、size、槽位越界/冲突、
`[menu]` 目标、动作前缀）→ 打包 jar 并复制到 `dist\` → 校验 plugin.yml 主类一致性。

参数：

- `-SkipDeps` 离线构建，跳过依赖下载
- `-Clean` 清理上次产物。**删除或重命名过源文件后必须加**，否则会残留旧 class

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1 -SkipDeps
```

### 6.3 用 IDE 改代码

`plugin-src/pom.xml` 已配好，用 IntelliJ IDEA 导入该目录即可。依赖全部 `scope=provided`，
不会被打进 jar 造成类冲突。

### 6.4 依赖版本已锁定，勿随意升级

| 依赖           | 锁定版本              | 原因                           |
| -------------- | --------------------- | ------------------------------ |
| spigot-api     | 1.21.11-R0.1-SNAPSHOT | 对应 Leaf 1.21.11 核心         |
| floodgate api  | 2.2.5-SNAPSHOT        | Floodgate 当前 API             |
| **cumulus**    | **1.1.2**             | ⚠️ **不可换成 2.0.0-SNAPSHOT** |

> Floodgate 2.2.5 依赖 cumulus **1.1.2**；2.0.0-SNAPSHOT 缺少旧版 `FormBuilder`
> 路径，会导致 `FloodgatePlayer.sendForm()` 重载决议失败、编译报错。

---

## 7. 故障排查

| 现象                                       | 原因                                                              | 处理                                                                                                                               |
| ------------------------------------------ | ----------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| 右键物品没反应                             | 物品不是插件发放的（`strict-match: true` 时要求 PDC 标记）        | 用 `/qm give` 重新获取；或把 `strict-match` 设为 `false`                                                                           |
| 菜单打开但点击提示无权限                   | 只给了 `quickmenu.use`，漏了底层 `essentials.*`                   | 按第 3.3 / 第 8 节补齐权限                                                                                                         |
| 某个菜单项不显示                           | 配了 `permission` 且玩家无权，被过滤跳过                          | 属正常行为；要显示就去掉该项的 permission                                                                                           |
| 基岩玩家也看到箱子界面                     | Floodgate 未装 / `bedrock-native-form: false` / 表单发送失败回退 | 执行 `/qm info` 诊断；检查控制台回退警告                                                                                           |
| 基岩表单按钮显示乱码                       | 在 `button-label` 里写了 `&` 颜色码                                | 原生按钮不支持颜色码，删掉即可（插件会自动去除 `display-name` 的色码）                                                             |
| 返回按钮不显示                             | 右下角槽位（`size-1`）被其他菜单项占用                            | 控制台会有警告；把该物品的 slot 改开                                                                                               |
| 改配置不生效                               | 未热重载                                                          | 执行 `/qm reload`；若改的是 `plugin.yml` 需重启                                                                                    |
| 物品被玩家丢弃后丢失                       | `strict-match: true` 时插件会阻止丢弃                             | 若被丢（如死亡掉落），用 `/qm give` 补发                                                                                           |
| 进服时背包满没拿到触发物品                 | 自动发放失败（背包已满），插件会提示「请用 /qm」                  | 腾出背包空位（快捷栏第 9 格）后重进服自动补发；或直接输入 `/qm` 打开菜单                                                           |
| 右键触发物品被其他插件抢走（如被绑定传送） | 其他插件在同一右键事件上抢先处理                                  | 插件已用 LOWEST 最早优先级拦截：识别为触发物品即取消右键并打开菜单；仍异常时检查其他插件的右键绑定                                 |
| **Java 玩家点某项没反应**                  | 该项只配了 `[bedrock-*]` 动作，Java 端被跳过                      | 补一条 `[java-player] 指令` 或 `[message] 指引`；开 `debug: true` 看控制台「跳过平台条件动作」日志                                 |
| **基岩玩家点某项没反应**                   | 该项只配了 `[java-*]` 动作，基岩端被跳过                          | 补一条 `[bedrock-player] 指令`；同上开 debug 查日志                                                                                |
| **点了报「未知指令」**                      | 用了 `[bedrock-player] warpgui` 但未装 BedrockPlayerSupport       | 装上 BPS，或把该项改回无条件动作（如 `[player] warp`）                                                                            |
| 构建告警「无任何有效反馈」                 | 菜单项某一端只有 `[close]`，没有实际功能或提示                    | 属真实缺陷，按第 4.5 节补齐另一端动作                                                                                              |
| 构建报 `cumulus` 找不到                    | 依赖版本被改成 2.0.0-SNAPSHOT                                     | 改回 **1.1.2**，见第 6.4 节                                                                                                        |
| **主菜单看不到「管理面板」**               | 没有 `quickmenu.admin`，该项被权限过滤跳过                        | 按第 8.3 节给组授权；先确认自己所在组 `/lp user <自己> info`                                                                        |
| **`/qm open admin` 打开后是空的**          | 管理项每个都单独配了 permission，你一个都没有                      | 按第 8.3 / 8.4 节补齐 EssentialsX / Residence / CoreProtect 等权限                                                                  |
| 管理项点了提示无权限                       | 有 `quickmenu.admin` 但缺该插件自身的权限                         | 例：点「封禁」需 `essentials.ban`（仅服主组），见第 8.4 节                                                                         |
| 菜单项只显示指令用法、不执行               | 该项需要参数（玩家名/领地名），设计如此                           | 见第 5.5 节；复制提示里的指令补上参数即可                                                                                          |
| 点「获取选区木斧」没反应                   | //wand 经 performCommand 分发可能无效                             | 把 config.yml 里该项改为 [player] wand（去掉双斜杠）后 /qm reload                                                                  |
| **「回主城」点了报未知指令/没反应**        | 未装 EssentialsSpawn 模块，/spawn 命令不存在                      | 已在 Essentials 建 warp 传送点 主城，并把主菜单 spawn 按钮动作改为 `[player] warp 主城`                                            |

---

## 8. LuckPerms 权限授予清单

菜单只是前端，**真正干活的是各插件的指令**。菜单项点了没反应，
99% 是对应的插件权限没给。

> 权限组结构（default / admin / owner + bedrock 附加组）见权限管理章节。

### 8.1 玩家组（default）

```bash
# ---- 菜单自身 ----
lp group default permission set quickmenu.use true

# ---- EssentialsX：传送（家园功能已禁用）----
lp group default permission set essentials.tpa true
lp group default permission set essentials.tpaccept true
lp group default permission set essentials.tpdeny true
lp group default permission set essentials.warp true
lp group default permission set essentials.spawn true   # 注：未装 EssentialsSpawn 模块，/spawn 命令实际不存在；主菜单「回主城」按钮已改用 essentials.warp + /warp 主城
# /back（返回上一个位置）功能已取消：菜单无入口，default 组不授予 essentials.back / essentials.back.ondeath
lp group default permission set essentials.near true

# ---- EssentialsX：经济 ----
lp group default permission set essentials.balance true
lp group default permission set essentials.balancetop true
lp group default permission set essentials.pay true
# /sell /sellall /worth 已命令级禁用（Essentials disabled-commands）且不授予权限，卖物走 /ah /shop /ds

# ---- 经济系统插件 ----
# 拍卖行（AuctionHouse auction.* 默认 true，无需授予）
lp group default permission set economyshop.use true
lp group default permission set economyshop.sell true
lp group default permission set economyshop.chestshop.create true
lp group default permission set economyshop.chestshop.use true
lp group default permission set quests.command.start true
lp group default permission set quests.command.track true
lp group default permission set quests.command.cancel true
lp group default permission set quests.command.quest true
lp group default permission set eztax.stats true

# ---- EssentialsX：工具包与物品 ----
lp group default permission set essentials.kit true
lp group default permission set essentials.kits.starter true
lp group default permission set essentials.repair true
lp group default permission set essentials.workbench true

# ---- EssentialsX：信息与社交 ----
lp group default permission set essentials.list true
lp group default permission set essentials.motd true
lp group default permission set essentials.rules true
lp group default permission set essentials.help true
lp group default permission set essentials.msg true
lp group default permission set essentials.reply true
lp group default permission set essentials.ignore true
lp group default permission set essentials.nick true
lp group default permission set essentials.whois true

# ---- Residence：领地 ----
lp group default permission set residence.create true
lp group default permission set residence.delete true
lp group default permission set residence.rename true
lp group default permission set residence.select.basic true
lp group default permission set residence.select.area true
lp group default permission set residence.select.chunk true
lp group default permission set residence.select.expand true
lp group default permission set residence.info true
lp group default permission set residence.list true
lp group default permission set residence.teleport true
lp group default permission set residence.subzone true
lp group default permission set residence.message true
lp group default permission set residence.unstuck true
lp group default permission set residence.kick true
# 常用 flag：菜单里「设置领地开关 / 给玩家授权」用到
lp group default permission set residence.flags.build true
lp group default permission set residence.flags.destroy true
lp group default permission set residence.flags.use true
lp group default permission set residence.flags.container true
lp group default permission set residence.flags.pvp true
lp group default permission set residence.flags.move true
lp group default permission set residence.flags.tp true

# ---- SkinsRestorer：皮肤 ----
lp group default permission set skinsrestorer.command true
lp group default permission set skinsrestorer.command.set true
lp group default permission set skinsrestorer.command.clear true
lp group default permission set skinsrestorer.command.gui true
lp group default permission set skinsrestorer.command.update true

# ---- EasyBot：绑定 QQ ----
lp group default permission set easybot.command.bind true

# ---- SimplePets：宠物系统 ----
# 菜单项「宠物系统」执行 /pet gui，需以下命令权限
lp group default permission set pet.commands.gui true
lp group default permission set pet.commands.help true
lp group default permission set pet.commands.summon true
lp group default permission set pet.commands.rename true
lp group default permission set pet.commands.remove true
# 召唤具体宠物还需 pet.type.<mob> 类型权限（如 pet.type.wolf），按需发放

# ---- Simple Voice Chat：语音 ----
lp group default permission set voicechat.speak true
lp group default permission set voicechat.listen true
lp group default permission set voicechat.groups true

# ---- PumpkinMail：南瓜邮箱（玩家侧 /mailbox 领奖励，无需额外权限）----
# 管理子命令（admin/give/preset/list/stats）需要 pumpkinmail.admin，见 8.3
```

### 8.3 管理组（admin / owner）

```bash
# ---- 菜单管理入口（关键：没有这个看不到「管理面板」）----
lp group admin permission set quickmenu.admin true
lp group owner permission set quickmenu.admin true

# ---- OpenInv：查背包 ----
lp group admin permission set openinv.open true
lp group admin permission set openinv.search true
lp group admin permission set openinv.crossworld true
lp group admin permission set openinv.silent true
lp group admin permission set openinv.exempt true   # 免疫被别人偷看，务必给

# ---- EssentialsX：玩家管理 ----
lp group admin permission set essentials.tp true
lp group admin permission set essentials.tphere true
lp group admin permission set essentials.tpall true
lp group admin permission set essentials.top true
lp group admin permission set essentials.setspawn true
lp group admin permission set essentials.setwarp true
lp group admin permission set essentials.delwarp true
lp group admin permission set essentials.tpo true
lp group admin permission set essentials.tpohere true
lp group admin permission set essentials.clearinventory true
lp group admin permission set essentials.give true
lp group admin permission set essentials.repair.all true
lp group admin permission set essentials.gamemode true
lp group admin permission set essentials.heal true
lp group admin permission set essentials.feed true
lp group admin permission set essentials.fly true
lp group admin permission set essentials.god true
lp group admin permission set essentials.vanish true
lp group admin permission set essentials.invsee true
lp group admin permission set essentials.seen true
lp group admin permission set essentials.balance.others true
lp group admin permission set essentials.eco true

# ---- EssentialsX：处罚 ----
lp group admin permission set essentials.kick true
lp group admin permission set essentials.mute true
lp group admin permission set essentials.unmute true
lp group admin permission set essentials.jail true
lp group admin permission set essentials.unjail true
lp group admin permission set essentials.setjail true
lp group admin permission set essentials.broadcast true
lp group admin permission set essentials.socialspy true

# ---- Residence：领地管理 ----
lp group admin permission set residence.admin true
lp group admin permission set residence.admin.info true
lp group admin permission set residence.admin.remove true
lp group admin permission set residence.admin.setowner true
lp group admin permission set residence.admin.select true
lp group admin permission set residence.admin.tp true
lp group admin permission set residence.flags.bypass true

# ---- CoreProtect：审计与回滚（purge 不给，见 8.4）----
lp group admin permission set coreprotect.inspect true
lp group admin permission set coreprotect.inspect.others true
lp group admin permission set coreprotect.lookup true
lp group admin permission set coreprotect.near true
lp group admin permission set coreprotect.rollback true
lp group admin permission set coreprotect.restore true
lp group admin permission set coreprotect.reload true
lp group admin permission set coreprotect.help true

# ---- Chunky：区块预生成（trim 不给，见 8.4）----
lp group admin permission set chunky.start true
lp group admin permission set chunky.pause true
lp group admin permission set chunky.continue true
lp group admin permission set chunky.cancel true
lp group admin permission set chunky.progress true
lp group admin permission set chunky.world true
lp group admin permission set chunky.radius true
lp group admin permission set chunky.shape true
lp group admin permission set chunky.center true
lp group admin permission set chunky.spawn true
lp group admin permission set chunky.worldborder true
lp group admin permission set chunky.selection true
lp group admin permission set chunky.reload true

# ---- WorldEdit：创世神 ----
lp group admin permission set worldedit.wand true
lp group admin permission set worldedit.selection.pos true
lp group admin permission set worldedit.selection.chunk true
lp group admin permission set worldedit.selection.expand true
lp group admin permission set worldedit.selection.size true
lp group admin permission set worldedit.clipboard.copy true
lp group admin permission set worldedit.clipboard.paste true
lp group admin permission set worldedit.history.undo true
lp group admin permission set worldedit.history.redo true
lp group admin permission set worldedit.region.set true
lp group admin permission set worldedit.region.replace true
lp group admin permission set worldedit.limit.50000 true
lp group admin permission set worldedit.navigation.unstuck true

# ---- spark：性能监控 ----
lp group admin permission set spark.tps true
lp group admin permission set spark.health true
lp group admin permission set spark.ping true
lp group admin permission set spark.profiler true
lp group admin permission set spark.heapsummary true
lp group admin permission set spark.activity true

# ---- Geyser / Floodgate ----
lp group admin permission set geyser.command.reload true
lp group admin permission set geyser.command.dump true
lp group admin permission set geyser.command.statistics true

# ---- ViaVersion / EasyBot / 语音 / 原版 ----
lp group admin permission set viaversion.admin true
lp group admin permission set easybot.command.reload true
lp group admin permission set easybot.admin true
lp group admin permission set voicechat.admin true
lp group admin permission set bukkit.command.plugins true
lp group admin permission set minecraft.command.time true
lp group admin permission set minecraft.command.weather true

# ---- 经济系统管理（管理菜单 admin-economy 用）----
lp group admin permission set auction.admin true
lp group admin permission set economyshop.admin true
lp group admin permission set dailysell.admin true
# eztax.stats 已在 default 授予（玩家经济中心 + 管理菜单共用）；调整税率/征收改 config.yml

# ---- PumpkinMail：南瓜邮箱管理（管理面板「南瓜邮箱管理」入口）----
lp group admin permission set pumpkinmail.admin true

# ---- LuckPerms：日常查询（危险项不给，见 8.4）----
lp group admin permission set luckperms.info true
lp group admin permission set luckperms.sync true
lp group admin permission set luckperms.reload true
lp group admin permission set luckperms.tree true
lp group admin permission set luckperms.verbose true
```

### 8.4 服主组（owner）——仅危险操作

这些会**造成不可恢复的破坏**或**提权**，只给服主：

```bash
# 封禁类（影响玩家账号，建议仅服主）
lp group owner permission set essentials.ban true
lp group owner permission set essentials.unban true
lp group owner permission set essentials.tempban true
lp group owner permission set essentials.banip true

# CoreProtect 清库：删除记录，不可恢复
lp group owner permission set coreprotect.purge true

# Chunky 裁剪：删除选区外区块，误用会删掉玩家建筑
lp group owner permission set chunky.trim true

# LuckPerms 网页编辑器：拿到链接的人都能改权限
lp group owner permission set luckperms.editor true

# 其余高危（按需）
lp group owner permission set worldedit.anyblock true
lp group owner permission set worldedit.butcher true
lp group owner permission set openinv.edit true
lp group owner permission set openinv.override true
```

> ☠️ `minecraft.command.op` / `deop` / `stop` **绝不给任何组**。

### 8.5 验证方法

```bash
# 检查某个玩家是否真的有某权限（含继承）
/lp user <玩家> permission check essentials.kit

# 实时追踪权限判定过程 —— 排查「菜单点了没反应」的神器
/lp verbose on <玩家> essentials
# 然后让该玩家点一次菜单项，看控制台输出的 true/false 判定链

# 看某插件到底注册了哪些节点（节点名写错时用它确认）
/lp tree essentials
/lp tree coreprotect
```

### 8.6 安全检测记录（2026-10-01）

**检测方法**：源码逻辑审阅 + 运行服 config 全量扫描（2863 行 / 213 按钮）+ LuckPerms H2 数据库直查（三组权限实测）。

#### ✅ 确认安全

| 检测项 | 结果 |
| --- | --- |
| `[console]` 动作（玩家点击→控制台执行） | 运行服 config 中 **0 个** |
| 管理菜单门控 | 入口 `quickmenu.admin` + 各按钮具体管理权限 |
| 渲染过滤 + 点击二次校验 | Java 箱子 / 基岩表单均过滤无权限按钮，点击时 `hasItemPermission` 再验 |
| default 组管理权限 | **无** quickmenu.admin / luckperms.* / essentials.ban/kick/give/eco/gamemode |
| build/打开箱子按钮 | 全是教学 message（`/res set 领地名 build true` 仅为提示文字，不执行） |

**结论**：快捷菜单**不可能**给玩家打开全局权限；`residence.flags.build/destroy` 是领地 flag 管理权限，仅领地内生效。

#### 三组权限实测（H2 直查）

| 组 | 权限 | 成员 |
| --- | --- | --- |
| **admin** | `*`（**通配全权限**）+ residence.admin.setowner + residence.group.admin + 前缀 | 1 人（UUID 8e682ed5） |
| **default** | 65 条玩家权限（无任何管理权限） | 163 人（全部玩家） |
| **guildmaster** | 仅 residence.create + residence.use | **0 人（残留空组）** |

> ⚠️ 文档 8.3/8.4 写的 **owner 组实际不存在**（H2 里没有），服主目前靠 admin 组的 `*` 通配或 OP 身份。

#### 发现并解决的问题

**① default 组残留印钞口权限（essentials.sell / essentials.worth = TRUE）**
已确认存在（ID=59/60），命令级 `disabled-commands` 已挡、权限层未清。**在运行服执行**：

```bash
lp group default permission unset essentials.sell
lp group default permission unset essentials.worth
```

**② default 组有 essentials.setwarp = TRUE**
玩家可创建公共传送点，与管理菜单「创建公共传送点」按钮（管理专用）设计不符。**在运行服执行**：

```bash
lp group default permission unset essentials.setwarp
```

**③ guildmaster 组残留**
0 成员 + 仅 2 条弱权限，确认为废弃组（非"服主"组）。确认无玩家使用后**在运行服执行**：

```bash
lp deletegroup guildmaster
```

**④ 源码 config 落后运行服**
✅ 已解决：运行服最新 config.yml（2863 行）已同步到 `plugin-src/src/main/resources/config.yml`，sell/worth 按钮残留 0。下次重新构建打包即生效。

**附带清理**（无害但建议清）：default 组脏数据 `essentials.compass`（contexts 是 lp 命令打错残留）：

```bash
lp group default permission unset essentials.compass
```

---

## 9. 与 BedrockPlayerSupport 的关系

两者都给基岩玩家提供表单，但定位不同：

|          | QuickMenu（本章）                | BedrockPlayerSupport                                   |
| -------- | -------------------------------- | ------------------------------------------------------ |
| 定位     | **自定义菜单**，内容完全由你配置 | **既有指令的表格外壳**，把 `/tpa` `/kit` 等包装成表单 |
| 覆盖面   | 任意指令、任意层级菜单           | 固定的传送/私信/工具包表单                        |
| 触发方式 | 物品右键 / `/qm`                 | 各自的 `/tpgui` `/kitgui` 等指令                      |
| Java 端  | 有箱子 GUI                       | 无（仅基岩端）                                         |

**推荐组合**：QuickMenu 负责「物品右键唤出的总菜单」，BPS 负责「事件驱动的自动表单」
（收到传送请求、死亡重生时主动弹出，这类场景菜单插件无法覆盖）。本配置已把
`teleport.warps`、`teleport.tpa`、`social.msg` 三项配成两端分发（`home.listhomes` 随家园功能一并禁用，2026-09-30），
基岩玩家点击后直接进入 BPS 原生表单，Java 玩家走 EssentialsX 指令。
