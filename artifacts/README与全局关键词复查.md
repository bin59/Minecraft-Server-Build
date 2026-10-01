# README 与全局关键词复查报告

> 只读检查，未修改任何业务文件。范围：文档根 `F:\game\pc\MC\开服\Minecraft-Server-Build`，.md 文件，已排除 `.history` / `.workbuddy` / `.git`（共扫描 99 个 md）。
> 判定口径：**残留**=该功能以"可用/启用/未标注禁用"状态出现；**一致**=已带 ❌/已禁用/已取消/已关闭/未安装/未启用/已删除 标注；**待确认**=含糊或跨文档矛盾。
> 标注格式基准（权限速查.md）：`❌ 已禁用（日期）` / `本服已取消 ❌`。
> 说明：README 实际 561 行（预估 458 行，行号按实际内容定位）。

---

## A. README.md 全文逐项核查

| 清单项 | 行号 | 原文摘录 | 判定 | 建议动作 |
|---|---|---|---|---|
| 1 `/back` | （README 无 `/back` 条目；grep README 零命中） | — | 一致 | 无需动作。玩家手册/速查卡/菜单均未出现 `/back` |
| 2 家园 `/home /sethome /renamehome /delhome` | 355 | `| 可设置的家数量 | 3 |`（"不同身份能用什么"速览表，玩家组） | **残留** | 该行暗示玩家可设 3 个家，但家园整体已禁用（default/admin 不授 `essentials.sethome.multiple.*`）。删除该行，或改为与 401 行一致的"领地数量 3"；玩家组实际家数=0 |
| 3 `/sell /sellall /worth` | 275 | `> /sell、/sellall、/worth 已命令级禁用（Essentials disabled-commands，2026-09-30），量产物品走玩家间交易（/ah 拍卖行、/shop 玩家商店）。` | 一致 | 无需动作。已带日期与禁用说明 |
| 4 EasyBot QQ 群绑定（allow_bind:false） | 315 | `> 📌 QQ 群绑定：目前服务器未开启游戏内绑定（绑定功能已关闭），群服消息同步由管理统一配置，无需玩家操作。` | 一致 | 无需动作。54/63/119/170/416 行的 EasyBot QQ 联动描述的是机器人本体在用，与"玩家自助绑定关闭"不冲突 |
| 5 BPS 死亡回传表单（form.back.enable=false） | 343 | `> 💡 …死亡重生后弹出"是否返回死亡点"的表单已关闭（BedrockPlayerSupport form.back.enable=false，2026-09-27）。` | 一致 | 无需动作 |
| 6 BPS 其它表单（/paygui /pointsgui 自动注册登录 加入退出命令） | 341 | `| /paygui | 转账表单 |`（335–341 基岩版专属命令表，未标注任何禁用） | **残留** | 删除 `/paygui` 行。BPS 分册（BedrockPlayerSupport…md:17,52）明确 `form.money.enable=false`、`/paygui` ❌关闭。同表 `/pointsgui`/自动登录/加入退出命令 README 未列，无残留 |
| 7 NovaAC 反作弊（已停用） | 543 | `│   └── 反作弊（开了一会问题太多，已关闭）/  NovaAC / 反作弊部署与配置指南` | 一致 | 无需动作。目录树已带"已关闭"标注 |
| 8 CarryMe（已删除，换 RideOnHead） | 164 / 516 | `| 玩家骑乘 | RideOnHead | 空手右键骑头、潜行下车、叠罗汉 |`；目录树 `11-RideOnHead玩家骑乘/` | 一致 | 无需动作。RideOnHead 在用，全文无 CarryMe 字样 |
| 9 TrackPlayer（从未采用） | （README 无；grep 全仓零命中） | — | 一致 | 无需动作。注意 169/530 行的 PosTracker 是另一在用插件，勿混淆 |
| 10 CoreProtect 自动清理（已关闭） | 56/112/157/369/412/444/537 | 仅记录/查询/回滚用途，无任何"自动清理"表述 | 一致 | 无需动作 |
| 11 EconomyShop sell-to-shop/sell-commands（已关闭） | 155 / 275 / 505 | `| 经济 | Vault + EconomyShop/AuctionHouse | 统一经济 API、商店、拍卖行 |`；目录树列 `EconomyShop玩家商店` | 待确认 | 商店本体仍可买（buy），仅 sell-to-shop/sell-commands 关闭；275 行已交代 `/sell` 禁用。155 行"商店"未承诺卖店，建议保留，可在 EconomyShop 分册注明"仅买不卖" |
| 12 EzTax 死亡费（已关闭） | 505 | 目录树 `…经济插件/ AuctionHouse拍卖行 / EconomyShop玩家商店 / EzTax周持有税 / Quests每日任务` | 待确认 | README 玩家面无 EzTax 扣费表述；目录树仅列文档夹。注意清单项称"死亡费"而 README 目录树写"周持有税"，两者口径需核对；玩家面无残留 |
| 13 CustomDeathMessages 死亡消息扣费（已禁用） | 161 | `| 死亡信息 | CustomDeathMessages | 整活死亡播报（音效 / 粒子 / 标题 / 收费） |` | **残留** | "收费"仍作为插件在用功能列出。删除"收费"二字，或补注"扣费已禁用（cost-per-death-message=0.0，2026-09-30）"。扣费点清单已记为 0.0 |
| 14 拍卖行上架费百分比（未启用） | （README 无上架费表述） | — | 一致 | 无需动作。扣费点清单记 listing-fee-percent=0.0（未启用） |
| 15 家扩位入口（已取消） | （README 无；grep README 零命中） | — | 一致 | 无需动作 |
| 16 ExcellentEnchants 80 附魔（已移入 _disabled_） | 458 / 519 | 458 行代表分册列 `ExcellentEnchants`；519 行目录树 `14-自定义附魔-ExcellentEnchants/ ExcellentEnchants自定义附魔.md` | 待确认 | 插件本体在用（仅 80 个附魔移入 `enchants/_disabled_/`，见该分册:114）。README 目录树/分册列表未标注"80 附魔已禁用"，建议在 458 或 519 旁补一句括注 |
| 17 LootrInstancer（未采用，用 v1.2） | 518 | `│   ├── 13-玩家独立战利品/           LootrInstancer.md` | 待确认 | 运行服实际 jar 为 LootrPlugin-1.2.jar；目录树把 LootrInstancer.md 与在用玩法插件并列，未标注。需确认该文档是"在用插件安装指南"还是"弃用评估文档"，再决定改名/标注 |
| 18 ViaBackwards/ViaRewind（未安装） | （README 无；grep README 零命中） | — | 一致 | 无需动作。ViaVersion 分册已明确"未装 ViaBackwards 与 ViaRewind" |
| 19 GemsEconomy Addon（未装） | （README 无；grep README 零命中） | — | 一致 | 无需动作。各宠物分册均标注 ❌未装 |
| 20 Quests 示例分类 examples/permissionexample（已删除） | （README 无该路径表述；505 行仅列 Quests每日任务目录） | — | 一致 | 无需动作 |
| 21 身份/权限组只留 default/admin/owner | 347–351 | `default [新手] / admin [管理] / owner [服主]` | 一致 | 无需动作 |
| 22 快捷菜单 home/back/家园/家扩位 入口（已隐藏/取消） | 195 | `菜单包含：传送 · 领地 · 空间传送阵 · 经济 · 工具包 · 皮肤 · 社交 · 服务器信息 · 语音` | 一致 | 无需动作。菜单枚举无 home/back/家园/家扩位 |

