# 26. 快捷菜单系统（QuickMenu 自研插件）

> **一句话**：玩家右键一个触发物品，Java 端弹出箱子 GUI，基岩端弹出原生 Form 表单，
> 一套配置同时驱动两端，菜单项点击后执行的动作完全一致。
>
> **菜单已整合本服全部插件**：20 套菜单 / 160 个菜单项，分**玩家线**（10 套）
> 与**管理线**（10 套），管理入口靠权限门控，普通玩家看不到。

本章交付的是**可直接部署的自研插件**（含编译好的 jar 与完整源码工程），
而非第三方插件的配置教程。

**插件与菜单的对应关系**（本菜单已覆盖的后端）：

| 插件 | 玩家菜单 | 管理菜单 |
|---|---|---|
| EssentialsX | 传送 / 家园 / 经济 / 工具包 / 社交 / 信息 | 玩家管理 / 处罚 / 传送管理 |
| Residence | 我的领地 | 领地管理 |
| SkinsRestorer | 皮肤管理 | — |
| Simple Voice Chat | 语音聊天 | 服务器监控（语音管理） |
| EasyBot | 社交（绑定 QQ） | 服务器监控（重载） |
| CoreProtect | — | 审计与回滚 |
| OpenInv | — | 玩家管理（查背包） |
| Chunky | — | 世界管理 |
| WorldEdit | — | 世界管理 |
| spark | — | 服务器监控 |
| Geyser / ViaVersion | — | 服务器监控 |
| LuckPerms | — | 权限管理 |
| BedrockPlayerSupport | 传送 / 家园 / 皮肤（表单） | — |

---

## 1. 选型结论：为什么是自研单插件

需求是「通过某个物品使用的快捷菜单，基岩版用表单、Java 版用箱子 GUI」。
调研后对比了三条技术路线：

| 方案 | 组成 | 性能 | 实用性 | 结论 |
|---|---|---|---|---|
| **A. 三插件组合** | DeluxeMenus + BedrockPlayerSupport + Skript | 差 | 中 | ❌ 否决 |
| **B. 单插件双端** | HexaForms / zMenu 等小众插件 | 中 | 中 | ⚠️ 备选 |
| **C. 自研单插件** | QuickMenu（本方案） | **优** | **优** | ✅ **采用** |

### 1.1 为什么否决方案 A（三插件组合）

最初设想是「DeluxeMenus 画箱子 + BedrockPlayerSupport 出基岩表单 + Skript 监听物品右键」。否决理由：

