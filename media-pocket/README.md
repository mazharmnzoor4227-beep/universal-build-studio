# Media Pocket
Native Android downloader for public permitted media, with a separate package from Universal Build Studio. No paid API or central download server. Android 10+, ARM64.

## Features
Paste/share link input; background worker; quality preference; cancellation; Gallery save; local history; open/share saved media; manually update yt-dlp.

Video extraction uses youtubedl-android 0.18.1 / yt-dlp and FFmpeg. Supports routes for Instagram, Facebook, TikTok, Pinterest and YouTube, but site availability requires testing with actual links and changes over time. No cookies, authentication, private access or age-gate bypass. No added watermark, and no removal of creator marks. A watermark-free stream is not always available.

Photo extraction currently reads public JSON-LD ImageObject content URLs. It does not substitute video thumbnails. Instagram carousels, TikTok slideshows and pages without exposed image metadata are not guaranteed; the app reports unsupported rather than saving a thumbnail as the original. This is a material limitation, not universal photo support.

Downloads are written to MediaStore Movies/MediaPocket or Pictures/MediaPocket. Only current temporary files are deleted; user downloads persist. No analytics, cloud account or paid API. Websites see requests/IP. No file upscaling. Platform compression remains.

## Build
Java 17, Android SDK 35, Gradle 8.11.1. Run `gradle :app:testDebugUnitTest :app:assembleDebug` in this directory. ARM64 APK is debug-signed. Production signing, store review and physical device checks are not complete.

## Licensing
Application source is GPL-3.0-or-later. youtubedl-android is GPL-3.0; yt-dlp and FFmpeg have their own licenses and bundled component notices. Corresponding application source ships with releases. See https://github.com/yausername/youtubedl-android and https://github.com/yt-dlp/yt-dlp and https://ffmpeg.org/legal.html for upstream source and licensing. Preserve notices when redistributing.
