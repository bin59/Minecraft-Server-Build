# 17. 网页世界地图 — BlueMap

本文介绍 BlueMap 这款**网页 3D 世界地图**插件：把服务器世界渲染成浏览器里可自由旋转、缩放、平移的 3D 模型，玩家/访客不开游戏也能看地形起伏与建筑立体结构。本服于 **2026-09-19 确定采用**，用于给玩家与访客提供"看世界"的沉浸入口，并把 Residence 领地图层画到地图上。

**当前版本**: BlueMap 5.x（paper 平台 jar；v5.23 起覆盖 26.1–26.2，向下兼容 1.21.x；本服取匹配 1.21.11 的版本）

**MC 要求**: 1.21.x（含 1.21.11，需 Java 21）

**作者/发布**: BlueColored | **协议**: 开源（详见 [BlueMap GitHub](https://github.com/BlueMap-Minecraft/BlueMap) LICENSE）

**Modrinth**: https://modrinth.com/plugin/bluemap ｜ **Hangar**: https://hangar.papermc.io/BlueMap/BlueMap ｜ **文档**: https://bluemap.bluecolored.de

> 替代方案曾考虑 Squaremap（最轻量 2D 俯视）、Pl3xMap（2D 可美化）、Dynmap（老牌重型 2D+overlay 生态）。本服选 BlueMap：3D 展示冲击力最强、性能适中、活跃维护、支持 1.21.x 与 Folia；生存社交服想给玩家/访客"哇"一下的世界观感。见对话选型对比（网页地图插件四款横评）。

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

| 操作 | 效果 |
|---|---|
| 浏览器打开地图地址（`http://服务器:8100`） | 查看 3D 世界 |
| 拖拽 / 滚轮 | 旋转、缩放、平移 |
| 点标记 | 看领地名 / 说明文字 |
| （若开玩家标记）看在线头像 | 实时位置 |

> 查看地图**不需要任何游戏内权限**——它是公开的网页。仅管理命令需要权限（见下）。

## 命令与权限

| 命令 | 权限节点（参考） | 默认 | 说明 |
|---|---|---|---|
| `/bluemap` | `bluemap.command` | op | 主命令 / 帮助 / 版本 |
| `/bluemap reload` | `bluemap.command.reload` | op | 重载配置 |
| `/bluemap stop` / `start` | `bluemap.command.stop` / `bluemap.command.start` | op | 暂停 / 恢复**全部**渲染（重启后仍保持） |
| `/bluemap freeze <地图>` / `unfreeze` | `bluemap.command.freeze` / `bluemap.command.unfreeze` | op | 冻结 / 解冻某张地图的更新（重启后仍保持） |
| `/bluemap marker ...` | `bluemap.command.marker` | op | 增删改标记（领地图层等） |
| `/bluemap version` | `bluemap.command.version` | op | 版本信息 |

> ⚠️ **权限节点以插件实际注册为准**：装好后跑 `/lp tree bluemap` 核对确切节点名，再按组授权（admin/owner 给 `bluemap.command.*` 即可）。
> 玩家**查看地图无任何权限要求**（网页匿名访问）；上面只是管理命令。
> 若要让特定玩家/组**不在地图上显示**，调整 `player-markers` 配置或相应权限（确切节点用 `/lp tree bluemap` 核对）。

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

## 领地图层（Residence 集成）

本服重度使用 Residence 圈地，把领地图层画到地图是采用 BlueMap 的加分项：

- BlueMap 提供 **marker API**，可用少量代码读取 Residence 数据生成区域标记（多边形 + 领主名标签）。
- 也可找社区插件（如 BlueMap 系 Residence 集成）自动渲染领地图层；**部署前核对其对 1.21.11 的兼容**。
- 玩家在网页直接看领地分布，"我的领地在哪"一目了然。

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

**权限**（仅管理命令需授权，查看网页匿名）：装好后跑 `/lp tree bluemap` 核对节点，`admin`/`owner` 给 `bluemap.command.*`。

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

与云端部署几乎一致，只是服务器在本机。已在本机测试服 `C:\mc_serve\1.21.11-test` 实测通过（见「部署状态」）。

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

| 维度 | A 云端部署 | B1 本机插件 | B2 本机 CLI |
|---|---|---|---|
| 是否跑游戏服 | 是（线上） | 是（本机测试服） | 否 |
| 实时性 | 实时增量 | 随本机服更新 | 手动重渲 |
| 占用线上资源 | 是 | 否（占本机） | 否 |
| 对外访问 | 反代后公网 | 仅本机 | 静态托管可公网 |
| 适合场景 | 玩家日常看地图 | 服主预览/演示 | 对外发布/归档 |
| 复杂度 | 中（反代） | 低 | 中（写 CLI 配置） |

> 实务建议：生产用 **A**（玩家实时看），对外只读展示/存档可视化用 **B2**（不占服资源），服主本机想随手看用 **B1**。

## 基岩版注意事项

- 网页地图与游戏客户端无关，基岩玩家用手机/电脑浏览器同样能看 3D 世界。
- 仅"玩家标记"实时位置依赖服务端上报，双端在线玩家都会显示（隐私设置同上）。

## 排错

| 现象 | 排查 |
|---|---|
| 地图打不开 | 端口是否开放 / 反代是否正确；核对 `webserver.port` 与防火墙 |
| 大片空白 | 区块未渲染或未预生成；先 Chunky 预生成再全量渲染 |
| 首渲卡服 | `render-threads` 调低、低峰期渲染，或用 `radiusrender` 限制范围 |
| 玩家标记不显示 / 暴露隐私 | 核对 `player-markers` 配置；要隐藏给对应权限（/lp tree bluemap 核对） |
| 更新慢 / 不动 | 确认未处于 `freeze` 状态；增量渲染需区块实际变化才更新 |

## 部署状态（已验证 ✅）

- **2026-09-19 已在测试服 `C:\mc_serve\1.21.11-test` 完成部署验证。**
- 验证环境与结果：
  1. **jar 加载**：`bluemap-5.16-paper.jar`（5,710,626 字节，Modrinth 校验 1.21.11 兼容）放入 `plugins/`，Leaf 1.21.11-174 在 36 插件列表中正常 `[BlueMap] Enabling BlueMap v5.16`。
  2. **web 端口可达**：内置 WebServer `bound to all network interfaces on port 8100` 并 `started`；`http://127.0.0.1:8100/` 探测返回 `HTTP 200`、页面标题 `BlueMap`、`CONTENTLEN=1650`。
  3. **首渲产出**：资源自动下载 `minecraft-client-1.21.11.jar`（约 31 MB）并 `Resources loaded`；`world` / `world_nether` / `world_the_end` 三张地图均 `Loading map` 成功，已生成大量 `.prbm.gz` 瓦片（位于 `bluemap/web/maps/<world>/tiles/`），证明 3D 渲染正常推进。
  4. **关键配置**：`BlueMap/core.conf` 的 `accept-download` 已从 `false` 改为 **`true`**（接受 Mojang EULA，允许 BlueMap 下载 3D 渲染所需 client.jar），否则 Web 无法工作。这是离线/首次部署必改项。
- 待生产环境后续确认项（测试服已具备基础条件）：
  - 首次全量渲染耗时与 TPS 影响（建议配合 Chunky 预生成后再全量，降低跑图成本）。
  - Residence 领地图层集成插件对 1.21.11 的兼容（另行核选）。
  - 玩家标记隐私：测试服 `player-markers` 保持关闭，生产如需开启再评估是否仅内网可见。
- 部署版本：BlueMap **5.16-paper**（兼容 1.21.11；v5.23+ 起支持 26.x，向下覆盖 1.21.x，可后续升级）。
