# CYBER_CORE
A self-contained index.html with embedded CSS/JS. No CDN dependencies. Open in a modern browser; HTTPS is recommended for microphone permissions. This is a web application, not a native speech implementation.

Features: responsive chat and drawer; provider detection with model discovery and verification; persistent local conversation; optional plain localStorage key retention; Gemini search toggle and actual grounding sources; generated HTML sandbox and export; animated voice face/rings and live microphone input analysis; Urdu TTS when a voice exists; foreground turn-taking recognition with English fallback only for an explicit language-not-supported error; typed input during calls; GitHub new-repository creation and atomic Capacitor workflow commit.

Differences from requested obsolete defaults: model discovery replaces Gemini 1.5 Pro and Claude 3.5 Sonnet hardcoding. OpenRouter selects a free text model instead of silently choosing a paid model. OpenAI generic sk- keys require confirmation of the destination; unknown/custom key destinations are not guessed. The search toggle does not claim all free accounts have search access. No search sources means no claim of successful live research.

Limitations: browser/WebView speech and Urdu voice support vary. A native speech plugin is required on WebViews without Web Speech recognition. This file does not implement a native plugin. Thinking/speaking visualizations are decorative animation, not TTS frequency measurements. Microphone rings use measured input when permitted. API requests may be blocked by browser CORS. A key alone does not make every provider compatible. There is no universal image/video generation, native code executor, background agent or direct Shopify publishing.

Build modal uploads only the generated project, package.json, Capacitor config and workflow, never the agent's key or history. Uses pinned Capacitor 8.0.0 / Node 22 / Java 21. No repo overwrites. Token needs repository creation, Contents and Workflows access. Artifact download is through GitHub's authenticated Actions page. Minute/storage limits apply. Debug signing is for tests, not reliable install-over updates.

Validation: browser fixtures test simulated model discovery, successful chat, HTML preview, HTTP 429 recovery and unavailable recognition. These are not live provider tests. API-account quota, real Urdu voice and GitHub PAT repository creation require actual account/device validation. Do not label the result bug-free or production-certified.

## Android Cyber Core
`android-app` wraps the same interface in a separate `com.mazhar.cybercore` application. Build Cyber Core APK publishes an installable personal-test debug APK and source archive. No API credentials are embedded. It does not replace the existing Jarvis or builder modules.

The trusted top-level app has a restricted native message bridge for Android speech recognition, Urdu system TTS, HTTPS requests to the three supported AI providers and GitHub, and Downloads/CyberCore text exports. Opaque generated preview frames cannot use this bridge. Microphone access requires permission; Urdu speech requires installed speech services. Missing services report errors without terminating the app. Conversation is turn-taking while the app is foreground, not a realtime full-duplex calling service. Device/API runtime validation is still needed.
