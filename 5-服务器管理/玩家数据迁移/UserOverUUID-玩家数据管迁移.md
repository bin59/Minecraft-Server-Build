# UserOverUUID（UuidMigrate）下载使用配置

> 玩家数据管理工具：用于在服务器 Online 模式（正版/在线UUID）与 Offline 模式（离线/盗版UUID）之间迁移玩家数据，切换正版/离线模式时**不丢背包、进度、成就**。

- 插件名：UserOverUUID（JAR 内部名：UuidMigrate）
- 最新版本：UuidMigrate-1.0.0（2026-01-04 更新）
- 支持核心：Spigot / Paper / Purpur / Bukkit，API 版本 1.19+
- 支持游戏版本：1.19 ~ 1.21.11
- 运行环境：Java 17 及以上

---

## 一、下载

### 官方下载（推荐，直接下载 JAR）
- CurseForge 项目页：<https://www.curseforge.com/minecraft/bukkit-plugins/useroveruuid>
- 文件下载页：<https://www.curseforge.com/minecraft/bukkit-plugins/useroveruuid/files>
- 主文件：**UuidMigrate-1.0.0.jar**（适配 1.21.11，向下兼容 1.19+）

### 源码（GitHub，未发布 Releases，需自行编译）
- 仓库：<https://github.com/center2055/UserOverUUID>
- 编译方式：需要 Maven 3.6+，执行 `mvn clean package`，产物在 `target/` 目录下

---

## 二、安装

1. 将 `UuidMigrate-1.0.0.jar` 放入服务端的 `plugins/` 文件夹；
2. 按需编辑 `plugins/UuidMigrate/config.yml`（首次启动插件后自动生成）；
3. 重启服务器。

---

## 三、工作原理

玩家加入时插件自动检测并迁移，无需手动操作：

| 服务器模式 | 迁移方向 |
|---|---|
| Online 模式（正版） | 离线 UUID → 在线 UUID |
| Offline 模式（离线/盗版） | 在线 UUID → 离线 UUID |

- **默认安全模式**：COPY（先备份再迁移，不改动原文件）；改为 MOVE 才真正搬移数据。
- **备份位置**：`plugins/UuidMigrate/backups/<时间戳>/<玩家名>/`
- 自动识别世界文件夹，并自动处理下界（Nether）/末地（End）维度的玩家数据。

---

## 四、config.yml 完整配置（含中文注释）

```yaml
operation: COPY            # COPY（复制并保留原数据，安全）或 MOVE（迁移后删除原数据）
overwriteTarget: false     # 目标数据已存在时是否覆盖（true=覆盖，false=跳过）
kickAfterMigrate: false    # 迁移完成后是否踢出玩家要求重进
kickMessage: "&aYour data was migrated. Please rejoin."   # 踢出提示消息（支持 & 颜色代码）
logLevel: INFO             # 日志级别：INFO / DEBUG 等

worlds:
  autoDetect: true         # 自动检测世界文件夹
  includeNetherEnd: true   # 是否包含下界/末地维度
  additionalWorldFolders: []   # 额外指定世界文件夹，例如 ["world2","creative"]

lookup:
  useUsercache: true       # 是否使用 usercache.json 查找玩家 UUID
  useMojangApi: false      # 离线模式下是否调用 Mojang API 获取在线UUID数据
                           # （离线服想迁移在线UUID数据时改为 true）
  mojangApiTimeoutMs: 2000 # Mojang API 超时（毫秒）
  cacheTtlMinutes: 1440    # UUID 缓存有效期（分钟），默认 24 小时

paths:
  includeVanillaPlayerdata: true   # 迁移原版 playerdata（背包/位置等）
  includeStats: true               # 迁移统计（stats）
  includeAdvancements: true        # 迁移进度/成就（advancements）
  customPaths: []                  # 其他插件数据的自定义路径
    # 占位符：{uuid} = 玩家UUID，{name} = 玩家名
    # 示例：
    # - "plugins/Essentials/userdata/{uuid}.yml"
    # - "plugins/SomePlugin/data/{uuid}.json"
```

---

## 五、命令

| 命令 | 说明 |
|---|---|
| `/uuidmigrate status <玩家>` | 查看该玩家是否有待迁移数据 |
| `/uuidmigrate migrate <玩家>` | 手动强制迁移该玩家数据 |
| `/uuidmigrate dryrun <玩家>` | 预演：只显示会做什么，不实际改动文件 |
| `/uuidmigrate reload` | 重载配置 |

## 六、权限

| 权限节点 | 说明 |
|---|---|
| `uuidmigrate.admin` | 全部命令权限，默认仅 OP 拥有 |

---

## 七、注意事项 / 限制

1. **权限数据不会自动迁移**（如 LuckPerms 等权限插件数据需手动迁移）；
2. 部分插件的配置数据可能需要手动配置自定义路径（`paths.customPaths`）才能迁移；
3. 迁移前务必先备份，`operation: COPY` 模式下插件本身也会自动备份；
4. 切换正版/离线模式前建议先在测试服试运行 `dryrun` 预演确认无误。
