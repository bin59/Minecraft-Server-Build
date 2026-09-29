# 13. 自定义死亡信息 — CustomDeathMessages

本文介绍 CustomDeathMessages（简称 CDM），它把原版单调的死亡提示替换成可自定义、可随机抽取的彩色整活播报，并支持史诗死亡特效、分范围广播和 Vault 收费。文档按安装、config.yml 与 messages.yml、占位符、/cdm 命令、权限和实战示例展开，是 Paper 服调节聊天氛围的实用参考。

**MC 要求**: Java 1.18 – 26.1.x（含 1.21.11）

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
2. 从 Modrinth 下载与 MC 版本匹配的最新稳定版 jar。
3. 将 jar 放入 `plugins/` 并**完整重启**服务端（不要用 `/reload`，见文末排错）。
4. 首次启动生成 `plugins/CustomDeathMessages/` 下的 `config.yml` 与 `messages.yml`。
5. 可选：安装 **Vault** + 一个经济插件（如 EssentialsX 经济）启用收费；安装 **PlaceholderAPI** 启用外部占位符。

## 目录结构

```
plugins/CustomDeathMessages/
├── config.yml        # 全局开关与参数（史诗概率、收费、广播模式、更新检查等）
└── messages.yml      # 各死亡原因的消息模板列表（含 broadcast-system 段）
```

## 核心配置 (`config.yml`)

以下为常用键（已加中文注释），首次生成的默认值可直接用，按需微调：

```yaml
# 史诗死亡触发概率（0.0 ~ 1.0），如 0.05 = 5% 概率
epic-death-chance: 0.05
# 展示死亡消息向玩家收取的费用（需 Vault + 经济插件，0 为免费）
cost-per-death-message: 0
# 免收费用的权限组（LuckPerms 组名）
exempt-groups-from-cost:
  - "admin"
# 低版本颜色兼容：true 时自动剥离现代十六进制颜色码，避免低版本显示为纯文本
legacy-color-support: false
# 更新通知由 cdm.admin 权限控制，config.yml 中无 update-checker 键
# 广播相关：各效果（消息/音效/粒子/标题/暗屏）的可见范围
# 取值：GLOBAL / WORLD / RADIUS / VICTIM_ONLY
effects-broadcast:
  default-mode: "WORLD"
  default-radius: 50
  sound-mode: "WORLD"
  sound-radius: 50
  particle-mode: "WORLD"
  particle-radius: 50
  title-mode: "VICTIM_ONLY"
  title-radius: 0
  actionbar-mode: "VICTIM_ONLY"
  darken-effect-mode: "RADIUS"
  darken-effect-radius: 30
```

> 实际文件另有 `play-sound-on-death`（音效 `entity.player.death`）、`play-particles-on-death`（粒子 EXPLOSION）、`respawn-message-enabled:true`、`use-permission-based-messages:true`、`help-permissions` 等键，文档仅列常用项。

> `messages.yml` 按死亡原因分组存放消息列表（如 `global-pvp-death-messages`、`melee-death-messages`、`arrow-messages`、`fireball-messages`、`fall-damage-messages`、`creeper-messages`、`warden-sonic-boom-messages`、`unknown-messages` 等），并含 `broadcast-system` 段。该文件首次生成后可直接编辑，或用 `/cdm editor` 在游戏内改。

## 消息配置规范 (`messages.yml`)

正确编写消息模板，必须遵守以下三条硬性规范，否则会匹配失败并回退默认文案。

### 1. 死因键名必须用 Bukkit 枚举

`conditional-messages` 与 `groups.<组>.cause-messages` 下的**原因键**必须是 Bukkit 的 `EntityDamageEvent.DamageCause` 枚举，大小写严格一致：

| 正确键 | 对应死亡 | 易错写法（错误） |
| --- | --- | --- |
| `ENTITY_ATTACK` | 近战击杀（玩家 / 怪物 / 蜜蜂） | `pvp`、`monster`、`bee` |
| `FALL` | 摔落 | `fall` |
| `FIRE_TICK` | 持续燃烧烧死 | `fire`（错误，见下） |
| `FIRE` | 火焰方块 | `fire` |
| `LAVA` | 岩浆 | `lava` |
| `DROWNING` | 溺水 | `drown` |
| `SUFFOCATION` | 窒息（方块夹压） | `suffocation` |
| `VOID` | 虚空 | `void` |
| `LIGHTNING` | 闪电 | `lightning` |
| `ENTITY_EXPLOSION` | 爆炸（苦力怕 / TNT） | `explosion` |
| `STARVATION` | 饥饿 | `starvation` |
| `CACTUS` | 仙人掌 | `cactus` |

