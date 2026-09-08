package com.generated.webapp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private JSONObject builderOptions = new JSONObject();
    private boolean option(String name) { return builderOptions.optBoolean(name, false); }
    private static final String APP_ORIGIN = "https://appassets.androidplatform.net";


    private WebView webView;

    private ValueCallback<Uri[]>
        fileCallback;

    private Uri cameraUri;

    private static final int
        FILE_REQUEST = 1001;

    private static final int
        MEDIA_PERMISSION_REQUEST = 2001;

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
                        "var m=window.__activeNativeMedia;" +
                        "if(!m)m=document.querySelector('audio,video');" +
                        "if(m)m.play();"
                    );

                } else if (
                    MediaPlaybackService.ACTION_PAUSE
                        .equals(action)
                ) {

                    runJs(
                        "var m=window.__activeNativeMedia||document.querySelector('audio,video');" +
                        "if(m)m.pause();"
                    );

                } else if (
                    MediaPlaybackService.ACTION_NEXT
                        .equals(action)
                ) {

                    runJs(
                        "window.dispatchEvent(" +
                        "new CustomEvent('native-media-next'));"
                    );

                } else if (
                    MediaPlaybackService.ACTION_PREVIOUS
                        .equals(action)
                ) {

                    runJs(
                        "window.dispatchEvent(" +
                        "new CustomEvent('native-media-previous'));"
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

        webView =
            new WebView(this);

        setContentView(
            webView
        );

        try (java.io.InputStream stream = getAssets().open("builder-options.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1024]; int n;
            while ((n = stream.read(buf)) != -1) out.write(buf, 0, n);
            builderOptions = new JSONObject(out.toString("UTF-8"));
        } catch (Exception ignored) { }
        if (option("fullscreen")) getWindow().setFlags(1024, 1024);
        configureWebView();

        registerMediaReceiver();

        requestMediaPermissions();

        webView.loadUrl(
            APP_ORIGIN + "/assets/www/index.html"
        );
    }

    private void configureWebView() {

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

        if (option("media")) webView.addJavascriptInterface(
            new NativeMediaBridge(),
            "NativeMedia"
        );

        if (option("library")) webView.addJavascriptInterface(
            new NativeLibraryBridge(),
            "NativeLibrary"
        );

        final androidx.webkit.WebViewAssetLoader loader = new androidx.webkit.WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new androidx.webkit.WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView.setWebViewClient(
            new WebViewClient() {
                @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                    android.webkit.WebResourceResponse response = loader.shouldInterceptRequest(request.getUrl());
                    if (response != null) {
                        java.util.Map<String, String> headers = new java.util.HashMap<>();
                        headers.put("Content-Security-Policy", "frame-src 'none'; object-src 'none'");
                        response.setResponseHeaders(headers);
                    }
                    return response;
                }
                @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    if ("https".equals(uri.getScheme()) && "appassets.androidplatform.net".equals(uri.getHost()) && (uri.getPort() == -1 || uri.getPort() == 443)) return false;
                    if (request.isForMainFrame() && java.util.Arrays.asList("https", "http", "mailto", "tel", "sms").contains(uri.getScheme())) {
                        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                    }
                    return true;
                }


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

                    runJs(
                        "window.dispatchEvent(" +
                        "new CustomEvent('native-library-ready'));"
                    );
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
    }

    private void requestMediaPermissions() {

        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        ArrayList<String> list =
            new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 33) {

            if (
                checkSelfPermission(
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                list.add(
                    Manifest.permission.READ_MEDIA_AUDIO
                );
            }

            if (
                checkSelfPermission(
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                list.add(
                    Manifest.permission.READ_MEDIA_IMAGES
                );
            }

            if (
                checkSelfPermission(
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                list.add(
                    Manifest.permission.READ_MEDIA_VIDEO
                );
            }

            if (
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                list.add(
                    Manifest.permission.POST_NOTIFICATIONS
                );
            }

        } else {

            if (
                checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                list.add(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                );
            }
        }

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

        list.removeIf(permission ->
            (permission.equals(Manifest.permission.CAMERA) && !option("camera")) ||
            (permission.equals(Manifest.permission.RECORD_AUDIO) && !option("microphone")) ||
            (permission.equals(Manifest.permission.POST_NOTIFICATIONS) && !option("media")) ||
            (permission.startsWith("android.permission.READ_") && !option("library")));
        if (!list.isEmpty()) {

            requestPermissions(
                list.toArray(
                    new String[0]
                ),
                MEDIA_PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        );

        if (
            requestCode ==
            MEDIA_PERMISSION_REQUEST
        ) {

            runJs(
                "window.dispatchEvent(" +
                "new CustomEvent('native-library-ready'));"
            );
        }
    }

    public class NativeLibraryBridge {

        @JavascriptInterface
        public String getSongs() {

            return queryAudio()
                .toString();
        }

        @JavascriptInterface
        public String getVideos() {

            return queryVideos()
                .toString();
        }

        @JavascriptInterface
        public String getImages() {

            return queryImages()
                .toString();
        }

        @JavascriptInterface
        public boolean hasMusicPermission() {

            if (Build.VERSION.SDK_INT >= 33) {

                return checkSelfPermission(
                    Manifest.permission.READ_MEDIA_AUDIO
                ) ==
                PackageManager.PERMISSION_GRANTED;
            }

            return Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) ==
                PackageManager.PERMISSION_GRANTED;
        }
    }

    private JSONArray queryAudio() {

        JSONArray result =
            new JSONArray();

        Uri uri =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
        };

        String selection =
            MediaStore.Audio.Media.IS_MUSIC +
            " != 0";

        try (
            Cursor cursor =
                getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED +
                    " DESC"
                )
        ) {

            if (cursor == null) {
                return result;
            }

            int idCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                );

            int titleCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                );

            int artistCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                );

            int albumCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM
                );

            int durationCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DURATION
                );

            int nameCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DISPLAY_NAME
                );

            while (cursor.moveToNext()) {

                long id =
                    cursor.getLong(idCol);

                Uri contentUri =
                    Uri.withAppendedPath(
                        uri,
                        String.valueOf(id)
                    );

                JSONObject item =
                    new JSONObject();

                item.put(
                    "id",
                    id
                );

                item.put(
                    "title",
                    safe(
                        cursor.getString(
                            titleCol
                        )
                    )
                );

                item.put(
                    "artist",
                    safe(
                        cursor.getString(
                            artistCol
                        )
                    )
                );

                item.put(
                    "album",
                    safe(
                        cursor.getString(
                            albumCol
                        )
                    )
                );

                item.put(
                    "name",
                    safe(
                        cursor.getString(
                            nameCol
                        )
                    )
                );

                item.put(
                    "duration",
                    cursor.getLong(
                        durationCol
                    )
                );

                item.put(
                    "uri",
                    contentUri.toString()
                );

                result.put(item);
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    private JSONArray queryVideos() {

        JSONArray result =
            new JSONArray();

        Uri uri =
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        };

        try (
            Cursor cursor =
                getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    MediaStore.Video.Media.DATE_ADDED +
                    " DESC"
                )
        ) {

            if (cursor == null) {
                return result;
            }

            int idCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media._ID
                );

            int nameCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DISPLAY_NAME
                );

            int durationCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DURATION
                );

            while (cursor.moveToNext()) {

                long id =
                    cursor.getLong(idCol);

                JSONObject item =
                    new JSONObject();

                item.put(
                    "id",
                    id
                );

                item.put(
                    "name",
                    safe(
                        cursor.getString(
                            nameCol
                        )
                    )
                );

                item.put(
                    "duration",
                    cursor.getLong(
                        durationCol
                    )
                );

                item.put(
                    "uri",
                    Uri.withAppendedPath(
                        uri,
                        String.valueOf(id)
                    ).toString()
                );

                result.put(item);
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    private JSONArray queryImages() {

        JSONArray result =
            new JSONArray();

        Uri uri =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        };

        try (
            Cursor cursor =
                getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED +
                    " DESC"
                )
        ) {

            if (cursor == null) {
                return result;
            }

            int idCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media._ID
                );

            int nameCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.DISPLAY_NAME
                );

            while (cursor.moveToNext()) {

                long id =
                    cursor.getLong(idCol);

                JSONObject item =
                    new JSONObject();

                item.put(
                    "id",
                    id
                );

                item.put(
                    "name",
                    safe(
                        cursor.getString(
                            nameCol
                        )
                    )
                );

                item.put(
                    "uri",
                    Uri.withAppendedPath(
                        uri,
                        String.valueOf(id)
                    ).toString()
                );

                result.put(item);
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    private String safe(
        String value
    ) {

        if (value == null) {
            return "";
        }

        return value;
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
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        }

        Intent chooser =
            Intent.createChooser(
                fileIntent,
                "Choose media"
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
            "if(window.__mediaBridge)return;" +
            "window.__mediaBridge=true;" +

            "function update(m,p){window.__activeNativeMedia=m;" +

            "var t=" +
            "m.dataset.title||" +
            "document.title||" +
            "'Now Playing';" +

            "var a=" +
            "m.dataset.artist||" +
            "'Media Player';" +

            "if(window.NativeMedia){" +
            "NativeMedia.updateMedia(t,a,p);" +
            "}" +

            "}" +

            "document.addEventListener(" +
            "'play',function(e){" +

            "if(e.target.matches('audio,video')){" +
            "update(e.target,true);" +
            "}" +

            "},true);" +

            "document.addEventListener(" +
            "'pause',function(e){" +

            "if(e.target.matches('audio,video')){" +
            "update(e.target,false);" +
            "}" +

            "},true);" +

            "})();"
        );
    }

    public class NativeMediaBridge {
        @JavascriptInterface public void play(String source, String title, String artist) {
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, MediaPlaybackService.class);
                i.setAction(MediaPlaybackService.ACTION_LOAD);
                i.putExtra("source", source).putExtra(MediaPlaybackService.EXTRA_TITLE, title).putExtra(MediaPlaybackService.EXTRA_ARTIST, artist);
                try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
                catch (Exception ignored) { }
            });
        }
        @JavascriptInterface public void stop() {
            runOnUiThread(() -> stopService(new Intent(MainActivity.this, MediaPlaybackService.class)));
        }


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

        if (webView != null) {

            webView.destroy();
        }

        super.onDestroy();
    }
}
