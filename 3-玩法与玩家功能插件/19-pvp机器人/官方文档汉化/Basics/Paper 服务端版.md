# Paper 服务端版（Paper）

> 原文：[Paper](https://mods.icondice.org/vexbot/wiki/paper)

> 把 VexBot 作为 Paper 插件运行：安装、哪些相同、哪些不同。

自 3.0 起，VexBot 也以 Paper 插件形式发布，支持 Paper 1.21.11、26.1.2 和 26.2。它和 Fabric mod 是同一个 VexBot：相同的机器人、AI、命令、kit、队伍、设置和 GUI。

## 安装

- 下载与你 Minecraft 版本对应的 Paper jar，例如 `vexbot-paper-3.0.4+paper-mc26.2.jar`。
- 把它放进服务端的 `plugins/` 文件夹并重启。不需要 Fabric API 或其他插件。
- 1.21.11 用 Java 21，26.x 用 Java 25 —— Paper 要求同样的版本。

## 差异

- 附加组件（VexSim、VexTrial、VexShop、VexPull）是 Fabric mod，因此它们需要 Fabric 构建。
- `/vexbot recordpov` 是客户端侧功能，只在 1.21.11 的 Fabric 单人游戏中可用。
- 其他一切 —— 命令、设置、各个宝箱和队伍设置界面 —— 行为完全相同。

> 在服务端上二选一：要么把 Fabric jar 放进 `mods/`，要么把 Paper jar 放进 `plugins/`，绝不要两者同时装。
