# 14. 宠物系统 — SimplePets

本文介绍 SimplePets 这款 GUI 管理的纯服务端宠物插件，支持 90 多种生物当跟随伙伴，可骑乘、戴头、多宠同出并存档重召，本服还已接入 Vault Addon 用主货币购买。文档覆盖安装、目录结构、界面汉化、config.yml、/pet 命令、权限与经济联动，末尾另附一起客户端猫模型错位的排查结论（与服务端无关，升级 Realistic Animals 资源包即可）。

**当前版本**: SimplePets R5-B315 | **MC 要求**: 1.17 – 26.2（含 1.21.11，需 Java 21）

**作者/发布**: brainsynder-Dev（BS-Development） | **协议**: GPL-3.0（开源）

**GitHub**: https://github.com/brainsynder-Dev/SimplePets | **Modrinth**: https://modrinth.com/project/yNVORkCB

**官方 Wiki**: https://github.com/brainsynder-Dev/SimplePets-Wiki

> EchoPets 的精神继任者（自 2015 年起），纯服务端、GUI 管理的宠物插件。本服 1.21.11 需 **Java 21**；南瓜生存服为 Paper，直接放入 `plugins/` 即可。勿与 FarPets / RZXPets 同装（功能重叠，二选一）。

## 功能说明

SimplePets 把「跟随伙伴」做成了开箱即用的装饰/养成系统，核心能力：

- **90+ 宠物类型**：几乎覆盖游戏内全部生物（狼、猫、末影人、凋灵、 warden… 均可当宠物）。
- **骑乘 / 帽子 / 多宠物**：可骑乘宠物、把宠物戴到头上（坐肩）、允许多只宠物同时出场。
- **宠物存档（Pet Saves）**：玩家可保存宠物及其数据，之后随时重新召唤，重启/换服不丢。
- **多种改名方式**：铁砧 GUI、对话框、告示牌、聊天，或 `/pet rename` 命令。
- **每只宠物独立 JSON 配置**：控制该宠物的权限、移动速度、外观数据等。
- **服务器管控**：召唤冷却、世界限制、每世界宠物数量上限。
- **数据存储**：支持 **MySQL 与 SQLite**（存档跨服/重启保留）。
- **PlaceholderAPI 支持**：可在其他插件（TAB、计分板）显示宠物信息。
- **Addon 系统**：通过经济 Addon 联动，实现「花钱买宠物 / 解锁宠物」；本服已启用 **Vault Addon**（用服务器主货币 EssentialsX 余额，与全服统一），GemsEconomy Addon 未装。

## 安装与前置

1. 确认服务端为 **Paper / Spigot / Purpur / Folia**（Spigot 及其分支均可）。
2. **Java 版本必须匹配**（否则启动失败）：
   - 1.21.x（含 1.21.11）→ **Java 21**
   - 1.19 – 1.20.4 → Java 17
   - 26.x → Java 25
3. 下载 **单一 jar**（一个文件覆盖全部支持版本），放入 `plugins/` 并**完整重启**服务端。
4. 可选依赖：
   - **PlaceholderAPI**：用于外部占位符。
   - **Vault Addon（本服已安装 ✅）**：用服务器主货币（EssentialsX 余额）买宠物，与全服统一账本；无需第二套货币。

> 1.21.x 需要精确版本支持，插件启动时会自动链接到对应支持并控制台提示；如某补丁版出问题会启用专用支持。

## 目录结构

```
plugins/SimplePets/
├── config.yml            # 全局配置（存储方式、冷却、世界限制、消息等）
├── Addons/               # 经济 Addon（VaultAddon.jar，已装）
├── AddonConfig.yml       # Addon 配置（Vault 启用开关、宠物价格）
├── pets/                 # 玩家宠物存档（SQLite 本地存储时）
└── <pet>.json            # 每个宠物类型的独立配置（权限/速度/选项）
```

> 启用 MySQL 后存档写入数据库，本地 `pets/` 不再为主。消息约 95% 可自定义（少数系统消息除外）。

## 界面汉化（✅）

运行服已做**全界面中文**汉化（无需再改）：

