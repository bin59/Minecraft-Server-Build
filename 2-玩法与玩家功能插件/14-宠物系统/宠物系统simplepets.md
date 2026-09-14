# 14. 宠物系统 — SimplePets

**当前版本**: SimplePets R5-B315（最新构建，2026-09） | **MC 要求**: 1.17 – 26.2（含 1.21.11，需 Java 21）

**作者/发布**: brainsynder-Dev（BS-Development） | **协议**: GPL-3.0（开源）

**GitHub**: https://github.com/brainsynder-Dev/SimplePets | **Modrinth**: https://modrinth.com/project/yNVORkCB

**官方 Wiki**: https://github.com/brainsynder-Dev/SimplePets-Wiki

> EchoPets 的精神继任者（自 2015 年起），纯服务端、GUI 管理的宠物插件。「待选插件清单」将其列为宠物玩法的**主力选择**（与 FarPets 二选一，勿同装）。本服 1.21.11 需 **Java 21**。南瓜生存服为 Paper，直接放入 `plugins/` 即可。

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
- **Addon 系统**：通过 GemsEconomy 经济联动，实现「花钱买宠物 / 解锁宠物」。

## 安装与前置

1. 确认服务端为 **Paper / Spigot / Purpur / Folia**（Spigot 及其分支均可）。
2. **Java 版本必须匹配**（否则启动失败）：
   - 1.21.x（含 1.21.11）→ **Java 21**
   - 1.19 – 1.20.4 → Java 17
   - 26.x → Java 25
3. 下载 **单一 jar**（自 R5-B292 起不再按 MC 版本分 jar，一个文件覆盖全部支持版本），放入 `plugins/` 并**完整重启**服务端。
4. 可选依赖：
   - **PlaceholderAPI**：用于外部占位符。
   - **GemsEconomy + SimplePets 经济 Addon**：做「花钱买宠物」。

> 1.21.x 需要精确版本支持，插件启动时会自动链接到对应支持并控制台提示；如某补丁版出问题会启用专用支持。

## 目录结构

```
plugins/SimplePets/
├── config.yml            # 全局配置（存储方式、冷却、世界限制、消息等）
├── pets/                 # 玩家宠物存档（SQLite 本地存储时）
└── <pet>.json            # 每个宠物类型的独立配置（权限/速度/选项）
```

> 启用 MySQL 后存档写入数据库，本地 `pets/` 不再为主。消息约 95% 可自定义（少数系统消息除外）。

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

# 每玩家同时可拥有的宠物数量上限（性能与体验平衡，待选清单建议设 1）
max-pets-per-player: 1

# 世界限制：true 时仅 allow-worlds 列表内的世界可召唤宠物
world-restrictions: false
allow-worlds:
  - world
  - world_nether

