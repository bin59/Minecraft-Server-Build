# PlayerHeadDrops（玩家头颅掉落）

## 是什么

玩家死亡时**掉落带有本人皮肤的头颅**（皮肤直接嵌入物品，重启/重进不丢）。掉落概率、一次性、冷却、仅 PvP、防刷头、世界黑白名单、自定义名字与 lore、生物头、Looting/权限加成、公告、Vault 经济奖励、音效粒子——全部可配。轻量无依赖（Vault 仅经济奖励可选）。

- 版本：2.0.0（SpigotMC #135728，作者 Kasperoid）
- 平台：Paper / Purpur / Spigot / Bukkit；Minecraft **1.13+**；Java 8+
- **1.21.11（Leaf）兼容性**：原生主版本 1.21，官方实测到 26.2——大概率兼容，放入 plugins 实测加载

## 部署状态（2026-10-04 核对）

- ✅ **已部署**：运行服 `plugins\PlayerHeadDrops-2.0.0.jar`（31 KB，10/4 23:00 放入）
- ✅ 插件已正常加载并生成数据目录 `plugins\PlayerHeadDrops\`（`config.yml` + `messages.yml`，10/4 23:09）
- **已应用配置（10/4 23 时改动）**：`drop-chance: 0.3`、`cooldown-seconds: 600`（同一玩家头 10 分钟内不再掉）、`announce.enabled: true`（全服公告，中文提示语）。其余保持官方默认（仅 PvP 关、生物头关、经济关）。
- 改配置后**重启服务端或重载插件**生效。
- `messages.yml` 为插件语言文案文件（默认英文/俄语），改聊天提示语在这里。

## 功能

| 功能 | 说明 |
|---|---|
| 皮肤保留 | 头颅皮肤嵌入物品，服务器重启 / 玩家重进不丢 |
| 掉落概率 | 0–100%（`drop-chance`），另可叠加 Looting 与权限加成 |
| 一次性掉落 | `one-time-drop: true`：每玩家头颅全场只掉一次（数据跨重启保留） |
| 冷却 | `cooldown-seconds`：同一玩家头颅掉落后冷却期内不再掉 |
| 仅 PvP | `only-pvp: true`：只有被其他玩家击杀才掉头；怪物/自杀/`/kill` 不算 |
| 直入背包 | `drop-to-inventory`：头直接进击杀者背包而非地面 |
| 防刷头 | `anti-farm`：忽略自杀、虚空死亡、自我击杀 |
| 世界控制 | `worlds` 黑名单 / 白名单 |
| 自定义名字/Lore | MiniMessage + 占位符 `%player% %killer% %world% %date% %time%` |
| 生物头 | 原版生物头（僵尸/骷髅/苦力怕/凋零骷髅/猪灵/末影龙）各自独立概率 |
| Looting 加成 | 击杀者武器每级 Looting 加掉落概率 |
| 权限加成 | 有 `playerheaddrops.boost` 权限的玩家概率乘倍数 |
| 公告 | 掉落时全局或半径内聊天公告 |
| 经济奖励 | 每次掉落给击杀者 Vault 货币（`economy.reward`） |
| 音效/粒子 | 掉落伴随音效与粒子 |

## 默认配置要点（config.yml · v2.0.0）

```yaml
config-version: 2
drop-chance: 1.0          # 掉落概率 0~1（1=100%）
one-time-drop: false      # 每人只掉一次
cooldown-seconds: 0       # 冷却秒数
only-pvp: false           # 仅 PvP 击杀才掉
drop-to-inventory: false  # 直接进击杀者背包

anti-farm:
  ignore-suicide: true    # 忽略自杀
  ignore-void: false      # 忽略虚空
  ignore-self-kill: true  # 忽略自我击杀

worlds:
  mode: blacklist         # blacklist / whitelist
  list:
    - world_the_lobby     # 示例值，上服前按真实世界名改
    - creative_world

