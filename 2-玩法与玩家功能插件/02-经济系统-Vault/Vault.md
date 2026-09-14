这份教程专为你的服务器定制，重点解决 `Residence` 报错问题，并补充了 `LuckPerms` 权限配置。

### 📦 Vault + EssentialsX 部署配置指南

> **适用环境**：已安装 `LuckPerms` 和 `Residence` 的服务器
> **服务端版本**：Paper / Spigot / Purpur 1.21.11
> **目标**：修复 `Could not determine economy` 报错，开启领地买卖/租赁功能

---

### 一、 核心原理与安装顺序

你的服务器现状是：有“管家”（Residence）和“门禁系统”（LuckPerms），但缺了“银行”（EssentialsX）和“通讯线路”（Vault）。

**依赖关系图：**

```text
Residence (领地插件)
   ⬇️ 调用经济API
Vault (中间件/桥梁)  <-- 必须先装 EssentialsX，Vault 才能连上它
   ⬇️ 提供经济数据
EssentialsX (经济核心)
```

**⚠️ 关键安装顺序：**

1.  **先装 EssentialsX**（建立银行）
2.  **再装 Vault**（铺设线路）
3.  **最后重启**（Residence 会自动检测到线路通了）

---

### 二、 EssentialsX 安装与配置（经济核心）

#### 1. 下载与安装

- **EssentialsX (必须)**: [SpigotMC 下载](https://www.spigotmc.org/resources/essentialsx.9089/)
- **EssentialsXSpawn (推荐)**: [SpigotMC 下载](https://www.spigotmc.org/resources/essentialsxspawn.20939/) (提供 `/spawn` 和出生点设置功能)

**操作：** 将下载的 `.jar` 文件放入 `plugins` 文件夹，**暂时不要重启**。

#### 2. 基础配置 (`config.yml`)

首次启动后会生成配置文件。停止服务器，编辑 `plugins/Essentials/config.yml`：

```yaml
# --- 经济系统设置 ---
economy:
  # 必须设为 true，Residence 才能读取到钱
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

### 三、 Vault 安装与配置（关键桥梁）

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

- 如果看到 `Essentials Economy: Found`，说明 **Residence 的报错将消失**。
- 如果看到 `LuckPerms...`，说明权限系统已连接。

---

### 四、 与 Residence 联动配置

安装完上述插件后，Residence 会自动开启经济功能。你需要检查并配置权限。

#### 1. 检查 Residence 配置

编辑 `plugins/Residence/config.yml`：

```yaml
Economy:
  # 必须开启，否则无法买卖领地
  EnableEconomy: true

  # 货币名称，建议与 EssentialsX 保持一致
  CurrencyName: '金币'
```

#### 2. LuckPerms 权限配置

由于你使用 LuckPerms，需要手动赋予玩家使用经济指令和领地经济指令的权限。

**给默认组 (default) 添加基础经济权限：**

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
```

**给管理员组 (admin) 添加无限金钱权限 (可选)：**

```bash
lp group admin permission set essentials.money.unlimited true
```

---

### 五、 功能测试

在游戏内进行以下测试，确保一切正常：

| 测试项目         | 指令                     | 预期结果                      |
| :--------------- | :----------------------- | :---------------------------- |
| **查看余额**     | `/bal`                   | 显示 `¥0.00` (或对应金额)     |
| **设置领地价格** | `/res price 领地名 1000` | 提示“领地价格已设置为 1000”   |
| **购买领地**     | `/res buy 领地名`        | 扣除 1000 金币，领地归 属变更 |
| **租赁领地**     | `/res rent 领地名`       | 扣除租金，获得租期            |

---

### 六、 常见问题排查

1.  **Residence 依然报错 `Could not determine economy`**
    - **原因**：Vault 没找到 EssentialsX。
    - **解决**：检查 `plugins` 文件夹里是否有 `EssentialsX.jar`，确保先启动了 EssentialsX，再启动 Vault。查看日志是否有 `Essentials Economy: Found`。

2.  **提示 `No economy system found`**
    - **原因**：EssentialsX 的经济模块被关掉了。
    - **解决**：检查 `plugins/Essentials/config.yml` 中的 `economy: enabled: true`。

3.  **玩家无法使用 `/res sell`**
    - **原因**：LuckPerms 权限不足。
    - **解决**：执行 `lp group default permission set residence.sell true`。

按照此流程操作，你的服务器经济系统和领地插件即可完美联动。
