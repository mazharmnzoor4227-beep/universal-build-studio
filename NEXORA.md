# NEXORA AI

NEXORA is a separate native Android project inside `universal-build-studio`. It is not a WebView chatbot: the main interface is Jetpack Compose and the generated-site preview is the only isolated WebView surface.

## Included in the first functional build

- Separate conversations backed by Room: create, search, rename, pin, duplicate and delete.
- Streaming responses for OpenAI-compatible, Gemini and Anthropic protocols.
- Provider profiles encrypted with Android Keystore-backed AES-GCM. Keys are never committed or exported.
- OpenRouter `openrouter/free`, Google AI Studio, Groq, DeepSeek, Mistral, xAI, Anthropic and custom HTTPS profiles.
- Optional Puter.js free connector in a dedicated WebView bridge. It requires internet access and follows Puter's own sign-in, rate-limit and data terms.
- Voice typing with editable transcript, full-screen voice workspace, Android TTS replies and saved voice transcripts.
- General, Code, Website, Android app and Document workspace modes.
- Project parser, local project library, HTML preview and ZIP export.
- GitHub Actions workflow that compiles the NEXORA source into a debug APK. The app does not pretend that an AI response alone compiled an APK.

## Build with GitHub

Open the repository's **Actions → Build NEXORA AI → Run workflow**. The workflow uses Java 17 and Gradle 8.11.1, then publishes:

1. `app-debug.apk` as an Actions artifact.
2. `nexora-source.zip` as an Actions artifact.
3. A preview GitHub Release for builds from `main`.

The same workflow runs automatically when `nexora/**` changes on `main`.

## Provider setup

Go to **Settings → Add provider or free API**. For a normal OpenAI-compatible service, choose a preset or Custom, enter an HTTPS base URL, API key and model ID, then use **Test connection**. Examples:

- OpenRouter: `https://openrouter.ai/api/v1`, model `openrouter/free`.
- Groq: `https://api.groq.com/openai/v1`.
- DeepSeek: `https://api.deepseek.com`.

Free access is controlled by each provider and can change. The app does not promise unlimited use or silently fall back to a paid provider.

## Security notes

- No API key is in source, Gradle files, logs or GitHub Actions.
- Keys are encrypted locally using an Android Keystore AES-GCM key.
- Provider requests go directly to the selected HTTPS endpoint.
- Generated ZIP files are kept separate from provider profiles and never include credentials.
- Puter is intentionally opt-in because it is a hosted web connector rather than a conventional API-key service.

## Known boundaries

The initial build generates Android source projects and exports them. Compiling arbitrary generated Android projects is a separate build-service problem; the repository's GitHub Actions workflows can be extended for that later. Website preview works best when the selected model returns a complete `index.html` with inline or project-relative assets.
