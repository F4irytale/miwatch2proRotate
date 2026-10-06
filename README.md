# miwatch2proRotate

给 **小米 Watch 2 Pro** 用的屏幕方向旋转工具。

## 这玩意儿干什么用的

我这台 Watch 2 Pro 是**工程机**，硬件陀螺仪和指南针的方向是反的 —— 屏幕上的方向指示和实际朝向对不上。

传感器那层翻过一遍（`dumpsys sensorservice`、`/vendor/etc/sensors/` 下的 `sns_reg_config`、`atherton_lw_ak991x_0.json`、`oem_config.xml` 等），没找到能改的开关，属于硬件标定问题，软件层改不了。

既然修不了传感器，那就反过来 —— **把屏幕转 N 度**，让画面方向对上实际朝向。视觉上就正常了。

两种改法，按需求选：

| 方式 | 生效 | 重启后 | 适用 |
|---|---|---|---|
| [adb 临时改](#一临时改adb一条命令) | 立即 | **失效，回到 0°** | 快速验证某个角度合不合适 |
| [App 持久化](#二用-apk-改持久化) | 立即 | **自动恢复** | 日常长期使用 |

> 已经确认过：adb 改的方向**重启必丢**，所以长期用必须靠 App。

---

## 一、临时改（adb 一条命令）

适合先试角度。不用装任何东西。

### 1. 连接手表

手表上打开「设置 → 关于 → 开发者选项 → 无线调试」（没有开发者选项的话，先在「关于」里连点版本号开启）。

在「无线调试」页面能看到 **IP 地址和端口**，配对码也在那儿。先配对，再连接：

```bash
# 配对（只需做一次，每次开关无线调试会变）
adb pair 192.168.1.6:42851 938957

# 连接（注意：配对端口和连接端口不一样）
adb connect 192.168.1.6:5555

adb devices          # 确认连上了
```

> 实际端口以你手表上显示的为准，`192.168.1.6` 只是示例。

### 2. 转屏幕

```bash
# 关掉自动旋转，避免被传感器抢回去
adb shell settings put system accelerometer_rotation 0

# 锁定方向
adb shell wm user-rotation lock <N>
```

`N` 的取值：

| N | 效果 |
|---|---|
| `0` | 0°（默认） |
| `1` | 90° |
| `2` | 180° ← **我这台工程机用这个** |
| `3` | 270° |

转完抬手看一眼就行，立即生效，不用重启。

### 3. 查看当前方向 / 恢复

```bash
# 查看
adb shell wm user-rotation
adb shell settings get system user_rotation

# 恢复默认
adb shell wm user-rotation lock 0
```

**注意**：这种方式重启手表就没了，会自己变回 `0`。这就是为什么要做下面这个 App。

---

## 二、用 APK 改（持久化）

开机自动恢复，不用每次 adb。

### 1. 安装

```bash
adb install -r RotateApp.apk
```

### 2. 授权（关键，必须做一次）

App 要写系统设置，需要 `WRITE_SETTINGS` 权限。手表上没有授权界面，只能用 adb 给：

```bash
adb shell appops set com.czsmart.rotate WRITE_SETTINGS allow
```

不授权的话，App 里点角度没反应，界面会提示"未授权"。

> 卸载重装后要重新授权一次。

### 3. 使用

手表应用列表里找到「**屏幕旋转**」打开（或者用 adb 启动）：

```bash
adb shell am start -n com.czsmart.rotate/.MainActivity
```

界面就四个按钮：

- **0° / 90° / 180° / 270°** —— 点哪个转哪个，当前生效的那个会变成蓝色
- **开机自动恢复** —— 勾上（默认开）。开着的话，重启后 `BootReceiver` 会自动把上次选的角度写回去

因为手表重启时系统可能还会重置一次方向，所以 App 在开机后会**补写两次**（开机时一次，8 秒后再一次），确保生效。

选完就可以关了，设置是立即生效并持久保存的。

### 4. 卸载

```bash
adb uninstall com.czsmart.rotate
adb shell wm user-rotation lock 0     # 顺手把方向复位
```

---

## 从源码构建

不想用仓库里的 APK、想自己改的话：

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
bash build.sh
```

依赖：

- **Android SDK** —— 需要 `build-tools/36.0.0` 和 `platforms/android-35`
- **JDK 11+**（`javac`、`keytool`）
- **Python 3**（往 APK 里塞 `classes.dex` 用）
- **bash** —— Windows 上用 Git Bash

脚本**不用 Gradle**，直接调 SDK 里的 `aapt2` → `javac` → `d8` → `zipalign` → `apksigner` 走一遍，Windows 和 Linux/macOS 都能跑（自动处理 `.exe` / `.bat` 后缀差异）。

产物：`RotateApp.apk`

签名用的是仓库里的 `debug.keystore`（密码 `android`，公开的调试密钥）—— 只够自用，别拿去正式分发。

### 源码结构

```
src/com/czsmart/rotate/
├── Rotation.java       读写 Settings.System.USER_ROTATION + 记住用户选择
├── MainActivity.java   四个角度按钮 + 开机恢复开关
└── BootReceiver.java   开机后补写方向
```

实际改的就是系统里这两个值：

```
settings put system accelerometer_rotation 0      # 关自动旋转
settings put system user_rotation <0|1|2|3>       # 锁定角度
```

App 做的事和上面 adb 那两条命令完全一样，只是帮你记住选择并在开机后自动重放。

---

## 说明

- 不同批次的 Watch 2 Pro 情况可能不一样，**先用 adb 那条命令试出你的正确角度**，再决定要不要装 App。
- 只在这台工程机上验证过，其他机型自行判断。
- 改的是 `USER_ROTATION` 这种运行时可逆的值，**不动传感器标定等持久化硬件配置**，卸载后 `wm user-rotation lock 0` 即可完全恢复。
