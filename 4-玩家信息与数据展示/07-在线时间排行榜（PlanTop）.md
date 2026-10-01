# 在线时间排行榜（PlanTop 自研插件 + DecentHolograms）

主城全息显示全服玩家**总在线时间 Top 30**（3 页 × 10 名），右键/左键点击全息翻页。数据源 = **Plan 数据库**——历史在线时间**全部保留**，不会重新计时。

## 为什么用 PlanTop 而不是 PlayTime

PlayTime 插件从**安装那一刻**才开始计时，此前玩家的在线时间全部没有（重新计时）。本服 Plan 一直运行，`plan_sessions` 表已记录全部历史（8-31 至今，157 名玩家、6 千多条会话），PlanTop 直接读它做排行。

## 插件与占位符

**PlanTop 1.0.0**（自研，源码与技术文档在 `自研\在线时间排行-PlanTop\`）：
- 每 5 分钟从 Plan 库聚合一次 playtime，内存缓存 Top 30；全息读缓存，零 DB 压力
- 注册 PAPI 占位符（**装完插件必须重启服务器**才注册）：

| 占位符 | 说明 |
| --- | --- |
| `%ptop_<N>_name%` | 第 N 名玩家名 |
| `%ptop_<N>_displayname%` | 第 N 名显示名（当前同 name） |
| `%ptop_<N>_time%` | 第 N 名总时长（**只显示分钟**，如 `11963分`） |
| `%ptop_updated%` | 最后成功刷新时间（排障用） |
| `%ptop_error%` | 最近一次错误（`ok` = 正常，排障用） |

验证注册：`/papi parse me %ptop_1_name%` 返回玩家名；`/papi parse me %ptop_updated%` 返回时间即正常。

## 创建全息（命令方式，DecentHolograms 2.10.1）

> ⚠️ **不要手写 yml**：2.10.1 文件缺字段（`location`/`actions`）会在 `/dh reload` 报 NPE、全息图全部消失；用命令创建最稳。`location` 必须是 **4 段** `<world>:<x>:<y>:<z>`（无 yaw/pitch，朝向由 `facing` 控制），逗号格式或 6 段都会解析成 null。命令内容**不要加单引号**（DH 不是 shell，`'` 会原样显示）。
>
> ⚠️ 多页（pages）只支持 `/dh h` 创建的 Armor Stand 型全息；`/dh displays` 的 Display Entity 型不支持翻页。

### 一、创建 + 第 1 页（#1-#10）

```
/dh h create playtime -l:world:41:65:32
/dh lines add playtime 1 &6&l🏆 &f&l在线时间排行榜 &7[1/3]
/dh lines add playtime 1 &7&m                            
/dh lines add playtime 1 &e#1  &f%ptop_1_displayname%  &7%ptop_1_time%
/dh lines add playtime 1 &f#2  &f%ptop_2_displayname%  &7%ptop_2_time%
/dh lines add playtime 1 &f#3  &f%ptop_3_displayname%  &7%ptop_3_time%
/dh lines add playtime 1 &f#4  &f%ptop_4_displayname%  &7%ptop_4_time%
/dh lines add playtime 1 &f#5  &f%ptop_5_displayname%  &7%ptop_5_time%
/dh lines add playtime 1 &f#6  &f%ptop_6_displayname%  &7%ptop_6_time%
/dh lines add playtime 1 &f#7  &f%ptop_7_displayname%  &7%ptop_7_time%
/dh lines add playtime 1 &f#8  &f%ptop_8_displayname%  &7%ptop_8_time%
/dh lines add playtime 1 &f#9  &f%ptop_9_displayname%  &7%ptop_9_time%
/dh lines add playtime 1 &f#10 &f%ptop_10_displayname%  &7%ptop_10_time%
/dh lines add playtime 1 &7&m                            
```

### 二、第 2 页（#11-#20）

