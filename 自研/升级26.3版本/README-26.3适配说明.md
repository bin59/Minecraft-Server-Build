# 南瓜服自研插件 · Leaf 26.3 适配版

> 目标：把除 `disabled/` 外的 **9 个自研插件**全部落到 **Leaf 26.3（Minecraft 26.3 / Wilderness Bound，API `26.3.build.13-alpha`）**，功能不丢失。
> 完成时间：2026-11-03

---

## 一、一句话结论

**9 个插件全部移植完成，源码功能不变，且已用「真 leaf-api 26.3 + JDK 25 `--release 21`」逐个编译通过（rc=0，共 81 个 class）。** 8 个插件源码与原版**字节一致**（仅 `api-version` 改为 26.3 + 新增 pom），SpacePortal 仅 1 处必要适配，QuickMenu 仅加注释。

---

## 二、目录结构

```
升级26.3版本/
├─ lib/
│  ├─ leaf-api-26.3.build.13-alpha.jar   # 共享服务端 API，各 pom 用 ../lib 引用
│  └─ PumpkinMail.jar                     # 供 BuildRate / OfflineReward 编译的软依赖占位
├─ SpacePortal/ EnchantShop/ BuildRate/ PumpkinMail/ OfflineReward/
│  DailySell/ QuickMenu/ MoneyLedger/ PlanTop/
│     └─ src/main/java/... + src/main/resources/{plugin.yml,config.yml,...} + pom.xml
└─ README-26.3适配说明.md
```

---

## 三、逐插件状态

| 插件 | 源码与原版 | 适配点 | 外置依赖（上线前替换为 26.3 版） | 编译 |
|---|---|---|---|---|
| **SpacePortal** 传送阵 | 1 处改动 | Bungee 动作栏 → Adventure `sendActionBar`（行为一致） | 无（用命令/API 互通 Geyser） | ✅ 13 class |
| **EnchantShop** 附魔商店 | 字节一致 | 无 | 无（磁盘读 ExcellentEnchants 配置） | ✅ 3 |
| **BuildRate** 建筑评分 | 字节一致 | 无（已回退 `getName()` 保 DB 格式） | PumpkinMail（同级构建） | ✅ 13 |
| **PumpkinMail** 南瓜邮箱 | 字节一致 | 无 | 无（SQLite 自带） | ✅ 12 |
| **OfflineReward** 离线奖励 | 字节一致 | 无 | PumpkinMail（同级构建） | ✅ 6 |
| **DailySell** 每日收购 | 字节一致 | 无 | **Vault**（26.3 版 / VaultUnlocked） | ✅ 3 |
| **QuickMenu** 快捷菜单 | 2 处仅加 TODO 注释 | 无 | **Geyser / Floodgate / Residence** | ✅ 20 |
| **MoneyLedger** 南瓜账本 | 字节一致 | 无 | **EssentialsX**（26.3 版） | ✅ 8 |
| **PlanTop** 在线排行 | 字节一致 | 无 | **PlaceholderAPI**；运行连 Plan MySQL | ✅ 3 |

> “源码与原版字节一致”= 与 `自研/<插件>/` 原目录逐字节 diff 无差异，仅 `plugin.yml` 的 `api-version` 改为 `26.3`。

---

## 四、关键适配说明

1. **`api-version: 26.3`**（全部 9 个）——MC 大版本重编号后不再是 `1.xx`，服务端日志确认 `Implementing API version 26.3.build.13-alpha`。
2. **SpacePortal/TeleportService**：旧 `player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(...))` 属已废弃的 BungeeCord 通道，改为 Adventure `player.sendActionBar(Component.text(..., NamedTextColor.AQUA))`。显示内容/颜色不变。
3. **BuildRate/BuildRateCommand**：曾改为 `world.getKey().toString()` 会得到 `minecraft:world`、破坏既有数据库里 `world` 的匹配；已**回退为 `world.getName()`**（26.3 仍可用，仅 obsolete），保证存储格式与老数据一致。
4. **QuickMenu**：`BedrockFormRenderer`、`ResidenceHook` 仅追加 26.3 TODO 注释，逻辑零改动。
5. **其余 6 个**：源码零改动即可在 26.3 编译运行——它们只用了跨版本稳定的 Bukkit API（`sendMessage(String)`、`ChatColor`、`ItemStack`/`ItemMeta`、`Inventory`、`BukkitRunnable` 等）。

---

## 五、编译验证方法（已实测，可复现）

