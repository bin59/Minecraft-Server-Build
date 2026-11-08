# 10. 向下兼容 — ViaBackwards

> **一句话**：ViaBackwards 让**比服务端更旧的 Java 版客户端**连入服务器，是 ViaVersion 的扩展插件，硬依赖 ViaVersion，不能单独运行。
>
> **本服 jar**：`plugins/ViaBackwards-5.12.2.jar`，配套 `plugins/ViaVersion-5.12.2.jar`
>
> **配置文件**：`plugins/ViaBackwards/config.yml`
>
> **官方网站**：https://viaversion.com/backwards ｜ **文档**：https://docs.viaversion.com ｜ **源码**：https://github.com/ViaVersion/ViaBackwards

## 一、它解决什么问题

Via 系列按「客户端版本相对服务端是更新还是更旧」分工，官方组合如下：

| 加入的客户端 | 需要安装的插件 | 本服状态 |
|---|---|---|
| 比服务端**更新** | ViaVersion | ✅ 已装 |
| 比服务端**更旧**（低至 1.9） | ViaVersion + **ViaBackwards** | ✅ 已装 |
| 1.7.x – 1.8.x | 再加 ViaRewind | ❌ 未装 |

本服装了 ViaVersion + ViaBackwards，因此旧版 Java 客户端可以加入；**1.7.x / 1.8.x 客户端连不上**，要支持就得补装 ViaRewind。

> ⚠️ 常见混淆：ViaVersion 的方向是「新客户端 → 旧服务端」，ViaBackwards 才是「旧客户端 → 新服务端」。本章节的「向下兼容」指**向下兼容旧客户端**。

**能力边界**：ViaBackwards 只做协议与数据翻译，**不能给旧客户端补上新版本的功能**。官方明确列出的限制：

- 1.17+ 的非 `0/256` 世界高度不支持；1.17 以下客户端看不到也无法交互 `y<0` 与 `y>255` 的方块。
- 1.17 以下客户端在某些背包点击操作上可能出现物品不同步。
- 音效映射不完整。
- 1.19.4 以下客户端在 1.20+ 服务端上无法使用锻造台（如需修复要另装 AxSmithing）。

基岩版客户端走 **Geyser + Floodgate** 通道，不经过 ViaBackwards；基岩端的问题请查 `03-Java-Bedrock互通层-Geyser-Floodgate/`。

## 二、安装与依赖

`ViaBackwards-5.12.2.jar` 的 `plugin.yml`：

```yaml
name: ViaBackwards
version: 5.12.2
main: com.viaversion.viabackwards.BukkitPlugin
api-version: 1.13
folia-supported: true
depend: [ViaVersion]          # 硬依赖：缺 ViaVersion 直接不加载
website: "https://viaversion.com/backwards"
```

- **要求 Java 17+**（本服 Java 25）。
- 两个 jar 一起放进 `plugins/`，Bukkit 会按 `depend` 保证加载顺序；首次启动自动生成 `plugins/ViaBackwards/config.yml`。
- jar 版本要与 ViaVersion 保持同一发布批次，不要单独升降其中一个。
- 替换 jar 或改配置文件请**停服替换 + 完整重启**，不要用 `/reload`。
- `plugins/ViaBackwards/` 下只有 `config.yml` 一个文件，没有数据库、没有额外数据目录。

## 三、配置要点（`plugins/ViaBackwards/config.yml` 实际值）

本服全部保持官方默认值，未做自定义修改：

```yaml
# 把自定义模型数据与 1.21.4+ 原版物品名传入 custom_model_data 字符串，
# 让客户端资源包能还原旧版本里完全缺失的新物品外观
pass-original-item-name-to-resource-packs: true
# 始终显示被映射生物的原名，而不是只在准星悬停时显示
always-show-original-mob-name: true
# 把自定义附魔的名称与等级写进物品 lore（附魔插件自己写了就设 false，避免重复）
add-custom-enchants-into-lore: true
# 1.12 客户端在 1.13+ 服务端上，把队伍颜色补到 prefix 之后
add-teamcolor-to-prefix: true
# 转换 1.13 的 look-at 脸朝向包给 1.12- 客户端，需额外缓存；本服关闭
fix-1_13-face-player: false
# 1.13- 客户端容器标题的颜色/格式转 legacy 文本；可翻译文本显示错乱时设 false
fix-formatted-inventory-titles: true
# 用背包确认包代替 ping 包发给 1.17- 客户端，反作弊兼容用途；本服关闭
handle-pings-as-inv-acknowledgements: false
# 给 1.17- 客户端在 y=0 补假基岩，可能引发奇怪交互；本服关闭
bedrock-at-y-0: false
# 1.18.2 客户端在 1.19+ 服务端上，幽匿尖啸体显示为哭泣黑曜石（修碰撞与破坏）；
# 关掉则显示为末地传送门框架
sculk-shriekers-to-crying-obsidian: true
# 1.13.2 客户端在 1.14+ 服务端上，脚手架显示为水（修碰撞）；关掉则为干草块
scaffolding-to-water: false
# 1.18.2 客户端在 1.19+ 服务端上，把黑暗效果映射为失明
map-darkness-effect: true
# 1.21.3 客户端接收 1.21.4+ 自定义模型数据的第一个 float 作为 int
map-custom-model-data: true
# 1.19.3 客户端在 1.19.4+ 服务端上，Display 实体以带自定义数据的盔甲架呈现
# （不支持 Display 实体的全部特性）
map-display-entities: true
# 抑制"某些特性没有模拟实现"的警告（如世界高度）；本服 false = 保留警告
suppress-emulation-warnings: false
# 1.21.5 客户端在 1.21.6+ 服务端上，对话框用箱子界面模拟
dialogs-via-chests: true
# 1.21.7 客户端在 1.21.9+ 服务端上，行为准则作为对话框显示
# （1.21.6 以下客户端不支持配置阶段的对话框）
code-of-conduct-as-dialog: true
```