> **关键陷阱**：`FIRE_TICK`（持续燃烧）与 `FIRE`（火焰方块）是**两个不同的枚举**。玩家"被烧死"（`burned to death`）实际原因是 `FIRE_TICK`，若只配了 `FIRE` 就匹配不到，会回退默认值。同类陷阱还有 `DROWNING`（不是 `drown`）等。

### 2. 占位符统一用花括号 `{xx}`，禁用 `%xx%`

本插件 v1.3 的占位符是花括号格式：

```yaml
# ✅ 正确
- '&c{victim} &e被 &c{killer_name} &e用 &6{killer_source} &e送走了。'
# ❌ 错误（旧版格式，不会被替换，显示为字面量）
- '&c%victim% &e被 &c%killer% &e用 &6%kill-weapon% &e送走了。'
```

| `%xx%`（错误） | `{xx}`（正确） |
| --- | --- |
| `%victim%` | `{victim}` |
| `%killer%` | `{killer_name}` |
| `%kill-weapon%` | `{killer_source}` |
| `%player%` | `{player}` |

### 3. 结构完整：原因键下用列表，条目不写成裸字符串

`cause-messages` 下的每条文案都是列表元素；`conditional-messages` 的条目则是 `condition:` + `message:`（或 `default:`）结构，不要把二者混用。

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

## 常用命令（统一 `/cdm` 体系）

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

> **别名**仍可用：`/customdeathmessages`、`/customdeathmessage`、`/deathmessage`、`/deathmessages`。

## 权限

| 权限节点 | 默认 | 说明 |
| --- | --- | --- |
| `cdm.use` | 所有玩家 | 使用主 `/cdm` 命令及其子命令的基础权限 |
| `cdm.reload` | OP | 重载配置、校验配置 |
| `cdm.test` | OP | 测试消息 |
| `cdm.editor` | OP | 游戏内配置编辑器 |
| `cdm.broadcast` | OP | 广播测试套件 |
| `cdm.bypass` | false | 拥有者**不触发**死亡消息（适合管理/staff 隐身） |
| `cdm.message.admin` | false | 将玩家归入 "admin" 消息组 |
| `cdm.admin` | OP | 接收更新检查通知 |

> 实战：给服主/管理组 `cdm.admin` + `cdm.reload` + `cdm.editor`；普通玩家无需任何权限即可正常看到死亡播报（`cdm.use` 默认开启）。用 `cdm.bypass` 让管理人员测试时不刷屏。

### ⚠️ 关键权限陷阱：OP 玩家不显示自定义死亡消息

这是本服（南瓜生存服）实际踩过的坑，务必了解：

- **原因**：`onPlayerDeath` 逻辑第一行即判断 `hasPermission("cdm.bypass")`，有该权限则**直接 `return` 跳过**，保留原版死亡消息，且**不打印任何 `[CustomDeathMessages] Death Message:` 日志**。
- **OP 默认拥有通配权限 `*`**：Bukkit/Paper 中，`ops.json` 里登记的 OP（如 `PumpkinVegetable`，level 2）会自动获得所有权限，`cdm.bypass`（虽 default 为 false）对 OP 会解析为 `true` → **OP 死亡时永远走原版消息**。
- **表现**：日志里只有 `PumpkinVegetable was shot by Pillager` 这类原版消息，没有任何 CDM 输出；非 OP 普通玩家则正常显示自定义整活文案。
- **排查口诀**：`日志里只有原版死亡消息 + 无 CDM 输出` → 先查该玩家是否为 OP / 是否持有 `cdm.bypass`。

**如何区分 OP / 非 OP 生效情况：**

| 玩家身份 | `cdm.bypass` 实际值 | 死亡时表现 |
| --- | --- | --- |
| OP（ops.json 登记） | `true`（靠 `*` 通配吃到） | 走**原版**死亡消息，CDM 不生效 |
| 非 OP 普通玩家 | `false` | 走**自定义**死亡消息（中文整活文案） |
| 被显式授予 `cdm.message.admin/vip` | 组权限命中 | 走对应组文案 |
| 被显式 `unset cdm.bypass` 的 OP | 视通配而定（通常仍为 true） | 多数仍走原版 |

**解决方案（按推荐顺序）：**
1. **测试时用非 OP 账号**——最直接，死亡即见中文整活文案。
2. **`deop 玩家名`** 临时降权后再测；测试完再 `op` 恢复。
3. 若确需让某个 OP 也触发自定义消息：`/lp user <名> permission unset cdm.bypass`，但**注意通配权限通常无法被 unset 覆盖**，更可靠是方案 1/2，或等待作者在 v1.4 调整该机制（日志提示 1.3 → 1.4 有更新）。
4. `cdm.bypass` 是插件源码写死的（无配置项开关），无法通过 config.yml 关闭。