**A 小结**：明确残留 3 处——README:355（家数量=3）、README:341（/paygui）、README:161（死亡播报"收费"）；待确认 4 处（EconomyShop 卖店口径、EzTax 命名、ExcellentEnchants 标注、LootrInstancer 命名）。其余 15 项与现状一致。

---

## B. 仓库级窄关键词 grep 命中与判定

扫描 99 个 md（排除 .history/.workbuddy/.git）。

### B.1 CarryMe
- 命中：**0 条**。
- 判定：**一致**。全仓库无 CarryMe 痕迹，骑乘由 RideOnHead 承担（在用）。

### B.2 TrackPlayer
- 命中：**0 条**。
- 判定：**一致**。从未采用，无残留（勿与在用的 PosTracker 混淆）。

### B.3 NovaAC
| 路径:行号 | 原文摘录 | 判定 |
|---|---|---|
| README.md:543 | `反作弊（开了一会问题太多，已关闭）/  NovaAC / …` | 一致（目录树已标已关闭） |
| 5-服务器管理\反作弊（…已关闭）\NovaAC.md:2,5,9,11,15,17,23,29,33,35 | 历史排错记录；明确"暂时停用 NovaAC""先停用"，文件夹名即"已关闭" | 一致（历史记录，结论=停用） |
| 5-服务器管理\反作弊（…已关闭）\反作弊部署与配置指南.md:15,16,18,35,119,121,124,125,128,149 | `| NovaAC | 1.24.10 | 副反作弊… | ✅ 已安装 |`；手动安装指引"将 NovaAC.jar 放入 plugins/" | **待确认**：文档本身仍把 NovaAC 标为 ✅已安装并给出安装步骤，与同文件夹"已关闭/停用"结论矛盾。建议在该指南的 NovaAC 行补"❌ 已停用（2026-09，跨世界报错）"（该指南同时覆盖的 GrimAC/Themis 才是在用主反作弊） |
| TODO\各种插件（待选）.md:534 | `反作弊联动：NovaAC / Grim 等对 PVP 场景容易误判…活动前加白名单` | 一致（待选/活动规划语境，非声称在用） |

