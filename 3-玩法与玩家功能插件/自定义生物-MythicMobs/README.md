# MythicMobs

自定义生物 / 怪物技能 / 自定义物品插件。本服已装但**内容仍是官方示例，未投入实际玩法**。

## 官方资料

- Wiki 首页：<https://wiki.mythiccraft.io/mythicmobs/>
- 官网：<https://www.mythicmobs.net/>
- 命令与权限：<https://wiki.mythiccraft.io/mythicmobs/Commands-and-Permissions>
- 常见问题：<https://wiki.mythiccraft.io/mythicmobs/FAQ>
- 示例合集：<https://wiki.mythiccraft.io/mythicmobs/examples/Common-Examples>
- 新手指南：<https://wiki.mythiccraft.io/mythicmobs/Guides>
- 开发版下载（需付费）：<https://mythiccraft.io/index.php?pages/official-mythicmobs-dev/>
- Discord：<https://www.mythiccraft.io/discord>
- 配置注释里指向的旧地址（已迁出，勿用）：`git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/...`

## 版本与兼容性

| 项 | 值 |
| --- | --- |
| jar | `MythicMobs-5.14.0.jar`，21,836,163 B |
| 版本标识 | `5.14.0-SNAPSHOT-local` |
| 主类 | `io.lumine.mythic.bukkit.MythicBukkit` |
| api-version | 1.13 |
| 服务端 | Leaf 26.3 / Java 25 |
| 加载结果 | 正常：`Loading MythicMobs for Paper (MC: 26.3.0)`、`enabled Paper exclusive functionality` |
| 物品加载耗时 | 38~137 ms（多次启动日志） |

**26.3 可用，不要被 jar 内目录误导。** `META-INF/MANIFEST.MF` 的 Class-Path 里明确带 `Mythic-v26_1-5.14.0.jar`、`Mythic-v26_2-5.14.0.jar`、`Mythic-v26_3-5.14.0.jar` 三份适配分包；`version/wrappers` 目录里只到 `v1_20`，只看那个会误判成不兼容。另有 `Mythic-v1_21_R4~R7` 等历史适配。

运行时由 `SpigotLibraryLoader` 加载 Kyori Adventure 系列依赖（adventure-api 4.16.0、adventure-platform-bukkit 4.3.2、minimessage、text-serializer 等），无需手动放库。

## 已挂接的集成

`plugin.yml` 里 `softdepend`：

```yaml
softdepend:
- Multiverse-Core
- Heroes
- PhatLoots
- Vault
- MagicSpells
- MiniaturePets
- LibsDisguises
- WorldGuard
- Holograms
- HolographicDisplays
- PlaceholderAPI
- PlotSquared
- Residence
loadbefore: [Quests]
```

本服已具备其中：Multiverse-Core、Vault、PlaceholderAPI、Residence。`config-general.yml` 的 `Compatibility` 段默认全开：

| 集成 | 开关 | 附加行为 |
| --- | --- | --- |
| ProtocolLib | `Enabled: true` | 发包类机制依赖 |
| Heroes | `Enabled: true` | — |
| McMMO | `Enabled: true` | `ShowXPMessage: true` |
| SkillAPI | `Enabled: true` | `ShowXPMessage: true` |
| Vault | `Enabled: true` | `ShowMoneyMessage: true`，击杀提示货币 |

货币 / 经验提示格式：

```yaml
MoneyMessageFormat: '&7You receive <drop.amount> currency for slaying <dropper.name>'
XPMessageFormat: '&7You receive <drop.amount> experience for slaying <dropper.name>'
```

## 目录结构与本服现状

`plugins/MythicMobs/` 下：

| 目录 | 本服内容 | 作用 |
| --- | --- | --- |
| `mobs/` | `ExampleMobs.yml`、`VanillaMobs.yml` | 自定义生物定义 |
| `skills/` | `ExampleSkills.yml` | 技能脚本（机制 / 目标 / 条件） |
| `items/` | `ExampleItems.yml` | 自定义物品 |
| `droptables/` | `ExampleDropTables.yml` | 掉落表 |
| `randomspawns/` | `ExampleRandomSpawns.yml` | 随机替换原版刷怪 |
| `dialogs/` | `ExampleDialogs.yml` | Paper Dialog API（1.21.6+）对话框 |
| `config/` | 5 份配置 + `lang/`、`language/`、`menus/` | 全局设置 |
| `spawners/`、`menus/`、`packs/`、`cutscenes/` | 空 | 刷怪笼 / 菜单 / 资源包 / 过场 |
| `data/` | `global-data.json`、`players/`、`worlds/` | 运行时数据 |
| `placeholders.yml` | 0 B | PAPI 占位符扩展 |
| `stats.yml` | 15,491 B | 统计 |

