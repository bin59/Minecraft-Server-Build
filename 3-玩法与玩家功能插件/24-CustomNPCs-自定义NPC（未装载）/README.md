# CustomNPCs — 自定义 NPC

在服务器中创建可交互的 NPC，用于商店、传送点、任务发布、信息展示等。

**官网/下载**：https://modrinth.com/plugin/CustomNPCs
**版本**：1.8.2
**支持版本**：MC 1.20+（本服 1.21.11）
**作者**：Foxikle

## 核心功能

- **创建 NPC**：在世界中放置自定义 NPC，可设置皮肤、名称、对话；
- **交互动作**：NPC 可设置点击动作（运行命令、发送消息、打开菜单、传送等）；
- **对话气泡**：NPC 头顶显示名称和对话内容；
- **全息文字**：NPC 上方显示多行全息文字；
- **NPC 克隆**：复制现有 NPC 快速创建；
- **跨世界传送**：通过命令快速传送到指定 NPC。

## 常用命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/npc` | 打开 NPC 编辑菜单 | customnpcs.edit |
| `/npc create` | 创建新 NPC | customnpcs.create |
| `/npc delete` | 删除准星指向的 NPC | customnpcs.delete |
| `/npc edit` | 编辑准星指向的 NPC | customnpcs.edit |
| `/npc manage` | 管理 NPC（动作、对话、皮肤等） | customnpcs.commands.manage |
| `/npc clone` | 克隆 NPC | customnpcs.commands.clone |
| `/npc movehere` | 将 NPC 传送到自己位置 | customnpcs.commands.movehere |
| `/npc goto <NPC名>` | 传送到指定 NPC | customnpcs.commands.goto |
| `/npc list` | 列出所有 NPC | customnpcs.commands.list |
| `/npc reload` | 重载配置 | customnpcs.commands.reload |
| `/npc clear_holograms` | 清除全息文字 | customnpcs.commands.removeHolograms |
| `/npc wiki` | 打开官方文档 | customnpcs.commands.wiki |

## NPC 可设置的动作

NPC 被玩家右键点击时可执行：
- **运行玩家命令**：以玩家身份执行命令；
- **运行控制台命令**：以控制台身份执行命令（需 `customnpcs.run_command.enable_console` 权限）；
- **发送消息**：向玩家发送聊天消息；
- **打开菜单**：打开 DeluxeMenus 等 GUI 菜单；
- **传送**：将玩家传送到指定位置。

## 配置文件

- `plugins/CustomNPCs/config.yml`：插件主配置；
- `plugins/CustomNPCs/npcs.yml`：所有 NPC 的数据存储。

## 玩家权限

| 功能 | 权限 | 说明 |
|---|---|---|
| 右键点击 NPC 交互 | 无需权限 | 所有玩家默认可与 NPC 交互（触发动作、对话） |
| 创建/编辑/删除 NPC | customnpcs.create / edit / delete | 仅 OP 可用，不对普通玩家开放 |
| 管理 NPC 动作/皮肤 | customnpcs.commands.manage | 仅 OP 可用 |
| 传送到 NPC（/npc goto） | customnpcs.commands.goto | 仅 OP 可用 |
| 查看 NPC 列表（/npc list） | customnpcs.commands.list | 仅 OP 可用 |

**本服状态**：所有 NPC 管理命令仅管理员可用；普通玩家只能右键与 NPC 交互（如商店 NPC、传送 NPC、任务 NPC）。

## 注意事项

- **控制台命令权限**：`customnpcs.run_command.enable_console` 权限要谨慎授予，允许 NPC 以控制台身份执行命令；
- **Multiverse/MultiWorld**：支持多世界插件，NPC 可跨世界存在；
- **PlaceholderAPI**：支持 PAPI 占位符，NPC 对话和全息文字中可使用。
