# Orbit — 环绕观察 / 定时巡视插件

让管理员以旁观者视角**绕着指定玩家缓慢环绕**，并支持**定时自动巡视**全部在线玩家。

- 目标版本：Leaf / Paper 1.21.11（Paper API）
- 需要：JDK 21

## 功能

| 功能 | 说明 |
|---|---|
| `/orbit <玩家>` | 开关：开始/停止环绕观察该玩家（再次执行同一玩家即关闭） |
| `/orbit stop` | 停止自己的环绕 |
| `/orbit reload` | 重载 config.yml（需要 `orbit.admin`） |
| `/orbit tour start\|stop` | 手动开始/停止自动巡视（需要 `orbit.admin`） |
| 自动巡视 | 按配置定时让巡视员依次环绕所有在线玩家 |

## 权限

| 权限 | 默认 | 说明 |
|---|---|---|
| `orbit.use` | op | 使用 `/orbit <玩家>` |
| `orbit.admin` | op | reload / tour / 作为自动巡视员 |
| `orbit.bypass` | false | 拥有者不会被自动巡视选中 |

## 配置（config.yml）

```yaml
radius: 6.0                     # 环绕半径（格）
height: 5.0                     # 相机高度（玩家脚底上方）
speed-degrees-per-second: 16.0  # 旋转速度，越小越慢（16 ≈ 22.5 秒一圈）
look-height: 1.0                # 看向玩家身体的高度偏移

auto-tour:
  enabled: false                # 是否启用定时巡视
  viewer: ''                    # 巡视员名；留空 = 第一个在线的 orbit.admin
  interval-seconds: 3600        # 两轮之间间隔（秒）
  per-player-seconds: 30        # 每个玩家巡视时长（秒）
```

## 构建

### 方式 A：Maven（推荐）
```bash
mvn clean package
# 产物 target/Orbit.jar
```
> 若 `paper-api` 1.21.11 的版本号不同，改 `pom.xml` 里的 `<version>`。

### 方式 B：直接用服务端 jar 编译（无需联网）
```bash
mkdir -p _build/classes
javac -encoding UTF-8 -cp "leaf-1.21.11-174.jar" -d _build/classes src/main/java/com/nangua/orbit/*.java
cp -r src/main/resources/* _build/classes/
cd _build/classes && jar cf ../../Orbit.jar . && cd ../..
```

## 安装

1. 把 `Orbit.jar` 丢进 `plugins/`。
2. 重启服（或 `/reload confirm`，但推荐重启）。
3. 编辑 `plugins/Orbit/config.yml` 后 `/orbit reload`。

## 实现说明

- 相机是**隐形 ArmorStand（marker 模式）**，`setPersistent(false)` 防止存档残留。
- 观察者切到 `SPECTATOR` 并 `setSpectatorTarget(相机)`，每 tick 用三角函数 `(r·cosθ, h, r·sinθ)` 移动相机，实现平滑环绕。
- 停止时恢复观察者原游戏模式与原位置。
- 自动巡视切换目标用 `retarget`，不重建会话，避免游戏模式闪烁。
