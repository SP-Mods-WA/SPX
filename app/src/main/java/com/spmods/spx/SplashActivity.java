package com.spmods.spx;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private static final int SPLASH_DURATION = 2200; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen, no status bar
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView fromText = findViewById(R.id.splash_from);

        // --- Logo animation: scale up + fade in (like WhatsApp) ---
        ScaleAnimation scale = new ScaleAnimation(
            0.85f, 1.0f,
            0.85f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(400);
        scale.setFillAfter(true);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);
        fadeIn.setFillAfter(true);

        AnimationSet logoAnim = new AnimationSet(true);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(fadeIn);
        logoAnim.setFillAfter(true);

        logo.startAnimation(logoAnim);

        // --- "from SPMODS" fade in after short delay ---
        AlphaAnimation fromFade = new AlphaAnimation(0f, 1f);
        fromFade.setDuration(500);
        fromFade.setStartOffset(700);
        fromFade.setFillAfter(true);
        fromText.startAnimation(fromFade);

        // --- Navigate to MainActivity after splash ---
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            // Pass through any VIEW/SEND intent that launched us
            Intent incoming = getIntent();
            if (incoming != null && incoming.getAction() != null) {
                intent.setAction(incoming.getAction());
                intent.setData(incoming.getData());
                if (incoming.getExtras() != null) {
                    intent.putExtras(incoming.getExtras());
                }
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Disable back press on splash
    }
}
