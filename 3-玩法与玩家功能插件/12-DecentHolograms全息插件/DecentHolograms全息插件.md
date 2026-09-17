# 12. 全息投影系统 — DecentHolograms

本文介绍 DecentHolograms（简称 DH）这款不依赖 ProtocolLib 的全息投影插件，讲解在世界中生成浮动文字、物品、头颅与方块，以及点击交互、多页翻页、动画和按权限隐藏等玩法。内容按安装、目录结构、config.yml、全息文件格式、命令与权限、实战示例和排错顺序组织，适合 Paper/Spigot/Folia 服主照做。

**当前版本**: DecentHolograms 2.10.1（稳定版） | **MC 要求**: Java 1.8.9 – 26.2（含 1.21.11）

**官方网站**: https://www.decentholograms.eu | **Modrinth**: https://modrinth.com/plugin/decentholograms

**官方文档(Wiki)**: https://wiki.decentholograms.eu/ | **GitHub**: https://github.com/DecentSoftware-eu/DecentHolograms

**下载**: https://modrinth.com/plugin/decentholograms/versions | **协议**: GPL-3.0-or-later（开源免费）

> 适用平台：**Paper / Spigot / Folia**（需要 Paper API，CraftBukkit 不可用）。**无任何前置依赖**，不依赖 ProtocolLib。南瓜生存服为 Paper 服务端，直接放入 `plugins/` 即可。

> ⚠️ **命令写法警告（重要）**：网上很多旧博客 / 第三方教程写的是过时或错误语法，本服一律以**下方官方语法**为准：
> - ❌ 旧写法 `/dh create <名>`、`/dh line add <名> <内容>`、`/dh line action add ...`、`/dh gui <名>` —— **均已失效或不存于 2.x**。
> - ❌ 旧权限节点 `decentholograms.use` / `decentholograms.admin` / `decentholograms.*` —— **前缀错误**，正确的是 `dh.*`。
> - ✅ 正确主命令 `/dh`，全息用 `/dh hologram create`，加行用 `/dh lines add <名> <页> [内容]`，点击动作用 `/dh p addaction <名> <页> <点击类型> <动作>`。
> - 动作数据是**冒号分隔**：`TELEPORT:world:0:64:0`（不是 `world, 0, 64, 0`）。

## 功能说明

DecentHolograms（简称 DH）是一款轻量但功能极强的全息投影插件，可在世界任意坐标生成**浮动的全息文字 / 物品 / 头颅 / 实体 / 方块**，广泛用于：

- 出生点欢迎语、服务器规则、在线人数实时展示（配合 PlaceholderAPI）；
- 商店 / 传送点指示牌，点击直接传送或执行命令；
- 排行榜（ajLeaderboards / CMI 等 PAPI 扩展驱动）；
- 战斗飘字（伤害 / 治疗显示，可开关）；
- 多页全息（翻页菜单）、动画文字（彩虹 / 滚动 / 波浪）。

核心优势：

- **无 ProtocolLib 依赖**——直接发包，大型在线服务器比 Holographic Displays + ProtocolLib 方案更省 TPS。
- **按全息独立可视距离**：超出 `display-range` 的玩家不收包，静态标牌可把距离调小省带宽。
- **文件存储**：每个全息存为 `holograms/<name>.yml`，迁移 / 备份就是复制文件，无需数据库。
- **每全息 / 每行权限**：可设置“只有特定权限节点才看得见”的全息（如仅 VIP 可见的公告）。

## 安装与前置

1. 确认服务端为 **Paper / Spigot / Folia**（运行 `/version` 查看；CraftBukkit 会加载失败）。
2. 从 Modrinth 下载与 MC 版本匹配的 jar：
   - 1.21.x 服务器首选 **2.10.1**（最新稳定，支持 1.8.9–26.2）；
   - 若需要更保守，可选 **2.9.10**（稳定线，支持 1.17–26.1）。
   - 标有 `[EXPERIMENTAL]`（如 2.10.0 / 2.9.0）为实验版，生产环境不推荐。
3. 将 jar 放入 `plugins/` 并**完整重启**服务端（不要用 `/reload`，会破坏全息，详见文末排错）。
4. 插件首次启动会生成 `plugins/DecentHolograms/` 目录与默认 `config.yml`。

