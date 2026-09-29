# 在线时间排行榜（PlayTime + DecentHolograms）

主城全息显示全服玩家**总在线时间 Top 30**（3 页 × 10 名），右键/左键点击全息翻页。

## 插件

**PlayTime 3.4.1**（作者 PCPSells，免费）：

- 下载：https://www.spigotmc.org/resources/playtime-»-1-17-1-21-11.44852/ （SpigotMC 需登录下载）
- 版本 3.4.1 支持 **1.21.11**（2026-09-26 更新，与 Leaf 1.21.11 匹配）
- 功能：总在线时长统计、排行榜、AFK 检测、按日/月排行、MySQL 多服同步（可选）
- 装进 `plugins/` 重启后自动生成 `config.yml`；玩家数据默认存 YAML

## 权限（LuckPerms）

```
lp group default permission set playtime.use true
lp group default permission set playtime.top true
```

- `playtime.use`：玩家可用 `/playtime` 看自己时长
- `playtime.top`：玩家可开 `/playtime top` 排行 GUI（可选，全息已够用）
- 隐藏某玩家不出现在排行榜：`lp user <玩家> permission set playtime.top.exclude true`

## 占位符（PlaceholderAPI）

| 占位符 | 说明 |
| --- | --- |
| `%playtime_top_<N>_displayname%` | 第 N 名玩家显示名（支持 PAPI 前缀，空位显示 no-data） |
| `%playtime_top_<N>_time%` | 第 N 名总时长（格式化） |
| `%playtime_top_<N>_name%` | 第 N 名玩家名（无前缀） |
| `%playtime_playtime%` | 查看者本人总时长（可放 TAB/称号） |
| `%playtime_top_active_<N>_time%` | 第 N 名非 AFK 活跃时长（可选换用） |

## 全息文件 `plugins/DecentHolograms/holograms/playtime.yml`

```yaml
location: world, x, y, z, 0, 0
pages:
  1:
    lines:
      - '&6&l🏆 &f&l在线时间排行榜 &7[1/3]'
      - '&7&m                            '
      - '&e#1  &f%playtime_top_1_displayname%  &7%playtime_top_1_time%'
      - '&f#2  &f%playtime_top_2_displayname%  &7%playtime_top_2_time%'
      - '&f#3  &f%playtime_top_3_displayname%  &7%playtime_top_3_time%'
      - '&f#4  &f%playtime_top_4_displayname%  &7%playtime_top_4_time%'
      - '&f#5  &f%playtime_top_5_displayname%  &7%playtime_top_5_time%'
      - '&f#6  &f%playtime_top_6_displayname%  &7%playtime_top_6_time%'
      - '&f#7  &f%playtime_top_7_displayname%  &7%playtime_top_7_time%'
      - '&f#8  &f%playtime_top_8_displayname%  &7%playtime_top_8_time%'
      - '&f#9  &f%playtime_top_9_displayname%  &7%playtime_top_9_time%'
      - '&f#10 &f%playtime_top_10_displayname%  &7%playtime_top_10_time%'
      - '&7&m                            '
      - '&8点击翻页：&a右键&8下一页 &8/ &c左键&8上一页'
  2:
    lines:
      - '&6&l🏆 &f&l在线时间排行榜 &7[2/3]'
      - '&7&m                            '
      - '&f#11 &f%playtime_top_11_displayname%  &7%playtime_top_11_time%'
      - '&f#12 &f%playtime_top_12_displayname%  &7%playtime_top_12_time%'
      - '&f#13 &f%playtime_top_13_displayname%  &7%playtime_top_13_time%'
      - '&f#14 &f%playtime_top_14_displayname%  &7%playtime_top_14_time%'
      - '&f#15 &f%playtime_top_15_displayname%  &7%playtime_top_15_time%'
      - '&f#16 &f%playtime_top_16_displayname%  &7%playtime_top_16_time%'
      - '&f#17 &f%playtime_top_17_displayname%  &7%playtime_top_17_time%'
      - '&f#18 &f%playtime_top_18_displayname%  &7%playtime_top_18_time%'
      - '&f#19 &f%playtime_top_19_displayname%  &7%playtime_top_19_time%'
      - '&f#20 &f%playtime_top_20_displayname%  &7%playtime_top_20_time%'
      - '&7&m                            '
      - '&8点击翻页：&a右键&8下一页 &8/ &c左键&8上一页'
  3:
    lines:
      - '&6&l🏆 &f&l在线时间排行榜 &7[3/3]'
      - '&7&m                            '
      - '&f#21 &f%playtime_top_21_displayname%  &7%playtime_top_21_time%'
      - '&f#22 &f%playtime_top_22_displayname%  &7%playtime_top_22_time%'
      - '&f#23 &f%playtime_top_23_displayname%  &7%playtime_top_23_time%'
      - '&f#24 &f%playtime_top_24_displayname%  &7%playtime_top_24_time%'
      - '&f#25 &f%playtime_top_25_displayname%  &7%playtime_top_25_time%'
      - '&f#26 &f%playtime_top_26_displayname%  &7%playtime_top_26_time%'
      - '&f#27 &f%playtime_top_27_displayname%  &7%playtime_top_27_time%'
      - '&f#28 &f%playtime_top_28_displayname%  &7%playtime_top_28_time%'
      - '&f#29 &f%playtime_top_29_displayname%  &7%playtime_top_29_time%'
      - '&f#30 &f%playtime_top_30_displayname%  &7%playtime_top_30_time%'
      - '&7&m                            '
      - '&8点击翻页：&a右键&8下一页 &8/ &c左键&8上一页'
```

## 翻页（点击交互，每页都要加）

```
/dh p addaction playtime 1 RIGHT NEXT_PAGE
/dh p addaction playtime 1 LEFT PREV_PAGE
/dh p addaction playtime 2 RIGHT NEXT_PAGE
/dh p addaction playtime 2 LEFT PREV_PAGE
/dh p addaction playtime 3 RIGHT NEXT_PAGE
/dh p addaction playtime 3 LEFT PREV_PAGE
```

点击类型 `RIGHT`=右键、`LEFT`=左键；首尾页自动无动作，无需特殊处理。

## 部署步骤

1. 下载 PlayTime 3.4.1 jar 放进 `plugins/`，**重启服务器**生成配置
2. 给玩家组加权限（见上）
3. 创建全息并定位置：站到主城目标点 → `/dh create playtime` → `/dh move playtime`（微调 `/dh adjust playtime up/down`）
4. 把上面的 `playtime.yml` 内容写进 `plugins/DecentHolograms/holograms/playtime.yml` → `/dh reload`
5. 执行翻页 actions（见上）
6. 验证：`/papi parse me %playtime_top_1_time%` 有值 → 主城看全息 → 点击测试翻页

## 性能注意

- PlayTime 3.3.2 曾有用户报主线程占用（3.4.x 已优化）；全息每秒刷新 30 行 top 占位符仍有开销
- **建议调大全息刷新间隔**：`plugins/DecentHolograms/config.yml` 中 `update-interval`（若存在，默认 1 秒）改为 **5–10 秒**，排行榜无需实时
- 时间显示格式（如 `2d 5h 12m`）在 PlayTime `config.yml` 的 time format 项调整，以插件生成的默认注释为准
- 多服同步才需要 MySQL；单服默认 YAML 即可，不额外引入远程库依赖