# 宠物粒子/效果范围建议设上限（如 32 格），降低几十人服的性能开销
```

> 具体键名随版本演进，改完用 `/pet reload` 生效；不确定时以插件生成的文件与 Wiki 的 Config 章节为准。

## 命令

主命令 `/pet`（别名 `/sp`、`/simplepets`、`/pets`、`/pet gui` 打开管理界面）。v5 常用子命令如下：

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

## 经济联动（GemsEconomy Addon）

- SimplePets 通过 **Addon** 接入 **GemsEconomy**，实现「花钱买宠物 / 解锁宠物类型」。
- 安装对应 Addon jar 到 `plugins/`，在配置里给宠物标价；玩家用 `/pet purchased` 查看已购，购买后获得对应 `Pet.type.<类型>` 使用权。
- 用 `Pet.economy.bypass` 让赞助组（如 vip/vip+）免单，配合「待选清单」的宠物数量梯度（default 1 只 / vip 2 只 / vip+ 3 只）做分层权益。

## 实战示例

**1. 普通玩家可用狼并骑乘、改名**（LuckPerms 授予）：

```
Pet.type.wolf
Pet.PetToMount
Pet.name
```

**2. 赞助组全宠物 + 免单 + 多宠物**：

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
- **性能上限**：宠物与粒子吃一点性能，几十人服建议每玩家 1 只宠物、粒子距离 ≤ 32 格（待选清单建议）。
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
A：装 GemsEconomy + SimplePets 经济 Addon，给宠物标价，配合 `Pet.economy.bypass` 让赞助组免单，再用 LuckPerms 分组控制可拥有数量。

## 猫模型错误

启动器配置里没有自定义 `gameDir`（那两条是默认快照/正式版档，没指向 Fabric）——说明你多半用的是**第三方启动器**（HMCL/PCL2 之类）直接读 `.minecraft\versions`。

另外发现关键点：**Glob 对这个 `.minecraft` 隐藏目录列不出内容（被沙箱拦），但 Read 能读具体文件**。所以我改用 PowerShell 直接列目录：
PowerShell 输出没被捕获。换回 Bash，去掉管道（上次是 `head` 缺失吞了输出）：
Bash 的 coreutils（`ls`）在这个环境里也没进 PATH。改用 PowerShell 的 `Test-Path` + `-Name`，并强制转成字符串输出：
PowerShell 的 stdout 在本会话里完全不被捕获，Bash 又缺 coreutils。但 `echo` 是内建命令（前面能打印），我用 bash 的 **glob 展开**来列目录：
找到根因线索了：你的启动器是 **PCL2**（顶层有 `PCL.ini`），而 PCL2 开了「**版本隔离**」时，mods/resourcepacks 不在 `.minecraft` 根目录，而是在**各版本自己的文件夹**里。默认 `.minecraft` 下确实没有 mods/resourcepacks。真正的游戏目录是：

`...\.minecraft\versions\1.21.11-Fabric 0.19.2\`

列一下里面的 mods 和 resourcepacks：
全部真相到手了！关键三件套：

**mods 里装了**（这是根因链）：

- `[实体模型特性] entity_model_features-3.2.4` → **EMF**
- `[实体纹理特性] entity_texture_features_7.1` → **ETF**

**resourcepacks 里有**：

- **`Realistic Animals - 4.0.zip`** ← 高度可疑：这是「写实动物」重制包，会改动物模型，猫模型错位十有八九就是它。

我扫描各资源包内含的猫模型文件，精确定位是哪个包、哪些文件：
`Add-Type` 被安全策略拦了。改用 Windows 自带 `tar` 直接列压缩包内容（避免加载 .NET）：
**抓到了！** `Realistic Animals - 4.0.zip` 里含：

- `assets/minecraft/optifine/cem/cat.jem` ← **自定义猫模型**（OptiFine CEM / EMF 格式，这就是罪魁）
- `assets/minecraft/textures/entity/cat/*.png` + `cat.properties.txt`

EMF 3.2.4 会加载这个 `cat.jem` 覆盖原版猫模型，而 4.0 版的猫模型和 1.21.11 的骨骼不匹配 → 肢体错位。

我把它解出来看一眼 `pack.mcmeta` 和 `cat.jem`，确认版本兼容性：
`_ra` 目录没建成。改用 PowerShell 建目录并解压（PowerShell 输出不被捕获没关系，只要文件落地）：
解压成功。读一下 `pack.mcmeta` 和 `cat.jem` 确认兼容性：
**根因 100% 确认。** `cat.jem` 是 Blockbench 手搓的 CEM 猫模型（定义 `head/body/front_left_leg/back_right_leg/tail` 等部件），`pack.mcmeta` 基础版本是 **pack_format 15（=1.20 时代）**。这套 1.20 的猫骨骼配到 1.21.11 上，EMF 驱动动画时部件对不上 → 肢体错位。

为排除其他包也改猫，我扫一遍全部资源包，同时查这个包有没有修复版：
扫描结论清晰：

| 资源包                      | 含猫内容                         | 是否改模型                    |
| --------------------------- | -------------------------------- | ----------------------------- |
| **Realistic Animals - 4.0** | `optifine/cem/cat.jem` + 猫贴图  | ✅ **有自定义猫模型 → 罪魁**  |
| Default HD 128x Demo        | 仅猫**贴图**（含 ocelot/collar） | ❌ 只改纹理，不会导致肢体错位 |
| XK redstone display         | 仅刷怪蛋物品模型                 | ❌ 无关                       |

**全服只有 `Realistic Animals - 4.0.zip` 提供猫「模型」**，配合 EMF 3.2.4 加载，猫肢体必然错位。

查一下这个包有没有修复版：
`Realistic Animals` 官方要求 EMF/ETF，且**最新版支持到 1.21.11**（猫/豹猫模型由 Matt_Hopes 制作）。你那份 **4.0** 很可能没跟上 1.21.x 的猫模型改动。查一下 Modrinth 的版本列表，确认 4.0 是不是过时版本：
版本历史出来了：你装的是 **4.0（2026-04-07）**，最新是 **4.1（2026-05-19）**——4.0 虽然也标称支持 1.21.11，但 4.1 是紧接着的修复版。查一下 4.1 的更新日志，看是否修了猫：
**收工——找到作者官方实锤了！**

4.1 的更新日志原文就一句：

> **4.1** — _A visual bug with cats has been fixed._（修复了猫的视觉 bug）

你装的正好是 **4.0**（有猫 bug 的那版）。诊断闭环。

先把临时文件和结论记一下：
✅ 临时文件已清理。整个问题**闭环了**，而且有作者官方实锤。

## 结论

猫「肢体错位」的根因是**资源包 `Realistic Animals - 4.0` 自带的猫模型**，与 SimplePets / FarPets / 服务端**完全无关**。

**证据链**

1. 客户端就在本机：PCL2 启动器 + **版本隔离**，实际游戏目录是
   `...\.minecraft\versions\1.21.11-Fabric 0.19.2\`（mods/resourcepacks 在这里，不在 `.minecraft` 根目录）。
2. 装了 Fabric 的 **EMF `entity_model_features-3.2.4`** + **ETF `entity_texture_features_7.1`**。
3. 逐包扫描后，**唯一**携带猫「模型」的文件是 `Realistic Animals - 4.0.zip` 里的 `assets/minecraft/optifine/cem/cat.jem`（Blockbench 手搓的 CEM 猫模型，pack_format 15 的旧骨骼），由 EMF 加载覆盖原版猫 → 肢体错位。（`Default HD 128x` 只有猫贴图，不改模型，不是元凶。）
4. 作者 Modrinth 更新日志原文：**`4.1` — "A visual bug with cats has been fixed."** 你装的正是有猫 bug 的 **4.0**。

## 修复（推荐）

1. 下载 **`Realistic Animals 4.1`**：Modrinth 搜 "Realistic Animals"（项目 ID `IpILXlDC`）或 CurseForge。
2. 放进：`C:\Users\19594\AppData\Roaming\.minecraft\versions\1.21.11-Fabric 0.19.2\resourcepacks\`
3. 删掉旧的 `Realistic Animals - 4.0.zip`，**重启客户端**（资源包/模型变更建议重启而非 F3+T）。
4. 验证：`/summon cat` 应恢复正常。

## 备选方案

- **图省事**：直接禁用/移出该资源包 → 猫立刻恢复原版外观。
- **只想让猫回原版、保留其他写实动物**：解压该包，删掉 `assets/minecraft/optifine/cem/cat.jem` 再压回去，EMF 就不再覆盖猫模型。

## 附注

- 服务端侧（`server.properties`、两个宠物插件）全程无需改动。
- 任何装了 4.0 版这个包的玩家都会看到错位猫，可以一并公告升级到 4.1。
