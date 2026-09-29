# 7. 方块记录与回滚 — CoreProtect

本页是南瓜生存服方块记录与回滚插件 CoreProtect CE 的说明文档，涵盖当前版本、关键配置、MySQL 数据库搭建步骤、常用查询与回滚命令，以及第三方网页查询面板 CoLWI 的部署与安全要求，供服主排查 grief 破坏、追回被盗物品和审计玩家行为时使用。

**当前版本**: CoreProtect CE v24.0 | **MC 要求**: 1.16.5+

**官方网站**: https://coreprotect.net | **GitHub**: https://github.com/PlayPro/CoreProtect

**官方文档**: https://docs.coreprotect.net/
**下载**: https://modrinth.com/plugin/coreprotect/versions

> 📚 **同目录文档**：《一些操作.md》（完整命令/参数手册）、[CoreProtect插件：玩家的行为数据库过大](CoreProtect插件：玩家的行为数据库过大)（数据库清理与 MySQL 迁移专题）

## 功能说明

CoreProtect 是 Minecraft 最知名的方块记录插件，能记录服务器中几乎所有的方块操作、容器交互、实体杀戮和聊天命令，并支持精确到单个方块的**回滚和还原**操作。此版本为 **CoreProtect Community Edition (CE)**，开源免费。

v24.0 主要更新：回滚性能提升约 80%、新增 8 个 API 查询方法、支持 MC 26.1、`/co purge` 参数验证、大量新记录类型（村民 AI 行为、漏斗拾取、自定义方块等）。

## 关键配置 (`plugins/CoreProtect/config.yml`)

```yaml
use-mysql: true # 本服实际：已启用 MySQL（库 coreprotect）
language: zh-CN # 中文语言
api-enabled: true # 启用 API
default-radius: 10 # 默认回滚半径
max-radius: 100 # 最大回滚半径
rollback-items: true # 回滚包含物品
rollback-entities: true # 回滚包含实体
skip-generic-data: true # 跳过通用数据
verbose: true # 详细模式

# 以下记录全部启用:
block-place: true # 记录方块放置
block-break: true # 记录方块破坏
natural-break: true # 记录自然破坏
block-movement: true # 记录方块移动（沙/沙砾）
pistons: true # 记录活塞推动
block-burn: true # 记录烧毁
block-ignite: true # 记录自然起火
explosions: true # 记录爆炸
entity-change: true # 记录实体改方块
entity-kills: true # 记录实体击杀
sign-text: true # 记录告示牌文字
buckets: true # 记录桶操作
tree-growth: true # 记录树木生长
water-flow: true # 记录水流
lava-flow: true # 记录岩浆流
liquid-tracking: true # 液体追踪
item-transactions: true # 记录物品交易
item-drops: false # 记录物品丢弃
item-pickups: false # 记录物品拾取
hopper-transactions: true # 记录漏斗交易
player-interactions: true # 记录玩家交互
player-messages: true # 记录聊天消息
player-commands: true # 记录执行命令
player-sessions: true # 记录登录登出
username-changes: true # 记录改名
worldedit: true # 记录 WorldEdit 操作
```

```
item-drop: false         # 丢物品日志，建议关
item-pickup: false       # 拾取物品日志【强烈关闭，这就是百万条记录元凶】
```

## 黑名单（blacklist.txt）——按用户/伪用户/命令/方块/实体禁用记录

黑名单可整体禁用指定**用户（含伪用户 `#`）、命令、方块、实体**的记录。文件：`plugins/CoreProtect/blacklist.txt`（无则新建），每行一条，改完 `/co reload` 生效（无需重启）：

```
#hopper      ; 漏斗自动传输（矿车漏斗轨道循环会疯狂刷库，见下）
#dispenser   ; 发射器自动传输
#dropper     ; 投掷器自动传输
#tnt         ; TNT 爆炸
#creeper     ; 苦力怕
/help        ; 命令
minecraft:stone   ; 方块（需带命名空间，仅影响 block 动作，v23+）
minecraft:creeper ; 实体（需带命名空间，禁用其死亡记录，v24+）
```

要点（官方文档确认）：

- **伪用户条目不需要命名空间**（`#hopper` 直接写）；方块/实体/物品必须带 `minecraft:`
- **普通物品/方块条目不影响 item / container 动作**，只有过滤条目（`id@user`）才影响它们
- **过滤语法**（v24+）：`id@user` 只禁用"指定来源操作指定物品"的记录，如 `minecraft:minecart@#hopper`——只挡漏斗传输矿车，其余漏斗操作照常记录
- 实体黑名单与 `id@user` 过滤需要 **v24+**（本服 v24.0 满足）；方块黑名单需要 v23+

