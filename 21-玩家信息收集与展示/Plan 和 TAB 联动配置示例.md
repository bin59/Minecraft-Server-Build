### Plan × TAB 联动配置示例

Plan 和 TAB 的联动原理很简单：**Plan 负责收集数据，TAB 通过 PlaceholderAPI 调用 Plan 的占位符，将数据展示在 TAB 列表中。**

---

### 前置准备

确保以下插件已安装并正常运行：

| 插件               | 作用                               |
| ------------------ | ---------------------------------- |
| **Plan**           | 收集玩家在线时长、击杀、死亡等数据 |
| **TAB**            | 自定义 TAB 列表显示                |
| **PlaceholderAPI** | 占位符桥梁，连接 Plan 和 TAB       |

安装完成后，执行以下命令确认 Plan 的 PAPI 扩展已注册：

```
/papi ecloud download Plan
/papi reload
```

---

### TAB config.yml 联动配置

以下是将 Plan 数据集成到 TAB 列表中的完整配置示例：

```yaml
# ================================ #
#   TAB × Plan 联动配置示例        #
# ================================ #

# -------- 头部（Header）--------
header:
  - ''
  - '&6&l✦ 我的服务器 ✦'
  - '&7欢迎回来, &f%player%&7!'
  - ''

# -------- 底部（Footer）--------
# 这里大量使用 Plan 提供的占位符
footer:
  - ''
  - '&7在线人数: &a%plan_server_players_online%&7/&6%max% &8| &7TPS: %plan_server_tps%'
  - '&7今日新玩家: &b%plan_server_new_players_day% &8| &7今日活跃: &e%plan_server_unique_players_day%'
  - '&7你的在线时长: &d%plan_player_times_online%'
  - ''

# -------- 玩家名称格式 --------
tablist-name-formatting:
  # 默认玩家组
  default:
    tabprefix: '&7[&f玩家&7] '
    # 在玩家名字后面显示 Plan 统计的在线时长
    tabsuffix: ' &8| &7在线: %plan_player_times_online%'
    tagprefix: '&7[&f玩家&7] '
    tagsuffix: ''

  # VIP 组
  vip:
    tabprefix: '&6[&eVIP&6] '
    tabsuffix: ' &8| &7在线: %plan_player_times_online%'
    tagprefix: '&6[&eVIP&6] '
    tagsuffix: ''

  # 管理员组
  admin:
    tabprefix: '&4[&c管理员&4] '
    tabsuffix: ' &4✦'
    tagprefix: '&4[&c管理员&4] '
    tagsuffix: ''

# -------- 排序设置 --------
# 按 Plan 的活动指数排序，越活跃的玩家排越前面
sorting:
  type: 'PLACEHOLDERS'
  placeholder: '%plan_player_activity_index%'
  ascending: false

# -------- 刷新间隔 --------
refresh-interval:
  header: 20
  footer: 20
  tablist: 40

# -------- Ping 显示 --------
ping:
  enabled: true
```

---

### 可用占位符速查表

以下是 Plan 提供的所有可在 TAB 中使用的占位符：

#### 玩家级占位符

| 占位符                           | 说明             | 示例输出     |
| -------------------------------- | ---------------- | ------------ |
| `%plan_player_times_online%`     | 玩家总在线时长   | `12h 34m`    |
| `%plan_player_kills_player%`     | PVP 击杀数       | `42`         |
| `%plan_player_deaths%`           | 死亡次数         | `128`        |
| `%plan_player_activity_index%`   | 活动指数（0~5）  | `3`          |
| `%plan_player_last_seen%`        | 上次在线时间     | `2h ago`     |
| `%plan_player_registered%`       | 注册日期         | `2025-03-15` |
| `%plan_player_login_times%`      | 总登录次数       | `89`         |
| `%plan_player_playtime_session%` | 当前会话在线时长 | `1h 23m`     |

#### 服务器级占位符

| 占位符                             | 说明           | 示例输出 |
| ---------------------------------- | -------------- | -------- |
| `%plan_server_players_online%`     | 当前在线人数   | `32`     |
| `%plan_server_unique_players_day%` | 今日独立玩家数 | `56`     |
| `%plan_server_new_players_day%`    | 今日新玩家数   | `3`      |
| `%plan_server_tps%`                | 服务器 TPS     | `19.8`   |
| `%plan_server_cpu%`                | CPU 占用率     | `45%`    |
| `%plan_server_ram%`                | 内存占用       | `2.1GB`  |

---

### 进阶：按活动指数显示不同颜色

你可以根据 Plan 的活动指数，为不同活跃度的玩家显示不同颜色的前缀。这需要配合 TAB 的分组系统：

```yaml
# groups.yml 中的配置
# 活跃玩家（活动指数 >= 4）
active:
  tabprefix: '&a[&2活跃&a] '
  tabsuffix: ' &8| &7在线: %plan_player_times_online%'
  tagprefix: '&a[&2活跃&a] '
  tagsuffix: ''
  sorting: 80
  permission: 'plan.activity.4'

# 普通玩家（活动指数 2~3）
regular:
  tabprefix: '&7[&f常客&7] '
  tabsuffix: ' &8| &7在线: %plan_player_times_online%'
  tagprefix: '&7[&f常客&7] '
  tagsuffix: ''
  sorting: 40
  permission: 'plan.activity.2'

# 默认（新玩家）
default:
  tabprefix: '&8[&7新手&8] '
  tabsuffix: ' &8| &7在线: %plan_player_times_online%'
  tagprefix: '&8[&7新手&8] '
  tagsuffix: ''
  sorting: 10
  permission: 'group.default'
```

> **提示**：活动指数的权限节点需要在 Plan 的配置中开启自动分配功能，或者通过 LuckPerms 手动根据玩家活跃度分配。

---

### 部署验证步骤

1. 确保 Plan、TAB、PlaceholderAPI 三个插件都已安装
2. 执行 `/papi ecloud download Plan` 安装 Plan 的 PAPI 扩展
3. 将上述配置写入 `plugins/TAB/config.yml`
4. 执行 `/tab reload` 重载 TAB 配置
5. 在游戏内按 Tab 键查看效果
6. 如果占位符显示为原始文本（如 `%plan_player_times_online%`），说明 PAPI 扩展未正确加载，执行 `/papi list` 检查是否包含 Plan

---

### 常见问题

**Q: 占位符显示为原始文本怎么办？**
执行 `/papi list` 查看已注册的扩展列表，确认 Plan 在列表中。如果不在，执行 `/papi ecloud download Plan` 后 `/papi reload`。

**Q: 数据显示为 0？**
Plan 需要一定时间收集数据。如果是新安装的 Plan，玩家需要先登录一次，数据才会开始记录。

**Q: 如何测试占位符是否正常？**
在游戏内执行 `/papi parse me %plan_player_times_online%`，如果返回具体时长则说明配置正确。

---
