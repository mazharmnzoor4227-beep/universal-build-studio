#!/usr/bin/env bash
set -euo pipefail

SOURCE_DIR="$1"
OUTPUT_DIR="$2"
ICON_FILE="${3:-}"
META_FILE="${4:-}"

WRAPPER="$GITHUB_WORKSPACE/web-wrapper"

MAIN_ACTIVITY_SOURCE="$GITHUB_WORKSPACE/scripts/web-wrapper/MainActivity.java"
MEDIA_SERVICE_SOURCE="$GITHUB_WORKSPACE/scripts/web-wrapper/MediaPlaybackService.java"

APP_NAME="Generated App"
PACKAGE_NAME="com.generated.webapp"

# --------------------------------------------------
# READ APP NAME + PACKAGE ID
# --------------------------------------------------

if [ -n "$META_FILE" ] && [ -f "$META_FILE" ]; then

    APP_NAME="$(
        python3 -c '
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    data = json.load(f)

print(data.get("appName", "Generated App"))
' "$META_FILE"
    )"

    PACKAGE_NAME="$(
        python3 -c '
import json
import sys

with open(sys.argv[1], encoding="utf-8") as f:
    data = json.load(f)

print(data.get("packageName", "com.generated.webapp"))
' "$META_FILE"
    )"
fi

# --------------------------------------------------
# CLEAN APP NAME
# --------------------------------------------------

APP_NAME="$(
python3 - "$APP_NAME" <<'PY'
import sys

value = sys.argv[1]

lines = [
    line.strip()
    for line in value.splitlines()
    if line.strip()
]

print(
    lines[0]
    if lines
    else "Generated App"
)
PY
)"

# --------------------------------------------------
# CLEAN PACKAGE ID
# --------------------------------------------------

PACKAGE_NAME="$(
python3 - "$PACKAGE_NAME" <<'PY'
import re
import sys

value = sys.argv[1]

match = re.fullmatch(
    r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+',
    value
)

if match:
    package = match.group(0)
else:
    sys.exit("Invalid package ID: expected com.example.myapp")

print(package.lower())
PY
)"

echo "APP NAME: $APP_NAME"
echo "PACKAGE: $PACKAGE_NAME"

# --------------------------------------------------
# CLEAN WRAPPER
# --------------------------------------------------

rm -rf "$WRAPPER"

mkdir -p \
"$WRAPPER/app/src/main/java/com/generated/webapp"

mkdir -p \
"$WRAPPER/app/src/main/assets/www"

mkdir -p \
"$WRAPPER/app/src/main/res/drawable"

mkdir -p \
"$WRAPPER/app/src/main/res/values"

mkdir -p "$OUTPUT_DIR"
printf 'android.useAndroidX=true\n' > "$WRAPPER/gradle.properties"

WEB_DIR="$SOURCE_DIR"

# --------------------------------------------------
# BUILD NODE / WEB PROJECT
# --------------------------------------------------

if [ -f "$SOURCE_DIR/package.json" ]; then

    cd "$SOURCE_DIR"

    if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; else npm install --no-audit --no-fund; fi

    npm run build \
        --if-present

    if [ -d "$SOURCE_DIR/dist" ]; then

        WEB_DIR="$SOURCE_DIR/dist"

    elif [ -d "$SOURCE_DIR/build" ]; then

        WEB_DIR="$SOURCE_DIR/build"

    else
        echo "Node web project must produce dist/ or build/ with index.html (static export). Backend-only and SSR projects are not APKs."; exit 1
    fi
fi

# --------------------------------------------------
# CHECK INDEX.HTML
# --------------------------------------------------

if [ ! -f "$WEB_DIR/index.html" ]; then

    echo "index.html not found"
    exit 1
fi

# --------------------------------------------------
# COPY WEB APP
# --------------------------------------------------

cp -R \
"$WEB_DIR"/. \
"$WRAPPER/app/src/main/assets/www/"

# --------------------------------------------------
# COPY JAVA TEMPLATES
# --------------------------------------------------

if [ ! -f "$MAIN_ACTIVITY_SOURCE" ]; then

    echo "MainActivity.java missing"
    exit 1
fi

if [ ! -f "$MEDIA_SERVICE_SOURCE" ]; then

    echo "MediaPlaybackService.java missing"
    exit 1
fi

cp \
"$MAIN_ACTIVITY_SOURCE" \
"$WRAPPER/app/src/main/java/com/generated/webapp/MainActivity.java"

cp \
"$MEDIA_SERVICE_SOURCE" \
"$WRAPPER/app/src/main/java/com/generated/webapp/MediaPlaybackService.java"

echo "MainActivity copied"
echo "MediaPlaybackService copied"

# --------------------------------------------------
# APP ICON
# --------------------------------------------------

HAS_ICON="false"

