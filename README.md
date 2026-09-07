# PornWeb Android

[![Android CI](https://github.com/cnsunsz/pornweb-android/actions/workflows/android.yml/badge.svg)](https://github.com/cnsunsz/pornweb-android/actions/workflows/android.yml)
[![GitHub release](https://img.shields.io/github/v/release/cnsunsz/pornweb-android)](https://github.com/cnsunsz/pornweb-android/releases)

Repo: <https://github.com/cnsunsz/pornweb-android> (public)

---

## 简介 / Overview

**中文：** 自托管媒体库 [PornWeb](https://github.com/cnsunsz/pornweb) 的原生 Android 客户端（Kotlin + Jetpack Compose + Media3 ExoPlayer）。深色管站风 UI，海报墙、继续观看、演员、详情与内置播放器；也可调起系统/第三方外部播放器（VLC、MX 等）。

**English:** Native Android client for the self-hosted [PornWeb](https://github.com/cnsunsz/pornweb) media library (Kotlin + Jetpack Compose + Media3 ExoPlayer). Dark tube-style UI with cover wall, continue watching, actors, details, and built-in player — plus Emby-style **external player** via the system chooser (VLC, MX Player, system player, etc.).

| | |
| --- | --- |
| Package / 包名 | `com.pornweb.android` |
| App name | PornWeb |
| SDK | min 26 · target/compile 35 |
| Default UI language | 简体中文 |
| Default server | `http://web.cnsun.top:2052` (editable in Connect / Me) |
| Credentials | **Never** baked into the APK |

Related fixed-server build (no server picker): [`pornweb-android-fixed`](https://github.com/cnsunsz/pornweb-android-fixed).

---

## Features / 功能

- Home cover wall, libraries, search, actors grid  
- Actor photos: load only when API returns `/api/actors/photo?name=…`; otherwise leave blank (no work-poster fallback) — aligned with web **v2.1.6**  
- Detail + built-in ExoPlayer (gestures, long-press speed, resume)  
- **External player**: open the stream URL with another app (not a second built-in player)  
- Playback settings (speed, skip, swipe, resume, …)  
- Stable signing from **v1.0.5+** for overlay updates  

---

## Requirements / 环境

- JDK 17 or 21 (CI uses 21)
- Android SDK: `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`
- Android Studio Ladybug/Koala+ or Gradle Wrapper 8.9

`local.properties` (do not commit):

```
sdk.dir=/opt/android-sdk
```

---

## Build / 编译

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # adjust to your JDK
export ANDROID_HOME=/opt/android-sdk
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio → Sync → Run.

---

## Connect your server / 连接服务器

1. First launch → **Connect server** (prefilled `http://web.cnsun.top:2052`).
2. Test `GET /api/health`, then Connect.
3. Login / register with your own account (password ≥ 6 chars).
4. Change the URL anytime under **Me**.

Cleartext HTTP is allowed. Auth uses `Authorization: Bearer` plus `token=` on poster/stream URLs.

---

## Screens / 界面

| Screen | 说明 / Notes |
| --- | --- |
| Connect | Server URL, health check |
| Login / Register | Token in EncryptedSharedPreferences |
| Home | Continue + dense cover wall + libraries |
| Libraries | Grid, paging, sort/filter |
| Actors | Searchable grid; photo only if API provides it |
| Search | Debounced `search=` |
| Detail | Metadata, cast chips, play / resume / **external player** |
| Player | ExoPlayer + external-player shortcut |
| Me / Playback settings | Server, password, player prefs |

Bottom tabs: **Home · Libraries · Actors · Search · Me**.

---

## Releases / 发行

Push to `main` or tag `v*` → [Android CI](https://github.com/cnsunsz/pornweb-android/actions) builds debug/release APKs. Tags also create GitHub Releases (notes from `CHANGELOG.md`).

```bash
git tag v1.0.13
git push origin v1.0.13
```

**Overlay install:** from v1.0.5 onward, debug/release share a fixed project keystore (`keystore/pornweb.jks`). Upgrading from ≤1.0.4 requires one uninstall first.

Optional CI secrets for a custom keystore: `SIGNING_STORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

---

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) (Chinese release notes per version).

---

## License / 许可证

MIT
