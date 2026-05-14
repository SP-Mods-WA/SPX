package com.spmods.spx;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI      = "extra_video_uri";
    public static final String EXTRA_VIDEO_TITLE    = "extra_video_title";
    public static final String EXTRA_START_POSITION = "extra_start_position";

    private VideoView videoView;
    private MediaController mediaController;
    private TextView tvPlayerTitle;
    private ImageButton ibBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_video_player);

        videoView     = findViewById(R.id.videoView);
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        ibBack        = findViewById(R.id.ibBack);

        String uriString = getIntent().getStringExtra(EXTRA_VIDEO_URI);
        String title     = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);
        int startPos     = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        if (title != null) tvPlayerTitle.setText(title);
        ibBack.setOnClickListener(v -> onBackPressed());

        if (uriString != null) {
            setupPlayer(Uri.parse(uriString), startPos);
        } else {
            finish();
        }
    }

    private void setupPlayer(Uri videoUri, int startPos) {
        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            if (startPos > 0) videoView.seekTo(startPos);
            videoView.start();
        });

        videoView.setOnCompletionListener(mp -> finish());

        videoView.setOnErrorListener((mp, what, extra) -> {
            tvPlayerTitle.setText("Cannot play this video");
            return true;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) videoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) videoView.stopPlayback();
    }
}
