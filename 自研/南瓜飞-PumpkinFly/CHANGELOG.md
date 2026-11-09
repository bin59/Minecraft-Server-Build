# PumpkinFly 变更记录

## 2026-11-09 · 1.0.0 首发

**新增插件**：按飞行时长扣秒的付费飞行，独立 `/pfly` 命令 + QuickMenu 固定档位按钮 + 本地 YAML 数据存储。

### 实现要点

| 模块                       | 内容                                                                                                                                                                                          |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PumpkinFlyPlugin`         | 组件装配、两个主线程定时任务（每秒主循环 / 定时落盘）、`/pfly reload` 重建任务、Essentials 自动飞行配置告警                                                                                   |
| `FlyConfig`                | 价格、计时、区域、安全、存储、集成、消息七组配置，所有键有代码内默认值                                                                                                                        |
| `FlyManager`               | 凭证 → 区域 → `setAllowFlight` → 扣秒 → 会话记录 → 安全落地；`FLYING_ONLY` 与 `REAL_TIME` 两种口径；250ms 节流的移动即时判定                                                                  |
| `ZoneChecker`              | 四种区域模式 `OWN_AND_MAIN` / `ALL_RESIDENCES` / `WHITELIST` / `EVERYWHERE`，世界黑白名单、领地属主与信任名单、主城白名单                                                                     |
| `EssentialsGuard`          | 监听 `FlyStatusChangeEvent`（继承 `StatusChangeEvent implements Cancellable`），拦住 `/fly`、`/efly`、`/essentials:fly`、控制台 `/fly <玩家>` 绕过区域限制；独立成类，Essentials 缺失时不注册 |
| `FlyListener`              | 进出服、跨方块移动、切世界/模式、摔落伤害免疫                                                                                                                                                 |
| `EconomyHook`              | Vault `Economy`，扣款用 `withdrawPlayer(OfflinePlayer, double)`，余额真源仍是 Essentials userdata                                                                                             |
| `DataStore` / `PlayerData` | `players.yml`（余额/永久标记/累计时长）、`purchases.yml`（购买记录）、`sessions.yml`（每次起飞时间、结束原因）；定时 + 事件双重落盘，记录数上限裁剪                                           |
| `FlyCommand`               | `buy / on / off / toggle / status / records / give / take / setperm / reload / help` + Tab 补全                                                                                               |

### 关键设计决策

- **不用 `essentials.fly` 权限当凭证**：权限一给就是全局可飞，且 Residence 的 `fly()` 对持有该权限的玩家会跳过干预，容易绕过。插件直接写 `setAllowFlight`。
- **不用 `settemp` 临时权限**：按现实时间走、离线也扣，续费会覆盖，到期只撤权限不关飞行状态。改为插件自己记秒数余额。
- **独立 `/pfly` 而非接管 `/fly`**：避免同名命令的加载顺序覆盖与状态抖动；保留 `/fly` 给管理员菜单使用，另用 `FlyStatusChangeEvent` 兜底门禁。
- **区域判定放运行时**：`saveDefaultConfig()` 不覆盖服务器旧 `config.yml`，所以不能只靠改默认配置生效；`getDisplayMode` 式的运行时判定也一样，所有规则每次 tick 都从当前配置读。
- **Residence `fly` 默认保持 `false`**：查 6.0.4.2 字节码确认 `fly: false` / `nofly: false` 时 Residence 不主动改飞行；改成 `true` 反而会给所有进领地的人开飞。

### 配套改动

- `自研/快捷菜单系统-QuickMenu/src/main/resources/config.yml`：主菜单 slot 28 加「南瓜飞」入口（原 slot 空闲），新增 `pumpkinfly` 子菜单（8 个按钮：状态、开关、1/5/30 分钟、永久、购买记录、飞行记录）；`target/classes/config.yml` 与 `target/QuickMenu-1.0.0.jar` 内配置同步。
- 原 `admin-player` 菜单的 `[player] fly` 按钮保留，未改动。

### 验证

- `python build.py` 编译通过：10 源文件 / 14 class，class major 65，`api-version: '26.3'`，42305 字节。
- 编译期曾报缺 `com.google.common.base.Function`、`net.md_5.bungee.api.chat.BaseComponent`，已在 `build.py` 的 classpath 补入 Guava 33.6.0-jre 与 bungeecord-chat；`@argfile` 里的 Windows 路径统一转正斜杠，避免 javac 把 `\` 当转义符。
- QuickMenu `config.yml` 经 SnakeYAML 2.6 解析通过：30 个菜单，`pumpkinfly` 8 个按钮 slot 无重复，全局 `[menu]` 跳转 0 处失效。

## 2026-11-09 · 1.0.0-r2 修订（已飞时长统计 + 飞行界面显示）

**问题**：`/pfly status` 里「已飞」恒为 0 秒，`sessions.yml` 也没有飞行秒数；且查看状态只能敲 `/pfly`。

### 原因

- `totalUsedSeconds` 原来写在 `if (d.usesTime() && !isRealTime())` 分支里，**永久飞行与 `REAL_TIME` 完全不累加**。
- 落地走 `closeSession(p, "LANDED", false)`，`write=false` 直接丢弃会话；只有到期 / 离区 / 关服才写盘，正常落地不留记录。
- 界面显示没有实现。

### 修改

| 文件 | 内容 |
| --- | --- |
| `FlyManager` | `totalUsedSeconds` 移到飞行分支顶层，所有凭证类型 / 计时模式都累加；落地改 `closeSession(..., true)`；`closeSession` 过滤空会话并按 `track-permanent-sessions` 决定是否记录永久飞行；新增 `currentSessionSeconds(UUID)`；关飞 / 退出 / 关服 / reload 清理显示 |
| `FlyDisplay`（新增） | ActionBar / BossBar / BOTH / OFF 四种模式，每秒刷新「飞行状态 + 剩余时长 + 本次已飞 + 累计已飞 + 进度条」；临期标红变红条，永久飞行满格蓝条；每个玩家独立 BossBar，离开区域自动移除 |
| `FlyConfig` | 新增 `display` 段 10 个键（mode / show-when / update-seconds / warn-seconds / bar-color / bar-style / show-progress-bar / 三个文本模板），全部带代码内默认值，旧 `config.yml` 缺键可直接启动 |
| `PlayerData` | 新增 `hud-enabled`（默认 true），随 `players.yml` 持久化 |
| `FlyCommand` | 新增 `/pfly hud [on\|off]`；`status` 增加「本次已飞」「界面显示」两行；Tab 补全加 `hud` |
| `config.yml` / `plugin.yml` | 新增 `display` 段与 `hud-on` / `hud-off` 消息，命令 usage 补 `hud` |
| `PumpkinFlyPlugin` | `/pfly reload` 先清掉旧 BossBar 再重建任务 |
| QuickMenu | `pumpkinfly` 子菜单 slot 23 加「飞行界面显示」按钮（`pfly hud toggle`），共 9 个按钮；源码 / `target/classes` / jar 内配置字节级一致 |

### 验证

- `python build.py` rc=0：11 源文件 / 15 class / major 65 / `api-version: '26.3'` / 48172 字节。
- `config.yml`、`plugin.yml`、QuickMenu `config.yml` 均经 SnakeYAML 2.2 解析通过；`display` 10 键齐全，`pumpkinfly` 菜单 9 个 slot 无重复且 < 27。
- 三处 QuickMenu 配置 MD5 一致；PumpkinFly 新 jar 与项目根目录副本一致。

### 部署注意

替换 jar 后**完整重启**，不要用 `/reload`。服务器已有的 `plugins/PumpkinFly/config.yml` 不会被覆盖，想用新的 `display` 默认值需手动合并该段（缺键也能跑，代码里有默认值）。旧 `total-used-seconds: 0` 是历史值，不会补记。

## 2026-11-09 · 1.0.0-r3 修订（限时体验奖励 + 全局/单人管理 + QuickMenu 管理面板）

**需求**：插件可以设置几天 / 几小时 / 几分钟的限时体验奖励；可以做全局或单个人的飞行管理；接入快捷菜单。

### 设计

- **体验凭证独立于余额**：`trial-expire-at` 是现实时间戳，有效期内可飞且**不扣余额**，离线也计时；到期自动恢复按个人凭证判定。多次发放叠加到期时间，`max-trial-days` 防误输入。
- **统一时长格式**：`30m` / `2h` / `3d` / `45s` / `1d2h30m` / `1天2小时30分`，纯数字沿用旧口径按分钟；`buy` / `give` / `take` / `trial` / `global trial` / `player trial` 全部共用。
- **三层管理**：全服总开关（`global.enabled`）、全服免费飞行、全服体验（含之后上线玩家）；单人 `banned` / `manual-off` / 个人体验 / 永久标记 / 余额。全局状态单独存 `data/global.yml`，重启不丢，`/pfly reload` 不覆盖。
- **凭证判定收敛到一处**：`FlyManager.hasFlightPass` / `consumesBalance` / `effectiveRemainingSeconds`，tick、移动即时判定、Essentials 门禁、HUD、status 共用，避免口径不一致。

### 修改

| 文件 | 内容 |
| --- | --- |
| `TimeUtil`（新增） | 时长解析（含中文单位、纯数字兼容、366 天上限）、`x 天 x 小时 x 分 x 秒` 格式化、到期时间与日期工具 |
| `GlobalState`（新增） | `enabled` / `free-fly` / `trial-expire-at` / `trial-total-seconds` / `trial-granted-by`，读写 `data/global.yml`，每秒检查到期 |
| `PlayerData` | 新增 `trial-expire-at` / `trial-total-seconds` / `trial-granted-at` / `trial-granted-by` / `claim-date` / `join-trial-given` / `banned`；`hasPass(now)` 改为 永久 > 体验 > 余额且封禁优先 |
| `FlyConfig` | 新增 `global` 与 `rewards` 两段读取（全部带代码内默认值）；消息键从 39 扩到 66 |
| `FlyManager` | tick 支持体验到期、全局状态、体验期暂停 `REAL_TIME` 同步并按剩余余额重算 `expire-at`；新增体验/全服体验发放与清除、购买记录、`passLabel`；批量发放统一落盘；`blockReason` 区分 `global-off` / `banned` |
| `FlyDisplay` | `{remaining}` 带「体验 / 全服体验」前缀，新增 `{source}` 占位符；进度条改用「剩余 / 本次发放总时长」，永久与全服免费恒满蓝条 |
| `FlyCommand` | 新增 `claim` / `trial` / `global` / `player` 四组命令；`buy` / `give` / `take` 支持 `d/h/m`；`status` 支持离线玩家并显示凭证来源、体验剩余、全服状态、禁用标记；Tab 补全分层 |
| `FlyListener` | `PlayerJoinEvent` 按 `rewards.join-trial` 发放首次进服体验，`join-trial-once` 防重复 |
| `PumpkinFlyPlugin` | 装配 `GlobalState`（`config.reload()` 之后），关服落盘 |
| `config.yml` / `plugin.yml` | 新增 `global` / `rewards` 段、全部新消息键、`{source}` 模板；命令 usage 与权限说明同步 |
| QuickMenu `config.yml` | `pumpkinfly` 菜单加 slot 20「领取体验奖励」、slot 24「飞行管理」（`pumpkinfly.admin`），共 11 按钮；新增 `pumpkinfly-admin` 管理子菜单 12 按钮（`[player-selector]` 单人管理）；三处字节级一致 |

### 验证

- `python build.py` rc=0：13 源文件 / 17 class / major 65 / `api-version: '26.3'` / 63248 字节；jar 内 `config.yml`、`plugin.yml` 与源码字节级一致。
- `TimeUtil` 回归测试 18/18 通过（合法 9 组、非法 6 组、格式化 3 组）。
- SnakeYAML 2.2：PumpkinFly 66 条消息键齐全；QuickMenu 31 个菜单 / 256 个菜单项，`pumpkinfly` 11 按钮、`pumpkinfly-admin` 12 按钮无 slot 冲突，40 处 `[menu]` 跳转 0 失效；QuickMenu 原 CRLF 行尾保持。
- 仅本地编译与静态校验，**未在实跑服验证**。

### 部署注意

- 停服替换 `PumpkinFly-1.0.0.jar` 与 `QuickMenu-1.0.0.jar`，**完整重启**，不要 `/reload`。
- 旧 `plugins/PumpkinFly/config.yml` 不会被覆盖：缺 `global` / `rewards` 段时代码默认会开放「`/pfly claim` 每人一次 30 分钟体验」。不打算开放就显式写 `rewards: {claim-trial: ''}`。
- 旧 `plugins/QuickMenu/config.yml` 不会被覆盖：按 `使用说明.md` 第六节合并 `claim` / `admin` 两个按钮与整段 `pumpkinfly-admin`，再 `/qm reload`。
- 旧 `players.yml` 缺新字段按默认值载入，余额与永久标记不受影响；首次启动生成 `data/global.yml`。

---

## 2026-11-10 · 配置基线对齐（QuickMenu）

- `QuickMenu/config.yml` 改以服务器实跑 `plugins/QuickMenu/config.yml` 为基线，完整保留实跑独有的
  `quests-daily` 每日任务菜单、`admin` 面板 `postracker` 按钮、每日任务按钮文案与常用指令速查，
  只增量合入 `main.items.pumpkinfly`（slot 28）、`pumpkinfly`（11 按钮）、`pumpkinfly-admin`（12 按钮）。
- 五处配置字节级一致：服务器实跑配置、项目 `src/main/resources/config.yml`、`target/classes/config.yml`、
  项目根 `config.yml` 副本、jar 内 `config.yml`；统一 LF 行尾（与实跑配置一致）。
- 校验：SnakeYAML 2.2 解析通过，32 套菜单 / 267 个菜单项，全库 slot 无冲突越界，
  42 处 `[menu]` 跳转与 30 个 `back-menu` 0 失效；jar 内 23 个条目中除 `config.yml` 外与旧 jar 完全一致。
- 旧配置备份与合并/校验脚本在 `.workbuddy/backup_快捷菜单实跑对齐_20261110/`。

---

## 2026-11-10 · 实跑配置对齐与排错（无代码改动）

- 服务器实跑 `plugins/PumpkinFly/config.yml` 原为 6687 字节旧版，缺 `global` / `rewards` / `display`
  三段与 29 条 `messages` 键；`FlyConfig.msg()` 缺键会回显 `&c[缺少消息: xxx]`，导致
  `/pfly claim` / `global` / `player` / `trial` 等新命令的反馈全部异常，HUD 也拿不到新模板。
- 逐键比对确认 `prices` / `timing` / `zones` / `safety` / `storage` / `integration` 六段业务参数
  与项目源码**零差异**后，用源码配置整体覆盖实跑配置（12278 字节，统一 LF），仅补段落、消息键
  与 6 条旧版提示文案（「南瓜飞行」→「南瓜飞」、`usage` 补 `claim` / `hud` 等）。
- QuickMenu 实跑 jar 由 73790 → 77310 字节，与项目 `target/QuickMenu-1.0.0.jar` 字节一致
  （20 个 class 未变，仅内嵌默认 `config.yml` 对齐为 32 套菜单 / 267 个菜单项）。
- 归档并移除 `自研/南瓜飞-PumpkinFly/pumpkinfly/` 下两个不参与 `build.py` 编译的旧源码副本，
  清理 `__pycache__/`；备份与脚本在 `.workbuddy/backup_三插件查缺补漏_20261110/`。
- PumpkinFly 的 Java 源码与 jar 本次**未改动**（仍为 13 源文件 / 17 class / 63248 字节）。

---

## 2026-11-10 · 1.0.0-r4 修订（南瓜飞「没界面 / 记录报错」修复）

**现象**（最新启动后）：`/pfly` 相关——没界面显示、没飞行时长正确显示、没自动结束；日志刷
`Couldn't find profile with name: session` / `buy`。

