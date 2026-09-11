# NEXORA AI Android project

Open this directory as its own Gradle project in Android Studio or let GitHub Actions build it. Package: `com.mazhar.nexora`.

Architecture:

- `NexoraViewModel.kt` coordinates local state, Room, providers and project creation.
- `NexoraDatabase.kt` stores conversations, messages, projects and files.
- `SecureVault.kt` encrypts provider metadata and keys with Android Keystore.
- `ProviderEngine.kt` contains provider-specific request/response adapters.
- `PuterBridge.kt` is an opt-in hosted SDK bridge and is not used unless the user selects Puter.
- `VoiceController.kt` abstracts Android speech recognition and TTS.
- `ProjectTools.kt` parses generated files, previews HTML and exports ZIPs.
- `NexoraUi.kt` contains the responsive Compose workspace, history drawer, projects and settings.

The app needs a user-supplied provider key for normal API providers. No key is built into the project.
