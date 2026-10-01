# 文档修复 B 清单 — 已取消/已禁用功能残留清理报告

> 执行日期：2026-09-30
> 范围：仅本指令列出的 6 个文件；已排除 `.history` / `.workbuddy` / `.git`。
> 编码：全部目标文件原均为 UTF-8 无 BOM、LF 行尾；修改后保持完全一致（用 `[System.IO.File]::ReadAllText/WriteAllText` + `UTF8Encoding($false)` 处理）。
> 标注基线：`❌ 已禁用（2026-09-30）` / `❌ 已关闭` / `❌ 已取消` / `❌ 已停用`。

---

## 一、修改明细（共 16 处）

### 1. README.md

| # | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|
| 1 | 161 | `| 死亡信息 \| CustomDeathMessages \| 整活死亡播报（音效 / 粒子 / 标题 / 收费） \|` | `| 死亡信息 \| CustomDeathMessages \| 整活死亡播报（音效 / 粒子 / 标题，扣费已关闭） \|` |
| 2 | 341 | `` | `/paygui`  \| 转账表单               \|`` | `` | `/paygui`  \| 转账表单 ❌ 已关闭（form.money.enable=false）\|`` |
| 3 | 355 | `| 可设置的家数量 \|  3   \|` | `| 领地数量（玩家）\|  3   \|` |

- 条目1（/paygui）：按指令标 ❌ 已关闭（参照 L343 BPS 死亡回传口径），未删行；已扫 README 基岩命令表，同表无 `/homegui` `/phomegui` `/pointsgui` 同类未标条目（grep 零命中）。
- 条目2（家数量）：改为"领地数量（玩家）\| 3"，与 L401"领地数量按身份不同（玩家 3 个）"数值一致；玩家实际家数=0，不再以"家 3 个"可用表述出现。
- 条目3（收费）：删"收费"二字并补"扣费已关闭"。

### 2. 5-服务器管理\01-权限管理系统-LuckPerms\权限节点速查.md

| # | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|
| 4 | 417 | `` | `bedrockplayersupport.paygui` \| `/paygui` \| 转账表单 \|`` | `` | `bedrockplayersupport.paygui` \| `/paygui` \| 转账表单 ❌ 已禁用（2026-09-30，form.money.enable=false）\|`` |
| 5 | 766 | `| `easybot.command.bind` \| `/bind <验证码>` \| 绑定 QQ ✅ \|` | `| `easybot.command.bind` \| `/bind <验证码>` \| 绑定 QQ ❌ 已禁用（2026-09-30，allow_bind=false） \|` |

- 条目5顺带扫描：全文件 `/bind` / `bind` 命中仅 L131（`essentials.hat.ignore-binding`，头饰/骷髅，与 QQ 绑定无关）、L761（节点清单说明）、L766（已改）、L772（实测命令示例）；无其它"✅"绑定条目。

### 3. 2-运维监控与面板\01-QQ机器人联动-EasyBot\EasyBot.md

| # | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|
| 6 | 252 | `| `/bind <验证码>` \| 绑定 QQ 账号      \|` | `| `/bind <验证码>` \| 绑定 QQ 账号 ❌ 已关闭（allow_bind=false）\|` |

- 与 L23/L26 配置段 `allow_bind:false`、`enable_success_event:false` 口径一致。

### 4. 5-服务器管理\06-BedrockPlayerSupport基岩版GUI表单界面\BedrockPlayerSupport基岩版GUI表单界面.md

| # | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|
| 7 | 44 | `    enable: true         # /phomegui（仅 HuskHomes）` | `    enable: false        # /phomegui（家园功能已禁用，2026-09-30，运行服关闭）` |
| 8 | 85 | `lp group default permission set bedrockplayersupport.homegui true` | `# lp group default permission set bedrockplayersupport.homegui true   # ❌ 已禁用（/homegui 已关闭，2026-09-30；form.home.enable=false，勿授予）` |
| 9 | 89 | `lp group default permission set bedrockplayersupport.phomegui true` | `# lp group default permission set bedrockplayersupport.phomegui true  # ❌ 已禁用（/phomegui 已关闭，2026-09-30；form.phome.enable=false，勿授予）` |

- 条目7：`form.phome.enable` 由 true 改 false，与 L14"/phomegui ❌关闭"内部一致。
- 条目8：两条授权改为注释行（`#` 前缀）+ ❌ 注，防止复制粘贴误授已禁用表单。

### 5. 5-服务器管理\01-权限管理系统-LuckPerms\权限组设计方案.md

| # | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|
| 10 | 171 | `| DeluxeMenus `homes_menu` \| 动态 … \| `essentials.sethome.multiple.2/3/4` \| 玩家花南瓜币买家扩位，**EssentialsX 取所有节点最大值**，与组上限叠加不冲突 \|` | 同前，末尾追加 ` ❌ 已取消（家园已禁用，本机制未上线，2026-09-30）` |
| 11 | 173 | `> ⚠️ **家扩位与组家园上限叠加**：default 组本有 `sethome.multiple.3`，玩家再买"+2→3"无收益（取 max）；买"+3→4"则升到 4。设计组上限时已预留此叠加空间，不必改组节点。` | `> ❌ **已取消（家园已整体禁用，本方案不再适用，2026-09-30）**：原设计——家扩位与组家园上限叠加：default 组本有 …（原文保留为历史记录）` |
| 12 | 291 | `| 6 \| 家园数量 \| 用 default 玩家 `/sethome a` 后再 `/sethome b` `/sethome c` \| 第三个提示数量超限 \|` | `| 6 \| 家园数量 ❌ 已禁用（无需验收） \| 家园 /sethome 本服已禁用，default 无家园权限 \| 跳过（2026-09-30） \|` |
| 13 | 311 | `| 家园数量不对 \| EssentialsX 取最大值，某组给了更高的 `multiple.<n>` \| … 排查 \|` | `| 家园数量不对 ❌ 仅恢复家园后适用 \| … \| … 排查（家园已禁用，此行仅日后恢复时适用，2026-09-30）\|` |

- 与 L100/L104 基线"`sethome.multiple.*` 全部不授予"一致；原"default 组本有 sethome.multiple.3"矛盾表述已加注取消。

### 6. 5-服务器管理\反作弊（开了一会问题太多，已关闭）\反作弊部署与配置指南.md

| # | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|
| 14 | 16 | `| **NovaAC** \| 1.24.10 \| 副反作弊（载具/实体坐骑加速补位） \| ✅ 已安装 \|` | `| **NovaAC** \| 1.24.10 \| 副反作弊（载具/实体坐骑加速补位） \| ❌ 已停用（开了一会问题太多，已关闭；NovaAC.jar 已改 .disabled）\|` |
| 15 | 18 | `> **NovaAC 说明**：其发布平台…需要管理员手动下载放入 `plugins` 目录。详见第五节。` | `> **NovaAC 已停用（2026-09-30）**：因跨世界 `Location.distance()` 代码 bug 导致控制台刷屏，`plugins/NovaAC.jar` 已改名为 `NovaAC.jar.disabled`。…原手动下载说明见第五节（**此部分已不适用，仅留档**）。` |
| 16 | 119 | `## 五、NovaAC 手动安装指引（需管理员操作）` | `## 五、NovaAC 手动安装指引（❌ 此部分已不适用，仅留档 —— NovaAC 已停用，2026-09-30）` |

- 经核对同目录 `NovaAC.md`：GrimAC（Java 主反作弊）、Themis（基岩真检测）、PacketEvents（数据包协议库）仍在运行使用，**未**标停用；仅 NovaAC 因跨世界 distance bug 停用。故 L13/L14/L15 三行保留 ✅ 已安装（其中 L15 PacketEvents 行曾被正则误中已即时回退为 ✅）。

---

## 二、grep 验证结果

| 验证项 | 结果 |
|---|---|
| README `可设置的家` / `家数量` | 0 命中（已清除） |
| README `收费` | 0 命中（已清除） |
| README `/paygui` | 仅 L341，已带 ❌ 已关闭 |
| 速查 `paygui` | 仅 L417，已带 ❌ 已禁用 |
| 速查 `绑定 QQ` | 仅 L766，已带 ❌ 已禁用；无其它 ✅ 绑定条目 |
| EasyBot `/bind` | 仅 L252，已带 ❌ 已关闭 |
| BPS `phome.enable` | L44 = `enable: false`；L89 已注释 |
| BPS `homegui` 权限 | L85 已注释；L55 config = false |
| 组方案 `homes_menu` / `sethome.multiple` | L100/L104 基线已禁用；L171/L173/L291/L311 均带 ❌ |
| 反作弊 `已安装` | 仅剩 GrimAC/Themis/PacketEvents/Floodgate（在用） |
| 反作弊 `已停用` | NovaAC L16/L18/L119 均带 ❌ |

结论：所有指定残留均不再以"可用/启用/未标注禁用"状态出现。

---

## 三、待确认回报清单（未擅自修改）