| 位置 | 内容 | 状态 |
|---|---|---|
| `messages.yml` | 插件全部消息（召唤/移除/购买/权限等） | ✅ 中文（插件自带简体） |
| `Pets/<类型>.json` | **95 种宠物的显示名**（GUI 图标名 + 头顶名牌，如「狼」「%player% 的狼宠物」） | ✅ 已汉化 |
| `Items/*.json` | GUI 功能按钮（将宠物戴在头上/骑乘/改名/移除/保存/存档/上下页/安装扩展/宠物数据） | ✅ 已汉化（含新增 `storage.json`，覆盖插件内置的"宠物数据"按钮） |
| `Inventories/*.json` | 界面标题（宠物/宠物存档/宠物数据修改/宠物插件扩展模块） | ✅ 已汉化 |
| `config.yml` | `Simpler-Pet-GUI-Command: true`——**`/pet` 无参直接打开宠物界面**（不再需要 `/pet gui`） | ✅ 已启用 |

> **已知限制**：点击「宠物数据」后打开的**数据子界面**（大小/颜色/声音等数据项，及颜色名如 RED/BLUE）由插件代码硬编码英文，**无法通过配置文件汉化**，需改插件源码或等待插件官方中文。

> 汉化只改显示文字，不动权限与经济逻辑；改回英文可对照插件默认文件恢复。

## 核心配置 (`config.yml`)

配置文件首次启动自动生成，以下为**常见可改方向**与示意（精确键名以实际生成的 `config.yml` 与官方 Wiki 为准）：

```yaml
# 存储后端：SQLITE（默认本地文件）或 MYSQL
storage: SQLITE
mysql:
  host: localhost
  port: 3306
  database: minecraft
  username: <用户>
  password: <密码>

# 召唤冷却（秒），防刷屏
summon-cooldown: 3

# 每玩家同时可拥有的宠物数量上限（性能与体验平衡，建议设 1）
max-pets-per-player: 1

# 世界限制：true 时仅 allow-worlds 列表内的世界可召唤宠物
world-restrictions: false
allow-worlds:
  - world
  - world_nether

# 宠物粒子/效果范围建议设上限（如 32 格），降低几十人服的性能开销
```

> 具体键名随版本演进，改完用 `/pet reload` 生效；不确定时以插件生成的文件与 Wiki 的 Config 章节为准。

> ✅ **运行服实际配置**：上方 yaml 为示意，运行服实际——
> - 存储：`MySQL.Enabled: false`，即 **SQLite 本地存储**（`storage.db`），未启用 MySQL。
> - 召唤冷却：`pet-cooldown.enabled: false`，**运行服未启用召唤冷却**（`duration: 5` 仅在 enabled=true 时生效）；绕过权限 `pet.cooldown.bypass`。
> - 经济 Addon：`AddonConfig.yml` 中 `Vault.Enabled: true`；`Addons/configs/Vault.yml` 全部宠物默认价 **2000**（如 `type.wolf: 2000`），一次性购买（`Pay-Per-Use-Enabled: false`），免付父权限 `pet.vault.bypass`。

## 宠物独立配置与飞行机制

每只宠物一个独立 JSON 文件（`plugins/SimplePets/Pets/<生物>.json`），控制该宠物的开关、速度与外观。**飞行不是全局配置，而是逐宠开关**：

```jsonc
// 蝙蝠 bat.json（会飞）
"mount": true,        // 可骑乘
"hat": true,          // 可戴头上
"fly": true,          // ← 飞行开关
"fly_speed": 0.3,     // 飞行速度
"ride_speed": 0.2,    // 骑乘速度
"walk_speed": 0.58,   // 走路速度
"water_speed": 0.15,  // 游泳速度
"float_down": false,  // 飞行时是否下沉

// 狼 wolf.json（不会飞）
"mount": true,
"fly": false,         // ← 关着的
```

### 让宠物飞起来（三步）

1. **开开关**：把目标宠物 JSON 里的 `"fly": false` 改为 `"fly": true`，重启服务器（或 `/pet reload`）。
2. **给权限**：玩家需要 `Pet.type.<生物>.fly` 权限（如 `Pet.type.bat.fly`；官方 Wiki 权限页写作小写 `pet.type.<mob>.fly`——Bukkit 权限大小写不敏感，两者等价）。
3. **起飞**：召唤后宠物会**悬空飞行跟随**；`mount: true` 的宠物可骑乘，骑上会飞的宠物（蝙蝠、鹦鹉、幻翼，或任意开了 `fly` 的生物）即可空中飞行。

> **要点**：所有生物都能开飞行（狼、牛、猪都能飞），不限于原生会飞的种类；`fly_speed` 可单独调快慢。当前 89 个宠物 JSON 中，蝙蝠开着飞行、狼关着。

## 命令

主命令 `/pet`（别名 `/sp`、`/simplepets`、`/pets`、`/pet gui` 打开管理界面）。常用子命令如下：

