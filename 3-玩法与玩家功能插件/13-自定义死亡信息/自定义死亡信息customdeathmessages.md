# 13. 自定义死亡信息 — CustomDeathMessages

本文介绍 CustomDeathMessages（简称 CDM），它把原版单调的死亡提示替换成可自定义、可随机抽取的彩色整活播报，并支持史诗死亡特效、分范围广播和 Vault 收费。文档按安装、config.yml 与 messages.yml、占位符、/cdm 命令、权限和实战示例展开，是 Paper 服调节聊天氛围的实用参考。

**当前版本**: CustomDeathMessages 1.3（稳定版） | **MC 要求**: Java 1.18 – 26.1.x（含 1.21.11）

**作者/发布**: SicklySurgeon | **协议**: MIT（开源免费）

**Modrinth**: https://modrinth.com/plugin/customdeathmessages | **Discord**: https://discord.gg/K3gFT9Hnhg

> 适用平台：**Bukkit / Paper / Purpur / Spigot**（需 Paper API 特性，推荐 Paper）。无强制前置依赖；可选 **Vault**（经济收费）、**PlaceholderAPI**（外部占位符）。南瓜生存服为 Paper 1.21.11，直接放入 `plugins/` 即可。
>
> 本插件在「待选插件清单」中被列为**整活核心**：把默认死亡提示换成搞怪文案，支持随机多条、按死亡方式/实体条件匹配，是聊天区氛围拉满的必装件。

## 功能说明

CustomDeathMessages（简称 CDM）把原版干巴巴的 "x 死了" 替换成可定制、彩色、带音效/粒子/标题的整活播报。核心能力：

- **全场景自定义死亡文案**：PvP、近战、弓箭/火球等抛射物、环境（摔落/岩浆/窒息…）、怪物、未知兜底，各自一组消息列表，随机抽一条。
- **随机多条 + 去重**：同一玩家不会连续两次刷到同一条消息（内置冷却去重）。
- **史诗死亡（Epic Death）**：可配置概率触发特殊演出——专属文案 + 强力音效 + 炫酷粒子 + 全屏 Title + 在死亡点劈下一道闪电。
- **广播系统（Broadcast）**：消息、音效、粒子、Title、暗屏（Darken Screen）可分别设定广播范围（玩家自身 / 击杀者 / 世界 / 全局），精细控制谁看到什么。
- **点击交互**：可选悬停显示原版死亡原因、悬停显示凶器物品；被击杀时在死亡点劈闪电；按概率掉落自定义名称的玩家头颅。
- **经济收费（Vault）**：可向玩家收取「展示死亡消息」费用，指定权限组（如 admin）免单。
- **复活欢迎语**：玩家重生时发送自定义欢迎消息。
- **彩色与占位符**：支持 `&#RRGGBB` 十六进制颜色、渐变/彩虹，以及丰富的上下文占位符；可接 PlaceholderAPI 扩展。
- **条件消息**：仅当满足特定条件（如手持钻石武器、处于中毒状态）才显示专属文案。
- **实时重载 / 测试**：`/cdm reload` 原子重载（新配置出错自动回退旧配置）；`/cdm test` 预览；`/cdm editor` 游戏内可视化编辑。

## 安装与前置

1. 确认服务端为 **Paper / Purpur / Spigot**（运行 `/version` 查看）。
2. 从 Modrinth 下载与 MC 版本匹配的 jar：
   - 1.21.x 服务器首选 **1.3**（最新稳定，支持 1.18–26.1.x）；
   - 1.2 同样覆盖 1.21.x 并额外列出 26.1.x，若 1.3 有兼容问题可回退。
3. 将 jar 放入 `plugins/` 并**完整重启**服务端（不要用 `/reload`，见文末排错）。
4. 首次启动生成 `plugins/CustomDeathMessages/` 下的 `config.yml` 与 `messages.yml`。
5. 可选：安装 **Vault** + 一个经济插件（如 EssentialsX 经济）启用收费；安装 **PlaceholderAPI** 启用外部占位符。

## 目录结构

```
plugins/CustomDeathMessages/
├── config.yml        # 全局开关与参数（史诗概率、收费、广播模式、更新检查等）
└── messages.yml      # 各死亡原因的消息模板列表（1.1 版结构，含 broadcast-system 段）
```

> 早期 0.0.x 版本用 `/cdmreload`、`/cdmconfig` 等独立命令，且 messages.yml 为西语结构；**1.x 已统一为 `/cdm` 单命令体系**并英文化。老版本升级到 1.x 无需改配置即可兼容，但想用新消息结构可删掉 messages.yml 让其重新生成。

## 核心配置 (`config.yml`)

以下为常用键（已加中文注释），首次生成的默认值可直接用，按需微调：