> 可选增强：**PlaceholderAPI**（用于 `%server_online%`、`%player_name%` 等动态变量）与 **LuckPerms**（用于按权限显示全息）。两者均非强制。

## 目录结构

```
plugins/DecentHolograms/
├── config.yml            # 全局默认设置（可视距离、刷新间隔、伤害飘字等）
├── holograms/            # 每个全息一个 .yml 文件（核心数据存储）
│   ├── welcome.yml
│   └── shop.yml
├── animations/           # 自定义动画（彩虹/滚动/波浪等，文件名即动画名）
│   └── wave.yml
└── lang/
    └── en.yml            # 插件内部提示文本（一般无需改）
```

## 核心配置 (`plugins/DecentHolograms/config.yml`)

以下为默认配置（已加中文注释），一般只需关注 `defaults` 与 `damage-display`：

```yaml
defaults:
  # 全息默认可视距离（方块），超出此距离的玩家看不见也不收包
  display-range: 48
  # 全息默认“内容更新”距离（仅此范围内的玩家才会刷新占位符）
  update-range: 48
  # 占位符刷新间隔（游戏刻 ticks，20 = 1 秒）；PAPI 越便宜可设越大省性能
  update-interval: 20
  # 模式匹配结果缓存上限；不懂不要改，过大会吃内存（5–10000，默认 500）
  lru-cache-size: 500
  # 各类行类型的默认高度（格）
  height:
    text: 0.3
    icon: 0.6
    head: 0.75
    smallhead: 0.6
  # 全息是否以“底部”为原点（true 时 y 坐标代表底部而非中心）
  down-origin: false
  # 启动是否检查更新
  update-checker: true
  # 点击冷却（游戏刻），防连点
  click-cooldown: 1
  # 是否允许在动画帧里解析占位符；会显著增加 CPU，不需要就保持 false
  allow-placeholders-inside-animations: false
  # 玩家被传送/重生后是否强制刷新全息可见性；默认关（开会有闪烁），遇到不显示再开
  update-visibility-on-teleport: false
  # 全息是否生成在玩家视线高度（true=眼高，false=脚底高度，默认）
  holograms-eye-level-positioning: false
  # 拉取玩家皮肤数据的超时（秒，1–60）；网络差且频繁出现 "Failed to fetch UUID" 再调大
  player-skin-connection-timeout: 5

# 伤害飘字（每次成功命中出现的临时全息）
damage-display:
  enabled: false          # 是否开启
  players: true           # 是否对玩家显示
  mobs: true              # 是否对怪物显示
  zero-damage: false      # 是否显示 0 或更低伤害
  duration: 40            # 停留时长（刻）
  appearance: '&c{damage}'                 # 普通伤害外观，{damage} 为占位符
  critical-appearance: '&4&lCrit!&4 {damage}'  # 暴击外观
  height: 0

# 治疗飘字（每次血量上升出现的临时全息）
healing-display:
  enabled: false
  players: true
  mobs: true
  duration: 40
  appearance: '&a+ {heal}'
  height: 0

# 自定义字符替换（类似 Holographic Displays 的方块字）
custom-replacements:
  '[x]': '█'
  '[X]': '█'
  '[/]': '▌'
  '[,]': '░'
  '[,,]': '▒'
  '[,,,]': '▓'
  '[p]': '•'
  '[P]': '•'
  '[|]': '⎹'
```

> `config.yml` 由插件自动生成，键名以实际生成文件为准；如不确定某字段，保留默认值即可，不要照抄过时教程里的键名。

> ✅ **运行服校准（2026-09-16，`plugins\DecentHolograms\config.yml` 实测）**：上表数值与运行服逐项一致——`display-range:48`（第26行）、`update-range:48`（第28行）、`update-interval:20`（第30行）、`lru-cache-size:500`（第36行）、`height.*`（第39-42行）、`damage-display.enabled:false`（第103行）、`healing-display.enabled:false`（第132行）、`player-skin-connection-timeout:5`（第89行）均为默认值。**结构微调**：实际文件中 `update-checker`（第47行）与 `click-cooldown`（第50行）为**顶层键**，不在 `defaults:` 下；另实际多出 `defaults.text: Blank Line`（第24行）与 `displays-eye-level-positioning: false`（第76行）。运行服现有全息文件：`holograms/` 下 `welcome.yml / menu.yml / 1.yml / nanguascunf.yml`。

