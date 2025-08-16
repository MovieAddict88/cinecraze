package com.cinecraze.free.wear;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class WearMainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);

        TextView title = findViewById(R.id.title);
        TextView subtitle = findViewById(R.id.subtitle);
        if (title != null) {
            title.setText("CineCraze");
        }
        if (subtitle != null) {
            subtitle.setText("Wear OS companion");
        }
    }
}