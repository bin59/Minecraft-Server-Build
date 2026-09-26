# 14. 核心依赖库 — CMILib

CMILib 是 CMI 系列插件的共享依赖库，提供 Hex 颜色解析、GUI 工具、皮肤 API、物品/NBT 处理等底层能力。本服未装 CMI 主插件，CMILib 由其他插件自动引入。

**文件**: 内置于 `plugins/CMILib/` 目录

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