head:
  name-format: "%player%'s Head"
  name-color: yellow
  lore:
    - "<gray>Killed by: <white>%killer%</white></gray>"
    - "<gray>World: <white>%world%</white></gray>"
    - "<dark_gray>%date% %time%</dark_gray>"

mob-heads:
  enabled: false
  default-chance: 0.05
  only-player-kill: true
  chances:
    ZOMBIE: 0.05
    SKELETON: 0.05
    CREEPER: 0.05
    WITHER_SKELETON: 0.10
    PIGLIN: 0.05
    ENDER_DRAGON: 1.0

looting-boost:
  enabled: false
  per-level: 0.1

permission-boost:
  enabled: false
  multiplier: 2.0

announce:
  enabled: false
  message: "<yellow>☠ %player%'s head has dropped!</yellow>"
  radius: -1              # -1 = 全服公告

economy:
  enabled: false
  reward: 0.0

effects:
  sound:
    enabled: false
    name: entity.player.levelup
  particles:
    enabled: false
    name: SOUL
```

### 当前运行值 vs 默认值（2026-10-04）

| 配置项 | 默认值 | 当前运行值 | 说明 |
|---|---|---|---|
| `drop-chance` | 1.0 | **0.3** | 30% 概率掉头 |
| `one-time-drop` | false | false | 每人可重复掉落 |
| `cooldown-seconds` | 0 | **600** | 同一玩家头 10 分钟内不再掉 |
| `only-pvp` | false | false | 非 PvP（怪物/自杀等）也掉 |
| `drop-to-inventory` | false | false | 掉到地面 |
| `anti-farm.ignore-suicide` | true | true | 忽略自杀 /kill |
| `anti-farm.ignore-void` | false | false | 虚空掉落也算 |
| `anti-farm.ignore-self-kill` | true | true | 忽略自我击杀 |
| `worlds.mode` | blacklist | blacklist | 黑名单制 |
| `worlds.list` | world_the_lobby / creative_world | 同（示例值） | 上服前按真实世界名改 |
| `head.name-format` | %player%'s Head | 同 | 头颅名 |
| `head.lore` | 英文默认 | 同 | 头颅说明 |
| `mob-heads.enabled` | false | false | 生物头关闭 |
| `looting-boost.enabled` | false | false | Looting 加成关闭 |
| `permission-boost.enabled` | false | false | 权限加成关闭 |
| `announce.enabled` | false | **true** | 全服掉落公告 |
| `announce.message` | ☠ %player%'s head has dropped! | **☠ %player% 的头颅掉落了！**（中文） | |
| `announce.radius` | -1 | -1 | -1 = 全服 |
| `economy.enabled` | false | false | Vault 经济奖励关闭 |
| `effects.sound.enabled` / `particles.enabled` | false / false | false / false | 音效粒子关闭 |

**加粗 = 相对默认已修改**；其余均保持官方默认。

## 权限

| 权限 | 作用 |
|---|---|
| `playerheaddrops.boost` | 掉落概率加成倍数（需在配置开启 `permission-boost`） |

> 其他指令/权限（如重载）官方页未列出，以服内实际为准；装好后可实测 `/ph ...` 或 `playerheaddrops.reload` 是否存在。

## 与现有玩法注意

- **当前已调优**：掉头概率 0.3 + 同一玩家头 10 分钟冷却 + 全服掉落公告（中文），刷头/刷屏风险已可控。若仍觉得频，可把 `drop-chance` 再降或把 `cooldown-seconds` 调大。
- `drop-chance: 0.3` = 30% 概率掉头；`cooldown-seconds: 600` = 同一玩家死亡后 10 分钟内不再掉该玩家的头。
- 有领地（Residence）的生存服，若不想"头当战利品炫耀/乱放"，可结合 `only-pvp` 与 `anti-farm` 控制掉头场景。
- `worlds.list` 默认是示例世界名（world_the_lobby / creative_world），**上服前务必按你服真实世界名改**，否则黑名单不生效。

## 下载

SpigotMC：https://www.spigotmc.org/resources/playerheaddrops.135728/
