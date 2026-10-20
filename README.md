# 工具箱 Toolbox

[![platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android)](https://developer.android.com/)
[![kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

[English](README.en.md)

一款纯本地、无广告、无网络权限的 Android 实用工具集合，使用 Kotlin + Jetpack Compose Material3 单 Activity 架构开发。手机上的传感器能做什么，这个 App 就做什么：磁场、声音、震动、光线、位置、相机、天文历算……全部计算都在设备本地完成。

## 功能（16 个工具）

| 工具 | 说明 | 依赖的硬件 / 权限 |
| --- | --- | --- |
| 指南针 Compass | 磁北/真北切换，旋转矢量 + 磁力计融合 | 磁力计、方向传感器 |
| 亮度计 Light | 环境照度（lux）实时读数、Min/Avg/Max、曲线 | 光线传感器 |
| 高度计 Altimeter | GPS 海拔（内置 EGM96 大地水准面模型，把 WGS84 椭球高离线换算为正高）；有气压计时使用气压计，支持 QNH/已知高度校准 | 定位权限 |
| 手电筒 Flashlight | 前置/后置闪光灯常亮 | 相机闪光灯、CAMERA |
| 水平仪 Level | 气泡、俯仰角/横滚角、可置零 | 重力/加速度传感器 |
| 分贝计 Sound | 麦克风 RMS → dB，支持手动校准偏移估算 SPL | 麦克风、RECORD_AUDIO |
| 速度表 Speed | GPS 当前/最大/平均速度与行程里程，km/h ↔ mph | 定位权限 |
| 金属探测器 Metal | 磁场强度 µT、档位提示、60 秒滚动曲线 | 磁力计 |
| 计步器 Pedometer | 步数、距离、卡路里（步长可估） | 步数传感器、ACTIVITY_RECOGNITION |
| GPS 信息 GPS | 经纬度（小数/度分秒）、海拔、精度、速度航向、卫星（参与定位/可见）、复制/分享/打开地图 | 定位权限 |
| 震动仪 Vibration | 线加速度幅值 m/s²、波形、峰值 | 加速度传感器 |
| 节拍器/音频发生器 Audio | BPM 节拍（拍号、Tap tempo）+ 正弦/方波/锯齿波纯音 | 扬声器 |
| 秒表/倒计时 Timer | 百分秒计时、计圈；1–15 分钟倒计时，结束提示音 + 震动 | VIBRATE |
| 直尺/量角器/挂线锤 Measure | 按屏幕物理 DPI 绘制的 30 cm 刻度尺；倾角量角器；铅垂 | 重力传感器 |
| 放大镜/镜子 Magnifier | CameraX 实时预览、捏合变焦、闪光灯、冻结画面、前后摄切换 | 相机、CAMERA |
| 日月天文 Sun & Moon | 真太阳时/平太阳时与均时差、日出日落、昼长、正午太阳高度、月相与照亮比例 | 定位权限 |

没有对应硬件的工具会自动从首页隐藏；有权限的工具在未授权时显示内联说明与「授予权限」按钮，不弹骚扰式对话框。

每个工具都可以**固定为桌面 1×1 快捷小组件**（Glance），点击直接深链打开对应工具。首页支持长按拖动自定义排序，顺序持久化保存。

## 隐私

- 清单中**没有 `INTERNET` 权限**，应用在技术上无法联网，不存在数据上传或第三方 SDK 追踪。
- 麦克风、相机、传感器只在对应页面处于前台时采集，离开页面立即释放（麦克风隐私指示灯随之熄灭）。
- 所有设置（单位制、校准值、首页排序、置零角度等）仅保存在本地 DataStore。

## 技术栈

- Kotlin 2.3、AGP 9.4、JVM 17；minSdk 26（Android 8.0）、targetSdk 36、compileSdk 37
- Jetpack Compose（BOM 2026.09）Material3、Navigation Compose 单 Activity
- Glance 1.2 AppWidget、DataStore Preferences
- CameraX 1.4（放大镜）
- 传感器 / LocationManager / AudioTrack / AudioRecord / Vibrator 系统 API，无第三方运行时库

## 构建

需要 JDK 17 与 Android SDK（Platform 37）：

```bash
./gradlew assembleDebug          # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug              # 静态检查
./gradlew testDebugUnitTest      # 引擎层 JVM 单元测试
```

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构与架构

每个工具遵循 `Engine`（纯 Kotlin 数学，JVM 可测）→ `ViewModel`（StateFlow）→ `Screen`（Compose）的分层；首页、路由、深链、小组件全部由 `core/Tool.kt` 中的一个枚举生成。新增一个工具只需按清单添加，详见 [docs/architecture.md](docs/architecture.md)。

```text
app/src/main/java/com/phongchen/toolbox/
├── core/            # Tool 注册表、传感器 Flow、定位、设置、通用 UI 组件
├── feature/         # 每个工具一个包：Engine + ViewModel + Screen
├── widget/          # Glance 通用 1×1 入口组件与各工具 Receiver
└── MainActivity.kt  # 单 Activity + NavHost + 深链
app/src/test/        # 各 Engine 的 JVM 单元测试
```

## 数据来源

- 海拔换算使用的 EGM96 0.5° 大地水准面网格（`app/src/main/assets/egm96_0_5.bin`）由 NGA 官方 15′ 网格 `WW15MGH.DAC`（美国政府作品，公有领域）抽点生成，双三次插值，与全阶球谐参考值偏差一般 < 0.3 m、最坏约 2 m；生成与校验方法见 `GeoidModel.kt` 注释。

## 许可证

MIT
