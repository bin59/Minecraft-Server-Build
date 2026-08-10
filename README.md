# Minecraft-Server-Build

南瓜生存服 — Minecraft Java/基岩互通服务器 完整技术文档

我的世界服务器搭建记录

> **服务器核心**: Leaf 1.21.11 (Paper 分支) | **MOTD**: §e南瓜§f生存服
>
> **Java 端口**: 55551 | **Bedrock 端口**: 19132 | **Web 面板端口**: 25555
>
> **启动内存**: 初始 1GB / 最大 4GB | **运行模式**: 单端末 (无代理/BungeeCord)
>
> **文档生成时间**: 2026-08-09

---

## 目录

| 章节 | 路径                                                                        | 说明                            |
| ---- | --------------------------------------------------------------------------- | ------------------------------- |
| 1    | [01-服务器核心-Leaf/](01-服务器核心-Leaf/Leaf服务端核心.md)                 | Leaf 服务端核心                 |
| 2    | [02-外置登录代理-YggdrasilOfficialProxy/](02-外置登录代理-YggdrasilOfficialProxy/YggdrasilOfficialProxy.md) | YggdrasilOfficialProxy 外置认证 |
| 3    | [03-Java-Bedrock互通层-Geyser-Floodgate/](03-Java-Bedrock互通层-Geyser-Floodgate/) | Geyser + Floodgate 协议互通     |
| 4    | [04-跨版本协议兼容-ViaVersion/](04-跨版本协议兼容-ViaVersion/ViaVersion.md) | ViaVersion 跨版本支持           |
| 5    | [05-权限管理系统-LuckPerms/](05-权限管理系统-LuckPerms/LuckPerms.md)         | LuckPerms 权限管理              |
| 6    | [06-经济系统-Vault/](06-经济系统-Vault/Vault.md)                            | Vault 经济系统                  |
| 7    | [07-方块记录与回滚-CoreProtect/](07-方块记录与回滚-CoreProtect/CoreProtect.md) | CoreProtect 方块审计            |
| 8    | [08-领地系统-Residence/](08-领地系统-Residence/Residence.md)                 | Residence 领地保护              |
| 9    | [09-皮肤管理-SkinsRestorer/](09-皮肤管理-SkinsRestorer/SkinsRestorer.md)      | SkinsRestorer 皮肤管理          |
| 10   | [10-QQ机器人联动-EasyBot/](10-QQ机器人联动-EasyBot/EasyBot.md)               | EasyBot + NapCat QQ 机器人      |
| 11   | [11-Web管理面板-OPanel/](11-Web管理面板-OPanel/OPanel.md)                    | OPanel Web 管理面板             |
| 12   | [12-离线背包查看-OpenInv/](12-离线背包查看-OpenInv/OpenInv.md)               | OpenInv 离线背包编辑            |
| 13   | [13-性能分析-spark/](13-性能分析-spark/spark.md)                             | spark 性能诊断                  |
| 14   | [14-核心依赖库-CMILib/](14-核心依赖库-CMILib/CMILib.md)                      | CMILib 共享依赖库               |
| 15   | [15-统计系统-bStats/](15-统计系统-bStats/bStats.md)                          | bStats 匿名统计                 |
| 16   | [16-服务器配置文件补充/](16-服务器配置文件补充/README.md)     | bukkit.yml / spigot.yml 等      |
| 17   | [17-端口与网络架构总览/](17-端口与网络架构总览/README.md)     | 端口表 + 网络拓扑图             |
| 18   | [18-常用管理命令速查/](18-常用管理命令速查/README.md)         | 各插件管理命令速查              |
| —    | [附录A-插件清单](附录A-插件清单.md)                           | 完整插件版本与文件清单          |
| —    | [附录B-常见问题排查](附录B-常见问题排查.md)                   | 常见问题与解决方案              |

---

> 本文档基于 `c:\mc_serve\1.21.11-test` 服务器实际配置文件生成，涵盖所有已安装工具、插件、模组的详细使用说明和配置解读。
