# AdvancedSensitiveWords — 敏感词过滤与替代（Paper）

> 一站式多平台敏感词治理插件：基于 **DFA 词典匹配** + **事件级内容检查** + **模块化违规等级（VL）**，并可选接入 **LLM(AI) 聊天审查**。命中词可「替代」或「取消」，逐模块自动惩罚。

## 基本信息

| 项目     | 内容                                                                             |
| -------- | -------------------------------------------------------------------------------- |
| 插件名   | AdvancedSensitiveWords（简称 ASW）                                               |
| 命令前缀 | `/asw`（全名 `/advancedsensitivewords`）                                         |
| 数据目录 | `plugins/AdvancedSensitiveWords/`                                                |
| 配置文件 | `config.yml`、`messages_en.yml` / `messages_zhcn.yml`、`llm-history/`（AI 审计） |
| 兼容核心 | **仅 Paper 1.21.11+**（Leaf/Purpur/Spigot 不完整支持），Java **21**              |
| 可选依赖 | Velocity（跨服通知）、PlaceholderAPI、PacketEvents（告示牌假象）、TrChat、Floodgate/AuthMe |

> ⚠️ **2.x 是破坏性重构**：仅面向 Paper，配置改为 kebab-case 新模型。**1.x 的旧配置不能直接复用**，升级必须逐项核对所有设置。

## 它提供什么

- **聊天与命令**：`AsyncChatEvent` + 命令预处理，支持跨消息上下文检测、`REPLACE`（替换）/`CANCEL`（取消），取消时可发假消息；兼容 TrChat 的假消息/禁言展示。
- **书本**：可写书本事件检测，取消模式下支持跨页检查，带处理缓存。
- **告示牌**：逐行/多行/近期告示牌上下文检查；取消模式下可用 PacketEvents 给作者显示"假内容"，其他玩家看到真实干净内容。
- **铁砧与物品**：改名结果过滤 + 物品 displayname / lore 过滤（Adventure 组件）。
- **玩家名与广播**：可拒绝含敏感词的登录名，可选过滤全服广播。
- **可选 AI 审查**：对 DFA 与聊天上下文都未命中的消息做 LLM 复核，异步、成本限额、默认关闭，绝不撤回聊天，可通知/记录/计独立 AI VL/执行动作。

## 安装部署

1. 从发布页下载 **Paper 版 jar**（或本地 `gradlew shadowJar` 构建），放入 `plugins/`。
2. 启动一次 Paper，生成 `config.yml` 与消息文件。
3. 编辑 `config.yml` 与 `messages_zhcn.yml`。
4. 改完词库执行 `/asw reload all`；只改配置用 `/asw reload config`。

> Velocity 通知 / 跨服命令：在代理端装 Velocity 版 jar，Paper 端 `plugin.hook-velocity: true`，两端都重启。代理模块本身**不**过滤聊天。

## 快速配置（`config.yml`）

```yaml
plugin:
  language: zhcn        # 语言：en / zhcn（对应 messages_en.yml / messages_zhcn.yml）
  enable-chat-check: true      # 聊天检查
  enable-sign-edit-check: true # 告示牌检查

chat:
  method: CANCEL       # REPLACE（替换）或 CANCEL（取消）
  fake-message-on-cancel: false  # 取消时向玩家发假消息（仅 CANCEL 模式）
  context-check: true           # 跨消息上下文检查

  # 命令参数检查规则（配合下面的白名单）
  invert-command-white-list: true
  command-white-list:
    - "[default:include] /msg [ignore:1]"          # 检查 /msg，跳过第 1 个参数（玩家名）
    - "[default:include] /bc [ignore:1,-1]"        # 检查 /bc，跳过首末参数
    - "[default:ignore] /mail send [include:2..]"  # 默认忽略，仅检查第 2 参数起
```

- `REPLACE` 把命中的词换成配置的替代字符；`CANCEL` 拒绝该交互。
- 参数从命令路径后从 `1` 开始编号，`-1` 表示最后一个参数，`2..` 表示第 2 参数到结尾。`ignore` 会把检测段切开，避免词跨被跳过的玩家名/服务器名/数字匹配。

## 玩家/管理员命令

| 命令 | 用途 |
| --- | --- |
| `/asw help [query]` | 查看命令帮助 |
| `/asw status` | 查看插件总体状态 |
| `/asw ai status` | 查看 LLM 运行时计数、队列、模型、API 模式、分类策略 |
| `/asw reload all` | 重载配置 + 词典 |
| `/asw reload config` | 仅重载配置 |
| `/asw test <text...>` | 用 DFA 过滤测试一段文本 |
| `/asw word add/remove <word> [word...]` | 临时增删屏蔽词（仅当前运行时） |
| `/asw allow add/remove <word> [word...]` | 临时增删白名单词（仅当前运行时） |
| `/asw player info <在线玩家>` | 查看该玩家各模块 VL |
| `/asw player reset <在线玩家> [module]` | 重置全部或某个模块 VL |
| `/asw player punish <在线玩家> [method...]` | 执行手动惩罚或指定动作 |
| `/asw teleport <world-id> <x> <y> <z>` | 传送到举报坐标 |

