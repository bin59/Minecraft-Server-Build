# CHANGELOG — OfflineReward（离线奖励）

## 2026-10-29

### 明确：管理员 / 管理组离线同样发放离线奖励

- **结论：代码行为本来就满足，本次只修正文档误导，未改动发放逻辑。**
- 排查结论：
  - 发放链路上唯一的权限门槛是 `PlayerListener.onJoin` → `plugin.isExempt(p)`，
    而 `isExempt` 只检查 `config.yml` 里 `exempt-permission` 指向的权限节点（默认 `offlinereward.exempt`）。
  - `plugin.yml` 中 `offlinereward.exempt` 是 `default: false`，`offlinereward.admin` 是 `default: op`。
    **op 不会自动获得 exempt**，因此管理员 / 管理组成员默认照常领取，与普通玩家同一套规则。
  - `RewardCalculator`（时长 → 金额）与 `Delivery`（写邮箱 / 回退 eco give）都没有任何身份相关的分支。
- 文档修正（原文把「管理员」和豁免绑在一起，容易让人误以为管理员拿不到）：
  - `src/main/resources/config.yml`：`exempt-permission` 注释改为「仅用于豁免测试号/特殊情况账号」，
    并写明管理员默认照常领取、要单独排除才给 exempt。
  - `使用说明.md` 三处：
    - 四章「命令与权限」加了「管理员能不能领？能」的说明块
    - 六章防滥用第 5 条：由「管理/测试号」改为「测试号/特殊账号」，并标注管理员不在豁免范围
    - 七章 LuckPerms 衔接：说明 `admin` 是管理权限、与发不发奖无关，附 `permission check` / `unset` 排查命令
- 重新打包：只改了资源文件（源码未变），故未走 `build.ps1` 全量编译，
  改用 `jar uf OfflineReward.jar -C src/main/resources config.yml` 只替换 jar 内 config.yml，
  旧 jar 备份为 `OfflineReward.jar.bak`。已验证 jar 内主类 / 监听器 / sqlite-jdbc / plugin.yml 完好。

### 踩坑

- `build.ps1` 第 21 行的清理写法 `Get-ChildItem $d -Recurse -Force | Remove-Item -Recurse -Force`
  在 Windows PowerShell 5.1 下报 `ParameterBindingException`（管道输入无法绑定）。
  - 需要全量重建时，先改这段（改用 `Remove-Item "$d\*" -Recurse -Force`）；
  - 只改资源文件时，直接用 `jar uf` 增量替换更省事。