| 命令                                                         | 权限                          | 说明                                                    |
| ------------------------------------------------------------ | ----------------------------- | ------------------------------------------------------- |
| `/pet` / `/pet gui`                                          | `Pet.commands.*`              | 打开宠物选择/管理 GUI（召唤、骑乘、存档、改名、戴头上） |
| `/pet summon <类型>`                                         | `Pet.type.<类型>`             | 召唤指定宠物（如 `/pet summon wolf`）                   |
| `/pet hat`                                                   | `Pet.PetToHat`                | 把宠物戴到头上（坐肩跟随）                              |
| `/pet ride`                                                  | `Pet.PetToMount`              | 骑乘宠物                                                |
| `/pet rename [名字]`                                         | `Pet.name`                    | 给宠物改名（颜色/魔法码需对应子权限）                   |
| `/pet remove [玩家]`                                         | `Pet.commands.remove[.other]` | 移除自己/他人宠物                                       |
| `/pet list`                                                  | `Pet.commands.list`           | 列出当前版本可用的宠物                                  |
| `/pet save`                                                  | —                             | 保存宠物数据以便重召（GUI 内亦可）                      |
| `/pet modify <数据>`                                         | `Pet.commands.modify`         | 修改宠物外观/数据                                       |
| `/pet info` / `/pet settings`                                | `Pet.commands.info`           | 查看/设置宠物信息                                       |
| `/pet permissions`                                           | `Pet.commands.*`              | 查看权限相关                                            |
| `/pet purchased`                                             | —                             | 查看已购买的宠物（配合经济 Addon）                      |
| `/pet regenerate`                                            | `Pet.commands.reload`         | 重新生成宠物实体                                        |
| `/pet reload`                                                | `Pet.commands.reload`         | 重载配置                                                |
| `/pet addon`                                                 | —                             | Addon 管理                                              |
| `/pet debug` / `/pet report` / `/pet database` / `/pet data` | `Pet.commands.debug` 等       | 调试/数据/数据库管理（管理用）                          |

## 权限

采用 **`Pet.` 前缀**方案（官方 Wiki Permissions 页）：

- `Pet.*` — 全部权限（服主）。
- 类型总括：`Pet.type.*`（所有宠物）、`Pet.type.passive`（全部被动生物）、`Pet.type.hostile`（全部敌对生物）。
- 单类型：`Pet.type.<类型>`（如 `Pet.type.wolf`）；其下细分行为权限 `.fly` / `.hat` / `.mount` 与数据通配 `.data.*`、具体属性 `.data.<属性>`（如 `.data.sitting`、`.data.age`、`.data.collar`）。
- 帽子/骑乘：`Pet.PetToHat`、`Pet.PetToMount`。
- 改名：`Pet.name`、`Pet.name.color`、`Pet.name.magic`、`Pet.name.bypass`、`Pet.name.bypassLimit`、`Pet.name.*`。
- 经济：`Pet.economy.bypass`（免付费）。
- 命令：`Pet.commands.*` 及 `Pet.commands.<summon/remove/list/modify/reload/info/debug/...>`；对他人操作需 `.other` 变体（如 `Pet.commands.summon.other`、`Pet.commands.remove.other`）。
- 其他：`Pet.itemstorage`（宠物物品栏）。

> 权限以官方 Wiki 与插件实际生成的权限参考为准；若某节点不生效，优先核对前缀（`Pet.` 而非 `simplepets.`）与版本。

## 经济联动（Vault Addon — 已启用 ✅）

SimplePets 通过**经济 Addon** 接入经济系统，实现「花钱买宠物 / 解锁宠物类型」。可选 Addon：

| Addon | 货币 | 状态 |
|---|---|---|
| **Vault Addon**（本服选用） | Hook 进 Vault，用服务器主货币（EssentialsX 余额）购买，与 `/bal`、Residence、死亡收费**同一套账** | ✅ **已安装启用** |
| GemsEconomy Addon | 用 GemsEconomy 独立货币，两套账 | ❌ 未装（不推荐，除非想分开货币） |

### 运行服安装记录

- Addon jar：`plugins/SimplePets/Addons/VaultAddon.jar`（**0.4**，MC 1.18–1.19.4 兼容构建，来源 Modrinth 官方源 `spets-vault` 项目；`curl -k` 下载）
- 重启后日志确认：`[SimplePets ADDON] Loading modules for the Vault addon`
- 自动生成 `plugins/SimplePets/AddonConfig.yml`：默认 `Vault.Enabled: true`，**所有宠物默认价格 2000**
- Vault 经济挂钩：`[Vault] [Economy] Essentials Economy hooked.`（宠物购买扣的是服务器主货币）