**本服用途：防漏斗刷库**——矿车在漏斗轨道上循环时 `#hopper` / `#dispenser` 伪用户记录剧增（可占 91%、单时段数万条）。推荐先写 `#hopper` / `#dispenser` / `#dropper` 三条（跳过自动传输、保留玩家手动容器记录）；要更彻底可另设 `hopper-transactions: false`（见「数据库过大」文档方案五）。已有历史数据用 `/co purge t:30d #optimize` 清理。

## MySQL 数据库（本服已启用）

本服 CoreProtect 已连接 MySQL（库 coreprotect，连接信息见服务端 plugins/CoreProtect/config.yml 的 mysql-* 段）。

完整迁移教程（建库建用户 → 改配置 → 迁移数据 → 验证清理）见 [2.将CoreProtect从SQLite切换到MySQL.md](CoreProtect插件：玩家的行为数据库过大/2.将CoreProtect从SQLite切换到MySQL.md)。迁移涉及数据安全，操作前务必备份 plugins/CoreProtect/database.db。
## 常用命令

### 查询类

| 命令                                    | 说明                                  |
| --------------------------------------- | ------------------------------------- |
| `/co inspect`                           | 开启/关闭检查模式（点击方块查看记录） |
| `/co lookup <参数>`                     | 查询方块变更记录                      |
| `/co lookup u:<玩家> t:<时间> r:<半径>` | 查询某玩家在范围内操作                |
| `/co lookup b:<方块ID> t:<时间>`        | 查询特定方块变更                      |
| `/co lookup a:<操作类型>`               | 查询特定操作类型                      |
| `/co near`                              | 查询附近最近变更                      |

| 表名             | 中文名称               | 作用说明                                                                                                                                                              | 运维备注（对你锁等待问题很关键）                                                                                        |
| ---------------- | ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| co_block         | 方块操作日志表         | **主表**，记录方块放置/破坏/爆炸/火烧/液体流动。存储：时间、玩家ID、世界、X/Y/Z、旧方块、新方块、方块状态。`/co lookup`、`/co rollback`、`/co purge` 主要操作这张表。 | 这张表数据量最大！`/co purge` /大范围rollback会对这张表执行大量DELETE，极易触发行锁，就是你报错的源头。**必须建立索引** |
| co_container     | 容器库存操作表         | 箱子、潜影盒、熔炉等容器物品增减记录。记录谁拿了/放入物品、坐标、容器NBT。                                                                                            | 容器查询专用，数据量一般小于co_block                                                                                    |
| co_item          | 实体物品日志表         | 掉落物、物品展示框、盔甲架物品变化。拾取、放置、丢失物品。                                                                                                            | 可在config关闭item记录，减少写入压力                                                                                    |
| co_sign          | 告示牌文本记录表       | 记录告示牌文字修改。                                                                                                                                                  | 体积很小                                                                                                                |
| co_chat          | 聊天记录表             | 玩家公屏聊天内容。                                                                                                                                                    | 独立表，不参与方块回滚                                                                                                  |
| co_command       | 命令执行日志表         | 玩家执行的服务器指令（需要开启记录命令选项）                                                                                                                          | 小表                                                                                                                    |
| co_session       | 玩家会话表             | 玩家上下线记录，登录/退出时间。                                                                                                                                       | 小表                                                                                                                    |
| co_user          | 用户映射表             | 玩家UUID → 用户ID映射。CoreProtect内部用数字id代替UUID，减少主表体积。                                                                                                | 缓存玩家信息，**不要手动删**，否则日志全部无法匹配玩家                                                                  |
| co_world         | 世界映射表             | 世界名称→内部数字ID，减少主表重复存储世界名字。                                                                                                                       | 小表                                                                                                                    |
| co_username_log  | 玩家昵称变更日志       | 记录玩家改名历史，旧昵称关联UUID。                                                                                                                                    | 处理玩家改名后查询历史记录                                                                                              |
| co_material_map  | 物品/方块材质映射表    | 方块ID、物品名称的数字编码映射。                                                                                                                                      | 内部字典，几乎只读，很少锁冲突                                                                                          |
| co_entity_map    | 实体类型映射表         | 实体类型（僵尸、村民等）编码字典。                                                                                                                                    | 只读字典表                                                                                                              |
| co_blockdata_map | 方块状态字典表（可选） | 方块附加状态，例如台阶方向、门开关状态。新版CE才有。                                                                                                                  | 只读字典                                                                                                                |

