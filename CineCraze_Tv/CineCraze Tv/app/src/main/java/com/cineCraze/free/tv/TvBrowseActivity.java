package com.cinecraze.free.tv;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.cinecraze.free.R;
import com.cinecraze.free.utils.PlaylistDownloadManager;

public class TvBrowseActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Redirect to blocking download screen if DB not ready
        PlaylistDownloadManager pdm = new PlaylistDownloadManager(this);
        if (!pdm.isDatabaseExists() || pdm.isDatabaseCorrupted()) {
            android.content.Intent i = new android.content.Intent(this, com.cinecraze.free.PlaylistDownloadActivity.class);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_tv_browse);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.tv_browse_container, new TvMainBrowseFragment())
                .commitNow();
        }
    }
}