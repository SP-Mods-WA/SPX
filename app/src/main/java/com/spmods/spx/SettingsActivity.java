package com.spmods.spx;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton ibBack = findViewById(R.id.ibSettingsBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> onBackPressed());
        }
    }
}
