# CoordinateLogger 玩家位置记录（实际安装配置）

## 配置文件（config.yml）

```yaml
# 跟踪间隔（秒）：越小越密、开销越大；百人在线服建议 ≥ 3~5
tracking-interval: 5

# 数据保留天数，到期由插件自动清理
data-retention-days: 30

# 最小移动距离（格）才记录，0 表示每次采样都记
min-distance-to-save: 0.0

# 详细日志开关（调试用，日常关）
verbose-logging: false
```
