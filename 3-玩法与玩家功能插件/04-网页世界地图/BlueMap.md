# 6. 网页世界地图 — BlueMap

本文介绍 BlueMap 这款**网页 3D 世界地图**插件：把服务器世界渲染成浏览器里可自由旋转、缩放、平移的 3D 模型，玩家/访客不开游戏也能看地形起伏与建筑立体结构。用于给玩家与访客提供"看世界"的沉浸入口，并把 Residence 领地图层画到地图上。

**当前版本**: BlueMap 5.x（paper 平台 jar，本服取匹配 1.21.11 的 5.16 版本）

**MC 要求**: 1.21.x（含 1.21.11，需 Java 21）

**作者/发布**: BlueColored | **协议**: 开源（详见 [BlueMap GitHub](https://github.com/BlueMap-Minecraft/BlueMap) LICENSE）

**Modrinth**: https://modrinth.com/plugin/bluemap ｜ **Hangar**: https://hangar.papermc.io/BlueMap/BlueMap ｜ **文档**: https://bluemap.bluecolored.de

> 本服选 BlueMap：3D 展示冲击力最强、性能适中、活跃维护、支持 1.21.x 与 Folia；生存社交服想给玩家/访客"哇"一下的世界观感。

## 功能说明

- **3D 网页地图**：浏览器用 Three.js 渲染，鼠标旋转/缩放/平移，保留地形高低、建筑立体结构、水面反光。
- **多世界标签**：主世界 / 下界 / 末地各一个标签，分别渲染。
- **标记系统（Markers）**：放标点、画区域、加文字标签；可通过 Web API 动态更新（例如把 Residence 领地图层画上去）。
- **玩家标记**：可显示在线玩家头像与实时位置（**可关**，涉及隐私见下）。
- **增量渲染**：首次全量后只更新变化区块，日常开销小。
- **资源包方块支持**：自定义方块也能正确显示。
- **CLI 离线模式**：给世界存档文件夹即可离线烘焙成静态站点，丢任意 Web 服务器。
- **与客户端无关**：网页地图是浏览器应用，Java / 基岩玩家都能看。

## 安装与前置

1. 服务端为 **Paper / Purpur**（Leaf 为 Paper 分支，兼容）。
2. 下载 BlueMap **paper 平台** jar（取兼容 1.21.11 的 5.x 版本），放入 `plugins/`，**完整重启**服务端（非 /reload）。
3. 无强制前置依赖（不需 PlaceholderAPI / Vault）。
4. 开放一个额外 TCP 端口给内置 web 服务（默认 **8100**），或用 nginx/Caddy 反代 / 静态托管。

## 玩家如何使用

| 操作                                       | 效果                |
| ------------------------------------------ | ------------------- |
| 浏览器打开地图地址（`http://服务器:8100`） | 查看 3D 世界        |
| 拖拽 / 滚轮                                | 旋转、缩放、平移    |
| 点标记                                     | 看领地名 / 说明文字 |
| （若开玩家标记）看在线头像                 | 实时位置            |

> 查看地图**不需要任何游戏内权限**——它是公开的网页。仅管理命令需要权限（见下）。

## 命令与权限

BlueMap **没有标记命令**（标记靠配置文件或第三方插件 API，见下"领地图层"）。实际命令表（来源：官方 Wiki Commands and Permissions）：

| 命令                                    | 权限节点                                  | 默认 | 说明                                   |
| --------------------------------------- | ----------------------------------------- | ---- | -------------------------------------- |
| `/bluemap`                              | `bluemap.status`                          | op   | 显示渲染状态                           |
| `/bluemap version`                      | `bluemap.version`                         | op   | 版本与系统信息                         |
| `/bluemap help`                         | `bluemap.help`                            | op   | 官方 Wiki / Discord 链接               |
| `/bluemap reload [light]`               | `bluemap.reload` / `bluemap.reload.light` | op   | 重载资源/配置/Web 服务（`light` 更快） |
| `/bluemap maps`                         | `bluemap.maps`                            | op   | 列出已加载地图                         |
| `/bluemap stop` / `start`               | `bluemap.stop` / `bluemap.start`          | op   | 暂停 / 恢复全部渲染（重启后保持）      |
| `/bluemap freeze <map-id>` / `unfreeze` | `bluemap.freeze` / `bluemap.unfreeze`     | op   | 冻结 / 解冻某张地图更新（重启后保持）  |
| `/bluemap purge <map-id>`               | `bluemap.purge`                           | op   | 清空某地图渲染数据（之后自动重渲）     |
| `/bluemap update [map-id] [x z] [半径]` | `bluemap.update`                          | op   | 手动更新（默认全图，只渲变化区块）     |
| `/bluemap tasks` / `tasks cancel`       | `bluemap.tasks` / `bluemap.tasks.cancel`  | op   | 查看 / 取消渲染队列                    |
| `/bluemap troubleshoot [map] [x z]`     | `bluemap.troubleshoot`                    | op   | 排查地图/世界问题并给建议              |

> 权限节点以官方为准；`admin`/`owner` 给 `bluemap.*` 即可。完整命令列表见官方 Wiki（Commands and Permissions）。
> 玩家**查看地图无任何权限要求**（网页匿名访问）；上面只是管理命令。
> 若要让特定玩家/组**不在地图上显示**，调整 `player-markers` 配置或相应权限（`/lp tree bluemap` 核对节点）。

## 配置要点

首次启动生成 `plugins/BlueMap/` 配置，关键文件：

- **`webserver.conf`**：`webserver.bind` + `webserver.port`（默认 **8100**）。需对外可达或走反代。
- **`bluemap.conf`**：`data`（meshes 存储路径，随已探索区块增长）、`render-threads`（渲染线程数，默认 -1 自动）、`compression`、`metrics`。
- **每世界设置**：`map-type`（正常 3D）、`player-markers`（开/关）、`marker-sets`（标记集）。

**隐私设置（重要）**：本服重视玩家隐私（信任传送都需对方同意）。公开网页若开 `player-markers`，等于把全员实时坐标暴露给任何人。建议：

- 关掉 `player-markers`；或
- 仅内网 / 白名单访问地图；或
- 仅对特定世界开启玩家标记。

**首渲很重**：第一次全量渲染大世界可能几十分钟~几小时，且占盘（meshes 随探索增长，几百 MB~数 GB）。务必低峰期执行，并用下面 Chunky 配合减少"跑图"成本。

## 如何只渲染加载主世界?

BlueMap 5.x 是**一张地图 = `plugins/BlueMap/maps/` 下一个 `.conf` 文件**。删掉下界 / 末地的 conf 文件，BlueMap 就不再加载 / 渲染它们，网页上只剩主世界一个标签。

## 领地图层（Residence 集成）——重点

本服重度使用 Residence 圈地，把领地图层画到 3D 地图是 BlueMap 的核心加分项。

> **关键差异（对比 Dynmap）**：Residence 对 **Dynmap 是原生内置集成**（config 一段即开），但**没有原生 BlueMap 集成**（Residence 6.0.0.1 的 config 只含 DynMap / Pl3xMap 两段）。BlueMap 侧需装第三方桥接插件 **BlueMap Residence**（SpigotMC 资源 107389，作者 CZMixer），或自写 marker API 脚本。**本服目前 BlueMap 已部署但桥接插件尚未安装**（见文末「部署状态·待办」）。

### 方案一（推荐）：BlueMap Residence 桥接插件

**BlueMap Residence 3.1.1**（截至 2024-11-10 最新）——3D Residence viewer integration for BlueMap：

- **要求**：已装 BlueMap + Residence（本服已满足）。
- **兼容性**：作者标注 Native MC 1.13、Tested 1.13~1.20；2.9.0 起支持 MC 1.21、2.6.0 起支持 Folia、2.6.1 起支持 Luminol 等 Folia 分支。**对 Leaf 1.21.11 的兼容需实测**（BlueMap API 向后兼容性较好，但 1.21.11 属较新版本线，装后看启动日志确认）。
- **功能**：自动更新（按周期 + 监听 Residence 变更事件即时刷新）、可自定义点击详情（领主名/旗帜列表，支持 HTML）、自定义标记集名、3D 与 2D 两种标记形态、可单独配置出售/出租领地的专属配色、PlaceholderAPI 支持、Folia 支持。
- **注意性能**：作者实测 7000+ 领地会产生明显延迟；领地极多的服把 `update.period` 调大（如 600+ 秒）并视情况关 `onchange`。

**安装步骤**

1. 从 [SpigotMC BlueMap Residence](https://www.spigotmc.org/resources/bluemap-residence.107389/) 下载 `BlueMap-Residence-3.1.1.jar` 放入 `plugins/`。
2. **完整重启**服务器（非 /reload）——让 BlueMap 与桥接插件依次加载、注册标记集。
3. 首次启动生成 `plugins/BlueMap_Residence/config.yml`，按下方说明调整。
4. 浏览器打开地图 → 左侧菜单出现 **"Residences"** 标记集 → 打开即见全部领地（默认样式：红色描边 + 半透明红色填充）。

**命令与权限**

| 命令                                                 | 权限节点              | 默认 | 说明                           |
| ---------------------------------------------------- | --------------------- | ---- | ------------------------------ |
| `/bluemapresidence`（别名 `blueres` / `bluemapres`） | `blueres.reload`      | op   | 重载插件配置并重建全部领地标记 |
| （加入服务器时的更新检查提示）                       | `blueres.updatecheck` | op   | 新版本检测提示                 |

**配置详解**（`plugins/BlueMap_Residence/config.yml`，全部可改，注释为中文说明）：

```yaml
update:
  # 每隔多少秒检查一次 Residence 数据变化并刷新标记（300 = 5 分钟）
  period: 300
  # true = 按上面周期定时刷新
  onperiod: true
  # true = 监听 Residence 事件，领地一变（创建/删除/改名/转手）立即刷新
  onchange: true

marker:
  # 地图左侧标记集名称（显示为菜单条目）
  name: 'Residences'
  # 点击领地时弹出的详情文字。
  # 占位符：[ResName] 领地名、[OwnerName] 领主名；支持基础 HTML（<b>粗体、<br>换行）
  detail: '<b>[ResName]</b> owned by [OwnerName]<br><br><b>Flags:</b><br>'
  # 详情里最多显示多少个旗帜；0 = 不显示旗帜，-1 = 不限
  maxFlags: -1
  # 每条旗帜的显示格式，占位符：[FlagKey] 旗帜名、[FlagValue] 旗帜值
  flagDetail: '[FlagKey]: [FlagValue]<br>'
  # 标记形态：rectangle（3D 立体矩形，默认）/ circle / ellipse / point
  #               2D 平面：rectangle2d / circle2d / ellipse2d
  type: 'rectangle'
  # 仅 2D 标记有效：标记贴在地图上的高度（Y 坐标）
  Yheight: 60
  # true = point 标记的高度取领地中心 Y；false = 取上面的 Yheight
  centerPointerMarkerHeight: true
  # true = 高地势地形会遮挡标记；false = 标记永远在最上层（2D 标记推荐）
  depth-test: true
  # 圆形/椭圆形标记的取点数（越大越圆滑，性能略增）
  points: 100
  # 边界线颜色（RGBA 0-255）
  LineColor: { r: 255, g: 0, b: 0, a: 1.0 }
  # 填充色（RGBA；a 是透明度，0.3 = 半透明）
  FillColor: { r: 200, g: 0, b: 0, a: 0.3 }
  # 边界线粗细（像素）
  LineWidth: 3
  # 仅 point 类型：图标 URL 与锚点偏移
  icon:
    url: 'https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg'
    anchorX: 25
    anchorY: 45

# 子领地单独一套配置（与上面 marker 相同字段；想区分主领地/子领地外观就改这里）
subzone:
  name: 'Residences'
  detail: '<b>[ResName]</b> owned by [OwnerName]<br><br><b>Flags:</b><br>'
  maxFlags: -1
  flagDetail: '[FlagKey]: [FlagValue]<br>'
  type: 'rectangle'
  Yheight: 60
  centerPointerMarkerHeight: true
  depth-test: true
  points: 100
  LineColor: { r: 255, g: 0, b: 0, a: 1.0 }
  FillColor: { r: 200, g: 0, b: 0, a: 0.3 }
  LineWidth: 3
  icon:
    url: 'https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg'
    anchorX: 25
    anchorY: 45

# 出售中的领地单独一套配置（如绿色填充方便玩家找地买房）
marker-For_Sale:
  detail: '<b>[ResName]</b> owned by [OwnerName]<br><br><b>Flags:</b><br>'
  maxFlags: -1
  flagDetail: '[FlagKey]: [FlagValue]<br>'
  type: 'rectangle'
  Yheight: 60
  centerPointerMarkerHeight: true
  depth-test: true
  points: 100
  LineColor: { r: 255, g: 0, b: 0, a: 1.0 }
  FillColor: { r: 200, g: 0, b: 0, a: 0.3 }
  LineWidth: 3
  icon:
    url: 'https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg'
    anchorX: 25
    anchorY: 45

# 出租中的领地单独一套配置
marker-For_Rent:
  # 字段同上（detail/maxFlags/flagDetail/type/Yheight/depth-test/points/LineColor/FillColor/LineWidth/icon）
```

**接入要点与调优**

- **配色建议（贴合本服经济玩法）**：普通领地默认红边红填充即可；给 `marker-For_Sale` 配绿色（`FillColor: {r: 0, g: 200, b: 0, a: 0.3}`）、`marker-For_Rent` 配蓝色，让玩家一眼分辨"可买 / 可租 / 普通"。
- **领地 hidden 联动**：BlueMap Residence 读取 Residence 数据时遵循 `hidden` 旗帜——游戏里 `/res set hidden true` 的领地不会出现在地图上（隐私友好）。如需"地图强制显示"，只能排除该插件或改 Residence 端隐藏逻辑。
- **改动后刷新**：改完 config 执行 `/bluemapresidence`（reload）；日常领地变动由 `onchange: true` 即时刷新。
- **中文详情**：`detail`/`flagDetail` 支持中文与 HTML，可直接改成中文模板（如 `"<b>[ResName]</b> 领主：[OwnerName]<br><br><b>旗帜：</b><br>"`）。

### 方案二：BlueMap 原生 marker API / 静态标记（备选）

不装桥接插件，直接在 BlueMap 各世界配置 `maps/world.conf` 末尾追加静态 `marker-sets`（BlueMap 官方 Markers 语法，动态数据不适合）——适合"少量固定地标"，不适合全量领地：

```yaml
marker-sets: {
  residence-example: {
    label: "Residence 示例"
    toggleable: true
    default-hidden: false
    markers: {
      example-shape: {
        type: "shape"
        position: { x: 0, y: 64, z: 0 }
        shape: [ { x: 0, z: 0 }, { x: 16, z: 0 }, { x: 16, z: 16 }, { x: 0, z: 16 } ]
        shape-y: 64
        label: "示例领地"
        detail: "这是静态手写标记"
        line-color: { r: 255, g: 0, b: 0, a: 1.0 }
        fill-color: { r: 200, g: 0, b: 0, a: 0.3 }
      }
    }
  }
}
```

> 全量领地用这个方案不现实（领地会持续增删改，需每次手改 config + 重载）。真要自研动态方案，需用 BlueMap API（`bluecolored.bluemap.api`）写一个读取 Residence API 数据的 Java 插件，量级等同方案一，不如直接用现成的 BlueMap Residence。

### 三方案怎么选

| 维度                    | 方案一 BlueMap Residence 插件 | 方案二 静态 marker-sets | Dynmap 原生集成（对照）           |
| ----------------------- | ----------------------------- | ----------------------- | --------------------------------- |
| 动态跟随领地变化        | ✅（周期+事件）               | ❌（纯静态）            | ✅（原生）                        |
| 安装成本                | 一个 jar + 重启               | 手写 config             | 装 Dynmap + Residence 开 Use      |
| 3D 显示                 | ✅ 立体矩形                   | ✅（shape/extrude）     | 仅 2D 平面（可 3dRegions 立体块） |
| 自定义配色/出售出租区分 | ✅ 三套独立配置               | 手写每个标记            | ✅（Fill 多色）                   |
| 领地量大的性能          | 需调大 period                 | 无动态开销              | 原生、较轻                        |
| 1.21.11 兼容风险        | 需实测（1.21 起支持）         | 无                      | 需实测（3.x 版本线）              |

> **实务建议**：本服已定 BlueMap，直接采用**方案一**（BlueMap Residence 插件），装后实测 1.21.11 兼容；领地数量中等（百~千级）性能无忧。若日后发现该插件不兼容或想加"洞穴视图 + 原生领地"，可并行装 Dynmap（Residence 原生段零成本开）——两个地图互不冲突。

## 与 Chunky 配合

本服已装 **Chunky**（区块预生成）。标准流程：

1. 先用 Chunky 预生成常用活动范围（`/chunky start` → 进度 100%）。
2. 再让 BlueMap 全量渲染 → 地图一次成型，**无需玩家跑图**即可显示完整地形。
3. 日常靠增量渲染更新变化区块。

## 部署方案（两种路径）

BlueMap 有两种典型落地方式，按"地图谁来渲染、谁来访问"决定：

- **A. 云端服务器部署**：地图在游戏服务器本机（或同机/同内网另一进程）实时渲染，玩家直接访问服务器 IP/域名。
- **B. 本地电脑部署**：把云端存档下载到本机，在本机渲染。又分两种子模式——**B1 插件模式**（本机跑一个测试服加载 BlueMap 插件，随开随渲）与 **B2 CLI 离线渲染**（不跑服务端，用 BlueMap CLI 把存档烘焙成静态站点，丢任意 Web 服务器）。

> 本服为 **Leaf + Velocity 代理、外网直连禁、仅 127.0.0.1:55561 对内**。生产部署地图对外访问必须经反代（见各方案端口段），不要让 8100 直接裸奔公网。隐私见上文「配置要点·隐私设置」。

### A. 云端服务器部署（生产推荐）

地图随游戏服务器运行，玩家开浏览器即看最新世界。

**步骤**

1. 取兼容 1.21.11 的 `bluemap-5.16-paper.jar` 放入服务端 `plugins/`，**完整重启**（非 /reload）。
2. 首启生成 `plugins/BlueMap/`。编辑 `core.conf` 把 `accept-download: false` → `true`（接受 Mojang EULA，允许下载 `minecraft-client-1.21.11.jar`，约 31 MB，否则 Web 不工作）。
3. 编辑 `webserver.conf`：
   - `webserver.port`：默认 `8100`（若被占用改别的）。
   - `webserver.bind`：默认 `0.0.0.0`。**生产不要裸奔**——建议绑内网/回环，由下列反代对外。
4. 编辑每世界 `map-<world>.conf`：`player-markers` 按隐私策略设 `false`（本服默认关）或仅特定世界开。
5. 低峰期先 `Chunky` 预生成活动范围，再让 BlueMap 全量渲染（首渲几十分钟~数小时，占盘随探索增长）。
6. 对外暴露：用 **nginx / Caddy 反代** `127.0.0.1:8100`，或把 `bluemap/web/` 作为静态站点托管；**切勿**直接把 8100 开到公网。

**nginx 反代示例**

```nginx
server {
    listen 443 ssl;
    server_name map.your-server.com;

    location / {
        proxy_pass http://127.0.0.1:8100;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

> 反代场景下 `webserver.bind` 绑 `127.0.0.1` 即可，无需公网监听。如需访问控制（白名单/登录），在反代层加 basic auth 或 WAF。

**权限**（仅管理命令需授权，查看网页匿名）：`admin`/`owner` 给 `bluemap.*`。

**优点**：实时、玩家零门槛、增量渲染自动更新。
**注意**：首渲吃 CPU/盘；`player-markers` 隐私；8100 不要裸公网。

---

### B. 本地电脑部署（下载云端存档）

适合：你（服主/管理）想在本机看地图、做展示、离线出图，或给美术/文案导出截图，**不占用服务器资源**。

**前置**

- 从云端服务器把世界存档拉到本机。本服存档位于游戏服务端 `world/` `world_nether/` `world_the_end/`（含 `region/` `.mca` 与 `level.dat`）。拉取方式任选：
  - 服务器面板/文件管理打包下载（zip）；
  - `rsync` / `scp` 增量同步（`rsync -avz user@server:/path/world/ ./world/`）；
  - 关服后整目录复制，避免读 half-written 区块。
- 本机装 **Java 21**（BlueMap 5.x 要求）。
- 下载 BlueMap 发行包（[Modrinth](https://modrinth.com/plugin/bluemap) / [Hangar](https://hangar.papermc.io/BlueMap/BlueMap)）：`bluemap-5.16-paper.jar`（插件模式用）或独立 `BlueMap` 压缩包里的 CLI（`bluemap-cli.jar`，离线渲染用）。

#### B1. 插件模式（本机跑测试服随开随渲）

与云端部署几乎一致，只是服务器在本机。已在本机测试服实测通过（见「部署状态」）。

**步骤**

1. 把下载的云端存档放到本机测试服目录（如 `C:\mc_serve\1.21.11-test\world*`）。注意 `server.properties` 的 `level-name` 与存档目录名一致。
2. 放 `bluemap-5.16-paper.jar` 入 `plugins/`，完整启动测试服。
3. 改 `core.conf` `accept-download: true`（首部署必改，允许下载 client.jar）。
4. 浏览器开 `http://127.0.0.1:8100/` 即可看（已验证 HTTP 200、三世界瓦片正常生成）。
5. 本机调试/展示完，直接 `stop` 关服即停渲染，**不影响线上服务器**。

**优点**：和正式环境完全一致、随开随渲、增量更新、零额外工具。
**适用**：服主本机预览、给朋友演示、临时出图。
**注意**：仍是跑一个服务端，占本机内存/CPU；存档需先下载到本机。

#### B2. CLI 离线渲染（不跑服务端，烘焙静态站点）

不启动 Minecraft 服务端，用 **BlueMap CLI（`BlueMap-cli.jar`，独立 jar，非插件 jar）** 直接读存档文件夹，离线烘焙成静态地图，丢任意 Web 服务器（甚至 GitHub Pages / 对象存储）。命令以官方为准（`java -jar BlueMap-cli.jar -h` 看全部 flag）。

**步骤**

1. 准备 BlueMap CLI：从 [BlueMap 发布页](https://modrinth.com/plugin/bluemap/versions) 取 **`BlueMap-cli.jar`**（含依赖，Java 21）。建一个工作目录（如 `D:\mc_maps\bluemap-cli\`）放 jar。
2. 准备存档：把云端下载的 `world/` `world_nether/` `world_the_end/` 放到本机某目录（如 `D:\mc_maps\saves\`）。
3. 生成配置：在该目录跑一次 `java -jar BlueMap-cli.jar`，会自动生成 `config/`（`core.conf` `webserver.conf` 与各 `map-<world>.conf`）。
4. 编辑 `config/core.conf`：`accept-download: true`（接受 Mojang EULA，首次自动下载 `minecraft-client-1.21.11.jar` 约 31 MB，离线渲染必需）。
5. 编辑各 `map-<world>.conf`，把 `world` 指向**存档路径**而非游戏服目录：
   - `world:` `D:\mc_maps\saves\world`
   - `world-nether:` `D:\mc_maps\saves\world_nether`
   - `world-the-end:` `D:\mc_maps\saves\world_the_end`
   - `player-markers: false`（离线无玩家，本就可关）。
6. 离线渲染（`-r` = render，`-c` 指定配置目录）：

   ```bat
   java -jar BlueMap-cli.jar -r -c D:\mc_maps\bluemap-cli\config
   ```

   渲染产出的静态站点在 `config/../web/`（即 `bluemap/` 同级的 `web/` 目录，含 `maps/<world>/tiles/`）。

7. 预览 / 托管，二选一：
   - 本地预览：跑 `java -jar BlueMap-cli.jar -w -c <config目录>` 起内置 WebServer（`http://127.0.0.1:8100`）；
   - 正式托管：把 `web/` 目录丢进 **nginx / Caddy / 对象存储 / GitHub Pages**，无需 Java，纯静态。
8. 增量更新：云端存档有变动时，重新拉取变更区块，再跑一次 `-r`（meshes 按修改时间增量，未变区块跳过）。

> 常用 flag（CLI）：`-r` 渲染、`-w` 起 WebServer、`-f` 强制全量重渲、`-u` 监听文件变化自动更新、`-m world,world_nether` 只渲指定地图、`-g` 重新生成 webapp。详见 `BlueMap-cli.jar -h` 或 [BlueMap CLI 文档](https://bluemap.bluecolored.de/wiki/getting-started/Installation.html)。

**优点**：**不跑服务端、不吃游戏服资源**、纯静态可长期托管、适合对外公开只读地图。
**适用**：对外发布世界地图、存档归档可视化、CI 自动出图。
**注意**：离线渲染不与在线玩家状态联动（本就无玩家标记）；每次更新需重新拉存档+重渲；首渲同样吃 CPU/盘；必须用 `BlueMap-cli.jar` 而非插件 jar。

---

### 两种路径怎么选

| 维度         | A 云端部署     | B1 本机插件      | B2 本机 CLI       |
| ------------ | -------------- | ---------------- | ----------------- |
| 是否跑游戏服 | 是（线上）     | 是（本机测试服） | 否                |
| 实时性       | 实时增量       | 随本机服更新     | 手动重渲          |
| 占用线上资源 | 是             | 否（占本机）     | 否                |
| 对外访问     | 反代后公网     | 仅本机           | 静态托管可公网    |
| 适合场景     | 玩家日常看地图 | 服主预览/演示    | 对外发布/归档     |
| 复杂度       | 中（反代）     | 低               | 中（写 CLI 配置） |

> 实务建议：生产用 **A**（玩家实时看），对外只读展示/存档可视化用 **B2**（不占服资源），服主本机想随手看用 **B1**。

## 基岩版注意事项

- 网页地图与游戏客户端无关，基岩玩家用手机/电脑浏览器同样能看 3D 世界。
- 仅"玩家标记"实时位置依赖服务端上报，双端在线玩家都会显示（隐私设置同上）。

## 排错

| 现象                      | 排查                                                                  |
| ------------------------- | --------------------------------------------------------------------- |
| 地图打不开                | 端口是否开放 / 反代是否正确；核对 `webserver.port` 与防火墙           |
| 大片空白                  | 区块未渲染或未预生成；先 Chunky 预生成再全量渲染                      |
| 首渲卡服                  | `render-threads` 调低、低峰期渲染，或用 `radiusrender` 限制范围       |
| 玩家标记不显示 / 暴露隐私 | 核对 `player-markers` 配置；要隐藏给对应权限（/lp tree bluemap 核对） |
| 更新慢 / 不动             | 确认未处于 `freeze` 状态；增量渲染需区块实际变化才更新                |

## 部署状态（已验证 ✅）

- **已在测试服 `C:\mc_serve\1.21.11-test` 完成部署验证。**
- 验证环境与结果：
  1. **jar 加载**：`bluemap-5.16-paper.jar` 放入 `plugins/`，Leaf 1.21.11 在 36 插件列表中正常 `[BlueMap] Enabling BlueMap v5.16`。
  2. **web 端口可达**：内置 WebServer `bound to all network interfaces on port 8100` 并 `started`；`http://127.0.0.1:8100/` 探测返回 `HTTP 200`、页面标题 `BlueMap`。
  3. **首渲产出**：资源自动下载 `minecraft-client-1.21.11.jar`（约 31 MB）并 `Resources loaded`；`world` / `world_nether` / `world_the_end` 三张地图均 `Loading map` 成功，已生成大量 `.prbm.gz` 瓦片（位于 `bluemap/web/maps/<world>/tiles/`），证明 3D 渲染正常推进。
  4. **关键配置**：`BlueMap/core.conf` 的 `accept-download` 已从 `false` 改为 **`true`**（接受 Mojang EULA，允许 BlueMap 下载 3D 渲染所需 client.jar），否则 Web 无法工作。这是离线/首次部署必改项。
- 待生产环境后续确认项（测试服已具备基础条件）：
  - 首次全量渲染耗时与 TPS 影响（建议配合 Chunky 预生成后再全量，降低跑图成本）。
  - **Residence 领地图层（待装）**：本服已装 BlueMap 5.16，但 **BlueMap Residence 桥接插件尚未安装**——按上文「方案一」装 `BlueMap-Residence-3.1.1.jar` 后完整重启，实测对 1.21.11 的兼容与领地标记渲染。
  - 玩家标记隐私：测试服 `player-markers` 保持关闭，生产如需开启再评估是否仅内网可见。
- 部署版本：BlueMap **5.16-paper**（兼容 1.21.11）。
