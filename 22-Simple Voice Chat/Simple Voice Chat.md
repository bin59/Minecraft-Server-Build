**Simple Voice Chat（SVC）** 是 Minecraft 中最流行的近距离语音聊天模组，安装后玩家可以在游戏内直接语音交流，无需借助 Discord、YY 等第三方软件。以下是完整的介绍、下载、配置与使用指南。

---

### 📖 模组简介

Simple Voice Chat 由作者 **Max Henkel（henkelmax）** 开发，核心特性包括：

- **近距离语音**：只有一定距离内的玩家才能听到你的声音，距离可自定义
- **仿 3D 音效**：声音会根据玩家方位产生空间感
- **两种通话模式**：按键通话（Push-to-Talk）和语音触发通话（自由麦）
- **Opus 编码**：采用 Opus Interactive Audio Codec 进行高质量语音编码
- **群聊功能**：可以创建群组，与不在附近的玩家语音交流
- **可静音/调音量**：可以单独静音某个玩家或调节其语音音量
- **麦克风测试**：内置麦克风测试功能，方便调试
- **提示图标**：说话时屏幕左下角显示麦克风图标，其他玩家名字旁显示扬声器图标

---

### 📥 下载

> ⚠️ **安全提醒**：请务必从以下官方渠道下载，不要从不明网站下载所谓的"语音工具"，近期有恶意软件伪装成语音聊天工具进行钓鱼攻击。

