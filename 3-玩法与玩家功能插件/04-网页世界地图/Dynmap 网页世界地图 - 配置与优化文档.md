# Dynmap 网页世界地图 \- 配置与优化文档

## 一、插件简介

Dynmap 是 Minecraft 服务端主流网页地图插件，可实时渲染游戏世界、展示玩家位置、建筑地形、区块更新，支持网页端实时查看。默认配置为**全局全量渲染**，会持续扫描整个世界生成地图瓦片，高配无碍，但低配/轻量服务器会出现 CPU、磁盘 IO 占用过高、后台持续刷屏日志的问题。

## 二、核心问题：默认全量渲染负载过高

### 问题现象

服务端后台持续输出大量渲染日志，不间断批量生成地图瓦片：

`[dynmap] Full render of map 'surface' of 'world' in progress - 520400 tiles rendered`

### 问题危害

- 持续占用磁盘读写、后台线程、CPU 资源，拉高服务器整体负载

- 大量冗余瓦片文件堆积，占用服务器存储空间

- 渲染任务与插件、数据库、玩家事件争抢资源，轻微导致 TPS 波动

### 优化目标

关闭无意义的全局自动全量渲染，切换为**按需增量渲染模式**：仅渲染玩家加载、区块改动、新建建筑的区域，零闲置扫描、零无效渲染。

## 三、紧急处置（立刻停止高负载渲染）

服务端控制台 / 游戏内管理员直接执行，终止正在运行的全局渲染任务：

```Plain Text
# 终止主世界全量渲染
/dynmap cancelrender world

# 可选：暂停所有渲染任务
/dynmap pause all

# 恢复渲染（优化配置完成后使用）
/dynmap pause none
```

## 四、核心配置优化（configuration\.txt）

文件路径：`plugins/dynmap/configuration.txt`

⚠️ 严格使用**空格缩进，禁止 Tab**，修改完毕执行 `/dynmap reload` 热重载，无需重启服务器。

整合高性能、低负载、按需渲染全套最优配置：

```Plain Text
# ===================== 全量渲染保护机制 =====================
# TPS低于18自动暂停全量渲染，保护服务器性能
fullrender-min-tps: 18.0
# 只要有玩家在线，直接禁止全量渲染，杜绝后台负载抢占
fullrenderplayerlimit: 1

# ===================== 按需增量渲染核心 =====================
# 区块变更刷新间隔（秒）
renderinterval: 1
# 渲染积压加速刷新间隔
renderaccelerateinterval: 0.2
# 单次最大同时渲染瓦片数（低配服务器固定2，极致低配可改1）
tiles-rendered-at-once: 2
# TPS过低时自动暂停增量更新，防止崩服、掉TPS
update-min-tps: 18.0

# ===================== 磁盘IO减负优化 =====================
# 瓦片渲染间隔，降低瞬时磁盘写入压力
timesliceinterval: 0.1
# 关闭多线程暴力渲染，避免IO打满（按需开启）
#parallelrendercnt: 1
```

## 五、进阶优化：限定世界渲染边界（worlds\.txt）

文件路径：`plugins/dynmap/worlds.txt`

限制地图渲染矩形范围，范围外区块不生成瓦片，网页自动填充石头底色，从根源减少瓦片总量、降低存储与渲染压力。

主世界标准配置示例，可自行修改坐标适配服务器活动范围：

```Plain Text
- name: world
    maps:
      - surface
    # 限定地图渲染边界
    visibilitylimits:
      - { x0: -10000, z0: -10000, x1: 10000, z1: 10000 }
    hidestyle: stone
```

## 六、Dynmap 标准运维指令库

日常运维**禁止使用 /dynmap fullrender**，杜绝全局扫描渲染：

```Plain Text
# 终止指定世界全量渲染
/dynmap cancelrender world

# 暂停/恢复所有渲染
/dynmap pause all
/dynmap pause none

# 热重载全部配置（改配置必用）
/dynmap reload

# 局部按需渲染（推荐！替代fullrender）
# 以自身为中心，渲染指定半径区块，仅更新建筑区域
/dynmap radiusrender 200
```

## 七、运维关键注意事项

1. **旧瓦片清理**：切换按需渲染后，历史冗余瓦片不会自动删除。清理方式：停服 → 删除 `plugins/dynmap/web/tiles/world` 文件夹 → 重启服务器，系统仅按需生成新瓦片。

2. **渲染机制说明**：优化后仅在区块被玩家加载、破坏、放置方块、自然更新时触发渲染，不会后台遍历全图扫描。

3. **功能独立性**：Dynmap 渲染任务为独立后台线程，与插件报错、玩家登录事件无关联，互不影响。

## 八、同期服务器关联问题备注（非Dynmap问题）

本次优化同期日志报错记录：**NightCore v2\.16\.6 皮肤域名校验报错**

报错内容：`Expected host 'textures.minecraft.net' but got 'littleskin.cn'`

原因：旧版 NightCore 硬编码限制皮肤仅允许 Mojang 官方域名，不兼容 LittleSkin 第三方皮肤站。

影响：不踢人、不影响进服，但插件内玩家头像、个人资料加载失败。

解决方案：升级 NightCore 至新版（需同步升级所有 NightExpress 系列附属插件）；或关闭 NightCore 皮肤自动拉取配置。

> （注：部分内容由豆包工作 AI 生成）
