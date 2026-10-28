# CHANGELOG — QuickMenu

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
