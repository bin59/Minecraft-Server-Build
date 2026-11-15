# ClickMobs（点击收纳生物）

## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | `plugins/ClickMobs-paper-1.3.1.jar`，`version: 1.3.1` |
| 官方最新 | 插件版 `paper-1.3.1`（2026-03-26，官方 Modrinth 正式版，支持 26.3 / 26.2 / 26.1.x / 1.21.x / 1.20.x，平台 Paper / Purpur / Spigot / Bukkit；另有 Fabric / NeoForge 模组版）；本服已是该版 |
| 官方文档站 | <https://modrinth.com/plugin/clickmobs> |
| 官方下载 | <https://modrinth.com/plugin/clickmobs>；paper-1.3.1 版本页 <https://modrinth.com/plugin/clickmobs/version/tte1UNeO> |
| 主要页面 | `modrinth.com/plugin/clickmobs`（Overview：功能 / 黑白名单标签 / 配置）、`modrinth.com/plugin/clickmobs/version/tte1UNeO`（paper-1.3.1 版本页）、<https://clickism.de/docs/clickmobs/tags>（官方标签文档） |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方文档复核。

## 是什么

shift+右键把生物**收进背包**（变成对应生物头颅），放回头颅即可恢复生物；可直接放进船/矿车，发射器可自动生成生物。轻量实用插件。

- 版本：1.3.1（官方 Modrinth `paper-1.3.1` 正式版，2026-03-26；本服 `ClickMobs-paper-1.3.1.jar`）
- 平台：Paper / Purpur / Spigot / Bukkit（Leaf 可用；另有 Fabric / NeoForge 模组版，别下混）；官方项目页列 26.3 / 26.2 / 26.1.x / 1.21.x / 1.20.x；本服 26.3 已实跑加载

## 功能

| 功能 | 说明 |
|---|---|
| 收纳生物 | shift + 右键生物 → 收入背包（变成头颅） |
| 放置恢复 | 手持对应生物头颅放置 → 生物恢复 |
| 船/矿车 | 可直接放入船、矿车等载具 |
| 发射器 | 发射器可自动生成生物（Paper 1.21+） |
| 敌对生物 | 默认不可捡（可配置允许） |
| 黑名单 | `blacklisted-mobs` 配置禁止收纳的生物 |

## 已知问题（重要）

- **与 PatPat 摸头插件的共存方案（实测通过后启用）**：两者都用 **Shift + 右键**，但 **PatPat 摸头要求空手**、**ClickMobs 收纳不要求空手**——按持物状态分流：
  - **手持物品** + shift+右键生物 → 收纳（PatPat 不触发，无冲突）
  - **空手** + shift+右键生物 → 摸头优先（ClickMobs 空手也响应，**需实测**确认是否错乱；若冲突，约定"收纳必须手持物品"即可规避）
- **黑名单不生效 bug**：`WANDERING_TRADER`（流浪商人）/`TRADER_LLAMA` 加进黑名单后**仍可被捡走**——生存服有村民交易体系，装前务必实测并考虑用权限/区域限制兜底
- 基岩玩家（Geyser）shift + 长按右键交互**需实测**

## 下载

官方 Modrinth：https://modrinth.com/plugin/clickmobs（插件版 `ClickMobs-paper-1.3.1.jar` 即本服所用；SpigotMC 旧页 https://www.spigotmc.org/resources/clickmobs.121939/ 仅作参考）

## 同名项目防混淆

Modrinth 上的 **Click Mobs（作者 Clickism）** 同时发布**插件版（Paper / Purpur）**与**模组版（Fabric / NeoForge）**；本服 `ClickMobs-paper-1.3.1.jar` 就是插件版，Leaf 可装。模组版才放 `mods/`，别把两种构建下混。
