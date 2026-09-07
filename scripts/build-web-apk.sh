#!/usr/bin/env bash

set -e

SOURCE_DIR="$1"
OUTPUT_DIR="$2"

if [ -z "$SOURCE_DIR" ] || [ -z "$OUTPUT_DIR" ]; then
  echo "Usage: build-web-apk.sh <source-dir> <output-dir>"
  exit 1
fi

SOURCE_DIR="$(cd "$SOURCE_DIR" && pwd)"

mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

WORK_DIR="$(mktemp -d)"
APP_DIR="$WORK_DIR/GeneratedWebApp"

mkdir -p "$APP_DIR/app/src/main/java/com/universal/generated"
mkdir -p "$APP_DIR/app/src/main/assets/www"
mkdir -p "$APP_DIR/app/src/main/res/values"

WEB_SOURCE="$SOURCE_DIR"

# Build React / Vite / Vue / Node web project first
if [ -f "$SOURCE_DIR/package.json" ]; then

  echo "Node/Web project detected."

  cd "$SOURCE_DIR"

  if [ -f package-lock.json ]; then
    npm ci
  else
    npm install
  fi

  npm run build

  if [ -d "$SOURCE_DIR/dist" ]; then
    WEB_SOURCE="$SOURCE_DIR/dist"

  elif [ -d "$SOURCE_DIR/build" ]; then
    WEB_SOURCE="$SOURCE_DIR/build"

  else
    echo "Build finished, but dist/ or build/ folder was not found."
    exit 1
  fi
fi

if [ ! -f "$WEB_SOURCE/index.html" ]; then
  echo "index.html not found."
  exit 1
fi

echo "Using web files from:"
echo "$WEB_SOURCE"

cp -R "$WEB_SOURCE"/. "$APP_DIR/app/src/main/assets/www/"

cat > "$APP_DIR/settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GeneratedWebApp"
include(":app")
EOF

cat > "$APP_DIR/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
EOF

cat > "$APP_DIR/app/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.universal.generated"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.universal.generated"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
EOF

cat > "$APP_DIR/app/src/main/AndroidManifest.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>

<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="Generated App"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:screenOrientation="sensor">

            <intent-filter>

                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />

            </intent-filter>

        </activity>

    </application>

</manifest>
EOF

cat > "$APP_DIR/app/src/main/res/values/styles.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>

<resources>

    <style
        name="AppTheme"
        parent="android:style/Theme.Material.NoActionBar">

        <item name="android:windowFullscreen">true</item>
        <item name="android:navigationBarColor">#000000</item>
        <item name="android:statusBarColor">#000000</item>

    </style>

</resources>
EOF

cat > "$APP_DIR/app/src/main/java/com/universal/generated/MainActivity.kt" <<'EOF'
package com.universal.generated

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        setContentView(webView)

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
        }

        webView.loadUrl(
            "file:///android_asset/www/index.html"
        )
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
EOF

cd "$APP_DIR"

echo "Building Android APK..."

gradle :app:assembleDebug --stacktrace

APK_PATH="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
  echo "APK was not generated."
  exit 1
fi

cp "$APK_PATH" "$OUTPUT_DIR/generated-app.apk"

echo "--------------------------------"
echo "WEB / GAME APK BUILD SUCCESS"
echo "--------------------------------"

ls -lh "$OUTPUT_DIR/generated-app.apk"