## 实战示例

**1. 整活广播**（待选清单推荐玩法）：

```
/cdm broadcast message all "🎃 南瓜被苦力怕炸飞了！"
```

**2. 配置一条 PvP 搞怪文案**（`messages.yml` 的 `ENTITY_ATTACK` 列表追加）：

```yaml
ENTITY_ATTACK:
  - '&6{victim} &r被 &c{killer_name} &r用 &e{killer_source} &r送走了，距离 {distance} 格'
  - '&c{victim} &e被 &c{killer_name} &e用 &6{killer_source} &e送回了重生点，走得很安详。'
```

**3. 史诗死亡演出**：把 `config.yml` 的 `epic-death-chance` 设为 `0.05`，触发时玩家会看到专属 Title + 闪电 + 粒子。

**4. 经济收费**：装好 Vault + 经济后，设 `cost-per-death-message: 5`，`exempt-groups-from-cost` 已含 `"admin"`，普通玩家每次死亡展示扣 5 金币，管理组免单。

## 性能与排错

- **务必完整重启而非 `/reload`**：`/reload` 会破坏插件状态，导致消息不刷新或报错；改完用 `/cdm reload`（原子重载，安全）。
- **群死不卡服**：广播消息的发送已在后台异步处理，大规模死亡事件不会阻塞主线程。
- **配置缓存**：消息与配置有缓存，频繁文件读取被大幅削减；改完执行 `/cdm reload` 生效。
- **低版本颜色**：若客户端/服务端低于 1.16 出现 `&#` 颜色显示为纯文本，开启 `legacy-color-support` 自动剥离。
- **1.21.11 兼容**：本服 1.21.11 可直接使用；若遇到边缘报错，先 `/cdm config validate` 排查配置。
- **音效名**：插件已支持新版小写音效名（如 `entity.player.death`），无需旧式大写。

## 常见问题（FAQ）

**Q：日志刷 `[CustomDeathMessages] 未找到该世界/组/原因对应的消息。已使用默认值。` 怎么办？**
A：这是 **WARN 不是报错** —— 三层键（世界 / 组 / 死因）都没命中，插件用了兜底文案 `<玩家> has died.`。按此顺序排查：
1. 确认死因：让死者复现一次，查日志里实际 cause（`FALL` / `ENTITY_ATTACK` / `FIRE_TICK` / `LAVA` / `UNKNOWN`…），再到 `messages.yml` 找同名分段。
2. **最可能死因键命名错误**：见上文「消息配置规范」——键名必须用 Bukkit 枚举（`FIRE_TICK` ≠ `FIRE`，`DROWNING` ≠ `drown`），且 `%xx%` 占位符不会被替换。用 `/cdm test <原因>` 复现验证。
3. 第 2 层「组」：`use-permission-based-messages: true` 时组名走**权限组**（admin / 默认组），玩家所在组没配就会漏；关闭该开关则走语义分组。
4. 第 1 层「世界」：世界名必须逐字符一致（`world` / `world_nether` / `world_the_end`），自定义世界要单独建段。
5. 改完 `/cdm reload`；仍不生效就 `/cdm config validate`，再考虑完整重启。

**Q：基岩版（手机/Win10）玩家能看到彩色死亡消息吗？**
A：能。Geyser 会把带颜色的聊天消息同步给基岩客户端；`&#RRGGBB` 等格式在基岩端按客户端支持渲染。

**Q：怎样让某条消息只对特定组显示？**
A：给目标玩家 `cdm.message.admin` 权限（LuckPerms 授予），并在 `messages.yml` 的对应分组里配置专属文案。

**Q：收费功能不扣钱？**
A：需同时安装 **Vault** 与一个经济插件（如 EssentialsX），且 `cost-per-death-message > 0`；被 `exempt-groups-from-cost` 列出的组不扣费。

**Q：我是 OP/管理员，怎么测试死亡都不显示自定义消息，普通玩家却正常？**
A：这是 `cdm.bypass` 权限陷阱，详见上文「权限」章节的专项说明。OP 自动拥有通配 `*`，`cdm.bypass` 对 OP 生效，死亡时插件直接跳过（保留原版消息、无 CDM 日志）。**测试时改用非 OP 账号，或 `deop 自己` 再测**；这不是配置或插件故障，无需改 messages.yml。

**Q：想接 Discord 转发死亡消息？**
A：本体不含 Discord 转发；可配合 DiscordSRV 的聊天转发，或选用带 EssentialsDiscord/DiscordSRV 转发的同名 fork（见上条 FAQ）。