### 根因

| 症状 | 真正原因 | 性质 |
| --- | --- | --- |
| 没界面显示 | QuickMenu `pumpkinfly` 菜单 slot 23「飞行界面显示」按钮原配 `[player] pfly hud toggle`，玩家点一下把默认 true 的 `hud-enabled` 切到 false 并落盘 | 真 bug（按钮语义） |
| 日志记录报错 | QuickMenu「购买记录」`[player] pfly records buy`、「飞行记录」`[player] pfly records session` 被 `records` 命令当成「第二参=玩家名」去 `resolve(name)`，查不到 UUID 就刷 `Couldn't find profile` | 真 bug（命令解析） |
| 没飞行时长 / 没自动结束 | 玩家数据当时有 01:21 自领的 30 分钟限时体验（`trial-expire-at` 激活），体验期 `consume=false` 冻结余额不递减、HUD（又因上条被关）看不到倒计时；体验+余额都耗尽才结束，短测试看不到递减/结束 | 设计行为，非 bug |

### 修改

| 文件 | 内容 |
| --- | --- |
| `FlyCommand.java` | `records()` 重写：第二参若是记录类型关键字（`buy`/`购买`/`b`/`session`/`飞行`/`s`）则视为「自查本人记录」，仅需 `pumpkinfly.records` 权限，不再要求 admin，也不再 `resolve` 成玩家名；第二参是玩家名才走 admin 查他人分支；无参默认查自己购买记录 |
| QuickMenu `config.yml` | `pumpkinfly` 菜单 slot 23 按钮 actions 由 `[player] pfly hud toggle` 改为 `[player] pfly hud on`（点一下强制开界面，不再误关）；项目根 / `src/main/resources` / `target/classes` 三处同步 |

