# 7. 方块记录与回滚 — CoreProtect

**当前版本**: CoreProtect CE v24.0 | **MC 要求**: 1.16.5+

**官方网站**: https://coreprotect.net | **GitHub**: https://github.com/PlayPro/CoreProtect

**官方文档**: https://docs.coreprotect.net/
**下载**: https://modrinth.com/plugin/coreprotect/versions

## 功能说明

CoreProtect 是 Minecraft 最知名的方块记录插件，能记录服务器中几乎所有的方块操作、容器交互、实体杀戮和聊天命令，并支持精确到单个方块的**回滚和还原**操作。此版本为 **CoreProtect Community Edition (CE)**，开源免费。

v24.0 主要更新：回滚性能提升约 80%、新增 8 个 API 查询方法、支持 MC 26.1、`/co purge` 参数验证、大量新记录类型（村民 AI 行为、漏斗拾取、自定义方块等）。

## 关键配置 (`plugins/CoreProtect/config.yml`)

```yaml
use-mysql: true # 使用 MySQL（按下方"MySQL 数据库配置"章节设置；false 则使用 SQLite 本地存储）
language: zh-CN # 中文语言
api-enabled: true # 启用 API
default-radius: 10 # 默认回滚半径
max-radius: 100 # 最大回滚半径
rollback-items: true # 回滚包含物品
rollback-entities: true # 回滚包含实体
skip-generic-data: true # 跳过通用数据
verbose: true # 详细模式

# 以下记录全部启用:
block-place: true # 记录方块放置
block-break: true # 记录方块破坏
natural-break: true # 记录自然破坏
block-movement: true # 记录方块移动（沙/沙砾）
pistons: true # 记录活塞推动
block-burn: true # 记录烧毁
block-ignite: true # 记录自然起火
explosions: true # 记录爆炸
entity-change: true # 记录实体改方块
entity-kills: true # 记录实体击杀
sign-text: true # 记录告示牌文字
buckets: true # 记录桶操作
tree-growth: true # 记录树木生长
water-flow: true # 记录水流
lava-flow: true # 记录岩浆流
liquid-tracking: true # 液体追踪
item-transactions: true # 记录物品交易
item-drops: true # 记录物品丢弃
item-pickups: true # 记录物品拾取
hopper-transactions: true # 记录漏斗交易
player-interactions: true # 记录玩家交互
player-messages: true # 记录聊天消息
player-commands: true # 记录执行命令
player-sessions: true # 记录登录登出
username-changes: true # 记录改名
worldedit: true # 记录 WorldEdit 操作
```

## MySQL 数据库配置（推荐生产环境）

SQLite 适合小型服务器，但数据量增长后查询和回滚速度会明显下降。生产环境建议改用 MySQL，具体步骤如下。

### 第一步：创建 MySQL 数据库和用户

1. 使用 root 登录 MySQL：

   ```bash
   mysql -u root -p
   ```