### B.4 反作弊
- 通用提及（Leaf"反作弊友好"、ProtocolLib 数据包校验、Plan 安全监控、AntiLitematica Grim 集成 enabled:false、TODO 待选）均为中性描述，不构成 NovaAC 在用声称。
- 关键命中：
  - 反作弊部署与配置指南.md:13 `| GrimAC … | ✅ 已安装 |`（主反作弊，在用，正常）；:16 `NovaAC … ✅ 已安装`（见 B.3，待确认）。
  - 5-服务器管理\反作弊（…已关闭）\NovaAC.md:33 提到 Themis/GrimAC 问题处置——历史记录。
- 判定：除"反作弊部署与配置指南.md:16 NovaAC ✅已安装"需补停用标注外，其余一致。

### B.5 ViaBackwards
- 1-服务端核心与网络层\04-…ViaVersion\ViaVersion.md:3,13：`本服仅装了 ViaVersion，未装 ViaBackwards 与 ViaRewind`、`未安装 ViaBackwards（…）和 ViaRewind（…）`。
- 判定：**一致**（明确未安装）。

### B.6 ViaRewind
- 同上 ViaVersion.md:3,13。
- 判定：**一致**（明确未安装）。

### B.7 GemsEconomy
| 路径:行号 | 原文摘录 | 判定 |
|---|---|---|
| 3-玩法…\Vault.md:213,216,262,263 | `| GemsEconomy Addon | … | ❌ 两套账 |`；`不要装 GemsEconomy Addon` | 一致 |
| 3-玩法…\SimplePets-Vault-Addon经济联动.md:15,17,85 | `| GemsEconomy Addon | … | ❌ 未装 |`；`本服未装 GemsEconomy，不生效` | 一致 |
| 3-玩法…\宠物系统simplepets.md:27,181,199,209,210 | `GemsEconomy Addon 未装`、`❌ 未装（不推荐）`、官方示例仓库链接 | 一致 |
| 5-服务器管理\…\权限节点速查.md:539 | `GemsEconomy 的 Pet.economy.bypass 本服未装不用` | 一致 |
| 3-玩法…\FarPets-vs-…对比.md:47 | 对比表列 GemsEconomy 为"全免费 addon"（横向对比语境） | 一致 |
| TODO\各种插件（待选）.md:135 | `有 GemsEconomy 经济联动…`（待选插件介绍） | 一致 |
- 判定：**一致**。全仓凡涉及本服决策处均标注 ❌未装/不推荐。

### B.8 LootrInstancer
| 路径:行号 | 原文摘录 | 判定 |
|---|---|---|
| README.md:518 | `13-玩家独立战利品/  LootrInstancer.md`（与在用玩法插件并列于目录树） | 待确认（见 A-17） |
| 3-玩法…\13-玩家独立战利品\LootrInstancer.md:3,13,26,68,72 | 路径自述；`选「1.2」的 Paper jar`；`下载 v1.2 版本 LootrInstancer.jar…放入 plugins`；备份 `plugins/LootrInstancer/data` | **待确认**：文档以"安装指南"口吻写 LootrInstancer v1.2，但运行服实际 jar 为 LootrPlugin-1.2.jar。需确认：是同一插件的文档命名差异（则文件夹/文件应更名对齐 LootrPlugin），还是弃用文档误留在主目录树（则应移出/标注历史） |
| 同文件:23 | `| v1.2.5 等最新版 | … | ❌ Leaf 1.21.11 加载失败 |` | 一致（说明为何锁 v1.2） |
- 判定：命名/归属待确认，非明确"在用"残留，但 README 目录树并列展示易误读。

