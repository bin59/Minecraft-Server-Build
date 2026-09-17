# 15. 统计系统 — bStats

bStats 是 Minecraft 插件普遍接入的匿名使用统计服务，服务器启动后会匿名上报服务器数量、版本、地区等数据，帮助插件作者了解用户群体，本身不收集任何玩家信息。本文档记录了其 config.yml 的开关与服务器 UUID 等配置。供管理员确认统计是否开启时参考。

**文件**: 内置于 `plugins/bStats/` 目录

**官方网站**: https://bstats.org

## 功能说明

bStats 是 Minecraft 服务器插件使用统计服务，匿名收集插件的使用数据（服务器数量、版本、国家等），帮助插件作者了解用户群体。完全匿名，不收集任何玩家数据。

## 配置 (`plugins/bStats/config.yml`)

```yaml
enabled: true
serverUuid: fed727e4-8c9f-45ba-9395-75bdaa730a7f
logFailedRequests: false
```