#### #explosion 排查

在 CoreProtect 中排查 `#explosion`（爆炸事件）通常分为三步：**定位 → 确认来源 → 处理**。

**第一步：查看爆炸记录**

```
/co lookup e:#explosion radius:30 time:24h
```

这会列出你周围 30 格范围内、最近 24 小时内所有爆炸事件。日志会显示：

- 爆炸发生的**坐标位置**
- 被破坏的**方块类型**
- 爆炸的**时间**

**第二步：确认爆炸来源**

`#explosion` 本身只记录了"有爆炸发生"，但**不会直接显示是什么引起的**。要判断来源，可以结合以下方法：

- **查看同一时间段、同一位置的其他日志**：

```
/co lookup radius:10 time:1h
```

如果同一位置附近有玩家放置了 TNT 的记录，那大概率是玩家所为。

- **按玩家筛选**：如果你怀疑某个玩家，可以查看他的操作记录：

```
/co lookup user:玩家名 action:block radius:20 time:2h
```

看看他是否在爆炸前放置了 TNT 或其他爆炸物。

- **常见爆炸来源判断**：

| 场景特征                         | 可能来源          |
| -------------------------------- | ----------------- |
| 附近有玩家放置 TNT 的记录        | 玩家使用 TNT      |
| 发生在夜晚、户外、无玩家操作记录 | 苦力怕（Creeper） |
| 发生在下界或末地                 | 床爆炸 / 末影水晶 |
| 大面积破坏、无玩家附近           | 恶魂火球 / 凋灵   |

**第三步：处理爆炸破坏**

确认情况后，根据需要选择操作：

- **恢复爆炸破坏的方块**：

```
/co rollback e:#explosion radius:30 time:2h
```

- **只恢复特定区域的爆炸**：先用检查模式（`/co inspect`）右键点击被破坏区域的方块，确认范围后再执行回滚。

- **如果是玩家恶意使用 TNT 搞破坏**：除了回滚爆炸，还要回滚该玩家放置的方块：

```
/co rollback user:破坏者用户名 radius:30 time:2h
```

**💡 排查小技巧**

- **开启检查模式**：`/co inspect` 后右键点击方块，可以直接看到该方块的完整历史，包括是否被爆炸破坏、之前是什么方块，非常方便精确定位。
- **缩小时间范围**：如果你知道大致破坏时间，把 `time` 参数设小一些（如 `time:10m`），可以减少无关记录，提高排查效率。
- **结合坐标排查**：如果有玩家报告某个位置被炸，可以直接传送到那个坐标附近执行查询，快速定位问题。

### 回滚类

| 命令                                      | 说明                 |
| ----------------------------------------- | -------------------- |
| `/co rollback u:<玩家> t:<时间> r:<半径>` | 回滚某玩家的操作     |
| `/co rollback t:<时间> r:<半径>`          | 回滚某时间段所有操作 |
| `/co restore u:<玩家> t:<时间> r:<半径>`  | 还原（撤销回滚）     |

**时间格式示例**: `t:1h` (1小时前), `t:1d` (1天前), `t:1w` (1周前)

## 网页端查询面板（CoLWI / CommunityCraft rework）

CoreProtect **本体没有官方网页端**，查询只能靠游戏内聊天命令。若想在浏览器里翻记录，可使用第三方 PHP 面板 **CoreProtect Lookup Web Interface（CoLWI）**——它不经过插件，直接读取 CoreProtect 的数据库表。

| 项目       | 信息                                                                                          |
| ---------- | --------------------------------------------------------------------------------------------- |
| 现维护版本 | CoreProtect Lookup Web Interface（CoLWI）— CommunityCraft rework for **CoreProtect 23.2**     |
| 仓库       | https://github.com/CommunityCraftMC/CoreProtect-Lookup-Web                                    |
| 原项目     | https://github.com/chuushi/CoreProtect-Lookup-Web-Interface （Simon OrJ，CoreProtect 2 时代） |
| SpigotMC   | https://www.spigotmc.org/resources/135396                                                     |
| 许可/形态  | 纯 PHP 网页，与服务器本体的插件目录无关                                                       |

### 能查什么

