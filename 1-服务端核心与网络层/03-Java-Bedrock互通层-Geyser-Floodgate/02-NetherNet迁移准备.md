# 3.2 NetherNet 迁移准备（Geyser）

> 适用：本服 **Leaf 单后端 + Geyser-Spigot + Floodgate** 架构（南瓜国际服 / 南瓜生存服）。
> 来源：GeyserMC 官方博客《Preparing Your Geyser Server for NetherNet》（2026-10-08）+ 官方系统属性/配置文档 + 本服真实 `config.yml` 核对。
> 关联文档：[01-Geyser-Spigot.md](./01-Geyser-Spigot.md)
> 本服环境：**Windows 服务端，无 Caddy，暂未绑定域名**。

## 0. 截止时间（务必先看）

- **Bedrock 26.60 预计 2026-10-27 发布，RakNet 正式停用。**
- 该版本起，所有基岩版客户端**只能走 NetherNet**，不再支持 RakNet。
- Geyser **≥ build 1237** 支持 NetherNet，但默认仍是 RakNet，**需手动开启**。
- 26.30 支持会在加入 26.60 后被移除 → 老客户端请尽快更新。
- ⚠️ **10-27 前未切换的服，基岩玩家将全部无法加入。**

## 1. NetherNet 做了什么改变

| 项目 | RakNet（旧） | NetherNet（新，26.60 起强制） |
|---|---|---|
| 连接路径 | 基岩客户端直接 UDP 连服务器 | 先发 **TCP 信令(signaling)** 问"怎么连"，回 WebRTC(UDP) 信息后再加入 |
| 端口需求 | 仅 UDP（游戏端口） | 信令需要 1 个 TCP 端口（内置信令默认复用 bedrock 端口）；或交给外部信令服务 |
| MOTD / 人数 | 直连获取 | 改走信令获取 |
| 信任提示 | 无 | 信令走明文 HTTP 时，首次进服弹 **"trust on first use"**（用 HTTPS 或 Warden 则不弹） |

## 2. 本服真实配置与已踩坑（已核对生产服备份 + 本地测试服）

```yaml
bedrock:
  address: 0.0.0.0
  port: 19132                # 基岩端口，与 Java 端口解耦（独立端口，避免冲突）
  webrtc-port: 0             # 默认跟随 port（WebRTC 媒体复用同一 UDP 端口）
  transport: nethernet       # 已经开着（≥1237）
  clone-remote-port: false
  signaling:
    mode: builtin            # 内置信令（默认会在 19132 上监听 TCP 做握手）
    builtin:
      https:
        certificate: /path/to/fullchain.pem   # ⚠️ Geyser 默认占位符，文件不存在
      advertise-addresses: []                 # ⚠️ 云主机/NAT 场景需填公网地址
```

迁移其实基本配好了（`transport: nethernet` 已开、端口独立）。但有两个实测坑：

**坑① 内置信令起不来的"真因"——不是端口被占，是占位符证书**
`builtin.https.certificate` 默认是 Geyser 自带的占位符 `/path/to/fullchain.pem`，**该文件不存在**。builtin 模式启动时会尝试加载它来开 HTTPS → 证书读取失败 → 整个信令服务起不来，报：
```
[Geyser-Spigot] Built-in signaling could not start. Make sure no other program is using TCP port 19132.
```
注意提示语说"端口被占"是**误导**——实际端口可能是空的（换端口、netstat 查不到占用依然报这个错，就是因为证书炸了）。修法见第 4.2 节。

**坑② WebRTC 握手超时——UDP 没放 / NAT 广播内网地址**
信令握上了，但基岩玩家卡在"连接中"最终超时：
```
juice: Send failed, errno=10051        # 10051 = 网络不可达
NetherNetServerChannel ... timed out during handshake (30s)
```
根因（信令通了，但游戏 WebRTC UDP 媒体连不上）：
1. 安全组只开了 **TCP 信令端口**，没开 **UDP 游戏端口**（`webrtc-port: 0` 复用基岩 UDP 端口）。
2. 云主机/NAT 后 `advertise-addresses: []` 广播的是**内网地址**，客户端拿到连不上的地址 → ICE 失败。需显式填公网地址（见第 4.2 节）。

## 3. 方案选择（按"Windows / 无 Caddy / 无域名"约束）

