# Universal Build Studio

A phone-based APK builder using your own GitHub Actions repository. The app uploads a complete project, builds it remotely and downloads the APK. No paid AI API is required. GitHub runner/storage quotas still apply; public input releases are public until cleanup.

## Supported projects

| Input | Requirements | Output |
|---|---|---|
| Pasted HTML or `.html` / `.htm` | Self-contained HTML, or ZIP with its CSS/JS/images | Android WebView APK |
| Static web ZIP | `index.html` at a clear project root | Android WebView APK |
| React / Vite / compatible Node web ZIP | `package.json`, build script producing `dist/index.html` or `build/index.html` | Android WebView APK |
| Android Java/Kotlin project ZIP | Complete Gradle project with one application output | Project debug APK |
| Flutter project ZIP | `pubspec.yaml`, `lib/main.dart`, `android/`, compatible SDK/plugins | Flutter debug APK |

A backend server, Python script, isolated Java/Dart file, Next.js SSR server, Unity or Godot project is not automatically converted. The builder does not design the application: provide the application's complete source and assets. Native Android and Flutter projects keep their own name, icon, package ID and version settings.

## Setup

Install the newly built Universal Build Studio APK from Actions. Enter your GitHub username, repository and a repository-scoped token in the app's GitHub settings. It needs repository Contents read/write (temporary input releases), Actions read/write (dispatch and artifacts), and Metadata read. Do not embed your token in projects or share it. Workflow changes may require additional GitHub app permissions.

Use `main` after the repair PR is merged. The app dispatches `build-generated-app.yml` on `main`.

Choose a complete ZIP or paste HTML; enter the app name and package ID. For Web APKs, select only the capabilities your code uses. Camera/microphone/library permissions are currently requested on first launch for enabled capabilities; denied permissions are not silently granted. File pickers remain available without broad library access.

Public repositories expose their input release assets. Each new build has a unique release and run name. Its temporary release is deleted after the build; deletion cannot undo earlier public access. Only build source you trust. Never include credentials or private customer information in an input ZIP.

## Build tracking and storage

Each build uses a UUID instead of the latest unrelated run. WorkManager checks in short attempts and retries without re-uploading an already dispatched build. History stores the run link and supports CHECK STATUS / DOWNLOAD. Background scheduling is controlled by Android and may be delayed.

The UI prevents concurrent builds on one phone. A network failure during dispatch is reconciled by request ID; if no matching run appears, inspect Actions before starting a new request. Old runs made before this update are not imported into History.

Generated artifacts expire after one day. Export APKs to keep them. Only the latest verified APK is stored inside the builder; failed downloads preserve it. Exported files are not automatically deleted.

## Native audio for Web APKs

Enable **Media controls** and, for device music, **Media library**. The wrapper offers:

```javascript
// Returns JSON; permission may be denied, giving an empty list.
const songs = JSON.parse(NativeLibrary.getSongs());
if (songs.length) NativeMedia.play(songs[0].uri, songs[0].title, songs[0].artist);
// Playback runs in a native service, independently of the Activity.
NativeMedia.stop();
```

`NativeMedia.play` accepts `content://` and `https://` audio URIs. It does not support WebView `blob:` URLs. Playback has audio focus and headphone-disconnection handling. Device behavior after screen lock, process reclamation and Activity recreation still needs device validation. Force-stop terminates playback.

Existing HTML audio/video notification integration remains, but HTML playback is still tied to its WebView. Use the explicit native API for independent background audio. Automatic next/previous playlists are not provided by the native API.

Bundled Web content uses an HTTPS asset origin. External navigation opens in the browser; embedded frames/objects are blocked in the generated wrapper. Remote script dependencies still run as part of your app, so include only dependencies you trust. Native capabilities are not available inside the builder's simple preview.

## Signing and updates

Default output is a test/debug APK with the project's signature. A successful debug build does **not** guarantee that another build will install over an existing installation. For stable signing, configure these repository Actions secrets (never put them in source or an input ZIP):

- `APK_KEYSTORE_BASE64`: your backed-up signing keystore encoded as base64
- `APK_STORE_PASSWORD`
- `APK_KEY_ALIAS`
- `APK_KEY_PASSWORD`

The optional signing job executes on a separate runner, downloads only the output APK, aligns/signs/verifies it and replaces the output artifact. Uploaded project build commands never run in that job. Without all four values it clearly keeps the project/debug signature. This does not change a debug manifest into a production release configuration. Native release optimization/flavor builds remain project-specific.

Keep the same package ID and signing key for updates; increase the version code. Web APK version codes use the workflow run number. Existing APKs with a different signature cannot be updated using a new key. Release signing has not been tested with your private key because no private signing key was provided.

## Validation and limitations

- Python tests cover nested web imports, ambiguous roots, Flutter detection and ZIP traversal rejection.
- GitHub Actions compiles the builder, the generated Java wrapper and a Flutter sample.
- Preview and generated APK behavior differ for native APIs.
- Phone installation, runtime permissions, lock-screen playback, and signed updates require device checks; compilation alone is not proof of these behaviors.
- Optional future work: named project library, full in-app build logs/cancel controls, Godot export, native release/flavor selection, and preview parity.
