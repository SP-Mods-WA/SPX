package com.spmods.spx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
    private static final int CONTROLS_HIDE_DELAY = 3000; // 3 seconds

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;

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
    private ImageView ivFullscreen;
    private ImageView ivClose;

    private VideoModel currentVideo;
    private boolean isPlaying = false;
    private boolean controlsVisible = false;

    private final Handler seekHandler  = new Handler();
    private final Handler hideHandler  = new Handler();

    private final Runnable seekUpdater = new Runnable() {
        @Override public void run() {
            if (heroVideoView != null && isPlaying) {
                int cur   = heroVideoView.getCurrentPosition();
                int total = heroVideoView.getDuration();
                if (total > 0) {
                    seekBar.setMax(total);
                    seekBar.setProgress(cur);
                    tvCurrentTime.setText(formatTime(cur));
                    tvDuration.setText(formatTime(total));
                }
                seekHandler.postDelayed(this, 500);
            }
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

        // Header buttons
        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());
        findViewById(R.id.ivSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.tvToolsSeeAll).setOnClickListener(v ->
                Toast.makeText(this, "All Tools - Coming Soon!", Toast.LENGTH_SHORT).show());

        // Tap on video banner to toggle controls
        heroVideoView.setOnClickListener(v -> toggleControls());
        flControls.setOnClickListener(v -> toggleControls());

        // Play / Pause
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
                scheduleHide();
            }
        });

        // Rewind 10s
        ivRewind.setOnClickListener(v -> {
            heroVideoView.seekTo(Math.max(heroVideoView.getCurrentPosition() - 10000, 0));
            scheduleHide();
        });

        // Forward 10s
        ivForward.setOnClickListener(v -> {
            heroVideoView.seekTo(Math.min(heroVideoView.getCurrentPosition() + 10000,
                    heroVideoView.getDuration()));
            scheduleHide();
        });

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) heroVideoView.seekTo(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {
                hideHandler.removeCallbacks(hideControls);
            }
            @Override public void onStopTrackingTouch(SeekBar s) { scheduleHide(); }
        });

        // Fullscreen
        ivFullscreen.setOnClickListener(v -> {
            if (currentVideo != null) {
                int pos = heroVideoView.getCurrentPosition();
                heroVideoView.pause();
                Intent intent = new Intent(this, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, currentVideo.getUri().toString());
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, currentVideo.getTitle());
                intent.putExtra(VideoPlayerActivity.EXTRA_START_POSITION, pos);
                startActivity(intent);
            }
        });

        // Close - stop and show placeholder
        ivClose.setOnClickListener(v -> stopHeroVideo());
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

        rvVideoList.setLayoutManager(new LinearLayoutManager(this));
        rvVideoList.setHasFixedSize(false);
    }

    @Override
    public void onVideoClick(VideoModel video) {
        currentVideo = video;

        llHeroOverlay.setVisibility(View.GONE);
        heroVideoView.setVisibility(View.VISIBLE);
        tvNowPlayingTitle.setText(video.getTitle());
        ivPlayPause.setImageResource(R.drawable.ic_pause);

        heroVideoView.setVideoURI(video.getUri());
        heroVideoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            heroVideoView.start();
            isPlaying = true;
            seekHandler.post(seekUpdater);
            // Show controls briefly then hide
            showControls();
            scheduleHide();
        });

        heroVideoView.setOnCompletionListener(mp -> {
            isPlaying = false;
            ivPlayPause.setImageResource(R.drawable.ic_play);
            seekHandler.removeCallbacks(seekUpdater);
            showControls(); // keep visible when stopped
        });
    }

    private void toggleControls() {
        if (controlsVisible) {
            fadeOutControls();
        } else {
            showControls();
            if (isPlaying) scheduleHide();
        }
    }

    private void showControls() {
        controlsVisible = true;
        flControls.setVisibility(View.VISIBLE);
        flControls.animate().alpha(1f).setDuration(200).start();
    }

    private void fadeOutControls() {
        controlsVisible = false;
        flControls.animate().alpha(0f).setDuration(300).withEndAction(() ->
                flControls.setVisibility(View.GONE)).start();
    }

    private void scheduleHide() {
        hideHandler.removeCallbacks(hideControls);
        hideHandler.postDelayed(hideControls, CONTROLS_HIDE_DELAY);
    }

    private void stopHeroVideo() {
        heroVideoView.stopPlayback();
        isPlaying = false;
        currentVideo = null;
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);
        heroVideoView.setVisibility(View.GONE);
        flControls.setVisibility(View.GONE);
        flControls.setAlpha(1f);
        controlsVisible = false;
        llHeroOverlay.setVisibility(View.VISIBLE);
    }

    private void checkPermissionAndLoadVideos() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    private void loadVideos() {
        List<VideoModel> videos = VideoLoader.getAllVideos(this);
        if (videos.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvVideoList.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvVideoList.setVisibility(View.VISIBLE);
            videoAdapter = new VideoAdapter(this, videos, this);
            rvVideoList.setAdapter(videoAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentVideo != null && !heroVideoView.isPlaying()) {
            heroVideoView.resume();
            isPlaying = true;
            ivPlayPause.setImageResource(R.drawable.ic_pause);
            seekHandler.post(seekUpdater);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (heroVideoView != null && heroVideoView.isPlaying()) {
            heroVideoView.pause();
            isPlaying = false;
        }
        seekHandler.removeCallbacks(seekUpdater);
        hideHandler.removeCallbacks(hideControls);
    }

    private String formatTime(int ms) {
        int s = ms / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Storage permission required to show videos.");
        }
    }
}
