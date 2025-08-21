package com.cinecraze.free;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cinecraze.free.utils.PlaylistDownloadManager;

import java.io.File;

/**
 * A blocking first-run activity that downloads the playlist.db before entering the app.
 */
public class PlaylistDownloadActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView progressText;
    private TextView statusText;
    private Button continueButton;
    private Button retryButton;

    private PlaylistDownloadManager downloadManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_download);

        // Prevent dismiss by touches outside, keep screen on during download
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        statusText = findViewById(R.id.status_text);
        continueButton = findViewById(R.id.continue_button);
        retryButton = findViewById(R.id.retry_button);

        continueButton.setOnClickListener(v -> goToApp());
        retryButton.setOnClickListener(v -> startDownload());

        downloadManager = new PlaylistDownloadManager(this);

        // If DB already exists, skip
        if (downloadManager.isDatabaseExists() && !downloadManager.isDatabaseCorrupted()) {
            goToApp();
        } else {
            startDownload();
        }
    }

    private void startDownload() {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        progressText.setText("0%");
        statusText.setText("Downloading database...");
        continueButton.setVisibility(android.view.View.GONE);
        retryButton.setVisibility(android.view.View.GONE);

        downloadManager.downloadDatabase(new PlaylistDownloadManager.DownloadCallback() {
            @Override
            public void onDownloadStarted() {
                runOnUiThread(() -> statusText.setText("Starting download..."));
            }

            @Override
            public void onDownloadProgress(int progress) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    progressText.setText(progress + "%");
                });
            }

            @Override
            public void onDownloadCompleted(File dbFile) {
                runOnUiThread(() -> {
                    statusText.setText("Download complete");
                    progressBar.setProgress(100);
                    progressText.setText("100%");
                    goToApp();
                });
            }

            @Override
            public void onDownloadFailed(String error) {
                runOnUiThread(() -> {
                    statusText.setText("Failed: " + error);
                    retryButton.setVisibility(android.view.View.VISIBLE);
                });
            }

            @Override
            public void onUpdateAvailable() { }
        });
    }

    private void goToApp() {
        // Decide which launcher to go to based on presence of leanback launcher activity
        // Default to FragmentMainActivity
        Intent intent = new Intent(this, FragmentMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Disable back press to prevent closing before download completes
    }
}

