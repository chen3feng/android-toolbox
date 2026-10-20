# 架构说明

本文档介绍工具箱的分层结构，以及如何按既有模式新增一个工具。

## 1. 总览

单 Activity + Compose Navigation：`MainActivity` 持有一个 `NavHost`，首页是 `"home"`，其余每个工具一个 route（即 `Tool.route`）。小组件和深链通过 Intent extra `com.phongchen.toolbox.extra.TOOL_ROUTE` 携带 route 启动 `MainActivity`，由 `onNewIntent` / 初始 Intent 转入对应页面；首页始终在返回栈底部。

依赖注入是一个手写的极小容器：`ToolboxApplication` 创建 `AppContainer`（SensorManager、SettingsRepository、LocationProvider、TorchController、SensorAvailability），ViewModel 继承 `ContainerViewModel` 取用，不引入 DI 框架。

## 2. 三件套：Engine / ViewModel / Screen

每个功能位于 `feature/<route>/`，固定三层：

| 层 | 职责 | 约束 |
| --- | --- | --- |
| `XxxEngine.kt` | 纯 Kotlin 数学/换算：滤波、单位换算、格式化、阈值判定 | 不 import 任何 `android.*`（除 `android.util.Log` 外基本不需要），可直接在 JVM 单测中验证 |
| `XxxViewModel.kt` | 继承 `ContainerViewModel`，暴露单个 `StateFlow<XxxUiState>`；收集传感器/定位 Flow、申请与释放硬件、写设置 | 状态集中在一个 data class；页面不可见时停止采集 |
| `XxxScreen.kt` | `@Composable`，套 `ToolScaffold(tool, onBack)`；无权限用 `ActionBanner`，无硬件用 `UnsupportedPanel` | 只渲染 state、回调 ViewModel；不直接持有传感器 |

通用 Compose 组件在 `core/ui/`：`ToolScaffold`、`ActionBanner`、`UnsupportedPanel`、滚动折线图 `RollingLineChart`（数据点 `ChartPoint(timeMs, value)`）。

## 3. 核心模块（core/）

- `Tool.kt` —— **唯一注册表**。枚举条目不只是 route，还携带标题/副标题/图标字符串资源、图标 drawable、widget 描述与 widget Receiver 类。首页网格、导航、深链合法性（`Tool.fromRoute`）、首页排序（`Tool.orderedBy`，新版本新增工具自动追加到末尾）全部由它生成。
- `sensor/SensorFlow.kt` —— `SensorManager.sensorFlow(type, periodUs): Flow<SensorSample>`，冷流：collect 时注册监听、取消时注销；设备缺少该传感器时以 `SensorMissingException` 结束。默认 `.conflate()`，UI 消费不过来时丢弃旧样本。
- `sensor/SensorAvailability.kt` —— 启动时探测一次各类传感器是否存在，首页据此隐藏不支持的卡片。
- `location/LocationProvider.kt` —— `fixes(): Flow<Location>` 封装定位权限后的 LocationManager 回调；`gnssStatus()` 提供 `GnssInfo(inView, usedInFix)`（可选能力，错误静默）。
- `prefs/SettingsRepository.kt` —— DataStore Preferences：真北开关、公制开关、水平仪置零角度、分贝计校准增益、首页排序等。
- `AppContainer.kt` / `ContainerViewModel.kt` —— 手写服务容器。

## 4. 硬件与权限约定

- 权限：CAMERA、RECORD_AUDIO、ACCESS_FINE/COARSE_LOCATION、ACTIVITY_RECOGNITION、VIBRATE；**不申请 INTERNET**。传感器本身 `<uses-feature required="false">`，相机/麦克风/步数等均声明为可选特性，保证任何手机都能安装。
- 运行时权限一律用 `rememberLauncherForActivityResult` + 页面内 `ActionBanner` 说明，拒绝后仍停留在页面。
- **硬件只在页面前台使用**：麦克风、相机、AudioTrack 等绑定 `ON_START/ON_STOP`（或 `DisposableEffect` / `awaitClose`），离开页面立即释放。CameraX 用 `ProcessCameraProvider.unbindAll()` 释放；录音停止后隐私指示灯应熄灭。
- 耗时/阻塞操作（ToneGenerator 蜂鸣、AudioTrack 写循环等）放到 `Dispatchers.Default/IO`，不阻塞主线程。

