TAB 是 Minecraft 插件服中最经典、最广泛使用的 TAB 列表自定义插件，支持 1.7 ~ 1.21+ 全版本，几乎所有服务器都会用到它。以下是详细的使用教程。

---

### 🚀 安装

1. **下载插件**
   - 前往 SpigotMC 官方页面下载最新版本的 `TAB.jar` 文件
   - 官方页面：https://www.spigotmc.org/resources/tab-1-7-1-21.57806/

2. **放入插件目录**
   - 将 `TAB.jar` 放入服务器的 `plugins` 文件夹中

3. **重启服务器**
   - 启动或重启服务器，TAB 会自动在 `plugins/TAB/` 目录下生成配置文件

---

### ⚙️ 核心配置文件

TAB 的主要配置文件位于 `plugins/TAB/` 目录下，包含以下几个关键文件：

| 文件             | 用途                                         |
| ---------------- | -------------------------------------------- |
| `config.yml`     | 主配置文件，控制 TAB 列表的整体布局和内容    |
| `animations.yml` | 动画配置，定义文字渐变、彩虹、呼吸等动态效果 |
| `groups.yml`     | 分组配置，定义不同权限组的显示样式           |
| `users.yml`      | 单玩家配置，为特定玩家设置独立的显示样式     |

---

### 📋 基本布局配置

打开 `config.yml`，TAB 列表分为四个区域：

```yaml
header: # TAB列表顶部（头部）
  - '&6&l我的服务器'
  - '&7欢迎来到 %player%！'

footer: # TAB列表底部（尾部）
  - '&7在线人数: &a%online%&7/&6%max%'
  - '&7TPS: %tps%'
```

你可以使用颜色代码（`&0` ~ `&f`）和格式代码（`&l` 加粗、`&o` 斜体、`&n` 下划线等）来美化文字。

---

### 👤 玩家显示配置

在 `config.yml` 中，你可以定义每个玩家在 TAB 列表中的显示格式：

```yaml
tablist-name-formatting:
  # 玩家名称的显示格式
  # 支持前缀、后缀、排序权重等
  default:
    tabprefix: '&7[&f玩家&7] ' # 名称前的前缀
    tabsuffix: ' &7| %ping%ms' # 名称后的后缀
    tagprefix: '&7[&f玩家&7] ' # 头顶名牌前缀
    tagsuffix: '' # 头顶名牌后缀
```

---

### 🎨 动画效果

打开 `animations.yml`，可以定义各种动态文字效果：

```yaml
# 彩虹效果
rainbow:
  type: 'RAINBOW'
  speed: 5

# 渐变效果
gradient:
  type: 'GRADIENT'
  speed: 3
  colors:
    - '#FF0000'
    - '#00FF00'
    - '#0000FF'

# 呼吸变色效果
breathing:
  type: 'BREATHING'
  speed: 10
  colors:
    - '&a'
    - '&2'
```

在 `config.yml` 中引用动画：

```yaml
header:
  - '%animation:rainbow%我的服务器%animation:rainbow%'
```

> **提示**：1.16+ 版本支持 HEX 颜色，格式为 `&#RRGGBB`（如 `&#FF5500`）。

---

### 📊 分组系统

TAB 支持根据权限组对玩家进行分组显示。在 `groups.yml` 中配置：

```yaml
owner:
  tabprefix: '&4[&4服主&4] '
  tabsuffix: ' &4◆'

Admin:
  tabprefix: '&4[&c管理&4] '
  tabsuffix: ' &4*'
  header: '&4管理员列表'

default:
  tabprefix: '&7[&f玩家&7] '
  tabsuffix: ''
```

分组与权限插件（如 LuckPerms）联动，自动识别玩家所属的权限组并应用对应样式。

---

### 🔌 PlaceholderAPI 集成

TAB 完美支持 PlaceholderAPI，可以在 TAB 列表中使用任何 PAPI 占位符：

```yaml
footer:
  - '&7在线时长: %plan_player_time_total%'
  - '&7余额: $%vault_eco_balance%'
  - '&7等级: %player_level%'
```

**使用步骤**：

1. 安装 PlaceholderAPI 插件
2. 安装所需的数据源插件（如 Plan、Vault 等）
3. 下载对应的 PAPI 扩展：`/papi ecloud download <扩展名>`
4. 在 TAB 配置中使用 `%占位符%` 格式调用

> `%vault_eco_balance%` 不显示时：`/papi ecloud download Vault` → `/papi reload`。

### 🛠 常用指令

