package com.generated.webapp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]>
        fileCallback;

    private Uri cameraUri;

    private static final int
        FILE_REQUEST = 1001;

    private static final int
        PERMISSION_REQUEST = 2001;

    private final BroadcastReceiver
        mediaReceiver =
        new BroadcastReceiver() {

            @Override
            public void onReceive(
                Context context,
                Intent intent
            ) {

                String action =
                    intent.getAction();

                if (
                    MediaPlaybackService.ACTION_PLAY
                        .equals(action)
                ) {

                    runJs(
                        "var m=document.querySelector('audio,video');" +
                        "if(m){m.play();}"
                    );

                } else if (
                    MediaPlaybackService.ACTION_PAUSE
                        .equals(action)
                ) {

                    runJs(
                        "var m=document.querySelector('audio,video');" +
                        "if(m){m.pause();}"
                    );

                } else if (
                    MediaPlaybackService.ACTION_NEXT
                        .equals(action)
                ) {

                    runJs(
                        "window.dispatchEvent(new CustomEvent(" +
                        "'native-media-next'));" +
                        "var b=document.querySelector(" +
                        "'[data-media-next],.next,.next-button');" +
                        "if(b){b.click();}"
                    );

                } else if (
                    MediaPlaybackService.ACTION_PREVIOUS
                        .equals(action)
                ) {

                    runJs(
                        "window.dispatchEvent(new CustomEvent(" +
                        "'native-media-previous'));" +
                        "var b=document.querySelector(" +
                        "'[data-media-previous],.previous," +
                        ".prev,.prev-button');" +
                        "if(b){b.click();}"
                    );
                }
            }
        };

    @Override
    protected void onCreate(
        Bundle savedInstanceState
    ) {

        super.onCreate(
            savedInstanceState
        );

        requestPermissionsNeeded();

        registerMediaReceiver();

        webView =
            new WebView(this);

        setContentView(
            webView
        );

        WebSettings settings =
            webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);

        settings.setMediaPlaybackRequiresUserGesture(
            false
        );

        webView.addJavascriptInterface(
            new NativeMediaBridge(),
            "NativeMedia"
        );

        webView.setWebViewClient(
            new WebViewClient() {

                @Override
                public void onPageFinished(
                    WebView view,
                    String url
                ) {

                    super.onPageFinished(
                        view,
                        url
                    );

                    installMediaBridge();
                }
            }
        );

        webView.setWebChromeClient(
            new WebChromeClient() {

                @Override
                public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params
                ) {

                    if (fileCallback != null) {
                        fileCallback.onReceiveValue(
                            null
                        );
                    }

                    fileCallback =
                        callback;

                    openFileChooser(
                        params
                    );

                    return true;
                }

                @Override
                public void onPermissionRequest(
                    PermissionRequest request
                ) {

                    runOnUiThread(
                        () -> request.grant(
                            request.getResources()
                        )
                    );
                }
            }
        );

        webView.loadUrl(
            "file:///android_asset/www/index.html"
        );
    }

    private void openFileChooser(
        WebChromeClient.FileChooserParams params
    ) {

        Intent fileIntent;

        try {

            fileIntent =
                params.createIntent();

        } catch (Exception e) {

            fileIntent =
                new Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                );

            fileIntent.addCategory(
                Intent.CATEGORY_OPENABLE
            );

            fileIntent.setType(
                "*/*"
            );
        }

        fileIntent.putExtra(
            Intent.EXTRA_ALLOW_MULTIPLE,
            true
        );

        Intent cameraIntent =
            new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            );

        cameraUri =
            createCameraUri();

        if (cameraUri != null) {

            cameraIntent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                cameraUri
            );

            cameraIntent.addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    |
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        }

        Intent chooser =
            Intent.createChooser(
                fileIntent,
                "Select file or camera"
            );

        if (
            cameraIntent.resolveActivity(
                getPackageManager()
            ) != null
        ) {

            chooser.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                new Intent[]{
                    cameraIntent
                }
            );
        }

        try {

            startActivityForResult(
                chooser,
                FILE_REQUEST
            );

        } catch (Exception e) {

            fileCallback.onReceiveValue(
                null
            );

            fileCallback = null;
        }
    }

    private Uri createCameraUri() {

        try {

            ContentValues values =
                new ContentValues();

            values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "camera_" +
                    System.currentTimeMillis() +
                    ".jpg"
            );

            values.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            );

            if (Build.VERSION.SDK_INT >= 29) {

                values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/GeneratedApps"
                );
            }

            return getContentResolver()
                .insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                );

        } catch (Exception e) {

            return null;
        }
    }

    private void registerMediaReceiver() {

        IntentFilter filter =
            new IntentFilter();

        filter.addAction(
            MediaPlaybackService.ACTION_PLAY
        );

        filter.addAction(
            MediaPlaybackService.ACTION_PAUSE
        );

        filter.addAction(
            MediaPlaybackService.ACTION_NEXT
        );

        filter.addAction(
            MediaPlaybackService.ACTION_PREVIOUS
        );

        if (Build.VERSION.SDK_INT >= 33) {

            registerReceiver(
                mediaReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            );

        } else {

            registerReceiver(
                mediaReceiver,
                filter
            );
        }
    }

    private void installMediaBridge() {

        runJs(
            "(function(){" +
            "if(window.__nativeMediaReady)return;" +
            "window.__nativeMediaReady=true;" +

            "function update(m,p){" +

            "var title=" +
            "m.dataset.title||" +
            "m.getAttribute('title')||" +
            "document.title||'Now Playing';" +

            "var artist=" +
            "m.dataset.artist||" +
            "'Media Player';" +

            "if(window.NativeMedia){" +
            "NativeMedia.updateMedia(" +
            "title,artist,p);" +
            "}" +

            "}" +

            "document.addEventListener(" +
            "'play',function(e){" +
            "if(e.target.matches('audio,video')){" +
            "update(e.target,true);" +
            "}},true);" +

            "document.addEventListener(" +
            "'pause',function(e){" +
            "if(e.target.matches('audio,video')){" +
            "update(e.target,false);" +
            "}},true);" +

            "document.addEventListener(" +
            "'ended',function(e){" +
            "if(e.target.matches('audio,video')){" +
            "update(e.target,false);" +
            "}},true);" +

            "})();"
        );
    }

    private void runJs(
        String code
    ) {

        if (webView == null) {
            return;
        }

        webView.post(
            () ->
                webView.evaluateJavascript(
                    code,
                    null
                )
        );
    }

    public class NativeMediaBridge {

        @JavascriptInterface
        public void updateMedia(
            String title,
            String artist,
            boolean playing
        ) {

            Intent intent =
                new Intent(
                    MainActivity.this,
                    MediaPlaybackService.class
                );

            intent.setAction(
                MediaPlaybackService.ACTION_UPDATE
            );

            intent.putExtra(
                MediaPlaybackService.EXTRA_TITLE,
                title
            );

            intent.putExtra(
                MediaPlaybackService.EXTRA_ARTIST,
                artist
            );

            intent.putExtra(
                MediaPlaybackService.EXTRA_PLAYING,
                playing
            );

            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(
                    intent
                );

            } else {

                startService(
                    intent
                );
            }
        }
    }

    private void requestPermissionsNeeded() {

        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        java.util.ArrayList<String> list =
            new java.util.ArrayList<>();

        if (
            checkSelfPermission(
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            list.add(
                Manifest.permission.CAMERA
            );
        }

        if (
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            list.add(
                Manifest.permission.RECORD_AUDIO
            );
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            list.add(
                Manifest.permission.POST_NOTIFICATIONS
            );
        }

        if (!list.isEmpty()) {

            requestPermissions(
                list.toArray(
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
            requestCode != FILE_REQUEST ||
            fileCallback == null
        ) {
            return;
        }

        Uri[] result = null;

        if (resultCode == RESULT_OK) {

            if (
                data != null &&
                data.getClipData() != null
            ) {

                int count =
                    data.getClipData()
                        .getItemCount();

                result =
                    new Uri[count];

                for (
                    int i = 0;
                    i < count;
                    i++
                ) {

                    result[i] =
                        data.getClipData()
                            .getItemAt(i)
                            .getUri();
                }

            } else if (
                data != null &&
                data.getData() != null
            ) {

                result =
                    new Uri[]{
                        data.getData()
                    };

            } else if (
                cameraUri != null
            ) {

                result =
                    new Uri[]{
                        cameraUri
                    };
            }
        }

        fileCallback.onReceiveValue(
            result
        );

        fileCallback = null;
        cameraUri = null;
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

    @Override
    protected void onDestroy() {

        try {

            unregisterReceiver(
                mediaReceiver
            );

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}
