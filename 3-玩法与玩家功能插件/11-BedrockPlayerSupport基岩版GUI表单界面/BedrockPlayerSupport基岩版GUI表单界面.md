以下是 BedrockPlayerSupport 的完整配置方案与文档。

> ⚠️ **重要提示：** 搜索结果显示 SpigotMC 上存在两个名称相似的插件，请注意区分：
>
> - **BedrockPlayerSupport**（作者 DongShao）— 本文档介绍的插件，提供 GUI 表单界面
> - **BedrockPlayerManager**（作者 ofunny）— 另一个不同的插件，主要用于加入时执行命令和权限管理
>
> 请确保下载的是 **DongShao** 开发的 BedrockPlayerSupport。

---

### 📖 BedrockPlayerSupport 完整文档

#### 插件简介

BedrockPlayerSupport 是专为 GeyserMC 互通服设计的辅助插件，核心目标是为基岩版玩家提供 **GUI 表单界面**，替代不方便的指令输入，让基岩版玩家通过点击按钮即可完成传送、回家、领取工具包等操作。

---

#### 功能清单

| 分类       | 指令          | 功能说明                                          |
| ---------- | ------------- | ------------------------------------------------- |
| **传送**   | `/tpgui`      | 打开传送请求表单，选择要传送的玩家（运行服已启用） |
|            | `/warpgui`    | 打开传送点列表表单，选择传送点传送（已启用）       |
|            | 自动弹出      | 收到 tpa/tpahere 请求时自动弹出接受/拒绝/忽略表单（receive.enable=true） |
| **家园**   | `/homegui`    | 打开个人家园列表表单，快速传送回家（已启用）      |
|            | `/phomegui`   | 打开公共家园列表表单（仅 HuskHomes 基础插件时可用，已启用） |
| **消息**   | `/msggui`     | 打开私信发送表单（已启用）                        |
| **工具包** | `/kitgui`     | 打开工具包领取表单（已启用）                      |
| **经济**   | `/paygui`     | 经济支付表单（**运行服 form.money.enable=false，关闭**） |
| **点券**   | `/pointsgui`  | 点券支付表单（**运行服 form.points.enable=false，关闭**） |
| **其他**   | 自动注册/登录 | ⚠️ 运行服 `auth.register/auth.login` 均为 **false**，**未启用**自动随机密码注册登录 |
|            | 死亡回传      | 重生后自动弹出表单，返回死亡点（back.enable=true，命令 `/back`） |
|            | 加入/退出命令 | 支持基岩版玩家加入/退出时自动执行命令（运行服 join/quit-commands.enable=false） |

---

#### 支持的插件集成

BedrockPlayerSupport 可与其他插件联动：

| 插件                 | 集成内容               |
| -------------------- | ---------------------- |
| **EssentialsX**      | 家园、传送、工具包表单 |
| **HuskHomes**        | 家园表单、公共家园表单 |
| **AdvancedTeleport** | 传送表单               |
| **SunLight**         | 功能集成               |
| **AuthMe**           | 自动注册/登录          |
| **PlaceholderAPI**   | 变量支持               |

---

#### 安装步骤

1. 下载 BedrockPlayerSupport 插件 jar 文件
2. 放入服务器的 `plugins/` 文件夹
3. 确保已安装 GeyserMC + Floodgate
4. 启动服务器，插件会自动生成配置文件
5. 根据需要修改配置文件

---

#### config.yml 配置示例

> ✅ **运行服校准（2026-09-16）**：以下结构为运行服 `plugins\BedrockPlayerSupport\config.yml`（v2.1.1，作者 DongShaoNB）**真实命名空间结构**。早期文档中按 `tpa:/tpgui:/homegui:` 平铺的写法为臆测模板，与实际不符，已替换为真实结构。关键项：语言 `zh_CN`（第9行）。

