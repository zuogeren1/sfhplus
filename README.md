# SlimeFunHelper+

SlimeFunHelper 的扩展插件。**不是独立 mod**：它由宿主在运行时加载，编译期依赖宿主 jar。

## 构建位置

本仓库必须放在宿主仓库的 `plugin/` 目录下：

```
SlimefunHelper-26.x/
├── src/            ← 宿主源码
├── build/release/  ← 宿主 jar（插件编译期依赖）
└── plugin/         ← 本仓库
```

宿主构建脚本按相对路径读宿主版本号与宿主 jar，单独 clone 出来编译不了。

## 构建

本仓库没有自己的 Gradle wrapper，一律用**宿主仓库根**的 `gradlew` 加 `-p plugin`：

```bash
export JAVA_HOME='/f/Program Files/Zulu/zulu-25'
./gradlew -p plugin :26.2:compileJava :26.1.2:compileJava :26.3:compileJava --console=plain
```

宿主侧 `./gradlew :26.2:runClient` 会自动构建本插件并把 jar 放进 `run/26.2/mods`。

## 版本

`mod_version` 在 `gradle.properties`，只适配宿主版本，不随宿主号变化。

## 许可

[CC0 1.0 Universal](LICENSE)（公有领域贡献）。

第三方代码与资源的归属见 [`src/main/resources/NOTICE`](src/main/resources/NOTICE)，
它们保留原始许可，不因位于本仓库而改为 CC0。