## 5. 桌面小组件（widget/）

`EntryWidget(tool)` 是通用 Glance 1×1 组件：浅色/深色双主题下的圆角底色 + 24dp 工具图标 + 名称，点击用 `actionStartActivity` 向 `MainActivity` 发 `EXTRA_TOOL_ROUTE` 深链。每个工具只需：

1. 一个极薄的 `XxxWidgetReceiver : GlanceAppWidgetReceiver()`，返回 `EntryWidget(Tool.XXX)`（集中在 `widget/receiver/WidgetReceivers.kt`）；
2. `res/xml/<route>_widget_info.xml`（40dp 1×1，`previewLayout` 指向预览）；
3. `res/layout/widget_preview_<route>.xml`（选择器中的静态预览）；
4. `AndroidManifest.xml` 中注册 receiver（`APPWIDGET_UPDATE` filter + provider meta-data）。

`WidgetPinner` 封装 `AppWidgetManager.requestPinAppWidget`；在 MIUI 上系统不弹标准确认框，允许一次后自动放置。

## 6. 资源约定

- 字符串默认中文 `res/values/strings.xml`，英文 `res/values-en/strings.xml`，两份文件键保持一致；新增文案两处都要加。
- 工具图标为 24dp 黑色矢量 `res/drawable/ic_tool_<route>.xml`，风格统一为 Material 填充/描边图标。
- 大屏读数统一用大号 display 字体 + 线性档位条 + Min/Average/Max 卡片 + Reset，参考 Metal/Light/Vibration 页面。

## 7. 新增一个工具的清单

以新增 route `foo` 为例：

1. 新建 `feature/foo/FooEngine.kt`（纯 Kotlin，含文档注释）、`FooViewModel.kt`（`ContainerViewModel` + 单一 UiState）、`FooScreen.kt`（`ToolScaffold`）。
2. `core/Tool.kt`：import receiver，加 `FOO("foo", …)` 枚举条目。
3. `MainActivity.kt`：import Screen，加一个 `composable(Tool.FOO.route) { FooScreen(onBack = …) }`。
4. `widget/receiver/WidgetReceivers.kt`：加 `FooWidgetReceiver`。
5. `AndroidManifest.xml`：需要的权限（运行时权限 + 对应可选 `<uses-feature>`）、receiver 块。
6. `feature/home/HomeScreen.kt`：加硬件可用性分支（恒可用 / 按传感器 / 按 PackageManager feature）。
7. 资源：两份 `strings.xml`（tool_/subtitle_/widget_description_ 及功能文案、`unsupported_foo`）、`ic_tool_foo.xml`、`foo_widget_info.xml`、`widget_preview_foo.xml`。
8. `app/src/test/.../feature/foo/FooEngineTest.kt`：覆盖 Engine 的换算、边界与归一化。
9. 验证：`./gradlew testDebugUnitTest lintDebug assembleDebug`，模拟器注入传感器/位置做功能验证，再上真机；全程观察 `adb logcat -b crash`。

## 8. 测试策略

- **JVM 单元测试**只打 Engine：滤波系数、单位换算、角度 wrap、格式化字符串、边界钳制等。无 Android 依赖所以跑得快。
- **模拟器（emulator-5554）**做端到端：
  - 传感器注入：`(echo "auth <token>"; echo "sensor set magnetic-field 0:0:300"; echo quit) | nc localhost 5554`，token 在 `~/.emulator_console_auth_token`；
  - 定位注入：`adb emu geo fix <经度> <纬度> <海拔> <卫星数> <速度节> <方位角>`（参数顺序固定）；
  - 音频用 `dumpsys media.audio_flinger` 确认会话存在与离开后释放。
- **真机**（小米 12）补充模拟器给不了的硬件：真实磁场/GPS/相机闪光灯/震动（`cmd vibrator_manager synced oneshot <ms>`）/MIUI 的组件固定流程。踩坑记录见仓库外的测试笔记。
- UI 自动化注意：不要并发执行 uiautomator dump（会互相踢掉 UiAutomationService）；坐标以 dump 出的 bounds 为准，不要按截图显示尺寸估算。