```yaml
# 史诗死亡触发概率（0.0 ~ 1.0），如 0.05 = 5% 概率
# 实际值：epic-death-chance: 0.05（config.yml 第116行）
epic-death-chance: 0.05
# 展示死亡消息向玩家收取的费用（需 Vault + 经济插件，0 为免费）
# 实际值：cost-per-death-message: 0.0（第123行，未启用收费）
cost-per-death-message: 0
# 免收费用的权限组（LuckPerms 组名），如 admin / vip
# 实际值：exempt-groups-from-cost: ["admin"]（第125-126行）
exempt-groups-from-cost:
  - "admin"
# 旧版颜色兼容：true 时自动剥离现代十六进制颜色码，避免低版本显示为纯文本
# 实际值：legacy-color-support: false（第161行）
legacy-color-support: false
# 启动检查更新
# 注意：运行服 1.3 的 config.yml 中【没有 update-checker 键】（已核对全文），
#       更新通知由 cdm.admin 权限控制，无需在此配置。
# update-checker: true
# 广播相关：各效果（消息/音效/粒子/标题/暗屏）的可见范围
# 取值：GLOBAL / WORLD / RADIUS / VICTIM_ONLY
# ⚠️ 以下为运行服实际值（第79-98行），与旧文档示例不同：
effects-broadcast:
  default-mode: "WORLD"          # 第81行（旧文档示例误写 GLOBAL）
  default-radius: 50
  sound-mode: "WORLD"            # 第85行
  sound-radius: 50
  particle-mode: "WORLD"         # 第88行
  particle-radius: 50
  title-mode: "VICTIM_ONLY"      # 第91行（旧文档示例误写 WORLD）
  title-radius: 0
  actionbar-mode: "VICTIM_ONLY"  # 第94行
  darken-effect-mode: "RADIUS"   # 第97行
  darken-effect-radius: 30
```

> ✅ **运行服校准（2026-09-16，`plugins\CustomDeathMessages\config.yml` 实测）**：`epic-death-chance:0.05`、`cost-per-death-message:0.0`、`exempt-groups-from-cost:["admin"]`、`legacy-color-support:false` 均与文档一致；广播模式以上述实际值为准。实际文件另有 `play-sound-on-death`（音效 `entity.player.death`）、`play-particles-on-death`（粒子 EXPLOSION）、`respawn-message-enabled:true`、`use-permission-based-messages:true`、`help-permissions` 等键，文档仅列常用项。

> `messages.yml` 按死亡原因分组存放消息列表（如 `global-pvp-death-messages`、`melee-death-messages`、`arrow-messages`、`fireball-messages`、`fall-damage-messages`、`creeper-messages`、`warden-sonic-boom-messages`、`unknown-messages` 等），并含 `broadcast-system` 段。该文件首次生成后可直接编辑，或用 `/cdm editor` 在游戏内改。

## 占位符（Placeholders）

消息模板与复活语中可用的内置变量：

| 占位符 | 含义 |
| --- | --- |
| `{victim}` | 死亡玩家名 |
| `{killer_name}` | 致死责任实体（如射箭的骷髅） |
| `{killer_source}` | 直接伤害来源（如 "Arrow" / "Lava" / "Fireball"） |
| `{killer_displayname}` | 击杀者显示名（支持 EssentialsX / LuckPerms 的颜色与昵称） |
| `{cause}` | 技术死亡原因（如 FALL / ENTITY_ATTACK） |
| `{world}` | 死亡所在世界名 |
| `{biome}` | 生物群系（如 Plains / Nether Wastes / Deep Dark） |
| `{x}` / `{y}` / `{z}` | 死亡坐标 |
| `{time}` | 服务器时间（格式见 config.yml） |
| `{distance}` | 受害者与击杀者距离（1 位小数，如 5.2） |
| `{victim_weapon}` | 受害者主手武器名 |
| `{victim_effects}` | 受害者身上的药水效果列表 |

安装 **PlaceholderAPI** 后还可使用任意外部占位符（如 `%player_displayname%`）。

## 常用命令（统一 `/cdm` 体系，1.x）

| 命令 | 权限 | 作用 |
| --- | --- | --- |
| `/cdm` | `cdm.use` | 显示主菜单/可用命令概览 |
| `/cdm help [命令]` | `cdm.use` | 游戏内多页帮助（命令/权限/占位符/配置/支持） |
| `/cdm test [killer] [source]` | `cdm.test` | 预览死亡消息，可指定责任实体与伤害来源 |
| `/cdm editor` | `cdm.editor` | 打开配置编辑器（可视化改消息） |
| `/cdm reload` | `cdm.reload` | 原子重载配置（出错自动回退旧配置） |
| `/cdm config validate` | `cdm.reload` | 校验配置文件是否有错 |
| `/cdm broadcast test` | `cdm.broadcast` | 模拟所有广播模式，查看各模式会触达多少玩家 |
| `/cdm broadcast sound [模式] [音效]` | `cdm.broadcast` | 手动触发音效广播（如 `WORLD`） |
| `/cdm broadcast particle [模式] [粒子]` | `cdm.broadcast` | 手动触发粒子广播 |
| `/cdm broadcast message [模式] [文本]` | `cdm.broadcast` | 手动广播一条消息（如 `all "南瓜被苦力怕炸飞了！"`） |
| `/cdm broadcast global` | `cdm.broadcast` | 立即向所有玩家发送一次效果 |