对话框的文案样式（`dialog-style`，13 项全部为官方默认值，本服未改）：

```yaml
dialog-style:
  page-navigation-title: '&9&lPage navigation'
  page-navigation-next: '&9Left click: &6Go to next page'
  page-navigation-previous: '&9Right click: &6Go to previous page'
  toggle-value: '&9Left click: &6Toggle value'
  increase-value: '&9Left click: &6Increase value by %s'
  decrease-value: '&9Right click: &6Decrease value by %s'
  value-range: '&7(Value between &a%s &7and &a%s&7)'
  current-value: '&7Current value: &a%s'
  edit-value: '&9Left click: &6Edit text'
  next-option: '&9Left click: &6Go to next option'
  previous-option: '&9Right click: &6Go to previous option'
  set-text: '&9Left click/close: &6Set text'
  close: '&9Left click: &6Close'
```

几个与本服直接相关的开关：

| 配置项 | 本服值 | 影响 |
|---|---|---|
| `add-custom-enchants-into-lore` | `true` | 自定义附魔写进 lore；若带组件物品的 lore 出现**重复条目**，改成 `false` |
| `pass-original-item-name-to-resource-packs` | `true` | 与服务器资源包配合还原新物品外观，见 `08-服务器资源包-BGM/` |
| `map-display-entities` | `true` | 旧 Java 客户端会把 Display 实体看成盔甲架；这是**协议映射**，与 Geyser 基岩端看不到 Item Display 是两回事 |
| `bedrock-at-y-0` | `false` | 不造假基岩，避免主城/领地低空建造出现幽灵方块 |
| `suppress-emulation-warnings` | `false` | 保留警告日志，便于发现旧客户端遇到的未模拟特性 |

## 四、命令与权限

**ViaBackwards 5.12.2 没有独立的 Bukkit 命令**：`plugin.yml` 里没有 `commands` 段，jar 内的 `BukkitPlugin` 也只有 `onEnable` / `enable` / `disable`。所有管理操作走 ViaVersion 的命令：

| 命令 | 作用 | 权限 |
|---|---|---|
| `/viaversion`（别名 `/viaver`、`/vvbukkit`） | 查看版本与已加载的 Via 系列插件 | `viaversion.command` |
| `/viaversion list` | 列出在线玩家的客户端版本 | `viaversion.command.list` |
| `/viaversion reload` | 重载配置 | `viaversion.command.reload` |
| `/viaversion dump` | 生成诊断信息，报障时附上 | `viaversion.command.dump` |
| `/viaversion pps` | 查看数据包速率 | `viaversion.command.pps` |

`viaversion.admin` 默认给 OP，包含上面全部子命令权限。

> ⚠️ 改完 `ViaBackwards/config.yml` **优先完整重启**。`ViaVersion/config.yml` 里的 `reload-disconnect-msg` 就是为 `/viaversion reload` 准备的——装了 ProtocolLib 时 reload 会把在线玩家踢下线。

版本封禁、数据包限速这些**全局策略在 ViaVersion 的 config.yml 里**（`block-versions`、`block-protocols`、`packet-limiter`），不在 ViaBackwards 这边，见 `04-跨版本协议兼容-ViaVersion/ViaVersion.md`。

## 五、常见问题

| 现象 | 排查 |
|---|---|
| 旧版 Java 客户端连不上 | ① 确认两个 jar 都在且版本一致；② 查 ViaVersion 的 `block-versions` / `block-protocols` 有没有封禁；③ `/viaversion list` 看已连入玩家的客户端版本；④ 看启动日志有没有 ViaBackwards 加载失败 |
| 1.7.x / 1.8.x 客户端连不上 | 本服**未装 ViaRewind**，这是预期行为；官方也明确 ViaBackwards 不覆盖 1.8 及以下 |
| 旧客户端看不见 `y<0` 的方块 / 背包物品错乱 | 官方已知限制，协议层无法修复；让该玩家用与服务端相近的客户端版本 |
| 带自定义附魔的物品 lore 出现重复 | 把 `add-custom-enchants-into-lore` 设为 `false`（附魔插件通常会自己写 lore） |
| 旧客户端容器标题的可翻译文本显示异常 | 把 `fix-formatted-inventory-titles` 设为 `false` |
| 自定义物品/方块外观不对 | 检查 `pass-original-item-name-to-resource-packs` 与服务器资源包是否都生效 |
| 基岩玩家看不到东西 | 与 ViaBackwards 无关，走 Geyser/Floodgate 排查 |
| 换 jar 后配置没变化 | 插件不会覆盖已有的 `config.yml`；需要新默认值时先备份配置再删除让它重建 |

## 六、相关文档

- `04-跨版本协议兼容-ViaVersion/ViaVersion.md` — 基础协议层、版本封禁与数据包限速
- `09-协议库-ProtocolLib/ProtocolLib.md` — reload 会踢人的原因
- `03-Java-Bedrock互通层-Geyser-Floodgate/` — 基岩端通道
- `08-服务器资源包-BGM/` — 与 `pass-original-item-name-to-resource-packs` 配合的资源包
