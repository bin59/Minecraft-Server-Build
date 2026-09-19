# 16. 玩家骑乘 — RideOnHead

本文介绍 RideOnHead 这款轻量社交互动插件：让玩家**空手右键别的玩家**就骑到其头顶，潜行下车，支持多人叠罗汉。本服 1.21.11 双端互通（Java + 基岩）已安装 v1.1.3。文档覆盖玩法、命令与权限、配置要点、基岩端注意事项。

**当前版本**: RideOnHead 1.1.3 | **MC 要求**: 1.21 – 26.2（含 1.21.11，需 Java 21）

**作者/发布**: DeelTer | **协议**: 开源（Modrinth）

**Modrinth**: https://modrinth.com/plugin/rideonhead ｜ **官网**: https://deelter.ru

> 替代方案曾考虑 CarryMe（SpigotMC，命令为西班牙语 `/cargar`/`/aceptar`/`/soltar` 且发起权限默认 false），因不如 RideOnHead 顺手而弃用——RideOnHead 玩家权限 `rideonhead.user` 默认 true，开箱即用。见《各种插件（待选）》§十二。

## 功能说明

- **骑头**：空手右键另一名玩家，即可骑到其头顶。
- **下车**：潜行（Shift）即从被骑者身上下来。
- **个人开关**：`/ride toggle` 切换自己是否允许被别人骑。
- **多人叠罗汉**：支持把玩家一层层叠上去（stack-climb，可配置）。
- **黑名单**：`/ride blacklist` 可配置不可被骑的对象。
- **轻量**：纯服务端、无 NMS 强依赖，Paper/Purpur 系直接跑。

## 安装与前置

1. 服务端为 **Paper / Purpur**（Leaf 为 Paper 分支，兼容）。
2. 将 `RideOnHead-1.1.3.jar` 放入 `plugins/`，**完整重启**服务端（非 /reload）。
3. 无强制前置依赖（不需要 PlaceholderAPI / Vault）。

## 玩法（玩家操作）

| 操作 | 效果 |
|---|---|
| 空手右键玩家 | 骑到其头顶 |
| 潜行（Shift） | 下车 |
| `/ride toggle` | 开关自己是否可被骑 |
| 继续右键叠在队伍顶端 | 多人叠罗汉 |

> 别名 `/ridehead` 等价于 `/ride`。

## 命令与权限

| 命令 | 权限节点 | 默认 | 说明 |
|---|---|---|---|
| `/ride [toggle]` | `rideonhead.user` | **true** | 基础骑乘（右键骑头、toggle） |
| `/ride blacklist` | `rideonhead.admin` | op | 配置黑名单 |
| `/ride reload` | `rideonhead.admin` | op | 重载配置 |
| `/ridehead` | — | — | `/ride` 别名 |

> ✅ 玩家侧**无需额外授权**：`rideonhead.user` 默认 true。admin 节点默认 op，已随 admin 组生效。
> 权限节点已登记进《权限节点速查》与《权限导入脚本》。

## 配置要点

首次启动后生成 `plugins/RideOnHead/config.yml`，常用项：

- 是否允许多人叠罗汉（stack-climb）
- 骑乘高度/偏移（基岩端错位时调）
- 黑名单对象
- 提示文案

> 具体键名以实际生成的 config.yml 为准；改完 `/ride reload` 或重启生效。

## 基岩版注意事项

- 骑乘关系走 Bukkit passenger API，Geyser 一般能把"人骑人"显示给基岩玩家。
- **可能的问题**：基岩端骑乘视角/身高错位、叠罗汉时位置偏移。需在测试服双端实测。
- 若基岩端体验差，可只在 Java 端主推该玩法。

## 排错

| 现象 | 排查 |
|---|---|
| 右键骑不上 | ① 是否空手；② `/ride toggle` 是否关了；③ `rideonhead.user` 权限是否被撤 |
| 骑上去模型错位 | 调 config.yml 里的骑乘高度/偏移 |
| 基岩端看不到人骑人 | Geyser 版本是否过旧；双人实测确认 |