- 游戏内 `/co lookup` 的既有筛选：**动作类型 / 玩家名 / 方块名 / 时间范围**
- 网页端独有：**按坐标 + 世界查询**、每页超过 4 条结果、过滤已回滚数据、关键词搜索
- v23.2 新增记录：**告示牌**（文字行、朝向、涂蜡、颜色）、**物品 / 容器**交易、`+session` / `-session` / `+item` / `-item`
- 元数据展示：`block.meta`、`block.blockdata`、`container.metadata`、`item.data`，以及 Java 序列化 Bukkit 物品信息的**纯 PHP 解码摘要**（显示名、Lore、附魔、物品旗标、盔甲纹样）——**不需要 Java 运行环境**

### 前置条件

- 网页服务器 + **PHP 8.4**（作者开发验证版本；原版 CoLWI 为 PHP 5.6+）
- PHP 扩展：**PDO**，并按数据库类型启用 **pdo_mysql** 或 **pdo_sqlite**
- CoreProtect **v23.2 及以上**的数据库
- **用 SQLite 时，网页服务器必须与 MC 服务器同机**（SQLite 文件不支持远程实时读取）；要分离部署必须先切 MySQL（见上文「MySQL 数据库配置」）

### 部署步骤

**1. 下载**

```bash
# 方式一（便于后续更新）
git clone https://github.com/CommunityCraftMC/CoreProtect-Lookup-Web.git

# 方式二：下载 Release 的 .zip 后解压到网页目录
```

**2. 建只读数据库账号（强烈建议）**

网页端只做查询，不需要写权限，用最小权限账号可避免误写日志库：

```sql
CREATE USER 'corelook'@'网页服务器IP' IDENTIFIED BY '另一个强密码';
GRANT SELECT ON coreprotect.* TO 'corelook'@'网页服务器IP';
FLUSH PRIVILEGES;
```

**3. 编辑 `config.php`**

MySQL（推荐，网页与游戏服务器可分离）：

```php
'server' => [
    'type'        => 'mysql',
    'host'        => 'localhost:3306',
    'database'    => 'coreprotect',
    'username'    => 'corelook',
    'password'    => '另一个强密码',
    'flags'       => '',
    'prefix'      => 'co_',        // 需与 config.yml 的 table-prefix 一致
    'preBlockName'=> true,
    'mapLink'     => '',
],
```

SQLite（同机）：

```php
'server' => [
    'type'        => 'sqlite',
    'path'        => '/path/to/plugins/CoreProtect/database.db',
    'prefix'      => 'co_',
    'preBlockName'=> true,
    'mapLink'     => '',
],
```

查询安全上限（设在 `config.php` 的 `form` 段，防止大范围查询拖垮数据库）：

```php
'form' => [
    'count' => 30,                    // 默认每页条数
    'moreCount' => 10,                // “加载更多”条数
    'max' => 300,                     // 单次查询上限
    'maxCoordinateVolume' => 5000000, // 坐标选区最大体积，0 为不限制
    'timeoutSeconds' => 20,           // PHP / 语句超时，0 为不限制
],
```

**4. 启动并访问**

```bash
php -v                  # 确认版本
php -m                  # 确认 pdo_mysql / pdo_sqlite 已加载
php -S 127.0.0.1:18080  # 本地快速验证（生产请用 Nginx/Apache + php-fpm）
# 浏览器打开 http://127.0.0.1:18080/index.php
```

**5. 更新**

- git 安装：`git stash` → `git pull` → `git stash pop`（若 `config.php` 冲突，手动合并后 `git add config.php`）
- zip 安装：重新下载并手动迁移旧的 `config.php`

### 一键部署脚本（Ubuntu / Debian + Nginx）

手动步骤较多，仓库内已备好脚本与完整部署文档：

- 📄 [网页端面板部署/部署步骤.md](网页端面板部署/部署步骤.md) —— 环境要求、准备清单、手动部署（非 apt 系 / SELinux）、安全加固、验收标准、故障排查、更新卸载
- 🔧 `网页端面板部署/deploy.sh` —— 一键脚本
- ⚙️ `网页端面板部署/nginx-colwi.conf.example` —— Nginx 站点模板

`deploy.sh` 会自动完成**装依赖 → 校验 PHP 与 PDO 扩展 → 收权限 → 改写 `config.php` 的 server 段 → 生成 Nginx 站点 → 创建 Basic Auth → 放行防火墙 → 自检**。

