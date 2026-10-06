# LuckPerms + TAB + PlaceholderAPI 炫彩多层称号

用 **LuckPerms + TAB + PlaceholderAPI** 实现玩家头顶 / TAB 列表的多行炫彩称号（本服当前启用状态见文末）。

## 前置条件

- **TAB**（列表 + 头顶名牌）、**LuckPerms**（权限组 + 前缀）、**PlaceholderAPI**（占位符解析）
- LuckPerms 的 PAPI 扩展：`/papi ecloud download LuckPerms` → `/papi reload`

## 一、LuckPerms 设置多行前缀

TAB 支持用 `\n` 实现多行显示，在 LuckPerms 中给权限组设置前缀：

```bash
# 管理员 - 三行炫彩称号
/lp group admin meta setprefix "&4&l✦ 管理员 ✦\n&c&lSERVER\n&7"
# 普通玩家 - 单行
/lp group default meta setprefix "&7"
```

## 二、TAB 头顶多行显示

`plugins/TAB/config.yml`：

```yaml
tablist-name-formatting:
  default:
    tabprefix: '%luckperms_prefix%'
    tabname: '&f%player_name%'
    tabsuffix: ''
    tagprefix: '%luckperms_prefix%'
    tagname: '&f%player_name%'
    tagsuffix: ''
```

头顶效果：

```
    ✦ 管理员 ✦
    SERVER
  Steve
```

## 三、渐变效果（1.16+ HEX 色）

TAB 支持 `&#RRGGBB` 十六进制颜色，逐字变色形成渐变：

```bash
/lp group admin meta setprefix "&#FF0000&l✦ &#FF4400管 &#FF8800理 &#FFAA00员 &#FF0000&l✦ &#FF6600&lSERVER &7"
```

## 四、彩虹动画

TAB 内置动态彩虹占位符，动画在 `animations.yml` 定义（键为 `change-interval` + `texts`，`change-interval` 单位毫秒），在 LuckPerms 前缀中组合使用即可。

## 五、TAB 列表多行显示

```yaml
tablist-name-formatting:
  default:
    tabprefix: '%luckperms_prefix%'
    tabname: '&f%player_name%'
    tabsuffix: ' &7| &f%player_health%&c❤'
    tagprefix: '%luckperms_prefix%'
    tagname: '&f%player_name%'
    tagsuffix: ''
```

## 常用命令速查

| 命令                                    | 作用                 |
| --------------------------------------- | -------------------- |
| `/lp group <组> meta setprefix <前缀>`  | 设置权限组前缀       |
| `/lp user <玩家> meta setprefix <前缀>` | 单独给某玩家设置前缀 |
| `/lp user <玩家> parent add <组>`       | 将玩家加入权限组     |
| `/tab reload`                           | 重载 TAB 配置        |

## 前缀设置示例

- 默认组：`/lp group default meta setprefix "[&d玩家&f] &6"`
- 基岩版（Floodgate 按 UUID）：`/lp user 00000000-0000-0000-0009-01f42087bd04 meta setprefix &a[青蛙🐸]`
- 指定 Java 玩家：`/lp user PumpkinVegetable meta setprefix "[&4南瓜🎃&f] &6"`

`/lp user TODAO_owo meta setprefix "[&4至道大王👑&f][玩家] &6"`

## 注意事项

- 多行前缀**最后一行**应为玩家名颜色前缀（如 `&7`），否则名字会继承上一行颜色
- `\n` 换行在头顶名牌（tag）有效，TAB 列表中可能只显示第一行
- HEX 颜色 `&#RRGGBB` 仅 **1.16+** 生效，低版本自动降级为最近颜色码
- 前缀不生效时执行 `/papi parse me %luckperms_prefix%` 验证占位符是否正常返回

## 本服实际状态

`plugins/TAB/config.yml` 中 `tablist-name-formatting.enabled: false`——本服**未启用**该组件，头顶 / TAB 列表前缀实际由 `scoreboard-teams`（`enabled: true`）读取 LuckPerms 前缀渲染。若要启用本方案的多行炫彩称号，先把 `tablist-name-formatting.enabled` 改为 `true` 并 `/tab reload`。
