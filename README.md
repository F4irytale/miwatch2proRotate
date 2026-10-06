# MI Watch 2 Pro 屏幕旋转

针对 **小米 Watch 2 Pro** 的屏幕方向调整工具，用于规避部分工程机上陀螺仪与指南针方向反向所导致的显示异常。

## 背景

部分小米 Watch 2 Pro 工程机的硬件传感器存在方向标定问题：陀螺仪与指南针的输出方向与设备实际朝向相反，导致系统界面中的方向指示（方位显示、指南针等）始终反向。

该问题位于传感器标定层。经排查 `/vendor/etc/sensors/` 下的配置文件（`sns_reg_config`、`atherton_lw_ak991x_0.json`、`oem_config.xml` 等）以及 `dumpsys sensorservice` 的输出，确认无法通过软件配置修正。

作为替代方案，本项目通过**旋转屏幕显示方向**，使画面与设备实际朝向重新对齐，从视觉上消除反向问题。

## 方案对比

| 方案 | 生效时机 | 重启后 | 适用场景 |
|---|---|---|---|
| adb 临时修改 | 立即 | 失效，恢复 0° | 验证角度、临时使用 |
| APK 持久化 | 立即 | 自动恢复 | 长期使用 |

经实测，通过 adb 修改的方向在设备重启后必然丢失，长期使用需依赖 APK 方案。

## 方式一：adb 临时修改

无需安装应用，适用于快速验证角度是否合适。

### 1. 连接设备

在手表上开启无线调试（设置 → 关于 → 开发者选项 → 无线调试），获取设备 IP、端口及配对码。

```bash
adb pair <IP>:<配对端口> <配对码>
adb connect <IP>:<连接端口>
adb devices
```

配对端口与连接端口不同，具体以设备显示为准。

### 2. 设置旋转方向

```bash
# 关闭自动旋转，防止方向被传感器覆盖
adb shell settings put system accelerometer_rotation 0

# 锁定方向
adb shell wm user-rotation lock <N>
```

`<N>` 的取值：

| N | 旋转角度 |
|---|---|
| 0 | 0°（默认） |
| 1 | 90° |
| 2 | 180° |
| 3 | 270° |

### 3. 查询与恢复

```bash
# 查询当前方向
adb shell wm user-rotation
adb shell settings get system user_rotation

# 恢复默认
adb shell wm user-rotation lock 0
```

> 此方式在设备重启后失效，方向将恢复为 0°。

## 方式二：安装 APK 持久化

支持开机自动恢复，无需重复执行 adb 命令。

### 1. 下载与安装

从 [Releases](https://github.com/F4irytale/miwatch2proRotate/releases/latest) 下载 `RotateApp.apk`，或直接使用仓库根目录下的 APK：

```bash
adb install -r RotateApp.apk
```

### 2. 授权

应用需要写入系统设置，依赖 `WRITE_SETTINGS` 权限。由于手表未提供授权界面，需通过 adb 授予：

```bash
adb shell appops set com.czsmart.rotate WRITE_SETTINGS allow
```

未授权时，应用界面将提示「未授权」，点击角度按钮不会生效。卸载重装后需重新授权。

### 3. 使用

在应用列表中找到「屏幕旋转」并打开，或通过 adb 启动：

```bash
adb shell am start -n com.czsmart.rotate/.MainActivity
```

界面包含以下元素：

- **0° / 90° / 180° / 270°**：点击立即切换，当前生效的角度以蓝色高亮显示
- **开机自动恢复**：默认开启。开启后，设备重启时由 `BootReceiver` 自动恢复上次选择的角度

考虑到系统在启动过程中可能重置显示方向，应用会在开机后写入两次（启动时一次，8 秒后补写一次）以确保生效。

设置立即生效并持久保存，无需保持应用运行。

### 4. 卸载

```bash
adb uninstall com.czsmart.rotate
adb shell wm user-rotation lock 0
```

## 从源码构建

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
bash build.sh
```

### 依赖

- **Android SDK**：需包含 `build-tools/36.0.0` 与 `platforms/android-35`
- **JDK 11 或更高版本**：需提供 `javac`、`keytool`
- **Python 3**
- **bash**：Windows 环境下使用 Git Bash

构建脚本不依赖 Gradle，直接调用 SDK 工具链完成 `aapt2` → `javac` → `d8` → `zipalign` → `apksigner` 流程，兼容 Windows 与 Linux/macOS。

输出文件：`RotateApp.apk`

签名使用仓库内的 `debug.keystore`（密码 `android`），仅供本地调试使用，不适用于正式分发。

## 项目结构

```
.
├── AndroidManifest.xml
├── build.sh                     构建脚本
├── debug.keystore               调试签名密钥
├── RotateApp.apk                构建产物
├── res/drawable/ic_launcher.xml
└── src/com/czsmart/rotate/
    ├── Rotation.java            读写屏幕方向设置并持久化用户选择
    ├── MainActivity.java        角度选择界面与开机恢复开关
    └── BootReceiver.java        开机后恢复方向
```

## 实现原理

应用修改的是系统运行时的两个配置项：

```
settings put system accelerometer_rotation 0
settings put system user_rotation <0|1|2|3>
```

与 adb 方案完全一致，区别在于应用会记录用户选择，并在设备开机后自动重新写入。

该方案仅修改可逆的运行时配置，不涉及传感器标定等持久化硬件参数。卸载后执行 `wm user-rotation lock 0` 即可完全恢复。

## 注意事项

- 不同批次的设备情况可能存在差异，建议先通过 adb 命令确定适用角度，再决定是否安装 APK
- 目前仅在小米 Watch 2 Pro 工程机上验证，其他机型请自行评估
- 本项目不涉及传感器固件或标定数据的修改
