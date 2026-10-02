# 6. 网页世界地图 — Dynmap（备选方案）

> **本服现状（2026-09-27）**：当前生产选的是 **BlueMap**（3D 网页地图，已部署验证，见同目录 `BlueMap.md`）。本文档是 **Dynmap 备选方案**的完整接入说明——包括与 Residence 领地的原生集成配置（Residence 6.x 已内置 Dynmap 支持，无需额外桥接插件）。若日后想切换或对照评估，按本文档操作。

本文介绍 Dynmap 这款**经典 2D 平铺网页地图**插件：把服务器世界实时渲染成浏览器里的平面地图（卫星/地表/洞穴三种视图），支持标记（markers）、玩家位置、在线人数等。特点是**轻量、老牌、生态最全**，Residence 对其有**原生内置集成**。

**当前版本参考**：Dynmap 3.x（Paper/Spigot 平台，需 Java 17+；本服 Leaf 1.21.11 兼容 3.7+ 版本线）

**MC 要求**：1.21.x（含 1.21.11）

**作者/发布**：mikeprimm 等 | **协议**: Apache-2.0

**官网**: https://www.dynmap.org ｜ **SpigotMC**: https://www.spigotmc.org/resources/dynmap.274/ ｜ **GitHub**: https://github.com/webbukkit/dynmap

---

## 功能说明

- **2D 平铺地图**：浏览器打开即看，卫星视图（从上往下）、地表视图、洞穴视图（地下矿洞/红石分布）。
- **多世界支持**：主世界 / 下界 / 末地 / 自定义世界各自成图，可配置哪些世界渲染。
- **标记系统（Markers）**：POI 图标、区域多边形、文字标签；**Residence 领地自动图层**是最大卖点（原生）。
- **玩家标记**：在线玩家头像与实时坐标（可关/可限权限，涉及隐私见下）。
- **实时增量**：区块变化即时（或按配置周期）更新，日常开销小。
- **Web 服务内置**：默认 8123 端口，自带 WebUI，也可用 nginx 反代。
- **与客户端无关**：Java / 基岩玩家浏览器都能看。

## 安装与前置

1. 服务端为 **Paper / Purpur / Leaf**（Paper 分支均可）。
2. 下载 Dynmap **Paper/Spigot 平台 jar**（取兼容 1.21.11 的 3.x），放入 `plugins/`，**完整重启**（非 /reload）。
3. 无需强制前置依赖；装 **PlaceholderAPI** 可让标记/网页使用其占位符（可选）。
4. 开放 TCP 端口 **8123**（内置 Web），或用 nginx/Caddy 反代。
5. **Residence 集成零配置即可用**：Residence 检测到 Dynmap 存在且 `DynMap.Use: true`（默认已开）就会自动画领地图层。

## 玩家如何使用

| 操作                            | 效果                                        |
| ------------------------------- | ------------------------------------------- |
| 浏览器打开 `http://服务器:8123` | 查看平面地图                                |
| 滚轮 / 拖拽                     | 缩放、平移                                  |
| 左上角图层开关                  | 卫星 / 地表 / 洞穴 / **Residence** 领地图层 |
| 点领地                          | 看领地边界与详情（领主、旗帜等）            |

> 查看地图不需要任何游戏内权限（网页匿名访问）。仅管理命令需要权限。

## 命令与权限

| 命令                         | 权限节点（参考）                   | 默认   | 说明                                 |
| ---------------------------- | ---------------------------------- | ------ | ------------------------------------ |
| `/dynmap`                    | `dynmap.*`                         | op     | 主命令 / 帮助                        |
| `/dynmap reload`             | `dynmap.reload`                    | op     | 重载配置与地图                       |
| `/dynmap hide` / `show`      | `dynmap.hide` / `dynmap.show`      | 视配置 | 隐藏 / 显示玩家在地图上的位置        |
| `/dynmap radiusrender`       | `dynmap.radiusrender`              | op     | 按半径强制渲染区域（配合 Chunky 用） |
| `/dynmap fullrender`         | `dynmap.fullrender`                | op     | 全量渲染某世界                       |
| `/dynmap pause` / `pauseall` | `dynmap.pause` / `dynmap.pauseall` | op     | 暂停 / 暂停全部渲染                  |
| `/dynmap webregister`        | `dynmap.webregister`               | op     | 注册/更新 web 登录令牌               |

> ⚠️ 权限节点以插件实际注册为准：装好后跑 `/lp tree dynmap` 核对确切节点名，再按组授权（admin/owner 给 `dynmap.*` 即可）。
> 玩家**查看地图无任何权限要求**；若要让特定玩家/组**不在地图上显示**，用 `dynmap.hide` 权限或配置 `player-marker` 相关项。

## 配置要点

首次启动生成 `plugins/dynmap/` 配置，关键文件与节点：