```bash
# 1. 上传脚本到服务器（与程序目录同级即可）
# 2. 改脚本顶部「必填配置区」：WEB_ROOT / LISTEN_PORT / DB_* / ACCESS_MODE 等
sudo bash deploy.sh            # 部署（可重复执行）
sudo bash deploy.sh verify     # 只做健康检查，不改任何东西
sudo bash deploy.sh conf       # 只重新生成 Nginx 站点配置
```

要点：

- 脚本**只改写 `config.php` 中 `server` 段已存在的键**，找不到就跳过并提示，不会破坏文件结构；改写前自动备份为 `config.php.bak.<时间戳>`。
- `ACCESS_MODE` 三选一：`auth`（账号密码）/ `ip`（IP 白名单）/ `both`。因为 rework 已移除内置认证，**务必保留其中至少一种**。
- Nginx 配置里已强制 `deny` 掉 `config.php`、`.git`、`tests`、`.env`，自检会验证 `config.php` 返回 403/404，防止数据库凭据泄露。
- 若脚本在 Windows 上编辑过，先在服务器执行 `sed -i 's/\r$//' deploy.sh`（或 `dos2unix deploy.sh`）再运行。
- 非 apt 系（CentOS / RHEL）请照 `网页端面板部署/nginx-colwi.conf.example` 手动配置，思路一致。

### ⚠️ 安全要求（必读）

- 该 rework **已移除内置登录认证**，任何人拿到 URL 就能看到全服玩家的坐标、破坏记录、聊天与命令历史——属于高敏感运营数据。
- 必须加一层外部访问控制：**反向代理 Basic Auth / SSO、IP 白名单、仅 VPN 或内网访问**，不要直接暴露公网端口。
- 数据库账号务必只给 `SELECT`。
- 页面需访问完整玩家行为记录，**不建议开放给普通玩家**，仅服主 / 管理组可用。

### 版本兼容提醒

- rework 明确面向 **CoreProtect v23.2** 的数据库结构。本文档服务器用的是 **v24.0**，v24 新增/调整的记录类型可能出现「表或列不存在」——好在面板带 **schema 能力检测**，缺失时会返回清晰的 JSON 不支持提示而不是裸 SQL 报错。
- 面板依赖的默认表：`co_block` `co_container` `co_item` `co_sign` `co_chat` `co_command` `co_session` `co_username_log` `co_user` `co_world` `co_material_map` `co_entity_map`（可选 `co_blockdata_map`）。
- 若 v24.0 下部分新记录查不出来，属于上游适配滞后，可先用游戏内 `/co lookup` 兜底。

### 网页端 vs 游戏内命令

| 维度          | 网页面板                                | 游戏内 `/co lookup`                |
| ------------- | --------------------------------------- | ---------------------------------- |
| 需要进游戏    | 否，服主不在电脑前也能查                | 是                                 |
| 结果展示      | 分页、超 4 条、可过滤已回滚、可看元数据 | 聊天框翻页，每页 4 条              |
| 坐标/世界筛选 | 支持，可视化选区                        | 依赖 `r:` 半径与所在位置           |
| 回滚 / 还原   | **不支持**（只读）                      | 支持 `/co rollback`、`/co restore` |
| 维护成本      | 需额外 PHP 环境 + 安全加固              | 零                                 |

> 结论：网页端用于**审计与排查**，真正动手恢复仍需回到游戏内执行 `/co rollback`。偶尔自查用 `/co lookup u:玩家 t:1d r:100` 就够，网页端的价值在于远程、批量、可翻页的历史追溯。

### 常见故障

| 现象                                  | 原因 / 处理                                                          |
| ------------------------------------- | -------------------------------------------------------------------- |
| 页面空白或 500                        | `php -l index.php`、`php -l lookup.php` 查语法；确认 PHP 版本与扩展  |
| `could not find driver`               | 未启用 `pdo_mysql` / `pdo_sqlite`，在 php.ini 打开对应扩展并重启     |
| 连接被拒 / 超时                       | MySQL 账号 host 不是网页服务器 IP；防火墙未放行 3306                 |
| 查询报 unsupported schema             | 数据库缺少 v23.2 新表/列（多见于 v24.0 新记录），属已知兼容滞后      |
| `Query too large` / `Query timed out` | 调小选区，或调 `form.max` / `maxCoordinateVolume` / `timeoutSeconds` |
| 数据不更新（SQLite）                  | 网页与 MC 服务器不同机，或指向了旧的 `database.db` 副本              |
