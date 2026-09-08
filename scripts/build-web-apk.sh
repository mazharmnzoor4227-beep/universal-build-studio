#!/usr/bin/env bash
set -e

SOURCE_DIR="$1"
OUTPUT_DIR="$2"
ICON_FILE="${3:-}"

WRAPPER="$GITHUB_WORKSPACE/web-wrapper"

MEDIA_SERVICE_SOURCE="$GITHUB_WORKSPACE/scripts/web-wrapper/MediaPlaybackService.java"

rm -rf "$WRAPPER"

mkdir -p "$WRAPPER/app/src/main/java/com/generated/webapp"
mkdir -p "$WRAPPER/app/src/main/assets/www"
mkdir -p "$WRAPPER/app/src/main/res/drawable"
mkdir -p "$OUTPUT_DIR"

WEB_DIR="$SOURCE_DIR"

# --------------------------------------------------
# BUILD WEB / NODE PROJECT
# --------------------------------------------------

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

# --------------------------------------------------
# CUSTOM APP ICON
# --------------------------------------------------

HAS_ICON="false"

if [ -n "$ICON_FILE" ] && [ -f "$ICON_FILE" ]; then

    cp "$ICON_FILE" \
    "$WRAPPER/app/src/main/res/drawable/app_icon.png"

    HAS_ICON="true"

    echo "Custom app icon added"

else

    echo "No custom icon found"

fi

# --------------------------------------------------
# COPY MEDIA PLAYBACK SERVICE
# --------------------------------------------------

if [ -f "$MEDIA_SERVICE_SOURCE" ]; then

    cp "$MEDIA_SERVICE_SOURCE" \
    "$WRAPPER/app/src/main/java/com/generated/webapp/MediaPlaybackService.java"

    echo "MediaPlaybackService added"

else

    echo "MediaPlaybackService.java missing"

    exit 1
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

    compileOptions {

        sourceCompatibility JavaVersion.VERSION_17

        targetCompatibility JavaVersion.VERSION_17
    }
}
EOF

# --------------------------------------------------
# MANIFEST WITH ICON
# --------------------------------------------------

if [ "$HAS_ICON" = "true" ]; then

cat > "$WRAPPER/app/src/main/AndroidManifest.xml" <<'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

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
        android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_VIDEO" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_AUDIO" />

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

            android:foregroundServiceType="mediaPlayback" />

    </application>

</manifest>
EOF

else

# --------------------------------------------------
# MANIFEST WITHOUT ICON
# --------------------------------------------------

cat > "$WRAPPER/app/src/main/AndroidManifest.xml" <<'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

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
        android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_VIDEO" />

    <uses-permission
        android:name="android.permission.READ_MEDIA_AUDIO" />

    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <application

        android:label="Generated App"

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

            android:foregroundServiceType="mediaPlayback" />

    </application>

</manifest>
EOF

fi

# --------------------------------------------------
# MAIN ACTIVITY
# FILE / IMAGE / AUDIO / VIDEO PICKER
# --------------------------------------------------

cat > "$WRAPPER/app/src/main/java/com/generated/webapp/MainActivity.java" <<'EOF'
package com.generated.webapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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

    private static final int
            PERMISSION_REQUEST = 2001;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        requestRuntimePermissions();

        webView =
                new WebView(this);

        setContentView(
                webView
        );

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(
                true
        );

        settings.setDomStorageEnabled(
                true
        );

        settings.setDatabaseEnabled(
                true
        );

        settings.setAllowFileAccess(
                true
        );

        settings.setAllowContentAccess(
                true
        );

        settings.setMediaPlaybackRequiresUserGesture(
                false
        );

        settings.setLoadsImagesAutomatically(
                true
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
                                    callback,
                            FileChooserParams params
                    ) {

                        if (
                                filePathCallback != null
                        ) {

                            filePathCallback
                                    .onReceiveValue(null);
                        }

                        filePathCallback =
                                callback;

                        try {

                            Intent intent =
                                    params.createIntent();

                            intent.addCategory(
                                    Intent.CATEGORY_OPENABLE
                            );

                            intent.putExtra(
                                    Intent.EXTRA_ALLOW_MULTIPLE,
                                    true
                            );

                            startActivityForResult(
                                    intent,
                                    FILE_CHOOSER_REQUEST
                            );

                        } catch (Exception e) {

                            Intent fallback =
                                    new Intent(
                                            Intent.ACTION_OPEN_DOCUMENT
                                    );

                            fallback.addCategory(
                                    Intent.CATEGORY_OPENABLE
                            );

                            fallback.setType(
                                    "*/*"
                            );

                            fallback.putExtra(
                                    Intent.EXTRA_ALLOW_MULTIPLE,
                                    true
                            );

                            startActivityForResult(
                                    fallback,
                                    FILE_CHOOSER_REQUEST
                            );
                        }

                        return true;
                    }
                }
        );

        webView.loadUrl(
                "file:///android_asset/www/index.html"
        );
    }

    private void requestRuntimePermissions() {

        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        java.util.ArrayList<String>
                permissions =
                new java.util.ArrayList<>();

        if (
                checkSelfPermission(
                        Manifest.permission.CAMERA
                )
                        !=
                PackageManager.PERMISSION_GRANTED
        ) {

            permissions.add(
                    Manifest.permission.CAMERA
            );
        }

        if (
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                )
                        !=
                PackageManager.PERMISSION_GRANTED
        ) {

            permissions.add(
                    Manifest.permission.RECORD_AUDIO
            );
        }

        if (Build.VERSION.SDK_INT >= 33) {

            if (
                    checkSelfPermission(
                            Manifest.permission.POST_NOTIFICATIONS
                    )
                            !=
                    PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }

            if (
                    checkSelfPermission(
                            Manifest.permission.READ_MEDIA_IMAGES
                    )
                            !=
                    PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                        Manifest.permission.READ_MEDIA_IMAGES
                );
            }

            if (
                    checkSelfPermission(
                            Manifest.permission.READ_MEDIA_VIDEO
                    )
                            !=
                    PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                        Manifest.permission.READ_MEDIA_VIDEO
                );
            }

            if (
                    checkSelfPermission(
                            Manifest.permission.READ_MEDIA_AUDIO
                    )
                            !=
                    PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                        Manifest.permission.READ_MEDIA_AUDIO
                );
            }

        } else {

            if (
                    checkSelfPermission(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                            !=
                    PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                );
            }
        }

        if (!permissions.isEmpty()) {

            requestPermissions(
                    permissions.toArray(
                            new String[0]
                    ),
                    PERMISSION_REQUEST
            );
        }
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
                requestCode !=
                FILE_CHOOSER_REQUEST
        ) {

            return;
        }

        if (
                filePathCallback == null
        ) {

            return;
        }

        Uri[] results = null;

        if (
                resultCode ==
                RESULT_OK
        ) {

            if (
                    data != null &&
                    data.getClipData() != null
            ) {

                int count =
                        data.getClipData()
                                .getItemCount();

                results =
                        new Uri[count];

                for (
                        int i = 0;
                        i < count;
                        i++
                ) {

                    results[i] =
                            data.getClipData()
                                    .getItemAt(i)
                                    .getUri();
                }

            } else if (
                    data != null &&
                    data.getData() != null
            ) {

                results =
                        new Uri[]{
                                data.getData()
                        };
            }
        }

        filePathCallback
                .onReceiveValue(
                        results
                );

        filePathCallback = null;
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

# --------------------------------------------------
# BUILD APK
# --------------------------------------------------

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
