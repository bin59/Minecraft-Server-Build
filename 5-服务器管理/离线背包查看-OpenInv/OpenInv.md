# 12. 离线背包查看 — OpenInv

OpenInv 允许管理员在玩家离线时查看和编辑其物品栏与末影箱，适用于处理物品丢失、调查作弊等场景。本文档记录了 config.yml 的关键配置，以及打开离线背包、搜索容器等常用命令。供服务器管理员日常物品核查与纠纷处理时使用。

**文件**: `plugins/OpenInv.jar` (347.81 KB)

## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | `plugins/OpenInv.jar`，`version: 5.3.5` |
| 官方最新 | 官网持续更新，当前按官方 wiki 口径 |
| 官方文档站 | <https://github.com/Jikoo/OpenInv>（官方维护仓库）· 命令 <https://github.com/Jikoo/OpenInv/wiki/Commands> · 权限 <https://github.com/Jikoo/OpenInv/wiki/Permissions> |
| 官方下载 | <https://github.com/Jikoo/OpenInv/releases> · <https://modrinth.com/plugin/openinv> · <https://hangar.papermc.io/jikoo/OpenInv> |
| 主要页面 | `/wiki/Commands` · `/wiki/Permissions` · `/wiki/Customizing-Inventories` · `/releases` |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方命令/权限表复核。

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
