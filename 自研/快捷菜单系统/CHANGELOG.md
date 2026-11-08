# CHANGELOG — QuickMenu

## 2026-10-29

### 新增：管理面板「离线奖励管理」入口（admin-offlinereward）

- 在 `admin` 菜单新增 **`admin-offlinereward`**（CLOCK，**slot 25**），对接自研插件 OfflineReward：
  - 点击执行 `[player] offlinereward reload`（重载离线奖励配置）
  - 随后提示 `[message] &7查询玩家时间戳：&e/offlinereward check 玩家名`
  - 权限 `offlinereward.admin`（OfflineReward 自带，默认 op，**无需改权限即生效**）
  - 说明：`check` 需要输入玩家名，故保留文字指引。
- 玩家侧**不加**入口：离线奖励上线自动发放到南瓜邮箱（PumpkinMail），玩家在 `/mailbox` 领取即可。
- 文档同步：依赖表补 OfflineReward 行、`admin` 菜单描述、管理菜单表补 `admin-offlinereward` 行、LuckPerms 授权块补 `offlinereward.admin`。

### 修复：槽位冲突回归（两份都已修过，本次检查时发现又回来了）

- 本次跑 `ConfigProbe` 发现 **2 处 ERROR**，正是之前修过的位置（说明 `config.yml` 在两次操作之间被外部改动/回退过）：
  - `main.admin` 与 `main.fishing` 同为 `slot: 24` → `main.admin` 重新挪到 **26**
  - `admin.admin-mailbox` 与 `admin.admin-fishing` 同为 `slot: 18` → `admin-fishing` 重新挪到 **24**
- ⚠️ 教训：**只要动过 `config.yml`，部署前必须重跑一次 `ConfigProbe`**。这份文件看起来会在外部被编辑，修复有可能失效。

### 构建

- 编译通过（19 源文件 → 20 class），`config.yml` 校验 **233 菜单项、0 错误、0 警告**。
- 已同步根目录 → `plugin-src/src/main/resources/config.yml`，产物 `dist/QuickMenu-1.0.0.jar`（73.6 KB）。

### 变更：移除「皮肤管理」菜单（skin）

- 应需求去掉快捷菜单里的皮肤管理功能，共删除 122 行配置，**`fishing` / `admin-fishing` / `enchshop` 全部保留**（这三个是此前各版本新增的功能，不在移除范围）。
- 删除内容：
  - 顶层 `skin:` 菜单块（含 skingui / skinhistory / skinfavourites / skinset / skinclear / skinupdate / skinrandom / skinundo 共 8 个菜单项）
  - `main.skin` 入口（PLAYER_HEAD，原 slot 27）
  - `guide-play.skin` 入口（原 slot 12）
  - 文件头菜单索引注释里的 `skin 皮肤管理 SkinsRestorer` 一行
- **保留内容**（不影响玩家直接使用命令）：
  - `config.yml` 中「常用指令速查」消息里的 `/skins` 文本提示
  - 文档中的 LuckPerms `skinsrestorer.command.*` 授权命令块
  - 理由：SkinsRestorer 插件仍在服务器，玩家可直接用 `/skin` 系列命令换肤，只是不再有菜单入口。若想连提示一起清掉可以再说。
- 已将根目录 `config.yml` 同步到 `plugin-src/src/main/resources/config.yml`（jar 内嵌），旧文件备份在 `_build/config.yml.bak.preskin`。
- 重新构建通过：19 源文件 → 20 class，config 校验 **232 菜单项、0 错误、0 警告**，产物 `dist/QuickMenu-1.0.0.jar`（73.4 KB）。解包 jar 验证内嵌配置已无 skin 菜单。

### 修复：config.yml 槽位冲突（静态校验报错）

- 用 `ConfigProbe` 校验根目录 `config.yml`，发现 **2 处槽位冲突**（同菜单内两项占同一格子，后放置的会覆盖前一个，导致功能在 Java 箱子界面上消失）：
  - `main.admin`（管理面板）与 `main.fishing`（更多鱼）都用了 `slot: 24`
  - `admin.admin-mailbox`（南瓜邮箱管理）与 `admin.admin-fishing`（更多鱼管理）都用了 `slot: 18`
- 修复方式（最小改动，保留原有项位置）：
  - `main.fishing` 留在 24，把权限门控的 `admin` 移到空闲槽 **26**
  - `admin-mailbox` 留在 18，把 `admin-fishing` 移到空闲槽 **24**
- 根因：新增钓鱼功能时直接复用了已有项的槽位号，没有做冲突检查。

