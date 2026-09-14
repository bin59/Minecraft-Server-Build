# 18. 常用管理命令速查

## 性能监控

```bash
/spark tps                          # 查看 TPS
/spark health                       # 健康报告
/spark profiler start --timeout 300 # 5分钟性能分析
```

## 玩家管理

```bash
/lp user <玩家> info                # 查看玩家权限详情
/lp user <玩家> parent add <组>     # 将玩家加入权限组
/openinv <玩家>                     # 查看/编辑玩家背包（支持离线）
/openender <玩家>                   # 查看玩家末影箱
```

## 方块操作审计

```bash
/co inspect                         # 开启检查模式（点击方块查记录）
/co lookup u:<玩家> t:1d r:50       # 查某玩家今天在50格范围内的操作
/co rollback u:<玩家> t:1h r:50     # 回滚某玩家1小时内的操作
/co near                            # 查看附近最近的变更
```

## 经济管理

```bash
/eco give <玩家> <金额>             # 给予金钱
/eco take <玩家> <金额>             # 扣除金钱
/vaultop                            # 财富排行榜
/vault reload                       # 重载经济配置
```

## 基岩版/Geyser

```bash
/geyser dump                        # 生成调试信息
/geyser statistics                  # 查看统计
/geyser reload                      # 重载 Geyser 配置
```

## 皮肤

```bash
/sr reload                          # 重载皮肤插件
/sr props <玩家>                    # 查看玩家皮肤属性
/sr applyskin <目标> <皮肤>         # 强制应用皮肤
```

## 领地

```bash
/resadmin                            # 管理员模式管理领地
/res list <玩家>                     # 列出玩家所有领地
/res message <领地> enter <消息>     # 设置进入欢迎语
/res message <领地> leave <消息>     # 设置离开消息
```