## 全息文件结构 (`holograms/<name>.yml`)

每个全息是一个独立 YAML。手写或 `/dh hologram create` 生成后可直接编辑。下面是**官方 2.x 真实文件格式**（注意：行用 `content:` 直接写，动作挂在**整页**下并以点击类型为键）：

```yaml
location:
  world: world
  x: 100.5
  y: 65.0
  z: 200.5
enabled: true
display-range: 48          # 本全息可视距离（覆盖全局默认）
update-range: 48
update-interval: 20
facing: 0.0                # 头颅/实体朝向（0=南,90=西,180=北,270=东）
down-origin: false
pages:
- lines:                   # 第 1 页的行
  - content: '&6&l南瓜生存服'        # 文字行，颜色码务必用单引号包裹
    height: 0.3
  - content: '&a当前在线: %server_online%'   # PlaceholderAPI 变量
    height: 0.3
  - content: '&e点击下方传送到主城'
    height: 0.3
  - icon: DIAMOND          # 物品图标行（icon 值填材质名）
    height: 0.6
  - head: Notch            # 玩家头颅行（玩家名 / UUID / base64 皮肤）
    height: 0.75
  actions:                 # 整页点击交互：以点击类型为键，值为动作列表
    RIGHT:
    - TELEPORT:world:0:64:0
    LEFT:
    - MESSAGE:&a欢迎来到南瓜生存服！
- lines:                   # 第 2 页
  - content: '&7—— 第二页 ——'
    height: 0.3
  actions:
    LEFT:
    - PAGE:2
```

> 行类型在文件里由**唯一键**决定：`content`（文字）、`icon`（物品）、`head` / `smallhead`（头颅）、`entity`（实体）、`block`（方块）。颜色码含 `&`，必须加单引号，否则 YAML 会把 `&` 当成锚点而报错。

## 行类型

在命令里直接写行内容时，用**前缀语法**区分类型（`<页>` 为页码，通常 `1`）：

| 类型 | 命令写法 | 说明 |
| --- | --- | --- |
| 文字 | `/dh lines add <名> <页> &a你好` | 支持颜色代码、PAPI 占位符、动画标记 |
| 物品 | `/dh lines add <名> <页> ICON:DIAMOND` | 显示该物品图标 |
| 头颅 | `/dh lines add <名> <页> HEAD:Notch` | 显示玩家头颅（玩家名 / UUID / base64 皮肤串） |
| 实体 | `/dh lines add <名> <页> entity:COW` | 悬浮显示实体模型 |
| 方块 | `/dh lines add <名> <页> block:STONE` | 悬浮显示方块 |
| 空白 | `/dh lines add <名> <页>`（留空） | 占位空行，用于间距 |

## 颜色、渐变与彩虹

- **十六进制颜色**：`&#RRGGBB`（如 `&#ff8800` 橙色）；旧式 `&x&r&r&g&g&b&b` 也支持。
- **渐变文字**：`&u起始色,结束色 文字`，例如 `&u#ff0000,#0000ff 南瓜生存服` 红→蓝渐变。
- **彩虹文字**：`&r 文字` 自动彩虹流动效果。

## 动画 (`animations/`)

在 `plugins/DecentHolograms/animations/` 新建 `<动画名>.yml`，文件名即动画 ID。示例 `wave.yml`：

```yaml
wave:                       # 动画 ID，引用时写 #ANIMATION: wave
  frames:                   # 每一帧的文字
  - '&eW&6a&av&be&6!'
  - '&6W&av&be&6!'
  - '&aW&6v&be&6a!'
  - '&av&6e&6b&aa!'
  speed: 10                # 每帧间隔（刻）
```

在全息行里引用：把该行 `content` 设为 `#ANIMATION: wave`（或 `&e#ANIMATION: wave`）。插件内置示例动画常含 `rainbow`（彩虹）、`scroll`（滚动）、`wave`（波浪）。修改动画文件后执行 `/dh reload` 生效。

