package com.spmods.spx;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Rational;
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

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements VideoAdapter.OnVideoClickListener, AudioService.OnAudioListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CONTROLS_HIDE_DELAY     = 3000;

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private int currentIndex  = -1;
    private int savedPosition = 0;

    // Hero player views
    private VideoView heroVideoView;
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

    private boolean isPlaying       = false;
    private boolean controlsVisible = false;
    private boolean autoNextEnabled = false;
    private boolean repeatEnabled   = false;
    private boolean bgPlayEnabled   = false;  // Background play toggle
    private boolean isBound         = false;

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
            int cur   = heroVideoView.getCurrentPosition();
            int total = heroVideoView.getDuration();
            if (total > 0) {
                seekBar.setMax(total);
                seekBar.setProgress(cur);
                tvCurrentTime.setText(formatTime(cur));
                tvDuration.setText(formatTime(total));
            }
            if (isPlaying) seekHandler.postDelayed(this, 300);
        }
    };

    private final Runnable hideControls = () -> { if (isPlaying) fadeOutControls(); };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        checkPermissionAndLoadVideos();

        // Bind AudioService
        Intent svcIntent = new Intent(this, AudioService.class);
        bindService(svcIntent, serviceConn, BIND_AUTO_CREATE);

        // Header
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

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

        // Action buttons
        ivAutoNext.setOnClickListener(v -> {
            autoNextEnabled = !autoNextEnabled;
            updateAutoNextUI();
            Toast.makeText(this, autoNextEnabled ? "Auto Next ON" : "Auto Next OFF",
                    Toast.LENGTH_SHORT).show();
        });

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

    private void initViews() {
        rvVideoList       = findViewById(R.id.rvVideoList);
        tvEmptyState      = findViewById(R.id.tvEmptyState);
        heroVideoView     = findViewById(R.id.heroVideoView);
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

        rvVideoList.setLayoutManager(new LinearLayoutManager(this));
        rvVideoList.setHasFixedSize(false);
    }

    @Override
    public void onVideoClick(VideoModel video) {
        currentIndex  = videoList.indexOf(video);
        savedPosition = 0;
        playVideo(video, 0);
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
                    android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
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
    }

    private void openFullscreen() {
        if (currentIndex < 0 || videoList == null) return;
        savedPosition = heroVideoView.getCurrentPosition();
        heroVideoView.pause();
        isPlaying = false;
        VideoModel video = videoList.get(currentIndex);
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI,   video.getUri().toString());
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.getTitle());
        intent.putExtra(VideoPlayerActivity.EXTRA_START_POSITION, savedPosition);
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && currentIndex >= 0 && videoList != null) {
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
        Intent chooser = Intent.createChooser(share, "Share Video");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chooser);
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (currentIndex < 0) {
                Toast.makeText(this, "Play a video first", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9)).build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    // ── AudioService callbacks ─────────────────────
    @Override public void onPlaybackStateChanged(boolean playing) { }
    @Override public void onCompletion() {
        // Auto next via service
        runOnUiThread(() -> {
            if (autoNextEnabled && videoList != null && currentIndex + 1 < videoList.size()) {
                currentIndex++;
                VideoModel next = videoList.get(currentIndex);
                if (isBound) {
                    audioService.startAudio(next.getUri(), next.getTitle(), 0);
                }
            }
        });
    }

    // ── App goes to background - start AudioService ─
    @Override
    protected void onStop() {
        super.onStop();
        if (currentIndex >= 0 && videoList != null && isPlaying) {
            int pos = heroVideoView.getCurrentPosition();
            heroVideoView.pause();
            // Start background audio from same position
            VideoModel video = videoList.get(currentIndex);
            Intent svcIntent = new Intent(this, AudioService.class);
            svcIntent.setAction(AudioService.ACTION_PLAY);
            svcIntent.putExtra(AudioService.EXTRA_URI,      video.getUri().toString());
            svcIntent.putExtra(AudioService.EXTRA_TITLE,    video.getTitle());
            svcIntent.putExtra(AudioService.EXTRA_POSITION, pos);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svcIntent);
            } else {
                startService(svcIntent);
            }
        }
    }

    // ── App comes back - stop AudioService, resume video ─
    @Override
    protected void onStart() {
        super.onStart();
        // Stop background audio
        stopService(new Intent(this, AudioService.class));

        // Resume video if was playing
        if (currentIndex >= 0 && videoList != null && !isPlaying) {
            heroVideoView.resume();
            isPlaying = true;
            ivPlayPause.setImageResource(R.drawable.ic_pause);
            seekHandler.post(seekUpdater);
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
        ivAutoNext.setColorFilter(c); tvAutoNext.setTextColor(c);
    }

    private void updateRepeatUI() {
        int c = repeatEnabled ? 0xFFFF00CC : 0xFFAAAAAA;
        ivRepeat.setColorFilter(c); tvRepeat.setTextColor(c);
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
        videoList = VideoLoader.getAllVideos(this);
        if (videoList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvVideoList.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvVideoList.setVisibility(View.VISIBLE);
            videoAdapter = new VideoAdapter(this, videoList, this);
            rvVideoList.setAdapter(videoAdapter);
        }
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
                && results[0] == PackageManager.PERMISSION_GRANTED) loadVideos();
        else {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Storage permission required.");
        }
    }
}
