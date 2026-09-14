# 4. 跨版本协议兼容 — ViaVersion

**文件**: `plugins/ViaVersion-5.9.1.jar` (6.08 MB)

**官方网站**: https://viaversion.com | **文档**: https://docs.viaversion.com

## 功能说明

ViaVersion 允许**低于服务器版本的 Minecraft 客户端**连接到服务器。本服运行 1.21.1，安装了 ViaVersion 后，使用较旧版本（如 1.8.x ~ 1.21.x）的 Java 版客户端也能加入游戏。

> ⚠️ **注意**: 本服仅安装了 ViaVersion（允许低版本客户端加入），未安装 **ViaBackwards**（允许高版本客户端加入低版本服务器）和 **ViaRewind**（允许更旧的 1.7.x 客户端）。

## 关键配置 (`plugins/ViaVersion/config.yml`)

```yaml
check-for-updates: true
send-supported-versions: false      # 不向客户端暴露支持的旧版本列表
block-versions: []                  # 未封禁任何版本
block-protocols: []                 # 未封禁任何协议版本

packet-limiter:
  enabled: true
  max-per-second: 800               # 每秒最大数据包数
  sustained-max-per-second: 200     # 持续每秒最大数据包数
  sustained-period-seconds: 7       # 持续监控周期
  sustained-threshold: 4            # 超限次数阈值

# 旧版兼容选项
serverside-blockconnections: true   # 启用服务端方块连接
blockconnection-method: packet      # 数据包级别处理
team-colour-fix: true               # 队伍颜色修复
shield-blocking: true               # 1.9+ 盾牌格挡支持
prevent-collision: true             # 碰撞防护
auto-team: true                     # 自动组队
simulate-pt: true                   # 玩家 tick 模拟
nms-player-ticking: true            # NMS 玩家 tick
bossbar-patch: true                 # Boss 血条修复
item-cache: true                    # 物品缓存
left-handed-handling: true          # 左手持物支持
use-1_8-hitbox-margin: true        # 1.8 碰撞箱兼容

# 1.21 特定修复
fix-1_21-placement-rotation: true   # 修复 1.21 放置旋转
fix-infested-block-breaking: true   # 修复虫蚀方块破坏
fix-non-full-blocklight: true       # 修复非完整方块光照
fix-1_14-health-nan: true           # 修复生命值 NaN 问题
```

## 常用命令

| 命令 | 说明 |
|---|---|
| `/viaversion` 或 `/viaver` | 查看插件信息 |
| `/viaversion list` | 列出在线玩家的客户端版本 |
| `/viaversion reload` | 重载配置 |