```yaml
# ===== plugin 通用设置 =====
plugin:
  # 插件语言（实际值：'zh_CN'，见 config.yml 第9行）
  language: 'zh_CN'
  # 登录插件：auto/authme/catseedlogin/nexauth/other/none
  # 实际值：'auto'（第18行）
  auth: 'auto'
  # 启动时检测更新
  # 实际值：true（第21行）
  check-update: true
  # 基础插件：auto/cmi/essentialsx/huskhomes/advancedteleport/sunlight/none
  # 实际值：'auto'（第28行，自动检测到 EssentialsX）
  basic: 'auto'
  # 表单支持 PlaceholderAPI
  # 实际值：true（第33行）
  support-papi: true

# ===== form 各表单开关 =====
form:
  # 死亡回传（重生后自动打开返回死亡点表单）
  back:
    open-delay-time: 20          # 打开延迟（刻），第43行
    enable: true                 # 第46行
    command: '/back'             # 返回死亡点命令，第51行
  teleport:
    receive:
      enable: true               # 收到 tpa/tpahere 自动弹出表单，第57行
    enable: true                 # /tpgui，第61行
    cross-server: false          # 跨服（仅 HuskHomes），第66行
  phome:
    enable: true                 # /phomegui 公共家（仅 HuskHomes），第73行
  msg:
    enable: true                 # /msggui，第78行
  kit:
    enable: true                 # /kitgui 工具包，第85行
  warp:
    enable: true                 # /warpgui 传送点，第90行
  money:
    enable: false               # /paygui 经济支付表单（运行服关闭），第95行
    pay-command: 'pay %playerName% %amount%'   # 第102行
  home:
    enable: true                 # /homegui，第107行
  points:
    enable: false               # /pointsgui 点券表单（运行服关闭），第112行
    pay-command: '/points pay %playerName% %amount%'  # 第119行

# ===== 加入/退出命令 =====
general:
  quit-commands:
    enable: false                # 第128行（默认关闭，示例命令保留在配置内）
    commands:
      - '[CONSOLE] say Bedrock Player %playerName% quit the server'
      - '[PLAYER] me I''m a Bedrock Player %playerName%'
  join-commands:
    enable: false                # 第148行
    commands:
      - '[CONSOLE] say Welcome Bedrock Player %playerName%'
      - '[PLAYER] me I''m a Bedrock Player %playerName%'

# ===== 自动登录/自动注册 =====
# ⚠️ 运行服实际均为关闭（第168、182行）：
#   自动登录 enable: false；自动注册 enable: false
#   即基岩版玩家加入时不会自动注册/登录，需自行通过登录流程
auth:
  login:
    enable: false                # 第168行
    command: 'forcelogin %playerName%'   # 第177行
  register:
    enable: false                # 第182行
    password-length: 16          # 第185行
```

> 📌 **与早期文档的差异提醒**：运行服 `auth.register.enable=false`、`auth.login.enable=false`，即**未启用**"基岩版玩家加入自动随机密码注册并登录"；`form.money`（/paygui）与 `form.points`（/pointsgui）也为关闭。若需开启自动注册，需先在 `plugin.auth` 指定登录插件并把对应 `enable` 改为 `true`（涉及登录/权限，建议用户决策后再改）。

---

#### 权限配置（LuckPerms）

```
# 默认玩家组 - 基岩版 GUI 权限
lp group default permission set bedrockplayersupport.tpgui true
lp group default permission set bedrockplayersupport.homegui true
lp group default permission set bedrockplayersupport.msggui true
lp group default permission set bedrockplayersupport.kitgui true
lp group default permission set bedrockplayersupport.warpgui true
lp group default permission set bedrockplayersupport.phomegui true

# 管理员组 - 完整权限
lp group admin permission set bedrockplayersupport.* true
```

---

#### 与 EssentialsX 的配合使用

BedrockPlayerSupport 作为 GUI 前端，底层调用 EssentialsX 的命令。配合方式如下：

