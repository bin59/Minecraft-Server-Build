# MCA Selector 的详细使用

MCA Selector 是 Minecraft Java 版的区块管理工具，可视化查看、选择、删除或导出世界中的区块，用于优化存档体积、清理跑图产生的冗余区块。

### 1. 获取与安装

- **官方下载**：GitHub 仓库 `github.com/Querz/mcaselector` 的 Releases 页面，按系统选 Windows 安装包、macOS 版或通用 `.jar`。
- **环境要求**：需 Java 8 或更高版本。
- **⚠️ 核心警告**：任何操作前**务必备份世界存档**。

### 2. 清理跑图产生的冗余区块

定期清理玩家跑图产生的无用区块，是控制存档体积、降低 I/O 负载的关键。

**常规可视化清理：**

1. **停服备份**：操作前停服，对 `world`、`world_nether`、`world_the_end` 完整备份。
2. **智能筛选**：打开 MCA Selector，点击 `Filter`。推荐组合条件定位"鬼区"：
   - `InhabitedTime < 1m`（玩家停留时间小于 1 分钟，通常是跑图误加载）
   - `LastUpdate > 30d`（最后更新在 30 天前）
   - `DistanceFromSpawn > 5000`（距出生点大于 5000 格，保护主城和核心基地）
3. **执行删除**：预览无误后按 `Ctrl + D` 批量删除。

**命令行自动化：**

可将清理动作写成脚本，结合 `cron` 定时任务在服务器低峰期自动执行：

```bash
# 自动清理主世界中距出生点 5000 格外、玩家停留时间小于 1 分钟的区块
java -jar mcaselector.jar --world /path/to/server/world --delete --filter "DistanceFromSpawn>5000 && InhabitedTime<1m"
```

### 3. 跨存档迁移建筑

**方案 A：MCA Selector（适合大型建筑群/完整地形）**

1. **导出**：源存档中框选建筑区域，`File` > `Export Selection` 保存为 `.mca` 文件。
2. **导入**：打开目标存档，`File` > `Import Selection`，拖动绿色预览框到目标坐标，按 `Ctrl + V` 覆盖。

- ⚠️ 跨存档迁移尽量保证两个存档的游戏版本和 Mod 环境一致，否则自定义方块/实体 NBT 可能丢失报错。

**方案 B：结构方块（适合纯建筑/小型搬迁）**

1. 源存档用结构方块（Save 模式）框选并保存建筑。
2. 去存档目录的 `structures` 文件夹，把生成的 `.nbt` 复制到目标存档同名文件夹。
3. 目标存档用结构方块（Load 模式）读取并放置。

### 4. 批量修改属性

点击 `Edit` > `Change Fields`，可批量修改选中区块的 NBT 数据，例如批量更改生物群系（Biome）或重置区块 `LastUpdate` 时间戳。

> ⚠️ 维护服务器时操作前务必先停服，避免文件被占用导致数据损坏或读写冲突。
