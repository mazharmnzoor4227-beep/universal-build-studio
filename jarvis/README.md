# Jarvis Android preview
Personal work assistant, Android 10+, Java 17 / Gradle 8.11.1 / SDK 35.

## Implemented
- Urdu turn-taking voice session: tap Start once, recognition → API → Urdu TTS → recognition. Visible foreground only; ends on backgrounding. This is not full-duplex calling, telephony or an always-on wake word. Requires installed speech recognition and Urdu TTS support.
- Encrypted device-local API connections and recent conversation using Android Keystore AES-GCM. No embedded provider keys. Select a different saved connection manually when credits run out. No automatic cross-provider spending.
- OpenAI Chat Completions, Responses, Anthropic Messages and Gemini generateContent adapters. Presets: OpenCode Zen, OpenAI, Gemini, Groq, xAI, OpenRouter and custom HTTPS bases. Set the exact supported model and protocol from your provider's documentation. Compatibility is not universal.
- Product photo input (resized JPEG), text/listing responses. Requires a vision model. The current photo is sent on the next request; photos are not retained in conversation history.
- Create-mode complete HTML/CSS/JS generation, current project and previous revision, WebView preview without native bridges or file access, HTML export.
- GitHub dispatch and artifact download for web-app APKs (45 KB HTML limit), fixed template workflow, 1-day artifact retention. Requires user-provided GitHub token with Actions read/write and the workflow in that repo. Generated HTML is packaged as an asset, never executed on the CI host. Saved APKs stay in Downloads/Jarvis.

## Current limits
Image generation uses an explicit image-model ID with a Gemini-native or Images-compatible connection and saves a decoded image to Gallery. This may consume paid credits and requires model access.

Not unrestricted native app creation, autonomous arbitrary tool execution, live web search, Shopify publishing, full-duplex realtime voice or universal image generation. Model prose is not proof an action was executed. Native workspaces, additional image generation adapters, cloud hosting and direct store connectors are separate work.
Voice/provider/billing behavior must be verified on the user's phone and actual accounts. CI verifies compilation, protocol transformations, response parsing and builds the generated-app template. No claim of live API validation without credentials.
Free quotas are provider-defined and may change. OpenCode Zen includes paid models; do not assume every model is free. Connections keep projects when switching, but context is sent to the newly selected provider. Cloud AI requires internet.
Debug signing is for testing; CI runners may produce different debug keys. Establish a private stable signing key before relying on install-over updates or distribution. Do not delete an installed copy to update without exporting its work first.

## Build
Run `gradle :app:testDebugUnitTest :app:assembleDebug` inside jarvis. Workflows are `.github/workflows/build-jarvis.yml` and `.github/workflows/jarvis-project.yml`.
