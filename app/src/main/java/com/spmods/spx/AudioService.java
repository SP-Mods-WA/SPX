package com.spmods.spx;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;

public class AudioService extends Service {

    public static final String ACTION_PLAY        = "com.spmods.spx.PLAY";
    public static final String ACTION_PAUSE       = "com.spmods.spx.PAUSE";
    public static final String ACTION_STOP        = "com.spmods.spx.STOP";
    public static final String ACTION_NEXT        = "com.spmods.spx.NEXT";
    public static final String ACTION_NOTIFY_ONLY = "com.spmods.spx.NOTIFY_ONLY";
    public static final String EXTRA_URI     = "extra_uri";
    public static final String EXTRA_TITLE   = "extra_title";
    public static final String EXTRA_PATH    = "extra_path";
    public static final String EXTRA_POSITION = "extra_position";

    private static final String CHANNEL_ID = "spx_audio_channel";
    private static final int    NOTIF_ID   = 1001;

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private String currentTitle = "";
    private String currentPath  = "";
    private boolean isPlaying   = false;

    public interface OnAudioListener {
        void onPlaybackStateChanged(boolean playing);
        void onCompletion();
    }

    private OnAudioListener listener;

    public class AudioBinder extends Binder {
        public AudioService getService() { return AudioService.this; }
    }
    private final IBinder binder = new AudioBinder();

    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case ACTION_PLAY:  resumeAudio(); break;
                case ACTION_PAUSE: pauseAudio();  break;
                case ACTION_STOP:  stopSelf();    break;
                case ACTION_NEXT:
                    if (listener != null) listener.onCompletion();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "SPXAudioService");
        mediaSession.setActive(true);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_NOTIFY_ONLY);
        registerReceiver(controlReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (action == null) return START_STICKY;
        switch (action) {
            case ACTION_PLAY:
                String uriStr = intent.getStringExtra(EXTRA_URI);
                String title  = intent.getStringExtra(EXTRA_TITLE);
                String path   = intent.getStringExtra(EXTRA_PATH);
                int    pos    = intent.getIntExtra(EXTRA_POSITION, 0);
                if (uriStr != null) startAudio(Uri.parse(uriStr), title, path, pos);
                break;
            case ACTION_NOTIFY_ONLY:
                // Show paused notification so user can tap to return - no audio
                currentTitle = intent.getStringExtra(EXTRA_TITLE) != null
                        ? intent.getStringExtra(EXTRA_TITLE) : "SPX Player";
                currentPath  = intent.getStringExtra(EXTRA_PATH)  != null
                        ? intent.getStringExtra(EXTRA_PATH)  : "";
                isPlaying = false;
                showNotification(false);
                break;
            case ACTION_PAUSE: pauseAudio(); break;
            case ACTION_STOP:  stopSelf();   break;
        }
        return START_NOT_STICKY;
    }

    public void startAudio(Uri uri, String title, String path, int seekTo) {
        currentTitle = title != null ? title : "Unknown";
        currentPath  = path  != null ? path  : "";
        try {
            if (mediaPlayer != null) { mediaPlayer.release(); }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (seekTo > 0) mp.seekTo(seekTo);
                mp.start();
                isPlaying = true;
                showNotification(true);
                if (listener != null) listener.onPlaybackStateChanged(true);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                showNotification(false);
                if (listener != null) listener.onCompletion();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            showNotification(false);
            if (listener != null) listener.onPlaybackStateChanged(false);
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            showNotification(true);
            if (listener != null) listener.onPlaybackStateChanged(true);
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            try { return mediaPlayer.getCurrentPosition(); } catch (Exception ignored) {}
        }
        return 0;
    }

    public boolean isPlaying() { return isPlaying; }
    public void setListener(OnAudioListener l) { this.listener = l; }

    // ── Notification ──────────────────────────────
    private void showNotification(boolean playing) {
        startForeground(NOTIF_ID, buildNotification(playing));
    }

    private Notification buildNotification(boolean playing) {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openIntent = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent toggleIntent;
        if (playing) {
            Intent pi = new Intent(ACTION_PAUSE);
            pi.setPackage(getPackageName());
            toggleIntent = PendingIntent.getBroadcast(this, 1, pi,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            Intent pi = new Intent(ACTION_PLAY);
            pi.setPackage(getPackageName());
            toggleIntent = PendingIntent.getBroadcast(this, 1, pi,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        Intent nextI = new Intent(ACTION_NEXT);
        nextI.setPackage(getPackageName());
        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 2, nextI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopI = new Intent(ACTION_STOP);
        stopI.setPackage(getPackageName());
        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 3, stopI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Load thumbnail for notification
        Bitmap thumb = null;
        if (!currentPath.isEmpty()) {
            try {
                thumb = ThumbnailUtils.createVideoThumbnail(
                        currentPath, MediaStore.Images.Thumbnails.MINI_KIND);
            } catch (Exception ignored) {}
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle(currentTitle)
                .setContentText(playing ? "Playing in background" : "Paused")
                .setContentIntent(openIntent)
                .addAction(playing ? R.drawable.ic_pause_white : R.drawable.ic_play_arrow_white,
                        playing ? "Pause" : "Play", toggleIntent)
                .addAction(R.drawable.ic_skip_next_white, "Next", nextIntent)
                .addAction(R.drawable.ic_close, "Stop", stopIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(playing)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setColor(0xFFFF00CC);

        if (thumb != null) {
            builder.setLargeIcon(thumb);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SPX Background Player", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Audio playback while app is in background");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(controlReceiver); } catch (Exception ignored) {}
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        if (mediaSession != null) { mediaSession.release(); }
        stopForeground(true);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }
}
