# RCON 密码加固 + motd 恢复 · 运维记录

> 日期：2026-10-01 ｜ 服务器：南瓜生存服（Leaf 1.21.11）
> 关联：`server.properties` 安全加固、文件编码踩坑与恢复

## 背景

为配合"网页上传皮肤 → 应用游戏"（云服务器网页端经 RCON 发 `/skin` 命令，连接方案由云服务器自行实现），发现 `server.properties` 中 RCON 密码为弱口令 `test_55551`，存在安全隐患，需加固。

## 处理内容

### 1. RCON 密码加固

- **原值**：`rcon.password=test_55551`（弱口令）
- **新值**：强随机密码（20 位大小写字母+数字）
- **位置**：`server.properties`
- **生效**：下次服务器启动时读取生效（改配置不重启不生效）
- **保管**：密码仅存于 `server.properties`（运行时读取，连接方案由云服务器自行实现），**不写入任何文档/备忘录**，请管理员自行记牢。

> 关联配置：`enable-rcon=true`、`rcon.port=25595` 保持不变。

### 2. motd 恢复（编码踩坑教训）

在修改 `server.properties` 过程中，曾使用 `Set-Content` 整文件重写，导致含中文颜色代码的 **motd 被双重转码损坏**（UTF-8 被按 ANSI 误读再写回，出现 `搂x搂f搂f搂5搂e...` 乱码，且不可逆）。

**恢复方法**：
1. 在备份 `F:\game\pc\MC\开服\服务器数据备份\leaf-26.2\server.properties` 中找到原始 motd（含 `§` 颜色代码的完整中文）。
2. 用 **Python 按行精确替换**（仅改 motd 行），UTF-8 无 BOM 写回，其余配置保持不动。
3. 验证文件 UTF-8 合法、关键配置（rcon/view-distance/server-port 等）完整。

## 经验教训（重要）

1. **修改含中文的 `server.properties` 切忌用 `Set-Content`/`Get-Content` 整文件重写**——PowerShell 默认按 ANSI 编解码，会破坏 UTF-8 中文。应改用 Python 按行/正则精确替换，UTF-8 无 BOM 写回。
2. 若改坏 motd 等中文行，可从其它服务器版本备份（`leaf-26.2`）恢复原始值。
3. 文档中不记录明文密码，避免泄露；密码只在运行时配置中。

## 遗留 / 后续

- 服务器当前未运行，新 RCON 密码**下次启动生效**；云服务器连接方案已自行实现，启动后可用其验证 RCON 链路。
- RCON 密码已加固完成，待办已勾除；密码请妥善保管。
