这份教程专为你的服务器定制，重点说明 Vault 作为「经济 / 权限 API 桥梁」的作用，并记录它与各玩法条线插件的联动方式。

# 经济系统 — Vault 部署与多插件联动

Vault 是连接本服各玩法插件与经济、权限核心的 API 桥，本文档记录它与 EssentialsX、LuckPerms 的安装顺序，以及 Residence 买卖、死亡收费、QuickMenu 菜单、SimplePets 宠物等插件如何经 Vault 统一调用同一套货币。文档同时给出了对照运行服实际配置的校准结论和需要管理员决策的事项。面向负责服务器经济与权限对接的管理员。

> **Vault 是什么**：Vault 本身不存钱、也不管权限，它是中间件——把 Residence、宠物、菜单、死亡信息等一群「功能插件」统一接到同一套**经济核心（EssentialsX）**和**权限核心（LuckPerms）**上。装好 Vault，这些插件才「找得到钱」。

> **⚠️ 运行服实际配置校准（2026-09-16，重点）**：
> 打开运行服 `plugins/Vault/config.yml` 发现，本服装入的 Vault 并非「纯 API 桥接版」，而是 **Vault 2.0 自带内置经济**（config 第 2–5 行标注 `Vault 2.0 — internal economy`，`plugin_version: 1.6.2`）。其实际行为与本文「Vault 只做桥、钱存在 EssentialsX」的描述有出入，以下为**实际值**：
>
> | 配置项 | 实际值（plugins/Vault/config.yml） |
> | --- | --- |
> | 经济定位 | Vault 自带内部经济，账目存 `plugins/Vault/balances.yml`；`import.essentials.enabled: false`（第 45 行，未从 EssentialsX 导入） |
> | 货币符号 | `currency.symbol: $`（第 21 行），位置 suffix —— **不是** EssentialsX 的 ¥ |
> | 语言 | `language: en`（第 7 行） |
> | 借贷系统 | `loans.enabled: true`（第 78 行），账目 `plugins/Vault/loans.yml` |
> | 存储 | 文件存储（`storage.use_mysql: false`，第 27 行） |
> | 自带指令 | `/balance`、`/pay`、`/eco give|take`、`/vault reload`、`/vault loan`（见 config 注释第 113–130 行） |
>
> **需用户决策**：(1) 究竟以 Vault 内置经济为准，还是以 EssentialsX 经济为准——二者可能存在 `/bal` 与 `/balance` 双经济并存；(2) 货币符号实际为 `$`，与 EssentialsX 已改的 `¥` 不一致；(3) 本文 §4.1 让 Residence `EnableEconomy: true`，但运行服 Residence 当前为 `false`。在确认经济核心前，请勿按本文「下载 Vault Updated / 接 EssentialsX」一节继续操作。

--- | --- |
> | 经济定位 | Vault 自带内部经济，账目存 plugins/Vault/balances.yml；import.essentials.enabled: false（第 45 行，未从 EssentialsX 导入） |
> | 货币符号 | currency.symbol: $（第 21 行），位置 suffix —— **不是** EssentialsX 的 ¥ |
> | 语言 | language: en（第 7 行） |
> | 借贷系统 | loans.enabled: true（第 78 行），账目 plugins/Vault/loans.yml |
> | 存储 | 文件存储（storage.use_mysql: false，第 27 行） |
> | 自带指令 | /balance、/pay、/eco give|take、/vault reload、/vault loan（见 config 注释第 113–130 行） |
>
> **需用户决策**：(1) 究竟以 Vault 内置经济为准，还是以 EssentialsX 经济为准——二者可能存在 /bal 与 /balance 双经济并存；(2) 货币符号实际为 $，与 EssentialsX 已改的 ¥ 不一致；(3) 本文 §4.1 让 Residence EnableEconomy: true，但运行服 Residence 当前为 alse。在确认经济核心前，请勿按本文「下载 Vault Updated / 接 EssentialsX」一节继续操作。


---

## 一、核心原理与安装顺序

你的服务器现状是：有「门禁系统」（LuckPerms，管权限）和一堆「功能插件」（Residence、宠物、菜单…），但缺了「银行」（EssentialsX，实际存钱）和「通讯线路」（Vault，把大家连到银行）。

**依赖关系图：**

```text
各功能插件 (Residence / 自定义死亡信息 / QuickMenu / 宠物…)
   ⬇️ 调用经济 API
Vault (中间件/桥梁)
   ⬇️ 提供经济数据 / 权限
EssentialsX (经济核心) + LuckPerms (权限核心)
```

**⚠️ 关键安装顺序：**

1.  **先装 EssentialsX**（建立银行，提供 `/bal` `/pay` `/eco`）
2.  **再装 Vault**（铺设线路，桥接 EssentialsX 与 LuckPerms）
3.  **最后重启**（其余插件会自动检测到线路通了）

---

## 二、 EssentialsX 安装与配置（经济核心）

#### 1. 下载与安装