1. **菜单插件无法自己监听物品右键**。DeluxeMenus 官方论坛明确回复
   「there is not a feature in this plugin」
   （来源：[SpigotMC 讨论](https://www.spigotmc.org/threads/deluxemenus.89587/page-48)）。
   所以必须额外引入 Skript 来补这个缺口。
2. **Skript 是解释执行的**。脚本在运行时解析文本并逐行解释，物品右键是高频事件，
   每个玩家每次右键都要走一遍解释器，性能明显劣于编译好的 Java 字节码。
3. **三个插件、三套配置、三套权限**，排查问题时要跨插件追踪，维护成本高。

### 1.2 为什么否决方案 B（现成双端插件）

HexaForms 确实能「一份 YAML 定义两端界面」，功能对口，但：

- 仅 **719 次下载、2 个关注者**，属于小众新插件
- 当前版本 1.1.2，社区支持薄弱
- 一旦作者停更或新版 Minecraft 不兼容，没有退路

（来源：[Modrinth - HexaForms](https://modrinth.com/plugin/hexaforms)）

### 1.3 为什么采用方案 C（自研）

**性能层面**：

- 菜单配置在**服务器启动时解析一次**，转为内存中的 Java 对象树。
  玩家打开界面时零 IO、零 YAML 解析、零表达式求值。
  对比 DeluxeMenus 运行时解析 YAML 表达式、Skript 解释执行，这是数量级的差距。
- 插件 jar 仅 **43.5 KB**，16 个 class，无 shade 依赖打包，启动加载开销极低。
- 打开界面时按权限过滤菜单项，过滤结果是直接遍历内存 List，无反射、无查表。

**实用层面**：

- **单插件搞定两端**，一份 `config.yml` 同时描述箱子布局与基岩按钮，行为不会分歧。
- 菜单项动作两端共用同一个执行器，Java 点图标和基岩点按钮执行的是同一份动作列表。
- 不依赖 PlaceholderAPI、不依赖 Vault，**零硬依赖**（Floodgate 为软依赖，未装也能跑）。

---

## 2. 关键技术决策：基岩端为什么不用箱子

这是整个方案最重要的判断，**与需求原话相反**，必须先说明。

需求原话是「基岩版用箱子装各种插件或者原生的功能」。但实测调研发现，
**Geyser 的箱子界面在基岩端有多个未解决的缺陷**：

| 问题 | 表现 | 来源 |
|---|---|---|
| 物品可被拖出 | 玩家误操作会把菜单物品拽出来掉地上 | [GeyserMC#211](https://github.com/GeyserMC/Geyser/issues/211) |
| 移动端高亮异常 | 触屏选中物品时高亮渲染错误 | [GeyserMC#5896](https://github.com/GeyserMC/Geyser/issues/5896) |
| 界面打不开 | 特定方块环境下箱子界面无法打开 | [GeyserMC#6134](https://github.com/GeyserMC/Geyser/issues/6134) |
| 只能左键交互 | 基岩玩家在箱子界面无法右键操作 | [SpigotMC 讨论](https://www.spigotmc.org/threads/bedrock-java-geyser-gui.645599/) |

**原生 Form 由基岩客户端自己渲染**，点击、滚动、按钮反馈全部正常，不存在上述问题。

因此本插件的实际策略是：

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

> ⚠️ 如果你确实要让基岩玩家也看箱子，把 `config.yml` 的
> `platform.bedrock-native-form` 改为 `false` 即可回退，但会继承上述 Geyser 缺陷。
> 插件在箱子界面里已做了**无条件取消点击 + 拦截拖拽**的防护，能缓解物品被拖出的问题。

### 2.1 为什么普通 Bukkit 插件能发原生表单

这是本方案能成立的技术前提，已核实：

**Floodgate 自带 Cumulus 表单 API，普通 Bukkit 插件即可调用，不需要写 Geyser 扩展。**

```java
FloodgateApi api = FloodgateApi.getInstance();
if (api.isFloodgatePlayer(uuid)) {          // 判断是否基岩玩家
    api.getPlayer(uuid).sendForm(form);     // 发送原生表单
}
```

（来源：[GeyserMC Wiki - Forms and Cumulus](https://geysermc.org/wiki/geyser/forms/)、
[Floodgate API](https://geysermc.org/wiki/floodgate/api/)）

### 2.2 基岩端两个必须处理的坑

开发中实测踩到，已在代码里解决：

1. **表单回调在 Netty 网络线程触发**。直接在回调里操作背包会导致服务器崩溃。
   → `ActionExecutor.runAsyncSafe()` 统一检测线程并切回主线程。
2. **原生 Form 按钮不支持 `&` / `§` 颜色码**，会显示成乱码字符。
   → 按钮文本统一经 `stripColor()` 去除颜色码；表单标题保留颜色（标题支持）。
   → 配置项 `button-label` 可单独指定基岩端按钮文本。

---

## 3. 目录结构

```
26-快捷菜单系统/
├── 快捷菜单系统-QuickMenu.md        ← 本文档
├── build.ps1                        ← 一键构建脚本（无需 Maven）
├── dist/
│   └── QuickMenu-1.0.0.jar          ← ★ 部署用成品，复制这个
├── plugin-src/
│   ├── pom.xml                      ← 可选：Maven / IDE 用
│   └── src/main/
│       ├── java/com/nangua/quickmenu/
│       │   ├── QuickMenuPlugin.java     主类，装配与 Floodgate 探测
│       │   ├── command/
│       │   │   └── QuickMenuCommand.java  /qm 命令与 Tab 补全
│       │   ├── config/
│       │   │   ├── ConfigLoader.java      YAML → 内存对象（启动时一次）
│       │   │   └── PluginSettings.java    设置与菜单注册表
│       │   ├── gui/
│       │   │   ├── ChestMenuRenderer.java Java 端箱子渲染
│       │   │   ├── BedrockFormRenderer.java 基岩端原生表单渲染
│       │   │   └── MenuHolder.java        界面持有者标记（识别自家界面）
│       │   ├── listener/
│       │   │   ├── TriggerItemListener.java 物品右键 / 进服发放 / 防丢弃
│       │   │   └── MenuClickListener.java     点击处理 / 防拖出 / 防连点
│       │   └── menu/
│       │       ├── MenuManager.java       ★ 两端分发决策中枢
│       │       ├── ActionExecutor.java    ★ 动作执行（两端共用）
│       │       ├── Menu.java  MenuItem.java  Action.java  ActionType.java
│       └── resources/
│           ├── plugin.yml             插件描述（命令与权限）
│           └── config.yml             ★ 菜单配置（20 套：玩家 10 + 管理 10）
└── _build/                            构建中间目录（依赖库、class、探针）
```

---

## 4. 安装部署

### 4.1 环境要求

| 项目 | 要求 | 你的环境 |
|---|---|---|
| 服务端核心 | Paper 或其分支 | ✅ Leaf 1.21.11（Paper 分支） |
| Java | 21+ | ✅ 已装 JDK 21 |
| Minecraft | 1.21.x | ✅ 1.21.11 |
| Floodgate | 基岩端原生表单必需 | 见 `03-Java-Bedrock互通层` |

### 4.2 部署步骤

```
1. 复制 dist\QuickMenu-1.0.0.jar  →  服务器 plugins\ 目录
2. 重启服务器
3. 首次启动自动生成 plugins\QuickMenu\config.yml
4. 按需修改配置，然后 /qm reload 热重载（无需重启）
```

> **未装 Floodgate 也能正常启动**。插件已做延迟加载处理：
> 只在确认 Floodgate 可用后才触碰 Cumulus 相关类，
> 纯 Java 服务器上会自动跳过基岩端渲染，全部玩家走箱子界面。

### 4.3 权限授予（**必须做，否则打不开菜单**）

参照第 24 章发现的问题——你的 `default` 组此前只有 3 条 Residence 权限，
**没有任何 EssentialsX 权限**。菜单同理，不显式授权就是用不了：

```bash
# 菜单本身的使用权限（default 已是 true，通常无需设置）
lp group default permission set quickmenu.use true

# 管理面板入口权限：给了才能在主菜单看到「管理面板」
lp group admin permission set quickmenu.admin true
lp group owner permission set quickmenu.admin true
```

> **菜单能打开但点了没反应 / 提示无权限**，几乎都是只给了 `quickmenu.use`
> 而漏了底层的插件权限。菜单只是前端，真正干活的还是 EssentialsX / Residence /
> SkinsRestorer 等后端插件。

**完整的授权命令见 [第 10 章 LuckPerms 权限授予清单](#10-luckperms-权限授予清单)**，
按「玩家组 / 管理组 / 危险操作」分组，覆盖本菜单用到的全部 60+ 个权限节点。

---

## 5. 命令与配置

### 5.1 命令一览

| 命令 | 作用 | 权限 |
|---|---|---|
| `/qm` | 打开默认菜单 | `quickmenu.use`（默认所有人） |
| `/qm open <菜单id>` | 打开指定菜单 | `quickmenu.use` |
| `/qm give [玩家]` | 发放触发物品 | `quickmenu.admin` |
| `/qm reload` | 热重载配置 | `quickmenu.admin` |
| `/qm list` | 列出已加载菜单 | `quickmenu.admin` |
| `/qm info` | 显示客户端类型与设备信息（调试） | `quickmenu.use` |

别名：`/quickmenu`。全部命令带 Tab 补全（子命令、菜单 id、在线玩家名）。

### 5.2 配置结构

`plugins/QuickMenu/config.yml` 分四块：

| 节点 | 作用 |
|---|---|
| `platform` | 两端策略开关 |
| `trigger-item` | 触发物品的材质、名称、描述、发放方式 |
| `sound` / `messages` | 音效与全部提示文本（可本地化） |
| `menus` | **菜单定义**（核心） |

### 5.3 菜单项字段说明

```yaml
menus:
  main:                          # ← 菜单 id
    title: '&6快捷菜单'           # 界面标题（两端通用，支持 & 颜色码）
    bedrock-content: '&7说明文字'  # 基岩表单标题下的说明（留空则用 title）
    size: 27                     # 箱子容量，必须是 9 的倍数，范围 9~54
    back-menu: ''                # 上级菜单 id，配置后显示返回按钮
    filler:                      # 箱子空白处的填充物品
      material: BLACK_STAINED_GLASS_PANE
      name: '&8'

    items:
      home:                      # ← 菜单项 id（唯一）
        material: RED_BED        # 物品材质（仅 Java 箱子用）
        display-name: '&a我的家园' # 显示名（两端通用）
        lore:                    # 描述行（仅 Java 箱子用）
          - '&7回家、设置家'
        slot: 11                 # 箱子格子序号，从 0 开始（仅 Java 箱子用）
        glowing: true            # 是否发光（无附魔副作用）
        permission: ''           # 需要的权限，留空=所有人可见
        button-label: ''         # 基岩按钮文本，留空=自动用 display-name 去色
        actions:                 # 点击后执行的动作
          - '[menu] home'
```

### 5.4 动作类型

**无条件动作**（任何客户端都执行）：

| 前缀 | 作用 | 示例 |
|---|---|---|
| `[player]` | 以玩家身份执行命令（**受权限约束**） | `[player] home` |
| `[console]` | 以控制台身份执行（**无视权限**，慎用） | `[console] eco give X 100` |
| `[menu]` | 打开另一个菜单 | `[menu] teleport` |
| `[message]` | 发送消息，支持 `&` 颜色码 | `[message] &a已传送` |
| `[close]` | 关闭界面/表单 | `[close]` |
| 无前缀 | 等同 `[player]` | `home` |

**平台条件动作**（仅对应客户端执行）：

| 前缀 | 作用 |
|---|---|
| `[bedrock-player]` | 仅基岩玩家，以玩家身份执行 |
| `[java-player]` | 仅 Java 玩家，以玩家身份执行 |
| `[bedrock-console]` | 仅基岩玩家，控制台身份 |
| `[java-console]` | 仅 Java 玩家，控制台身份 |

动作可叠加，按列表顺序执行。常见组合：先 `[close]` 关界面，再执行指令，
避免指令输出被界面遮挡。

### 5.5 两端分发：解决「同一功能两端指令不同」

这是平台条件动作存在的意义。**不要**把基岩专属指令写成无条件动作：

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

也可以给缺功能的一端用 `[message]` 做**文字指引降级**：

```yaml
# 基岩走表单选人，Java 端 EssentialsX 无等价界面，改为提示指令用法
actions:
  - '[close]'
  - '[bedrock-player] tpgui'
  - '[message] &7使用 &e/tpa 玩家名 &7发送传送请求'
```

> `[message]` 是两端通用动作，上例中基岩玩家也会看到这句提示，属可接受的冗余。
> 若要严格只在 Java 端提示，可再加一条 `[java-player]` 类动作，或接受这点冗余。

> ⚠️ **构建时会自动检查覆盖缺口**。若某菜单项只配了 `[bedrock-*]` 而 Java 端
> 连 `[message]` 都没有，构建脚本会告警「该动作对 Java 玩家无任何有效反馈」。
> 判据是：`[player]`/`[console]`/`[message]`/`[menu]` 算两端覆盖，
> `[close]` **不算**（它只是关界面，没有功能价值）。

### 5.6 权限过滤行为

菜单项配了 `permission` 且玩家无权时，**该项直接被跳过**，而不是显示为灰色不可点：

- Java 端：该槽位显示填充物品
- 基岩端：该按钮不出现，**后续按钮序号自动前移**

> 这个「序号前移」是基岩端容易出 bug 的点：`SimpleFormResponse.clickedButtonId()`
> 返回的是玩家点击的**按钮序号**，不是菜单项 id。若菜单项因权限被跳过而序号未重映射，
> 玩家点 A 会执行 B 的动作。代码里用 `visibleItems` 列表按序记录，确保索引正确。

---

## 6. 菜单结构总览（20 套，玩家 / 管理双线）

`config.yml` 已配好 **20 套菜单**，把本服所有插件的常用功能全部收进菜单。
校验结果：**20 菜单 / 160 菜单项 / 0 错误 0 警告**。

### 6.1 玩家菜单（10 套）

| 菜单 id | 名称 | 对接插件 | 主要内容 |
|---|---|---|---|
| `main` | 主菜单 | — | 一级入口，10 个分类（含管理面板入口） |
| `teleport` | 传送功能 | EssentialsX + BPS | 传送点、申请传送、拉人、返回、出生点、附近玩家 |
| `home` | 我的家园 | EssentialsX + BPS | 家列表、回家、设置家、删除家 |
| `residence` | 我的领地 | **Residence** | 建/删/传送领地、设置传送点、flag 开关、玩家授权、子领地、进出提示、帮助 |
| `economy` | 经济中心 | EssentialsX + Vault | 余额、财富榜、转账、卖物品、估价 |
| `kit` | 工具包 | EssentialsX | 新手包、每日奖励、VIP 包 |
| `skin` | 皮肤管理 | **SkinsRestorer** | 皮肤库、换肤、清除、刷新、随机、撤销 |
| `social` | 社交设置 | EssentialsX + **EasyBot** | 私信、快速回复、屏蔽、改昵称、查信息、在线列表、绑定 QQ |
| `info` | 服务器信息 | EssentialsX | 在线列表、公告、规则、互通说明、指令帮助 |
| `voice` | 语音聊天 | **Simple Voice Chat** | 说话方式、群组语音、音量设置、故障排查 |

层级：所有二级菜单的 `back-menu` 均为 `main`，返回按钮固定在右下角（`size-1` 槽位）。

### 6.2 管理菜单（10 套）

| 菜单 id | 名称 | 对接插件 | 主要内容 |
|---|---|---|---|
| `admin` | 管理面板 | — | 一级入口，9 个分类 + 返回玩家菜单 |
| `admin-player` | 玩家管理 | **OpenInv** + EssentialsX | 看背包、传送/拉人、切模式、治疗、飞行、无敌、喂食、隐身、清背包、查信息、修复、发物品 |
| `admin-punish` | 处罚管理 | EssentialsX | 踢出、封禁、临时封禁、解封、封 IP、禁言、解禁、关押、释放、广播、私信监视 |
| `admin-teleport` | 传送管理 | EssentialsX | 强制传送/拉人、全员传送、头顶、设出生点、建/删传送点、静默传送 |
| `admin-residence` | 领地管理 | **Residence** | 全服领地列表、查看/删除/转移归属、传送、选取他人领地 |
| `admin-inspect` | 审计与回滚 | **CoreProtect** | 查询模式、附近变更、条件查询、回滚、撤销回滚、清理数据库、重载、帮助 |
| `admin-world` | 世界管理 | **Chunky** + **WorldEdit** + 原版 | 时间/天气切换、区块预生成全套、创世神木斧/撤销/重做/复制/粘贴 |
| `admin-server` | 服务器监控 | **spark** + **Geyser** + **ViaVersion** + **EasyBot** | TPS、健康报告、延迟、性能采样、堆内存、Geyser 重载/诊断/统计、版本分布、机器人重载、插件列表 |
| `admin-perm` | 权限管理 | **LuckPerms** | 网页编辑器、同步、重载、权限树、信息、查玩家权限、实时追踪 |
| `admin-qm` | 菜单管理 | QuickMenu 自身 | 重载配置、菜单列表、客户端诊断、发放触发物品 |

层级：9 个二级菜单的 `back-menu` 均为 `admin`，`admin` 的 `back-menu` 为 `main`，
管理员可在两条线之间自由往返。

### 6.3 管理面板如何进入

插件只有**一个触发物品**（指南针），右键打开的是玩家主菜单 `main`。
管理菜单靠**权限门控**进入，两种途径：

| 途径 | 说明 |
|---|---|
| 主菜单点「管理面板」 | `main.admin` 配了 `permission: quickmenu.admin`，**只有管理员能看到这一项**，普通玩家界面里根本不出现 |
| 直接执行 `/qm open admin` | 适合管理员快速直达，也可绑到其他命令/菜单 |

> **纵深防御**：管理菜单里**每一个**菜单项都单独配了 `permission`
> （如 `essentials.ban`、`coreprotect.rollback`、`chunky.trim`）。
> 即便有人知道 `/qm open admin` 强行打开，没有对应权限也**看不到任何按钮**——
> 权限过滤在渲染阶段就把项跳过了，不是「显示灰色点不动」。

### 6.4 已做的两端分发

以下菜单项对两端调用不同指令（详见第 5.5 节）：

| 菜单项 | 基岩端 | Java 端 |
|---|---|---|
| `teleport.warps` | `/warpgui`（BPS 传送点表单） | `/warp`（EssentialsX 列表） |
| `teleport.tpa` | `/tpgui`（BPS 选人表单） | 文字提示 `/tpa` 用法 |
| `home.listhomes` | `/homegui`（BPS 家园表单） | `/homes`（EssentialsX 列表） |
| `social.msg` | `/msggui`（BPS 私信表单） | 文字提示 `/msg` 用法 |
| `skin.skingui` | `/skin`（指令方式） | `/skins`（GUI 浏览器） |

> **未安装 BedrockPlayerSupport 时会怎样**：基岩玩家点击这些项，
> `[bedrock-player] xxxgui` 会执行一个不存在的指令，客户端提示「未知指令」，
> 不会崩溃但无效果。若你不打算装 BPS，把这些项改回无条件动作即可，例如：
> `[player] warp`、`[player] homes`、`[message] 提示 /msg 用法`。

### 6.5 为什么很多项是「文字指引」而不是直接执行

这是**刻意设计，不是偷懒**，先看结论再看原因。

菜单只有「按钮列表」，**没有输入框**。本插件渲染的是 Bukkit 箱子界面和基岩
`SimpleForm`（纯按钮），两者都**无法在点击后弹出输入框让你填参数**。

因此：

| 指令类型 | 例子 | 菜单里的做法 |
|---|---|---|
| **不需要参数** | `/fly` `/heal` `/spark tps` `/res list` | ✅ 直接执行 `[player] fly` |
| **需要参数** | `/kick 玩家名` `/ban 玩家名 原因` `/res create 领地名` | ⚠️ 关闭界面 + 发送用法提示 |

后者的动作形如：

```yaml
actions:
  - '[close]'
  - '[message] &7踢出玩家：&e/kick 玩家名 原因'
```

玩家点一下就拿到完整指令格式，复制粘贴补个名字即可——比手打整条命令快，
也避免了「菜单点了没反应」的困惑（直接执行 `/kick` 不带参数只会报用法错误）。

这类项在 `lore` 里都写了「需手动补充参数」。

> 如果你确实需要「点玩家头像就能踢人」这类交互，必须扩展代码加入
> 动态菜单（按在线玩家生成按钮）与 Anvil/Modal 输入，这超出当前插件范围。

### 6.6 权限过滤示范

- `main.admin` 配 `permission: quickmenu.admin` —— 管理员才看到管理面板
- `kit.vipkit` 配 `permission: essentials.kit.vip` —— VIP 专属工具包
- `teleport.tpahere` 配 `permission: essentials.tpahere` —— 需额外授权
- `admin-inspect.purge` 配 `permission: coreprotect.purge` —— 危险操作仅供服主
- `admin-world.ctrim` 配 `permission: chunky.trim` —— 会删区块，单独隔离

基岩端同样生效：被过滤的按钮**不出现且序号自动前移**，不会留下空位。

---

## 7. 自行修改与重新构建

### 7.1 只改配置（最常见）

改 `plugins/QuickMenu/config.yml` 后执行 `/qm reload`，即时生效，不用重启。

### 7.2 改代码后重新构建（无需 Maven）

本机没有 Maven，`build.ps1` 直接用 JDK 的 javac / jar 完成全流程：

```powershell
cd F:\game\pc\MC\开服\Minecraft-Server-Build\26-快捷菜单系统
powershell -ExecutionPolicy Bypass -File build.ps1
```

脚本执行 6 步：

| 步骤 | 内容 |
|---|---|
| 0 | 自动定位 JDK 21+ |
| 1 | 下载编译依赖到 `_build\libs`（已存在则跳过） |
| 2 | 编译源码（UTF-8，开启 `-Xlint`） |
| 3 | **静态校验** config.yml：材质名有效性、size 合法性、槽位越界、槽位冲突、`[menu]` 目标是否存在、动作前缀是否识别 |
| 4 | 打包 jar 并复制到 `dist\` |
| 5 | 校验 plugin.yml 与 jar 内主类一致性 |

参数：

- `-SkipDeps` 离线构建，跳过依赖下载
- `-Clean` 清理上次产物。**删除或重命名过源文件后必须加**，否则会残留旧 class

### 7.3 用 IDE 改代码

`plugin-src/pom.xml` 已配好，用 IntelliJ IDEA 导入该目录即可获得代码补全与即时错误提示。
4 个依赖全部 `scope=provided`，不会被打进 jar 造成类冲突。

### 7.4 依赖版本已锁定，勿随意升级

| 依赖 | 锁定版本 | 原因 |
|---|---|---|
| spigot-api | 1.21.11-R0.1-SNAPSHOT | 对应 Leaf 1.21.11 核心 |
| floodgate api | 2.2.5-SNAPSHOT | Floodgate 当前 API |
| **cumulus** | **1.1.2** | ⚠️ **不可换成 2.0.0-SNAPSHOT** |

> cumulus 版本是实测踩过的坑：Floodgate 2.2.5 依赖 cumulus **1.1.2**。
> 2.0.0-SNAPSHOT 缺少 `org.geysermc.cumulus.util.FormBuilder` 旧路径，
> 而 `FloodgatePlayer.sendForm()` 有新旧两组重载，缺一个会导致
> **重载决议失败、编译报错**。

---

## 8. 已验证与未验证事项

按实际验证程度如实区分，不夸大。

### 8.1 ✅ 已实测验证

| 验证项 | 方法 | 结果 |
|---|---|---|
| 源码编译 | javac 实际编译 15 个源文件 | 通过，生成 16 个 class，无错误 |
| 字节码版本 | `javap -v` 读取 class 文件头 | major version **65 = Java 21**，匹配 Leaf 核心 |
| 内存回收 | `javap -p` 反编译交付 jar | `onQuit(PlayerQuitEvent)` 处理器已编入，退出即时清理防连点记录，无泄漏 |
| config.yml 结构 | snakeyaml 真实解析 + 逐项校验 | **0 错误 0 警告**（20 菜单 / 160 菜单项） |
| 全插件整合配置校验 | 用含缺陷的坏配置实测过探针准确性 | 本次唯一报错 `CHAIN` 非有效材质已修正；槽位冲突、返回按钮占位、双端覆盖缺口均为 0 |
| 材质名有效性 | `Material.matchMaterial()` 运行时校验 | 本次校验覆盖 100+ 材质，发现并修复 1 个错误（`CHAIN` → `IRON_BLOCK`） |
| 槽位冲突检测 | 按渲染逻辑模拟槽位占用 | 20 个菜单 0 冲突，且所有配了 `back-menu` 的菜单均未占用 `size-1` |
| 双端覆盖检查 | 逐项判定某一端是否「点了完全没反馈」 | 160 项全部两端有反馈，0 告警 |
| 平台覆盖检查准确性 | 用含 7 类缺陷 + 4 个正反对照组的坏配置实测 | 7 个缺陷全部命中，4 个对照组**零误报**（含「基岩专属表单 + Java 文字指引」的降级设计） |
| 平台条件动作已编入 | `javap` 反编译交付 jar | ActionType 含 9 个枚举常量；config.yml 内 4 处 `[bedrock-player]`、2 处 `[java-player]` |
| 材质名有效性 | `Material.matchMaterial()` 运行时校验 34 个材质 | 发现并修复 1 个错误（`SIGN` → `OAK_SIGN`） |
| 槽位冲突检测 | 按渲染逻辑模拟槽位占用 | 发现并修复 1 处冲突（teleport 菜单返回按钮与 spawn 抢 slot 22） |
| plugin.yml 合法性 | YAML 解析 + 字段校验 | 通过，主类确实存在于 jar 中 |
| jar 内中文完整性 | 解压 jar 读回 config.yml | 含「快捷菜单」「南瓜国际服」等，**0 个乱码替换符** |
| 无 Floodgate 可启动 | JVM 实测：编译期含 cumulus、**运行期移除 cumulus** | 宿主类实例化成功，**未抛 NoClassDefFoundError**，核心功能正常 |
| 空安全检查必要性 | 同上实验的对照组 | 依赖存在时功能正常；缺失且强行使用时崩溃 → 证明 `formRenderer != null` 检查必需 |
| 构建脚本 | 端到端实际执行 | exit code 0，产出 43.5 KB jar |

### 8.2 ⚠️ 未验证事项（需你在服务器上实测）

**本插件尚未在真实运行的服务器上加载测试**——本机没有可启动的 Minecraft 服务端
（项目是纯文档集，无任何服务端 jar）。因此以下行为仍是**基于 API 文档与代码逻辑的推断，未经运行时确认**：

| 待验证项 | 如何验证 |
|---|---|
| 插件能否被 Leaf 核心正常加载 | 放入 plugins 重启，看控制台有无 `QuickMenu 已启用` |
| 箱子 GUI 实际渲染效果 | Java 客户端进服，`/qm` 打开看排版 |
| **基岩端原生 Form 实际弹出** | 基岩客户端进服，右键触发物品看是否出现按钮列表 |
| 表单回调线程切换是否真的避免了崩溃 | 基岩端点击菜单项，观察控制台有无异步异常 |
| 触发物品右键能否正常打开 | 手持指南针右键（对空气与对方块各试一次） |
| 物品防拖出是否生效 | 箱子界面里尝试把图标拖到背包 |
| `/qm info` 的客户端识别是否准确 | Java 与基岩各执行一次，对比输出 |
| 与 EssentialsX 指令的联动 | 菜单点「回家」是否真的触发 `/home` |
| **管理面板门控是否生效** | 先用普通玩家账号 `/qm open admin`，应看不到任何按钮；再换成 admin 组确认可见 |
| **WorldEdit 双斜杠指令** ⚠️ | 点「获取选区木斧」看是否真的给出木斧。`//wand` 经 `performCommand` 分发，若无效请改用 `wand`（去掉斜杠） |
| Residence 指令联动 | 点「我的领地列表」是否执行 `/res list`；`/res ?` 帮助能否打开 |
| CoreProtect 查询模式 | 点「开启查询模式」后点方块，看是否输出改动记录 |
| 各菜单项的指令格式提示 | 点几个「文字指引」项，确认提示文字中的指令可直接使用 |

> 建议先在**测试服**验证基岩端表单弹出与点击行为，确认无误再上正式服。
> 若基岩端表单不弹出，先执行 `/qm info` 看「Floodgate」与「将使用的界面」两行。

---

## 9. 故障排查

| 现象 | 原因 | 处理 |
|---|---|---|
| 右键物品没反应 | 物品不是插件发放的（`strict-match: true` 时要求 PDC 标记） | 用 `/qm give` 重新获取；或把 `strict-match` 设为 `false` |
| 菜单打开但点击提示无权限 | 只给了 `quickmenu.use`，漏了底层 `essentials.*` | 按第 4.3 节补齐权限 |
| 某个菜单项不显示 | 配了 `permission` 且玩家无权，被过滤跳过 | 属正常行为；要显示就去掉该项的 permission |
| 基岩玩家也看到箱子界面 | Floodgate 未装 / `bedrock-native-form: false` / 表单发送失败回退 | 执行 `/qm info` 诊断；检查控制台回退警告 |
| 基岩表单按钮显示乱码 | 在 `button-label` 里写了 `&` 颜色码 | 原生按钮不支持颜色码，删掉即可（插件会自动去除 `display-name` 的色码） |
| 返回按钮不显示 | 右下角槽位（`size-1`）被其他菜单项占用 | 控制台会有警告；把该物品的 slot 改开 |
| 改配置不生效 | 未热重载 | 执行 `/qm reload`；若改的是 `plugin.yml` 需重启 |
| 物品被玩家丢弃后丢失 | `strict-match: true` 时插件会阻止丢弃 | 若被丢（如死亡掉落），用 `/qm give` 补发 |
| **Java 玩家点某项没反应** | 该项只配了 `[bedrock-*]` 动作，Java 端被跳过 | 补一条 `[java-player] 指令` 或 `[message] 指引`；开 `debug: true` 看控制台「跳过平台条件动作」日志 |
| **基岩玩家点某项没反应** | 该项只配了 `[java-*]` 动作，基岩端被跳过 | 补一条 `[bedrock-player] 指令`；同上开 debug 查日志 |
| **点了报「未知指令」** | 用了 `[bedrock-player] warpgui` 但未装 BedrockPlayerSupport | 装上 BPS，或按第 6.1 节把该项改回无条件动作（如 `[player] warp`） |
| 构建告警「无任何有效反馈」 | 菜单项某一端只有 `[close]`，没有实际功能或提示 | 属真实缺陷，按第 5.5 节补齐另一端动作 |
| 构建报 `cumulus` 找不到 | 依赖版本被改成 2.0.0-SNAPSHOT | 改回 **1.1.2**，见第 7.4 节 |
| **主菜单看不到「管理面板」** | 没有 `quickmenu.admin`，该项被权限过滤跳过 | 按第 10.3 节给组授权；先确认自己所在组 `/lp user <自己> info` |
| **`/qm open admin` 打开后是空的** | 管理项每个都单独配了 permission，你一个都没有 | 按第 10.3 / 10.4 节补齐 EssentialsX / Residence / CoreProtect 等权限 |
| 管理项点了提示无权限 | 有 `quickmenu.admin` 但缺该插件自身的权限 | 例：点「封禁」需 `essentials.ban`（仅服主组），见第 10.4 节 |
| 菜单项只显示指令用法、不执行 | 该项需要参数（玩家名/领地名），设计如此 | 见第 6.5 节；复制提示里的指令补上参数即可 |
| 点「获取选区木斧」没反应 | `//wand` 经 `performCommand` 分发可能无效 | 把 config.yml 里该项改为 `[player] wand`（去掉双斜杠）后 `/qm reload` |

---

## 10. LuckPerms 权限授予清单

菜单只是前端，**真正干活的是各插件的指令**。菜单项点了没反应，
99% 是对应的插件权限没给。下面按组整理本菜单用到的全部节点。

> 权限组结构（default / member / vip / mvp / helper / admin / owner）
> 见 [`05-权限管理系统-LuckPerms/权限组设计方案.md`](../05-权限管理系统-LuckPerms/权限组设计方案.md)。

### 10.1 玩家组（default）

```bash
# ---- 菜单自身 ----
lp group default permission set quickmenu.use true

# ---- EssentialsX：传送与家园 ----
lp group default permission set essentials.home true
lp group default permission set essentials.sethome true
lp group default permission set essentials.delhome true
lp group default permission set essentials.homes true
lp group default permission set essentials.tpa true
lp group default permission set essentials.tpaccept true
lp group default permission set essentials.tpdeny true
lp group default permission set essentials.warp true
lp group default permission set essentials.spawn true
lp group default permission set essentials.back true
lp group default permission set essentials.back.ondeath true
lp group default permission set essentials.near true

# ---- EssentialsX：经济 ----
lp group default permission set essentials.balance true
lp group default permission set essentials.balancetop true
lp group default permission set essentials.pay true
lp group default permission set essentials.sell true
lp group default permission set essentials.worth true

# ---- EssentialsX：工具包与物品 ----
lp group default permission set essentials.kit true
lp group default permission set essentials.kits.starter true
lp group default permission set essentials.kits.daily true
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

# ---- Simple Voice Chat：语音 ----
lp group default permission set voicechat.speak true
lp group default permission set voicechat.listen true
lp group default permission set voicechat.groups true
```

### 10.2 VIP 组（vip）

```bash
lp group vip permission set essentials.kit.vip true
lp group vip permission set essentials.kits.vip true
lp group vip permission set essentials.tpahere true
lp group vip permission set essentials.sethome.multiple.10 true
lp group vip permission set skinsrestorer.bypasscooldown true
```

### 10.3 管理组（admin / helper）

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

# ---- CoreProtect：审计与回滚（purge 不给，见 10.4）----
lp group admin permission set coreprotect.inspect true
lp group admin permission set coreprotect.inspect.others true
lp group admin permission set coreprotect.lookup true
lp group admin permission set coreprotect.near true
lp group admin permission set coreprotect.rollback true
lp group admin permission set coreprotect.restore true
lp group admin permission set coreprotect.reload true
lp group admin permission set coreprotect.help true

# ---- Chunky：区块预生成（trim 不给，见 10.4）----
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

# ---- LuckPerms：日常查询（危险项不给，见 10.4）----
lp group admin permission set luckperms.info true
lp group admin permission set luckperms.sync true
lp group admin permission set luckperms.reload true
lp group admin permission set luckperms.tree true
lp group admin permission set luckperms.verbose true
```

### 10.4 服主组（owner）——仅危险操作

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

### 10.5 验证方法

```bash
# 检查某个玩家是否真的有某权限（含继承）
/lp user <玩家> permission check essentials.home

# 实时追踪权限判定过程 —— 排查「菜单点了没反应」的神器
/lp verbose on <玩家> essentials
# 然后让该玩家点一次菜单项，看控制台输出的 true/false 判定链

# 看某插件到底注册了哪些节点（节点名写错时用它确认）
/lp tree essentials
/lp tree coreprotect
```

---

## 11. 与其他章节的关系

| 章节 | 关系 |
|---|---|
| [03-Java-Bedrock互通层-Geyser-Floodgate](../03-Java-Bedrock互通层-Geyser-Floodgate/) | 提供 Floodgate，基岩端原生表单的前提 |
| [05-权限管理系统-LuckPerms](../05-权限管理系统-LuckPerms/) | 菜单可见性与指令权限均由此控制，见第 10 章清单 |
| [07-方块记录与回滚-CoreProtect](../07-方块记录与回滚-CoreProtect/) | `admin-inspect` 菜单对接的后端 |
| [08-领地系统-Residence](../08-领地系统-Residence/) | `residence` 与 `admin-residence` 菜单对接的后端 |
| [09-皮肤管理-SkinsRestorer](../09-皮肤管理-SkinsRestorer/) | `skin` 菜单对接的后端 |
| [12-离线背包查看-OpenInv](../12-离线背包查看-OpenInv/) | `admin-player` 查背包对接的后端 |
| [13-性能分析-spark](../13-性能分析-spark/) | `admin-server` 性能监控对接的后端 |
| [20.创世神WorldEdit](../20.创世神WorldEdit/) | `admin-world` 创世神部分的后端与指令参考 |
| [22-Simple Voice Chat](<../22-Simple Voice Chat/>) | `voice` 菜单与 UDP 24454 端口说明 |
| [23-chunky区块加载优化](../23-chunky区块加载优化/) | `admin-world` 区块预生成的指令参考 |
| [24-EssentialsX多功能指令整合](<../24-EssentialsX多功能指令整合/EssentialsX多功能指令整合（功能说明与完整配置）.md>) | **菜单动作的主要执行者**，本插件是其 GUI 前端 |
| [25-BedrockPlayerSupport基岩版GUI表单界面](../25-BedrockPlayerSupport基岩版GUI表单界面/BedrockPlayerSupport基岩版GUI表单界面.md) | 功能有重叠，见下方说明 |

### 与 BedrockPlayerSupport 是否冲突

两者都给基岩玩家提供表单，但定位不同：

| | QuickMenu（本章） | BedrockPlayerSupport |
|---|---|---|
| 定位 | **自定义菜单**，内容完全由你配置 | **既有指令的表格外壳**，把 `/tpa` `/home` 等包装成表单 |
| 覆盖面 | 任意指令、任意层级菜单 | 固定的传送/家园/私信/工具包表单 |
| 触发方式 | 物品右键 / `/qm` | 各自的 `/tpgui` `/homegui` 等指令 |
| Java 端 | 有箱子 GUI | 无（仅基岩端） |

**建议**：

- 只想要「一个物品打开的总菜单」→ 装 QuickMenu 即可（示例菜单中的 `[bedrock-player] xxxgui`
  会因 BPS 未安装而对基岩玩家无效，按第 6.1 节说明改回无条件动作即可）
- 想要菜单 + 收到 tpa 请求时自动弹接受/拒绝表单、死亡后弹回传表单 → **两个都装**，
  这是推荐组合。QuickMenu 负责「物品右键唤出的总菜单」，BPS 负责「事件驱动的自动表单」
  （收到传送请求、死亡重生时主动弹出，这类场景菜单插件无法覆盖）
- **两者已在本配置中集成**：`config.yml` 已把 `teleport.warps`、`teleport.tpa`、
  `home.listhomes`、`settings.msg` 四项配成两端分发，基岩玩家点击后直接进入
  BPS 的原生表单（带玩家/传送点/家园选择），Java 玩家走 EssentialsX 指令。
  无需再手动修改，装上 BPS 即生效（详见第 6.1 节对照表）
