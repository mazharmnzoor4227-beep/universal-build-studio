package com.generated.webapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;

public class MediaPlaybackService extends Service {

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
            "Generated App";

    private boolean isPlaying =
            false;

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

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

                updatePlaybackState();
            }
        }

        Notification notification =
                createNotification();

        startForeground(
                NOTIFICATION_ID,
                notification
        );

        return START_STICKY;
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

        Notification.Action previousAction =
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_previous,
                        "Previous",
                        previousIntent
                ).build();

        Notification.Action playPauseAction =
                new Notification.Action.Builder(
                        isPlaying
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        isPlaying
                                ? "Pause"
                                : "Play",
                        playPauseIntent
                ).build();

        Notification.Action nextAction =
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
                .setOngoing(
                        isPlaying
                )
                .setVisibility(
                        Notification.VISIBILITY_PUBLIC
                )
                .addAction(
                        previousAction
                )
                .addAction(
                        playPauseAction
                )
                .addAction(
                        nextAction
                )
                .setStyle(
                        new Notification.MediaStyle()
                                .setMediaSession(
                                        mediaSession
                                                .getSessionToken()
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
                            NotificationManager
                                    .IMPORTANCE_LOW
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