- **EssentialsX (必须)**: [SpigotMC 下载](https://www.spigotmc.org/resources/essentialsx.9089/)
- **EssentialsXSpawn (推荐)**: [SpigotMC 下载](https://www.spigotmc.org/resources/essentialsxspawn.20939/) (提供 `/spawn` 和出生点设置功能)

**操作：** 将下载的 `.jar` 文件放入 `plugins` 文件夹，**暂时不要重启**。

#### 2. 基础配置 (`config.yml`)

首次启动后会生成配置文件。停止服务器，编辑 `plugins/Essentials/config.yml`：

```yaml
# --- 经济系统设置 ---
economy:
  # 必须设为 true，依赖经济的插件才能读取到钱
  enabled: true

  # 货币符号，建议与 Residence 保持一致
  currency-symbol: '¥'

  # 货币名称（单数/复数），Residence 会读取这个
  currency-name: '金币'

# --- 新手初始资金 ---
# 新玩家进服送多少钱
starting-balance: 0

# --- 其他常用设置 ---
# 死亡是否掉落经验
keep-levels-on-death: false

# 是否允许玩家转账
pay-exempt: []
```

#### 3. 验证经济系统

重启服务器后，进入游戏输入：

- `/bal`：查看余额（应显示 `¥0.00`）
- `/pay <玩家名> 10`：测试转账

---

## 三、 Vault 安装与配置（关键桥梁）

#### 1. 版本选择（针对 1.21.11）

由于你的服务端是 **1.21.11**，原版 Vault 可能显示“过时”，但通常可用。为了最佳兼容性，推荐使用更新版：

- **推荐版本 (Vault Updated)**: [GitHub 下载 (Roydogman/Vault-Updated)](https://github.com/Roydogman/Vault-Updated)
  - _优势：专为 1.20+ 编译，完美支持 LuckPerms 和现代服务端。_
- **原版 Vault**: [SpigotMC 下载](https://www.spigotmc.org/resources/vault.34315/)
  - _注意：如果安装后报错，请换用上面的推荐版本。_

#### 2. 安装步骤

1.  将 `Vault.jar` 放入 `plugins` 文件夹。
2.  **重启服务器**。

#### 3. 验证是否成功 (关键步骤)

查看控制台启动日志，必须看到以下两行才算成功：

```text
[Vault] [Economy] Essentials Economy: Found
[Vault] [Permission] SuperPermissions: Loaded as LuckPermsSuperPermissionBridge
```

- 如果看到 `Essentials Economy: Found`，说明 **依赖经济的插件报错将消失**。
- 如果看到 `LuckPerms...`，说明权限系统已连接。

---

## 四、与其他插件联动

### 4.1 Residence（领地买卖 / 租赁）

Residence 通过 Vault 调用经济，实现领地出售、租赁、设价。

编辑 `plugins/Residence/config.yml`：

```yaml
Economy:
  # 必须开启，否则无法买卖领地
  EnableEconomy: true

  # 货币名称，建议与 EssentialsX 保持一致
  CurrencyName: '金币'
```

常用经济指令：

| 指令 | 说明 |
| :--- | :--- |
| `/res price <领地名> <金额>` | 设置领地售价 |
| `/res buy <领地名>` | 购买领地（扣款） |
| `/res rent <领地名>` | 租赁领地（扣租金） |
| `/res sell <领地名>` | 出售领地（退款） |

LuckPerms 权限：`residence.buy` / `residence.rent` / `residence.sell` / `residence.price` 等。

### 4.2 LuckPerms（权限桥接）

Vault 把权限请求统一转发给 LuckPerms。各经济指令权限需手动授予：

```bash
# 基础经济指令
lp group default permission set essentials.balance
lp group default permission set essentials.pay
lp group default permission set essentials.money

# Residence 经济相关权限 (买卖/租赁)
lp group default permission set residence.rent true
lp group default permission set residence.unrent true
lp group default permission set residence.sell true
lp group default permission set residence.buy true
lp group default permission set residence.price true

# 管理员组无限金钱 (可选)
lp group admin permission set essentials.money.unlimited true
```

### 4.3 自定义死亡信息 CustomDeathMessages（Vault 收费）

CDM 可选接入 Vault，每次展示死亡消息向玩家扣费；指定 LuckPerms 组（如 `vip`）免单。

`plugins/CustomDeathMessages/config.yml`：

```yaml
# 每次展示死亡消息收取的费用（需 Vault + 经济插件，0 为免费）
cost-per-death-message: 5
# 免收费用的权限组（LuckPerms 组名）
exempt-groups-from-cost:
  - "admin"
  - "vip"
```

前置：必须同时装 **Vault + 经济插件（EssentialsX）**，且 `cost-per-death-message > 0`。效果：普通玩家每次死亡播报扣 5 金币，admin / vip 免单。

### 4.4 快捷菜单 QuickMenu（经济中心）

QuickMenu 的 `economy` 菜单直接对接 **EssentialsX + Vault**，提供：余额查询、财富榜、转账、卖物品、估价。玩家在菜单里看到的「经济中心」就是走 Vault 经济 API。无需额外配置，只要 Vault + EssentialsX 在线即可。

### 4.5 余额展示联动（PlaceholderAPI）

Vault 的余额可通过 **PlaceholderAPI** 的 Vault 扩展暴露为占位符，被其他插件读取展示：

- `%vault_eco_balance%` / `%vault_eco_balance_formatted%`：玩家当前余额

已联动的展示位：

| 插件 | 用途 |
| :--- | :--- |
| **TAB** | 玩家列表显示余额 |
| **DecentHolograms** | 全息显示余额 / 财富榜（`%top_money%` 等 PAPI 扩展） |
| **Plan** | 玩家数据分析面板统计 |
| **自定义死亡信息** | 死亡消息可嵌入余额占位符 |

前置：安装 PlaceholderAPI + Vault 扩展（`/papi ecloud download Vault` 后 `/papi reload`）。

### 4.6 跨服经济（Velocity 多服）

本服使用 Velocity 代理。注意：**EssentialsX 经济是单服数据库**，跨子服会各自为政。

多服共享经济的做法（二选一）：

1.  **换用支持共享存储的经济实现**：CMI 经济 / CoinsEngine / 带 SQL 存储的 Vault 经济插件，多个子服连同一数据库。
2.  **经济主服模式**：选一个子服当「经济主服」，其余子服通过菜单 / 命令网关调用主服 API。

若只做单服生存（一个后端），保持 EssentialsX + Vault 即可，无需额外处理。

### 4.7 宠物系统 SimplePets（用 Vault Addon 即可统一货币）

**可以，而且推荐这么做。** SimplePets 的「花钱买宠物」通过**经济 Addon** 实现，官方提供多种经济 Addon，**其中就有 Vault Addon**——它 Hook 进 Vault，宠物直接用服务器主货币（即 EssentialsX 余额）购买，与 `/bal`、Residence、死亡收费等是**同一套账**。

| 经济 Addon | 货币来源 | 与 Vault 余额关系 |
| :--- | :--- | :--- |
| **Vault Addon（推荐，本服适用）** | Vault → EssentialsX | ✅ 同一套账 |
| GemsEconomy Addon | GemsEconomy | ❌ 两套账（除非把 GemsEconomy 设为 Vault 经济实现） |
| PlayerPoints / TokenManager / Treasury / ItemEconomy | 各自点券 / 物品 | 独立 |

**本服推荐方案**：装 **SimplePets Vault Addon**（不要装 GemsEconomy Addon），宠物花费直接从 EssentialsX 余额扣，全服货币统一。

安装 Vault Addon：
- 游戏内：`/pet addon install Vault`（自动下载并启用）
- 或手动：把 Addon 的 jar 放进 `plugins/SimplePets/Addons/`，重启服务端

配置：在 Addon 配置里给宠物标价；玩家用 `/pet purchased` 查看已购；免单权限 `Pet.economy.bypass` 让赞助组（vip / vip+）免费解锁。

> 结论：**宠物可以用 Vault + EssentialsX**——装 Vault Addon 即可，全服货币统一，无需引入第二套货币。

---

## 五、功能测试

在游戏内进行以下测试，确保一切正常：

| 测试项目 | 指令 | 预期结果 |
| :--- | :--- | :--- |
| **查看余额** | `/bal` | 显示 `¥0.00` (或对应金额) |
| **设置领地价格** | `/res price 领地名 1000` | 提示"领地价格已设置为 1000" |
| **购买领地** | `/res buy 领地名` | 扣除 1000 金币，领地归属变更 |
| **租赁领地** | `/res rent 领地名` | 扣除租金，获得租期 |
| **死亡收费** | 触发死亡消息 | 普通玩家扣 5 金币（vip 免单） |
| **菜单经济** | QuickMenu `economy` 菜单 | 显示余额 / 财富榜 / 转账 |

---

## 六、常见问题排查

1.  **Residence 依然报错 `Could not determine economy`**
    - **原因**：Vault 没找到 EssentialsX。
    - **解决**：检查 `plugins` 文件夹里是否有 `EssentialsX.jar`，确保先启动了 EssentialsX，再启动 Vault。查看日志是否有 `Essentials Economy: Found`。

2.  **提示 `No economy system found`**
    - **原因**：EssentialsX 的经济模块被关掉了。
    - **解决**：检查 `plugins/Essentials/config.yml` 中的 `economy: enabled: true`。

3.  **自定义死亡信息不扣钱**
    - **原因**：Vault / 经济插件未装，或费用为 0，或玩家在免单组。
    - **解决**：确保 Vault + EssentialsX 都在，且 `cost-per-death-message > 0`；`exempt-groups-from-cost` 列出的组不扣费。

4.  **跨服余额不一致**
    - **原因**：EssentialsX 经济是单服数据库。
    - **解决**：按 §4.6 换用共享存储的经济实现或经济主服模式。

5.  **宠物花钱但 `/bal` 不变**
    - **原因**：SimplePets 走 GemsEconomy（见 §4.7），与 Vault 余额是两套账。
    - **解决**：统一货币需让 GemsEconomy 作为 Vault 经济实现，或用 GemsEconomy 作经济核心。

按照此流程操作，你的服务器经济系统即可与各玩法条线插件完美联动。
