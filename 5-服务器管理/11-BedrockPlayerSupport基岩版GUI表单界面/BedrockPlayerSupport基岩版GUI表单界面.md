# BedrockPlayerSupport 基岩版 GUI 表单界面

> ⚠️ 注意区分同名插件：本文档是作者 **DongShao** 的 **BedrockPlayerSupport**（GUI 表单界面）；**BedrockPlayerManager**（ofunny）是另一个插件，别下错。

## 简介

专为 GeyserMC 互通服设计，给基岩版玩家提供 GUI 表单界面，点按钮即可传送、回家、领工具包，免去敲指令。前置：GeyserMC + Floodgate（必须）。

## 功能清单（运行服当前状态）

| 分类 | 指令 | 状态 |
| --- | --- | --- |
| 传送 | `/tpgui`、`/warpgui`、收到 tpa/tpahere 自动弹接受/拒绝表单 | ✅ 启用 |
| 家园 | `/homegui`、`/phomegui` | ✅ 启用 |
| 消息 | `/msggui` | ✅ 启用 |
| 工具包 | `/kitgui` | ✅ 启用 |
| 经济 | `/paygui` | ❌ 关闭（`form.money.enable=false`） |
| 点券 | `/pointsgui` | ❌ 关闭（`form.points.enable=false`） |
| 自动注册/登录 | — | ❌ 关闭（`auth.register/login.enable=false`） |
| 死亡回传 | 重生后弹"返回死亡地点"表单 | ❌ 关闭（`form.back.enable=false`，2026-09-27） |
| 加入/退出命令 | — | ❌ 关闭（`general.join/quit-commands.enable=false`） |

## 配置（`plugins/BedrockPlayerSupport/config.yml`）

```yaml
plugin:
  language: 'zh_CN'      # 语言
  auth: 'auto'           # 登录插件：auto/authme/catseedlogin/nexauth/other/none
  check-update: true     # 启动检测更新
  basic: 'auto'          # 基础插件：auto/cmi/essentialsx/huskhomes/advancedteleport/sunlight/none
  support-papi: true     # 表单支持 PlaceholderAPI

form:
  back:
    open-delay-time: 20  # 死亡回传表单延迟打开（刻，20刻=1秒）
    enable: false        # 死亡回传表单（默认 true，运行服已关闭）
    command: '/back'     # 返回死亡点命令（部分插件用 /dback 可在此改）
  teleport:
    receive:
      enable: true       # 收到 tpa/tpahere 自动弹出接受/拒绝表单
    enable: true         # /tpgui
    cross-server: false  # 跨服（仅 HuskHomes）
  phome:
    enable: true         # /phomegui（仅 HuskHomes）
  msg:
    enable: true         # /msggui
  kit:
    enable: true         # /kitgui
  warp:
    enable: true         # /warpgui
  money:
    enable: false        # /paygui（运行服关闭）
    pay-command: 'pay %playerName% %amount%'
  home:
    enable: true         # /homegui
  points:
    enable: false        # /pointsgui（运行服关闭）
    pay-command: '/points pay %playerName% %amount%'

general:
  quit-commands:
    enable: false        # 退出命令（运行服关闭）
    commands:
      - '[CONSOLE] say Bedrock Player %playerName% quit the server'
  join-commands:
    enable: false        # 加入命令（运行服关闭）
    commands:
      - '[CONSOLE] say Welcome Bedrock Player %playerName%'

auth:
  login:
    enable: false        # 自动登录（运行服关闭）
    command: 'forcelogin %playerName%'
  register:
    enable: false        # 自动注册（运行服关闭）
    password-length: 16
```

> 改配置后需 `/bps reload` 或重启服务器才生效。

## 权限（LuckPerms）

```
lp group default permission set bedrockplayersupport.tpgui true
lp group default permission set bedrockplayersupport.homegui true
lp group default permission set bedrockplayersupport.msggui true
lp group default permission set bedrockplayersupport.kitgui true
lp group default permission set bedrockplayersupport.warpgui true
lp group default permission set bedrockplayersupport.phomegui true
lp group admin permission set bedrockplayersupport.* true
```

> ⚠️ `bedrockplayersupport.form.backdeath`（死亡回传表单权限）默认 `true`，所有玩家自动拥有、无需授权。关闭死亡表单：config `form.back.enable=false`；双保险可再执行 `lp group default permission set bedrockplayersupport.form.backdeath false`。

## 与 EssentialsX 配合

| 玩家操作 | 表单 | 底层命令 |
| --- | --- | --- |
| 回家 | `/homegui` | → `/home <名称>` |
| 传送到玩家 | `/tpgui` | → `/tpa <玩家>` |
| 收到传送请求 | 自动弹出 | → `/tpaccept` 或 `/tpdeny` |
| 领工具包 | `/kitgui` | → `/kit <名称>` |
| 传送点 | `/warpgui` | → `/warp <名称>` |

## 语言文件（`lang/zh_CN.yml`）

```yaml
prefix: '&6[互通服] &r'

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

homegui:
  title: '我的家园'
  content: '点击家园即可传送'
  no-homes: '{prefix}&c你还没有设置任何家园，使用 /sethome 设置一个吧'

phomegui:
  title: '公共家园'
  content: '点击即可访问公共家园'
  no-homes: '{prefix}&c当前没有可用的公共家园'

msggui:
  title: '发送私信'
  content: '选择要发送私信的玩家'
  no-players: '{prefix}&c当前没有其他在线玩家'

kitgui:
  title: '工具包'
  content: '选择要领取的工具包'
  no-kits: '{prefix}&c当前没有可用的工具包'
  cooldown: '{prefix}&c该工具包还在冷却中，请等待 {time}'

death-back:
  title: '返回死亡地点'
  content: '是否返回你的死亡地点？'
  yes: '返回死亡地点'
  no: '不返回'
  teleported: '{prefix}已返回死亡地点'
  no-location: '{prefix}&c没有找到死亡地点记录'

form:
  back: '返回'
  next-page: '下一页'
  prev-page: '上一页'
  close: '关闭'
```

## 注意事项

- **必须**配合 GeyserMC + Floodgate 使用，纯 Java 服无需安装
- 表单仅对基岩版玩家生效
- 表单底层调用 EssentialsX 命令，玩家需同时有对应 EssentialsX 权限（如 `/homegui` 需 `essentials.home`）
- 若用 HuskHomes 替代 EssentialsX，`/phomegui` 自动适配；自动注册需 AuthMe 配合
- 建议为基岩版玩家配中文别名指令（`commands.yml`），与 GUI 表单互补
