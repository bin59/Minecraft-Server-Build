# 空岛服 skyblock（BentoBox）

南瓜多服矩阵里的空岛 / 单方块后端子服，Velocity 代理下的第三个后端。核心是 **BentoBox 3.23.6**，装了 **BSkyBlock**（经典空岛）和 **AOneBlock**（单方块）两个游戏模式 addon。

> 网络与端口的当前真源是 [多服网络与端口架构.md](../../1-服务端核心与网络层/多服Velocity +Multiverse/多服网络与端口架构.md)，游戏内跨服与菜单是 [游戏内传送、大厅、菜单等配置.md](../../1-服务端核心与网络层/多服Velocity +Multiverse/游戏内传送、大厅、菜单等配置.md)。本文只讲空岛服本身。

## 官方资料

- BentoBox Modrinth：<https://modrinth.com/plugin/bentobox>
- BentoBox 官方文档：<https://docs.bentobox.world>
- BentoBox addons 组织页：<https://modrinth.com/organization/bentobox>
- 下载站：<https://download.bentobox.world>
- GitHub 组织：<https://github.com/BentoBoxWorld>
- 隐私政策：<https://docs.bentobox.world/en/latest/BentoBox/Privacy/>

## 版本与兼容性

| 组件 | 版本 | api-version | 说明 |
| --- | --- | --- | --- |
| 服务端 | Leaf `leaf-26.3-18.jar`（82,865,974 B） | — | 与本服矩阵其他后端同一个 jar |
| BentoBox | `BentoBox-3.23.6.jar` | 1.21 | 主类 `world.bentobox.bentobox.BentoBox` |
| BSkyBlock | `BSkyBlock-1.20.0.jar` | 1.21 | 经典空岛游戏模式 |
| AOneBlock | `AOneBlock-1.28.0.jar` | 1.21 | 单方块游戏模式 |
| Java | 25 | — | 26.3 基线 |

BentoBox Modrinth 页明确列出兼容 **26.3 / 26.2 / 26.1 / 1.21.x / 1.20.x**，平台支持 Paper / Purpur / Spigot（新版本只正式支持 Paper，Leaf 属 Paper 系）。addon 的 api-version 都是 1.21，在 26.3 上正常加载。

## 网络与启动

| 项 | 值 |
| --- | --- |
| 后端端口 | `30004`，绑 `127.0.0.1` |
| 代理入口 | Velocity `55551`（`/server skyblock`） |
| YOP 认证端口 | `32220`（四服分配：survival 32217 / velocity 32218 / minigames 32219 / skyblock 32220） |
| RCON | `25596` |
| 对外 | ❌ 禁止直连 30004，modern 转发会拒绝无签名连接 |
| MOTD | `§6南瓜国际服 §8▏ §a空岛 · 单方块` |
| 主世界 | `skyblock_hub`（出生枢纽） |
| 内存 | `-Xmx3G -Xms1G` |

`start.bat` 启动参数：

```bat
java -Xmx3G -Xms1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 ^
  -Dauthlibinjector.disableHttpd ^
  -javaagent:YggdrasilOfficialProxy-2.3.0-paperclip.jar ^
  -jar leaf-26.3-18.jar nogui
```

`server.properties` 关键项：

```properties
server-port=30004
level-name=skyblock_hub
gamemode=survival
difficulty=easy
allow-flight=true
online-mode=false
spawn-protection=0
view-distance=6
simulation-distance=4
```

`online-mode=false` 是代理后端的标准配置，认证交给 Velocity + YOP + Floodgate，不要改成 true。

Velocity 侧 `[servers]` 里：`skyblock = "127.0.0.1:30004"`。启动顺序 **survival → minigames → skyblock → Velocity**，等各控制台 `Done (` 再放玩家。

## 插件清单

`skyblock/plugins/` 下只有 4 个 jar：

| jar | 作用 |
| --- | --- |
| `BentoBox-3.23.6.jar` | 空岛核心框架 |
| `FancyNpcs-2.12.1+382.jar` | NPC |
| `QuickMenu-1.0.0.jar` | 快捷菜单（自研） |
| `floodgate-spigot.jar` | 基岩玩家身份 |

BentoBox addon（`plugins/BentoBox/addons/`）：

| addon | 版本 | 说明 |
| --- | --- | --- |
| AOneBlock | 1.28.0 | 单方块模式，jar + 解包配置目录都在 |
| BSkyBlock | 1.20.0 | 经典空岛模式，jar + 解包配置目录都在 |