### B.9 家扩位
| 路径:行号 | 原文摘录 | 判定 |
|---|---|---|
| 3-玩法…\经济系统落地实施-测试服.md:243 | `家园入口隐藏；家扩位入口已取消（命令仍可用）` | 一致（明确已取消） |
| 同文件:104,242 | `新增…1 个 DeluxeMenus 家扩位菜单`、`第二阶段插件落地（…家扩位菜单…5/5）` | 一致（历史落地记录，:243 已交代后续取消） |
| 5-服务器管理\…\权限组设计方案.md:171,173 | `DeluxeMenus homes_menu … essentials.sethome.multiple.2/3/4 … 玩家花南瓜币买家扩位`；`家扩位与组家园上限叠加…` | **待确认**：该处仍以"可购买家扩位"的活设计口吻书写，但整个家园 /sethome 已禁用、sethome.multiple.* 全部不授予（同文件:104）。建议在 171/173 补注"家扩位入口已取消，本机制未上线" |

**B 小结**：CarryMe/TrackPlayer/ViaBackwards/ViaRewind/GemsEconomy 全仓一致；NovaAC、LootrInstancer、家扩位各有 1 处跨文档/口径待确认（详见上表）。

---

## C. 状态标注行收集（不判定，仅罗列，跨文档矛盾核对用）

> 已排除 .history/.workbuddy/.git。行过长已截断至约 160 字符。

### C.1 `❌`（83 条，列关键相关项；其余为 Velocity/对比表通用记号）
- 3-玩法…\南瓜生存服经济体系探索\服务器扣费点清单.md:31 `| 上架费百分比 listing-fee-percent | — | 0.0（未启用） | ❌ |`
- 3-玩法…\SimplePets-Vault-Addon经济联动.md:15 `| GemsEconomy Addon | …两套账 | ❌ 未装 |`
- 3-玩法…\宠物系统simplepets.md:181 `| GemsEconomy Addon | …两套账 | ❌ 未装（不推荐…） |`
- 3-玩法…\13-玩家独立战利品\LootrInstancer.md:23 `| v1.2.5 等最新版 | … | ❌ Leaf 1.21.11 加载失败 |`
- 5-服务器管理\01-权限…\权限组设计方案.md:68 `| 家园（/home /sethome）| ❌（已禁用）| ❌（已禁用）| ✅ |`；:72 `| /back | ❌* | ❌ | ✅ |`
- 5-服务器管理\01-权限…\权限节点速查.md:94 `/home 本服已禁用 ❌`；:95 `/sethome 本服已禁用 ❌`；:96 `/renamehome 本服已禁用 ❌`；:97 `/delhome 本服已禁用 ❌`；:104 `/back 本服已取消 ❌`；:118 `/sell /worth 卖物/估价 ❌ 已禁用（2026-09-30…）`
- 5-服务器管理\06-BedrockPlayerSupport…\…md:14 `家园 /homegui /phomegui ❌ 关闭（家园功能已禁用，2026-09-30）`；:17 `/paygui ❌ 关闭（form.money.enable=false）`；:18 `/pointsgui ❌ 关闭（form.points.enable=false）`；:19 `自动注册/登录 ❌ 关闭（auth.register/login.enable=false）`；:20 `死亡回传 ❌ 关闭（form.back.enable=false，2026-09-27）`；:21 `加入/退出命令 ❌ 关闭（general.join/quit-commands.enable=false）`
- （其余 ❌ 命中：Velocity 多服部署对照、经济设计手册数值约束、网页地图对比、宠物贴图对比、CoreProtect 排查表、QuickMenu 对比表、自定义模型物品对比等通用记号，与本次 22 项清单无直接冲突。）