### ✅ 方案 A（强烈推荐）：Warden 外部信令（nxs），零端口/证书/域名

- 改动：`signaling.mode: builtin` → `nxs`。
- Geyser 不再监听 TCP 端口，改走 `https://agent.warden.cloud`（第三方免费服务，无需账号）。
- **玩家加入地址不变**（仍 `IP:19132`），Warden 自带 HTTPS → **无信任提示**，也不需要域名/证书/新端口。
- 代价：依赖第三方 Warden；MOTD/人数改为周期性更新（非实时）；若国内访问 Warden 慢/被墙，握手会卡。

### 🟡 方案 B：坚持内置信令（builtin），需解决证书占位 + TCP 端口 + NAT

- 把占位符证书置空（见 4.2），确保 TCP 信令端口可用；云主机必须填 `advertise-addresses` 公网地址。
- 无证书 → 玩家首连弹一次信任提示（点允许即可，功能正常）。

### ⚪ 方案 C（HTTPS 去提示）：仅在有域名时考虑

- 需域名 + Let's Encrypt 证书（Windows 用 win-acme 申请 PEM）+ Geyser 内置证书配置。
- 本服当前无域名，**暂不做**。详见第 5 节。

> 临时兼容可用 `transport: both`（<26.40 走 RakNet、≥26.40 走 NetherNet），但需给 NetherNet 单独 `webrtc-port`（≠ 基岩端口）；26.60 全量后切回 `nethernet`。

## 4. 落地配置

### 4.1 方案 A — Warden（推荐，最小改动）

只改 `signaling.mode` 一行，其余段（java/motd/gameplay/advanced）**全部保持不变**：

```yaml
bedrock:
  address: 0.0.0.0
  port: 19132
  webrtc-port: 0
  transport: nethernet
  clone-remote-port: false
  signaling:
    mode: nxs                 # 原来是 builtin，改成 nxs
    nxs:
      endpoint: https://agent.warden.cloud   # 默认值，一般无需改
      token: ''                                # 留空即可，无需注册账号
```

> 升级 Geyser 到 ≥ build 1237 后，备份原 `config.yml`，套用上述片段，重启 Leaf 即可。

### 4.2 方案 B — 内置信令（需处理证书占位 + NAT 广播）

**① 把占位符证书置空（关键）**——这是 builtin 起不来的真因：
```yaml
bedrock:
  signaling:
    mode: builtin
    builtin:
      https:
        certificate: ''      # 原来是 /path/to/fullchain.pem 占位符，置空即只跑明文 HTTP 信令
        private-key: ''
```
置空后重启，不再报 `could not start`。

**② 独立信令端口（可选，避开端口冲突）**：若 TCP 信令端口确被占用，启动加参数并放行对应 TCP：
```bash
java -DgeyserTransport=nethernet -DgeyserSignalingPort=19134 -jar leaf-1.21.11.jar
```
玩家仍用 UDP 19132 加入，需额外放行 TCP 19134。

**③ 云主机 / NAT 必须广播公网地址**（否则 WebRTC 握手超时）：
```yaml
bedrock:
  signaling:
    builtin:
      advertise-addresses:
        - "<公网IP>:19132"   # 玩家实际用来连的公网地址:端口，不要留空（留空会广播内网地址）
```
> ⚠️ 本地局域网测试时，这里应填本机 LAN IP（如 `192.168.x.x:19132`），不要填云端公网 IP，否则本地基岩客户端拿到连不上的地址。

### 4.3 防火墙 / 安全组

- **方案 A（nxs）**：无需新增。游戏 UDP 19132 原本已放行（RakNet 时代就在用）。
- **方案 B（builtin）**：**必须同时放行 TCP + UDP 的 19132**（或你指定的独立信令端口）。**只开 UDP 会导致信令(TCP)失败；只开 TCP 会导致 WebRTC 媒体(UDP)超时。**

Windows 防火墙（PowerShell 管理员）示例：
```powershell
New-NetFirewallRule -DisplayName "Geyser NetherNet TCP" -Direction Inbound `
  -Protocol TCP -LocalPort 19132 -Action Allow
New-NetFirewallRule -DisplayName "Geyser NetherNet UDP" -Direction Inbound `
  -Protocol UDP -LocalPort 19132 -Action Allow
```

