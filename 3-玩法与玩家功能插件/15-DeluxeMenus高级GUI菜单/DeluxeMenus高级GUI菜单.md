# DeluxeMenus 高级 GUI 菜单

本文介绍 DeluxeMenus 这款用 YAML 定义高级 GUI 菜单的插件，支持命令打开、点击执行、物品条件、PlaceholderAPI 占位符、分页与动态物品，适合做帮助菜单、商店、活动菜单等复杂交互。文档先讲它与自研 QuickMenu 的分工和运行服现状，再列目录结构、常用命令权限、菜单文件骨架和动作/条件写法。

> **插件**：DeluxeMenus | **版本**：1.14.1-Release | **状态**：✅ 已安装（测试服）
> **作者**：extended_clip | **官网**：<https://www.spigotmc.org/resources/deluxemenus.11734/> | **Wiki**：<https://wiki.helpch.at/helpchat-plugins/deluxemenus>

## 一、插件定位

DeluxeMenus 是一个**高度可定制的高级 GUI 菜单插件**：用 YAML 定义菜单界面，支持命令打开、点击执行命令、物品条件（requirements）、占位符（PlaceholderAPI）、分页、动态物品等。

**与 QuickMenu 的分工**：

| 插件         | 负责场景                                                     |
| ------------ | ------------------------------------------------------------ |
| **QuickMenu**（自研） | 触发物品右键打开玩家主菜单，Java 版箱子 GUI + 基岩版原生表单 |
| **DeluxeMenus**      | 需要复杂交互、条件分支、动态刷新的自定义 GUI（帮助菜单、商店、活动菜单等） |

> 当前服务器用 QuickMenu 做玩家日常入口；DeluxeMenus 已安装备用，可按需用 YAML 快速做新 GUI，无需改代码。

## 二、运行服现状

- 配置文件：`plugins/DeluxeMenus/config.yml`
- 菜单目录：`plugins/DeluxeMenus/gui_menus/`
- 已注册菜单（当前为官方示例，均可删除/替换）：

| 菜单文件              | open_command          | 说明                       |
| --------------------- | --------------------- | -------------------------- |
| `advanced_menu.yml`   | `/advancedmenu`（另有别名 `/advancedexamplemenu`、`/themostadvancedmenuintheworld`） | 高级配置示例（27 格，gui_menus/advanced_menu.yml 第4-7行） |
| `basics_menu.yml`     | `/basicsmenu`         | 基础配置教程示例（第17行；注意是 `basicsmenu` 非 `basicmenu`） |
| `requirements_menu.yml` | `/requirementsmenu` | 条件（requirements）教程示例（第14行） |

> ✅ **运行服校准（2026-09-16）**：`config.yml` 第11-17行 `gui_menus:` 注册了上述 3 个菜单文件，与目录一致；主配置 `check_updates: true`（第6行）、`debug: LOW`（第7行）。示例菜单面向新手学习，正式使用前建议删除或改造成自己的菜单。

## 三、目录结构

```
plugins/DeluxeMenus/
├── config.yml           # 主配置：调试级别、菜单注册列表
└── gui_menus/
    ├── advanced_menu.yml
    ├── basics_menu.yml
    └── requirements_menu.yml
```

`config.yml` 的 `gui_menus:` 段把「菜单名 → 文件」注册起来，文件名与菜单名可以不同：

```yaml
gui_menus:
  basics_menu:
    file: basics_menu.yml
```

## 四、常用命令与权限

| 命令            | 说明                     | 权限                    |
| --------------- | ------------------------ | ----------------------- |
| `/dm open <菜单>` | 打开指定菜单             | `deluxemenus.open`      |
| `/dm reload`    | 重载全部菜单与配置       | `deluxemenus.admin`     |
| `/dm list`      | 列出已加载菜单           | `deluxemenus.admin`     |
| `/dm debug`     | 调试模式开关             | `deluxemenus.admin`     |

> 完整权限节点（8 个）见 [5-服务器管理/01-权限管理系统-LuckPerms/权限节点速查.md](../5-服务器管理/01-权限管理系统-LuckPerms/权限节点速查.md)。

## 五、菜单文件要点速记

一个菜单文件的骨架：

```yaml
menu_title: '&8> &6&l菜单标题'      # 界面标题
open_command:                        # 打开命令（可多个）
  - mymenu
  - openmymenu
open_commands:                       # 打开时执行的动作
  - '[sound] BLOCK_NOTE_BLOCK_PLING'
size: 27                             # 格子数（9 的倍数，最大 54）
items:
  example_item:
    material: DIAMOND
    slot: 13                         # 也可以写 0-8/9-17 等行区间
    display_name: '&b示例物品'
    lore:
      - '&7点击执行命令'
    left_click_commands:
      - '[message] &aHello!'
    right_click_commands:
      - '[close]'
```

**常用动作占位符**：

| 动作            | 说明                                     |
| --------------- | ---------------------------------------- |
| `[message] 文本` | 发送消息                                 |
| `[sound] 音效`   | 播放音效                                 |
| `[close]`        | 关闭菜单                                 |
| `[open] 菜单名`  | 打开另一个菜单                           |
| `[console] 命令` | 以控制台身份执行命令                     |
| `[player] 命令`  | 以玩家身份执行命令                       |
| `[refresh]`      | 刷新当前菜单（配合动态物品）             |
| `%placeholder%`  | PlaceholderAPI 占位符（如 `%vault_eco_balance%`） |

**requirements（条件）**：可限制「整个菜单」或「单个物品」仅在满足条件时可用，例如权限、余额、世界、手持物品等：

```yaml
  vip_only_item:
    material: DIAMOND_BLOCK
    slot: 22
    view_requirement:
      requirements:
        is_vip:
          type: has permission
          permission: vip.use
```

## 六、参考

- 官方 Wiki（完整选项/动作/条件）：<https://wiki.helpch.at/helpchat-plugins/deluxemenus>
- 作者 SpigotMC 页面：<https://www.spigotmc.org/resources/deluxemenus.11734/>