**为什么必须 JDK 25**：`leaf-api-26.3.build.13-alpha.jar` 是 **Java 25 字节码（class v69）**，系统 JDK 21 无法读取；但 `--release 21` 保证**产物仍是 Java 21**（可被仍跑 Java 21 的老服加载）。所以：

```bash
JAVAC="F:/game/pc/MC/开服/test/1.21.11升级26版本/jdk25.0.5_0/bin/javac.exe"
# classpath = leaf-api + 服务端 libraries 全部 jar（含 Adventure 5.2.0 / bungeecord-chat / jspecify）+ 注解 + 插件外置依赖
"$JAVAC" --release 21 -encoding UTF-8 -d out -cp "<leaf-api>;<外置依赖>" $(find src/main/java -name '*.java')
```

- Adventure 是**独立传递依赖**，`leaf-api` 的 systemPath **不会带入**，故每个 pom 都显式声明 `net.kyori:adventure-api:5.2.0` + `org.jetbrains:annotations`（provided）。
- 结果：9 个插件 `rc=0`，合计 81 个 class（见第三节）。

---

## 六、Maven 构建（可选）

各目录已有 `pom.xml`，`maven.compiler.release=21`，`leaf-api` 走 `${project.basedir}/../lib/`。

**构建前必须把 `JAVA_HOME` 指向 JDK 25**（让 Maven 能读 v69 的 leaf-api），否则会报 `class file has wrong version 69.0`：

```bat
set JAVA_HOME=F:\game\pc\MC\开服\test\1.21.11升级26版本\jdk25.0.5_0
mvn -q -f 升级26.3版本\PumpkinMail\pom.xml clean package
mvn -q -f 升级26.3版本\BuildRate\pom.xml clean package
...
```

**构建顺序**：先 `PumpkinMail`，再 `BuildRate` / `OfflineReward`（后两者软依赖前者）。

---

## 七、上线前 TODO（本目录不替你做）

1. **替换外置依赖为 26.3 版**（当前 pom 里是 1.21.11 占位 jar，仅用于编译）：
   - Vault（或 VaultUnlocked 2.20.1）、Geyser ≥2.12.0、Floodgate、Residence 6.0.3.3、EssentialsX（dev build）、PlaceholderAPI 2.12.3、PumpkinMail（用本目录构建产物）。
2. **插件世界数据迁移**：带老世界时，插件存在 `world/<数据>` 的要搬到 `world/dimensions/minecraft/overworld/<数据>`（SpacePortal 等，见 `1.21.11-to-26.3-全量变化对比.md` 2.4）。
3. **真机验证**：QuickMenu 基岩表单 / Residence 反射、PlanTop 连 Plan MySQL、MoneyLedger 监听 Essentials `UserBalanceUpdateEvent`、SpacePortal 传送与粒子特效。
4. `disabled/` 里的插件（AdvZone、DragonManager、UuidMigrate、PosTracker 等）**不在本次范围**，未处理。

---

## 八、备注

- **无 NMS 反射**：编译期已证明（若引用 `craftbukkit`/`net.minecraft` 会直接编译失败）。全项目 `Class.forName` 仅用于 SQLite 驱动与插件间软依赖（MoneyLedger/PumpkinMail/Residence/Floodgate），安全。
- **Adventure 5**：全项目几乎不走 Component，仅 SpacePortal 动作栏一处已迁移。
- 参考：`F:/game/pc/MC/开服/test/1.21.11升级26版本/1.21.11-to-26.3-全量变化对比.md`

---

## 九、环绕观察-Orbit（第 10 个自研插件，独立目录）

> 不算 `disabled/`，本服自研插件实际为 **10 个**；前 9 个见上，第 10 个是 `环绕观察-Orbit`（独立目录，非本集合子目录）。

- 目录：`自研/环绕观察-Orbit/`，同属 26.3 适配目标。
- 适配动作：
  - `plugin.yml` 的 `api-version` 由 `1.21` 改为 **`26.3`**（否则 26.3 服加载失败/告警）；
  - `pom.xml` 依赖由 `paper-api 1.21.11` 改为 `leaf-api 26.3.build.13-alpha`（同本集合 `../升级26.3版本/lib/`）。
- 编译：JDK25 `javac --release 21` + leaf-api 26.3（复用 `_deps/` 缓存），产物 `Orbit.jar`（13K，5 class + plugin.yml + config.yml）。
- 功能：`/orbit <玩家>` 环绕镜头观察 + 定时巡视（半径/高度/速度可配），详见其目录内 `README.md`。
