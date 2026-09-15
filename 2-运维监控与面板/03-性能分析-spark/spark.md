# 13. 性能分析 — spark

**文件**: 内置于 `plugins/spark/` 目录

**官方网站**: https://spark.lucko.me

## 功能说明

spark 是由 LuckPerms 作者开发的高性能 Minecraft 服务器性能分析工具，可以实时监控 CPU 使用率、TPS、内存占用，并进行深度性能分析，帮助定位卡顿原因。

## 关键配置 (`plugins/spark/config.json`)

```json
{
  "backgroundProfiler": true    // 启用后台性能分析器
}
```

## 常用命令

| 命令 | 说明 |
|---|---|
| `/spark` | 查看基本信息和使用率 |
| `/spark health` | 健康报告 |
| `/spark tps` | 查看 TPS |
| `/spark profiler start` | 开始性能分析 |
| `/spark profiler stop` | 停止并获取分析报告链接 |
| `/spark profiler open` | 打开最近的分析报告 |
| `/spark gc` | 查看 GC 信息 |
| `/spark ping` | 查看玩家延迟 |
