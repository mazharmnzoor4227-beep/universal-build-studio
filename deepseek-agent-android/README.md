# Agent Starter Android

This Android starter was reconstructed from the DeepSeek shared conversation supplied by the user. The share contained only the `index.html` UI shell; the referenced `styles.css`, `app.js`, and backend provider code were not included, so those missing pieces were implemented here to make the project buildable and usable as a starter APK.

## What works

- Minimal chat UI based on the shared HTML
- One settings field: API key
- Provider detection from recognizable key prefixes
- Native Android network bridge (no browser CORS dependency)
- Conversation history in the current session
- OpenAI Responses API with web search
- DeepSeek Responses API with web search
- Anthropic Messages API
- Gemini generateContent API
- OpenRouter chat completions with automatic model routing
- API key is entered by the user at runtime and is not committed to source

## Provider routing

- `sk-proj-` / `sk-svcacct-` -> OpenAI (`gpt-5.6-luna`)
- `sk-ant-` -> Anthropic (`claude-sonnet-5`)
- `sk-or-` -> OpenRouter (`openrouter/auto`)
- `AIza...` -> Gemini (`gemini-2.5-flash`)
- other `sk-...` -> DeepSeek (`deepseek-v4-flash`)

Note: generic `sk-` keys are not globally unique across every AI provider, so perfect provider detection from a key alone is impossible. This starter intentionally does not send an unknown key to multiple providers just to probe it.

## Build

Requires JDK 17, Gradle 8.11.1, Android SDK 35.

```bash
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
