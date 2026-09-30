# Leashable Players（拴绳牵玩家）

## 是什么

作者 nightly 的项目：手持拴绳右键玩家即可拴住牵着走。**本服可直接装插件版**，无需 Fabric。

增进友谊小插件，可以用栓绳拴好友，防止好友乱跑

- **1.3.0-bukkit**（2026-07-31）：Bukkit / Paper / Spigot / Purpur **插件版**，官方支持 MC 1.20.1 – **1.21.11**、26.1、26.2
- 另有 Fabric / Forge / NeoForge / Quilt 模组版（本服不需要）
- 配置走 **gamerule**：启用开关、绳索断裂距离、开始拉动距离（插件注册自定义 gamerule，进服 `/gamerule` 查看实际名称）

## 本服部署

1. 下载插件版：https://cdn.modrinth.com/data/BKyMf6XK/versions/ir8urScS/leashmod-bukkit-1.3.0.jar
2. 放入 `plugins/` 重启即可（Leaf 为 Paper fork，1.21.11 官方支持）
3. 使用：手持拴绳右键玩家 = 拴住牵着走；潜行 = 摆脱

Modrinth 项目页：https://modrinth.com/plugin/leashable-players

## 注意

- 基岩玩家（Geyser）右键为长按交互，拴绳牵人是否正常**需实测**
- 1.3.0-bukkit 为较新发布，建议进服后先单机验证 gamerule 生效

## 备选

| 插件                                     | 说明                                                                       |
| ---------------------------------------- | -------------------------------------------------------------------------- |
| **LeashPlayers** 1.9（SpigotMC #100458） | 同为插件版，[1.16 - 1.21]，api 大概率兼容 1.21.11；无配置项，更新停在 2024 |