**这个后端是精简服**：没有 Multiverse、没有 EssentialsX、没有 Vault、没有经济 / 领地 / 统计那一套。回大厅用 `/spawn`（回 `skyblock_hub`），回岛用 `/island spawn`。

## BentoBox 核心配置

`plugins/BentoBox/config.yml`（24,148 B；另有 `config.yml.bak-20261114` 备份）。有效配置：

### 全局

```yaml
general:
  default-language: zh-CN
  use-economy: false          # 没接 Vault 经济
  charge-for-blueprint-on-reset: false
  metrics: true
  database:
    type: JSON                # 存 plugins/BentoBox/database/ 下的 JSON
    backup-period: 5
    max-saved-players-per-tick: 20
    max-saved-islands-per-tick: 20
    max-pool-size: 10
  fakeplayers: ['[CoFH]']
  flingback: 5.0
  did-you-mean:
    unknown-commands: true
    subcommands: true
  brigadier-commands: true
```

数据库是 **JSON 文件**，不是 MySQL：`database/Island`、`database/Names`、`database/Players`、`database/Ranks` 四个目录 + `housekeeping.yml`。另有 `database_backup/` 存 Names / Players / Ranks 的备份。

### 面板与地图

```yaml
panel:
  click-cooldown-ms: 250
  close-on-click-outside: true
  filler-material: light_blue_stained_glass_pane
dynmap:
  island-markers: true
  island-areas: true
bluemap:
  island-markers: true
  island-areas: true
  area-line-color: '#3388FF'
  area-fill-color: '#3388FF'
  area-fill-opacity: 0.15
  area-line-width: 2
```

面板 UI 用浅蓝玻璃片填充，语言 `zh-CN`，`panels/` 下有 8 个面板配置（admin_settings、command_ranks、island_creation、island_homes、language、settings、team_invite、team）。locales 下有 BentoBox / AOneBlock / BSkyBlock 三套语言文件，含 `zh.yml`。

### 岛屿规则

```yaml
island:
  concurrent-islands: 1
  clear-radius: 5
  portal-search-radius: 64
  paste-speed: 64
  purge-level: 10
  delete-speed: 100
  cooldown:
    time:
      invite: 60
      coop: 5
      trust: 5
      ban: 10
      reset: 300          # 重置冷却 5 分钟
    options:
      set-reset-cooldown-on-create: true
  confirmation:
    time: 10
    commands:
      kick: true
      leave: true
      reset: true
  dialogs:
    confirmations: true
    team-invites: true
    go-picker: true
    game-mode-selection: false
  name:
    min-length: 4
    max-length: 20
    uniqueness: false
  deletion:
    housekeeping:
      deleted-sweep:
        enabled: true
        interval-hours: 24
      age-sweep:
        enabled: false
  pregeneration:
    enabled: false
  expel:
    command: spawn         # 驱逐访客送回大厅
  obsidian-scooping-radius: 2
  obsidian-scooping-cooldown: 1
  history:
    max-entries: 0
```

关键值：岛屿名 4~20 字符、可不唯一；重置冷却 300 秒；踢人 / 离队 / 重置都要二次确认（10 秒内）；删除岛屿每 24 小时清扫一次，按年龄清扫关闭；访客被驱逐回 `spawn`。

## BSkyBlock（经典空岛）

`plugins/BentoBox/addons/BSkyBlock/config.yml`（22,802 B，有 `.bak-20261114`）。

命令：`/island is skyblock sb`；管理员：`/bsbadmin bsb skyblockadmin sbadmin sba`。新玩家进服默认动作 `create`（自动创建岛），之后 `/is` 默认 `go`（回自己岛）。

世界设定：

```yaml
world:
  friendly-name: BSkyBlock
  world-name: bskyblock_world
  difficulty: NORMAL
  distance-between-islands: 400
  protection-range: 50
  island-height: 80
  sea-height: 0
  max-islands: 0              # 不限
  concurrent-islands: 1
  disallow-team-member-islands: true
  default-game-mode: SURVIVAL
  default-biome: plains
  default-nether-biome: nether_wastes
  default-end-biome: the_end
  nether:
    generate: true
    islands: true
    roof: true
  end:
    generate: true
    islands: true
    dragon-spawn: false
```