- **`configuration.txt`**：`webport`（默认 **8123**）、`webpages`（WebUI 开关）、`webserver-bindaddress`（生产建议绑内网/回环，由反代对外）、`max-tiles`、`render-radius` 等。
- **`worlds.txt`**：每世界一节（`world:` / `world_nether:` / `world_the_end:`），含 `enabled`、`center`、`zoom`、`map`（`flat` / `surface` / `cave` 三视图开关）。
- **`markers.txt`**：marker 集定义；**Residence 图层由 Residence 插件自动注册，不在这里手写**。
- **`templates/`**：WebUI 模板与主题。

**隐私设置（重要）**：公开网页若显示玩家标记，等于把全员实时坐标暴露给任何人。建议：

- 关掉玩家标记（`player-marker` / 相关配置）；或
- 仅内网 / 白名单访问地图；或
- 仅对特定世界开启玩家标记。

**首渲很重**：第一次全量渲染大世界可能几十分钟~几小时，且占盘（tiles 随探索增长）。务必低峰期执行，并用 **Chunky** 预生成活动范围配合（见下）。

## 体积优化（瓦片压缩）

**本服现状**：`configuration.txt` 里 `image-format: jpg-q90`（已用 JPG 质量 90，比默认 PNG 省很多）。继续压按见效排序：

### 1. 降低 JPG 质量（最简单）

`configuration.txt`：

```yaml
image-format: jpg-q75 #   **q75 是甜点位**—— 体积砍半，画质几乎无损 jpg-q60（体积更小，远处画质略糊）
```

改完 `/dynmap reload` 生效。

### 2. 关掉洞穴地图（省约 1/3 瓦片）

默认每世界生成 surface（地表）/ flat（平面）/ cave（洞穴）三套地图，**cave 最占空间**。`worlds.txt` 里每世界只留 surface：

```yaml
worlds:
  - name: world
    maps:
      - class: org.dynmap.hdmap.HDMap
        name: surface
        title: 'Surface'
        prefix: t
        perspective: iso_SE_30_hires
        shader: stdtexture
        lighting: shadows
        mapzoomin: 1
```

（nether / world_the_end 同理，去掉 cave / flat 只留 surface）

<!-- ### 3. 只渲染主世界

`worlds.txt` 里禁用不需要的世界：

```yaml
- name: world_nether
  enabled: false
- name: world_the_end
  enabled: false
```
 -->
<!-- ### 4. 限制渲染范围（只渲染主城/玩家活动区）

`worlds.txt` 加 `visibilitylimits`，例如只渲染出生点 ±3000 格：

```yaml
- name: world
  visibilitylimits:
    - x0: -3000
      z0: -3000
      x1: 3000
      z1: 3000
``` -->

### 5. 清理旧瓦片并重渲

改完配置后**旧瓦片不会自动删**：

1. 删掉 `plugins/dynmap/web/tiles/`（云端运行服上）
<!-- 2. 重启后 `/dynmap fullrender world`（范围按上面配置）或 `/dynmap radiusrender world 3000`（以玩家为中心限半径渲染） -->

**推荐组合**：`jpg-q75` + 关 cave + 只渲染主世界 <!-- + visibilitylimits ±3000 → 体积可减到原来的 1/5 甚至更少。 -->

## 领地图层（Residence 原生集成）——重点

Residence（本服 6.0.0.1）**内置 Dynmap 支持**，只要 Dynmap 已安装且 Residence config 里 `DynMap.Use: true`（默认就是 true），领地会自动画到地图上，**不需要任何额外插件**。相关配置全部在：

`plugins/Residence/config.yml` 的 `DynMap:` 段（本服当前实配如下，逐项说明）：

```yaml
DynMap:
  # 是否启用 Dynmap 支持（装没装 Dynmap 都是 true 也不影响，插件会自检）
  Use: true

  # true = 网页地图上默认隐藏领地图层，玩家需在地图左侧菜单手动打开"Residence"图层
  # false = 默认显示全部领地
  # 本服：false（默认直接可见）
  HideByDefault: false

  # 是否在领地标记详情里显示领地旗帜（flags）列表
  # true = 点领地时展示该领地的权限/旗帜摘要
  ShowFlags: true

  # 是否在领地总览里排除"默认旗帜"（即只显示玩家自定义过的旗帜，减少噪音）
  ExcludeDefaultFlags: true

  # true = 游戏内设置了 hidden 旗帜（/res set hidden true）的领地，地图上也隐藏
  # false = 无视 hidden 旗帜，一律显示
  # 本服：true（与游戏内隐藏状态联动，隐私友好）
  HideHidden: true

  Layer:
    # 是否启用 3D 立体区域（Dynmap 3.x 支持立体块状渲染；关掉则纯平面多边形）
    3dRegions: true
    # 子领地显示到第几层（0 = 只显示顶级领地；2 = 显示到孙级子领地）
    SubZoneDepth: 2

  Border:
    # 领地边界线颜色（十六进制，可从 https://www.w3schools.com/colors/colors_picker.asp 挑）
    Color: '#FF0000'
    # 边界线透明度（0.3 = 只有 30% 不透明度，其余透出地图）
    Opacity: 0.3
    # 边界线粗细（像素）
    Weight: 3

  Fill:
    # 填充区总不透明度（0.3 = 半透明色块）
    Opacity: 0.3
    # 普通领地填充色（黄）
    Color: '#FFFF00'
    # 出租中（可租）领地填充色（绿）
    ForRent: '#33cc33'
    # 已被租出领地填充色（亮绿）
    Rented: '#99ff33'
    # 出售中领地填充色（蓝）
    ForSale: '#0066ff'

  # 只显示名单里的领地（留空 = 全部显示；填了则只显示这些）
  VisibleRegions: []
  # 强制隐藏名单里的领地（即使游戏里没设 hidden）
  HiddenRegions: []
```