### C.2 `已禁用`（25 条）
- 2-运维…\EasyBot.md:26 `enable_success_event: false # 绑定成功事件已禁用`
- 3-玩法…\服务器扣费点清单.md:63 `CustomDeathMessages | cost-per-death-message: 0.0 | 死亡消息扣费已禁用`
- 3-玩法…\EssentialsX…md:29 `传送系统（家园 /home 系列已禁用）`；:412 `家园系统（本服已禁用）`；:413 `家园功能（/home /sethome /delhome）已禁用…`
- 3-玩法…\14-自定义附魔-ExcellentEnchants\…md:114 `其余 80 个附魔全部移入 enchants/_disabled_/（已禁用…）`
- 5-服务器管理\01-权限…\权限组设计方案.md:68,100,102,104（家园已禁用，多处）
- 5-服务器管理\01-权限…\权限节点速查.md:94,95,96,97,118,412,413
- 5-服务器管理\06-BedrockPlayerSupport…\…md:14,55
- 自研\快捷菜单系统-QuickMenu.md:660 `EssentialsX：传送（家园功能已禁用）`
- TODO\各种插件（待选）.md:540 `/home（已禁用）`

### C.3 `已取消`（8 条）
- 3-玩法…\经济系统落地实施-测试服.md:243 `家扩位入口已取消（命令仍可用）`
- 3-玩法…\EssentialsX…md:48 `/back（已取消）…本服未启用`；:397 `/back…本服已取消`；:809 `/back…已取消：不授予 essentials.back…`
- 5-服务器管理\01-权限…\权限组设计方案.md:96 `/back 本服已取消（玩家/管理员均不授予，仅 owner 保留）`
- 5-服务器管理\01-权限…\权限节点速查.md:104 `/back 本服已取消 ❌`
- 自研\快捷菜单系统-QuickMenu.md:666 `/back…功能已取消：菜单无入口…`

### C.4 `已停用`
- 命中：**0 条**（注意：NovaAC 实际用的是"已关闭/暂时停用/先停用"，见 NovaAC.md:9,15,33，未出现"已停用"三字）。

### C.5 `未安装`（7 条）
- 1-服务端…\ViaVersion.md:13 `未安装 ViaBackwards …和 ViaRewind…`
- 3-玩法…\BlueMap.md:90,410（BlueMap-Residence 桥接待装，非本清单项）
- 3-玩法…\Simple Voice Chat.md:89,159（模组未装则踢出/提示，中性）
- 5-服务器管理\…20260929第二次CoreProtect疯涨排查记录.md:215（本机未装 CoreProtect jar 的环境说明）
- 自研\快捷菜单系统-QuickMenu.md:539（未安装 BedrockPlayerSupport 时的降级行为）

### C.6 `未启用`（18 条，列相关项）
- 3-玩法…\服务器扣费点清单.md:31 `上架费百分比 0.0（未启用）`；:64 `/tpr、/home、/warp、/spawn 等命令收费未启用`；:65 `EconomyShop…sell-commands: false …/sell 快捷卖出未启用`
- 3-玩法…\EssentialsX…md:48 `/back…本服未启用`
- （其余为白名单/bungeecord/RCON/默认皮肤/MySQL/召唤冷却/TAB 前缀等配置项说明，非本清单项）

---

## 待确认（汇总）

1. **README:355 "可设置的家数量 3"** —— 疑似残留（家园已禁用，玩家实际 0 个家）；需产品确认是否本意是"领地 3 个"。
2. **README:341 `/paygui`** —— 已可判定残留（BPS 分册确认 form.money.enable=false），但仍列出待改。
3. **README:161 CustomDeathMessages "收费"** —— 死亡消息扣费已禁用（cost-per-death-message=0.0），"收费"二字待删/标注。
4. **反作弊部署与配置指南.md:16 NovaAC ✅已安装** —— 与同文件夹"已关闭/停用"结论矛盾，建议补停用标注。
5. **LootrInstancer 命名**：README:518 目录树 + LootrInstancer.md 安装指南，与运行服 LootrPlugin-1.2.jar 的对应关系待确认（同物异名 or 弃用文档）。
6. **权限组设计方案.md:171/173 家扩位购买机制** —— 仍以活设计口吻书写，但家园已整体禁用、家扩位入口已取消，建议补未上线标注。
7. **EconomyShop 卖店口径（README:155）** 与 **EzTax 命名（README:505 "周持有税" vs 清单"死亡费"）** —— 玩家面无残留，仅口径需对齐。
8. **ExcellentEnchants（README:458/519）** —— 插件在用但 80 附魔已移入 `_disabled_`，目录树可补一句括注。

> 本报告为只读检查产物，未对任何业务 .md 做修改。
