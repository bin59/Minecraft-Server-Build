# 12. 离线背包查看 — OpenInv

**文件**: `plugins/OpenInv.jar` (347.81 KB)

## 功能说明

OpenInv 允许管理员**查看和编辑离线玩家**的物品栏和末影箱，无需玩家在线。这在处理玩家物品丢失、调查作弊等场景非常实用。

## 关键配置 (`plugins/OpenInv/config.yml`)

```yaml
config-version: 8
settings:
  equal-access: allow                 # 允许查看同等权限玩家背包
  command:
    open:
      no-args-opens-self: false       # 无参数不打开自身背包
    searchcontainer:
      max-radius: 10                  # 搜索容器最大半径
  disable-offline-access: false       # 允许离线访问
  disable-saving: false               # 允许保存修改
  console-locale: 'en'               # 控制台英文
```

## 常用命令

| 命令 | 说明 |
|---|---|
| `/openinv <玩家>` | 打开玩家背包（支持离线） |
| `/openender <玩家>` | 打开玩家末影箱 |
| `/searchinv <物品>` | 搜索在线玩家背包 |
| `/searchcontainer <物品> <半径>` | 搜索范围内容器 |
| `/silentcontainer` | 静默容器开关 |
| `/anycontainer` | 任意容器开关 |
