# Zinfo — 服务器状态监控与清理工具

Zinfo 是 Paper 管理员工具，通过 GUI 实时查看服务器实体分布、热点区块、红石机器，并支持半径清理和每区块自动限制。

**下载**：https://www.spigotmc.org/resources/zinfo.137616/

**要求**：Paper 1.21.11+ / Java 21+
**平台**：Paper / Purpur（不支持 Spigot/CraftBukkit/Folia）

## 功能

### 实时监控（GUI）
- 在线玩家、飞行玩家、生物、物品展示框、盔甲架、经验球、掉落物总数；
- 按世界分项统计；
- **热点区块排行**：哪个区块生物/展示框/盔甲架/经验球最多，直接传送到最卡的位置；
- **红石机器检测**：列出活塞/中继器/比较器/漏斗最多的区块，定位卡服机器。

### 手动清理
- `/zinfo remove <类型> [半径]`：在周围半径内清理指定实体；
- 类型：`mobs`（生物）/ `frames`（展示框）/ `armorstands`（盔甲架）/ `xp`（经验球）/ `commandblocks`（命令方块）；
- 半径默认 16，可选 10/30/100/1000/5000。

### 自动区块限制
- 每区块展示框上限 25 个；
- 每区块盔甲架上限 15 个；
- 每区块矿车上限 5 个；
- 每区块船上限 5 个；
- 命令方块自动清除；
- 每区块红石机制块上限 50 个，超额禁止放置；
- 水下灵魂沙/岩浆气泡柱平台自动清理。

## 命令

| 命令 | 说明 |
|---|---|
| `/zinfo` | 打开主 GUI 面板（玩家）/ 打印文本统计（控制台） |
| `/zinfo <世界名>` | 查看指定世界的统计 |
| `/zinfo tp <类型> [世界] [序号]` | 传送到热点区块（序号 0 = 最卡的） |
| `/zinfo tpfly [序号]` | 传送到飞行玩家 |
| `/zinfo remove <类型> [半径]` | 半径内清理指定实体 |
| `/zinfo mechanisms [世界] [序号]` | 查看红石机器热点 |
| `/zinfo reload` | 重载配置 |

**别名**：`/zinfo`、`/zi`、`/serverinfo`

## 权限

| 权限 | 默认 | 说明 |
|---|---|---|
| `zinfo.use` | OP | 打开 GUI / 查看统计 |
| `zinfo.tp` | OP | 传送到热点区块/飞行玩家/红石机器 |
| `zinfo.remove` | OP | 半径清理实体 |
| `zinfo.reload` | OP | 重载配置 |

## 配置

单文件：`plugins/Zinfo/config.yml`

```yaml
# 每区块展示框上限
frames-per-chunk: 25
# 每区块盔甲架上限
armor-stands-per-chunk: 15
# 每区块矿车上限
minecarts-per-chunk: 5
# 每区块船上限
boats-per-chunk: 5
# 每区块红石机制块上限
redstone-per-chunk: 50
# 命令方块自动清理间隔（秒）
command-block-scan-interval: 10
# 不限制的世界（主城/出生点保护）
world-whitelist:
  - world
  - world_nether
```

## 安装

1. 下载 Zinfo.jar 放入 `plugins/`；
2. 启动 Paper 一次生成 `config.yml`；
3. 编辑限制参数和世界白名单；
4. `/zinfo reload` 生效。

## 注意事项

- **无 PlaceholderAPI 占位符**：纯 GUI 工具，不对外暴露数据接口；
- **不支持 Folia**；
- 清理展示框不会掉落里面的物品/地图；
- 命令方块和红石热点扫描只采样已加载区块（不扫全服，避免卡主线程）；
- 版本 1（2026-08-02 发布），作者 Zoobastik。
