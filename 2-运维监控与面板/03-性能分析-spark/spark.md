# 13. 性能分析 — spark

spark 是 LuckPerms 作者开发的高性能服务器性能分析工具，实时监控 CPU、TPS、内存，profiler 生成报告定位卡顿。本文给出关键配置与常用命令。

**文件**: 内置于 `plugins/spark/` 目录 ｜ **官网**: https://spark.lucko.me

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
