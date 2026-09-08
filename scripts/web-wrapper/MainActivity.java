package com.generated.webapp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_REQUEST = 1001;
    private static final int PERMISSION_REQUEST = 2001;

    private final BroadcastReceiver mediaReceiver =
        new BroadcastReceiver() {

            @Override
            public void onReceive(
                Context context,
                Intent intent
            ) {

                String action = intent.getAction();

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
                        "window.dispatchEvent(" +
                        "new CustomEvent('native-media-next'));" +
                        "var b=document.querySelector(" +
                        "'[data-media-next],.next,.next-button');" +
                        "if(b){b.click();}"
                    );

                } else if (
                    MediaPlaybackService.ACTION_PREVIOUS
                        .equals(action)
                ) {

                    runJs(
                        "window.dispatchEvent(" +
                        "new CustomEvent('native-media-previous'));" +
                        "var b=document.querySelector(" +
                        "'[data-media-previous],.previous,.prev,.prev-button');" +
                        "if(b){b.click();}"
                    );
                }
            }
        };

    @Override
    protected void onCreate(
        Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        requestPermissionsNeeded();

        registerMediaReceiver();

        webView = new WebView(this);

        setContentView(webView);

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
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params
                ) {

                    if (fileCallback != null) {
                        fileCallback.onReceiveValue(null);
                    }

                    fileCallback = callback;

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
                            FILE_REQUEST
                        );

                    } catch (Exception e) {

                        Intent intent =
                            new Intent(
                                Intent.ACTION_OPEN_DOCUMENT
                            );

                        intent.addCategory(
                            Intent.CATEGORY_OPENABLE
                        );

                        intent.setType("*/*");

                        intent.putExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            true
                        );

                        startActivityForResult(
                            intent,
                            FILE_REQUEST
                        );
                    }

                    return true;
                }

                @Override
                public void onPermissionRequest(
                    final PermissionRequest request
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

            "function info(m,playing){" +
            "var title=m.dataset.title||" +
            "m.getAttribute('title')||" +
            "document.title||'Now Playing';" +

            "var artist=m.dataset.artist||" +
            "'Generated App';" +

            "if(window.NativeMedia){" +
            "NativeMedia.updateMedia(" +
            "title,artist,playing);" +
            "}" +
            "}" +

            "document.addEventListener(" +
            "'play',function(e){" +
            "if(e.target.matches('audio,video')){" +
            "info(e.target,true);" +
            "}},true);" +

            "document.addEventListener(" +
            "'pause',function(e){" +
            "if(e.target.matches('audio,video')){" +
            "info(e.target,false);" +
            "}},true);" +

            "document.addEventListener(" +
            "'ended',function(e){" +
            "if(e.target.matches('audio,video')){" +
            "info(e.target,false);" +
            "}},true);" +

            "})();"
        );
    }

    private void runJs(
        String javascript
    ) {

        if (webView == null) {
            return;
        }

        webView.post(
            () -> webView.evaluateJavascript(
                javascript,
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

                startForegroundService(intent);

            } else {

                startService(intent);
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

        if (Build.VERSION.SDK_INT >= 33) {

            if (
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                list.add(
                    Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }

        if (!list.isEmpty()) {

            requestPermissions(
                list.toArray(new String[0]),
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

                result = new Uri[count];

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
            }
        }

        fileCallback.onReceiveValue(
            result
        );

        fileCallback = null;
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