**当前只有示例文件在跑，没有本服自定义的怪物、物品或技能。** 下面是这些示例的实际写法，可直接照抄改。

## 配置逐项说明

### config-general.yml

```yaml
Configuration:
  General:
    Language: en-us
    ThreadPoolSize: -1          # -1 = 自动
    UseVirtualThreads: false
    AsyncPathfinding:
      Enabled: true
      Threads: 0                # 0 = 自动
    AllowMetrics: true
    AnnounceOpReload: false
    CheckForUpdates: true
    DebugLevel: 0
    DebugMode: false
    DebugSpawners: false
    ErrorLogging: true
    ShareComponents: false      # true 省内存，但报错里的来源信息会不准
    CompatibilityMode: false    # true 会把其他插件生成的同名生物认成 Mythic 生物
  Version: 5.6
```

`Packet` 段（26.3 新东西，控制粒子发包）：

```yaml
Packet:
  ParticleBundleEnabled: true       # 多次粒子合并进 ClientboundBundlePacket，原子渲染
  ParticleBundleSize: 512           # 每包上限，范围 [1,4096]
  ParticleTickBatchEnabled: true    # 一 tick 内同一玩家的粒子合并成一包，tick 末发
  RespectClientParticleVisibility: false  # false = 忽略客户端粒子设置，照发
  ClientParticleVisibility:
    All: 100
    Decreased: 50
    Minimal: 10
```

功能总开关：`PlayerFactions: true`、`RandomSpawning: true`、`Spawners: true`。设为 false 即彻底禁用对应功能。

时钟（单位 tick，官方建议不要改）：

```yaml
Clock:
  Main: 1
  Saving: 300
  Spawners: 2
  RandomSpawning: 1
  Scanner: 10
  Cleanup: 600
```

命令：`Commands.SendGiveItemFeedback: true`（`/mm give` 是否回显）。

### config-mobs.yml

掉落默认值：

```yaml
MobDrops:
  DefaultDropMethod: VANILLA
  DoLootsplosionByDefault: false
  DoHologramNameByDefault: false
  DoItemGlowByDefault: false
  DoItemBeamByDefault: false
  DoItemVFXByDefault: false
  DoPerPlayerDropsByDefault: false
  DoClientsideDropsByDefault: false
  MinimumDamagePercentForDrops: 1%
  DefaultLeaderboardTimeout: 6000
```

生物蛋：`Material: PIG_SPAWN_EGG`、`DefaultDisplay: <mob.name> Spawn Egg`、`Model: 0`。

生物默认选项（`DefaultMobOptions`）——**这几个值直接影响玩法**：

```yaml
Despawn: true
NoDamageTicks: 10
PreventOtherDrops: false
PreventVanillaDamage: false
PreventJockeyMounts: false
PreventConversion: false
PreventLeashing: true        # 防止拴绳牵引
PreventItemPickup: true      # 防止捡物品
```

`Mobs`：`CancelDamageIfZero: false`、`KillMessagePrefix: ''`。

全息偏移：全局 `GlobalOffset: 0.15`，血条 `Length: 50 / Scale: 0.4`，名牌背景 `0,0,0,60`。

等级成长（`MobLeveling`）：

```yaml
ScalingEquations:
  Health: V * ((1.05)^(L-1))
  Damage: V * ((1.05)^(L-1))
WorldScaling:
  Default:
    Enabled: true
    ScaleVanillaMobs: true
    PerBlocksFromSpawn: 250    # 离出生点每 250 格升一级
  world2:
    Enabled: true
    PerBlocksFromSpawn: 250
  world2_nether:
    Enabled: false
    PerBlocksFromSpawn: 100
```

> 注意：`world2` / `world2_nether` 是模板示例世界名，本服多世界是 survival / minigames / skyblock，要用得自己改。

### config-skills.yml

只有目标选择器过滤器的默认值，关键项：

```yaml
Targeters:
  DefaultEntityFilters:
    TargetSelf: false
    TargetPlayers: true
    TargetArmorStands: false
    TargetMarkers: false
    TargetCreativeMode: true
    TargetSpectatorMode: true
    TargetInvulnerable: true
    TargetAnimals: true
    TargetCreatures: true
    TargetMonsters: true
    TargetWaterMobs: true
    TargetFlyingMobs: true
    TargetSameFaction: true
    TargetOwner: true
    TargetNonMythic: true
    TargetVillagers: true
```

### config-items.yml