### 验证

- `python build.py` rc=0：13 源文件 / 17 class / major 65 / `api-version: '26.3'` / 63406 字节；新 jar 与项目根目录副本一致。
- QuickMenu 三处 `pfly hud` 配置均为 `on`（校验通过）。
- 逻辑核对：`[player]` 在 QuickMenu 中是 `PLAYER_COMMAND`（`player.performCommand(value)`，`value` 不含前缀、不注入玩家名），故菜单按钮实际下发命令为 `pfly records buy` / `pfly records session` / `pfly hud on`，`args[1]` 即类型关键字/动作，新解析口径完全对得上；`Couldn't find profile` 报错根因消除。

### 部署注意

- 停服替换 `PumpkinFly-1.0.0.jar`，**完整重启**；旧 `plugins/PumpkinFly/config.yml` 不会被覆盖。
- QuickMenu：把 `pumpkinfly` 菜单 slot 23 的 `actions` 改成 `[player] pfly hud on`（或整体替换 `plugins/QuickMenu/config.yml` 后 `/qm reload`）。
- **已受影响玩家**：其 `plugins/PumpkinFly/data/players.yml` 里 `hud-enabled: false` 已被旧按钮落盘，需手动改回 `true`，或让玩家点一次新按钮（现在强制 `on`）、或执行 `/pfly hud on` 即可恢复界面。
- 体验期冻结余额属预期；HUD 恢复后可见体验倒计时，体验到期且余额耗尽即自动关飞。

