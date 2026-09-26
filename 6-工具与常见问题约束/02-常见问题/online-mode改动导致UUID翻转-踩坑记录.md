# online-mode 改动导致 UUID 翻转：领地 / 皮肤 / 头衔集体失效（踩坑记录）

> 适用架构：**独立 / 单机服务端（本服非 Velocity 代理架构）**。
> 记录日期：2026-09-20 ｜ 关键词：`server.properties` `online-mode` `UUID` `Residence` `LuckPerms` `SkinsRestorer`

## 一句话铁律

**`online-mode` 一旦定下，永远别改。** 在独立服务端上，它是直接决定玩家 UUID 类型的开关，改一次 = 全体玩家数据脱钩一次。

## 一、现象特征（踩坑信号）

改动 `server.properties` 的 `online-mode` 后，以下症状**同时**出现（这是统一根因，不是三个独立 bug）：

- 玩家领地还在但权限没了 / 整个领地归属丢失（Residence）
- 皮肤不加载（SkinsRestorer）
- 管理员 / 玩家 TAB 头衔丢失（LuckPerms / TAB）
- 手动 `/resadmin setowner` 重绑后，下次再改配置又失效

> 注：基岩玩家（Floodgate，名带 `.` 前缀）的 UUID 由 Xbox UID 派生，**通常不受 online-mode 翻转影响**；若只有 Java 玩家丢数据，基本可锁定是这次的 online-mode 翻面。

## 二、根因

在独立服务端（无代理转发 UUID）上：

| `online-mode` | 玩家 UUID 来源 | 谁能进 |
|---|---|---|
| `true` | Mojang 正版账号 UUID（v4，随机） | 仅正版 session |
| `false` | 按**用户名**算的离线 UUID（v3，确定性） | 任意客户端 / 破解 |

- `Residence` / `LuckPerms` / `SkinsRestorer` / `TAB` **全部按 UUID 存数据**。
- 改动 `online-mode` → 同一玩家 UUID 在正版 UUID 与离线 UUID 之间翻转 → 所有按 UUID 的数据瞬间对不上。
- **反复 toggle** 会让 `setowner` 永远追不上：修好（写入当时 UUID）→ 又改配置 → UUID 再翻 → 刚修好的又失效。

## 三、诊断：如何看 UUID 并判定当前卡在哪种

### 3.1 看玩家「当前」UUID（服务端现在认定他是谁）

| 方式 | 操作 |
|---|---|
| LuckPerms（最方便） | `/lp user <玩家名> info` → 首行即 UUID |
| usercache | 服务端根目录 `usercache.json`（`name→uuid` 数组） |
| 玩家存档 | `world/playerdata/` 文件名即 UUID，配合 `usercache.json` 对应真人 |
| Essentials | `plugins/Essentials/usermap.csv` 或 `userdata/<UUID>.yml` |

### 3.2 看「领地数据里」记录的 UUID（领地被记在谁名下）

- 游戏内：`/res <出问题的领地名>` → 看 `Owner`
- 文件：`plugins/Residence/Residence.yml`（或 `Worlds.yml`，视版本）里的 `Owner:` 字段

### 3.3 3/4 判定法（一眼区分离线 / 正版）

看 UUID 字符串 **第 3 段的开头字符**（`xxxxxxxx-xxxx-Xxxx-...` 的第 13 个十六进制字符）：

| 第 3 段首字符 | 类型 | 对应 online-mode |
|---|---|---|
| `3` | 离线 UUID（按用户名算） | `false` |
| `4` | 正版 Mojang UUID（随机） | `true` |

例：`d4e0c2a1-2b3c-**3**d4e-...` → 离线；`069a79f4-44e9-**4**726-...` → 正版。

### 3.4 比对结论

- 两边 UUID **一致且相等** → 没脱钩；若仍看不到领地，可能是领地被删（查备份）。
- 两边第 3 段首字符**不同（一个 3 一个 4）** → 领地建于旧模式、玩家现为新模式 → 这就是丢领地的直接原因。

## 四、UUID 列表文件位置（相对服务端根目录）

| 文件 | 路径 | 说明 |
|---|---|---|
| `usercache.json` | 服务端根目录（与 `server.properties` 同级） | **核心 UUID 列表**，含所有曾进服玩家 |
| Essentials 映射 | `plugins/Essentials/usermap.csv` | `UUID,玩家名` 两列 |
| Essentials 数据 | `plugins/Essentials/userdata/<UUID>.yml` | 文件名即 UUID |
| 玩家存档 | `world/playerdata/<UUID>.dat` | 文件名即 UUID，需配合 usercache 对应真人 |

> 仅记录**实际进过服**的玩家；离线模式下存的是离线 UUID（第 3 段 `3` 开头）。

## 五、修复流程（止血 + 对账，只做一次）

1. **选定 `online-mode` 值并永久锁定**（正版服 `true` / 破解服 `false`），从此不再改。
2. **回到数据最初创建时的那个值** → 重启 → UUID 稳定 → 数据通常**自动**对上，无需 setowner。
   - 离线 UUID 按用户名确定，改回 `false` 即恢复同一 UUID；正版 UUID 固定，改回 `true` 即恢复。
3. 若**执意留在新模式**（如从离线转正版）：旧数据全在旧 UUID 下，只能逐领地重绑：
   ```
   /resadmin setowner <领地名> <玩家全名>
   ```
   （玩家**必须在线**；基岩玩家名带 `.`，如 `.NoviceMite1987`，Tab 补不出来要手打）
4. 领地若被删（连旧 UUID 都查不到）→ 只能从备份恢复 `Residence` 数据文件，setowner 救不回。

## 六、为什么 setowner "没用"（三个常见原因）

1. **还在 toggle online-mode**：刚修好又被翻面，永远追不上 —— 先锁死配置再修。
2. **基岩玩家漏 `.` 前缀**：`/resadmin setowner` 的玩家名若是基岩玩家，必须手打全名带 `.`，否则当成不存在的 Java 玩家 → 静默失败。
3. **玩家不在线**：setowner 按名字解析 UUID，离线玩家可能解析不到正确 UUID → 写歪。

## 七、相关文档索引

- **BlueMap 命令修正**：正确暂停/恢复是 `/bluemap stop` / `/bluemap start`（**不是** `pause`/`resume`）；详见 `3-玩法与玩家功能插件/17-网页世界地图-BlueMap/BlueMap.md`。
- **Residence 管理员命令**：单字 `/resadmin setowner <领地名> <玩家名>`（非 `/res admin` 两词）；基岩带 `.`、Tab 不补全。详见 `3-玩法与玩家功能插件/04-领地系统-Residence/Residence.md`。
- **EconomyShop 更新配置**：改 `config.yml` 后执行 `/shop admin reload`（权限 `economyshop.admin.reload`，op 默认有）；切 MySQL 存储需整服重启。详见 `3-玩法与玩家功能插件/02-经济系统-Vault/经济插件/EconomyShop玩家商店.md`。

## 八、命令 / 配置速查

| 用途 | 命令 / 文件 |
|---|---|
| 看玩家当前 UUID | `/lp user <名> info` |
| 看领地归属 | `/res <领地名>` |
| 管理员改领地主人 | `/resadmin setowner <领地名> <玩家名(基岩带.)>` |
| 锁定配置 | `server.properties` → `online-mode: true/false`（选定后不改） |
| UUID 列表 | 服务端根目录 `usercache.json` |