> 旧版独立命令（`/cdmreload`、`/cdmconfig`、`/cdmhelp` 等）在 1.x 已废弃，统一走 `/cdm` 子命令；**别名**仍可用：`/customdeathmessages`、`/customdeathmessage`、`/deathmessage`、`/deathmessages`。

## 权限

| 权限节点 | 默认 | 说明 |
| --- | --- | --- |
| `cdm.use` | 所有玩家 | 使用主 `/cdm` 命令及其子命令的基础权限 |
| `cdm.reload` | OP | 重载配置、校验配置 |
| `cdm.test` | OP | 测试消息 |
| `cdm.editor` | OP | 游戏内配置编辑器 |
| `cdm.broadcast` | OP | 广播测试套件 |
| `cdm.bypass` | false | 拥有者**不触发**死亡消息（适合管理/staff 隐身） |
| `cdm.message.vip` | false | 将玩家归入 "vip" 消息组（可配置专属文案） |
| `cdm.message.admin` | false | 将玩家归入 "admin" 消息组 |
| `cdm.admin` | OP | 接收更新检查通知 |

> 实战：给服主/管理组 `cdm.admin` + `cdm.reload` + `cdm.editor`；普通玩家无需任何权限即可正常看到死亡播报（`cdm.use` 默认开启）。用 `cdm.bypass` 让管理人员测试时不刷屏。

## 实战示例

**1. 整活广播**（待选清单推荐玩法）：

```
/cdm broadcast message all "🎃 南瓜被苦力怕炸飞了！"
```

**2. 配置一条 PvP 搞怪文案**（`messages.yml` 的 PvP 列表追加）：

```yaml
- '&6{victim} &r被 &c{killer_name} &r用 &e{victim_weapon} &r送走了，距离 {distance} 格'
```

**3. 史诗死亡演出**：把 `config.yml` 的 `epic-death-chance` 设为 `0.05`，触发时玩家会看到专属 Title + 闪电 + 粒子。

**4. 经济收费**：装好 Vault + 经济后，设 `cost-per-death-message: 5`，并把 `exempt-groups-from-cost` 加 `"vip"`，普通玩家每次死亡展示扣 5 金币，VIP 免单。

## 性能与排错

- **务必完整重启而非 `/reload`**：`/reload` 会破坏插件状态，导致消息不刷新或报错；改完用 `/cdm reload`（原子重载，安全）。
- **群死不卡服**：广播消息的发送已在后台异步处理，大规模死亡事件不会阻塞主线程。
- **配置缓存**：消息与配置有缓存，频繁文件读取被大幅削减；改完执行 `/cdm reload` 生效。
- **低版本颜色**：若客户端/服务端低于 1.16 出现 `&#` 颜色显示为纯文本，开启 `legacy-color-support` 自动剥离。
- **1.21.11 兼容**：1.3 明确支持 1.21.x（及 26.1.x），本服 1.21.11 可直接使用；若遇到边缘报错，先 `/cdm config validate` 排查配置，再考虑回退 1.2。
- **音效名**：插件已支持新版小写音效名（如 `entity.player.death`），无需旧式大写。

## 常见问题（FAQ）

**Q：和另一款同名插件（GitHub sb2bg 版）有什么区别？**
A：本仓库文档对应的是 **Modrinth 上 SicklySurgeon 的 CustomDeathMessages**（1.x 统一 `/cdm` 命令 + 广播系统）。GitHub 上另有同名 fork，使用 `/cdm set flag/message/number`、`cdm.modify` 权限、`config.yml` 内 `*-messages` 段落与 Discord 转发，命令与配置结构不同。若你实际装的是后者，请告知我替换本文档。

**Q：基岩版（手机/Win10）玩家能看到彩色死亡消息吗？**
A：能。Geyser 会把带颜色的聊天消息同步给基岩客户端；`&#RRGGBB` 等格式在基岩端按客户端支持渲染。

**Q：怎样让某条消息只对 VIP 显示？**
A：给目标玩家 `cdm.message.vip` 权限（LuckPerms 授予），并在 `messages.yml` 的对应分组里配置 VIP 专属文案。

**Q：收费功能不扣钱？**
A：需同时安装 **Vault** 与一个经济插件（如 EssentialsX），且 `cost-per-death-message > 0`；被 `exempt-groups-from-cost` 列出的组不扣费。

**Q：想接 Discord 转发死亡消息？**
A：1.x 本体不含 Discord 转发；可配合 DiscordSRV 的聊天转发，或选用带 EssentialsDiscord/DiscordSRV 转发的同名 fork（见上条 FAQ）。
