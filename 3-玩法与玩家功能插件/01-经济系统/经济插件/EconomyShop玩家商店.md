# EconomyShop 玩家商店

本文档介绍南瓜生存服经济体系的玩家商店插件 EconomyShop（iliüs），说明其作为玩家间交易渠道的定位、防印钞关键配置（关闭系统商店回收）、开店费与成交税，以及玩家命令和权限。

> 落地环境：测试服 `C:\mc_serve\1.21.11-test\`（Leaf 26.3）｜ 落地日期：2026-09-17

## 功能定位

- **玩家商店渠道**：玩家开设个人商店或木牌商店，向其他玩家出售物品，与拍卖行（/ah）互为补充的交易渠道。
- **防印钞核心**：关闭"卖物品给系统商店"和 `/sell` 类命令，堵住量产物品刷钱的漏洞（农场刷不出钱，见《经济系统设计手册》§4D）。
- **Sink 设计**：开店收费 + 玩家间成交税，两处抽水。

## 版本与来源

- 版本：**1.1.6（iliüs）**，从 Modrinth 官方下载，基于官方默认模板改写配置。
- 配置文件：`plugins/EconomyShop/config.yml`。

## 关键配置（当前值）

| 键                        | 值      | 说明                                                                                               |
| ------------------------- | ------- | -------------------------------------------------------------------------------------------------- |
| `sell-to-shop`            | `false` | 玩家无法卖物品给系统商店（防印钞）                                                                 |
| `seed-default-shop`       | `false` | 不自动生成默认商店                                                                                 |
| `sell-commands`           | `false` | 关闭 EconomyShop 自带 `/sell` 类快捷卖命令（⚠️ 仅本插件；EssentialsX 的 `/sell` 需另行禁用，见下） |
| `creation-cost`           | `100.0` | 开店费用 100 南瓜币（Sink）                                                                        |
| `transaction-tax-percent` | `5.0`   | 玩家商店成交税 5%                                                                                  |

## EssentialsX /sell 关闭方法（2026-10-01 更新）

`EconomyShop` 的 `sell-commands` 只关它自己，**EssentialsX 的 `/sell`、`/sellall`、`/worth` 与 `[sell]` 木牌是独立的印钞口**（卖给系统、系统凭空付钱），需两层一起关：

1. `plugins/Essentials/config.yml` → `disabled-commands:` 加 `sell`、`sellall`、`worth`（命令级禁用，已配置，`/ess reload` 生效）；
2. LuckPerms 移除 default 组权限（控制台/游戏内执行，**完整 8 条，已对照 EssentialsX 2.22.1-dev+28 实际权限树**）：

```
lp group default permission unset essentials.sell
lp group default permission unset essentials.sell.hand
lp group default permission unset essentials.sell.bulk
lp group default permission unset essentials.worth
lp group default permission unset essentials.setworth
lp group default permission unset essentials.signs.create.sell
lp group default permission unset essentials.signs.use.sell
lp group default permission unset essentials.signs.break.sell
```

> 注：`essentials.sell.others` 在现版本不存在（无需 unset）；`/sellall` 无对应权限节点（disabled-commands 兜底即可）。执行后 `/lp group default info` 确认。

### `[sell]` 木牌 vs `[Shop]` 木牌（重要区分）

| | EconomyShop `[Shop]` | Essentials `[sell]` |
| --- | --- | --- |
| 谁买 | 其他玩家 | 系统 |
| 钱从哪来 | 买家余额（玩家间直接转账） | 系统凭空生成 |
| 性质 | 玩家间交易（健康，保留） | 印钞口（禁止） |

关闭 Essentials 的 `/sell`、`[sell]` 不影响 EconomyShop 玩家商店：`[Shop]` 牌照常运作，钱仍在玩家之间流动。

> ⚠️ `worth.yml` 当前仍是 Essentials **默认全价表**（铁锭 22 / 钻石 200 / 金锭 105…），若命令级/权限级禁用失效，它就是兜底漏洞——必要时可清空或按《经济系统落地实施》只留半稀缺地板价。

## 命令

| 命令         | 用途                                  |
| ------------ | ------------------------------------- |
| `/shop`      | 打开/创建玩家商店（权限已授 default） |
| `/chestshop` | 创建木牌商店（权限已授 default）      |

## 玩家开店步骤

### 方式 A：木牌商店（实体店，放世界上，推荐）

1. **放箱子**：在地上放一个箱子。
2. **放牌子**：在箱子旁边（或正上方）放告示牌，第一行写 `[Shop]`。
3. **自动弹出设置界面**：把要卖的物品放进去 → 设**买价**（玩家购买价）→ 确认。
4. **完成**：牌子自动更新显示物品名与价格；其他玩家**右键点牌子**即可购买。

- **补货**：打开箱子放入更多物品即可，无需重设。
- 开店费 **100 南瓜币**（一次性），每笔成交抽 **5%** 税。

### 方式 B：`/shop` 商店 GUI

- 输入 `/shop` 打开个人商店界面，在 GUI 里创建/管理商店（放物品、标价、补货）。
- 快捷菜单主菜单「玩家商店」按钮直达（`[player] shop`）。

### 玩家须知

- **不能卖物品给系统商店**（`sell-to-shop: false`），只做玩家间买卖。
- 定价留出 5% 成交税 + 8% 拍卖行税空间，别定太满。
- 权限已全部授予 default 组（`economyshop.use` 等 4 个），玩家开箱即用。

## 权限（LuckPerms default 组，已授予）

| 权限                           | 说明         |
| ------------------------------ | ------------ |
| `economyshop.use`              | 使用商店功能 |
| `economyshop.sell`             | 出售物品     |
| `economyshop.chestshop.create` | 创建木牌商店 |
| `economyshop.chestshop.use`    | 使用木牌商店 |

## 交易资金流向（玩家间直接转账）

玩家商店走 **Vault 余额转账**，买卖双方直接结算，系统只抽水：

| 环节      | 金额                | 去向                                 |
| --------- | ------------------- | ------------------------------------ |
| 买家付款  | 扣全价（如 100 币） | 买家余额 → Vault 转账                |
| 卖家收款  | 到账 价格 × 95%     | **直接进卖家玩家余额**（玩家对玩家） |
| 成交税 5% | 系统抽走 5%         | Sink 抽水，不进任何玩家口袋          |
| 开店费    | 100 南瓜币          | 开店瞬间扣除，系统收走（一次性）     |

- **买家付的钱给卖家玩家，不是给系统**；系统仅在每笔交易抽 5% 税、开店时收 100 开店费。
- `sell-to-shop: false` 只关闭**玩家卖给系统商店**，不影响玩家间交易：系统不当买方后，钱始终在玩家圈子里流动（除税外不产生新钱），堵死量产物品印钞口。

## 与经济体系的关系

- **Sink 项**：开店费 100 南瓜币（一次性）+ 玩家商店成交税 5%（每笔交易）。
- **防印钞**：`sell-to-shop: false` + `sell-commands: false` 是手册 §4D 的关键落地，量产物品只能走玩家间交易。
- **监控**：启动日志确认 `Hooked into LuckPerms`、`AuctionHouse integration enabled`、`Default-shop seeding disabled`。

## 更新配置（修改后重载）

改完 `config.yml` 后按以下流程让配置生效，一般无需重启：

1. **备份**：编辑前先复制一份 `config.yml`，YAML 对缩进敏感，只用空格、别用 Tab。
2. **重载**：游戏内或控制台执行
   ```
   /shop admin reload
   ```
   权限节点 `economyshop.admin.reload`（op 默认持有）。该命令会重载 `config.yml`、语言文件和商店 GUI。
3. **必须重启的场景**：若修改存储后端（将 `storage.type` 从默认改为 `mysql` 并填写 `storage.mysql` 的 host/port/database/user/pass），`reload` 不会切换数据源，**必须整服重启**才生效。
4. **验证**：重载后观察执行日志无 YAML Exception / 格式报错；再用 `/shop` 开一个店，确认开店费、成交税按新值计算。

> 同名插件说明：EconomyShopGUI 等分支用 `/eshop reload`；本服使用的是 iliüs 版，认准 `/shop admin reload` 即可。

## 管理员上架带附魔/特殊属性的物品（系统商店）

系统商店给玩家**购买**的商品（区别于玩家间商店），默认只能上架"材质 + 数量"（如 `minecraft:diamond`）。要上架**带附魔、带数据组件、带 lore/自定义名**等特殊物品，走 EconomyShop 的「自定义物品」通道——它存的是整个 `ItemStack` 的 Bukkit 序列化（Base64），NBT/组件/lore 全保留。

### 原理
- `es_items.item_data` 列存的是 **Base64 + BukkitObjectOutputStream 序列化的完整 ItemStack**，不是可读 SNBT，也不是 `/give` 命令字符串。
- 上架时，插件把"你手里的实物"整件序列化写库；玩家购买时反序列化 → `setAmount` → 进背包，**组件原样保留**。
- 所以所有"特殊属性"都来自你**手里那件实物**，而不是命令文本本身。

### 方法一：手持上架（推荐，最稳）
1. 先用 `/give` 给自己造出这件特殊物品（1.21.x 用数据组件语法）：
   ```
   /give @s minecraft:stick[minecraft:enchantments={levels:{minecraft:knockback:10}}]
   ```
   - 这是 1.21 的 **item component** 写法（方括号 + 键值对），不是旧版 NBT 大括号里的 `Enchantments` 标签。
   - 命令给的**超上限附魔**（如 knockback 10，远超附魔台上限 2）会原样保留；只要序列化保留组件，玩家买回来仍是 10 级。
   - 基岩端用 `/minecraft:give`（带命名空间前缀），避免被 `commands.yml` 别名吞掉材质名。
2. 把物品**拿在右手**。
3. 游戏内 `/shop admin` → 进对应分类 → 找到子分类「自定义物品」(`CUSTOM_ITEMS`) → 点 **slot22 “Add Custom Item”**（手持物品点击它，会抓取你手里的 ItemStack）。
4. 设买价、数量 → 确认。买回来的物品与手里这件完全一致（附魔/组件/lore 全在）。
   - 系统商品通常**只设买价**，`sell_price` 留 0（因全局 `sell-to-shop: false`，玩家不能卖给系统商店）。

### 方法二：批量上架（sync_shop.py）
- 当前 `sync_shop.py` 写死 `item_data=None`，**只能上架普通材质物品**（Excel 的 material 列），**不支持**附魔/组件列。
- 若要批量上架带组件物品，需脚本把组件字符串解析成 `ItemStack` 再序列化写 `item_data`——原版 `/give` 字符串 EconomyShop 不能直吃。
- 该能力为**待办**：将脚本扩展为支持"组件列"，一行写 `minecraft:stick[minecraft:enchantments={levels:{minecraft:knockback:10}}]` 之类，自动转成 ItemStack 上架。

### 注意事项
- **不能直接把 `/give` 命令字符串贴进商店配置**：EconomyShop 不吃命令，只吃"实物序列化"。要么手持上架，要么脚本喂 ItemStack。
- 上架后务必 `/shop admin reload` 重载，并让一名玩家试买，确认附魔/组件确实到账（防老版本序列化丢组件）。
- 特殊物品定价应明显高于其"裸材质"市价，避免被囤货倒卖套利。

## 注意事项

- 关闭 `/sell` 后玩家可能不适应，需在公告中说明"量产物品走玩家间交易（/ah /shop）"。
- 玩家商店成交税 5% 与拍卖行成交税 8% 并存，定价时注意让利空间。
