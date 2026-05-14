package com.spmods.spx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spmods.spx.adapter.VideoAdapter;
import com.spmods.spx.model.VideoModel;
import com.spmods.spx.utils.VideoLoader;

import java.util.List;

public class MainActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView rvVideoList;
    private TextView tvEmptyState;
    private VideoAdapter videoAdapter;

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

        // Hero button
        findViewById(R.id.btnNewProject).setOnClickListener(v ->
                Toast.makeText(this, "New Project - Coming Soon!", Toast.LENGTH_SHORT).show());

        // See All
        findViewById(R.id.tvToolsSeeAll).setOnClickListener(v ->
                Toast.makeText(this, "All Tools - Coming Soon!", Toast.LENGTH_SHORT).show());
    }

    private void initViews() {
        rvVideoList = findViewById(R.id.rvVideoList);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvVideoList.setLayoutManager(new LinearLayoutManager(this));
        rvVideoList.setHasFixedSize(false);
    }

    private void checkPermissionAndLoadVideos() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_VIDEO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

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
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, video.getUri().toString());
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.getTitle());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos();
            } else {
                Toast.makeText(this, "Permission denied. Cannot load videos.", Toast.LENGTH_LONG).show();
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Storage permission required to show videos.");
            }
        }
    }
}