### 宠物价格调整

价格文件是 **`plugins/SimplePets/Addons/configs/Vault.yml`**（不是 AddonConfig.yml）——编辑其 `type:` 段按宠物类型分别定价（当前全部默认 2000，如 `wolf: 2000`），改完在游戏内执行 `/pet addon` **禁用再启用**刷新，或重启服务端。

> 完整配置说明（`Pay-Per-Use`、`Hide-Price-If-Bypassed`、各宠物子权限 `pet.vault.bypass.<type>`）见独立文档 [SimplePets-Vault-Addon经济联动.md](SimplePets-Vault-Addon经济联动.md)。

### 权限（重要）

- **Vault Addon 免付费权限：`pet.vault.bypass`**（服务端启动日志确认该节点已随 Addon 注册）——给管理组（admin）免单用这个；
- `Pet.economy.bypass` 是 **GemsEconomy Addon** 的免单权限，本服未装 GemsEconomy，**不要混用**；
- 购买后玩家获得对应 `Pet.type.<类型>` 使用权，用 `/pet purchased` 查看已购。

## 被动能力扩展（Addon）

**SimplePets 本身没有内置被动/技能系统**——FarPets 那种 22 种物种被动（炽足兽抗火、海豚辅助钓鱼、狼近战增伤等）是 FarPets 写死在插件里的功能，SimplePets 不能靠配置复刻。

但 R5 提供 **Addon API**，可以开发自定义 addon 实现同类效果：