| 平台        | 链接                                                                                |
| :---------- | :---------------------------------------------------------------------------------- |
| Modrinth    | [Modrinth 下载页](https://modrinth.com/mod/simple-voice-chat)                       |
| CurseForge  | [CurseForge 下载页](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) |
| 官方 Wiki   | [官方 Wiki](https://modrepo.de/minecraft/voicechat/wiki/?t=setup)                   |
| GitHub 源码 | [GitHub 仓库](https://github.com/henkelmax/simple-voice-chat)                       |

**支持的模组加载器**：Forge、Fabric、Quilt、NeoForge

**支持的 MC 版本**：1.12.2 ~ 1.21.x（覆盖面非常广）

**安装方式**：将下载的 `.jar` 文件放入客户端和服务端的 `mods` 文件夹中（服务端如果是插件端如 Paper/Spigot，则放入 `plugins` 文件夹）。

> **重要**：服务端和客户端**都必须安装**，否则无法使用语音功能。

---

### ⚙️ 服务端配置

#### 核心配置文件

首次启动服务端后，会自动生成配置文件：

- **Forge/Fabric/NeoForge 端**：`config/voicechat/voicechat-server.properties`
- **Bukkit/Spigot/Paper 插件端**：`plugins/voicechat/voicechat-server.properties`

#### 关键配置项

```properties
# 语音服务端口（UDP），默认 24454
port=24454

# 绑定地址，留空使用 server.properties 中的 server-ip
bind_address=

# 最大语音距离（方块数）
max_voice_distance=48.0

# 潜行时语音距离倍率
crouch_distance_multiplier=1.0

# 小声说话时语音距离倍率
whisper_distance_multiplier=0.5

# 客户端连接语音服务的地址（内网穿透时必填）
voice_host=

# 是否强制玩家安装模组，未安装则踢出
force_voice_chat=false

# 是否允许群聊
enable_groups=true

# 是否允许录音
allow_recording=true
```

#### 端口开放（最关键的一步）

SVC 使用 **UDP 协议**，默认端口为 **24454/UDP**。

- **有公网 IP**：在路由器/光猫上做端口转发，将 UDP 24454 端口转发到服务器内网 IP
- **无公网 IP（内网穿透）**：需要使用 frp、SAKURA FRP 等工具穿透 UDP 端口，并在配置文件中设置 `voice_host` 为穿透后的公网地址

> ⚠️ 很多云服务商（阿里云、腾讯云等）的安全组默认只开放 TCP，**必须手动添加 UDP 规则**放行 24454 端口，否则语音无法连接。

#### 内网穿透配置示例（frp）

在 `frpc.toml` 中添加：

```toml
[[proxies]]
name = "mc-voice"
type = "udp"                    # 必须是 UDP
localIP = "127.0.0.1"
localPort = 24454               # 本地语音端口
remotePort = 24454              # 公网映射端口
```

然后在 `voicechat-server.properties` 中设置：

```properties
voice_host=你的公网服务器IP
port=24454
```

---

### 🎮 客户端使用

#### 默认快捷键

| 功能         | 默认按键      | 说明                               |
| :----------- | :------------ | :--------------------------------- |
| 打开语音设置 | **V**         | 打开语音聊天设置菜单               |
| 按键通话     | **Caps Lock** | 按住说话（按键通话模式）           |
| 麦克风静音   | **M**         | 按住临时停止声音输入（自由麦模式） |
| 禁用语音     | **N**         | 完全退出语音频道                   |
| 隐藏图标     | **H**         | 隐藏所有语音相关图标               |

#### 语音设置界面

按 **V** 键打开设置界面，可以：

- 调节语音总音量和麦克风增益
- 选择麦克风/扬声器设备
- 点击"开启语音测试"按钮测试麦克风，调节语音触发灵敏度
- 点击"调节玩家音量"单独调整某个玩家的语音音量
- 创建/加入群聊（Group Chat），输入 `/voicechat invite <玩家名>` 邀请玩家

#### 提示图标说明

| 图标                      | 含义                     |
| :------------------------ | :----------------------- |
| 🎤 麦克风图标（左下角）   | 你正在说话               |
| 🔊 扬声器图标（玩家名旁） | 该玩家正在说话           |
| 🔇 静音图标               | 该玩家已关闭语音         |
| 🔌 断开图标               | 语音连接断开或未安装模组 |

---

### 🔧 常见问题排查

| 问题                   | 原因与解决                                                  |
| :--------------------- | :---------------------------------------------------------- |
| 玩家名旁显示"断开"图标 | 语音端口未开放或 `voice_host` 配置错误                      |
| 进服后无法说话         | 检查防火墙是否放行了 UDP 24454 端口                         |
| 客户端无语音设置界面   | 确认客户端也安装了**相同版本**的 SVC 模组                   |
| 语音有延迟/卡顿        | 检查网络质量，可在配置中调整 `mtu_size` 为更小的值          |
| 内网穿透后连不上       | 确认穿透的是 **UDP** 协议而非 TCP，且 `voice_host` 填写正确 |

---

### 🔌 扩展插件

SVC 拥有丰富的生态扩展，以下是截至 2026 年 7 月已知的一些扩展模组：

- **Simple Voice Chat Soundboard**：通过快捷键播放音效，其他玩家可通过语音听到
- **Voice Chat Interaction**：让语音与游戏内机制互动
- **Simple Voice Radio**：添加无线电语音功能
- **Replay Voice Chat**：在 Replay Mod 回放中保留语音录制
- **REPO Heads**：玩家说话时嘴巴会动态张合
- **VolumeScroll**：通过鼠标滚轮快速调节玩家音量
- **SimpleVoice-Geyser**：让基岩版（Bedrock）玩家通过网页界面使用语音聊天

## 云服务器上搭建

以下是在云服务器上搭建 Minecraft Simple Voice Chat 的完整教程，涵盖从服务器选购到最终调试的全流程。

---

### 🖥️ 服务器选购与准备

#### 配置建议

| 玩家规模 | CPU  | 内存 | 带宽     |
| :------- | :--- | :--- | :------- |
| 1~5人    | 1核  | 2GB  | 3~5Mbps  |
| 5~15人   | 2核  | 4GB  | 5~10Mbps |
| 15人以上 | 4核+ | 8GB+ | 10Mbps+  |

> 语音聊天本身对 CPU 和内存的额外开销不大，主要瓶颈在带宽。建议优先选择**带宽充足**的机型。

#### 系统选择

推荐使用 **Ubuntu 22.04/24.04** 或 **Debian 11/12**，社区支持最好，教程最多。

#### 国内 vs 海外

- **国内服务器**：延迟低，但域名需要备案，且部分云厂商对 UDP 端口有限制
- **海外服务器**（香港/日本/新加坡）：无需备案，端口限制少，但国内访问延迟稍高

---

### ☁️ 方案一：有公网 IP（推荐）

如果你的云服务器有独立公网 IP，这是最简单的方案。

#### 第一步：安装 Java 环境

SSH 登录服务器后执行：

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-21-jdk

# 验证安装
java -version
```

> 根据你选择的 MC 版本安装对应的 JDK，MC 1.20.5+ 需要 JDK 21，1.18~1.20.4 需要 JDK 17。

#### 第二步：安装 Minecraft 服务端

```bash
# 创建服务器目录
mkdir ~/mc-server && cd ~/mc-server

# 下载服务端（以 Paper 1.21 为例，请替换为实际下载链接）
wget https://api.papermc.io/v2/projects/paper/versions/1.21/builds/latest/downloads/paper-1.21.jar -O server.jar

# 首次启动（会生成配置文件，然后同意 EULA）
java -Xmx2G -Xms1G -jar server.jar nogui
echo "eula=true" > eula.txt

# 再次启动
java -Xmx2G -Xms1G -jar server.jar nogui
```

#### 第三步：安装 Simple Voice Chat

```bash
# 进入 mods 目录（如果是插件端则进入 plugins 目录）
cd ~/mc-server/mods

# 下载 Simple Voice Chat（请替换为对应 MC 版本和加载器的实际下载链接）
wget https://modrinth.com/.../simple-voice-chat.jar
```

> 客户端也需要安装**相同版本**的 Simple Voice Chat 模组。

#### 第四步：配置语音服务

首次启动后会自动生成配置文件，编辑它：

```bash
# Forge/Fabric 端
nano ~/mc-server/config/voicechat/voicechat-server.properties

# 插件端（Paper/Spigot）
nano ~/mc-server/plugins/voicechat/voicechat-server.properties
```

修改关键配置项：

```properties
# 语音端口，默认 24454
port=24454

# 绑定地址，云服务器建议设为 0.0.0.0
bind_address=0.0.0.0

# 填入你的云服务器公网 IP
voice_host=你的公网IP

# 最大语音距离（方块数）
max_voice_distance=48.0

# 是否强制玩家安装模组
force_voice_chat=false
```

#### 第五步：开放端口（最关键的一步）

SVC 使用 **UDP 协议**，需要同时开放两个端口：

| 端口  | 协议    | 用途        |
| :---- | :------ | :---------- |
| 25565 | TCP     | MC 游戏连接 |
| 24454 | **UDP** | 语音聊天    |

**云服务器安全组配置**（以阿里云/腾讯云为例）：

1. 进入云服务器控制台 → 安全组
2. 添加入方向规则：
   - 协议：**UDP**
   - 端口：**24454**
   - 来源：**0.0.0.0/0**

> ⚠️ 很多云厂商安全组默认只开放 TCP，**必须手动添加 UDP 规则**，否则语音无法连接。

**服务器防火墙**（如果开启了 ufw/firewalld）：

```bash
# ufw（Ubuntu）
sudo ufw allow 24454/udp
sudo ufw allow 25565/tcp

# firewalld（CentOS）
sudo firewall-cmd --permanent --add-port=24454/udp
sudo firewall-cmd --permanent --add-port=25565/tcp
sudo firewall-cmd --reload
```

#### 第六步：重启服务器并测试

```bash
# 回到服务器目录重启
cd ~/mc-server
java -Xmx2G -Xms1G -jar server.jar nogui
```

进入游戏后按 **V** 键打开语音设置，检查是否显示已连接。

---

### 🔗 方案二：无公网 IP（内网穿透）

如果你的服务器没有公网 IP（如校园网、家庭宽带），需要使用 frp 进行内网穿透。

#### 整体架构

```
玩家客户端 ──UDP──▶ 云服务器(frp服务端) ──UDP──▶ 本地电脑(frp客户端+MC服务端)
```

#### 云服务器端（frps）

```bash
# 下载 frp（请替换为最新版本号）
wget https://github.com/fatedier/frp/releases/download/v0.61.1/frp_0.61.1_linux_amd64.tar.gz
tar -xzf frp_0.61.1_linux_amd64.tar.gz
cd frp_0.61.1_linux_amd64
```

编辑 `frps.toml`：

```toml
bindPort = 7000
```

启动 frps：

```bash
./frps -c frps.toml
```

确保云服务器安全组放行：

- **7000/TCP**（frp 通信端口）
- **24454/UDP**（语音端口）
- **25565/TCP**（MC 游戏端口）

#### 本地电脑端（frpc）

编辑 `frpc.toml`：

```toml
serverAddr = "你的云服务器公网IP"
serverPort = 7000

[[proxies]]
name = "mc-server"
type = "tcp"
localIP = "127.0.0.1"
localPort = 25565
remotePort = 25565

[[proxies]]
name = "mc-voice"
type = "udp"
localIP = "127.0.0.1"
localPort = 24454
remotePort = 24454
```

启动 frpc：

```bash
./frpc -c frpc.toml
```

#### 修改 SVC 配置

在本地 MC 服务端的 `voicechat-server.properties` 中：

```properties
voice_host=你的云服务器公网IP
port=24454
```

---

### 🐳 方案三：Docker 部署（进阶）

如果你熟悉 Docker，可以用 docker-compose 一键部署：

```yaml
# docker-compose.yml
version: '3.8'

services:
  mc-server:
    image: itzg/minecraft-server
    container_name: mc-server
    environment:
      EULA: 'TRUE'
      TYPE: PAPER
      VERSION: '1.21'
      MEMORY: 2G
    ports:
      - '25565:25565'
      - '24454:24454/udp' # 注意 /udp 标记
    volumes:
      - ./mc-data:/data
    restart: unless-stopped
```

> 使用 Docker 时，Simple Voice Chat 的 jar 文件需要放入挂载的 `mc-data/mods` 目录中。

---

### 🔧 常见问题排查

| 问题                       | 原因与解决                                                   |
| :------------------------- | :----------------------------------------------------------- |
| 玩家名旁显示"插头断开"图标 | `voice_host` 未设置或填写错误；UDP 端口未放行                |
| 进服后语音一直连不上       | 检查安全组是否放行了 **UDP** 24454（不是 TCP）               |
| 能连上但听不到声音         | 客户端按 V 检查麦克风设置；确认双方模组版本一致              |
| 语音延迟高/卡顿            | 服务器带宽不足；可在配置中调小 `mtu_size`                    |
| 内网穿透后连不上           | 确认 frpc 中 `type = "udp"`；确认 `voice_host` 填的是公网 IP |
| 端口被占用                 | 修改 `port` 为其他端口，同时更新安全组和 frpc 配置           |

---

### ✅ 最终检查清单

- [ ] 云服务器安全组已放行 **25565/TCP** 和 **24454/UDP**
- [ ] 服务端和客户端安装了**相同版本**的 Simple Voice Chat
- [ ] `voicechat-server.properties` 中 `voice_host` 填写了正确的公网 IP
- [ ] `bind_address` 设为 `0.0.0.0`（云服务器必须）
- [ ] 服务器防火墙（如有）已放行对应端口
- [ ] 内网穿透场景下，frp 穿透的是 **UDP** 协议

如果你能告诉我你的具体情况（云厂商、是否有公网 IP、MC 版本、模组加载器类型），我可以帮你生成一份更精确的配置方案。