| 指令                                  | 说明                           |
| ------------------------------------- | ------------------------------ |
| `/tab reload`                         | 重新加载所有配置文件           |
| `/tab debug`                          | 输出调试信息，帮助排查配置问题 |
| `/tab group <组名> tabprefix <前缀>`  | 为指定组设置 TAB 前缀          |
| `/tab player <玩家> tabprefix <前缀>` | 为指定玩家设置 TAB 前缀        |
| `/tab parse <文本>`                   | 测试占位符解析结果             |

---

### 💡 实用技巧

#### 显示在线时长

结合在线时长统计插件（如 PlaytimeStats），在 TAB 中显示每位玩家的在线时长：

```yaml
tabsuffix: ' &7| %playertime_time%'
```

#### 动态排序

TAB 支持按条件对玩家排序，例如按权限组权重、Ping 值等：

```yaml
sorting:
  type: "GROUPS"    # 按权限组排序
  # 或
  type: "PLACEHOLDERS"
  placeholder: "%ping%"
  ascending: true   # 升序排列
```

#### 性能优化

TAB 插件本身性能开销极低，即使在玩家数量众多的服务器上也能保持良好运行效率。如果服务器人数特别多，可以适当降低占位符的刷新频率来减少性能消耗。

---

### 📌 推荐搭配

| 搭配插件           | 作用                               |
| ------------------ | ---------------------------------- |
| **PlaceholderAPI** | 提供数据占位符，是 TAB 的核心前置  |
| **LuckPerms**      | 权限管理，与 TAB 分组系统联动      |
| **Plan**           | 提供在线时长、击杀等统计数据占位符 |
| **Vault**          | 经济系统桥接，显示余额等信息       |


---

### 📌 侧边公告栏（Scoreboard）

TAB 的右侧面板（scoreboard）就是本服的**公告栏**，玩家可以**个人手动取消**。

**启用并配置**（`plugins/TAB/config.yml`）：

```yaml
scoreboard:
  enabled: true
  default-scoreboard: main
  toggle-command: /sb # 玩家个人开关命令
  remember-toggle-choice: false
  hidden-by-default: false # 默认显示
  scoreboards:
    main:
      title: <gradient:#FFD54A:#FF8C00>&l南瓜生存服</gradient>
      lines:
        - <#555555>&m                    </#555555>
        - '&7▸ 玩家 &f%player%'
        - '&7▸ 在线 &e%online%&7人'
        - '&7▸ 世界 &a%world%'
        - ''
        - '&7▸ 余额 &e%vault_eco_balance% 币'
        - '&7▸ 在线时长 &b%plan_player_current_session_length%'
        - '&7▸ 总时长 &b%plan_player_time_total%'
        - '&7▸ 实体: &a%clearlag_entity_total%'
        - '&7▸ RAM: &a%clearlag_ram%'
        - '&7▸ TPS: &a%tps%'
        - '&7▸ 延迟 &a%ping%ms'
        - '&7▸ 时间 &a%date%'
        - ''
        - <gradient:#FFD54A:#FF8C00>&l欢迎回家</gradient>
        - <#555555>&m                    </#555555>
        - '&8&o输入 /sb 隐藏本栏'
```

**占位符**：

| 占位符                      | 来源                      | 说明                           |
| --------------------------- | ------------------------- | ------------------------------ |
| `%online%` / `%maxplayers%` | TAB 内置                  | 在线人数 / 最大人数            |
| `%tps%`                     | TAB 内置                  | 服务器 TPS（满 20.0）          |
| `%clearlag_entity_total%`   | ClearLag++ 扩展           | 全服已加载实体总数             |
| `%clearlag_ram%`            | ClearLag++ 扩展           | 内存使用情况                   |

**为什么不用 PAPI 官方 Server 扩展（`%server_total_entities%`）？** 该扩展在请求时**同步遍历 chunk 取实体**，而 TAB 在**异步线程**刷新占位符，Leaf/Paper 的 AsyncCatcher 会拦截并**周期性刷屏报错**。

**ClearLag++ 扩展**（`plugins/PlaceholderAPI/expansions/ClearLag*.jar`）：ClearLag 自带的 PAPI 扩展，异步安全、零报错。部署：`/papi ecloud download ClearLag` → `/papi reload` → `/tab reload`。

**玩家个人取消**：输入 `/sb` 或 `/tab scoreboard off` 隐藏，再输一次恢复。权限 `tab.scoreboard.toggle`（默认开放，见权限速查）。


**性能**：TPS / 实体数占位符走 TAB 内置刷新周期，开销可忽略；侧边栏避免堆太多 PAPI 占位符即可。

---
