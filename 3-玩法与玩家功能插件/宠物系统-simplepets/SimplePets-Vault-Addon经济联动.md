# SimplePets Vault Addon 经济联动

本文是 SimplePets 主文档的经济扩展篇，专门讲本服已启用的 Vault Addon：让玩家用服务器主货币（EssentialsX 余额，与全服同一套账）花钱购买或解锁宠物，而不是另起一套货币。内容包括 AddonConfig.yml 与 Vault.yml 价格配置、按生物类型调价的方法，以及 pet.vault.bypass 免付权限的用法。

> **状态**：✅ 已安装启用 | **Addon 版本**：0.4（随 SimplePets 主插件运行；主插件当前最高声明支持 26.2，Leaf 26.3 需待官方 26.3 构建或测试服实测）
> **所属**：[宠物系统 SimplePets](宠物系统simplepets.md) 的经济扩展 | **上层经济**：[Vault](../经济系统/Vault.md) + [EssentialsX](../EssentialsX多功能指令整合/EssentialsX多功能指令整合（功能说明与完整配置）.md)

## 一、这是什么

SimplePets 通过「经济 Addon」实现**花钱买宠物 / 解锁宠物类型**。Vault Addon 是其中一种实现：Hook 进服务器已有的 Vault 经济（本服为 EssentialsX），宠物用**服务器主货币**购买——与 `/bal` 余额、Residence、死亡收费**同一套账**，玩家无需第二套货币。

| Addon             | 货币                           | 本服状态  |
| ----------------- | ------------------------------ | --------- |
| **Vault Addon**   | Vault（= EssentialsX 余额）    | ✅ 已启用 |
| GemsEconomy Addon | GemsEconomy 独立货币（两套账） | ❌ 未装   |

> 若改换 GemsEconomy Addon，货币会与全服经济脱钩，本服不推荐。

## 二、安装

| 项           | 值                                                                |
| ------------ | ----------------------------------------------------------------- |
| Addon jar    | `plugins/SimplePets/Addons/VaultAddon.jar`                        |
| 来源         | Modrinth 官方源（项目 `spets-vault`，版本 0.4；下载需 `curl -k`） |
| 启用方式     | 放入 `Addons/` 目录后**重启服务端**（非热加载）                   |
| 启动日志证据 | `[SimplePets ADDON] Loading modules for the Vault addon`          |
| 经济挂钩     | `[Vault] [Economy] Essentials Economy hooked.`（重启时确认）      |

> 重启后插件自动生成两个配置文件（见下）。

## 三、文件结构

```
plugins/SimplePets/
├── Addons/
│   ├── VaultAddon.jar          # Addon 本体
│   └── configs/
│       └── Vault.yml           # ★ 价格与购买行为配置（自动生成）
├── AddonConfig.yml             # Addon 总开关（仅 Vault.Enabled）
└── config.yml                  # SimplePets 主配置
```

## 四、配置详解

### 4.1 AddonConfig.yml（总开关）

```yaml
Vault:
  # Enable/Disable the Vault module
  Enabled: true
```

- `Vault.Enabled: true` = 启用 Vault Addon。
- **修改后需重启服务端**，或在游戏内执行 `/pet addon` **禁用再启用**刷新。

### 4.2 Addons/configs/Vault.yml（价格与行为）★

以下为**本服运行配置实跑值**（`leaf-26.3-多服/plugins/SimplePets/Addons/configs/Vault.yml`），货币即 Vault → EssentialsX 南瓜币，与全服同一套账。

### 4.2.1 行为开关与文案