## 点击交互（actions，挂在整页上）

DecentHolograms 的点击交互是**按页（page）绑定**的：整页的任意位置被点击都会触发该页配置的动作。通过 `/dh p addaction` 添加，需指定**点击类型**与**动作**。

**四种点击类型**：`LEFT`（左键）、`RIGHT`（右键）、`SHIFT_LEFT`（潜行+左键）、`SHIFT_RIGHT`（潜行+右键）。

**动作类型（`data` 一律冒号分隔）**：

| 动作类型 | 命令中写法 | 效果 |
| --- | --- | --- |
| `MESSAGE:<消息>` | `MESSAGE:欢迎来到南瓜服！` | 向点击者发送一条消息（支持颜色码 / PAPI） |
| `COMMAND:<命令>` | `COMMAND:/spawn` | 以**玩家身份**执行命令（必须以 `/` 开头，否则当成聊天消息发出） |
| `CONSOLE:<命令>` | `CONSOLE:say 有人点了全息` | 以**控制台身份**执行（**不能**执行 Bungee/Velocity 代理命令） |
| `CONNECT:<服务器>` | `CONNECT:lobby` | 把玩家转到代理下的子服务器（仅群服/Bungee/Velocity 有效） |
| `TELEPORT:[世界:]x:y:z[:yaw:pitch]` | `TELEPORT:world:0:64:0` | 传送到坐标；省略世界则用玩家当前世界；可加 yaw:pitch |
| `SOUND:<音效>[:音量:音高]` | `SOUND:ENTITY_CREEPER_PRIMED` | 为点击者播放音效（音量/音高默认 1.0） |
| `PERMISSION:<权限>` | `PERMISSION:vip.use` | 校验权限；无权限则**后续动作全部不执行**（可做“权限门槛”） |
| `NEXT_PAGE[:全息]` | `NEXT_PAGE` | 翻到下一页（仅对点击者本人生效） |
| `PREV_PAGE[:全息]` | `PREV_PAGE` | 翻到上一页 |
| `PAGE:[:全息:]页` | `PAGE:2` | 翻到指定页 |

添加动作的命令格式：

```
/dh p addaction <全息名> <页码> <点击类型> <动作>
```

示例：

```
# 右键欢迎全息 → 传送到主城；左键 → 发送欢迎消息
/dh p addaction welcome 1 RIGHT TELEPORT:world:0:64:0
/dh p addaction welcome 1 LEFT MESSAGE:&a欢迎来到南瓜生存服！
# 潜行右键 → 翻到下一页
/dh p addaction menu 1 SHIFT_RIGHT NEXT_PAGE
```

> 一个点击类型下可添加多个动作，点击时**按顺序依次执行**。想删动作用 `/dh p removeaction <全息> <页> <序号>`（`/dh list` / 文件里可看到序号，从 1 开始）。

## 分页（多页全息）

一个全息可有多个 `pages`，默认显示第 1 页。在某一页配置 `PAGE:` / `NEXT_PAGE` / `PREV_PAGE` 动作即可翻页：

```
/dh hologram create menu
/dh lines add menu 1 &a首页内容
/dh lines add menu 1 &e点击看下一页
/dh p addaction menu 1 LEFT PAGE:2      # 第 1 页左键 → 翻到第 2 页
/dh p add menu                          # 新增第 2 页
/dh lines add menu 2 &b这是第二页
/dh p addaction menu 2 LEFT PAGE:1      # 第 2 页左键 → 翻回第 1 页
```

## 常用命令

主命令 `/dh`（别名 `/decentholograms`）。三大分支及别名：
- `/dh hologram ...` — 别名 `holo` / `h`
- `/dh lines ...` — 别名 `line` / `l`
- `/dh pages ...` — 别名 `p`