### 修复：根目录 config.yml 丢失皮肤管理功能，且与 jar 内嵌配置双向不同步

- 根目录 `config.yml` 与 `plugin-src/src/main/resources/config.yml`（构建打进 jar 的那份）长期不同步，且是**双向**缺失：
  - 根有、jar 无：`enchshop`、`main.fishing`、`admin.admin-fishing`、`fishing` 菜单、`admin-fishing` 菜单（共 5 项）
  - jar 有、根无：**`skin`（皮肤管理）顶层菜单 + 8 个皮肤项**，以及 `main.skin`、`guide-play.skin` 两处入口（共 3 块）
- 即本地工作副本把皮肤管理整块弄丢了，而构建产出的 jar 里又没有钓鱼功能。
- 处理方式：以根目录为权威，合并为「皮肤 + 钓鱼」完整版：
  - 从 jar 配置补回 `skin:` 顶层菜单（含 skingui / skinhistory / skinfavourites / skinset / skinclear / skinupdate / skinrandom / skinundo）
  - 补回 `main.skin` 入口（PLAYER_HEAD，槽 **27**，因原 15 已被 `enchshop` 占用）
  - 补回 `guide-play.skin` 入口（槽 12）
- **重要**：构建脚本只打包 `plugin-src/src/main/resources/config.yml`，根目录那份不参与构建。已将合并后的根配置同步过去，旧文件备份在 `_build/config.yml.bak`。

### 构建（本节为「配置合并」那一步的快照，最终状态见本节上方）

- 编译通过（19 源文件 → 20 class），`config.yml` 校验 **242 菜单项、0 错误、0 警告**。
- 产物：`_build/out/QuickMenu-1.0.0.jar` → `dist/QuickMenu-1.0.0.jar`（74.2 KB）。
- 随后按需求移除了皮肤管理菜单并重建，最终为 **232 菜单项 / 73.4 KB**。

### 踩坑

- 在 Bash 工具里用 heredoc 写 Python 时，字符串里的 `\n` 转义会被传输层改写成字面 `/n`，导致插入的 YAML 全部挤在一行、语法失效。
  - 规避：改用 `chr(10)` 生成换行，或直接用 Edit 工具写文本（不经过 shell 转义）。

---

## 2026-10-24

### 新增：领地选择器动作 `[residence-selector]`

- 在快捷菜单里**直接展示玩家自己名下的领地**，无需手敲领地名。
- 点击菜单项后动态列出玩家所有领地（Java 端 = 箱子界面地图图标；基岩端 = 原生表单按钮列表），点选某个领地即执行命令模板，`{residence}` 占位符替换为所点领地名称。
- 典型用法：
  - `[residence-selector] res tp {residence}` —— 传送到所选领地
  - `[residence-selector] res info {residence}` —— 查看领地信息
- 玩家没有任何领地时，界面显示提示（`residence-selector.no-residences`）而非空白。
- 通过 Residence 插件的 `ResidenceApi` **软依赖 + 反射**访问（`getOwnedByPlayer` 兼容 `(Player)` 与 `(Player, boolean)` 两种签名），Residence 不在线时选择器为空，不影响其它菜单。
- 复用既有「动态列表选择器」范式（与 `[player-selector]` / `[item-selector]` 同架构），新增枚举 `ActionType.RESIDENCE_SELECTOR`、配置段 `residence-selector`（title/content/lore/no-residences/back-menu），以及渲染/点击分发。

### 配置变更（config.yml，根目录与 jar 内嵌保持一致）

- `residence` 菜单：
  - 原 `reslist`（查看领地列表）替换为 **`res-tp`**（ENDER_PEARL，slot 10，`[residence-selector] res tp {residence}`）与 **`res-list`**（MAP，slot 11，`[residence-selector] res info {residence}`）。
  - `resinfo` 改为 COMPASS（slot 12），`rescreate`（slot 13），`resremove`（slot 14）；其余项（resetset/resunstuck/resset/respset/ressubzone/resmessage/reshelp）保持不变。
- 新增顶层 `residence-selector:` 配置段，控制选择器界面标题/正文/说明/无领地提示/返回菜单。

### 构建

- `ConfigProbe` 静态校验新增 `[residence-selector]` 已知前缀（跳过误报警告）。
- 重新编译通过（19 源文件 → 20 class，含新增 `ResidenceSelectorHolder`），`config.yml` 校验 227 菜单项无报错。
- 产物：`_build/out/QuickMenu-1.0.0.jar`，已发布到 `dist/QuickMenu-1.0.0.jar`。
