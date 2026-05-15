package com.spmods.spx;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CONTROLS_HIDE_DELAY     = 3000;

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;
    private List<VideoModel> videoList;
    private int currentIndex = -1;

    // Hero player
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
    private ImageView ivFullscreen;
    private ImageView ivClose;

    // Action buttons
    private ImageView ivAutoNext;
    private TextView tvAutoNext;
    private ImageView ivRepeat;
    private TextView tvRepeat;

    private boolean isPlaying      = false;
    private boolean controlsVisible = false;
    private boolean autoNextEnabled = false;
    private boolean repeatEnabled   = false;

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

    private final Runnable hideControls = () -> {
        if (isPlaying) fadeOutControls();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkPermissionAndLoadVideos();

        // Header
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // Video tap = toggle controls
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
            heroVideoView.seekTo(Math.min(heroVideoView.getCurrentPosition() + 10000,
                    heroVideoView.getDuration()));
            scheduleHide();
        });

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    heroVideoView.seekTo(p);
                    tvCurrentTime.setText(formatTime(p));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {
                hideHandler.removeCallbacks(hideControls);
            }
            @Override public void onStopTrackingTouch(SeekBar s) { scheduleHide(); }
        });

        // Fullscreen (inside controls overlay)
        ivFullscreen.setOnClickListener(v -> openFullscreen());

        // Close
        ivClose.setOnClickListener(v -> stopHeroVideo());

        // ---- ACTION BUTTONS ----

        // Auto Next toggle
        ivAutoNext.setOnClickListener(v -> {
            autoNextEnabled = !autoNextEnabled;
            updateAutoNextUI();
            Toast.makeText(this,
                    autoNextEnabled ? "Auto Next ON" : "Auto Next OFF",
                    Toast.LENGTH_SHORT).show();
        });

        // Share
        findViewById(R.id.btnShare).setOnClickListener(v -> shareCurrentVideo());

        // PiP
        findViewById(R.id.btnPip).setOnClickListener(v -> enterPip());

        // Fullscreen action button
        findViewById(R.id.btnFullscreenAction).setOnClickListener(v -> openFullscreen());

        // Repeat toggle
        findViewById(R.id.btnRepeat).setOnClickListener(v -> {
            repeatEnabled = !repeatEnabled;
            updateRepeatUI();
            Toast.makeText(this,
                    repeatEnabled ? "Repeat ON" : "Repeat OFF",
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
        ivFullscreen      = findViewById(R.id.ivFullscreen);
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
        if (videoList != null) {
            currentIndex = videoList.indexOf(video);
        }
        playVideo(video);
    }

    private void playVideo(VideoModel video) {
        llHeroOverlay.setVisibility(View.GONE);
        heroVideoView.setVisibility(View.VISIBLE);
        tvNowPlayingTitle.setText(video.getTitle());
        ivPlayPause.setImageResource(R.drawable.ic_pause);

        // Reset seekbar
        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");
        tvDuration.setText("00:00");
        seekHandler.removeCallbacks(seekUpdater);

        heroVideoView.stopPlayback();
        heroVideoView.setVideoURI(video.getUri());
        heroVideoView.setOnPreparedListener(mp -> {
            mp.setLooping(repeatEnabled);
            mp.setVideoScalingMode(
                    android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
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

            if (repeatEnabled) {
                // looping handled by MediaPlayer
            } else if (autoNextEnabled && videoList != null && currentIndex + 1 < videoList.size()) {
                currentIndex++;
                VideoModel next = videoList.get(currentIndex);
                playVideo(next);
            } else {
                showControls();
            }
        });
    }

    private void openFullscreen() {
        if (currentIndex < 0 || videoList == null) return;
        VideoModel video = videoList.get(currentIndex);
        int pos = heroVideoView.getCurrentPosition();
        heroVideoView.pause();
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, video.getUri().toString());
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.getTitle());
        intent.putExtra(VideoPlayerActivity.EXTRA_START_POSITION, pos);
        startActivity(intent);
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
        startActivity(Intent.createChooser(share, "Share Video"));
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (currentIndex < 0 || videoList == null) {
                Toast.makeText(this, "Play a video first", Toast.LENGTH_SHORT).show();
                return;
            }
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } else {
            Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAutoNextUI() {
        if (autoNextEnabled) {
            ivAutoNext.setColorFilter(getResources().getColor(android.R.color.white));
            tvAutoNext.setTextColor(0xFFFFFFFF);
        } else {
            ivAutoNext.setColorFilter(0xFFAAAAAA);
            tvAutoNext.setTextColor(0xFFAAAAAA);
        }
    }

    private void updateRepeatUI() {
        if (repeatEnabled) {
            ivRepeat.setColorFilter(0xFFFF00CC);
            tvRepeat.setTextColor(0xFFFF00CC);
        } else {
            ivRepeat.setColorFilter(0xFFAAAAAA);
            tvRepeat.setTextColor(0xFFAAAAAA);
        }
        // Apply to current playback
        if (heroVideoView != null && heroVideoView.isPlaying()) {
            // Will take effect on next prepare
        }
    }

    // ---- Controls show/hide ----
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
        isPlaying = false;
        currentIndex = -1;
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
    }

    // ---- Permission / Load ----
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

    @Override
    protected void onResume() {
        super.onResume();
        if (currentIndex >= 0 && !heroVideoView.isPlaying()) {
            heroVideoView.resume();
            isPlaying = true;
            ivPlayPause.setImageResource(R.drawable.ic_pause);
            seekHandler.post(seekUpdater);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (heroVideoView != null && heroVideoView.isPlaying()) heroVideoView.pause();
        isPlaying = false;
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentIndex >= 0) {
            enterPip();
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
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Storage permission required.");
        }
    }
}
