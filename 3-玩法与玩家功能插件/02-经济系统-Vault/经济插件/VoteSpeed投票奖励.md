# VoteSpeed 投票奖励

本文档介绍南瓜生存服经济体系第二阶段落地的投票奖励插件 VoteSpeed 1.0.1，说明其作为经济体系**合规 Faucet（水龙头）**的定位、内置 NuVotifier 接收器配置、默认奖励表，以及玩家命令和权限。适用于管理员对照测试服配置、正式开服前接入投票服务商时使用。

> 落地环境：测试服 `C:\mc_serve\1.21.11-test\`（Leaf 1.21.11）｜ 落地日期：2026-09-17

## 功能定位

- **合规 Faucet**：玩家通过为服务器投票获得南瓜币，是新手玩家获取初始资金的主要渠道之一（与每日任务并列）。
- **内置 NuVotifier**：插件自带投票接收器，无需另装 NuVotifier。
- **激励留存**：连击奖励（7 天连续投票）和 VoteParty 全员奖励鼓励玩家持续投票。

## 版本与来源

- 版本：**1.0.1**，从 Modrinth 官方下载。
- 配置文件：`plugins/VoteSpeed/config.yml` + `plugins/VoteSpeed/rewards.yml`。

## 关键配置（当前值）

### config.yml

| 键 | 值 | 说明 |
|---|---|---|
| NuVotifier 端口 | `8192` | 内置投票接收器端口（已确认监听） |
| token | `nangua_pumpkin_2026_9f3a7c` | 投票站对接令牌 |
| 时区 | `Asia/Shanghai` | 连击/日期计算时区 |

### rewards.yml 默认奖励

| 条件 | 奖励 |
|---|---|
| 每票 | 30 南瓜币 |
| 7 天连击 | +100 南瓜币 |
| VoteParty 全员 | 100 南瓜币 |

## 命令

| 命令 | 用途 |
|---|---|
| `/vote` | 查看投票站并投票 |

## 权限（LuckPerms default 组，已授予）

| 权限 | 说明 |
|---|---|
| `votespeed.vote` | 投票 |
| `votespeed.gui` | 打开投票界面 |
| `votespeed.stats` | 查看投票统计 |
| `votespeed.top` | 查看投票排行 |
| `votespeed.points` | 查看点数 |
| `votespeed.shop` | 打开点数商店 |

## 与经济体系的关系

- **Faucet 项**：每票 30 南瓜币，计入周 Faucet 统计（见《经济系统落地实施-测试服.md》§4）。
- **配比约束**：系统发放占比（Faucet ÷ 玩家总收入）理想 < 50%，若投票收益过高导致印钱，优先降低票奖励或任务额度。

## 注意事项

- 测试服当前 `Vote sites: 0 active`（未接投票服务商），属正常；**正式开服前**需在 `config.yml` 配置投票站列表。
- token 属敏感配置，正式环境请自行更换并妥善保管（参照 README 隐私占位符约定）。