| 命令 | 作用 |
| --- | --- |
| `/dh hologram create <名> [-l:世界:x:y:z] [内容]` | 在当前位置（或 `-l` 指定坐标）新建全息（别名 `c`） |
| `/dh hologram delete <名>` | 永久删除全息（别名 `del` / `remove`） |
| `/dh hologram enable <名>` / `disable <名>` | 启用 / 停用（停用后玩家看不见） |
| `/dh hologram rename <旧> <新>` | 重命名 |
| `/dh hologram movehere <名>` | 把全息移到自己脚下 |
| `/dh hologram move <名> -l:世界:x:y:z` | 把全息移到指定坐标 |
| `/dh hologram setdisplayrange <名> <距离>` | 设置可视距离 |
| `/dh hologram setupdaterange <名> <距离>` | 设置刷新距离 |
| `/dh hologram setupdateinterval <名> <刻>` | 设置占位符刷新间隔 |
| `/dh hologram setpermission <名> [权限]` | 设置“仅该权限可见”（留空撤销），写入全息文件 |
| `/dh hologram info <名>` | 查看全息信息 |
| `/dh lines add <名> <页> [内容]` | 在指定页追加一行（别名 `append`） |
| `/dh lines set <名> <页> <行> [内容]` | 设置某行内容 |
| `/dh lines remove <名> <页> <行>` | 删除某行 |
| `/dh lines height <名> <页> <高度>` | 设置行高 |
| `/dh lines insert <名> <页> <行> [内容]` | 在某行前插入 |
| `/dh lines swap <名> <页> <行1> <行2>` | 交换两行 |
| `/dh p add <名>` | 新增一页 |
| `/dh p addaction <名> <页> <点击类型> <动作>` | 给某页添加点击动作 |
| `/dh p removeaction <名> <页> <序号>` | 删除某页的某个动作 |
| `/dh list` | 列出所有全息 |
| `/dh reload` | 重新加载配置与全部全息（**安全**，替代 `/reload`） |
| `/dh version` | 查看版本 |
| `/dh convert <插件> <输入> [输出]` | 从 Holographic Displays 迁移（读取其 database.yml） |
| `/dh displays ...` | 1.19.4+ 的“显示实体（Display Entity）”独立管理（高级） |

> 基岩版玩家（通过 Geyser 接入）也能正常看到并点击全息；点击 `COMMAND` 行为会以该玩家身份执行，权限由 LuckPerms 正常判定。

## 权限

DecentHolograms 的权限前缀是 **`dh.`**（不是 `decentholograms.`）：

1. **命令权限**（谁能使用 `/dh`）：

   | 权限节点 | 说明 |
   | --- | --- |
   | `dh.default` | 允许使用基础非管理员命令（如 `/dh version`） |
   | `dh.command` | 允许使用全部 `/dh` 命令与子命令 |
   | `dh.admin` | 管理员，等同全部命令 |
   | `dh.command.<命令>` | 仅允许某个主命令（如 `dh.command.reload` 可用 `/dh reload`） |
   | `dh.command.<命令>.<子命令>` | 仅允许某个子命令（如 `dh.command.hologram.create` 可用 `/dh hologram create`） |

2. **查看权限**（谁能看见某个全息）：由 `/dh hologram setpermission <名> <节点>` 设置，写入该全息文件。例如设为 `vip.board`，则只有被 LuckPerms 授予 `vip.board` 的玩家才看得见该全息；留空则所有人可见。

> 实战建议：服主 / 管理组给 `dh.admin`（或 `dh.command`）；普通玩家无需任何 DH 权限即可看到无限制全息。VIP 专属公告用 `setpermission` + LuckPerms 节点即可。

## PlaceholderAPI 与开发接口

- **PlaceholderAPI**：安装 PAPI 后，全息文字里可直接写 `%server_online%`、`%player_name%`、`%cmi_playtimetop_name_1%` 等任意占位符，配合对应扩展（如 ServerExpansion、CMI）实现实时数据。改完执行 `/dh reload` 与 `/papi reload`。
- **DHAPI（开发者）**：插件提供 Java API 与事件 `HologramClickEvent`、`HologramRegisterEvent`，可让其他插件动态创建 / 响应全息。第三方插件（ajLeaderboards、CMI 等）即借此驱动排行榜全息。

## 实战示例：出生点欢迎 + 在线人数 + 主城传送

