# Media Pocket 2.0

Native Android downloader for public downloadable media. Android 10+ (`minSdk 29`).

## Highlights
- Premium dark blue/purple Compose UI with a new launcher icon.
- Video quality choices: Best/Original, 4K (2160p), 2K (1440p), 1080p, 720p and 480p.
- No artificial upscaling. Best/Original requests the highest source streams available.
- Separate video/audio streams are merged by FFmpeg when required without intentional re-encoding.
- Universal ABI target: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`.
- Android MediaStore saving; no broad storage permission required on Android 10+.
- WorkManager background download, cancellation, retry-oriented yt-dlp options, Wi-Fi/mobile-data support.
- Saved-download history, open/share, clipboard paste and engine update control.
- Public routes for Instagram, Facebook, TikTok, Pinterest and YouTube.

Availability is controlled by each website and can change. Private/login-only/age-restricted content is not bypassed. The app does not remove creator watermarks and does not invent 4K resolution when a source does not provide it.

## Build
Java 17, Android SDK 35, Gradle 8.11.1.

`gradle :app:testDebugUnitTest :app:assembleDebug`

## Licensing
Application source is GPL-3.0-or-later. youtubedl-android, yt-dlp, FFmpeg and gallery-dl retain their respective licenses and notices. Preserve those notices when redistributing.
