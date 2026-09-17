# SimplePets Vault Addon 经济联动

本文是 SimplePets 主文档的经济扩展篇，专门讲本服已启用的 Vault Addon：让玩家用服务器主货币（EssentialsX 余额，与全服同一套账）花钱购买或解锁宠物，而不是另起一套货币。内容包括安装记录、AddonConfig.yml 与 Vault.yml 价格配置、按生物类型调价的方法，以及 pet.vault.bypass 免付权限的用法。

> **状态**：✅ 已安装启用（2026-09-16） | **Addon 版本**：0.4（MC 1.18–1.19.4 兼容构建，适配本服 1.21.11）
> **所属**：[宠物系统 SimplePets](宠物系统simplepets.md) 的经济扩展 | **上层经济**：[Vault](../02-经济系统-Vault/Vault.md) + [EssentialsX](../10-EssentialsX多功能指令整合/EssentialsX多功能指令整合（功能说明与完整配置）.md)

## 一、这是什么

SimplePets 通过「经济 Addon」实现**花钱买宠物 / 解锁宠物类型**。Vault Addon 是其中一种实现：Hook 进服务器已有的 Vault 经济（本服为 EssentialsX），宠物用**服务器主货币**购买——与 `/bal` 余额、Residence、死亡收费**同一套账**，玩家无需第二套货币。

| Addon | 货币 | 本服状态 |
|---|---|---|
| **Vault Addon** | Vault（= EssentialsX 余额） | ✅ 已启用 |
| GemsEconomy Addon | GemsEconomy 独立货币（两套账） | ❌ 未装 |

> 若改换 GemsEconomy Addon，货币会与全服经济脱钩，本服不推荐。

## 二、安装记录（2026-09-16）

| 项 | 值 |
|---|---|
| Addon jar | `plugins/SimplePets/Addons/VaultAddon.jar`（9,085 字节） |
| 来源 | Modrinth 官方源（项目 `spets-vault`，版本 0.4；下载需 `curl -k`） |
| 启用方式 | 放入 `Addons/` 目录后**重启服务端**（非热加载） |
| 启动日志证据 | `[SimplePets ADDON] Loading modules for the Vault addon` |
| 经济挂钩 | `[Vault] [Economy] Essentials Economy hooked.`（重启时确认） |

> 重启后插件自动生成两个配置文件（见下）。

## 三、文件结构

```
plugins/SimplePets/
├── Addons/
│   ├── VaultAddon.jar          # Addon 本体
│   └── configs/
│       └── Vault.yml           # ★ 价格与购买行为配置（421 行，自动生成）
├── AddonConfig.yml             # Addon 总开关（59 字节，仅 Vault.Enabled）
└── config.yml                  # SimplePets 主配置
```

## 四、配置详解

### 4.1 AddonConfig.yml（总开关）

```yaml
Vault:
  # Enable/Disable the Vault module
  Enabled: true
```

- `Vault.Enabled: true` = 启用 Vault Addon。
- **修改后需重启服务端**，或在游戏内执行 `/pet addon` **禁用再启用**刷新。

### 4.2 Addons/configs/Vault.yml（价格与行为）★

关键项（行号以当前自动生成版本为准）：

| 配置项 | 当前值 | 含义 |
|---|---|---|
| `Pay-Per-Use-Enabled`（第 8 行） | `false` | **一次性购买**：花钱解锁后永久可用，不再重复扣费；`true` 则每次召唤都要付费 |
| `Hide-Price-If-Bypassed`（第 3 行） | `true` | 有免付权限时隐藏价格、显示 `BYPASSED` |
| `Price.Free` / `Price.Bypassed` | `Free` / `BYPASSED` | 免费用/免付时 lore 显示文案 |
| `type.<生物>`（第 380–421 行） | **全部 `2000`** | ★ **每只宠物的价格**，按生物类型分别定价 |

**调价方法**：编辑 `Addons/configs/Vault.yml` 的 `type:` 段，例如把狼改成 500：

```yaml
type:
  wolf: 500        # 原 2000
```

改完执行 `/pet addon` 禁用再启用（或重启服务端）生效。

## 五、权限

| 权限 | 说明 |
|---|---|
| **`pet.vault.bypass`** | **父权限（免付总开关）**：持有者购买任何宠物都免单；给赞助组（vip/vip+）发这个 |
| `pet.vault.bypass.<生物>` | 单种宠物免付（如 `pet.vault.bypass.wolf`），父权限包含全部子权限 |
| `pet.commands.purchased` | 查看已购宠物（`/pet purchased`） |
| `pet.commands.addon` | Addon 管理（`/pet addon` 禁用/启用/更新）——管理用 |

> ⚠️ **勿混用**：`Pet.economy.bypass` 是 **GemsEconomy Addon** 的免单权限，本服未装 GemsEconomy，不生效。Vault Addon 一律用 **`pet.vault.bypass`**。

## 六、相关命令

| 命令 | 说明 |
|---|---|
| `/pet purchased` | 查看已购买的宠物 |
| `/pet addon` | Addon 管理（禁用/启用刷新配置） |
| `/pet` | 宠物主菜单（购买入口在宠物选择 GUI 中） |

## 七、与文档体系的关系

| 文档 | 关系 |
|---|---|
| [宠物系统simplepets.md](宠物系统simplepets.md) | SimplePets 主文档（本文件是其经济章节的展开） |
| [Vault.md](../02-经济系统-Vault/Vault.md) | Vault 经济 API 本身 |
| [EssentialsX 文档](../10-EssentialsX多功能指令整合/EssentialsX多功能指令整合（功能说明与完整配置）.md) | 主货币来源（currency-symbol `¥`） |
| [权限节点速查](../../5-服务器管理/01-权限管理系统-LuckPerms/权限节点速查.md) | 权限落地与 LuckPerms 配置 |

> 链接为相对路径，从 `3-玩法与玩家功能插件/14-宠物系统/` 出发。
