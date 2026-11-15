# AuctionHouse 拍卖行

本文档说明拍卖行插件 AuctionHouse（iliüs）作为主交易渠道的功能定位、关键配置（上架费与成交税）、玩家/管理命令与经济联动关系。

## 功能定位

- **主交易渠道**：玩家间买卖物品的主要场所，替代系统回收（`/sell` 已关闭），是经济体系的**核心 Sink** 之一。
- **纯回收设计**：上架费不退 + 成交税抽成，两笔钱都从玩家账本消失，用于对抗通胀。
- **拍卖形式**：玩家上架物品并设定价格，其他玩家可竞价或按一口价购买。

## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | `plugins/AuctionHouse-1.1.4.jar`，`version: 1.1.4`，配置文件 `plugins/AuctionHouse/config.yml` |
| 官方最新 | **1.1.4**（2026-07-23 发布）；本地已是最新，配置基于官方默认模板改写 |
| 官方文档站 | <https://www.spigotmc.org/resources/auctionhouse.133636>（ilus 版命令 / 权限说明） |
| 官方下载 | [SpigotMC 资源页](https://www.spigotmc.org/resources/auctionhouse.133636) · [Modrinth](https://modrinth.com/plugin/auctionhouse-by-ilius) |
| 主要页面 | [SpigotMC 资源页](https://www.spigotmc.org/resources/auctionhouse.133636) · [Modrinth 项目页](https://modrinth.com/plugin/auctionhouse-by-ilius) · [Modrinth 更新日志](https://modrinth.com/plugin/auctionhouse-by-ilius/changelog) · [作者 ilius](https://www.spigotmc.org/resources/authors/ilius.1438987) |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方命令/权限表复核。

## 关键配置（当前值）

| 键 | 值 | 说明 |
|---|---|---|
| `listing-fee` | `30.0` | 上架费 30 南瓜币（不退，纯 Sink） |
| `sales-tax` | `5.0` | 成交税 5%（卖家到手扣） |
| `currency-format` | `"{amount} 南瓜币"` | 显示统一为 南瓜币 |
| `listing-limits.default` | `20` | 默认每人最多 20 个在售 |

## 命令

| 命令 | 用途 |
|---|---|
| `/ah` | 玩家打开拍卖行界面（权限 `auction.*` 默认 true，无需授予） |
| `/ah admin` | 管理员管理拍卖行（如 `/ah admin reload` / `/ah admin stats`） |

## 权限

- `auction.*`：全部权限，默认对玩家开放，无需额外授予。

## 与经济体系的关系

- **Sink 项**：上架费 30 南瓜币 + 成交税 5%（卖家承担），每笔交易两处抽水，直接回收货币。
- **联动**：配合 Residence `CanSell` 后续可开放**玩家间卖领地**（当前 `CanSell: false` 保持关闭）。
- **监控**：周 Sink 统计时，将拍卖行上架费与成交税计入"拍卖税记录"。

## 注意事项

- 上架费在物品卖出/流拍时均不退，属纯回收，定价时需考虑玩家承受力。
- 默认每人 20 个在售位，玩家需合理周转；可按需调整 `listing-limits`。