| 配置项                   | 当前值              | 含义                                                                      |
| ------------------------ | ------------------- | ------------------------------------------------------------------------- |
| `Hide-Price-If-Bypassed` | `true`              | 有免付权限时隐藏价格、显示「已豁免」                                      |
| `Pay-Per-Use-Enabled`    | `false`             | **一次性购买**：花钱解锁后永久可用，不再重复扣费；`true` 则每次召唤都付费 |
| `Price.Free`             | `免费`              | 免费宠物在 lore 里的价格文案（本服已汉化）                                |
| `Price.Bypassed`         | `已豁免`            | 有免付权限时在 lore 里的价格文案（本服已汉化）                            |
| `Boolean.true` / `false` | `是` / `否`（带色） | 已购买状态的 lore 文案                                                    |
| `Messages.prefix`        | `&e简易宠物 &6>>`   | 本插件消息前缀                                                            |
| 购买成功 / 余额不足      | 已汉化              | `{prefix} {type} {price}` 占位符可用                                      |

### 4.2.2 价格档位概览（共 90 种）

| 档位   | 单价      | 数量 | 生物                                                                               |
| ------ | --------- | ---- | ---------------------------------------------------------------------------------- |
| ★ 顶级 | **57999** | 7    | 悦灵 allay、蝙蝠 bat、蜜蜂 bee、烈焰人 blaze、旋风人 breeze、鹦鹉 parrot、恼鬼 vex |
| 传说   | **9999**  | 1    | 凋灵 wither                                                                        |
| 稀有   | **5999**  | 1    | 恶魂 ghast                                                                         |
| 稀少   | **3999**  | 1    | 开心恶魂 happy_ghast                                                               |
| 标准   | **2000**  | 80   | 其余全部（下方完整清单）                                                           |

### 4.2.3 type 段完整价格（带中文注释）

```yaml
type:
  # ===== 57999 档：飞行 / 稀有宠（7 种）=====
  allay: 47999 # 悦灵
  bat: 47999 # 蝙蝠
  bee: 47999 # 蜜蜂
  blaze: 47999 # 烈焰人
  breeze: 47999 # 旋风人
  parrot: 47999 # 鹦鹉
  vex: 47999 # 恼鬼

  # ===== 9999 档：凋灵（1 种）=====
  wither: 59999 # 凋灵

  # ===== 5999 档：恶魂（1 种）=====
  ghast: 5999 # 恶魂

  # ===== 3999 档：开心恶魂（1 种）=====
  happy_ghast: 3999 # 开心恶魂

  # ===== 2000 档：标准宠（80 种）=====
  armadillo: 2000 # 犰狳
  armor_stand: 2000 # 盔甲架
  axolotl: 2000 # 美西螈
  bogged: 2000 # 沼骸
  camel: 2000 # 骆驼
  camel_husk: 2000 # 骆驼尸壳
  cat: 2000 # 猫
  cave_spider: 2000 # 洞穴蜘蛛
  chicken: 2000 # 鸡
  cod: 2000 # 鳕鱼
  cow: 2000 # 牛
  creeper: 2000 # 苦力怕
  creaking: 2000 # 嘎枝
  copper_golem: 2000 # 铜傀儡
  dolphin: 2000 # 海豚
  donkey: 2000 # 驴
  drowned: 2000 # 溺尸
  elder_guardian: 2000 # 远古守卫者
  enderman: 2000 # 末影人
  endermite: 2000 # 末影螨
  evoker: 2000 # 唤魔者
  fox: 2000 # 狐狸
  frog: 2000 # 青蛙
  giant: 2000 # 巨人
  glow_squid: 2000 # 发光鱿鱼
  goat: 2000 # 山羊
  guardian: 2000 # 守卫者
  hoglin: 2000 # 疣猪兽
  horse: 2000 # 马
  husk: 2000 # 尸壳
  illusioner: 2000 # 幻术师
  iron_golem: 2000 # 铁傀儡
  llama: 2000 # 羊驼
  magma_cube: 2000 # 岩浆怪
  mooshroom: 2000 # 哞菇
  mule: 2000 # 骡
  nautilus: 2000 # 鹦鹉螺
  ocelot: 2000 # 豹猫
  panda: 2000 # 熊猫
  parched: 2000 # 焦尸
  phantom: 2000 # 幻翼
  pig: 2000 # 猪
  piglin: 2000 # 猪灵
  piglin_brute: 2000 # 猪灵蛮兵
  pillager: 2000 # 掠夺者
  polarbear: 2000 # 北极熊
  pufferfish: 2000 # 河豚
  rabbit: 2000 # 兔子
  ravager: 2000 # 劫掠兽
  salmon: 2000 # 鲑鱼
  sheep: 2000 # 羊
  shulker: 2000 # 潜影贝
  silverfish: 2000 # 蠹虫
  skeleton: 2000 # 骷髅
  skeleton_horse: 2000 # 骷髅马
  slime: 2000 # 史莱姆
  sniffer: 2000 # 嗅探兽
  snowman: 2000 # 雪傀儡
  spider: 2000 # 蜘蛛
  squid: 2000 # 鱿鱼
  stray: 2000 # 流浪者
  strider: 2000 # 炽足兽
  sulfur_cube: 2000 # 硫磺怪
  tadpole: 2000 # 蝌蚪
  trader_llama: 2000 # 商贩羊驼
  tropical_fish: 2000 # 热带鱼
  turtle: 2000 # 海龟
  villager: 2000 # 村民
  vindicator: 2000 # 卫道士
  wandering_trader: 2000 # 流浪商人
  warden: 2000 # 监守者
  witch: 2000 # 女巫
  wither_skeleton: 2000 # 凋灵骷髅
  wolf: 2000 # 狼
  zoglin: 2000 # 僵尸疣猪兽
  zombie: 2000 # 僵尸
  zombie_horse: 2000 # 僵尸马
  zombie_nautilus: 2000 # 僵尸鹦鹉螺
  zombie_villager: 2000 # 僵尸村民
  zombified_piglin: 2000 # 僵尸猪灵
```