### 4.4 关于域名 / 备案（重要澄清）

- **用裸 IP:端口加服不需要 ICP 备案。** 备案只针对"在国内服务器上用域名建网站"；Minecraft 玩家直接填 IP:端口连接，不涉及域名，无需备案。
- 本服完全可以不用域名就把 NetherNet 跑起来（方案 A 或 B 都不需要域名）。
- 域名 + HTTPS 唯一目的是**消除那一次"信任提示"**，纯体验优化，非功能必需。

### 4.5 HTTPS（方案 C，可选，留给有域名时）

前置：一个域名指向服务器公网 IP + Let's Encrypt 证书（Windows 用 **win-acme** 申请 PEM）。
- win-acme 选 **`PEM encoded files`** 存储，得到 `certificate.crt`（含链）+ `privatekey.key`。
- Geyser 配置：
```yaml
bedrock:
  signaling:
    mode: builtin
    builtin:
      https:
        certificate: C:/Geyser/certs/certificate.crt
        private-key: C:/Geyser/certs/privatekey.key
        password: ''
```
- ⚠️ 续期后必须 `geyser reload`/重启，否则玩家收到过期证书。
- 国内服务器若把域名指向它做 HTTPS，ICP 备案仍可能要管 —— 这正是 nxs 能绕开的点。

## 5. 操作步骤

1. 下载最新 `Geyser-Spigot.jar`（https://geysermc.org/download）替换 `plugins/Geyser-Spigot.jar`。
2. 备份 `plugins/Geyser/config.yml`。
3. 按 4.1（推荐）或 4.2 改 config。
4. 方案 B 还需放行 TCP+UDP 端口（见 4.3）、云主机填 `advertise-addresses`（见 4.2③）。
5. 重启 Leaf，看控制台：`Built-in signaling started` 或 Warden 注册成功日志。
6. 用基岩 **26.40+** 客户端实测加入。

## 6. 排错表

| 控制台提示 | 真实原因 | 处理 |
|---|---|---|
| Built-in signaling could not start. Make sure no other program is using TCP port 19132 | **多为 `builtin.https.certificate` 指默认占位符 `/path/to/fullchain.pem`（不存在）→ 证书加载失败→信令整体起不来**；提示"端口被占"是误导，实际端口可能是空的。也可能确为旧 Geyser 进程占用 | 把 `builtin.https.certificate`/`private-key` 置空（只跑明文 HTTP 信令）；或改 `mode: nxs`。若确为端口占用：`netstat -ano \| findstr :<端口>` 查 PID 并结束，或 `-DgeyserSignalingPort=<新端口>` |
| juice: Send failed, errno=10051 / timed out during handshake (30s) | 信令通了但 **WebRTC UDP 媒体连不上**：① 安全组只开 TCP 信令未开 UDP 游戏端口；② 云主机 `advertise-addresses:[]` 广播内网地址，客户端连不上 | ① 安全组放行 **UDP <端口>** 入站；② 设 `bedrock.signaling.builtin.advertise-addresses: ["<公网IP>:<端口>"]` 广播公网地址 |
| 玩家连不上 / 握手超时 | Warden(`agent.warden.cloud`) 国内不可达 | 改用方案 B（内置信令，独立端口） |
| 玩家看到 trust on first use | 信令走明文 HTTP（方案 B 无证书） | 点允许即可；想彻底去掉走方案 A 或 C |
| … is a loopback address | `bedrock.address`=127.0.0.1 | 设 `0.0.0.0` |
| RakNet and NetherNet each need their own UDP port | `both` 模式但 `webrtc-port`=基岩端口 | `webrtc-port` 设成不同端口 |

## 7. 玩家告知文案

- **方案 A（推荐）**：基岩连接端口**不变**（仍是 `19132`），直接重连即可，无需任何改动，也无信任提示。
- **方案 B**：基岩连接端口仍为 `19132`；首次若提示信任，点允许即可。

## 8. 参考

- Geyser 官方博客：https://geysermc.org/blog/preparing-for-nethernet/
- Geyser 安装指南：https://geysermc.org/wiki/geyser/setup/
- 系统属性全量：https://geysermc.org/wiki/geyser/geyser-command-line-arguments-and-system-properties
- win-acme（Windows Let's Encrypt 客户端）：https://www.win-acme.com