```yaml
ItemUpdating:
  Enabled: false       # 物品更新器，改了定义后自动替换玩家背包旧物品
  OnLogin: true
  OnScroll: true
  OnInvClose: true
EnableFancyDropEffectsEverywhere: false
DefaultItemOptions:
  Material: STONE
  PreventEnchanting: false
  PreventCrafting: modeled   # true / false / modeled / default
  PreventAnviling: default
  PreventSmithing: default
```

下面这些段落标注 `requires Crucible`——**本服没装 Crucible，配了也不生效**：`ItemSkills`（`JoinDelay: 20`）、`ItemStats`、`LoreTemplates`、`CustomBlocks`、`Equippables`（NECKLACE / RING_1 / RING_2 自定义槽）、`Furniture`（`DefaultType: DISPLAY`）。

### config-spawning.yml

```yaml
Spawners:
  DisableCommandSaving: false
RandomSpawning:
  DisableVanillaSpawns: false
  GenerateSpawnPoints: false
  SpawnRadiusPerPlayer: 64
  SpawnRadiusPerPlayerMin: 12
  SpawnRadiusPerPlayerY: 16
  PlayerClusterDistance: 24.0
  LimitMultiplier: 1.2
  LocalGroupMultiplier: 0.1
  SpawningLimit: -1           # -1 = 不限
  LocalSpawningLimit: -1
  IgnoreVanillaMobs: false
  IgnoreUnnaturalMobs: false
  ReplaceObeysCap: false
  SharedCooldown: true
  MaxGenerationAttempts: 10
  Generator: NONE             # ADD 方式的生成器：NONE / CLUSTER / LEGACY
  MaxGenerationTime: 5        # 每 tick 最多花 5 ms 用于刷怪，防卡顿
```

## 语法速查（全部摘自本服示例文件）

### 生物：mobs/*.yml

```yaml
SkeletalKnight:
  Type: WITHER_SKELETON
  Display: '&aSkeletal Knight'
  Health: 40
  Damage: 8
  Equipment:
  - IRON_HELMET HEAD
  - IRON_SWORD HAND
  - SHIELD OFFHAND
  Drops:
  - GOLD_NUGGET{display="Gold Coin"} 1to2 0.5     # 物品 数量 概率
  LevelModifiers:
  - health 5
  - damage 0.5
  Skills:
  - speak{m="..."} @PlayersInRadius{r=40} ~onCombat 0.2
  Options:
    MovementSpeed: 0.1
```

要点：

- `Type` 是原版实体 ID；`Display` 支持 `&` 颜色码（示例用老式，新语法支持 MiniMessage）。
- `Equipment` 格式 `物品 槽位`，槽位 `HEAD/CHEST/LEGS/FEET/HAND/OFFHAND`，可直接写 Mythic 物品 ID（如 `KingsCrown HEAD`）。
- `Drops` 格式 `物品 数量 概率`；数量支持 `1to2`、`32-64`、`1-12`；概率省略为 1。
- `Skills` 格式 `技能 目标器 触发器 权重`（权重 0.2 等）。
- `Options` 常用：`AlwaysShowName`、`MovementSpeed`、`MaxCombatDistance`、`PreventOtherDrops`、`Size`（史莱姆）、`KnockbackResistance`。
- `DamageModifiers` 可作伤害免疫：`LIGHTNING 0`（免疫）、`FIRE 0.5`（半伤）。

触发器写法：`~onCombat`、`~onPlayerKill`、`~onTimer:100`（每 100 tick）。

### 技能：skills/*.yml

```yaml
SmashAttack:
  Cooldown: 8                 # 秒
  Conditions:
  - targetwithin{d=25}
  Skills:
  - message{cooldown=30;m="..."} @PlayersInRadius{r=40}
  - teleport @target
  - effect:sound{s=mob.endermen.portal;volume=1.0;pitch=0.5}
  - delay 10                  # tick
  - damage{amount=5;ignorearmor=true} @PlayersInRadius{r=5}
  - throw{velocity=10;velocityY=5} @PlayersInRadius{r=5}
  - effect:explosion @Self
```

示例里用到的机制：`message`、`speak`、`teleport`、`delay`、`damage`、`throw`、`effect:sound`、`effect:explosion`、`effect:particles`、`potion`、`summon`、`skill{s=子技能}`、`blockmask`、`showDialog`、`setvariable`、`variableAdd`、`variableSubtract`、`closeDialog`。

目标器：`@self`、`@target`、`@trigger`、`@PlayersInRadius{r=40}`、`@LivingInRadius{r=10}`、`@PlayersOnServer{targetself=true}`。

条件：`targetwithin{d=25}`、`playerwithin{d=40}`、`outside true`、`compareValues{v1=...;op=>;v2=...}`。

