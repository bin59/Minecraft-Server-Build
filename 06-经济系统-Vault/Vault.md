# 6. 经济系统 — Vault

**文件**: 内置于 `plugins/Vault/` 目录 | **版本**: 1.6.2

## 功能说明

Vault 2.0 是一个内建经济系统，同时兼容 Vault API。它提供货币余额、玩家间转账、贷款系统等功能。与传统的需要外部 Vault.jar 不同，此版本是自包含的经济插件。

## 关键配置 (`plugins/Vault/config.yml`)

```yaml
plugin_version: 1.6.2
language: en

currency:
  symbol: $              # 货币符号
  position: suffix       # 符号位置（后缀）
  space: true            # 符号与数字间有空格
  format: auto           # 自动格式化

storage:
  use_mysql: false       # 使用文件存储（非 MySQL）

offline-uuid-fallback: true  # 离线 UUID 回退

pay_limits:
  min: 1                 # 最小支付金额
  max: 100000            # 最大支付金额

loans:
  enabled: true          # 启用贷款系统
  max_active_per_player: 1   # 每人最多 1 笔贷款
  min_amount: 1          # 最小贷款金额
  max_amount: 100000     # 最大贷款金额
  max_installments: 60   # 最多 60 期
  default_interval_hours: 24  # 默认还款间隔(小时)
  max_missed_payments: 3     # 最多逾期 3 次
  defaulted_effects:
    enabled: true
    effects:
      - SLOW:1           # 违约惩罚：缓慢 I
      - SLOW_DIGGING:1   # 违约惩罚：挖掘疲劳 I
```

## 常用命令

### 玩家命令

| 命令 | 说明 |
|---|---|
| `/balance` | 查看余额 |
| `/pay <玩家> <金额>` | 转账给玩家 |
| `/pay` | 打开支付菜单 |
| `/vault loan` | 打开贷款菜单 |

### 管理命令

| 命令 | 说明 |
|---|---|
| `/eco give <玩家> <金额>` | 给予金钱 |
| `/eco take <玩家> <金额>` | 扣除金钱 |
| `/vault reload` | 重载配置 |
| `/vaultop` | 查看财富排行榜 |

## PlaceholderAPI 变量

| 变量 | 说明 |
|---|---|
| `%vault_balance%` | 原始余额数字 |
| `%vault_balance_formatted%` | 格式化余额 |
| `%vault_currency_symbol%` | 货币符号 ($) |
| `%vault_top%` | 财富排行前 10 |
| `%vault_top_1%` ~ `%vault_top_10%` | 各排名余额 |
