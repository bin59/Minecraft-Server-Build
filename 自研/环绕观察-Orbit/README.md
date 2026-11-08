# Orbit — 环绕观察 / 定时巡视插件

让管理员以旁观者视角**绕着指定玩家缓慢环绕**，并支持**依次自动巡视全部在线玩家**。

- 目标版本：Leaf / Paper **26.3**（`api-version: '26.3'`）
- 编译：JDK 25（`javac --release 21`），字节码 major 65，Java 21+ 可运行
- 依赖：仅 Bukkit/Paper API，无第三方运行时依赖

## 命令

| 命令 | 说明 |
|---|---|
| `/orbit <玩家>` | 开关：开始/停止环绕观察该玩家（再次执行同一玩家即关闭） |
| `/orbit stop` | 停止自己的环绕，恢复原游戏模式与原位置 |
| `/orbit reload` | 重载 `config.yml`（`orbit.admin`） |
| `/orbit tour start` | 手动开始自动巡视，以**命令发起者**为巡视员（`orbit.admin`） |
| `/orbit tour stop` | 停止自动巡视并恢复巡视员（`orbit.admin`） |
| `/orbit tour status` | 查看巡视员、当前目标、队列进度、剩余秒数（`orbit.admin`） |

自动巡视按 `per-player-seconds` 依次环绕队列中所有在线玩家；一轮结束等待
`interval-seconds` 后按**最新在线名单**重新排队，新上线的玩家会进入下一轮。
目标离线会立即跳到下一个，不空等。

## 权限

| 权限 | 默认 | 说明 |
|---|---|---|
| `orbit.use` | op | 使用 `/orbit <玩家>` |
| `orbit.admin` | op | reload / tour / 作为自动巡视员 |
| `orbit.bypass` | false | 拥有者**不会被自动巡视为目标**（不影响其成为巡视员） |

## 配置（config.yml）

```yaml
radius: 6.0                     # 环绕半径（格）
height: 5.0                     # 相机高度（目标玩家脚底上方）
speed-degrees-per-second: 16.0  # 旋转速度，越小越慢（16 ≈ 22.5 秒一圈）
look-height: 1.0                # 看向目标身体的高度偏移
update-interval-ticks: 2        # 本体位置更新间隔（tick）；1=20Hz，2=10Hz

messages:
  tour-switch: '&7巡视 &e%player% &7(%index%/%total%)'  # 切换目标提示

auto-tour:
  enabled: true                 # 插件启用后自动开始巡视
  viewer: ''                    # 巡视员名；留空 = 第一个在线的 orbit.admin
  interval-seconds: 60          # 两轮之间的等待（秒）
  per-player-seconds: 30        # 每个玩家巡视时长（秒）
```

> ⚠️ `auto-tour.enabled` 默认 **true**：插件启用后会自动把第一个在线的 `orbit.admin`
> 切成旁观者开始巡视。不希望这样，请填好 `auto-tour.viewer` 指定固定巡视员，
> 或设为 `false` 后用 `/orbit tour start` 手动开启。

`update-interval-ticks` 只影响 `PlayerTeleportEvent` 触发频率，**不改变实际转速**
（角速度按间隔自动换算）。慢速环绕下 2 tick（10Hz）肉眼无差别，推荐保持 2。

## 构建

### 方式 A：Maven
```bash
mvn clean package
```
`pom.xml` 依赖 `../升级26.3版本/lib/leaf-api-26.3.build.13-alpha.jar`（systemPath）。

### 方式 B：直接用 javac（本仓库实际使用，离线可编译）
```bash
cd 自研/环绕观察-Orbit
"C:/Program Files/Java/jdk25.0.5_0/bin/javac.exe" --release 21 -encoding UTF-8 \
  -cp "<leaf-api.jar>;<升级26.3版本/_deps 下全部 jar>" \
  -d _build/classes26 src/main/java/com/nangua/orbit/*.java
# 再把 _build/classes26/** 与 src/main/resources/* 用 zipfile 打成 Orbit.jar
```

踩坑记录：
- **只用 `leaf-api` 一个 jar 会编译失败**：`找不到 net.kyori.adventure.key.Namespaced 的类文件`，
  必须把 `升级26.3版本/_deps/` 里的依赖一起放进 classpath（排除重复的 leaf-api 与旧 adventure 4.16.0）。
- Windows 的 classpath 分隔符是 `;`，Git Bash 下整串要加引号。
- 打包时 zip 内**不要带 `_build/classes26/` 目录前缀**，否则插件加载不到主类。

产物同步到两处：`自研/环绕观察-Orbit/Orbit.jar` 与 `自研/升级26.3版本/_out/Orbit.jar`。

## 安装

1. 把 `Orbit.jar` 放进 `plugins/`。
2. 重启服（不建议 `/reload`）。
3. 编辑 `plugins/Orbit/config.yml` 后 `/orbit reload`。

> ⚠️ **从 1.0.0 升级时**：`saveDefaultConfig()` 不会覆盖已存在的
> `plugins/Orbit/config.yml`，所以旧文件里的 `auto-tour.enabled: false` 和
> `interval-seconds: 3600` 会继续生效，新默认值不会自动应用。
> 两种处理任选其一：
> - **最快**：直接执行 `/orbit tour start`（手动启动不受 `enabled` 影响，立即开始循环）；
> - **要开机自动**：把 `enabled` 改为 `true`、`interval-seconds` 改为 `60`，
>   或删掉 `plugins/Orbit/config.yml` 后重启让插件重新生成默认配置。
>
> 新增的 `update-interval-ticks` / `messages.tour-switch` 缺失时代码有内置默认值，
> 不补也能正常工作。

## 实现说明

- 观察者切到 `SPECTATOR`，**本体**按 `(r·cosθ, h, r·sinθ)` 每 `update-interval-ticks`
  传送一次，并用 `setDirection` 朝向目标身体。
- 停止时清空旁观目标、传送回原位、恢复原游戏模式；跨世界观察会先挪到目标世界。
- **1.0.1 起不再生成任何实体**：旧版用「隐形 ArmorStand + `setSpectatorTarget`」当相机，
  但 Camera 包要求客户端已追踪到该实体，spawn 后立刻绑定常被静默丢弃，
  表现为「盔甲架在转、观察者画面不动」，且盔甲架可能意外可见、有存档残留风险。
  改为直接移动观察者本体后，不依赖任何实体或客户端相机包。
- 自动巡视切换目标用 `retarget()`，不重建会话，避免游戏模式与位置来回闪烁。
- 一轮结束会恢复巡视员并等待 `interval-seconds`，下一轮重新进入旁观者模式。

## 排错

| 现象 | 处理 |
|---|---|
| 画面不动 / 一直在原地 | `/orbit tour status` 确认会话在跑；检查是否被其他插件取消了传送事件（Residence 等）；可临时把 `update-interval-ticks` 设为 1 观察 |
| 自动巡视没循环 | `/orbit tour status`；`enabled=false` 或没有在线 `orbit.admin` 时不会跑；`interval-seconds` 是**一轮结束后**的等待，默认 60 秒 |
| 切换目标时闪一下 | 跨世界传送属正常；一轮结束会恢复游戏模式，下一轮重新进入 |
| 自己被自动切成旁观者 | 填 `auto-tour.viewer` 指定巡视员，或设 `enabled: false` 后手动 `/orbit tour start` |

## 变更记录

见 `CHANGELOG.md`。
