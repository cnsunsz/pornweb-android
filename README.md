# PornWeb Android

[![Android CI](https://github.com/cnsunsz/pornweb-android/actions/workflows/android.yml/badge.svg)](https://github.com/cnsunsz/pornweb-android/actions/workflows/android.yml)
[![GitHub release](https://img.shields.io/github/v/release/cnsunsz/pornweb-android)](https://github.com/cnsunsz/pornweb-android/releases)

仓库：<https://github.com/cnsunsz/pornweb-android>（公开）


自托管成人媒体库 **PornWeb** 的原生 Android 客户端（Kotlin + Jetpack Compose + Media3 ExoPlayer）。界面参考 Emby / Jellyfin 手机端：深色影院风、海报墙、继续观看、详情与播放器。

- 包名：`com.pornweb.android`
- 应用名：PornWeb
- minSdk 26 / targetSdk 35 / compileSdk 35
- 默认界面语言：简体中文
- 默认服务器：`http://43.196.70.121:10086`（可在「连接服务器」或「我的」中修改）
- **不会**内置任何用户名或密码

## 环境要求

- JDK 17 或 21（本仓库用 JDK 21 构建通过）
- Android SDK：`platform-tools`、`platforms;android-35`、`build-tools;35.0.0`
- Android Studio Ladybug / Koala 及以上，或仅用命令行 Gradle Wrapper 8.9

将 SDK 路径写入 `local.properties`（不要提交到 git）：

```
sdk.dir=/opt/android-sdk
```

macOS / Windows 示例：

```
sdk.dir=/Users/你的用户名/Library/Android/sdk
sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

也可设置环境变量 `ANDROID_HOME` / `ANDROID_SDK_ROOT`。

## 用 Android Studio 打开

1. 安装 [Android Studio](https://developer.android.com/studio)，安装 SDK Platform 35 与 Build-Tools 35.0.0。
2. **File → Open**，选择本目录 `pornweb-android`。
3. 等待 Gradle Sync。若提示 SDK 路径，指向本机 Android SDK。
4. 连接手机或启动模拟器，点击 Run。首次安装需允许「未知来源」或通过 USB 调试。

命令行同步：

```bash
./gradlew :app:assembleDebug
```

## 命令行编译 Debug APK

在项目根目录：

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # 按本机 JDK 修改
export ANDROID_HOME=/opt/android-sdk
./gradlew :app:assembleDebug
```

成功后 APK 位于：

- `app/build/outputs/apk/debug/app-debug.apk`
- 根目录副本：`PornWeb-debug.apk`

安装到手机：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

或把 `PornWeb-debug.apk` 拷到手机后直接打开安装（需允许未知来源）。

## 如何指向你的服务器

1. 首次启动进入 **连接服务器**，地址栏已预填 `http://43.196.70.121:10086`。
2. 点「测试连接」确认 `GET /api/health` 正常，再点「连接」。
3. 使用你自己的用户名/密码登录（或注册，密码至少 6 位）。
4. 之后可在底部 **我的** 修改服务器 URL 并保存。

本应用允许明文 HTTP（`usesCleartextTraffic` + `network_security_config`），可直连 `http://IP:端口` 的自建实例。Token 会同时放在：

- 请求头 `Authorization: Bearer <token>`
- 海报 / 封面 / 视频流 URL 查询参数 `token=`

## 界面

| 屏幕 | 说明 |
| --- | --- |
| 连接服务器 | 填写 URL、测试、连接 |
| 登录 / 注册 | 保存 token（优先 EncryptedSharedPreferences） |
| 首页 | 继续观看、媒体库磁贴、最近添加 |
| 媒体库 | 海报网格、分页、下拉刷新、排序、类型/目录筛选 |
| 搜索 | 300ms 防抖，`search=` 参数 |
| 详情 | 背景、海报、元数据、播放 / 继续 / 从头 |
| 播放器 | Media3 ExoPlayer，全屏、熄屏锁定、进度上报（约 10 秒及暂停） |
| 我的 | 用户名、服务器地址、改密、退出 |

底部导航：**首页 / 媒体库 / 搜索 / 我的**。

## 网络说明

列表与详情超时约 60–90 秒。播放使用独立的 ExoPlayer HTTP 数据源（带 Bearer 头）。若服务端返回 401（登录/注册除外），会清除 token 并回到登录页。

## GitHub 编译发行

推送到 `main` 或提交 PR 会跑 [Android CI](https://github.com/cnsunsz/pornweb-android/actions)：自动编 debug / release APK，并作为 Actions Artifact 上传。

打 tag 会同时发 GitHub Release（APK 挂在发行页）：

```bash
git tag v1.0.0
git push origin v1.0.0
```

也可在 Actions 页手动 **Run workflow**。

默认 release APK 用 debug 密钥签名，方便直接安装。若要正式签名，在仓库 Settings → Secrets and variables → Actions 里添加：

- `SIGNING_STORE_BASE64`：keystore 文件的 base64
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`


## 覆盖安装（签名）

从 **v1.0.5** 起，debug / release APK 都使用仓库内固定的 `keystore/pornweb.jks` 签名，因此后续版本可以直接覆盖安装，不必先卸载。

若手机上已安装 **v1.0.4 及更早**（当时 GitHub Actions 用的是临时 debug 签名），第一次装 1.0.5 仍需卸载旧包一次；之后就都可覆盖更新。

## 许可证

MIT