> 运行时增删的词在整词库重载或重启后会丢失。

## 权限（LuckPerms）

| 权限 | 默认 | 用途 |
| --- | --- | --- |
| `advancedsensitivewords.bypass` | false | 绕过全部过滤 |
| `advancedsensitivewords.notice` | op | 接收管理通知 |
| `advancedsensitivewords.update` | op | 接收更新提示 |
| `advancedsensitivewords.command.*` | false | 全部管理命令父节点 |
| `advancedsensitivewords.command.help` | op | 帮助 |
| `advancedsensitivewords.command.status` | op | 总体状态 |
| `advancedsensitivewords.command.ai.status` | op | AI 状态 |
| `advancedsensitivewords.command.reload.all` | op | 重载配置+词典 |
| `advancedsensitivewords.command.reload.config` | op | 仅重载配置 |
| `advancedsensitivewords.command.test` | op | DFA 测试 |
| `advancedsensitivewords.command.word.add/remove` | op | 增删屏蔽词 |
| `advancedsensitivewords.command.allow.add/remove` | op | 增删白名单词 |
| `advancedsensitivewords.command.player.info/reset/punish` | op | 查看/重置/手动惩罚 VL |

```text
lp group admin permission set advancedsensitivewords.command.* true
lp group admin permission set advancedsensitivewords.bypass true
# 普通玩家无需授权（默认 op）；如需给管理组通知：lp group admin permission set advancedsensitivewords.notice true
```

## 惩罚与违规等级（VL）

每个过滤模块有独立的 `punishment` 列表和独立 VL：`CHAT`、`AI`、`BOOK`、`SIGN`、`ANVIL`、`ITEM`（命令共享 CHAT VL）。`plugin.manual-punishment` 例外，其 VL 条件用各模块总和。

```yaml
chat:
  punishment:
    - "COMMAND|kick %player% 发送违规内容|VL>2"   # 动作|参数|触发条件
    - "SHADOW|60|VL>5"                              # 禁言(影封) 60 秒，VL>5
```

支持动作：`COMMAND`（本服命令）、`COMMAND_PROXY`（代理命令）、`DAMAGE`、`HOSTILE`、`EFFECT`、`SHADOW`（影封/禁言）。命令动作里可用 `%player%`/`%PLAYER%`。列表留空则保留检测/日志/通知/VL 计数，但**禁用自动动作**。

## 可选 AI（LLM）审查

仅在配置好兼容服务商与 API Key 后启用。Paper 通过 `plugin.yml` 加载 LangChain4j 依赖，首次启动需联网或已有库缓存。

```yaml
ai:
  enabled: true
  base-url: https://api.deepseek.com
  api-mode: CHAT_COMPLETIONS   # CHAT_COMPLETIONS / RESPONSES / ANTHROPIC_MESSAGES
  api-key-environment: DEEPSEEK_API_KEY   # 从环境变量读 Key，勿写死在配置
  model-name: deepseek-v4-flash
```

- 请求前必须先通过 DFA + 上下文检查，再叠加消息长度、熵、玩家冷却、并发、队列等限制；LLM 输出在本地严格解析后才执行后续动作。
- 各分类的通知/惩罚置信阈值与动作在 `ai.category-policy` 下配置。
- 请求与响应审计记录在 `plugins/AdvancedSensitiveWords/llm-history/`，**当作敏感运维数据**。
- `ai.server-context-can-override` 开启后，`ai.server-context` 会插入受信系统策略。**不要把玩家输入、凭据或隐私数据放进策略文本**。

## 与南瓜服整合

- **中文消息**：`plugin.language: zhcn` → 使用 `messages_zhcn.yml`（原生中文，无需手译）。
- **PlaceholderAPI**：`plugin.enable-placeholder: true` 后可用 `%asw_version%`、`%asw_total_filtered%`、`%asw_is_shadow%`、`%asw_violation_count%`。
- **基岩互通**：Floodgate 处理基岩昵称、AuthMe 处理登录态，避免误伤正常玩家。
- **经济/惩罚衔接**：惩罚动作可接 `COMMAND` 走南瓜账本 / 禁言，替代硬踢人。
- **跨服**：若上 Velocity，装代理模块 + `hook-velocity: true`，实现跨服通知与代理端命令。

## 注意事项

1. **2.x 破坏性升级**：1.x 配置不可复用；升级后逐项核对 `config.yml` 全部设置。
2. **运行时词表易失**：`/asw word add/remove` 只对当前运行生效，整词典重载或重启即还原；长期词表要写进词典文件。
3. **书本跨页 / 告示牌假象依赖 CANCEL 模式**：想用"假消息/假告示牌"必须用 `CANCEL`，`REPLACE` 不支持。
4. **告示牌假象需 PacketEvents**：未装则只保留取消行为，无假视图。
5. **先本地测再上线**：用 `/asw test <文本>` 在测试服跑词库，避免误判正常聊天。
6. **本地暂无插件 jar 与数据目录**（当前仅空 README），尚未部署。落地前先从发布页下载 **Paper 版** jar 并按上面步骤初始化。

已汉化（原生 `messages_zhcn.yml`，可配合本插件自带中文消息文件）
