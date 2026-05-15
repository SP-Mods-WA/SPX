package com.spmods.spx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;

    // Hero player views
    private VideoView heroVideoView;
    private LinearLayout llHeroOverlay;
    private LinearLayout llControls;
    private View vControlsOverlay;
    private TextView tvNowPlayingTitle;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private SeekBar seekBar;
    private ImageView ivPlayPause;
    private ImageView ivRewind;
    private ImageView ivForward;
    private ImageView ivFullscreen;

    private VideoModel currentVideo;
    private boolean isPlaying = false;
    private final Handler seekHandler = new Handler();

    private final Runnable seekUpdater = new Runnable() {
        @Override
        public void run() {
            if (heroVideoView != null && isPlaying) {
                int current = heroVideoView.getCurrentPosition();
                int total   = heroVideoView.getDuration();
                if (total > 0) {
                    seekBar.setMax(total);
                    seekBar.setProgress(current);
                    tvCurrentTime.setText(formatTime(current));
                    tvDuration.setText(formatTime(total));
                }
                seekHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkPermissionAndLoadVideos();

        findViewById(R.id.ivNotification).setOnClickListener(v ->
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show());

        findViewById(R.id.ivSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.tvToolsSeeAll).setOnClickListener(v ->
                Toast.makeText(this, "All Tools - Coming Soon!", Toast.LENGTH_SHORT).show());

        // Play / Pause
        ivPlayPause.setOnClickListener(v -> {
            if (heroVideoView.isPlaying()) {
                heroVideoView.pause();
                isPlaying = false;
                ivPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                heroVideoView.start();
                isPlaying = true;
                ivPlayPause.setImageResource(R.drawable.ic_pause);
                seekHandler.post(seekUpdater);
            }
        });

        // Rewind 10s
        ivRewind.setOnClickListener(v -> {
            int pos = Math.max(heroVideoView.getCurrentPosition() - 10000, 0);
            heroVideoView.seekTo(pos);
        });

        // Forward 10s
        ivForward.setOnClickListener(v -> {
            int pos = Math.min(heroVideoView.getCurrentPosition() + 10000,
                    heroVideoView.getDuration());
            heroVideoView.seekTo(pos);
        });

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) heroVideoView.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
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
    }

    private void initViews() {
        rvVideoList      = findViewById(R.id.rvVideoList);
        tvEmptyState     = findViewById(R.id.tvEmptyState);
        heroVideoView    = findViewById(R.id.heroVideoView);
        llHeroOverlay    = findViewById(R.id.llHeroOverlay);
        llControls       = findViewById(R.id.llControls);
        vControlsOverlay = findViewById(R.id.vControlsOverlay);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvCurrentTime    = findViewById(R.id.tvCurrentTime);
        tvDuration       = findViewById(R.id.tvDuration);
        seekBar          = findViewById(R.id.seekBar);
        ivPlayPause      = findViewById(R.id.ivPlayPause);
        ivRewind         = findViewById(R.id.ivRewind);
        ivForward        = findViewById(R.id.ivForward);
        ivFullscreen     = findViewById(R.id.ivFullscreen);

        rvVideoList.setLayoutManager(new LinearLayoutManager(this));
        rvVideoList.setHasFixedSize(false);
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
    public void onVideoClick(VideoModel video) {
        currentVideo = video;

        llHeroOverlay.setVisibility(View.GONE);
        heroVideoView.setVisibility(View.VISIBLE);
        vControlsOverlay.setVisibility(View.VISIBLE);
        llControls.setVisibility(View.VISIBLE);
        tvNowPlayingTitle.setText(video.getTitle());
        ivPlayPause.setImageResource(R.drawable.ic_pause);

        heroVideoView.setVideoURI(video.getUri());
        heroVideoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            // Scale video to fill banner fully
            mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            heroVideoView.start();
            isPlaying = true;
            seekHandler.post(seekUpdater);
        });

        heroVideoView.setOnCompletionListener(mp -> {
            isPlaying = false;
            ivPlayPause.setImageResource(R.drawable.ic_play);
            seekHandler.removeCallbacks(seekUpdater);
        });
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
    }

    private String formatTime(int ms) {
        int s = ms / 1000;
        int m = s / 60;
        s = s % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos();
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_LONG).show();
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Storage permission required to show videos.");
            }
        }
    }
}
