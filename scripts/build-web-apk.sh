#!/usr/bin/env bash
set -e

SOURCE_DIR="$1"
OUTPUT_DIR="$2"
ICON_FILE="${3:-}"

WRAPPER="$GITHUB_WORKSPACE/web-wrapper"

rm -rf "$WRAPPER"

mkdir -p "$WRAPPER/app/src/main/java/com/generated/webapp"
mkdir -p "$WRAPPER/app/src/main/assets/www"
mkdir -p "$WRAPPER/app/src/main/res/drawable"
mkdir -p "$OUTPUT_DIR"

WEB_DIR="$SOURCE_DIR"

if [ -f "$SOURCE_DIR/package.json" ]; then
    cd "$SOURCE_DIR"

    npm install --no-audit --no-fund
    npm run build --if-present

    if [ -d "$SOURCE_DIR/dist" ]; then
        WEB_DIR="$SOURCE_DIR/dist"
    elif [ -d "$SOURCE_DIR/build" ]; then
        WEB_DIR="$SOURCE_DIR/build"
    fi
fi

if [ ! -f "$WEB_DIR/index.html" ]; then
    echo "index.html not found"
    exit 1
fi

cp -R "$WEB_DIR"/. \
"$WRAPPER/app/src/main/assets/www/"

HAS_ICON="false"

if [ -n "$ICON_FILE" ] && [ -f "$ICON_FILE" ]; then

    cp "$ICON_FILE" \
    "$WRAPPER/app/src/main/res/drawable/app_icon.png"

    HAS_ICON="true"

    echo "Custom app icon added"
else
    echo "No custom icon found"
fi

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

cat > "$WRAPPER/build.gradle" <<'EOF'
plugins {
    id 'com.android.application' version '8.7.3' apply false
}
EOF

cat > "$WRAPPER/app/build.gradle" <<'EOF'
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.generated.webapp'
    compileSdk 35

    defaultConfig {
        applicationId 'com.generated.webapp'
        minSdk 24
        targetSdk 35
        versionCode 1
        versionName '1.0'
    }
}
EOF

if [ "$HAS_ICON" = "true" ]; then

cat > "$WRAPPER/app/src/main/AndroidManifest.xml" <<'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <uses-permission android:name="android.permission.CAMERA" />

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <application
        android:label="Generated App"
        android:icon="@drawable/app_icon"
        android:roundIcon="@drawable/app_icon"
        android:usesCleartextTraffic="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">

            <intent-filter>

                <action
                    android:name="android.intent.action.MAIN" />

                <category
                    android:name="android.intent.category.LAUNCHER" />

            </intent-filter>

        </activity>

    </application>

</manifest>
EOF

else

cat > "$WRAPPER/app/src/main/AndroidManifest.xml" <<'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <uses-permission android:name="android.permission.CAMERA" />

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <application
        android:label="Generated App"
        android:usesCleartextTraffic="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">

            <intent-filter>

                <action
                    android:name="android.intent.action.MAIN" />

                <category
                    android:name="android.intent.category.LAUNCHER" />

            </intent-filter>

        </activity>

    </application>

</manifest>
EOF

fi

cat > "$WRAPPER/app/src/main/java/com/generated/webapp/MainActivity.java" <<'EOF'
package com.generated.webapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]>
        filePathCallback;

    private static final int
        FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(
        Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings =
            webView.getSettings();

        settings.setJavaScriptEnabled(true);

        settings.setDomStorageEnabled(true);

        settings.setAllowFileAccess(true);

        settings.setAllowContentAccess(true);

        settings.setMediaPlaybackRequiresUserGesture(
            false
        );

        webView.setWebViewClient(
            new WebViewClient()
        );

        webView.setWebChromeClient(
            new WebChromeClient() {

                @Override
                public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]>
                        filePathCallbackParam,
                    FileChooserParams
                        fileChooserParams
                ) {

                    if (
                        filePathCallback != null
                    ) {
                        filePathCallback
                            .onReceiveValue(null);
                    }

                    filePathCallback =
                        filePathCallbackParam;

                    try {

                        Intent intent =
                            fileChooserParams
                                .createIntent();

                        intent.addCategory(
                            Intent.CATEGORY_OPENABLE
                        );

                        startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                        );

                    } catch (Exception e) {

                        filePathCallback
                            .onReceiveValue(null);

                        filePathCallback = null;

                        return false;
                    }

                    return true;
                }
            }
        );

        webView.loadUrl(
            "file:///android_asset/www/index.html"
        );
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        );

        if (
            requestCode ==
            FILE_CHOOSER_REQUEST
        ) {

            if (
                filePathCallback == null
            ) {
                return;
            }

            Uri[] results =
                WebChromeClient
                    .FileChooserParams
                    .parseResult(
                        resultCode,
                        data
                    );

            filePathCallback
                .onReceiveValue(results);

            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (
            webView != null &&
            webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
EOF

cd "$WRAPPER"

gradle :app:assembleDebug \
    --stacktrace

APK="$WRAPPER/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then

    echo "APK was not created"

    exit 1
fi

cp "$APK" \
"$OUTPUT_DIR/generated-app.apk"

echo "WEB APK CREATED"
