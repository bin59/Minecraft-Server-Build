# 🔧 一、Chunky 区块预生成插件（服务器性能优化）

本文档介绍 Chunky 区块预生成工具的下载安装、命令用法与性能优化建议，核心用途是在玩家跑图之前提前生成指定范围的区块，从而降低跑图时的服务器卡顿与 TPS 下降。文档同时覆盖多任务并行、选区配置、各平台版本选择以及进阶优化分叉 Chunksmith，面向需要做世界预生成的服主。

#### 简介

Chunky 是由开发者 **pop4959** 开发的开源区块预生成工具，核心功能是在玩家探索之前提前生成指定范围的区块，**显著降低跑图时的服务器卡顿和 TPS 下降**。

**核心特性：**

- 支持同时启动多个区块生成任务，充分利用多核 CPU
- 支持暂停/继续任务，进度自动保存
- 实时显示已处理区块数、完成百分比、预计完成时间（ETA）、处理速率等
- 支持自定义生成形状（圆形、方形、椭圆等）
- 可配合 C2ME 等模组进一步加速生成
- 支持安装 ChunkyBorder 附加组件实现自定义世界边界

**支持平台：** Spigot、Paper、Folia、Fabric、Forge、NeoForge、Sponge 等主流服务端实现

**支持版本：** Minecraft 1.13 ~ 1.21.11

---

#### 下载与安装

**第一步：下载**

根据你的服务器平台选择对应版本：

- **Bukkit/Spigot/Paper 插件版**：从 Modrinth 或 SpigotMC 下载 JAR 文件
- **Fabric 模组版**：从 Modrinth 或 CurseForge 下载（需同时安装 Fabric API）
- **Forge/NeoForge 模组版**：从 CurseForge 下载
- **官方构建版**：从 GitHub Releases 页面下载最新构建

**第二步：安装**

1. 将下载的 JAR 文件放入服务器对应目录：
   - Bukkit/Spigot/Paper → `plugins/` 文件夹
   - Fabric/Forge/NeoForge → `mods/` 文件夹
2. 重启服务器
3. 在游戏中或控制台输入 `/chunky`，如果显示帮助菜单则表示安装成功

---

#### 权限要求

- 在专用服务器上拥有 **OP 管理员权限**（通过控制台执行 `/op <玩家名>` 获取）
- 在**单人世界**中（旧版本可能还需启用作弊模式）

---

#### 命令使用文档

**任务管理命令：**

| 命令               | 说明                           |
| ------------------ | ------------------------------ |
| `/chunky start`    | 开始新的区块生成任务           |
| `/chunky pause`    | 暂停并保存当前任务进度         |
| `/chunky continue` | 继续上次暂停的任务             |
| `/chunky cancel`   | 停止并取消当前任务（进度丢失） |
| `/chunky progress` | 显示所有任务的预生成进度       |

**选区配置命令：**

| 命令                                  | 说明                                                |
| ------------------------------------- | --------------------------------------------------- |
| `/chunky world [world]`               | 选择目标世界（如 `world`、`the_nether`、`the_end`） |
| `/chunky shape <shape>`               | 设置生成形状（如 `square`、`circle`）               |
| `/chunky center [<x> <z>]`            | 设置生成区域中心坐标                                |
| `/chunky radius <radius>`             | 设置生成半径（单位：方块，非区块）                  |
| `/chunky spawn`                       | 将中心设为世界出生点                                |
| `/chunky worldborder`                 | 匹配原版世界边界设置                                |
| `/chunky corners <x1> <z1> <x2> <z2>` | 通过两个对角坐标定义矩形区域                        |
| `/chunky pattern <pattern>`           | 设置生成模式（螺旋式、同心圆式等）                  |
| `/chunky selection`                   | 显示当前选区配置                                    |

**杂项命令：**

| 命令                       | 说明                         |
| -------------------------- | ---------------------------- |
| `/chunky silent`           | 切换是否显示更新消息         |
| `/chunky quiet <interval>` | 设置更新消息的静默间隔（秒） |
| `/chunky reload`           | 重新加载配置                 |
| `/chunky trim`             | 删除选区之外的区块           |

---

#### 典型使用示例

**示例一：在主世界以出生点为中心，生成半径 1000 方块的方形区域**

```
/chunky radius 1000
/chunky start
```

**示例二：在下界以出生点为中心，生成半径 1000 方块的圆形区域**

```
/chunky world the_nether
/chunky shape circle
/chunky spawn
/chunky radius 1000
/chunky start
```

**示例三：生成与世界边界一致的区块（半径 10000）**

```
/worldborder center 0 0
/worldborder set 20000
/chunky worldborder
/chunky start
```

---

#### 性能优化建议

- **选择低峰期执行**：在服务器玩家较少时进行区块预生成，避免影响在线玩家体验
- **分批生成**：大型区域建议分批进行，避免一次性占用过多资源
- **监控 TPS**：当 TPS 低于 18 时考虑暂停任务
- **内存分配**：建议为每个并行任务预留至少 2GB 堆内存，超大型项目（半径 > 20000）建议 4GB 以上
- **调整静默间隔**：将 `chunky quiet` 设置为 30 秒可减少约 15% 的 CPU 开销
- **搭配优化插件**：配合 PaperMC 优化补丁、Aikar 启动参数等可获得更好的性能表现

#### 进阶推荐：Chunksmith

如果你觉得原版 Chunky 在预生成时磁盘占用过高（约 95%-100%），可以考虑使用 **Chunksmith**——它是 Chunky 的加固优化分叉版本，磁盘占用仅约 2%-5%，支持自适应 I/O 限流，**玩家在线时也能安全运行**，无需专门安排闲时。

---