**生效方式**：改完 `plugins/Residence/config.yml` 后执行 `/res reload`（Residence 重载）即可；若 Dynmap 是新装的，先完整重启服务器让 Residence 完成地图注册。

**常见调优**：

- **领主名显示**：Residence 地图标记默认显示领主名与领地状态（出售/出租颜色区分），无需额外配置。
- **只想隐藏个别领地**：给该领地设 `hidden` 旗帜（`/res set hidden true`），配合 `HideHidden: true` 即地图同步隐藏。
- **只想显示特定领地**：在 `VisibleRegions` 填领地名列表（格式见配置文件注释，支持 `world:<世界名>` 整世界过滤）。
- **边界太淡/太艳**：调 `Border.Color/Opacity/Weight` 与 `Fill.Opacity`。
- **不想显示子领地**：`SubZoneDepth: 0`。
- **想让玩家默认不看到领地分布**：`HideByDefault: true`（玩家点地图左上角图层手动打开）。

## 与 Chunky 配合

本服已装 **Chunky**（区块预生成）。标准流程：

1. 先用 Chunky 预生成常用活动范围（`/chunky start` → 进度 100%）。
2. 再让 Dynmap 全量渲染（`/dynmap fullrender`）→ 地图一次成型，**无需玩家跑图**即可显示完整地形。
3. 日常靠增量渲染更新变化区块。

## 部署方案（本服语境）

本服为 **Leaf + Velocity 代理、外网直连禁**。生产部署地图对外访问必须经反代：

1. 放 Dynmap jar 入 `plugins/`，完整重启。
2. 改 `configuration.txt`：`webserver-bindaddress` 绑 `127.0.0.1`（或内网），`webport` 保持 8123。
3. nginx / Caddy 反代 `127.0.0.1:8123` 对外（示例见 `BlueMap.md` 同款写法，换端口即可）；**切勿**让 8123 裸奔公网。
4. 隐私：关玩家标记或仅内网访问（见上）。
5. 低峰期先 Chunky 预生成，再 `/dynmap fullrender` 全量。

## Dynmap vs BlueMap（本服为何选 BlueMap）

| 维度            | Dynmap                          | BlueMap（本服现行）                   |
| --------------- | ------------------------------- | ------------------------------------- |
| 视图            | 2D 平铺（卫星/地表/洞穴）       | **3D** 立体（可旋转俯仰）             |
| Residence 集成  | **原生内置**（config 一段即开） | 需第三方桥接插件（BlueMap Residence） |
| 资源占用        | 较轻                            | 3D 渲染较重（首渲更久）               |
| 洞穴/红石视图   | 有                              | 无                                    |
| 冲击力/演示效果 | 一般                            | 强                                    |
| 社区生态        | 最老牌                          | 活跃、增长快                          |

> 若后续想两者并存（Dynmap 看洞穴+领地、BlueMap 看 3D），可同时安装——Residence 两个集成互不冲突（Dynmap 用原生段、BlueMap 用桥接插件）。

## 排错

| 现象                      | 排查                                                                                                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| 地图打不开                | 8123 端口是否开放 / 反代是否正确；核对 `webport` 与防火墙                                                                                            |
| 大片空白                  | 区块未渲染或未预生成；先 Chunky 预生成再 `fullrender`                                                                                                |
| 首渲卡服                  | 用 `radiusrender` 限范围、低峰期渲染                                                                                                                 |
| 瓦片体积太大、占盘        | 按上文「体积优化」：降 JPG 质量、关 cave、只渲染主世界、限渲染范围，删旧 tiles 后重渲                                                                |
| Residence 领地不显示      | ① Dynmap 已装并重启；② `plugins/Residence/config.yml` 里 `DynMap.Use: true`；③ 改完执行 `/res reload`；④ 看控制台有没有 Residence 的 Dynmap 注册日志 |
| 个别领地地图上不显示      | 该领地设了 `hidden` 旗帜（`HideHidden: true` 时）或在 `HiddenRegions` 名单里                                                                         |
| 玩家标记不显示 / 暴露隐私 | 核对玩家标记配置与 `dynmap.hide` 权限；要隐藏给对应权限（/lp tree dynmap 核对）                                                                      |
