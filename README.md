# SlimeFunHelper+

[SlimeFunHelper-26.x](https://github.com/zuogeren1/SlimefunHelper-26.x) 的官方客户端扩展插件（`mod_id`: `slimefunhelper-plus`）。

> **注意**：本插件**不是独立模组**，必须与宿主 `SlimeFunHelper` 一起放在客户端的 `mods/` 目录中使用；它是纯客户端插件，**请勿放进服务端**。

---

## 环境与版本支持

| 项目 | 要求 / 说明 |
| --- | --- |
| Minecraft | `26.1.2` / `26.2` / `26.3`（按游戏版本选择对应 jar） |
| 运行环境 | Fabric Client（`Fabric Loader >= 0.19.5`，`Java >= 25`，需安装 `Fabric API`） |
| 必装前置 | `SlimeFunHelper` 宿主（`0.3.0` 需搭配宿主 `1.2.2.1` 及以上） |
| 可选联动 | `conflux-map`（兼容 `0.1.16` ~ `0.1.18`，未安装时地图联动部分自动跳过） |

---

## 安装与上手

1. 从 [Releases](https://github.com/zuogeren1/sfhplus/releases) 下载对应 Minecraft 版本的插件 jar：
   - `SlimeFunHelper+-26.1.2-<版本>.jar`
   - `SlimeFunHelper+-26.2-<版本>.jar`
   - `SlimeFunHelper+-26.3-<版本>.jar`
2. 与同 MC 版本的 `SlimeFunHelper` 宿主 jar、`Fabric API` 一起放进客户端 `mods/` 目录。
3. **升级插件版本时请删除旧版 `SlimeFunHelper+*.jar`**，避免 Fabric Loader 因重复 `mod_id` 报错。
4. 进入游戏后，插件模块会自动并入宿主模块列表（同时聚合展示在 `SlimeFunHelper+` 分组下）：
   - 可在宿主 ClickGui 或自绘 `NewClickGui` 中查看并配置所有模块；
   - `CarbonRender`、`CustomModel`、`FriendWine`、`LocatorTrack` 支持在配置里绑定热键，也支持通过 `/sfh toggle <模块名>` 开关与 Tab 补全；
   - 若想让插件模块名显示为中文，可在 `Tasks` 分组的 `Settings` 中将「模块语言」从 `ENGLISH` 切到 `CHINESE`（仅切换插件模块显示名称，`NewClickGui` 保持英文，模块介绍、tooltip 与配置项保持中文）。

---

## 功能模块一览

| 模块 | 所属分组 | 默认状态 | 说明 |
| --- | --- | --- | --- |
| `NewClickGui` | `Tasks` | 触发打开 | 全新自绘界面，含模块化开关列表、`ALL` 全局分组、搜索、模块/全局配置页面与多种自绘控件；支持调节背景不透明度、界面动效、字体排版优化，也可开启「接管多人游戏界面的 ClickGui」用多人游戏右上角按钮打开 |
| `Settings` | `Tasks` | 全局配置 | 集中管理插件统一设置，目前提供「模块语言」（`ENGLISH` / `CHINESE`）切换，在 `NewClickGui`、宿主 `ClickGui`、HUD 列表与 `/sfh toggle` 同步生效 |
| `CustomModel` | `Render` | 默认关闭 | 加载本地 `.ysm` 自定义玩家模型替换自身外观（**仅本地自己可见，不向服务端或他人同步**）；内置模型选择页、动作预览与动画轮盘（当前仍在持续打磨中） |
| `CarbonRender` | `Render` | 默认关闭 | 物品模型 GPU 实例化渲染（移植自 Carbon，以世界掉落物为主）：重复物品几何只上传一次并合并绘制；仅在 OpenGL 3.3 及以上启用，不支持或出错时自动退回原版，关闭后完全交还原版 |
| `ConfluxMapHelper` | `Survival` | 按需启用 | 与 `conflux-map` 联动的地图增强模块：支持全屏地图右键菜单（自定义指令、自动补全、复制坐标、`/!!travel to`）、已加载区块边界绘制、旅行目标点同步、小地图连续缩放（`0.1` ~ `8`）、全屏地图内聊天输入框，以及将宿主标点显示或同步写入 Conflux 的 `SFH` 路径点分组 |
| `LocatorTrack` | `Render` | 默认开启 | 利用原版定位栏方位角与自身移动视差反推本地未加载实体的玩家大致坐标，作为附加行挂入宿主 `NameList` |
| `FriendWine` | `Render` | 默认关闭 | 纯客户端趣味渲染：让生物像果冻一样按节拍伸缩并原地旋转（不影响坐标、碰撞箱与实际判定），支持生物黑白名单、振幅/转速调整与内置/本地 `.ogg` 音频播放 |

---

## 配置文件与资源目录

- **插件主配置**：`config/slimefunhelper-plus/config.yml`
  - 统一存放插件各模块与 `NewClickGui` 的配置，不占用宿主配置目录。
  - 若升级自早期的独立配置（`conflux.yml` / `locator.yml`）或未拆仓前宿主配置中的 Conflux/定位栏配置，首次启动会自动迁移一次到 `config.yml`。
- **自定义玩家模型目录**：`config/slimefunhelper-plus/models/`
  - 将本地模型放入该目录（支持按子目录分类放置）后，在 `CustomModel` 配置页或模型选择界面即可浏览与切换。

---

## 反馈与开发构建

- 涉及本插件功能的问题或建议，请直接在本仓库提交 Issue，**不要向原版 `SlimeFunHelper` 作者反馈插件分支的问题**。
- **源码构建位置**：本仓库没有独立的 Gradle Wrapper，必须作为 `plugin/` 目录放到宿主仓库 [`SlimefunHelper-26.x`](https://github.com/zuogeren1/SlimefunHelper-26.x) 下，由宿主 `settings.gradle` 通过复合构建接入；单独 clone 本仓库无法编译。

```text
SlimefunHelper-26.x/
├── build/release/  ← 宿主三版本 jar（插件编译期依赖）
├── src/            ← 宿主源码
└── plugin/         ← 本仓库
```

在宿主仓库根目录执行（需配置 JDK 25）：

```bash
# 三版本编译检查
./gradlew -p plugin :26.2:compileJava :26.1.2:compileJava :26.3:compileJava --console=plain

# 三版本打包并汇总到 plugin/build/libs/
./gradlew -p plugin :26.2:build :26.1.2:build :26.3:build --console=plain

# 启动宿主客户端联动调试（会自动构建并同步最新插件 jar 到宿主 run/26.2/mods）
./gradlew :26.2:runClient --console=plain
```

> 请勿对插件内的 `.java` 源码执行任何自动格式化器，否则会破坏多版本预处理器（`//#if` / `//$$`）注释标记。
>
> 若你在开发环境启动26.3遇到随机崩溃问题，请更新你的[JDK25](https://learn.microsoft.com/en-us/java/openjdk/download)至最新版本以修复[这个](https://mail.openjdk.org/archives/list/hotspot-runtime-dev@openjdk.org/thread/3YNXUW247GLMW6ZCOFUEGDGHNWIYT347/)问题

---

## 许可与第三方声明

- 本项目原创代码与资源采用 [CC0 1.0 Universal](LICENSE) 协议贡献至公有领域（按“现状”提供，不作任何担保）。
- 本项目可能包含借用或移植自第三方项目的代码与资源，它们保留各自原始许可与版权归属，不因位于本仓库而改为 CC0（完整资产声明见 [`src/main/resources/NOTICE`](src/main/resources/NOTICE)）：
  - **Carbon**（[CHUJIAN-afk/Carbon](https://github.com/CHUJIAN-afk/Carbon)，Copyright (c) 2026 FirstSight，MIT License）：`CarbonRender` 物品实例化核心与着色器移植自其 `26.1.2-neoforge` 分支。
  - **YesSteveModel / YSM**（Apache License 2.0；内置默认模型动画资产按原项目声明为 CC0）：用作自定义模型缺失动画时的内置默认动作兜底。
  - **YSM 格式读取、加密与 Zstd 解压工具代码**（Copyright (c) 2026 Marciken，MIT License；内含 Apache-2.0 Zstd 派生实现）：各自源码文件头部均保留原始版权与许可声明。
  - **ImageStream**（`com.github.TartaricAlkaline:ImageStream`）：用于模型贴图解码，打包时直接打入插件 jar。