```
/dh p add playtime
/dh p switch playtime 2
/dh lines add playtime 2 &6&l🏆 &f&l在线时间排行榜 &7[2/3]
/dh lines add playtime 2 &7&m                            
/dh lines add playtime 2 &f#11 &f%ptop_11_displayname%  &7%ptop_11_time%
/dh lines add playtime 2 &f#12 &f%ptop_12_displayname%  &7%ptop_12_time%
/dh lines add playtime 2 &f#13 &f%ptop_13_displayname%  &7%ptop_13_time%
/dh lines add playtime 2 &f#14 &f%ptop_14_displayname%  &7%ptop_14_time%
/dh lines add playtime 2 &f#15 &f%ptop_15_displayname%  &7%ptop_15_time%
/dh lines add playtime 2 &f#16 &f%ptop_16_displayname%  &7%ptop_16_time%
/dh lines add playtime 2 &f#17 &f%ptop_17_displayname%  &7%ptop_17_time%
/dh lines add playtime 2 &f#18 &f%ptop_18_displayname%  &7%ptop_18_time%
/dh lines add playtime 2 &f#19 &f%ptop_19_displayname%  &7%ptop_19_time%
/dh lines add playtime 2 &f#20 &f%ptop_20_displayname%  &7%ptop_20_time%
/dh lines add playtime 2 &7&m                            
```

### 三、第 3 页（#21-#30）

```
/dh p add playtime
/dh p switch playtime 3
/dh lines add playtime 3 &6&l🏆 &f&l在线时间排行榜 &7[3/3]
/dh lines add playtime 3 &7&m                            
/dh lines add playtime 3 &f#21 &f%ptop_21_displayname%  &7%ptop_21_time%
/dh lines add playtime 3 &f#22 &f%ptop_22_displayname%  &7%ptop_22_time%
/dh lines add playtime 3 &f#23 &f%ptop_23_displayname%  &7%ptop_23_time%
/dh lines add playtime 3 &f#24 &f%ptop_24_displayname%  &7%ptop_24_time%
/dh lines add playtime 3 &f#25 &f%ptop_25_displayname%  &7%ptop_25_time%
/dh lines add playtime 3 &f#26 &f%ptop_26_displayname%  &7%ptop_26_time%
/dh lines add playtime 3 &f#27 &f%ptop_27_displayname%  &7%ptop_27_time%
/dh lines add playtime 3 &f#28 &f%ptop_28_displayname%  &7%ptop_28_time%
/dh lines add playtime 3 &f#29 &f%ptop_29_displayname%  &7%ptop_29_time%
/dh lines add playtime 3 &f#30 &f%ptop_30_displayname%  &7%ptop_30_time%
/dh lines add playtime 3 &7&m                            
```

### 四、翻页动作（`/dh p` = `/dh pages`）

```
/dh p addaction playtime 1 RIGHT NEXT_PAGE
/dh p addaction playtime 1 LEFT PREV_PAGE
/dh p addaction playtime 2 RIGHT NEXT_PAGE
/dh p addaction playtime 2 LEFT PREV_PAGE
/dh p addaction playtime 3 RIGHT NEXT_PAGE
/dh p addaction playtime 3 LEFT PREV_PAGE
```

点击类型 `RIGHT`=右键、`LEFT`=左键；首尾页多余动作自动无效。

## 部署步骤

1. 复制 `自研\在线时间排行-PlanTop\PlanTop-1.0.0.jar` 到运行服 `plugins/`，**重启服务器**（必须，否则占位符不注册）
2. 编辑 `plugins/PlanTop/config.yml`：`database.password` 填 Plan 的真实密码（与 `plugins/Plan/config.yml` 的 Database 段一致）；单服 `server-id` 默认 1
3. **删掉/移走 `holograms/` 下所有手写 yml**（旧格式 `location: world, x, y, z, 0, 0` 会让 reload 直接 NPE、全息全消失），只留命令创建的 `playtime.yml`
4. 按上方一~四节命令依次执行（创建 → 3 页内容 → 翻页动作）
5. 验证：`/papi parse me %ptop_1_name%` 有值 + `%ptop_updated%` 是时间 → 主城看全息 → 点击测试翻页

## 性能注意

- PlanTop 每 5 分钟**异步**聚合一次（当前 6 千多条会话 <50ms），失败保留上次缓存不崩；全息读内存缓存，几乎零开销
- 全息 `playtime.yml` 顶部 `update-interval`（tick）建议调成 **100–200（5–10 秒）**，排行榜无需实时
- 时长口径 = 会话总时长（含 AFK）；Plan 对在线会话每几分钟续写，数据最多延迟几分钟
- 多服共库时，`plan_servers` 里多个 id，`config.yml` 的 `server-id` 必须填当前服那个
