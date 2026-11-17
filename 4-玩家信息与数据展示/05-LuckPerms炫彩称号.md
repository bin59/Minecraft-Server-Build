# LuckPerms + TAB + PlaceholderAPI 炫彩称号

用 **LuckPerms + TAB + PlaceholderAPI** 实现玩家头顶 / TAB 列表的炫彩称号（本服当前启用状态见文末）。

## 前置条件

- **TAB**（列表 + 头顶名牌）、**LuckPerms**（权限组 + 前缀）、**PlaceholderAPI**（占位符解析）
- LuckPerms 的 PAPI 扩展：`/papi ecloud download LuckPerms` → `/papi reload`

## 一、LuckPerms 设置前缀

TAB v6.2.1 头顶名牌为单行，`\n` 不换行（只显示字面字符）。多行称号需第三方名牌插件。

```bash
/lp group admin meta setprefix "&4&l✦ 管理员 ✦ &7"
/lp group default meta setprefix "&7"
```

## 二、TAB 头顶显示

值写在 `plugins/TAB/groups.yml`：

```yaml
# plugins/TAB/groups.yml
_DEFAULT_:
  tabprefix: '%luckperms-prefix%'
  customtabname: '%player%'
  tagprefix: '%luckperms-prefix%'
```

## 三、渐变效果（1.16+ HEX 色）

TAB 支持 `&#RRGGBB` 十六进制颜色，逐字变色形成渐变：

```bash
/lp group admin meta setprefix "&#FF0000&l✦ &#FF4400管 &#FF8800理 &#FFAA00员 &#FF0000&l✦ &#FF6600&lSERVER &7"
```

## 四、彩虹动画

TAB 内置动态彩虹占位符，动画在 `animations.yml` 定义（键为 `change-interval` + `texts`，`change-interval` 单位毫秒），在 LuckPerms 前缀中组合使用即可。

## 五、TAB 列表格式

```yaml
# plugins/TAB/groups.yml
_DEFAULT_:
  tabprefix: '%luckperms-prefix%'
  customtabname: '%player%'
  tabsuffix: '%luckperms-suffix%'
  tagprefix: '%luckperms-prefix%'
  tagsuffix: '%luckperms-suffix%'
```

## 常用命令速查

| 命令                                    | 作用                 |
| --------------------------------------- | -------------------- |
| `/lp group <组> meta setprefix <前缀>`  | 设置权限组前缀       |
| `/lp user <玩家> meta setprefix <前缀>` | 单独给某玩家设置前缀 |
| `/lp user <玩家> parent add <组>`       | 将玩家加入权限组     |
| `/tab reload`                           | 重载 TAB 配置        |

## 前缀设置示例

### 1.初始前缀设置

（1）默认组
`/lp group default meta setprefix "[&d玩家&f] &6"`

（2）基岩版玩家
需要用UUID设置

```bash
/lp user 00000000-0000-0000-0009-01f42087bd04 meta setprefix "&a[青蛙🐸] "
/lp user 00000000-0000-0000-0009-01f068214d30 meta setprefix "&d[枣温] &6"
/lp user 00000000-0000-0000-0009-01f9d96dcdd1 meta setprefix "&d[末] &6"
/lp user 00000000-0000-0000-0009-01f702bfe1ff meta setprefix "&d[魖飕(野人版)] &6"
/lp user 00000000-0000-0000-0009-01f4c3b0ae33 meta setprefix "&d[nangua1896] &6"
/lp user 00000000-0000-0000-0009-01f0f44e3cb2 meta setprefix "&d[镜寒火] &6"
/lp user 00000000-0000-0000-0009-01f129f7ffea meta setprefix "&d[海虾] &6"
/lp user 00000000-0000-0000-0009-01f6630347a2 meta setprefix "&d[派大星] &6"
/lp user 00000000-0000-0000-0009-01f1c7110173 meta setprefix "&d[BAIQUE白雀] &6"
/lp user 00000000-0000-0000-0009-01f4352179d9 meta setprefix "&d[余念本乐] &6"
/lp user 00000000-0000-0000-0009-01fda5a6602e meta setprefix "&d[忧的郁] &6"
/lp user 00000000-0000-0000-0009-01f077e6618e meta setprefix "&d[汽修二班 柳贯一] &6"
/lp user 00000000-0000-0000-0009-01f46d426bce meta setprefix "&d[SB.ery] &6"
/lp user 00000000-0000-0000-0009-01fa9c76aa41 meta setprefix "&d[布偶ovo] &6"

```

（3）Java版玩家
用真实id

```bash
/lp user yyzsy meta setprefix "&d[猫猫🐱] &6"
/lp user tiebi666 meta setprefix "&d[铁臂] &6"
/lp user JPZS9527 meta setprefix "&d[江姐农场主] &6"
/lp user j4rvx meta setprefix "&d[j4rvx] &6"
/lp user MatchaKing meta setprefix "&d[抹茶] &6"
/lp user BambooLoveFlower meta setprefix "&d[花的眷属] &6"
/lp user llbyz meta setprefix "&d[没良心] &6"
/lp user zuie2778 meta setprefix "&d[䧜軬] &6"
```

### 2.活动专属

如pvp比赛前三可自定义称号
`&4[至道大王👑]`

```bash
/lp user TODAO_owo meta setprefix "&4[pvp亚军]&d[至道大王👑] &6"
```

### 3.赞助

`&4[天使投资人]`

```bash
/lp user zuojiating meta setprefix "&4[天使投资人]&d[夏霞] &6"
/lp user 00000000-0000-0000-0009-01fab3620d50 meta setprefix "&4[天使投资人]&d[咸微饼] &6"
```

## 注意事项

- TAB v6.2.1 头顶名牌（tag）为单行，`\n` 不换行、只显示字面字符；多行需第三方名牌插件
- HEX 颜色 `&#RRGGBB` 仅 **1.16+** 生效，低版本自动降级为最近颜色码
- 前缀不生效时执行 `/papi parse me %luckperms_prefix%` 验证占位符是否正常返回

## 本服实际状态

头顶名牌前缀由 `scoreboard-teams` 渲染，值写在 `plugins/TAB/groups.yml` / `users.yml` 的 `tagprefix` / `tagsuffix`；`tablist-name-formatting` 只管 Tab 列表。本地 26.3 两者 `enabled` 均为 `true`。改完 `/tab reload`。