岛屿间距 400 格、保护范围 50 格、岛屿高度 80、海平面 0；下界和末地都生成且有岛，末地龙不刷。

世界 flags（摘录）：`CREEPER_GRIEFING: false`（苦力怕不破坏）、`WORLD_TNT_DAMAGE: false`、`WITHER_DAMAGE: false`、`LIQUIDS_FLOWING_OUT: false`（液体不流出岛）、`TREES_GROWING_OUTSIDE_RANGE: false`、`PISTON_PUSH: false`、`OFFLINE_REDSTONE: true`、`OFFLINE_GROWTH: true`、`PETS_STAY_AT_HOME: true`、`ENDERMAN_GRIEFING: true`（末影人仍可搬方块）、`ENDER_CHEST: false`、`VISITOR_KEEP_INVENTORY: false`、`ENTER_EXIT_MESSAGES: true`。

默认岛屿权限（部分）：

```yaml
default-island-flags:
  DRAGON_EGG: 500      # 访客等级才能拿龙蛋
  LOCK: 0
  HURT_TAMED_ANIMALS: 500
  ANVIL: 500
  TRAPPED_CHEST: 500
  BOOKSHELF: 500
  HARVEST: 500
  CHANGE_SETTINGS: 1000   # 只有岛主能改设置
  TRADING: 0
  CHEST: 500
  HURT_MONSTERS: 0
```

蓝本（`blueprints/`）：`classic-island.json`、`default.json`、`double.blu`、`double_island.json`、`end-island.blu`、`harder.blu`、`harder_island.json`、`island.blu`、`l-shaped.blu`、`nether-island.blu`。

## AOneBlock（单方块）

`plugins/BentoBox/addons/AOneBlock/config.yml`（26,710 B）。

命令：`/ob oneblock`；管理员：`/obadmin oneblockadmin oba`。新玩家默认 `create`，之后 `/ob` 默认 `go`。

```yaml
aoneblock:
  command:
    island: ob oneblock
    admin: obadmin oneblockadmin oba
    bossbar: true           # Boss 条显示阶段进度
    actionbar: true         # ActionBar 显示计数
    count-command: count
    phases-command: phases
    set-count-command: setCount
    set-count-cooldown: 5
    respawn-block-command: respawnBlock check
  placeholders:
    scale-symbol: ■
world:
  friendly-name: OneBlock
  world-name: oneblock_world
  difficulty: NORMAL
  holograms: true
  hologram-offset: 0.5, 1.1, 0.5
  hologram-duration: 10
  starting-safety-duration: 10
  block-id:
    click-type: LEFT        # 左键挖方块
    particle-size: 0.5
    particle-density: 0.65
  distance-between-islands: 400
  protection-range: 50
  island-height: 80
  default-biome: plains
  nether:
    generate: true
    islands: false          # 下界不出岛
    roof: false
  end:
    generate: true
    islands: false
  flags:
    CREEPER_DAMAGE: true
    OBSIDIAN_SCOOPING: false
    ISLAND_RESPAWN: true
```

单方块计数有 Boss 条 + ActionBar 双显示，阶段进度可用 `/ob phases` 查。宝箱粒子按稀有度分色：uncommon 绿、rare 白、epic 黄（DUST 粒子）。

### 阶段时间线

`AOneBlock/phases_index.yml` 与 `AOneBlock/phases/phases_index.yml` 两份索引，共 **21 个阶段**，按挖掘方块数推进：

| 起点 | 阶段 | 长度 | 起点 | 阶段 | 长度 |
| --- | --- | --- | --- | --- | --- |
| 0 | Plains 平原 | 700 | 9500 | Desolation 荒芜 | 1000 |
| 700 | Underground 地下 | 1300 | 10500 | Deep Dark 深暗 | 1000 |
| 2000 | Winter 雪原 | 1000 | 11500 | The End 末地 | 500 |
| 3000 | Ocean 海洋 | 1000 | 12000 | Lush Caves 繁茂洞穴 | 500 |
| 4000 | Jungle 丛林 | 1000 | 12500 | Dripstone 钟乳石洞 | 500 |
| 5000 | Swamp 沼泽 | 1000 | 13000 | Mangrove 红树林 | 500 |
| 6000 | Dungeon 地牢 | 1000 | 13500 | Meadow 草甸 | 500 |
| 7000 | Desert 沙漠 | 500 | 14000 | Cherry Grove 樱花林 | 500 |
| 7500 | The Nether 下界 | 1000 | 14500 | Jagged Peaks 尖峰 | 500 |
| 8500 | Plenty 富饶 | 1000 | 15000 | Sulfur Caves 硫磺洞 | 500 |