- **官方开发文档**：[How to make an addon](https://wiki.bsdevelopment.org/pet-addons/how-to-make-an-addon)（需 Java 16+，Maven 依赖 `simplepets.brainsynder:API`，编译为独立 jar 放入 `plugins/SimplePets/Addons/` 后重启生效）。
- **现有官方/社区 addon 清单**：全部是**经济类**（Vault / PlayerPoints / TokenManager / GemsEconomy / Treasury / ItemEconomy）、**区域类**（Residence / WorldGuard / PlotSquared / RedProtect…）、**实用类**（PetWeight / PvP / Vanish / PermissionLore）——**没有任何现成的被动/技能类 addon**。
- **要复刻 FarPets 式被动**：需自己写 Java addon——监听事件，按玩家当前宠物类型附加对应效果（防火、钓鱼幸运、攻击加成等）。可参考官方示例仓库结构（如 [BSDevelopers/GemsEconomyAddon](https://github.com/BSDevelopers/GemsEconomyAddon)）。

**结论**：加被动是**开发活，不是配置活**。本服已接 Vault Addon + 89 个宠物 JSON 且 QuickMenu 有 `/pet gui` 直达，除非确实想要"养成型被动"玩法，否则不建议为此投入开发；想要成熟被动/养成可另评估 PetBlocks / AdvancedPets / MyPet 这类 RPG 向宠物插件。

## 实战示例

**1. 普通玩家可用狼并骑乘、改名**（LuckPerms 授予）：

```
Pet.type.wolf
Pet.PetToMount
Pet.name
```

**2. 管理组全宠物 + 免单 + 多宠物**：

```
Pet.type.*
Pet.name.*
Pet.economy.bypass
# 另在插件配置里放宽 max-pets-per-player 对该组生效（或靠 Addon 实现多宠物）
```

**3. GUI 流操作**：`/pet` 打开菜单 → 点宠物召唤；点 Hat 戴头上；点 Ride 骑乘；改名走铁砧/聊天。

**4. 跨服共享宠物**：把 `storage` 改为 `MYSQL` 并填好连接，多个子服读同一数据库，宠物存档通用。

## 性能与排错

- **Java 版本必须匹配**：1.21.11 用 Java 21，否则启动直接失败；先确认服务端 Java 版本再放 jar。
- **单一 jar 已含全版本支持**，无需按 MC 版本挑 jar；1.21.x 需精确支持，控制台会提示版本链接情况。
- **性能上限**：宠物与粒子吃一点性能，几十人服建议每玩家 1 只宠物、粒子距离 ≤ 32 格。
- **勿与 FarPets / RZXPets 同装**：功能重叠，二选一即可。
- **重载用 `/pet reload`**，不要用 Bukkit `/reload`（会破坏宠物实体状态）。

## 常见问题（FAQ）

**Q：和 FarPets / RZXPets 怎么选？**
A：SimplePets 生态最完善（Addon/经济/API 完善，文档全），适合做主力；FarPets 功能全（22 种被动加成、70+ 粒子、骑乘、升级），RZXPets 主打浮空肩宠升级到 100 级。三者**二选一，别同装**。

**Q：基岩版（手机/Win10）玩家能看到/召唤宠物吗？**
A：宠物是服务端实体，Geyser 会同步显示给基岩客户端；命令交互按 Java 端权限判定，基岩玩家用表单/指令同样可召唤。

**Q：宠物数据存在哪？**
A：默认 SQLite 本地文件（`pets/`），可改 MySQL 实现跨服共享；存档包含宠物类型、名字、外观数据。

**Q：1.21.11 需要什么 Java？**
A：**Java 21**（1.20.5+ 与 1.21.x 均要求 Java 21）。

**Q：想做「买宠物蛋」经济玩法？**
A：本服已装 **SimplePets Vault Addon**，在 `plugins/SimplePets/AddonConfig.yml` 给宠物标价即可用服务器主货币购买（当前默认 2000/只），货币自动统一；调价后 `/pet addon` 禁用再启用刷新。管理组免费用 **`pet.vault.bypass`**，再用 LuckPerms 分组控制可拥有数量。

**Q：宠物怎么飞？**
A：逐宠开关——把 `plugins/SimplePets/Pets/<生物>.json` 里的 `"fly": false` 改成 `"true"`，重启或 `/pet reload`，再给玩家 `Pet.type.<生物>.fly` 权限即可。召唤后悬空飞行跟随，骑上会飞的宠物可空中骑乘（详见「宠物独立配置与飞行机制」一节）。

**Q：SimplePets 能加 FarPets 那样的被动加成吗？**
A：插件本身无被动/技能系统；需自行开发 addon（官方有 Addon API 与开发文档，但现成 addon 全是经济/区域/实用类，无被动类）。属于开发工作量，不是配置能做到的（详见「被动能力扩展（Addon）」一节）。

## 客户端猫模型错位（与服务端无关）

现象：玩家看到猫肢体错位。结论：与 SimplePets / FarPets / 服务端**完全无关**，根因在客户端资源包。

**证据链**

1. 客户端为 PCL2 启动器 + 版本隔离，实际游戏目录是 `...\.minecraft\versions\1.21.11-Fabric 0.19.2\`（mods/resourcepacks 在此，不在 `.minecraft` 根目录）。
2. 客户端装了 Fabric 的 **EMF `entity_model_features-3.2.4`** + **ETF `entity_texture_features_7.1`**。
3. 逐包扫描后，**唯一**携带猫「模型」的是 `Realistic Animals - 4.0.zip` 里的 `assets/minecraft/optifine/cem/cat.jem`（Blockbench 手搓的 CEM 猫模型，pack_format 15 的 1.20 旧骨骼），由 EMF 加载覆盖原版猫 → 1.21.11 上肢体错位。（`Default HD 128x` 只有猫贴图，不改模型，不是元凶。）
4. 作者 Modrinth 更新日志：**`4.1` — "A visual bug with cats has been fixed."** 当时装的正是有猫 bug 的 **4.0**。

| 资源包                      | 含猫内容                         | 是否改模型                    |
| --------------------------- | -------------------------------- | ----------------------------- |
| **Realistic Animals - 4.0** | `optifine/cem/cat.jem` + 猫贴图  | ✅ **有自定义猫模型 → 罪魁**  |
| Default HD 128x Demo        | 仅猫**贴图**（含 ocelot/collar） | ❌ 只改纹理，不会导致肢体错位 |
| XK redstone display         | 仅刷怪蛋物品模型                 | ❌ 无关                       |

**修复**

1. 下载 **`Realistic Animals 4.1`**：Modrinth 搜 "Realistic Animals"（项目 ID `IpILXlDC`）或 CurseForge。
2. 放进客户端 resourcepacks 目录（PCL2 版本隔离下为 `...\.minecraft\versions\1.21.11-Fabric 0.19.2\resourcepacks\`）。
3. 删掉旧的 `Realistic Animals - 4.0.zip`，**重启客户端**（资源包/模型变更建议重启而非 F3+T）。
4. 验证：`/summon cat` 应恢复正常。

**备选**

- 图省事：直接禁用/移出该资源包 → 猫立刻恢复原版外观。
- 只想让猫回原版、保留其他写实动物：解压该包，删掉 `assets/minecraft/optifine/cem/cat.jem` 再压回去，EMF 就不再覆盖猫模型。

> 服务端侧（`server.properties`、宠物插件）全程无需改动。任何装了 4.0 版该包的玩家都会看到错位猫，可公告升级到 4.1。