**调价方法**：编辑 `Addons/configs/Vault.yml` 的 `type:` 段，例如把狼改成 500：

```yaml
type:
  wolf: 500 # 原 2000
```

改完执行 `/pet addon` 禁用再启用（或重启服务端）生效。

## 五、权限

| 权限                      | 说明                                                                        |
| ------------------------- | --------------------------------------------------------------------------- |
| **`pet.vault.bypass`**    | **父权限（免付总开关）**：持有者购买任何宠物都免单；给管理组（admin）发这个 |
| `pet.vault.bypass.<生物>` | 单种宠物免付（如 `pet.vault.bypass.wolf`），父权限包含全部子权限            |
| `pet.commands.purchased`  | 查看已购宠物（`/pet purchased`）                                            |
| `pet.commands.addon`      | Addon 管理（`/pet addon` 禁用/启用/更新）——管理用                           |

> ⚠️ **勿混用**：`Pet.economy.bypass` 是 **GemsEconomy Addon** 的免单权限，本服未装 GemsEconomy，不生效。Vault Addon 一律用 **`pet.vault.bypass`**。

## 六、相关命令

| 命令             | 说明                                    |
| ---------------- | --------------------------------------- |
| `/pet purchased` | 查看已购买的宠物                        |
| `/pet addon`     | Addon 管理（禁用/启用刷新配置）         |
| `/pet`           | 宠物主菜单（购买入口在宠物选择 GUI 中） |

## 七、与文档体系的关系

| 文档                                                                                                | 关系                                          |
| --------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| [宠物系统simplepets.md](宠物系统simplepets.md)                                                      | SimplePets 主文档（本文件是其经济章节的展开） |
| [Vault.md](../经济系统/Vault.md)                                                                    | Vault 经济 API 本身                           |
| [EssentialsX 文档](../EssentialsX多功能指令整合/EssentialsX多功能指令整合（功能说明与完整配置）.md) | 主货币来源（currency-symbol `¥`）             |
| [权限节点速查](../../5-服务器管理/权限管理系统-LuckPerms/权限节点速查.md)                           | 权限落地与 LuckPerms 配置                     |

> 链接为相对路径，从 `3-玩法与玩家功能插件/宠物系统/` 出发。
