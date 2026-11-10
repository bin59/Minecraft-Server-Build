# QuickMenu 变更记录

## 2026-11-10 · 1.0.0-r2：菜单文案对齐（每日任务 / 每日收购）

### 配置改动（项目根 / src / target/classes / jar 内 / 实跑服 五处同步）

| 位置 | 旧 → 新 |
|---|---|
| main 菜单「每日任务」lore | 「共 8 个任务」→「每天 6 个任务轮换，奖励 40~50 南瓜币」 |
| guide-economy「每日任务」lore | 「完成 8 种日常目标」→「每天 6 个轮换任务」 |
| economy / guide-economy「每日收购」lore | 「每天随机 4 种物品高价收购」→「每天 8 种候选自选 5 种高价收购」 |
| 顶部菜单索引注释 | quests-daily 描述改为「14 任务按星期轮换，每天 6 个一键接取」 |
| quests-daily 段前旧注释块 | 删除 DailyQuestPick 时代的过时注释（dq take / 限领 5 个） |

代码零改动，仅 config；`python build.py` 重建 jar **78694 字节**，SnakeYAML 校验
（32 菜单 / quests-daily 17 项含 14 个 days）通过，五处 md5 一致（`631e5363`）。
实跑服已替换 jar 与 `plugins/QuickMenu/config.yml`（旧版备份 `.bak-20261110`）。

### 联动（同日其他改动）

- Quests 实跑 `config.yml`：`confirm-accept: true` → **false**（菜单点一下直接接任务，
  不再要求聊天栏打字确认）；同时清理了该文件里误追加的 3639 行旧 QuickMenu 配置
  （备份 `config.yml.bak-20261110-confirm`）。
- DailySell 升级 1.2.2：基岩玩家 `/ds` 弹原生表单自选/出售，见 `每日随机收购-DailySell/使用说明.md`。

### 回滚

- `git checkout -- 自研/快捷菜单系统-QuickMenu/config.yml` 后重跑 `build.py` 并覆盖实跑服；
- 实跑服 QuickMenu：`cp QuickMenu-1.0.0.jar.bak-20261110 QuickMenu-1.0.0.jar`，
  `cp QuickMenu/config.yml.bak-20261110 QuickMenu/config.yml`；
- Quests 确认开关：`confirm-accept` 改回 `true`。

## 2026-11-10 · 1.0.0-r1：菜单项按星期轮换（days 条件）

### 背景

用户要求每日任务「不要 DailyQuestPick 插件，改为每天切换任务、任务种类多一点」。
原方案（dq 网关）弃用并删除，改为**任务本身按星期轮换**，QuickMenu 提供展示层过滤。

### 代码改动（19 源文件 → target/QuickMenu-1.0.0.jar，79138 字节，major 65）

| 文件 | 改动 |
|---|---|
| `menu/MenuItem.java` | 新增 `days` 字段与 `visibleToday()`：支持 `MON`（三字缩写）/ `MONDAY`（完整名）/ `ALL` / `*`，按服务器本地 `LocalDate` 判定，空列表 = 每天可见 |
| `config/ConfigLoader.java` | `parseItem()` 解析 `days:` 字符串列表 |
| `gui/ChestMenuRenderer.java` | Java 箱子渲染循环加 `visibleToday()` 过滤（与权限过滤同层） |
| `gui/BedrockFormRenderer.java` | 基岩原生表单渲染循环同步过滤（按钮序号表保持正确） |
| `build.py`（新增） | 离线编译脚本（JDK25 `javac --release 21`；classpath = 自研/lib leaf-api + floodgate-api + Residence + 实跑服 Geyser-Spigot.jar + m2 adventure/annotations/guava/bungeecord-chat） |

### 配置改动（项目根 / src / target/classes / 实跑服 四份同步）

- `quests-daily` 菜单整段重写：**14 个每日任务按钮**（7 旧 + 7 新），每项带 `days:` 三天条件，
  每天恰好 6 个可见、相邻两天零重叠、全天 Faucet 约 250~260 南瓜币（周五/周日 250，其余 260）。
- 按钮 `actions` 由 `[player] dq take <id>` 回归 `[player] quests take <连写任务名>`（Quests 原生命令，无中间插件）。
- 布局：任务图标 slots 10-16 / 19-25，quest-list/stats/help 移至顶行 slots 3/4/5。
- 文案（title 下说明、quest-help lore）改为「每天 6 个可选，天天换一批」。

### 联动（不在本插件仓库内）

- 实跑服 `plugins/Quests/storage/quests.yml`：7 → **14 个任务**（新增 dailywalker/quarrier/warrior/shepherd/milker/craftsman/enchanter，
  stage 键名全部经 `BukkitQuestYamlStorage` javap 实测），详见 `3-玩法与玩家功能插件/经济系统/经济插件/Quests每日任务.md`。

### 验证

- SnakeYAML 2.2 Java 校验器（`.workbuddy/quest_validate/ValidateRotation.java`）全绿：
  14 个 `days:` 按钮 / 每天恰好 6 个可见 / 同日无槽位冲突 / 相邻日零重叠 / 奖励合计 250~260 / quests.yml 14 任务键白名单与列表配对通过。
- `python build.py` rc=0；`visibleToday` 已确认进 jar 字节码；项目根与 target jar 字节一致。

### 部署

1. 停服，替换 `plugins/QuickMenu-1.0.0.jar`（新 jar），完整重启。
2. 用项目根 `config.yml` 覆盖 `plugins/QuickMenu/config.yml`（旧文件已备份为 `config.yml.bak-20261110-dq`）。
3. Quests 侧：实跑 `quests.yml` 已更新（原文件备份 `quests.yml.bak-20261110-rotation`），`/questadmin reload`。
4. 验证：打开快捷菜单「每日任务」应只见 6 个任务图标；明天（周二）打开应换一批。

### 回滚

- 代码与配置：`git checkout -- 自研/快捷菜单系统-QuickMenu/`（旧源码在 git HEAD）。
- 实跑 quests.yml：`cp quests.yml.bak-20261110-rotation quests.yml` 后 `/questadmin reload`。