| 基岩版玩家操作 | GUI 表单                | 底层调用                               |
| -------------- | ----------------------- | -------------------------------------- |
| 想传送回家     | `/homegui` → 选择家园   | → EssentialsX `/home <名称>`           |
| 想传送到玩家   | `/tpgui` → 选择玩家     | → EssentialsX `/tpa <玩家>`            |
| 收到传送请求   | 自动弹出表单            | → EssentialsX `/tpaccept` 或 `/tpdeny` |
| 想领取工具包   | `/kitgui` → 选择工具包  | → EssentialsX `/kit <名称>`            |
| 想传送到传送点 | `/warpgui` → 选择传送点 | → EssentialsX `/warp <名称>`           |

---

#### 中文语言文件示例

文件路径：`plugins/BedrockPlayerSupport/lang/zh_CN.yml`

```yaml
# BedrockPlayerSupport 中文语言文件

prefix: '&6[互通服] &r'

# 传送相关
tpa:
  request-received: '{prefix}&e{player} &f请求传送到你的位置'
  request-sent: '{prefix}已向 &e{player} &f发送传送请求'
  accepted: '{prefix}已接受 &e{player} &f的传送请求'
  rejected: '{prefix}已拒绝 &e{player} &f的传送请求'
  ignored: '{prefix}已忽略 &e{player} &f的传送请求'

tpgui:
  title: '选择传送目标'
  content: '点击玩家头像即可发送传送请求'
  no-players: '{prefix}&c当前没有其他在线玩家'

warpgui:
  title: '传送点列表'
  content: '点击传送点即可传送'
  no-warps: '{prefix}&c当前没有可用的传送点'

# 家园相关
homegui:
  title: '我的家园'
  content: '点击家园即可传送'
  no-homes: '{prefix}&c你还没有设置任何家园，使用 /sethome 设置一个吧'

phomegui:
  title: '公共家园'
  content: '点击即可访问公共家园'
  no-homes: '{prefix}&c当前没有可用的公共家园'

# 消息相关
msggui:
  title: '发送私信'
  content: '选择要发送私信的玩家'
  no-players: '{prefix}&c当前没有其他在线玩家'

# 工具包相关
kitgui:
  title: '工具包'
  content: '选择要领取的工具包'
  no-kits: '{prefix}&c当前没有可用的工具包'
  cooldown: '{prefix}&c该工具包还在冷却中，请等待 {time}'

# 死亡回传
death-back:
  title: '返回死亡地点'
  content: '是否返回你的死亡地点？'
  yes: '返回死亡地点'
  no: '不返回'
  teleported: '{prefix}已返回死亡地点'
  no-location: '{prefix}&c没有找到死亡地点记录'

# 通用
form:
  back: '返回'
  next-page: '下一页'
  prev-page: '上一页'
  close: '关闭'
```

---

#### 注意事项

- BedrockPlayerSupport **必须**配合 GeyserMC + Floodgate 使用，单独的 Java 服务器无需安装
- 插件版本截至 2026 年 2 月为 v2.1.0，后续版本可能有配置项变更，建议以插件生成的默认配置为准
- 表单功能仅对基岩版玩家生效（可通过 `bedrock-only` 配置项调整）
- 如果同时使用 HuskHomes 替代 EssentialsX 的家园系统，`/phomegui` 公共家园表单会自动适配
- 自动注册功能需要 AuthMe 插件配合，如果服务器不使用登录插件可关闭此功能
- 建议为基岩版玩家设置中文别名指令（参考上一轮对话中的 `commands.yml` 配置），与 GUI 表单互补

---

> 📌 **总结：** BedrockPlayerSupport 是互通服的"基岩版玩家体验优化器"，通过 GUI 表单让基岩版玩家无需记忆复杂指令即可完成常用操作，与 EssentialsX 配合使用效果最佳。

---

需要我再帮你配一份GeyserMC的config.yml吗？互通服的基础配置也很关键，比如端口、桥接模式这些。
