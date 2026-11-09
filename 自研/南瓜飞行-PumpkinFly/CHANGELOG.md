# PumpkinFly 变更记录

## 2026-11-09 · 1.0.0 首发

**新增插件**：按飞行时长扣秒的付费飞行，独立 `/pfly` 命令 + QuickMenu 固定档位按钮 + 本地 YAML 数据存储。

### 实现要点

| 模块 | 内容 |
|---|---|
| `PumpkinFlyPlugin` | 组件装配、两个主线程定时任务（每秒主循环 / 定时落盘）、`/pfly reload` 重建任务、Essentials 自动飞行配置告警 |
| `FlyConfig` | 价格、计时、区域、安全、存储、集成、消息七组配置，所有键有代码内默认值 |
| `FlyManager` | 凭证 → 区域 → `setAllowFlight` → 扣秒 → 会话记录 → 安全落地；`FLYING_ONLY` 与 `REAL_TIME` 两种口径；250ms 节流的移动即时判定 |
| `ZoneChecker` | 四种区域模式 `OWN_AND_MAIN` / `ALL_RESIDENCES` / `WHITELIST` / `EVERYWHERE`，世界黑白名单、领地属主与信任名单、主城白名单 |
| `EssentialsGuard` | 监听 `FlyStatusChangeEvent`（继承 `StatusChangeEvent implements Cancellable`），拦住 `/fly`、`/efly`、`/essentials:fly`、控制台 `/fly <玩家>` 绕过区域限制；独立成类，Essentials 缺失时不注册 |
| `FlyListener` | 进出服、跨方块移动、切世界/模式、摔落伤害免疫 |
| `EconomyHook` | Vault `Economy`，扣款用 `withdrawPlayer(OfflinePlayer, double)`，余额真源仍是 Essentials userdata |
| `DataStore` / `PlayerData` | `players.yml`（余额/永久标记/累计时长）、`purchases.yml`（购买记录）、`sessions.yml`（每次起飞时间、结束原因）；定时 + 事件双重落盘，记录数上限裁剪 |
| `FlyCommand` | `buy / on / off / toggle / status / records / give / take / setperm / reload / help` + Tab 补全 |

### 关键设计决策

- **不用 `essentials.fly` 权限当凭证**：权限一给就是全局可飞，且 Residence 的 `fly()` 对持有该权限的玩家会跳过干预，容易绕过。插件直接写 `setAllowFlight`。
- **不用 `settemp` 临时权限**：按现实时间走、离线也扣，续费会覆盖，到期只撤权限不关飞行状态。改为插件自己记秒数余额。
- **独立 `/pfly` 而非接管 `/fly`**：避免同名命令的加载顺序覆盖与状态抖动；保留 `/fly` 给管理员菜单使用，另用 `FlyStatusChangeEvent` 兜底门禁。
- **区域判定放运行时**：`saveDefaultConfig()` 不覆盖服务器旧 `config.yml`，所以不能只靠改默认配置生效；`getDisplayMode` 式的运行时判定也一样，所有规则每次 tick 都从当前配置读。
- **Residence `fly` 默认保持 `false`**：查 6.0.4.2 字节码确认 `fly: false` / `nofly: false` 时 Residence 不主动改飞行；改成 `true` 反而会给所有进领地的人开飞。

### 配套改动

- `自研/快捷菜单系统-QuickMenu/src/main/resources/config.yml`：主菜单 slot 28 加「南瓜飞行」入口（原 slot 空闲），新增 `pumpkinfly` 子菜单（8 个按钮：状态、开关、1/5/30 分钟、永久、购买记录、飞行记录）；`target/classes/config.yml` 与 `target/QuickMenu-1.0.0.jar` 内配置同步。
- 原 `admin-player` 菜单的 `[player] fly` 按钮保留，未改动。

### 验证

- `python build.py` 编译通过：10 源文件 / 14 class，class major 65，`api-version: '26.3'`，42305 字节。
- 编译期曾报缺 `com.google.common.base.Function`、`net.md_5.bungee.api.chat.BaseComponent`，已在 `build.py` 的 classpath 补入 Guava 33.6.0-jre 与 bungeecord-chat；`@argfile` 里的 Windows 路径统一转正斜杠，避免 javac 把 `\` 当转义符。
- QuickMenu `config.yml` 经 SnakeYAML 2.6 解析通过：30 个菜单，`pumpkinfly` 8 个按钮 slot 无重复，全局 `[menu]` 跳转 0 处失效。
