# EconomyShop 玩家商店

本文档介绍南瓜生存服经济体系第二阶段落地的玩家商店插件 EconomyShop 1.1.6（作者 iliüs），说明其作为玩家间交易渠道的定位、防印钞关键配置（关闭系统商店回收）、开店费与成交税，以及玩家命令和权限。适用于管理员对照测试服配置、跟进上线前校准。

> 落地环境：测试服 `C:\mc_serve\1.21.11-test\`（Leaf 1.21.11）｜ 落地日期：2026-09-17

## 功能定位

- **玩家商店渠道**：玩家开设个人商店或木牌商店，向其他玩家出售物品，与拍卖行（/ah）互为补充的交易渠道。
- **防印钞核心**：通过关闭"卖物品给系统商店"和 `/sell` 命令，堵住量产物品刷钱的漏洞（农场刷不出钱，见《经济系统设计手册》§4D）。
- **Sink 设计**：开店收费 + 玩家间成交税，两处抽水。

## 版本与来源

- 版本：**1.1.6（iliüs）**，从 Modrinth 官方下载，基于官方默认模板改写配置。
- 配置文件：`plugins/EconomyShop/config.yml`。

## 关键配置（当前值）

| 键 | 值 | 说明 |
|---|---|---|
| `sell-to-shop` | `false` | 玩家无法卖物品给系统商店（防印钞） |
| `seed-default-shop` | `false` | 不自动生成默认商店 |
| `sell-commands` | `false` | 关闭 `/sell` 类命令 |
| `creation-cost` | `100.0` | 开店费用 100 南瓜币（Sink） |
| `transaction-tax-percent` | `5.0` | 玩家商店成交税 5% |

## 命令

| 命令 | 用途 |
|---|---|
| `/shop` | 打开/创建玩家商店（权限已授 default） |
| `/chestshop` | 创建木牌商店（权限已授 default） |

## 权限（LuckPerms default 组，已授予）

| 权限 | 说明 |
|---|---|
| `economyshop.use` | 使用商店功能 |
| `economyshop.sell` | 出售物品 |
| `economyshop.chestshop.create` | 创建木牌商店 |
| `economyshop.chestshop.use` | 使用木牌商店 |

## 与经济体系的关系

- **Sink 项**：开店费 100 南瓜币（一次性）+ 玩家商店成交税 5%（每笔交易）。
- **防印钞**：`sell-to-shop: false` + `sell-commands: false` 是手册 §4D 的关键落地，量产物品只能走玩家间交易。
- **监控**：启动日志确认 `Hooked into LuckPerms`、`AuctionHouse integration enabled`、`Default-shop seeding disabled`。

## 注意事项

- 关闭 `/sell` 后玩家可能不适应，需在公告中说明"量产物品走玩家间交易（/ah /shop）"。
- 玩家商店成交税 5% 与拍卖行成交税 8% 并存，定价时注意让利空间。