---

## 2026-11-10 · 1.0.0-r5 修订（基岩 HUD + 不飞也显示倒计时 + 配置文件补齐）

**现象**（基岩/Java 双端实测）：聊天刷 `[缺少消息: trial-grant/claim-ok/on/off/hud-on/hud-off]`；
基岩端「飞行界面显示」整条不显示或颜色符号缺失；QuickMenu 基岩端点击报错；不飞时体验看起来「暂停」。

### 根因

| 症状 | 真正原因 |
| --- | --- |
| `[缺少消息: ...]` 刷屏 | 实跑服 `plugins/PumpkinFly/config.yml` 仍是**旧版（缺 `global`/`rewards`/`display` 三段与 r2/r3 新增消息键）**；源码 `config.yml` 经全量比对确认 66 键齐全、代码引用的键 0 缺失。属**部署未覆盖**问题，非代码 bug |
| 基岩端 QuickMenu 报错 | 同一根因：菜单按钮（如 `pfly claim`/`pfly records buy`）下发的命令回的是缺失键消息；r4 已修 `records` 解析，补配置后按钮全部正常 |
| 基岩端界面不显示 / 颜色符号缺失 | Geyser 对 **ActionBar 经常整条不渲染**，且纯文本进度条 `■` 在基岩端字形缺失；源码 `使用说明.md` 也注明基岩看不到 ActionBar 应改 BOSS_BAR |
| 不飞时体验「暂停」 | HUD 原先按 `allowed`（有凭证 **且** 在飞行区）才显示；玩家在地面 / 暂时离开飞行区时 HUD 消失，造成体验仍在倒计时却「看不见 = 暂停」的错觉（体验本身按现实时间戳到期、离线也走，从未真暂停） |