末端 `16000_goto_0.yml` 把流程送回阶段 0，形成循环。每个阶段都有独立的 `phases/<起点>_<名字>.yml` 和 `_chests.yml` 战利品表（如 `0_plains.yml` 15,546 B、`6000_dungeon_chests.yml` 33,149 B）。蓝本：`bedrock.blu`、`default.json`。

`panels/phases_panel.yml` 是阶段面板配置（2,158 B）。

## QuickMenu 快捷菜单（两级）

`skyblock/plugins/QuickMenu/config.yml`，两级结构，3 个菜单（`default-menu: worldmenu`）：

```yaml
trigger-item:
  material: CLOCK
  give-on-join: true   # 进服自动给发光时钟（第 8 格）
```

右键时钟打开一级 **模式选择**（27 格，黑玻璃填充）：

| 槽位 | 图标 | 名称 | 动作 |
| --- | --- | --- | --- |
| 11 | OAK_SAPLING | 经典空岛 | `[menu] bskyblock` |
| 15 | CRAFTING_TABLE | 单方块 | `[menu] oneblock` |
| 20 | OAK_LOG | 生存主服 | `[close]` + `[player] server survival` |
| 24 | DIAMOND_SWORD | 小游戏服 | `[close]` + `[player] server minigames` |

二级各带 `back-menu: worldmenu`（右下角 slot 26 自动放返回按钮，Java / 基岩通用）：

**`bskyblock` · 经典空岛岛务**（绿玻璃填充，7 键）：创建岛 `/is create`、我的岛 `/island spawn`、邀请组队 `/is invite`（基岩选人表单）、队伍 `/is team`、设置 `/is settings`、重置 `/is reset`、回大厅 `/server skyblock`。

**`oneblock` · 单方块玩法**（浅蓝玻璃填充，6 键）：我的单方块 `/ob`、计数 `/ob count`、阶段 `/ob phases`、Boss 条 `/ob bossbar`、ActionBar `/ob actionbar`、回大厅 `/server skyblock`。

基岩端走原生表单（`bedrock-native-form: true`），Java 端走箱子 GUI。本服无 Multiverse / Essentials，回大厅靠 `/server skyblock` 重进服落 `skyblock_hub`，不是 `/spawn` 也不是 `/mvtp`。改配置 `/qm reload` 生效。

## 常用命令速查

| 场景 | 命令 |
| --- | --- |
| 创建岛 | `/is create`（新玩家进服自动创建） |
| 回自己岛 | `/is` 或 `/island spawn` |
| 回大厅 | `/server skyblock`（重进服落 `skyblock_hub`） |
| 邀请组队 | `/is invite <玩家>` |
| 队伍管理 | `/is team` |
| 岛屿设置 | `/is settings` |
| 重置岛 | `/is reset`（需二次确认，300 秒冷却） |
| 单方块计数 | `/ob count` |
| 单方块阶段 | `/ob phases` |
| Boss 条开关 | `/ob bossbar` |
| ActionBar 开关 | `/ob actionbar` |
| 跨服 | `/server survival`、`/server minigames` |
| 查赛季（EzSeasons） | 本服未装 |

具体命令名以游戏内 Tab 补全为准；BentoBox 的 Brigadier 命令已开启（`brigadier-commands: true`），支持参数补全。

## 部署约定

- 只改本地文件；云端、`leaf-26.3-多服` 由服主自行更新。本目录记录的是 `leaf-26.3-多服/skyblock` 的实跑配置。
- 后端 `server-port` 固定 `30004`，永远不要改成对外端口（`55551` / `19132`）。2026-11-18 曾因误改云端 survival 端口与 Velocity 撞车导致启动崩溃。
- 改 BentoBox / addon 配置后需重启该后端；addon jar 放进 `plugins/BentoBox/addons/` 后也要重启。
- 旧 jar / 配置改名 `.bak-YYYYMMDD`，不删除（现有 `config.yml.bak-20261114` 即此约定）。
- BentoBox 数据是 `database/` 下的 JSON，改配置前先备份整个 `plugins/BentoBox/`。
- 基岩端：BentoBox 的 GUI 面板在基岩端由 Geyser 转成表单，岛屿 / 单方块玩法本身跨端可用。