1. 站到出生点坐标，执行（带 `-l` 可一次性指定位置，控制台必须用 `-l`）：

   ```
   /dh hologram create welcome -l:world:0:64:0
   /dh lines add welcome 1 &6&l南瓜生存服
   /dh lines add welcome 1 &a当前在线: %server_online% 人
   /dh lines add welcome 1 &e点击下方传送到主城
   /dh lines add welcome 1 ICON:DIAMOND
   /dh lines add welcome 1 HEAD:Notch
   /dh p addaction welcome 1 RIGHT TELEPORT:world:0:64:0
   /dh p addaction welcome 1 LEFT MESSAGE:&a欢迎来到南瓜生存服！
   ```

2. 若在线人数不刷新，调大刷新间隔或确认 PAPI 已装：`/papi ecloud download Server` 后 `/papi reload`。

## 性能与排错

- **务必用 `/dh reload` 而非 Bukkit `/reload`**：`/reload` 会破坏已加载的全息，导致全息消失或报错，必须重启服务端才能恢复。
- **可视距离省资源**：静态标牌（如规则牌）把 `display-range` 设 32 甚至更小；大型竞技场排行榜设 64。超出距离的玩家完全不收包。
- **刷新间隔**：含 PAPI 的全息按 `update-interval`（刻）刷新，20=1 秒。变化慢的（如 `top_money`）可设 100–200（5–10 秒）以少调 PAPI，省 CPU。
- **ProtocolLib 对比**：DH 不依赖 ProtocolLib，直接发包；在 50 人在线、出生点 30 个全息的 Paper 1.21.4 测试中，DH 平均 TPS 19.95，而 HD + ProtocolLib 约 19.4（ProtocolLib 自身在中大型服会吃 5–10% TPS）。
- **头颅 / 皮肤拉取超时**：日志出现 `Failed to fetch UUID for player` 时，调大 `player-skin-connection-timeout`（默认 5 秒，最高 60）。
- **全息不显示**：按序排查——`/dh list` 是否在列；`display-range` 是否过小；是否被 `disable`（`/dh hologram enable <名>`）；是否设了 `setpermission` 而当前账号无该节点。
- **动作不触发**：确认是用 `/dh p addaction`（不是旧版 `/dh line action add`）；点击类型是否对应（左键 / 右键 / 潜行）；`TELEPORT` 等数据是否用冒号分隔；`PERMISSION:` 门槛是否挡住了后续动作。
- **动画不生效**：确认动画文件在 `animations/` 目录（不是插件根目录）；文件名与 `#ANIMATION:` 引用一致；`speed > 0`；改完执行 `/dh reload`。

## 常见问题（FAQ）

**Q：能替代 Holographic Displays 吗？**
A：可以。DH 提供 `/dh convert <插件> <输入> [输出]` 直接读取 Holographic Displays 的 `database.yml` 自动生成等价全息文件，名称保留；静态文字、物品行、CommandHolograms 的占位符都能迁移，旧 HolographicExtension 动画需手动改写为 DH 动画语法。迁移后删掉 HD 与 ProtocolLib 的 jar 并重启即可。

**Q：基岩版（手机 / Win10）玩家能看到 / 点击吗？**
A：能。Geyser 会把全息作为显示实体同步给基岩客户端，点击 `COMMAND` / `TELEPORT` 等行为照常触发，权限由 LuckPerms 判定。

**Q：全息数据存在哪？会丢吗？**
A：每个全息是 `holograms/<name>.yml` 文件，备份该目录即备份全部全息；不依赖数据库，不会因 MySQL 故障丢失。

**Q：Folia 能用吗？**
A：能。DH 支持 Folia（区域化多线程服务端），但注意命令需在正确区域执行；如遇异常优先用最新 2.10.x。

**Q：想做“仅 VIP 可见”的隐藏公告？**
A：`/dh hologram setpermission <名> vip.board`，然后在 LuckPerms 给 VIP 组 `vip.board` 节点即可；普通玩家看不到该全息。

**Q：为什么我的 `/dh gui` 打不开？**
A：DecentHolograms 2.x **没有** `/dh gui` 命令。所有编辑都通过 `/dh hologram ...`、`/dh lines ...`、`/dh p addaction ...` 完成，或直接编辑 `holograms/<name>.yml` 文件后 `/dh reload`。
