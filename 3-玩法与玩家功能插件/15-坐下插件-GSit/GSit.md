# GSit（坐下 / 躺 / 爬行 / 转圈）

## 是什么

作者 Gecolay 的现代坐下玩法插件：右键楼梯/台阶/地毯坐下，命令躺下、爬行、原地转圈，支持坐在玩家身上。免费开源。

## 兼容性（本服确认）

- 支持 Bukkit / Spigot / Paper **及所有分支**（Leaf 1.21.11 可用），Folia 也支持
- 版本覆盖 1.16 – 26.2；**1.21.11 官方支持**（3.1.1 起，现 3.2.1）
- Java 21（躺/爬行姿势功能需 1.18+，本服满足）

## 功能

| 功能 | 说明 |
|---|---|
| 右键坐下 | 右键楼梯 / 台阶 / 地毯 / 可配置方块即可坐下，自动对正朝向 |
| `/sit` | 原地坐下 |
| `/lay` | 躺下（`layback` 可向后躺） |
| `/crawl` | 趴下爬行（1.18+） |
| `/spin` | 原地旋转 |
| 坐在玩家身上 | 对玩家使用可坐其头顶 |

## 命令与权限

| 命令 | 权限 |
|---|---|
| `/sit` | `gsit.sit` |
| `/lay` | `gsit.lay` |
| `/crawl` | `gsit.crawl` |
| `/spin` | `gsit.spin` |
| `/gsit toggle`（开关本服内坐下） | `gsit.toggle` |
| `/gsit reload` | `gsit.reload` |
| 绕过区域/方块限制 | `gsit.bypass` |

总开关：`gsit.use`（默认所有玩家拥有）。完整权限清单见 SpigotMC 资源页。

## 配置要点（config.yml）

- `sit-on-stairs-by-click` / `sit-on-slabs-by-click` / `sit-on-carpets-by-click`：分别控制楼梯/台阶/地毯能否右键坐（默认 true）
- `sit-on-blocks-by-click`：其他方块是否可坐
- `sit-on-players`：能否坐在玩家身上
- `blocked-regions`：指定区域禁止坐下（配合领地）
- `max-distance`：右键坐下时与方块的最大距离

## 与基岩版（Geyser）兼容

服务端交互实现，基岩玩家长按右键可触发坐下；`/sit` 等命令同样可用。建议装后实测一次确认交互。

## 领地注意

内置区域联动是 **WorldGuard / GriefPrevention**（Residence 无内置联动）；坐下不涉及方块破坏，默认领地内不受限。如需在特定区域禁止，用 `blocked-regions`。

## 下载

SpigotMC：https://www.spigotmc.org/resources/gsit-modern-sit-seat-and-chair-lay-and-crawl-plugin-1-16-1-21-11.62325/
（最新版 3.5.1 支持至 26.2，1.21.11 可用 3.2.1+ 任意版本）