2. 创建数据库（建议使用 `utf8mb4` 编码）：

   ```sql
   CREATE DATABASE coreprotect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. 创建专用用户并授权（请将 `strong_password_here` 替换为强密码）：

   ```sql
   CREATE USER 'coreprotect'@'localhost' IDENTIFIED BY 'strong_password_here';
   GRANT ALL PRIVILEGES ON coreprotect.* TO 'coreprotect'@'localhost';
   FLUSH PRIVILEGES;
   ```

   > 如果 Minecraft 服务器与 MySQL 不在同一台机器，请将 `'localhost'` 替换为服务器 IP。

### 第二步：修改 CoreProtect 配置文件

1. 停止服务器，用编辑器打开 `plugins/CoreProtect/config.yml`。
2. 修改数据库设置为如下内容，填入第一步创建的数据库信息：

   ```yaml
   use-mysql: true # 启用 MySQL
   table-prefix: co_ # 表前缀，多插件共用数据库时用于区分，保持默认即可
   mysql-host: localhost # MySQL 地址
   mysql-port: 3306 # MySQL 端口
   mysql-database: coreprotect # 数据库名
   mysql-username: coreprotect # 数据库用户名
   mysql-password: strong_password_here # 数据库密码
   ```

### 第三步：迁移现有数据（仅原用 SQLite 时需要）

- **Patreon 版**：启动服务器后，在服务器**控制台**（非游戏内聊天框）执行 `/co migrate-db mysql`，等待迁移完成。
- **免费版**：使用 sqlite3 导出再导入 MySQL，完整步骤见 `CoreProtect插件：玩家的行为数据库过大/2.1.将CoreProtect从SQLite切换到MySQL`。

> 数据库迁移涉及数据安全，操作前请务必备份服务器文件，尤其是 `plugins/CoreProtect/database.db`。

### 第四步：验证

启动服务器，进入游戏用 `/co lookup` 查询历史记录，确认旧数据完整、新数据正常记录。确认无误后可删除旧的 `plugins/CoreProtect/database.db` 释放磁盘空间。

## 常用命令

### 查询类

| 命令                                    | 说明                                  |
| --------------------------------------- | ------------------------------------- |
| `/co inspect`                           | 开启/关闭检查模式（点击方块查看记录） |
| `/co lookup <参数>`                     | 查询方块变更记录                      |
| `/co lookup u:<玩家> t:<时间> r:<半径>` | 查询某玩家在范围内操作                |
| `/co lookup b:<方块ID> t:<时间>`        | 查询特定方块变更                      |
| `/co lookup a:<操作类型>`               | 查询特定操作类型                      |
| `/co near`                              | 查询附近最近变更                      |

### 回滚类

| 命令                                      | 说明                 |
| ----------------------------------------- | -------------------- |
| `/co rollback u:<玩家> t:<时间> r:<半径>` | 回滚某玩家的操作     |
| `/co rollback t:<时间> r:<半径>`          | 回滚某时间段所有操作 |
| `/co restore u:<玩家> t:<时间> r:<半径>`  | 还原（撤销回滚）     |

**时间格式示例**: `t:1h` (1小时前), `t:1d` (1天前), `t:1w` (1周前)

## `CoreProtect CE v24.0 发布说明`

```txt
新增了对Minecraft 26.1版本的支持。
增加了自动清除\*的支持。
增加了对自定义方块的支援（例如通过CraftEngine实现）。
增加了对#bundle标签的支持。
在CoreProtectPreLogEvent中添加了Action、actionId、material、entityType以及message这些元数据。
为purge命令添加了参数验证功能。
增加了僵尸破坏门时的日志记录。
增加了关于花朵被放入花盆的日志记录。
增加了将物品放入料斗时的日志记录。
增加了用于移除预激TNT及火焰衰减的日志记录。
增加了对被扔出的鸡蛋的记录功能（@Warriorrrr).
增加了对村民闲话数据的日志记录。
增加了对村民工作地点记忆的日志记录。
增加了对遭雷击死亡的村民的记录功能。
为自定义头骨纹理添加了日志记录功能。
为所有银鱼侵扰情况添加了日志记录。
新增了8种用于查询的新API方法。
添加了类型化的API辅助函数，并提供了通用的LookupOptions支持。
新增了将查询结果中的物品加入库存的选项。@MrSteppy).
为语言文件增加了颜色编码支持（@1wairesd).
增加了对护甲架损坏事件的额外验证。
为食尸鬼添加了火球攻击属性。
增加了自动错误报告功能。
改进了黑名单处理功能（@guss-alberto）。
改进了MySQL索引处理机制。
改进了Folia支持。
改进了WorldEdit/FAWE版本的解析功能。
改进了砂石记录功能，以防止出现虚拟块。
针对滴液器、分配器、流体以及实体消灭功能的去重逻辑已得到优化。
提升了料斗与容器事务日志记录的性能。
回滚性能得到提升（速度大约快80%）。
已将实体爆炸时的处理方式改为始终在实体名称下方记录伤害数值。
将最低支持的MC版本更改为1.16.5。
固定颜色的数据包无法正确回滚（@guss-alberto）。
有固定状态的积水水桶被错误地记录在系统中了（@guss-alberto）。
固定风荷载记录功能（@Warriorrrr).
修复了下界中水值被错误记录的问题。
解决了固定的深红色以及不受蘑菇生长规律影响的异常菌类生长问题。
修复了导致循环加载顺序的AdvancedChests问题。
修复了旧版MySQL数据库中的字符串值错误。
放置在讲台上的固定书籍未被登记。
处于无效状态的固定音符块正在回退。
修复了在记录实体死亡信息时导致的Folia程序崩溃问题。
固定更新通知并不总是能正确显示。
固定床并不总是能以正确的状态回退。
在回滚后，固定村民不再补充商品。
CoreProtect未记录的固定FAWE命令（@TWME-TW).
解决了在Minecraft 1.21+版本中记录绘画内容时出现的MySQL警告问题。
修复了在记录大型数据包时出现的MySQLDataTruncation错误。
修复了在Folia服务器上记录掉落块信息时出现的IllegalStateException错误。
修复了由无效字节数据引起的NoSuchElementException错误。
解决了在使用Arclight时处理交易事件时出现的NoSuchMethodError错误。
修复了库存中岩浆与水相关情况下的IllegalArgumentException错误。
解决了在MC 26+版本中创建新数据库时出现的NullPointerException问题。
解决了当目标传送带移动时，Purpur服务器会出现NullPointerException的问题。
修复了使用无效数据进行预览时出现的NullPointerException问题。
解决了在API中使用queueLookup时出现的ConcurrentModificationException错误。
```

[完整变更日志：v23.2...v24.0](https://github.com/PlayPro/CoreProtect/compare/v23.2...v24.0)
