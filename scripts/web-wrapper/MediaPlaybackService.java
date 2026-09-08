package com.generated.webapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;

public class MediaPlaybackService extends Service {
    public static final String ACTION_LOAD = "com.generated.webapp.LOAD";
    public static final String ACTION_STOP = "com.generated.webapp.STOP";
    private android.media.MediaPlayer player;
    private boolean prepared;
    private android.media.AudioManager audioManager;
    private final android.media.AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        if (change < 0 && player != null && prepared) { player.pause(); setPlaying(false); }
    };
    private final android.content.BroadcastReceiver noisyReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, Intent i) {
            if (player != null && prepared) { player.pause(); setPlaying(false); }
        }
    };
    private void playNative() {
        if (player == null || !prepared) return;
        int focus = audioManager.requestAudioFocus(focusListener, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN);
        if (focus == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { player.start(); setPlaying(true); }
    }
    private void releasePlayer() {
        if (player != null) { player.release(); player = null; }
        prepared = false;
        if (audioManager != null) audioManager.abandonAudioFocus(focusListener);
    }
    private void loadAudio(String source) {
        releasePlayer();
        try {
            android.net.Uri uri = android.net.Uri.parse(source);
            String scheme = uri.getScheme();
            if (!"content".equals(scheme) && !"https".equals(scheme)) throw new IllegalArgumentException("Use a content or HTTPS audio URI");
            player = new android.media.MediaPlayer();
            player.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setWakeMode(this, android.os.PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(this, uri);
            player.setOnPreparedListener(p -> { prepared = true; playNative(); });
            player.setOnCompletionListener(p -> { setPlaying(false); });
            player.setOnErrorListener((p, what, extra) -> { releasePlayer(); setPlaying(false); return true; });
            player.prepareAsync();
        } catch (Exception e) { releasePlayer(); setPlaying(false); }
    }


    public static final String ACTION_PLAY =
        "com.generated.webapp.PLAY";

    public static final String ACTION_PAUSE =
        "com.generated.webapp.PAUSE";

    public static final String ACTION_NEXT =
        "com.generated.webapp.NEXT";

    public static final String ACTION_PREVIOUS =
        "com.generated.webapp.PREVIOUS";

    public static final String ACTION_UPDATE =
        "com.generated.webapp.UPDATE_MEDIA";

    public static final String EXTRA_TITLE =
        "title";

    public static final String EXTRA_ARTIST =
        "artist";

    public static final String EXTRA_PLAYING =
        "playing";

    private static final String CHANNEL_ID =
        "media_playback";

    private static final int NOTIFICATION_ID =
        5001;

    private MediaSession mediaSession;

    private String currentTitle =
        "Now Playing";

    private String currentArtist =
        "Media Player";

    private boolean isPlaying = false;

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();
        audioManager = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
        android.content.IntentFilter noisy = new android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(noisyReceiver, noisy, android.content.Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(noisyReceiver, noisy);

        mediaSession =
            new MediaSession(
                this,
                "GeneratedWebPlayer"
            );

        mediaSession.setCallback(
            new MediaSession.Callback() {

                @Override
                public void onPlay() {

                    sendControlBroadcast(
                        ACTION_PLAY
                    );

                    setPlaying(true);
                }

                @Override
                public void onPause() {

                    sendControlBroadcast(
                        ACTION_PAUSE
                    );

                    setPlaying(false);
                }

                @Override
                public void onSkipToNext() {

                    sendControlBroadcast(
                        ACTION_NEXT
                    );
                }

                @Override
                public void onSkipToPrevious() {

                    sendControlBroadcast(
                        ACTION_PREVIOUS
                    );
                }
            }
        );

        mediaSession.setActive(true);

        updatePlaybackState();
    }

    @Override
    public int onStartCommand(
        Intent intent,
        int flags,
        int startId
    ) {

        // Publish a foreground notification promptly, including during asynchronous preparation.
        startForeground(NOTIFICATION_ID, createNotification());
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        if (ACTION_STOP.equals(intent.getAction())) { releasePlayer(); stopForeground(true); stopSelf(); return START_NOT_STICKY; }
        if (ACTION_LOAD.equals(intent.getAction())) {
            currentTitle = intent.getStringExtra(EXTRA_TITLE) == null ? "Now Playing" : intent.getStringExtra(EXTRA_TITLE);
            currentArtist = intent.getStringExtra(EXTRA_ARTIST) == null ? "" : intent.getStringExtra(EXTRA_ARTIST);
            loadAudio(intent.getStringExtra("source"));
            return START_NOT_STICKY;
        }
        if (intent != null) {

            String action =
                intent.getAction();

            if (ACTION_PLAY.equals(action)) {

                sendControlBroadcast(
                    ACTION_PLAY
                );

                setPlaying(true);

            } else if (
                ACTION_PAUSE.equals(action)
            ) {

                sendControlBroadcast(
                    ACTION_PAUSE
                );

                setPlaying(false);

            } else if (
                ACTION_NEXT.equals(action)
            ) {

                sendControlBroadcast(
                    ACTION_NEXT
                );

            } else if (
                ACTION_PREVIOUS.equals(action)
            ) {

                sendControlBroadcast(
                    ACTION_PREVIOUS
                );

            } else if (
                ACTION_UPDATE.equals(action)
            ) {

                String title =
                    intent.getStringExtra(
                        EXTRA_TITLE
                    );

                String artist =
                    intent.getStringExtra(
                        EXTRA_ARTIST
                    );

                if (
                    title != null &&
                    !title.trim().isEmpty()
                ) {
                    currentTitle = title;
                }

                if (
                    artist != null &&
                    !artist.trim().isEmpty()
                ) {
                    currentArtist = artist;
                }

                isPlaying =
                    intent.getBooleanExtra(
                        EXTRA_PLAYING,
                        isPlaying
                    );
            }
        }

        updatePlaybackState();

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        );

        return START_NOT_STICKY;
    }

    private void setPlaying(
        boolean playing
    ) {

        isPlaying = playing;

        updatePlaybackState();

        NotificationManager manager =
            (NotificationManager)
                getSystemService(
                    NOTIFICATION_SERVICE
                );

        manager.notify(
            NOTIFICATION_ID,
            createNotification()
        );
    }

    private void updatePlaybackState() {

        if (mediaSession == null) {
            return;
        }

        long actions =
            PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS;

        int state =
            isPlaying
                ? PlaybackState.STATE_PLAYING
                : PlaybackState.STATE_PAUSED;

        PlaybackState playbackState =
            new PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    state,
                    PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    isPlaying ? 1f : 0f
                )
                .build();

        mediaSession.setPlaybackState(
            playbackState
        );

        MediaMetadata metadata =
            new MediaMetadata.Builder()
                .putString(
                    MediaMetadata.METADATA_KEY_TITLE,
                    currentTitle
                )
                .putString(
                    MediaMetadata.METADATA_KEY_ARTIST,
                    currentArtist
                )
                .build();

        mediaSession.setMetadata(
            metadata
        );
    }

    private Notification createNotification() {

        Intent openIntent =
            new Intent(
                this,
                MainActivity.class
            );

        openIntent.setFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent contentIntent =
            PendingIntent.getActivity(
                this,
                10,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                    | PendingIntent.FLAG_IMMUTABLE
            );

        PendingIntent previousIntent =
            serviceAction(
                ACTION_PREVIOUS,
                20
            );

        PendingIntent playPauseIntent =
            serviceAction(
                isPlaying
                    ? ACTION_PAUSE
                    : ACTION_PLAY,
                21
            );

        PendingIntent nextIntent =
            serviceAction(
                ACTION_NEXT,
                22
            );

        Notification.Action previous =
            new Notification.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousIntent
            ).build();

        Notification.Action playPause =
            new Notification.Action.Builder(
                isPlaying
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play,
                isPlaying
                    ? "Pause"
                    : "Play",
                playPauseIntent
            ).build();

        Notification.Action next =
            new Notification.Action.Builder(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent
            ).build();

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {

            builder =
                new Notification.Builder(
                    this,
                    CHANNEL_ID
                );

        } else {

            builder =
                new Notification.Builder(
                    this
                );
        }

        builder
            .setContentTitle(
                currentTitle
            )
            .setContentText(
                currentArtist
            )
            .setSmallIcon(
                android.R.drawable.ic_media_play
            )
            .setContentIntent(
                contentIntent
            )
            .setVisibility(
                Notification.VISIBILITY_PUBLIC
            )
            .setOngoing(
                isPlaying
            )
            .addAction(
                previous
            )
            .addAction(
                playPause
            )
            .addAction(
                next
            )
            .setStyle(
                new Notification.MediaStyle()
                    .setMediaSession(
                        mediaSession.getSessionToken()
                    )
                    .setShowActionsInCompactView(
                        0,
                        1,
                        2
                    )
            );

        return builder.build();
    }

    private PendingIntent serviceAction(
        String action,
        int requestCode
    ) {

        Intent intent =
            new Intent(
                this,
                MediaPlaybackService.class
            );

        intent.setAction(action);

        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void sendControlBroadcast(
        String action
    ) {

        if (player != null) {
            if (ACTION_PLAY.equals(action)) playNative();
            else if (ACTION_PAUSE.equals(action) && prepared) { player.pause(); setPlaying(false); }
            return;
        }
        Intent intent =
            new Intent(action);

        intent.setPackage(
            getPackageName()
        );

        sendBroadcast(intent);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
                );

            channel.setDescription(
                "Background media playback controls"
            );

            channel.setLockscreenVisibility(
                Notification.VISIBILITY_PUBLIC
            );

            NotificationManager manager =
                getSystemService(
                    NotificationManager.class
                );

            manager.createNotificationChannel(
                channel
            );
        }
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        try { unregisterReceiver(noisyReceiver); } catch (Exception ignored) { }


        if (mediaSession != null) {

            mediaSession.setActive(false);

            mediaSession.release();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
        Intent intent
    ) {

        return null;
    }
}
