# 14. 核心依赖库 — CMILib

**文件**: 内置于 `plugins/CMILib/` 目录

## 功能说明

CMILib 是 CMI 系列插件的**共享依赖库**，提供了：
- 自定义 Hex 颜色解析
- GUI 创建工具
- 皮肤获取 API
- 物品/NBT 处理工具

> ⚠️ 本服并未安装 CMI 主插件，CMILib 的存在可能是其他插件的依赖项自动安装的。

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