1. **权限组设计方案.md L170**：`| CustomDeathMessages | exempt-groups-from-cost | ["admin"] | 死亡播报扣费 5 南瓜币，admin 组免单 |` —— 与"cost-per-death-message=0.0（死亡消息扣费已禁用）"矛盾。条目3仅指定 README:161，本行不在授权范围，未改；请确认是否同步把"扣费 5 南瓜币"改为"扣费已关闭（0.0）"。
2. **反作弊指南 L139 验证清单**："确认四个插件均正常加载" —— NovaAC 停用后实际为三个插件；L149 注意事项仍把 NovaAC 与 Themis 并列为"新项目需先调参"。超出条目11明示范围（L13-16 + 安装步骤段），未改；请确认是否把"四个"改"三个"并删除 NovaAC 相关注意事项。
3. **反作弊指南 L15 PacketEvents 定位**：仍写"NovaAC 的前置依赖"。PacketEvents 本体仍在用（GrimAC/Themis 依赖其数据包拦截），仅描述滞后；未改。
4. **EasyBot.md L3 / L11 功能概述**：仍列"玩家账号与 QQ 号绑定（白名单/验证）"为插件通用能力。条目6仅指定 L252 指令表，未改；请确认是否在概述处补"（本服已关闭绑定）"。
5. **反作弊文件夹整体口径**：文件夹名"反作弊（开了一会问题太多，已关闭）"但指南内 GrimAC/Themis/PacketEvents 仍标 ✅ 在用。经核对 NovaAC.md，实际仅 NovaAC 停用、GrimAC/Themis 在用；文件夹名是否需要改为更精确的"NovaAC已停用"或在指南顶部加整体水印，请产品确认。

---

> 本报告由文档修复 B 代理生成；所有修改仅限上述 16 处，未触碰其它文件。

---

## 四、补轮清理（2026-09-30 追加）— EasyBot.md 功能概述绑定残留

> 用户补单：QQ 群绑定已关闭（allow_bind:false、enable_success_event:false，运行服已同步 false，权限速查 L766 已标 ❌）。本轮处理 EasyBot.md 概述与功能列表中的"绑定 QQ"可用表述。

| # | 文件 | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|---|
| 17 | EasyBot.md | 3 | `…实现群聊/游戏内聊天双向同步、群内执行命令、玩家绑定 QQ 号。本文给出…` | `…实现群聊/游戏内聊天双向同步、群内执行命令（玩家绑定 QQ 号 ❌ 已关闭 2026-09-30）。本文给出…` |
| 18 | EasyBot.md | 11 | `- 玩家账号与 QQ 号**绑定**（白名单/验证）` | `- ~~玩家账号与 QQ 号**绑定**（白名单/验证）~~（❌ 已关闭 2026-09-30）` |

**全文档绑定相关行复查**（grep `绑定|bind|验证码|白名单`）：
- L3 概述（本轮已标 ❌）
- L11 功能列表（本轮已标 ❌）
- L23 `allow_bind: false # 允许玩家使用绑定命令（本服已关闭）` — 原本一致，未动
- L26 `enable_success_event: false # 绑定成功事件已禁用` — 原本一致，未动
- L147 文件路径表 `bind_config.json | 玩家-QQ 绑定规则` — 属配置文件路径/用途说明（非"可用功能"宣称），且绑定已关闭该文件已清空为 `[]`，保持原样
- L252 `/bind <验证码>` 指令表（上一轮已标 ❌ 已关闭）
- L255/L257/L258 "关闭 EasyBot QQ 验证码绑定"章节 — 关闭操作指引，与现状一致

结论：EasyBot.md 内不再有把"绑定 QQ"描述为可用功能的表述（正文/功能列表/指令表均已标注关闭）。文件编码仍为 UTF-8 无 BOM、LF 行尾。
---

## 五、补轮清理（2026-09-30 再追加）— 权限组设计方案.md L170 CDM 扣费残留

> 用户补单：CDM 扣费已关闭（cost-per-death-message=0.0，2026-09-30）。原 L170"死亡播报扣费 5 南瓜币，admin 组免单"为现状表述，属残留。

| # | 文件 | 行号 | 改前原文 | 改后内容 |
|---|---|---|---|---|
| 19 | 权限组设计方案.md | 170 | `| CustomDeathMessages \| exempt-groups-from-cost \| ["admin"] \| 死亡播报扣费 5 南瓜币，**admin 组免单** \|` | `| CustomDeathMessages \| exempt-groups-from-cost \| ["admin"] \| 死亡播报扣费 ❌ 已关闭（2026-09-30，cost-per-death-message=0.0，exempt-groups 保留但无实际作用）\|` |

**顺带确认（上一轮已处理）**：
- L291 验收#6 家园数量：已为 `家园数量 ❌ 已禁用（无需验收）…跳过（2026-09-30）`，无需再改。
- L311 故障表 `multiple.<n>`：已为 `家园数量不对 ❌ 仅恢复家园后适用…（家园已禁用，此行仅日后恢复时适用，2026-09-30）`，无需再改。

> 本轮同时关闭了原"待确认回报"第 1 项（组方案 L170 CDM 扣费矛盾）。文件编码仍为 UTF-8 无 BOM、LF 行尾。