if [ -n "$ICON_FILE" ] && [ -f "$ICON_FILE" ]; then

    cp \
    "$ICON_FILE" \
    "$WRAPPER/app/src/main/res/drawable/app_icon.png"

    HAS_ICON="true"

    echo "Custom icon added"

else

    echo "No custom icon selected"
fi

# --------------------------------------------------
# SETTINGS.GRADLE
# --------------------------------------------------

cat > "$WRAPPER/settings.gradle" <<'EOF'

pluginManagement {

    repositories {

        google()

        mavenCentral()

        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )

    repositories {

        google()

        mavenCentral()
    }
}

rootProject.name = "GeneratedWebApp"

include(":app")

EOF

# --------------------------------------------------
# ROOT BUILD.GRADLE
# --------------------------------------------------

cat > "$WRAPPER/build.gradle" <<'EOF'

plugins {

    id 'com.android.application' version '8.7.3' apply false
}

EOF

# --------------------------------------------------
# APP BUILD.GRADLE
# --------------------------------------------------

cat > "$WRAPPER/app/build.gradle" <<EOF

plugins {

    id 'com.android.application'
}

dependencies { implementation 'androidx.webkit:webkit:1.12.1' }

android {

    namespace 'com.generated.webapp'

    compileSdk 35

    defaultConfig {

        applicationId '$PACKAGE_NAME'

        minSdk 24

        targetSdk 35

        versionCode ${GITHUB_RUN_NUMBER:-1}

        versionName '1.${GITHUB_RUN_NUMBER:-0}'
    }

    compileOptions {

        sourceCompatibility JavaVersion.VERSION_17

        targetCompatibility JavaVersion.VERSION_17
    }
}

EOF

# --------------------------------------------------
# APP NAME
# --------------------------------------------------

python3 - \
"$APP_NAME" \
"$WRAPPER/app/src/main/res/values/strings.xml" <<'PY'

import sys
from xml.sax.saxutils import escape

name = sys.argv[1]
path = sys.argv[2]

with open(
    path,
    "w",
    encoding="utf-8"
) as f:

    f.write(
        "<resources>\n"
    )

    f.write(
        '    <string name="app_name">'
        + escape(name.replace("\\", "\\\\").replace("\'", "\\\'").replace('"', '\\"'))
        + "</string>\n"
    )

    f.write(
        "</resources>\n"
    )

PY

# --------------------------------------------------
# ICON MANIFEST ATTRIBUTES
# --------------------------------------------------

if [ "$HAS_ICON" = "true" ]; then

    ICON_ATTRS='
        android:icon="@drawable/app_icon"
        android:roundIcon="@drawable/app_icon"'

else

    ICON_ATTRS=""
fi

# --------------------------------------------------
# ANDROID MANIFEST
# --------------------------------------------------

cat > "$WRAPPER/app/src/main/AndroidManifest.xml" <<EOF

<manifest
    xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission
        android:name="android.permission.INTERNET" />

    <uses-permission
        android:name="android.permission.CAMERA" />

    <uses-permission
        android:name="android.permission.RECORD_AUDIO" />

    <uses-permission
        android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-permission
        android:name="android.permission.FOREGROUND_SERVICE" />

    <uses-permission
        android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <uses-permission
        android:name="android.permission.WAKE_LOCK" />

    <uses-permission
        android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_VIDEO" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_AUDIO" />

    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <application

        android:label="@string/app_name"

        $ICON_ATTRS

        android:usesCleartextTraffic="true"

        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity

            android:name=".MainActivity"

            android:exported="true"

            android:launchMode="singleTop">

            <intent-filter>

                <action
                    android:name="android.intent.action.MAIN" />

                <category
                    android:name="android.intent.category.LAUNCHER" />

            </intent-filter>

        </activity>

        <service

            android:name=".MediaPlaybackService"

            android:exported="false"

            android:stopWithTask="false"

            android:foregroundServiceType="mediaPlayback" />

    </application>

</manifest>

EOF

# --------------------------------------------------
# BUILD APK
# --------------------------------------------------

python3 "$GITHUB_WORKSPACE/scripts/configure-web.py" "$WRAPPER" "${META_FILE:-/nonexistent}"

cd "$WRAPPER"

gradle \
:app:assembleDebug \
--stacktrace

APK="$WRAPPER/app/build/outputs/apk/debug/app-debug.apk"

# --------------------------------------------------
# VERIFY APK
# --------------------------------------------------

if [ ! -f "$APK" ]; then

    echo "APK was not created"
    exit 1
fi

# --------------------------------------------------
# COPY FINAL APK
# --------------------------------------------------

cp \
"$APK" \
"$OUTPUT_DIR/generated-app.apk"

echo "--------------------------------"
echo "WEB APK CREATED"
echo "APP: $APP_NAME"
echo "PACKAGE: $PACKAGE_NAME"
echo "--------------------------------"
