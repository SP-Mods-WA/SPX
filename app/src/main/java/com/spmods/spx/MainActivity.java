package com.spmods.spx;

import android.Manifest;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Rational;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements VideoAdapter.OnVideoClickListener, AudioService.OnAudioListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CONTROLS_HIDE_DELAY     = 3000;

    // PiP action constants
    private static final String PIP_ACTION_REWIND  = "pip_rewind";
    private static final String PIP_ACTION_PLAY    = "pip_play_pause";
    private static final String PIP_ACTION_FORWARD = "pip_forward";
    private static final String PIP_ACTION_CLOSE   = "pip_close";
    private static final int    PIP_REQUEST_CODE   = 200;

    private BroadcastReceiver pipReceiver;

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private int currentIndex  = -1;
    private int savedPosition = 0;

    // Hero player views
    private VideoView heroVideoView;
    private FrameLayout flHeroBanner;
    private LinearLayout llHeroOverlay;
    private FrameLayout flControls;
    private TextView tvNowPlayingTitle;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private SeekBar seekBar;
    private ImageView ivPlayPause;
    private ImageView ivRewind;
    private ImageView ivForward;
    private ImageView ivClose;

    // Action buttons
    private ImageView ivAutoNext;
    private TextView  tvAutoNext;
    private ImageView ivRepeat;
    private TextView  tvRepeat;
    private ImageView ivBgPlay;
    private TextView  tvBgPlay;

    private boolean isPlaying                = false;
    private boolean controlsVisible          = false;
    private boolean autoNextEnabled          = false;
    private boolean repeatEnabled            = false;
    private boolean bgPlayEnabled            = false;
    private boolean isBound                  = false;
    private boolean isInPipMode              = false;
    private boolean isLaunchingChildActivity = false;

    // Pinch-to-zoom scale
    private float videoScale = 1.0f;
    private ScaleGestureDetector scaleDetector;

    // Non-video UI (hidden during PiP)
    private LinearLayout llHeader;
    private LinearLayout llMyVideosHeader;
    private View vMyVideosDivider;
    private LinearLayout hsvActionsWrapper;
    private View hsvActions;

    // AudioService
    private AudioService audioService;
    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            audioService = ((AudioService.AudioBinder) b).getService();
            audioService.setListener(MainActivity.this);
            isBound = true;
        }
        @Override public void onServiceDisconnected(ComponentName n) {
            isBound = false;
        }
    };

    private final Handler seekHandler = new Handler();
    private final Handler hideHandler = new Handler();

    private final Runnable seekUpdater = new Runnable() {
        @Override public void run() {
            if (heroVideoView == null) return;
            try {
                int cur   = heroVideoView.getCurrentPosition();
                int total = heroVideoView.getDuration();
                if (total > 0) {
                    seekBar.setMax(total);
                    seekBar.setProgress(cur);
                    tvCurrentTime.setText(formatTime(cur));
                    tvDuration.setText(formatTime(total));
                }
            } catch (Exception ignored) {}
            if (isPlaying) seekHandler.postDelayed(this, 300);
        }
    };

    private final Runnable hideControls = () -> { if (isPlaying) fadeOutControls(); };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupPinchZoom();
        checkPermissionAndLoadVideos();

        Intent svcIntent = new Intent(this, AudioService.class);
        bindService(svcIntent, serviceConn, BIND_AUTO_CREATE);

        // Register PiP broadcast receiver
        registerPipReceiver();

        // Header
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v -> {
            isLaunchingChildActivity = true;
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // Banner tap
        heroVideoView.setOnClickListener(v -> toggleControls());
        flControls.setOnClickListener(v -> toggleControls());

        // Play/Pause
        ivPlayPause.setOnClickListener(v -> {
            if (heroVideoView.isPlaying()) {
                heroVideoView.pause();
                isPlaying = false;
                ivPlayPause.setImageResource(R.drawable.ic_play);
                hideHandler.removeCallbacks(hideControls);
            } else {
                heroVideoView.start();
                isPlaying = true;
                ivPlayPause.setImageResource(R.drawable.ic_pause);
                seekHandler.post(seekUpdater);
                scheduleHide();
            }
        });

        // Rewind / Forward
        ivRewind.setOnClickListener(v -> {
            heroVideoView.seekTo(Math.max(heroVideoView.getCurrentPosition() - 10000, 0));
            scheduleHide();
        });
        ivForward.setOnClickListener(v -> {
            heroVideoView.seekTo(Math.min(
                    heroVideoView.getCurrentPosition() + 10000, heroVideoView.getDuration()));
            scheduleHide();
        });

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) { heroVideoView.seekTo(p); tvCurrentTime.setText(formatTime(p)); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {
                hideHandler.removeCallbacks(hideControls);
            }
            @Override public void onStopTrackingTouch(SeekBar s) { scheduleHide(); }
        });

        // Close
        ivClose.setOnClickListener(v -> stopHeroVideo());

        // Auto Next
        ivAutoNext.setOnClickListener(v -> {
            autoNextEnabled = !autoNextEnabled;
            updateAutoNextUI();
            Toast.makeText(this, autoNextEnabled ? "Auto Next ON" : "Auto Next OFF",
                    Toast.LENGTH_SHORT).show();
        });

        // BG Play - manual toggle only, no auto-start
        ivBgPlay.setOnClickListener(v -> toggleBgPlay());
        tvBgPlay.setOnClickListener(v -> toggleBgPlay());

        // Other buttons
        findViewById(R.id.btnShare).setOnClickListener(v -> shareCurrentVideo());
        findViewById(R.id.btnPip).setOnClickListener(v -> enterPip());
        findViewById(R.id.btnFullscreenAction).setOnClickListener(v -> openFullscreen());
        findViewById(R.id.btnRepeat).setOnClickListener(v -> {
            repeatEnabled = !repeatEnabled;
            updateRepeatUI();
            Toast.makeText(this, repeatEnabled ? "Repeat ON" : "Repeat OFF",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleBgPlay() {
        bgPlayEnabled = !bgPlayEnabled;
        updateBgPlayUI();
        Toast.makeText(this, bgPlayEnabled ? "BG Play ON" : "BG Play OFF",
                Toast.LENGTH_SHORT).show();
    }

    private void setupPinchZoom() {
        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        videoScale *= detector.getScaleFactor();
                        videoScale = Math.max(0.8f, Math.min(videoScale, 3.0f));
                        heroVideoView.setScaleX(videoScale);
                        heroVideoView.setScaleY(videoScale);
                        return true;
                    }
                });

        flHeroBanner.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            if (!scaleDetector.isInProgress()) {
                v.performClick();
            }
            return true;
        });
    }

    private void initViews() {
        rvVideoList       = findViewById(R.id.rvVideoList);
        tvEmptyState      = findViewById(R.id.tvEmptyState);
        heroVideoView     = findViewById(R.id.heroVideoView);
        flHeroBanner      = findViewById(R.id.flHeroBanner);
        llHeroOverlay     = findViewById(R.id.llHeroOverlay);
        flControls        = findViewById(R.id.flControls);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvCurrentTime     = findViewById(R.id.tvCurrentTime);
        tvDuration        = findViewById(R.id.tvDuration);
        seekBar           = findViewById(R.id.seekBar);
        ivPlayPause       = findViewById(R.id.ivPlayPause);
        ivRewind          = findViewById(R.id.ivRewind);
        ivForward         = findViewById(R.id.ivForward);
        ivClose           = findViewById(R.id.ivClose);
        ivAutoNext        = findViewById(R.id.ivAutoNext);
        tvAutoNext        = findViewById(R.id.tvAutoNext);
        ivRepeat          = findViewById(R.id.ivRepeat);
        tvRepeat          = findViewById(R.id.tvRepeat);
        ivBgPlay          = findViewById(R.id.ivBgPlay);
        tvBgPlay          = findViewById(R.id.tvBgPlay);

        rvVideoList.setLayoutManager(new LinearLayoutManager(this));
        rvVideoList.setHasFixedSize(false);

        llHeader         = findViewById(R.id.llHeader);
        llMyVideosHeader = findViewById(R.id.llMyVideosHeader);
        vMyVideosDivider = findViewById(R.id.vMyVideosDivider);
        hsvActions       = findViewById(R.id.hsvActions);
    }

    @Override
    public void onVideoClick(VideoModel video) {
        currentIndex  = videoList.indexOf(video);
        savedPosition = 0;
        videoScale = 1.0f;
        heroVideoView.setScaleX(1.0f);
        heroVideoView.setScaleY(1.0f);
        playVideo(video, 0);
    }

    @Override
    public void onVideoDeleted(int position) {
        // If playing video was deleted, stop it
        if (position == currentIndex) stopHeroVideo();
        else if (position < currentIndex) currentIndex--;
    }

    private void playVideo(VideoModel video, int startPos) {
        llHeroOverlay.setVisibility(View.GONE);
        heroVideoView.setVisibility(View.VISIBLE);
        tvNowPlayingTitle.setText(video.getTitle());
        ivPlayPause.setImageResource(R.drawable.ic_pause);
        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");
        tvDuration.setText("00:00");
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);

        heroVideoView.stopPlayback();
        heroVideoView.setVideoURI(video.getUri());
        heroVideoView.setOnPreparedListener(mp -> {
            mp.setLooping(repeatEnabled);
            mp.setVideoScalingMode(
                    android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            if (startPos > 0) heroVideoView.seekTo(startPos);
            heroVideoView.start();
            isPlaying = true;
            seekHandler.post(seekUpdater);
            showControls();
            scheduleHide();
        });

        heroVideoView.setOnCompletionListener(mp -> {
            isPlaying = false;
            seekHandler.removeCallbacks(seekUpdater);
            ivPlayPause.setImageResource(R.drawable.ic_play);
            if (!repeatEnabled && autoNextEnabled
                    && videoList != null && currentIndex + 1 < videoList.size()) {
                currentIndex++;
                playVideo(videoList.get(currentIndex), 0);
            } else {
                showControls();
            }
        });

        heroVideoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Cannot play this video", Toast.LENGTH_SHORT).show();
            stopHeroVideo();
            return true;
        });
    }

    private void openFullscreen() {
        if (currentIndex < 0 || videoList == null) {
            Toast.makeText(this, "Play a video first", Toast.LENGTH_SHORT).show();
            return;
        }
        savedPosition = heroVideoView.getCurrentPosition();
        heroVideoView.pause();
        isPlaying = false;
        VideoModel video = videoList.get(currentIndex);
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI,   video.getUri().toString());
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.getTitle());
        intent.putExtra(VideoPlayerActivity.EXTRA_START_POSITION, savedPosition);
        isLaunchingChildActivity = true;
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && currentIndex >= 0 && videoList != null) {
            isLaunchingChildActivity = false; // onStart already handled, block resume
            int returnPos = savedPosition;
            if (data != null)
                returnPos = data.getIntExtra(VideoPlayerActivity.EXTRA_START_POSITION, savedPosition);
            playVideo(videoList.get(currentIndex), returnPos);
        }
    }

    private void shareCurrentVideo() {
        if (currentIndex < 0 || videoList == null) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show();
            return;
        }
        VideoModel video = videoList.get(currentIndex);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("video/*");
        share.putExtra(Intent.EXTRA_STREAM, video.getUri());
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        isLaunchingChildActivity = true;
        startActivity(Intent.createChooser(share, "Share Video"));
    }

    private void registerPipReceiver() {
        pipReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null || !isInPipMode) return;
                switch (intent.getAction() != null ? intent.getAction() : "") {
                    case PIP_ACTION_REWIND:
                        heroVideoView.seekTo(Math.max(heroVideoView.getCurrentPosition() - 10000, 0));
                        updatePipParams();
                        break;
                    case PIP_ACTION_PLAY:
                        if (heroVideoView.isPlaying()) {
                            heroVideoView.pause();
                            isPlaying = false;
                        } else {
                            heroVideoView.start();
                            isPlaying = true;
                            seekHandler.post(seekUpdater);
                        }
                        updatePipParams();
                        break;
                    case PIP_ACTION_FORWARD:
                        heroVideoView.seekTo(Math.min(
                                heroVideoView.getCurrentPosition() + 10000,
                                heroVideoView.getDuration()));
                        updatePipParams();
                        break;
                    case PIP_ACTION_CLOSE:
                        isInPipMode = false;
                        stopHeroVideo();
                        // Exit PiP by moving task to back then bring it forward
                        moveTaskToBack(false);
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(PIP_ACTION_REWIND);
        filter.addAction(PIP_ACTION_PLAY);
        filter.addAction(PIP_ACTION_FORWARD);
        filter.addAction(PIP_ACTION_CLOSE);
        registerReceiver(pipReceiver, filter);
    }

    private PictureInPictureParams buildPipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;

        PendingIntent rewindIntent = PendingIntent.getBroadcast(this, PIP_REQUEST_CODE,
                new Intent(PIP_ACTION_REWIND),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playIntent = PendingIntent.getBroadcast(this, PIP_REQUEST_CODE + 1,
                new Intent(PIP_ACTION_PLAY),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent forwardIntent = PendingIntent.getBroadcast(this, PIP_REQUEST_CODE + 2,
                new Intent(PIP_ACTION_FORWARD),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent closeIntent = PendingIntent.getBroadcast(this, PIP_REQUEST_CODE + 3,
                new Intent(PIP_ACTION_CLOSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        List<RemoteAction> actions = new ArrayList<>();
        actions.add(new RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_replay_10),
                "Rewind", "Rewind 10s", rewindIntent));
        actions.add(new RemoteAction(
                Icon.createWithResource(this,
                        isPlaying ? R.drawable.ic_pause_white : R.drawable.ic_play_arrow_white),
                isPlaying ? "Pause" : "Play",
                isPlaying ? "Pause" : "Play", playIntent));
        actions.add(new RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_forward_10),
                "Forward", "Forward 10s", forwardIntent));
        actions.add(new RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_close),
                "Close", "Close", closeIntent));

        return new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .setActions(actions)
                .build();
    }

    private void updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
            PictureInPictureParams params = buildPipParams();
            if (params != null) setPictureInPictureParams(params);
        }
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (currentIndex < 0 || !isPlaying) {
                Toast.makeText(this, "Play a video first", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                isInPipMode = true;
                flControls.setVisibility(View.GONE);
                controlsVisible = false;
                hideHandler.removeCallbacks(hideControls);
                PictureInPictureParams params = buildPipParams();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                isInPipMode = false;
                Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPipUi() {
        // Show only the banner video area during PiP - hide everything else
        llHeader.setVisibility(View.GONE);
        hsvActions.setVisibility(View.GONE);
        llMyVideosHeader.setVisibility(View.GONE);
        vMyVideosDivider.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        rvVideoList.setVisibility(View.GONE);
        flControls.setVisibility(View.GONE);
        llHeroOverlay.setVisibility(View.GONE);
        heroVideoView.setVisibility(View.VISIBLE);
    }

    private void restoreNormalUi() {
        llHeader.setVisibility(View.VISIBLE);
        hsvActions.setVisibility(View.VISIBLE);
        llMyVideosHeader.setVisibility(View.VISIBLE);
        vMyVideosDivider.setVisibility(View.VISIBLE);
        if (videoList != null && !videoList.isEmpty()) {
            rvVideoList.setVisibility(View.VISIBLE);
        }
        // Restore hero state
        if (currentIndex >= 0) {
            heroVideoView.setVisibility(View.VISIBLE);
            llHeroOverlay.setVisibility(View.GONE);
        } else {
            heroVideoView.setVisibility(View.GONE);
            llHeroOverlay.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean inPip) {
        super.onPictureInPictureModeChanged(inPip);
        isInPipMode = inPip;
        if (inPip) {
            showPipUi();
            seekHandler.removeCallbacks(seekUpdater);
            hideHandler.removeCallbacks(hideControls);
        } else {
            // Returned from PiP
            restoreNormalUi();
            try {
                if (heroVideoView.isPlaying()) {
                    isPlaying = true;
                    ivPlayPause.setImageResource(R.drawable.ic_pause);
                    seekHandler.post(seekUpdater);
                    showControls();
                    scheduleHide();
                } else {
                    isPlaying = false;
                    ivPlayPause.setImageResource(R.drawable.ic_play);
                    if (currentIndex >= 0) showControls();
                }
            } catch (Exception ignored) {}
        }
    }

    // ── AudioService callbacks ─────────────────────
    @Override public void onPlaybackStateChanged(boolean playing) { }
    @Override public void onCompletion() {
        runOnUiThread(() -> {
            if (autoNextEnabled && videoList != null && currentIndex + 1 < videoList.size()) {
                currentIndex++;
                VideoModel next = videoList.get(currentIndex);
                if (isBound) audioService.startAudio(next.getUri(), next.getTitle(), next.getPath(), 0);
            }
        });
    }

    // ── App goes to background ─────────────────────
    @Override
    protected void onStop() {
        super.onStop();
        if (isInPipMode) return;
        seekHandler.removeCallbacks(seekUpdater);

        // Child activity (Settings, VideoPlayer, Share) - just save position, don't hand off to service
        if (isLaunchingChildActivity) {
            if (currentIndex >= 0 && videoList != null) {
                savedPosition = heroVideoView.getCurrentPosition();
                if (isPlaying) {
                    heroVideoView.pause();
                    isPlaying = false;
                }
            }
            return;
        }

        if (currentIndex >= 0 && videoList != null) {
            // Save position before pausing
            savedPosition = heroVideoView.getCurrentPosition();

            if (bgPlayEnabled && isPlaying) {
                // Hand off playback to AudioService (audio-only background)
                heroVideoView.pause();
                isPlaying = false;
                VideoModel video = videoList.get(currentIndex);
                Intent svcIntent = new Intent(this, AudioService.class);
                svcIntent.setAction(AudioService.ACTION_PLAY);
                svcIntent.putExtra(AudioService.EXTRA_URI,      video.getUri().toString());
                svcIntent.putExtra(AudioService.EXTRA_TITLE,    video.getTitle());
                svcIntent.putExtra(AudioService.EXTRA_PATH,     video.getPath());
                svcIntent.putExtra(AudioService.EXTRA_POSITION, savedPosition);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svcIntent);
                } else {
                    startService(svcIntent);
                }
            } else {
                // Not bg play - pause video and show minimal notification
                if (isPlaying) {
                    heroVideoView.pause();
                    isPlaying = false;
                }
                // Show notification so user can return to app
                if (currentIndex >= 0) {
                    VideoModel video = videoList.get(currentIndex);
                    Intent svcIntent = new Intent(this, AudioService.class);
                    svcIntent.setAction(AudioService.ACTION_NOTIFY_ONLY);
                    svcIntent.putExtra(AudioService.EXTRA_TITLE, video.getTitle());
                    svcIntent.putExtra(AudioService.EXTRA_PATH,  video.getPath());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(svcIntent);
                    } else {
                        startService(svcIntent);
                    }
                }
            }
        }
    }

    // ── App comes back ─────────────────────────────
    @Override
    protected void onStart() {
        super.onStart();
        if (isInPipMode) return;

        if (isLaunchingChildActivity) {
            // Returning from Settings/Share - VideoView still has URI, just resume
            isLaunchingChildActivity = false;
            if (currentIndex >= 0 && videoList != null) {
                playVideo(videoList.get(currentIndex), savedPosition);
            }
            return;
        }

        // Returning from background - sync position from AudioService FIRST, then kill it
        if (isBound && audioService != null) {
            int svcPos = audioService.getCurrentPosition();
            if (svcPos > 0) savedPosition = svcPos;
        }
        // Stop service synchronously before starting video to avoid double audio
        stopService(new Intent(this, AudioService.class));

        if (currentIndex >= 0 && videoList != null) {
            playVideo(videoList.get(currentIndex), savedPosition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) { unbindService(serviceConn); isBound = false; }
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);
        try { unregisterReceiver(pipReceiver); } catch (Exception ignored) {}
        // If bg play not enabled, stop service on destroy
        // Always stop service when app is destroyed (not just backgrounded)
        stopService(new Intent(this, AudioService.class));
    }

    // ── Controls ──────────────────────────────────
    private void toggleControls() {
        if (controlsVisible) fadeOutControls();
        else { showControls(); if (isPlaying) scheduleHide(); }
    }

    private void showControls() {
        controlsVisible = true;
        flControls.setVisibility(View.VISIBLE);
        flControls.animate().alpha(1f).setDuration(200).start();
    }

    private void fadeOutControls() {
        controlsVisible = false;
        flControls.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> flControls.setVisibility(View.GONE)).start();
    }

    private void scheduleHide() {
        hideHandler.removeCallbacks(hideControls);
        hideHandler.postDelayed(hideControls, CONTROLS_HIDE_DELAY);
    }

    private void stopHeroVideo() {
        heroVideoView.stopPlayback();
        isPlaying = false; currentIndex = -1; savedPosition = 0;
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);
        heroVideoView.setVisibility(View.GONE);
        flControls.setVisibility(View.GONE);
        flControls.setAlpha(1f);
        controlsVisible = false;
        llHeroOverlay.setVisibility(View.VISIBLE);
        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");
        tvDuration.setText("00:00");
        stopService(new Intent(this, AudioService.class));
    }

    private void updateAutoNextUI() {
        int c = autoNextEnabled ? 0xFFFFFFFF : 0xFFAAAAAA;
        ivAutoNext.setColorFilter(c);
        tvAutoNext.setTextColor(c);
    }

    private void updateRepeatUI() {
        int c = repeatEnabled ? 0xFFFF00CC : 0xFFAAAAAA;
        ivRepeat.setColorFilter(c);
        tvRepeat.setTextColor(c);
    }

    private void updateBgPlayUI() {
        int c = bgPlayEnabled ? 0xFF00E676 : 0xFFAAAAAA;
        ivBgPlay.setColorFilter(c);
        tvBgPlay.setTextColor(c);
    }

    // ── Permission / Load ─────────────────────────
    private void checkPermissionAndLoadVideos() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQUEST_CODE);
        }
    }

    private void loadVideos() {
        new Thread(() -> {
            List<VideoModel> loaded = VideoLoader.getAllVideos(this);
            runOnUiThread(() -> {
                videoList = loaded;
                if (videoList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvVideoList.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvVideoList.setVisibility(View.VISIBLE);
                    videoAdapter = new VideoAdapter(this, videoList, this);
                    rvVideoList.setAdapter(videoAdapter);
                }
            });
        }).start();
    }

    private String formatTime(int ms) {
        int s = ms / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PERMISSION_REQUEST_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Storage permission required.");
        }
    }
}
