# 15. 统计系统 — bStats

bStats 是插件普遍接入的匿名使用统计服务，启动后匿名上报服务器数量、版本、地区，不收集玩家信息。本文记录其开关与服务器 UUID。

**文件**: 内置于 `plugins/bStats/` 目录 ｜ **官网**: https://bstats.org

## 配置 (`plugins/bStats/config.yml`)

```yaml
enabled: true
serverUuid: fed727e4-8c9f-45ba-9395-75bdaa730a7f
logFailedRequests: false
```
