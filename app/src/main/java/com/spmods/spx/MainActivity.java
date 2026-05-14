package com.spmods.spx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class MainActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;

    // Hero player views
    private VideoView heroVideoView;
    private ImageView ivHeroThumb;
    private LinearLayout llHeroOverlay;
    private LinearLayout llNowPlaying;
    private TextView tvNowPlayingTitle;
    private ImageView ivFullscreen;

    private VideoModel currentVideo;

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

        // Fullscreen button - open VideoPlayerActivity
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
        ivHeroThumb      = findViewById(R.id.ivHeroThumb);
        llHeroOverlay    = findViewById(R.id.llHeroOverlay);
        llNowPlaying     = findViewById(R.id.llNowPlaying);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
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

        // Show VideoView, hide overlay/thumbnail
        ivHeroThumb.setVisibility(View.GONE);
        llHeroOverlay.setVisibility(View.GONE);
        heroVideoView.setVisibility(View.VISIBLE);
        llNowPlaying.setVisibility(View.VISIBLE);
        tvNowPlayingTitle.setText(video.getTitle());

        heroVideoView.setVideoURI(video.getUri());
        heroVideoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            heroVideoView.start();
        });

        heroVideoView.setOnCompletionListener(mp -> {
            // Show overlay again when done
            heroVideoView.setVisibility(View.GONE);
            ivHeroThumb.setVisibility(View.VISIBLE);
            llHeroOverlay.setVisibility(View.VISIBLE);
            llNowPlaying.setVisibility(View.GONE);
            currentVideo = null;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume hero video if was playing before fullscreen
        if (currentVideo != null && !heroVideoView.isPlaying()) {
            heroVideoView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (heroVideoView != null && heroVideoView.isPlaying()) {
            heroVideoView.pause();
        }
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