占位符：`<mob.name>`、`<caster.name>`、`<trigger.name>`、`<target.name>`、`<skill.value>`、`<&co>`（逗号）。

### 物品：items/*.yml

```yaml
SkeletonKingSword:
  Id: DIAMOND_SWORD
  Display: '&3Greatsword of the Skeleton King'
  Lore:
  - '&6A powerful sword used by'
  Enchantments:
  - DAMAGE_ALL:5
  - KNOCKBACK:2
  - FIRE_ASPECT:2
  Attributes:
    MainHand:
      Health: 10
      MovementSpeed: 0.1
```

`Hide: [ATTRIBUTES, ENCHANTS]` 隐藏属性 / 附魔行；`Options.Color: 255,40,40` 给皮革染色；护甲槽位 `Head`。

⚠️ `Enchantments:` 走 Bukkit 附魔表，用的是**老式枚举名**（`DAMAGE_ALL` = 锋利、`PROTECTION_ENVIRONMENTAL` = 保护）。能否写数据包新增的命名空间附魔（如 `minecraft:density`）尚未实测——这是武器方案里的待验证点。

### 掉落表：droptables/*.yml

```yaml
SkeletonKingDrops:
  Conditions:
  - playerwithin{d=40}
  Drops:
  - SkeletonKingItemDrops 1       # 嵌套掉落表
  - experience 100
  - heroesexp 200

SkeletonKingItemDrops:
  MinItems: 1
  MaxItems: 2
  Drops:
  - KingsCrown 1 0.01             # 1%
  - SkeletonKingSword 0.1         # 10%
  - GOLD_NUGGET 32-64 1
  - DIAMOND 1-12 0.8
```

### 随机刷怪：randomspawns/*.yml

```yaml
RandomSkeletonKing:
  Type: SkeletonKing
  Worlds: world
  Chance: 0.001        # 0.1%
  Priority: 1
  Action: REPLACE      # 替换原版刷怪
  Conditions:
  - outside true
```

> `Worlds: world` 是世界名，本服要用得改成 `survival` 等实际世界名。

### 对话框：dialogs/*.yml

基于 Paper Dialog API（1.21.6+），26.3 可用。触发器 `showDialog{dialog=WelcomeNotice} @trigger`。支持 MiniMessage、按玩家过滤的 Conditions、输入框（text / bool / range / option）、按钮技能、分页（`ForEach` + `PageSize` + `<dialog.page>/<dialog.pages>`）。示例里有 `WelcomeNotice`、`QuestOffer`、`CharacterSetup`、`PlayerPicker`、`PlayerProfile` 五个完整范例。

## 命令与权限

| 命令 | 别名 | 用途 |
| --- | --- | --- |
| `/mythicmobs` | `/mm` | 主命令，`/mm reload` 重载配置与内容文件 |
| `/mythicmobsmenu` | `/mmmenu`、`/mmm` | 图形菜单 |
| `/spawnmob` | `/mspawn`、`/msummon`、`/spawnmythicmob` | 生成自定义生物 |
| `/pins` | `/pin` | 固定点管理 |
| `/addpin` | — | 添加固定点 |

权限：`mythicmobs.admin`（默认 OP，全部权限）、`mythicmobs.reload`、`mythicmobs.signal`。更多见官方命令页。

## 长期武器制作中的定位

按能力分工：

| 需求 | 载体 |
| --- | --- |
| 底材、属性、耐久、附魔、配方、战利品、外观 | 数据包：`weapon` / `piercing_weapon` / `kinetic_weapon` 组件 + 数据驱动附魔 |
| 右键主动技能、AOE、条件判断、连击、怪物掉落 | MythicMobs |
| 与自研经济 / 菜单 / 玩家数据深度绑定 | 自研插件 |

性能定位：常驻监听 + 技能调度，属平台级（内存 20 MB 以上，启动十几秒），比纯数据包重；单次攻击开销在事件 → 条件 → 技能执行，微秒到毫秒级。真正的带宽消耗来自技能粒子 / 音效，与载体无关——本服 `ParticleTickBatchEnabled: true` 已把同 tick 粒子合并发包，有利。

## 部署约定

- 只改本地文件；云端、`leaf-26.3-多服` 由服主自行更新。
- 新增 / 修改 `mobs/`、`skills/` 等目录后执行 `/mm reload`；换 jar 必须完整重启，`/mm reload` 不能替代。
- 旧 jar 改名 `.bak-YYYYMMDD`，不删除。
- 改 `Worlds:` 前先确认实际世界名（survival / minigames / skyblock），别照抄示例的 `world` / `world2`。
- 基岩端：MythicMobs 的物品模型走 Java 客户端资源包，基岩玩家看到的是原版外观，靠名字与附魔光效区分。
