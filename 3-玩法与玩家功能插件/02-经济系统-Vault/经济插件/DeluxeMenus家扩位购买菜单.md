# DeluxeMenus 家扩位购买菜单

本文档介绍南瓜生存服经济体系第二阶段落地的家扩位购买菜单（DeluxeMenus 1.14.1 的 `homes_menu`），说明其作为**货币回收（Sink）工具**的定位、三档家园上限购买配置、扣款授权一体流程，以及打开命令的兼容性处理。适用于管理员维护家扩位菜单、排查打开命令问题时使用。

> 落地环境：测试服 `C:\mc_serve\1.21.11-test\`（Leaf 1.21.11）｜ 落地日期：2026-09-17
> DeluxeMenus 插件本体文档见 `3-玩法与玩家功能插件/15-DeluxeMenus高级GUI菜单/DeluxeMenus高级GUI菜单.md`

## 功能定位

- **家园扩容消费点**：玩家用南瓜币购买家园数量上限，是存量货币的主要消费出口之一。
- **Sink 设计**：三档价格直接扣款（`eco take`），货币从玩家账本消失。
- **一键生效**：扣款 + 授权（`lp user ... permission set`）一步完成，购买后立即生效。

## 菜单文件与注册

- 菜单文件：`plugins/DeluxeMenus/gui_menus/homes_menu.yml`（已注册进 `plugins/DeluxeMenus/config.yml`）。
- 重启日志确认：`4 GUI menus loaded!`。

## 购买档位（当前值）

| 按钮 | 价格 | 效果 | 授权权限 |
|---|---|---|---|
| 家园上限 +1（→2 个家） | 5,000 南瓜币 | 家园数上限提升 1 | `essentials.sethome.multiple.2` |
| 家园上限 +2（→3 个家） | 10,000 南瓜币 | 家园数上限提升 2 | `essentials.sethome.multiple.3` |
| 家园上限 +3（→4 个家） | 15,000 南瓜币 | 家园数上限提升 3 | `essentials.sethome.multiple.4` |

- 余额不足或已达上限时显示锁定按钮，无法购买。

## 打开命令

| 命令 | 说明 |
|---|---|
| `/homeshop` | 打开家扩位菜单 |

> **兼容性处理**：DeluxeMenus 的 `open_command` 在 Leaf 上未注册（已知兼容问题），已用 Bukkit 级 `commands.yml` 别名兜底：`homeshop → dm open homes_menu`（重启后生效，已验证）。升级插件后需复查是否仍需别名。

## 与经济体系的关系

- **Sink 项**：家园扩容费（5000/10000/15000 南瓜币），是玩家主动消费的主要出口，计入周 Sink 统计。
- **与领地费配合**：家园（EssentialsX `/sethome`）与领地（Residence 按格计费）构成两套"居住扩容"消费，分别对应 `/homeshop` 菜单和 `/res create`。

## 注意事项

- 菜单内使用的符号（❖✔✖）为 Minecraft 默认字体支持的字符，未使用会显示为方块的 emoji。
- 购买价格可随通胀情况调整（调参杠杆见《经济系统落地实施-测试服.md》§4）。