### 修改

| 文件 | 内容 |
| --- | --- |
| `FlyDisplay.java` | 新增 `isBedrock(Player)`（Floodgate UUID 以 `.` 开头 + Geyser 客户端品牌兜底，零新依赖）；基岩端 **ActionBar 自动升级为原生 BossBar**（BossBar 在基岩端渲染稳定），且基岩端不发送 `■` 文本进度条（原生 BossBar 自带图形进度） |
| `FlyManager.java` | `tick()` 给 HUD 的显示标志由「区域允许的 `allowed`」改为「有有效飞行凭证 `hasFlightPass`」：玩家在地面 / 暂时离区也能看到体验 / 余额倒计时，消除「不飞时暂停」错觉 |
| `config.yml` | 两处 `»` 改为 `>`（基岩端 `»` 易显示成缺字方框）；`src/main/resources` / `target/classes` / jar 内三处同步 |

### 验证

- `python build.py` rc=0：13 源文件 / 17 class / major 65 / `api-version: '26.3'` / 63724 字节；新 jar 与项目根目录副本一致。
- jar 内 `config.yml` `»` 计数 0；`messages` 段 66 键齐全；代码 `msg(...)` 引用键与配置 0 缺失（全量 diff 通过）。
- 逻辑核对：基岩玩家走 BossBar 分支，Java 玩家按 `display.mode` 原样（ACTION_BAR / BOSS_BAR / BOTH）；`show-when: ENABLED`（默认）下 HUD 在「有凭证且未手动关闭」即显示，与是否在飞 / 是否在区无关。

