# 14. 核心依赖库 — CMILib

CMILib 是 CMI 系列插件的共享依赖库，提供 Hex 颜色解析、GUI 工具、皮肤 API、物品/NBT 处理等底层能力。本服未装 CMI 主插件，CMILib 由其他插件自动引入。

## 0. 版本与官方文档（2026-11-14 核对）

| 项 | 实测 / 结论 |
| --- | --- |
| 本地 26.3 主服 | CMILib 1.6.1.2（本服 26.3 主服实装） |
| 官方最新 | 官网持续更新，当前按 SpigotMC 资源页口径 |
| 官方文档站 | https://spigotmc.org/resources/87610 （CMILib 官方资源页；无独立文档站；cmilib.876 本次未能验证） |
| 官方下载 | https://spigotmc.org/resources/87610 （资源页即下载页） |
| 主要页面 | 官方站未能验证（仅 SpigotMC 资源页，配置说明随 jar 内 config.yml 注释） |

> 以下正文以本服实跑配置为准；遇到与官方最新文档不一致的写法，以官方文档复核。

## 功能

- 自定义 Hex 颜色解析
- GUI 创建工具
- 皮肤获取 API
- 物品/NBT 处理工具

## 关键配置 (`plugins/CMILib/config.yml`)

```yaml
Language: EN
AutoUpdate: false                   # 不自动更新
ExploitPatcher:
  Placeholders:
    blocked:
      checkItem: true               # 阻止 PAPI checkitem 漏洞
Skins:
  SkinUpdateTimer: 1320             # 皮肤更新计时器: 1320 分钟
  SkinRequestFrequency: 10          # 皮肤请求频率: 10 分钟
Colors:
  OfficialHex: true                 # 支持官方 Hex 格式 (#f6f6f6)
  QuirkyHex: true                   # 支持非标准 Hex 格式 (&#f6f6f6)
```
