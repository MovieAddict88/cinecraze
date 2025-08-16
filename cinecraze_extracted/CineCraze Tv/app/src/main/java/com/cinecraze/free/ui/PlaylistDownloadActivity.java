package com.cinecraze.free.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cinecraze.free.R;
import com.cinecraze.free.utils.PlaylistDownloadManager;

import java.io.File;

/**
 * Activity for downloading the playlist database from GitHub
 */
public class PlaylistDownloadActivity extends Activity implements PlaylistDownloadManager.DownloadCallback {
    
    private static final String TAG = "PlaylistDownloadActivity";
    
    private TextView statusText;
    private TextView progressText;
    private ProgressBar progressBar;
    private Button retryButton;
    private Button skipButton;
    private Button continueButton;
    
    private PlaylistDownloadManager downloadManager;
    private boolean downloadCompleted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_download);
        
        initViews();
        downloadManager = new PlaylistDownloadManager(this);
        
        // Check if database already exists
        if (downloadManager.isDatabaseExists() && !downloadManager.isDatabaseCorrupted()) {
            // Database exists and is valid, go to main activity
            startMainActivity();
            return;
        }
        
        // Start download
        startDownload();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.status_text);
        progressText = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        retryButton = findViewById(R.id.retry_button);
        skipButton = findViewById(R.id.skip_button);
        continueButton = findViewById(R.id.continue_button);
        
        // Set click listeners
        retryButton.setOnClickListener(v -> startDownload());
        skipButton.setOnClickListener(v -> startMainActivity());
        continueButton.setOnClickListener(v -> startMainActivity());
        
        // Initially hide buttons
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
    }
    
    private void startDownload() {
        // Reset UI
        statusText.setText("Downloading playlist database...");
        progressText.setText("0%");
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
        
        // Start download
        downloadManager.downloadDatabase(this);
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(this, FragmentMainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onDownloadStarted() {
        runOnUiThread(() -> {
            statusText.setText("Downloading playlist database...");
            progressText.setText("0%");
            progressBar.setProgress(0);
        });
    }
    
    @Override
    public void onDownloadProgress(int progress) {
        runOnUiThread(() -> {
            progressText.setText(progress + "%");
            progressBar.setProgress(progress);
        });
    }
    
    @Override
    public void onDownloadCompleted(File dbFile) {
        downloadCompleted = true;
        runOnUiThread(() -> {
            statusText.setText("Download completed successfully!");
            progressText.setText("100%");
            progressBar.setProgress(100);
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Database downloaded successfully", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onDownloadFailed(String error) {
        runOnUiThread(() -> {
            statusText.setText("Download failed: " + error);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            
            // Show retry and skip buttons
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Download failed: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onUpdateAvailable() {
        // This can be used for future update notifications
        runOnUiThread(() -> {
            statusText.setText("Update available for playlist database");
        });
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from closing the activity during download
        if (!downloadCompleted) {
            Toast.makeText(this, "Please wait for download to complete", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}