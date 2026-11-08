# Orbit 变更记录

## 1.0.1（2026-11-07）

### 修复

- **观察者画面不环绕**：移除「隐形 ArmorStand + `setSpectatorTarget`」相机方案。
  该方案依赖客户端 Camera 包且要求相机实体已被客户端追踪，spawn 后立即绑定常被
  静默丢弃，导致「盔甲架在转、观察者停在原地」。现改为观察者在 `SPECTATOR` 模式下
  **本体**按圆周坐标直接传送并朝向目标。
- **盔甲架没有隐形**：同上，插件不再生成任何实体，彻底消除可见盔甲架、
  `setInvisible` 时机问题和存档残留风险。
- **没有循环每一个玩家**（多处叠加）：
  - `auto-tour.enabled` 默认由 `false` 改为 `true`；
  - `interval-seconds` 默认由 `3600` 改为 `60`，避免一轮结束要等一小时；
  - 目标离线 / 会话中断时立即跳到下一个，不再空等整段 `per-player-seconds`；
  - 每轮按最新在线名单重新排队，新上线玩家会进入下一轮；
  - 一轮内锁定巡视员，不再每秒重新解析导致会话串台；
  - `/orbit tour start` 现在以命令发起者为巡视员（控制台发起回退到配置/权限解析）。

### 新增

- `/orbit tour status`：查看运行状态、巡视员、当前目标、队列进度与剩余秒数。
- 切换目标时给巡视员发送 `messages.tour-switch`，支持 `%player%` / `%index%` / `%total%`。
- `update-interval-ticks`（默认 2）：控制本体位置更新频率，降低 `PlayerTeleportEvent`
  触发量；角速度按间隔换算，改变该值不影响实际转速。
- `reload` 时若把 `enabled` 改为 false，会正确停止巡视并恢复巡视员。

### 其他

- `plugin.yml` 版本 `1.0.0` → `1.0.1`，`usage` 补 `status`。
- README 更新为 26.3 / JDK25 口径，补充实际编译命令与踩坑记录。

---

## 1.0.0

- 初版：`/orbit <玩家>` 环绕观察、`/orbit tour` 定时巡视、半径/高度/速度可配。
- 26.3 适配：`api-version` 改 `26.3`，编译依赖改 `leaf-api 26.3.build.13-alpha`。
