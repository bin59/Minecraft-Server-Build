# 使用 BedWars API
ScreamingBedWars 官方文档简体中文翻译 — API
> 原文：[API](https://docs.screamingsandals.org/BedWars/latest/api/)

> 翻译说明：本译文为官方文档的简体中文翻译，命令、权限节点、配置项/键名、占位符、物品/升级 ID、API 名称、版本号、URL 及代码块一律保留英文原文。

使用 BedWars API 与 BedWars 插件交互并扩展其功能。

> 警告
>
> API 仍在演进中。从 `0.3.0` 开始，未来版本将引入破坏性变更。

## 安装

**Maven：**

```xml
<repositories>
  <repository>
    <id>screaming-repo</id>
    <url>https://repo.screamingsandals.org/public/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.screamingsandals.bedwars</groupId>
    <artifactId>BedWars-API</artifactId>
    <version>LATEST_VERSION_HERE</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

**Gradle（Groovy DSL）：**

```groovy
repositories {
    maven { url 'https://repo.screamingsandals.org/public/' }
}

dependencies {
    compileOnly 'org.screamingsandals.bedwars:BedWars-API:LATEST_VERSION_HERE'
}
```

**Gradle（Kotlin DSL）：**

```kotlin
repositories {
    maven(url = uri("https://repo.screamingsandals.org/public/"))
}

dependencies {
    compileOnly("org.screamingsandals.bedwars:BedWars-API:LATEST_VERSION_HERE")
}
```

如果你更喜欢或需要访问整个插件（包括未通过 API 暴露的内部实现），也可以依赖主 `BedWars` 插件而非 `BedWars-API`。但是，**不推荐这种做法**，因为内部类和方法**可能随时变更，且不会另行通知**。

## 接入 API

将其纳入构建后，即可通过以下方式访问 API：

```java
import org.screamingsandals.bedwars.api.BedwarsAPI;

...
BedwarsAPI api = BedwarsAPI.getInstance();
...

```

Javadoc：[https://repo.screamingsandals.org/javadoc/releases/org/screamingsandals/bedwars/BedWars-API/LATEST_VERSION_HERE](https://repo.screamingsandals.org/javadoc/releases/org/screamingsandals/bedwars/BedWars-API/LATEST_VERSION_HERE)