### 部署注意（关键）

1. **必须同时更新配置与 jar**，只换 jar 不够——Bukkit `saveDefaultConfig()` 不会覆盖已有 `config.yml`：
   - 用 `自研/南瓜飞-PumpkinFly/src/main/resources/config.yml`（或新 jar 内 `config.yml`）**覆盖**实跑服 `plugins/PumpkinFly/config.yml`（先备份旧文件）。
   - 该源码配置已含全部业务参数（价格 / 区域等沿用先前实跑对齐结果）+ `global`/`rewards`/`display` 三段 + 66 消息键；若你之后手改过业务参数，请合并而非整盖。
2. 停服替换 `PumpkinFly-1.0.0.jar`，**完整重启**（别用 /reload）。
3. 基岩端无需任何额外设置：检测到 Floodgate/Geyser 即自动走 BossBar；若想强制 Java 也走 BossBar，把 `display.mode` 改成 `BOTH` 或 `BOSS_BAR` 后 `/pfly reload`。
4. 仍显示 `[缺少消息: xxx]` 即表示配置未覆盖成功，检查 `plugins/PumpkinFly/config.yml` 是否真的被替换。

---

## 2026-11-10 · 1.0.0-r6 修订（价格 100/分钟 + 玩家单次封顶 2 小时）

**需求**：飞行价格改为 1 分钟 100 南瓜币；玩家单次任何方式只能购买 2 小时（120 分钟），管理员可随意。

### 修改

| 文件 | 内容 |
| --- | --- |
| `config.yml` | `prices.per-minute` 20 → **100**；`prices.max-minutes-per-buy` 60 → **120**（=2 小时）；`buy-usage` / `invalid-number` 文案同步提示「玩家单次最多 120 分钟」 |
| `FlyConfig.java` | 默认值同步：`perMinute` 默认 20→100，`maxMinutesPerBuy` 默认 60→120（仅在实跑配置缺键时回落，正常读配置） |
| `FlyCommand.java` · `buy()` | 新增 `isAdmin = p.hasPermission("pumpkinfly.admin")`：**普通玩家**仍受 `max-minutes-per-buy`（120）与时长余额上限约束；**管理员**两条上限均跳过，可随意购买任意时长（含 >2h） |

### 影响范围说明

- 所有玩家购买入口（`/pfly buy`、QuickMenu 的 `pfly buy 1/5/30/perm` 按钮）统一经 `buy()`，天然受 2 小时上限约束；QuickMenu 现有按钮最长 30 分钟，不受影响。
- 管理员自身执行 `/pfly buy` 也因持 `pumpkinfly.admin` 而免限；管理员给他人发放走 `/pfly player <名> give`，该路径本就无上限（属「发放」非「购买」）。
- 永久飞行价（`permanent: 100000`）本次未动；余额上限 `max-balance-minutes: 10080`（7 天）保留为存储安全网，仅管理员免限。

### 验证

- `python build.py` rc=0：13 源 / 17 class / major 65 / 63881 字节；项目根与 `target/` 两份 jar 一致。
- jar 内 `config.yml` 确认 `per-minute: 100` 与 `max-minutes-per-buy: 120`。
