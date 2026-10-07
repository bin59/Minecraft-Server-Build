# 3.4 基岩皮肤修复 BedrockSkinRestorer

本页说明本服用于「基岩版玩家自定义皮肤在 Java 端显示成史蒂夫」的专用修复插件。

| 项 | 内容 |
|---|---|
| 插件 | BedrockSkinRestorer（基岩皮肤修复） |
| 文件 | `plugins/BedrockSkinRestorer.jar` |
| 版本 | 1.0.5 |
| 来源 | Modrinth（project: `bedrock-skin-restorer`），作者 ajsea1sy |
| 兼容 | Bukkit / Paper / Purpur / Folia，MC 1.21.x（含 1.21.11） |
| 依赖 | Geyser-Spigot + Floodgate（可选依赖，Floodgate 已装） |

## 解决什么问题

Floodgate 自带的基岩皮肤上传是**排队制**的：基岩皮肤要先经 `GeyserMC Global API → MineSkin → Mojang` 队列转换并签名，才能成为 Java 端可见的皮肤纹理。这个队列一旦积压（从国内访问尤其慢），基岩玩家进服后 Java 端就一直显示默认 Steve/Alex，要等队列轮到才更新，甚至长期不更新。

BedrockSkinRestorer 的作用就是**绕开这条不可靠的上传队列**：玩家进服时主动抓取基岩客户端的真实皮肤，直接覆盖到该玩家的 GameProfile 上，使 Java 端立即显示基岩玩家客户端里的那张皮。它只作用于 Floodgate 基岩玩家，**不影响 Java 玩家的皮肤**。

## 部署与生效

1. 把 `BedrockSkinRestorer.jar` 放入后端 `plugins/`。
2. **重启服务端**（运行中的服不会热加载新 jar）。
3. 基岩玩家重新进服，Java 端即应显示其客户端自定义皮肤。

插件无需手动配置，装完即生效；如需调整触发时机（进服、换世界等），可在重启后查看生成的 `plugins/BedrockSkinRestorer/config.yml`。

## 与其他皮肤组件的分工

本服 `online-mode=true`，Java 玩家默认皮肤来自 Mojang 正版账号；各皮肤组件在此基础上各管一摊：

| 组件 | 管什么 | 触发方式 |
|---|---|---|
| Mojang 正版皮肤 | Java 玩家的默认皮肤 | 进服自动，来自账号 |
| Geyser `skin-provider-mode` | 基岩端看 Java 玩家的皮肤方向（见 3.2） | 配置项 |
| SkinsRestorer | 玩家**主动选的皮肤**：`/skin` 自助换肤；认带 `.` 前缀的基岩玩家（设皮肤时名字必须带点号） | `/skin` 命令 |
| **BedrockSkinRestorer** | **基岩玩家客户端里的真实自定义皮肤 → Java 端可见**，进服 0.75 秒主动覆盖默认 Steve/Alex，绕开 Floodgate 排队上传 | 基岩玩家进服自动 |

一句话：BSR 抓基岩客户端真实皮当默认底；玩家用 `/skin` 选的皮由 SkinsRestorer 套用。

## 关键配置

```yaml
# plugins/BedrockSkinRestorer/config.yml
fetch-on-join: true                  # 基岩玩家进服时自动抓真实皮肤
fetch-on-join-delay-ticks: 15        # 进服后约 0.75 秒再抓（等 Geyser 就绪）

compatibility-mode:
  skins-restorer: false              # 本服关闭：BSR 照常抓真实皮
  bedrock-auto-auth: false
```

`compatibility-mode.skins-restorer` 控制 BSR 是否在 SkinsRestorer 处理该玩家时跳过：

- 设 `true`：BSR 见 SR 就跳过、不抓真实皮；但 Geyser 进服会接管 profile，把 SR 套的皮冲掉，Java 端只剩默认 Steve，反而看不到皮。
- 设 `false`（本服采用）：BSR 在 0.75 秒抓客户端真实皮打底，Java 端至少稳定显示真实基岩皮。

已知局限：Geyser 进服接管 profile 的时机早于 SkinsRestorer 套皮，玩家用 `/skin` 选的皮**重进服后可能被还原成客户端真实皮**。若某玩家选皮后重进丢了，管理员在控制台手动跑一次 `skin update <带点号的玩家名>` 即可重新套上。

改完需重启服务端生效。玩家换皮后**完全重进一次**，头颅才会用稳定纹理重新渲染。

## 已排除的方案

| 方案 | 为什么不用 |
|---|---|
| GeyserSkinManager | 检测到 Floodgate 即自禁用皮肤功能（设计上把皮肤活儿交给 Floodgate）；且 1.7 调用的 `BedrockClientData.getSkinData()` 在本服 Geyser 2.11.3 已不存在，进服抛 `NoSuchMethodError`。已删除 jar |
| 仅靠 Floodgate 自带上传 | 排队制，国内链路慢/积压，长期不显示 |

## 排障

- 进服后仍显示史蒂夫：看 `logs/latest.log` 中 `BedrockSkinRestorer` 是否有报错；确认基岩客户端里该皮肤确实是当前激活皮肤（不是本地预览未上传）。
- 确认基岩玩家身份：Floodgate 玩家名带 `.` 前缀，UUID 以 `00000000-0000-0000-0009-` 开头。
- 皮肤方向反了（基岩看 Java 全是史蒂夫）是另一个问题，见 3.3 的修复 B。
- 基岩玩家用 `/skin` 后重进被还原成客户端皮：这是 Geyser 进服接管 profile 的已知行为，管理员控制台跑一次 `skin update <带点号玩家名>` 即可；并让玩家完全退出基岩客户端再重进。
- 头颅仍不显示：确认 Geyser `enable-custom-content: true`（头颅映射开关，见 3.3，本服已为 true，不要关）。
