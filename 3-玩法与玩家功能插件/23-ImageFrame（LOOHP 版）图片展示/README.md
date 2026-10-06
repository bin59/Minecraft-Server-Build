你装的是 **ImageFrame（LOOHP 版）**，版本 `2026.1.5.0`，核心功能：把网络图片/GIF 加载到**地图**或**物品展示框**上（还能加地图标记、做隐形展示框、网页上传图片）。本地配置已齐全且语言是 `zh_cn`（中文界面已开）。

下面按"怎么上手"给你一份实用指南。

---

## 一、三种核心玩法（看你想要哪种效果）

| 玩法                       | 命令                                                | 适用场景                                                     |
| -------------------------- | --------------------------------------------------- | ------------------------------------------------------------ |
| **① 单张地图物品**         | `/imageframe create 名字 图片URL 宽 高`             | 拿在手里/放地上看，最省事                                    |
| **② 物品展示框拼墙**       | `/imageframe create 名字 URL selection`（配合选区） | 一面墙拼大图/地铁导览图/服务器宣传墙                         |
| **③ 综合物品（Combined）** | `/imageframe create 名字 URL 宽 高 combined`        | 给玩家一张"大图物品"，右键自动找空展示框铺开，敲掉整块收回来 |

> 格式支持 **PNG/JPG/WebP/GIF**。尺寸填的是**展示框/地图数量**（如 3×2），会自动裁切不变形。

---

## 二、物品展示框拼墙的标准流程（最常用）

1. **摆框**：在墙上按行列放好物品展示框（矩形网格）
2. **选框**：`/imageframe select` 进入选区模式 → 按提示**右键点击对角两个展示框** → 确认选区大小
3. **放图**：`/imageframe create 名字 URL selection`
4. 完事：图片自动铺满你选好的所有展示框

> 提示：配合插件的**隐形展示框**更好看。生存模式把隐形药水泼在展示框物品上即可隐形（`InvisibleFrame.MaxConversionsPerSplash: 8`）。

---

## 三、管理/操作命令（中文对照）

| 命令                                                      | 作用                                      |
| --------------------------------------------------------- | ----------------------------------------- |
| `/imageframe list`                                        | 列出我创建的图片                          |
| `/imageframe get 名字 [combined/selection]`               | 取回已创建的图片                          |
| `/imageframe delete 名字`                                 | 删除图片                                  |
| `/imageframe rename 旧名 新名`                            | 重命名                                    |
| `/imageframe refresh [名字] [新URL]`                      | 从源地址刷新图片（改图源不用重新摆）      |
| `/imageframe clone 名字 新名`                             | 复制一张同属性图片                        |
| `/imageframe info`                                        | 查看手中图片的信息                        |
| `/imageframe playback 名字 pause`                         | 动图暂停/播放                             |
| `/imageframe playback 名字 jumpto 秒`                     | 动图跳转到指定秒                          |
| `/imageframe overlay 名字 URL`                            | 在原版地图上叠加图片                      |
| `/imageframe marker add 图片 标记名 方向0-15 类型 [文字]` | 在地图上加标记（指针/文字）               |
| `/imageframe marker remove/clear ...`                     | 移除/清空标记                             |
| `/imageframe setaccess 名字 玩家 权限`                    | 把图片分享给其他玩家（可授权编辑/复制等） |
| `/imageframe reload`                                      | 重载插件                                  |

**跨玩家访问**：`<玩家>:<名字>` 语法，如 `/imageframe get LOOHP:map combined`。

---

## 四、网页上传图片（你已开启）

你本地 `UploadService` 已开启（端口 **8517**）。流程：

1. 游戏内先 `create` 一个占位图（会给你一个上传链接）
2. 浏览器打开 `http://服务器IP:8517`，**5 分钟内**把图片拖进去上传
3. 回游戏继续，图片就换上了

> ⚠️ 你 config 里 `DisplayURL: [http://change.this.to.your.server.ip.in.the.config](http://change.this.to.your.server.ip.in.the.config)` **还是占位符**，需要改成你的服务器公网 IP，否则上传链接没法用。

---

## 五、本地配置注意（针对你的服务器）

```yaml
Language: zh_cn                    # 已中文
RequireEmptyMaps: true             # 生存模式创建需消耗空地图（非创造）
MaxSize: 100                       # 单张图片最大展示框数
PlayerCreationLimit: default: 10   # 每个玩家最多 10 张（vip 15 / moderator 20）
RestrictImageUrl.Enabled: false    # 当前未限制图片来源域名
UploadService.Enabled: true        # 网页上传已开（端口 8517）
ImageFrameClient.Enabled: true     # 高清客户端 mod 已开
```

---

## 六、基岩版（Geyser）注意事项

- **静态图片**：基岩玩家能看到展示框里的地图像素图（地图数据会同步）
- **GIF 动画 / ImageFrameClient 高清全彩**：是 Java 端特性，基岩玩家**看不到**（会显示静态帧/低清图）
- 如果想让基岩玩家也有体验，建议用**静态图**做展示墙

---

## 最省事的快速上手

想先看效果：找张图 URL，站到一面墙前，跑

```
/imageframe create test [https://example.com/pic.png](https://example.com/pic.png) 2 1 selection
```

（先 `/imageframe select` 框选 2×1 的展示框）——立刻能看到效果。

需要我帮你：把 `DisplayURL` 改成实际 IP、配几张展示墙（如服务器地图/宣传图）、或者整理一份完整的权限节点表吗？
