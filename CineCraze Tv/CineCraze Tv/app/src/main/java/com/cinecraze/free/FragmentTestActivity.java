package com.cinecraze.free;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple test activity to demonstrate that the fragment implementation is working.
 * This can be used to test the fragment-based navigation before fully switching over.
 *
 * To test this, you can temporarily change the AndroidManifest.xml to launch this activity instead:
 * android:name=".FragmentTestActivity"
 */
public class FragmentTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Redirect to FragmentMainActivity to exercise the fragment-based UI
        Toast.makeText(this, "Launching fragment-based CineCraze...", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, FragmentMainActivity.class));
        finish();
    }
}