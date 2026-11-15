# 15. 统计系统 — bStats

bStats 是插件普遍接入的匿名使用统计服务，启动后匿名上报服务器数量、版本、地区，不收集玩家信息。本文记录其开关与服务器 UUID。

## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | 外部匿名统计服务（`plugins/bStats/config.yml`），各插件内置 Metrics，不硬编版本号 |
| 官方最新 | 官网持续更新，当前按官方 docs 口径；外部服务，不硬编版本号 |
| 官方文档站 | https://bstats.org/ · https://bstats.org/docs |
| 官方下载 | 无独立插件 jar，按 https://bstats.org/docs 集成 Metrics 类（各插件内置） |
| 主要页面 | https://bstats.org/docs · https://bstats.org/register · https://bstats.org/add-plugin · https://bstats.org/ |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方文档复核。

## 配置 (`plugins/bStats/config.yml`)

```yaml
enabled: true
serverUuid: fed727e4-8c9f-45ba-9395-75bdaa730a7f
logFailedRequests: false
```
